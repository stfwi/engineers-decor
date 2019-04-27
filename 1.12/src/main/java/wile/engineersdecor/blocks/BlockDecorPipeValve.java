/*
 * @file BlockDecorPipeValve.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basically a piece of pipe that does not connect to
 * pipes on the side, and conducts fluids only in one way.
 */
package wile.engineersdecor.blocks;

import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
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


public class BlockDecorPipeValve extends BlockDecorDirected
{
  public BlockDecorPipeValve(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    if(!placer.isSneaking()) state = state.withProperty(FACING, state.getValue(FACING).getOpposite());
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  {
    EnumFacing fc = state.getValue(FACING);
    if(fromPos.equals(pos.offset(fc)) || fromPos.equals(pos.offset(fc.getOpposite()))) update_te(world, state, pos);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    update_te(world, state, pos);
    world.notifyNeighborsOfStateChange(pos, this, true);
  }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorPipeValve.BTileEntity(); }

  private void update_te(World world, IBlockState state, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te instanceof BlockDecorPipeValve.BTileEntity) ((BlockDecorPipeValve.BTileEntity)te).block_reconfigure(state.getValue(FACING));
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider
  {
    public static final int FLUID_CAPACITY_MB = 1000; // likely also the max flow per tick
    private static final BackFlowHandler back_flow_handler_ = new BackFlowHandler();
    private final IFluidTankProperties[] fluid_props_ = {this};
    private EnumFacing block_facing_ = EnumFacing.NORTH;
    private boolean filling = false;

    public BTileEntity()
    {}

    public void block_reconfigure(EnumFacing facing)
    { block_facing_ = facing; }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorPipeValve)); }

    @Override
    public void onLoad()
    {
      if(!hasWorld()) return;
      final IBlockState state = world.getBlockState(pos);
      if((!(state.getBlock() instanceof BlockDecorPipeValve))) return;
      block_reconfigure(state.getValue(FACING));
    }

    // ICapabilityProvider --------------------------------------------------------------------

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing)
    { return ((capability==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) && ((facing==block_facing_) || (facing==block_facing_.getOpposite()))); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability!=CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return super.getCapability(capability, facing);
      return (facing==block_facing_) ? ((T)back_flow_handler_) : (((T)this));
    }

    // IFluidHandler/IFluidTankProperties ---------------------------------------------------------------

    @Override
    public int fill(FluidStack resource, boolean doFill)
    {
      if(filling) return 0;
      final TileEntity te = world.getTileEntity(pos.offset(block_facing_));
      if(te == null) return 0;
      final IFluidHandler fh = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing_.getOpposite());
      if(fh==null) return 0;
      filling = true; // in case someone does not check the cap, but just "forwards back" what is beeing filled right now.
      int n_filled = fh.fill(resource, doFill);
      filling = false;
      return n_filled;
    }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain)
    { return null; }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain)
    { return null; }

    @Override
    public IFluidTankProperties[] getTankProperties()
    { return fluid_props_; }

    // IFluidTankProperties --

    @Override
    @Nullable
    public FluidStack getContents()
    { return null; }

    public int getCapacity()
    { return FLUID_CAPACITY_MB; }

    @Override
    public boolean canFill()
    { return true; }

    @Override
    public boolean canDrain()
    { return false; }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack)
    { return true; }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack)
    { return false; }

    // Back flow prevention handler --

    private static class BackFlowHandler implements IFluidHandler, IFluidTankProperties
    {
      private final IFluidTankProperties[] props_ = {this};
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
      @Override @Nullable public FluidStack getContents() { return null; }
      @Override public int getCapacity() { return 0; }
      @Override public boolean canFill() { return false; }
      @Override public boolean canDrain() { return false; }
      @Override public boolean canFillFluidType(FluidStack fluidStack) { return false; }
      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return false; }
    }
  }
}
