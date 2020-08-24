/*
 * @file EdTestBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Creative mod testing block
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class EdTestBlock
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestBlock extends DecorBlock.Directed implements Auxiliaries.IExperimentalFeature, IDecorBlock
  {
    public TestBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.fullCube(); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new TestTileEntity(); }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
    { return true; }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, TileEntity te, boolean explosion)
    {
      ArrayList<ItemStack> list = new ArrayList<ItemStack>();
      list.add(new ItemStack(this, 1));
      return list;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof TestTileEntity)) return ActionResultType.SUCCESS;
      ((TestTileEntity)te).activated(player, hand, hit);
      return ActionResultType.SUCCESS;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestTileEntity extends TileEntity implements ITickableTileEntity, ICapabilityProvider //, IItemHandler, IEnergyStorage
  {
    private int tick_interval_ = 10;
    private int passive_tank_capacity_ = 32000;
    private FluidStack passive_tank_ = FluidStack.EMPTY;
    private FluidStack passive_drain_fluidstack_ = new FluidStack(Fluids.WATER, 1000);
    private int passive_drain_max_flowrate_ = 1000;
    private int passive_fill_max_flowrate_ = 1000;
    private int passive_num_drained_general_mb_ = 0;
    private int passive_num_drained_specific_mb_ = 0;
    private int passive_num_filled_specific_mb_ = 0;
    private int passive_num_fh_interactions_ = 0;
    private FluidStack active_fill_fluidstack_ = FluidStack.EMPTY;
    private int active_num_filled_ = 0;
    private int tick_timer_ = 0;

    // ------------------------------------------------------------------------------------------

    public TestTileEntity()
    { this(ModContent.TET_TEST_BLOCK); }

    public TestTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    // ------------------------------------------------------------------------------------------

    private Direction block_facing()
    {
      BlockState st = getWorld().getBlockState(getPos());
      return (st.getBlock() instanceof TestBlock) ? st.get(TestBlock.FACING) : Direction.NORTH;
    }

    private String dump_fluid_stack(FluidStack fs)
    {
      String s = "";
      if(fs.getFluid().getRegistryName().getNamespace() != "minecraft") s += fs.getFluid().getRegistryName().getNamespace()+":";
      s += fs.getFluid().getRegistryName().getPath();
      s += " x" + fs.getAmount();
      return "[" + s + "]";
    }

    public void activated(PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      if(world.isRemote()) return;
      final ItemStack held_stack = player.getHeldItem(hand);
      // Empty hand -> statistics
      {
        if(held_stack.isEmpty()) {
          String message = "";
          if(passive_num_filled_specific_mb_>0 || passive_num_drained_specific_mb_>0 || passive_num_drained_general_mb_>0) {
            message += "Fluid handler: filled:" + passive_num_filled_specific_mb_ + ", drained:" + (passive_num_drained_specific_mb_+ passive_num_drained_general_mb_) + ", interactions:" + passive_num_fh_interactions_ + "\n";
          }
          if(active_num_filled_>0) {
            message += "Fluid insertion:" + active_num_filled_ + "mb, (current:" + dump_fluid_stack(active_fill_fluidstack_) + ")\n";
          }
          if(message.isEmpty()) {
            message = "No fluid, energy, or item interactions done yet.";
          }
          Auxiliaries.playerChatMessage(player, message);
          return;
        }
      }
      // Fluid container -> set fluid to insert, increase/decrease amount.
      {
        final IFluidHandlerItem fhi = held_stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
        if((fhi != null)) {
          int ntanks = fhi.getTanks();
          if(ntanks == 0) return;
          int capacity = fhi.getTankCapacity(0);
          FluidStack fs = fhi.drain(capacity, FluidAction.SIMULATE);
          if(!fs.isEmpty()) {
            if(active_fill_fluidstack_.isEmpty()) {
              active_fill_fluidstack_ = fs.copy();
              Auxiliaries.playerChatMessage(player, "Fluid insertion fluid set: " + dump_fluid_stack(active_fill_fluidstack_));
            } else if(fs.isFluidEqual(active_fill_fluidstack_)) {
              active_fill_fluidstack_.grow(fs.getAmount());
              Auxiliaries.playerChatMessage(player, "Fluid insertion flowrate increased: " + dump_fluid_stack(active_fill_fluidstack_));
            } else {
              int amount = active_fill_fluidstack_.getAmount();
              active_fill_fluidstack_ = fs.copy();
              active_fill_fluidstack_.setAmount(amount);
              Auxiliaries.playerChatMessage(player, "Fluid insertion fluid changed: " + dump_fluid_stack(active_fill_fluidstack_));
            }
          } else {
            if(!active_fill_fluidstack_.isEmpty()) {
              active_fill_fluidstack_.shrink(1000);
              if(active_fill_fluidstack_.isEmpty()) active_fill_fluidstack_ = FluidStack.EMPTY;
              Auxiliaries.playerChatMessage(player, "Fluid insertion flowrate decreased: " + dump_fluid_stack(active_fill_fluidstack_));
            } else {
              Auxiliaries.playerChatMessage(player, "Fluid insertion disabled.");
            }
          }
          passive_drain_fluidstack_ = active_fill_fluidstack_.copy(); // currently no difference
          return;
        }
      }
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
      super.read(state, nbt);
      if(nbt.contains("passive_tank")) passive_tank_ = FluidStack.loadFluidStackFromNBT(nbt.getCompound("passive_tank"));
      if(nbt.contains("passive_drain")) passive_drain_fluidstack_ = FluidStack.loadFluidStackFromNBT(nbt.getCompound("passive_drain"));
      if(nbt.contains("active")) active_fill_fluidstack_ = FluidStack.loadFluidStackFromNBT(nbt.getCompound("active"));
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
      super.write(nbt);
      if(!passive_tank_.isEmpty()) nbt.put("passive_tank", passive_tank_.writeToNBT(new CompoundNBT()));
      if(!passive_drain_fluidstack_.isEmpty()) nbt.put("passive_drain", passive_drain_fluidstack_.writeToNBT(new CompoundNBT()));
      if(!active_fill_fluidstack_.isEmpty()) nbt.put("active", active_fill_fluidstack_.writeToNBT(new CompoundNBT()));
      return nbt;
    }

    @Override
    public void remove()
    {
      super.remove();
      fluid_handler_.invalidate();
    }

    // ICapabilityProvider --------------------------------------------------------------------

    private LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)new MainFluidHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
        if(facing != block_facing()) return fluid_handler_.cast();
        return LazyOptional.empty();
      }
      return super.getCapability(capability, facing);
    }

    // IFluidHandler ---------------------------------------------------------------------------

    private static class MainFluidHandler implements IFluidHandler
    {
      private  TestTileEntity te;
      public MainFluidHandler(TestTileEntity te)
      { this.te = te; }

      @Override public int getTanks()
      { return 1; }

      @Override public FluidStack getFluidInTank(int tank)
      { return FluidStack.EMPTY; }

      @Override public int getTankCapacity(int tank)
      { return te.passive_tank_capacity_; }

      @Override public FluidStack drain(FluidStack resource, FluidAction action)
      {
        ++te.passive_num_fh_interactions_;
        if(resource.isEmpty()) return FluidStack.EMPTY;
        if(!resource.isFluidEqual(te.passive_drain_fluidstack_)) return FluidStack.EMPTY;
        FluidStack st = resource.copy();
        st.setAmount(MathHelper.clamp(st.getAmount(), 0, te.passive_drain_max_flowrate_));
        if(st.isEmpty()) return FluidStack.EMPTY;
        if(action==FluidAction.EXECUTE) te.passive_num_drained_specific_mb_ += st.getAmount();
        return st;
      }

      @Override public FluidStack drain(int maxDrain, FluidAction action)
      {
        ++te.passive_num_fh_interactions_;
        maxDrain = MathHelper.clamp(maxDrain, 0, te.passive_drain_max_flowrate_);
        if((te.passive_drain_fluidstack_.isEmpty()) || (maxDrain<=0)) return FluidStack.EMPTY;
        if(action==FluidAction.EXECUTE) te.passive_num_drained_general_mb_ += maxDrain;
        FluidStack st = te.passive_drain_fluidstack_.copy();
        st.setAmount(maxDrain);
        return st;
      }

      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack)
      { return true; }

      @Override public int fill(FluidStack resource, FluidAction action)
      {
        ++te.passive_num_fh_interactions_;
        int amount = MathHelper.clamp(resource.getAmount(), 0, te.passive_fill_max_flowrate_);
        if(action == FluidAction.EXECUTE) {
          te.passive_num_filled_specific_mb_ += amount;
          if(te.passive_tank_.isFluidEqual(resource)) {
            int level = (int)MathHelper.clamp((long)te.passive_tank_.getAmount() + (long)amount, (long)0, (long)Integer.MAX_VALUE);
            te.passive_tank_.setAmount(level);
          }
        }
        return amount;
      }
    }

    // ITickableTileEntity ----------------------------------------------------------------------

    private void fluid_insertion()
    {
      if(active_fill_fluidstack_.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos.offset(block_facing()));
      if(te == null) return;
      final IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing().getOpposite()).orElse(null);
      if(fh == null) return;
      int filled = fh.fill(active_fill_fluidstack_.copy(), FluidAction.EXECUTE);
      active_num_filled_ += filled;
    }

    @Override
    public void tick()
    {
      if(world.isRemote) return;
      if(--tick_timer_ > 0) return;
      tick_interval_ = MathHelper.clamp(tick_interval_ ,1 , 200);
      tick_timer_ = tick_interval_;
      fluid_insertion();
    }

  }
}
