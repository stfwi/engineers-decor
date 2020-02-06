/*
 * @file BlockDecorPassiveFluidAccumulator.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * A device that collects fluids from adjacent tank outputs
 * when a pump drains on the (only) output side. Shall support
 * high flow rates, and a vavuum suction delay. Shall not drain
 * high amounts of fluid from the adjacent tanks when no fluid
 * is requested at the output port. Shall drain balanced from
 * the adjacent input sides.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorPassiveFluidAccumulator extends BlockDecor.Directed implements IDecorBlock
{
  public BlockDecorPassiveFluidAccumulator(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BlockDecorPassiveFluidAccumulator.BTileEntity(); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return true;
    ((BTileEntity)te).send_device_stats(player);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te instanceof BlockDecorPipeValve.BTileEntity) ((BTileEntity)te).block_changed();
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickableTileEntity, ICapabilityProvider
  {
    protected static int tick_idle_interval = 20; // ca 1000ms, simulates suction delay and saves CPU when not drained.
    protected static int max_flowrate = 1000;
    private Direction block_facing_ = Direction.NORTH;
    private FluidStack tank_ = FluidStack.EMPTY;
    private int last_drain_request_amount_ = 0;
    private int vacuum_ = 0;
    private int tick_timer_ = 0;
    private int round_robin_ = 0;
    private boolean initialized_ = false;
    private int total_volume_filled_ = 0;
    private int total_volume_drained_ = 0;

    public void send_device_stats(PlayerEntity player)
    {
      int t_vol = tank_.getAmount();
      Auxiliaries.playerChatMessage(player,"" + t_vol + "mB");
    }

    public void block_changed()
    { initialized_ = false; tick_timer_ = MathHelper.clamp(tick_timer_ , 0, tick_idle_interval); }

    // TileEntity ------------------------------------------------------------------------------

    public BTileEntity()
    { this(ModContent.TET_PASSIVE_FLUID_ACCUMULATOR); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    @Override
    public void read(CompoundNBT nbt)
    {
      super.read(nbt);
      tank_ = (!nbt.contains("tank")) ? (FluidStack.EMPTY) : (FluidStack.loadFluidStackFromNBT(nbt.getCompound("tank")));
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
      super.write(nbt);
      if(!tank_.isEmpty()) nbt.put("tank", tank_.writeToNBT(new CompoundNBT()));
      return nbt;
    }

    // Input flow handler ---------------------------------------------------------------------

    private static class InputFillHandler implements IFluidHandler
    {
      private final BTileEntity parent_;
      InputFillHandler(BTileEntity parent) { parent_ = parent; }
      @Override public int getTanks() { return 0; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return max_flowrate; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return true; }
      @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    // Output flow handler ---------------------------------------------------------------------

    private static class OutputFlowHandler implements IFluidHandler
    {
      private final BTileEntity te;
      OutputFlowHandler(BTileEntity parent) { te = parent; }
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return te.tank_.copy(); }
      @Override public int getTankCapacity(int tank) { return max_flowrate; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return true; }
      @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

      @Override public FluidStack drain(FluidStack resource, FluidAction action)
      {
        if((resource==null) || (te.tank_.isEmpty())) return FluidStack.EMPTY;
        return (!(te.tank_.isFluidEqual(resource))) ? (FluidStack.EMPTY) : drain(resource.getAmount(), action);
      }

      @Override public FluidStack drain(int maxDrain, FluidAction action)
      {
        if(!te.initialized_) return FluidStack.EMPTY;
        if((action==FluidAction.EXECUTE) && (maxDrain > 0)) te.last_drain_request_amount_ = maxDrain;
        if(te.tank_.isEmpty()) return FluidStack.EMPTY;
        maxDrain = MathHelper.clamp(maxDrain ,0 , te.tank_.getAmount());
        FluidStack res = te.tank_.copy();
        if(action!=FluidAction.EXECUTE) return res;
        res.setAmount(maxDrain);
        te.tank_.setAmount(te.tank_.getAmount()-maxDrain);
        if(te.tank_.getAmount() <= 0) te.tank_ = FluidStack.EMPTY;
        te.total_volume_drained_ += res.getAmount();
        return res;
      }
    }

    // ICapabilityProvider --------------------------------------------------------------------

    private final LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> new OutputFlowHandler(this));
    private final LazyOptional<IFluidHandler> fill_handler_  = LazyOptional.of(() -> new InputFillHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if((initialized_) && (!this.removed) && (facing != null)) {
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
          if(facing == block_facing_) return fluid_handler_.cast();
          return fill_handler_.cast();
        }
      }
      return super.getCapability(capability, facing);
    }

    // ITickable--------------------------------------------------------------------------------

    public void tick()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = tick_idle_interval;
      if(!initialized_) {
        initialized_ = true;
        BlockState state = world.getBlockState(pos);
        if((state==null) || (!(state.getBlock() instanceof BlockDecorPassiveFluidAccumulator))) return;
        block_facing_ = state.get(FACING);
      }
      int n_requested = last_drain_request_amount_;
      last_drain_request_amount_ = 0;
      if(n_requested > 0) {
        vacuum_ += 2;
        if(vacuum_ > 5) vacuum_ = 5;
      } else {
        if((--vacuum_) <= 0) {
          vacuum_ = 0;
          if(!tank_.isEmpty()) {
            return; // nothing to do, noone's draining.
          } else {
            n_requested = 10; // drip in
          }
        }
      }
      boolean has_refilled = false;
      n_requested += (vacuum_ * 50);
      int tank_buffer_needed = n_requested;
      if(tank_buffer_needed > max_flowrate) tank_buffer_needed = max_flowrate;
      for(int i=0; i<6; ++i) {
        if(++round_robin_ > 5) round_robin_ = 0;
        if(n_requested <= 0) break;
        if((tank_.getAmount() >= tank_buffer_needed)) break;
        final Direction f = Direction.byIndex(round_robin_);
        if(f == block_facing_) continue;
        final TileEntity te = world.getTileEntity(pos.offset(f));
        if((te==null) || (te instanceof BTileEntity)) continue;
        final IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite()).orElse(null);
        if(fh==null) continue;
        if(tank_.isEmpty()) {
          FluidStack res = fh.drain(n_requested, FluidAction.EXECUTE).copy();
          if(res.isEmpty()) continue;
          total_volume_filled_ += res.getAmount();
          tank_ = res.copy();
          has_refilled = true;
        } else {
          if((tank_.getAmount() + n_requested) > max_flowrate) n_requested = (max_flowrate - tank_.getAmount());
          FluidStack rq = tank_.copy();
          rq.setAmount(n_requested);
          FluidStack res = fh.drain(rq, FluidAction.SIMULATE);
          if(!res.isFluidEqual(rq)) continue;
          res = fh.drain(rq, FluidAction.EXECUTE);
          if(res.isEmpty()) continue;
          tank_.setAmount(tank_.getAmount()+res.getAmount());
          total_volume_filled_ += res.getAmount();
          has_refilled = true;
          if(tank_.getAmount() >= max_flowrate) break;
        }
      }
      if(has_refilled) tick_timer_ = 0;
    }
  }
}
