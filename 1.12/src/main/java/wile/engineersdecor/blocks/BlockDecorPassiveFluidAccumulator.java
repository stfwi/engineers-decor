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

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
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

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider
, ModAuxiliaries.IExperimentalFeature
  {
    protected static int tank_fill_rate_mb = 1000;
    protected static int tank_capacity_mb = 2000;
    private final IFluidTankProperties[] fluid_props_ = {this};
    private final InputFillHandler fill_handler_ = new InputFillHandler(this);
    private EnumFacing block_facing_ = EnumFacing.NORTH;
    private FluidStack tank_ = null;
    private FluidStack last_filled_ = null;
    private FluidStack last_drain_request_fluid_ = null;
    private int last_drain_request_amount_ = 0;

    public BTileEntity()
    {}

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorPipeValve)); }

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
    { return ((capability==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)) || super.hasCapability(capability, facing); }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing)
    { return (capability!=CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) ? (super.getCapability(capability, facing)) : (facing==block_facing_) ? (((T)this)) : ((T)fill_handler_); }

    // IFluidHandler/IFluidTankProperties of the output port -----------------------------------

    @Override public IFluidTankProperties[] getTankProperties()
    { return fluid_props_; }

    @Override public int fill(FluidStack resource, boolean doFill)
    { return 0; }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain)
    {
      last_drain_request_fluid_ = resource.copy();
      return ((tank_==null) || (!tank_.isFluidEqual(resource))) ? (null) : drain(resource.amount, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
      last_drain_request_amount_ = maxDrain;
      if(tank_==null) return null;
      if(maxDrain >= tank_.amount) {
        if(!doDrain) return tank_.copy();
        FluidStack res = tank_;
        tank_ = null;
        return res;
      } else {
        FluidStack res = tank_.copy();
        res.amount = maxDrain;
        if(doDrain) tank_.amount -= maxDrain;
        return res;
      }
    }

    // IFluidTankProperties --
    @Override @Nullable public FluidStack getContents() { return (tank_==null) ? (null) : (tank_.copy()); }
    @Override public int getCapacity() { return tank_capacity_mb; }
    @Override public boolean canFill() { return false; }
    @Override public boolean canDrain()  { return true; }
    @Override public boolean canFillFluidType(FluidStack fluidStack)  { return false; }
    @Override public boolean canDrainFluidType(FluidStack fluidStack) { return true; }

    // Output flow handler --
    private static class InputFillHandler implements IFluidHandler, IFluidTankProperties
    {
      private final BTileEntity parent_;
      private final IFluidTankProperties[] props_ = {this};
      InputFillHandler(BTileEntity parent) { parent_ = parent; }
      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack getContents() { return (parent_.tank_==null) ? (null) : (parent_.tank_.copy()); }
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int getCapacity() { return tank_capacity_mb; }
      @Override public boolean canFill() { return true; }
      @Override public boolean canDrain() { return false; }
      @Override public boolean canFillFluidType(FluidStack fluidStack) { return true; }
      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return false; }

      @Override public int fill(FluidStack resource, boolean doFill)
      {
        FluidStack res = resource.copy();
        if(parent_.tank_ == null) {
          res.amount = MathHelper.clamp(res.amount, 0, tank_fill_rate_mb);
          if(doFill) parent_.tank_ = res;
          return res.amount;
        } else {
          res.amount = MathHelper.clamp(res.amount, 0, Math.min(tank_fill_rate_mb, tank_capacity_mb-parent_.tank_.amount));
          if((res.amount <= 0) || (!parent_.tank_.isFluidEqual(resource))) return 0;
          if(doFill) parent_.tank_.amount += res.amount;
          return res.amount;
        }
      }
    }
  }
}
