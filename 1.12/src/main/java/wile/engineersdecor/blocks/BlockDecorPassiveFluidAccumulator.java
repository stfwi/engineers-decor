/*
 * @file BlockDecorPassiveFluidAccumulator.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basically a piece of pipe that does not connect to
 * pipes on the side, and conducts fluids only in one way.
 */
package wile.engineersdecor.blocks;

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

  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  { TileEntity te = world.getTileEntity(pos); if(te instanceof BlockDecorPipeValve.BTileEntity) ((BTileEntity)te).block_changed(); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider, ITickable, ModAuxiliaries.IExperimentalFeature
  {
    protected static int tick_idle_interval = 20; // ca 1000ms, simulates suction delay and saves CPU when not drained.
    protected static int max_flowrate = 1000;
    protected static int tank_capacity_mb = max_flowrate * 2;
    private final IFluidTankProperties[] fluid_props_ = {this};
    private final InputFillHandler fill_handler_ = new InputFillHandler(this);
    private EnumFacing block_facing_ = EnumFacing.NORTH;
    private FluidStack tank_ = null;
    private FluidStack last_drain_request_fluid_ = null;
    private int last_drain_request_amount_ = 0;
    private int vacuum_ = 0;
    private int tick_timer_ = 0;
    private int round_robin_ = 0;
    private boolean initialized_ = false;

    public void block_changed()
    { initialized_ = false; tick_timer_ = MathHelper.clamp(tick_timer_ , 0, tick_idle_interval); }

    // Output flow handler ---------------------------------------------------------------------

    private static class InputFillHandler implements IFluidHandler, IFluidTankProperties
    {
      private final BTileEntity parent_;
      private final IFluidTankProperties[] props_ = {this};
      InputFillHandler(BTileEntity parent) { parent_ = parent; }
      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int getCapacity() { return tank_capacity_mb; }
      @Override public boolean canFill() { return true; }
      @Override public boolean canDrain() { return false; }
      @Override public boolean canFillFluidType(FluidStack fluidStack) { return true; }
      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return false; }

      @Nullable
      @Override public FluidStack getContents()
      {
        if(parent_.tank_==null) return null;
        FluidStack res = parent_.tank_.copy();
        if(res.amount > max_flowrate) res.amount = max_flowrate;
        return res;
      }

      @Override public int fill(FluidStack resource, boolean doFill)
      {
        if(!parent_.initialized_) return 0;
        FluidStack res = resource.copy();
        if(parent_.tank_ == null) {
          res.amount = MathHelper.clamp(res.amount, 0, max_flowrate*2);
          if(doFill) parent_.tank_ = res;
          return res.amount;
        } else {
          res.amount = MathHelper.clamp(res.amount, 0, Math.min(max_flowrate*2, tank_capacity_mb-parent_.tank_.amount));
          if((res.amount <= 0) || (!parent_.tank_.isFluidEqual(resource))) return 0;
          if(doFill) parent_.tank_.amount += res.amount;
          return res.amount;
        }
      }
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
      if(doDrain) last_drain_request_fluid_ = resource.copy();
      return ((tank_==null) || (!tank_.isFluidEqual(resource))) ? (null) : drain(resource.amount, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
      if(!initialized_) return null;
      if(doDrain) last_drain_request_amount_ = maxDrain;
      if(tank_==null) return null;
      FluidStack res;
      if(maxDrain >= tank_.amount) {
        if(!doDrain) return tank_.copy();
        res = tank_;
        tank_ = null;
      } else {
        res = tank_.copy();
        res.amount = maxDrain;
        if(doDrain) tank_.amount -= maxDrain;
      }
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
      final FluidStack req_res = last_drain_request_fluid_;
      last_drain_request_amount_ = 0;
      last_drain_request_fluid_ = null;
      if(tank_!=null) {
        if((n_requested > 0) && ((req_res == null) || (tank_.isFluidEqual(req_res)))) { vacuum_ += 2; } else { --vacuum_; }
        vacuum_ = MathHelper.clamp(vacuum_, 0, 5);
        if(vacuum_ <= 0) return; // nothing to do, noone's draining or can't because the fuild does not match.
      } else {
        n_requested = 10; // drip in
      }
      int tank_level = MathHelper.clamp( (tank_==null) ? 0 : tank_.amount, 0, tank_capacity_mb);
      if(tank_level >= Math.min(tank_capacity_mb, n_requested * 4)) return; // enough reserve
      tick_timer_ = 0; // pull next tick
      FluidStack match = (tank_==null) ? (req_res) : (tank_);
      for(int i=0; i<6; ++i) {
        if(++round_robin_ > 5) round_robin_ = 0;
        final EnumFacing f = EnumFacing.byIndex(round_robin_);
        if(f == block_facing_) continue;
        final TileEntity te = world.getTileEntity(pos.offset(f));
        if((te==null) || (!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite()))) continue;
        final IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, f.getOpposite());
        if(fh==null) continue;
        int refill = Math.min(Math.max(n_requested, 100), tank_capacity_mb-tank_level);
        if(refill <= 0) break;
        FluidStack res;
        if(match==null) {
          res = fh.drain(refill, true);
          if(res != null) match = res.copy();
        } else {
          match.amount = refill;
          res = fh.drain(match.copy(), true);
        }
        if(res == null) continue;
        if(tank_==null) {
          tank_ = res;
        } else {
          tank_.amount += res.amount;
        }
        if(tank_.amount >= tank_capacity_mb) break;
      }
    }
  }
}
