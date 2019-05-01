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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorPassiveFluidAccumulator extends BlockDecorDirected implements ModAuxiliaries.IExperimentalFeature
{
  public BlockDecorPassiveFluidAccumulator(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorPassiveFluidAccumulator.BTileEntity(); }

  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return true;
    ((BTileEntity)te).debug_info_dump(player);
    return true;
  }

  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  { TileEntity te = world.getTileEntity(pos); if(te instanceof BlockDecorPipeValve.BTileEntity) ((BTileEntity)te).block_changed(); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider, ITickable, ModAuxiliaries.IExperimentalFeature
  {
    protected static int tick_idle_interval = 20; // ca 1000ms, simulates suction delay and saves CPU when not drained.
    protected static int max_flowrate = 1000;
    private final IFluidTankProperties[] fluid_props_ = {this};
    private final InputFillHandler fill_handler_ = new InputFillHandler(this);
    private EnumFacing block_facing_ = EnumFacing.NORTH;
    private FluidStack tank_ = null;
    private int last_drain_request_amount_ = 0;
    private int vacuum_ = 0;
    private int tick_timer_ = 0;
    private int round_robin_ = 0;
    private boolean initialized_ = false;


    private int total_volume_filled_ = 0;
    private int total_volume_drained_ = 0;
    @Deprecated
    public void debug_info_dump(EntityPlayer player)
    {
      int t_vol = (tank_==null) ? 0 : (tank_.amount);
      ModAuxiliaries.playerChatMessage(player,"pfacc I:" + total_volume_filled_ + " O:" + total_volume_drained_ + " B:" + t_vol);
    }

    public void block_changed()
    { initialized_ = false; tick_timer_ = MathHelper.clamp(tick_timer_ , 0, tick_idle_interval); }

    // Output flow handler ---------------------------------------------------------------------

    private static class InputFillHandler implements IFluidHandler, IFluidTankProperties
    {
      private final BTileEntity parent_;
      private final IFluidTankProperties[] props_ = {this};
      InputFillHandler(BTileEntity parent) { parent_ = parent; }
      @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack getContents() { return null; }
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int getCapacity() { return max_flowrate; }
      @Override public boolean canFill() { return true; }
      @Override public boolean canDrain() { return false; }
      @Override public boolean canFillFluidType(FluidStack fluidStack) { return true; }
      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return false; }
    }

    // TileEntity ------------------------------------------------------------------------------

    public BTileEntity()
    {}

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    {
      block_changed();
      return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorPipeValve));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
      super.readFromNBT(nbt);
      tank_ = (!nbt.hasKey("tank")) ? (null) : (FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("tank")));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
      super.writeToNBT(nbt);
      if(tank_ != null) nbt.setTag("tank", tank_.writeToNBT(new NBTTagCompound()));
      return nbt;
    }

    // ICapabilityProvider --------------------------------------------------------------------

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing)
    { return (initialized_ && (capability==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)) || super.hasCapability(capability, facing); }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability != CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return super.getCapability(capability, facing);
      return (facing==block_facing_) ? (((T)this)) : ((T)fill_handler_);
    }

    // IFluidHandler of the output port --------------------------------------------------------

    @Override
    public IFluidTankProperties[] getTankProperties()
    { return fluid_props_; }

    @Override
    public int fill(FluidStack resource, boolean doFill)
    { return 0; }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain)
    {
      if((resource==null) || (tank_==null)) return null;
      return (!(tank_.isFluidEqual(resource))) ? (null) : drain(resource.amount, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
      if(!initialized_) return null;
      if(doDrain && (maxDrain > 0)) last_drain_request_amount_ = maxDrain;
      if(tank_==null) return null;
      maxDrain = MathHelper.clamp(maxDrain ,0 , tank_.amount);
      if(!doDrain) return tank_.copy();
      FluidStack res = tank_.copy();
      res.amount = maxDrain;
      tank_.amount -= maxDrain;
      if(tank_.amount <= 0) tank_= null;
      total_volume_drained_ += res.amount;
      return res;
    }

    // IFluidTankProperties --------------------------------------------------------------------

    @Override @Nullable public FluidStack getContents() { return (tank_==null) ? (null) : (tank_.copy()); }
    @Override public int getCapacity() { return max_flowrate; }
    @Override public boolean canFill() { return false; }
    @Override public boolean canDrain()  { return true; }
    @Override public boolean canFillFluidType(FluidStack fluidStack)  { return false; }
    @Override public boolean canDrainFluidType(FluidStack fluidStack) { return true; }

    // ITickable--------------------------------------------------------------------------------

    public void update()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = tick_idle_interval;
      if(!initialized_) {
        initialized_ = true;
        IBlockState state = world.getBlockState(pos);
        if((state==null) || (!(state.getBlock() instanceof BlockDecorPassiveFluidAccumulator))) return;
        block_facing_ = state.getValue(FACING);
      }
      int n_requested = last_drain_request_amount_;
      last_drain_request_amount_ = 0;
      if(n_requested > 0) {
        vacuum_ += 2;
        if(vacuum_ > 5) vacuum_ = 5;
      } else {
        if((--vacuum_) <= 0) {
          vacuum_ = 0;
          if(tank_!=null) {
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
        if(((tank_!=null) && (tank_.amount >= tank_buffer_needed))) break;
        final EnumFacing f = EnumFacing.byIndex(round_robin_);
        if(f == block_facing_) continue;
        final TileEntity te = world.getTileEntity(pos.offset(f));
        if((te==null) || (te instanceof BTileEntity) || (!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite()))) continue;
        final IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite());
        if(fh==null) continue;
        if(tank_==null) {
          FluidStack res = fh.drain(n_requested, true);
          if((res == null) || (res.amount==0)) continue;
          total_volume_filled_ += res.amount;
          tank_ = res.copy();
          has_refilled = true;
        } else {
          if((tank_.amount + n_requested) > max_flowrate) n_requested = (max_flowrate - tank_.amount);
          FluidStack rq = tank_.copy();
          rq.amount = n_requested;
          FluidStack res = fh.drain(rq, true);
          if(res == null) continue;
          tank_.amount += res.amount;
          total_volume_filled_ += res.amount;
          has_refilled = true;
          if(tank_.amount >= max_flowrate) break;
        }
      }
      if(has_refilled) tick_timer_ = 0;
    }
  }
}
