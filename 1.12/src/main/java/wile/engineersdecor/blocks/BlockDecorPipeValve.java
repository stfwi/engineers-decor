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

import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
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
import wile.engineersdecor.ModEngineersDecor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorPipeValve extends BlockDecorDirected
{
  public static final PropertyInteger RS_CONNECTION_DIR = PropertyInteger.create("rsdir", 0,4);

  public static void on_config(int container_size_decl, int redstone_slope)
  {
    BTileEntity.fluid_capacity_mb = MathHelper.clamp(container_size_decl, 1, 10000);
    BTileEntity.redstone_flow_slope_mb = MathHelper.clamp(redstone_slope, 1, 10000);
    ModEngineersDecor.logger.info("Config pipe valve: maxflow:" + BTileEntity.fluid_capacity_mb + "mb, redstone amp:" + BTileEntity.redstone_flow_slope_mb + "mb/sig");
  }

  public BlockDecorPipeValve(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, RS_CONNECTION_DIR); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
    if(!placer.isSneaking()) state = state.withProperty(FACING, state.getValue(FACING).getOpposite()).withProperty(RS_CONNECTION_DIR, 0);
    return state;
  }

  // world to model index transformations. [Facing block][Facing neighbour] -> int 0=nothing, 1=top, 2=right, 3=down, 4=left.
  private static final int rsconnectors[][] = {
   //D  U  N  S  W  E
    {0, 0, 1, 3, 4, 2}, // D
    {0, 0, 3, 1, 4, 2}, // U
    {3, 1, 0, 0, 4, 2}, // N
    {3, 1, 0, 0, 2, 4}, // S
    {3, 1, 2, 4, 0, 0}, // W
    {3, 1, 4, 2, 0, 0}, // E
  };

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    if((config & (CFG_REDSTONE_CONTROLLED))==0) return state;
    EnumFacing.Axis bfa = state.getValue(FACING).getAxis();
    int bfi = state.getValue(FACING).getIndex();
    for(EnumFacing f:EnumFacing.VALUES) {
      if(f.getAxis() == bfa) continue;
      BlockPos nbp = pos.offset(f);
      IBlockState nbs = world.getBlockState(nbp);
      if(nbs.canProvidePower() && (nbs.getBlock().canConnectRedstone(nbs, world, nbp, f))) {
        return state.withProperty(RS_CONNECTION_DIR, rsconnectors[state.getValue(FACING).getIndex()][f.getIndex()]);
      }
    }
    return state.withProperty(RS_CONNECTION_DIR, 0);
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

  public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EnumFacing side)
  { return (side!=null) && (side!=state.getValue(FACING)) && (side!=state.getValue(FACING).getOpposite()); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorPipeValve.BTileEntity(); }

  private void update_te(World world, IBlockState state, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te instanceof BlockDecorPipeValve.BTileEntity) ((BlockDecorPipeValve.BTileEntity)te).block_reconfigure(state.getValue(FACING), config);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider
  {
    private static final BackFlowHandler back_flow_handler_ = new BackFlowHandler();
    protected static int fluid_capacity_mb = 1000; // likely also the max flow per tick
    protected static int redstone_flow_slope_mb = 1000/15;
    private final IFluidTankProperties[] fluid_props_ = {this};
    private EnumFacing block_facing_ = EnumFacing.NORTH;
    private boolean filling_ = false;
    private boolean filling_enabled_ = true;
    private long block_config_ = 0;

    public BTileEntity()
    {}

    public void block_reconfigure(EnumFacing facing, long block_config)
    {
      block_facing_ = facing;
      block_config_ = block_config;
      filling_enabled_ = false;
      IFluidHandler fh = forward_fluid_handler();
      if(fh!=null) {
        if(fh.getTankProperties().length==0) {
          filling_enabled_ = true; // we don't know, so in doubt try filling.
        } else {
          for(IFluidTankProperties fp:fh.getTankProperties()) {
            if((fp!=null) && (fp.canFill())) { filling_enabled_ = true; break; }
          }
        }
      }
    }

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
      block_reconfigure(state.getValue(FACING), block_config_);
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

    @Nullable
    private IFluidHandler forward_fluid_handler()
    {
      final TileEntity te = world.getTileEntity(pos.offset(block_facing_));
      if(te == null) return null;
      return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing_.getOpposite());
    }

    @Override
    public int fill(FluidStack resource, boolean doFill)
    {
      if((filling_) || (!filling_enabled_)) return 0;
      if((block_config_ & CFG_REDSTONE_CONTROLLED) != 0) {
        int rs = world.getRedstonePowerFromNeighbors(pos);
        if(rs <= 0) return 0;
        if(((block_config_ & CFG_ANALOG) != 0) && (rs < 15)) resource.amount = MathHelper.clamp(rs * redstone_flow_slope_mb, 1, resource.amount);
      }
      final IFluidHandler fh = forward_fluid_handler();
      if(fh==null) return 0;
      filling_ = true; // in case someone does not check the cap, but just "forwards back" what is beeing filled right now.
      int n_filled = forward_fluid_handler().fill(resource, doFill);
      filling_ = false;
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
    { return fluid_capacity_mb; }

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
