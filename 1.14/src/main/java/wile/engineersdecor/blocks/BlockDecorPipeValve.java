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

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.state.BooleanProperty;
import net.minecraft.world.IWorld;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.common.capabilities.ICapabilityProvider;
//import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
//import net.minecraftforge.fluids.capability.IFluidHandler;
//import net.minecraftforge.fluids.capability.IFluidTankProperties;
//import net.minecraftforge.fluids.FluidStack;
import javax.annotation.Nullable;


public class BlockDecorPipeValve extends BlockDecorDirected
{
  public static final BooleanProperty RS_CN_N = BooleanProperty.create("rs_n");
  public static final BooleanProperty RS_CN_S = BooleanProperty.create("rs_s");
  public static final BooleanProperty RS_CN_E = BooleanProperty.create("rs_e");
  public static final BooleanProperty RS_CN_W = BooleanProperty.create("rs_w");
  public static final BooleanProperty RS_CN_U = BooleanProperty.create("rs_u");
  public static final BooleanProperty RS_CN_D = BooleanProperty.create("rs_d");

  public static void on_config(int container_size_decl, int redstone_slope)
  {
    BTileEntity.fluid_maxflow_mb = MathHelper.clamp(container_size_decl, 1, 10000);
    BTileEntity.redstone_flow_slope_mb = MathHelper.clamp(redstone_slope, 1, 10000);
    ModEngineersDecor.logger().info("Config pipe valve: maxflow:" + BTileEntity.fluid_maxflow_mb + "mb, redstone amp:" + BTileEntity.redstone_flow_slope_mb + "mb/sig");
  }

  public BlockDecorPipeValve(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  private BlockState get_rsconnector_state(BlockState state, IWorld world, BlockPos pos, @Nullable BlockPos fromPos)
  {
    if((config & (CFG_REDSTONE_CONTROLLED))==0) return state;
    Direction.Axis bfa = state.get(FACING).getAxis();
    int bfi = state.get(FACING).getIndex();
    for(Direction f:Direction.values()) {
      boolean cn = (f.getAxis() != bfa);
      if(cn) {
        BlockPos nbp = pos.offset(f);
        if((fromPos != null) && (!nbp.equals(fromPos))) continue; // do not change connectors except form the frompos.
        BlockState nbs = world.getBlockState(nbp);
        if(!nbs.canProvidePower()) cn = false; // @todo check if there is a direction selective canProvidePower().
      }
      switch(f) {
        case NORTH: state = state.with(RS_CN_N, cn); break;
        case SOUTH: state = state.with(RS_CN_S, cn); break;
        case EAST:  state = state.with(RS_CN_E, cn); break;
        case WEST:  state = state.with(RS_CN_W, cn); break;
        case UP:    state = state.with(RS_CN_U, cn); break;
        case DOWN:  state = state.with(RS_CN_D, cn); break;
      }
    }
    return state;
  }

  private void update_te(World world, BlockState state, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te instanceof BlockDecorPipeValve.BTileEntity) ((BlockDecorPipeValve.BTileEntity)te).block_reconfigure(state.get(FACING), config);
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { builder.add(FACING, RS_CN_N, RS_CN_S, RS_CN_E, RS_CN_W, RS_CN_U, RS_CN_D); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    return super.getStateForPlacement(context).with(RS_CN_N, false).with(RS_CN_S, false).with(RS_CN_E, false)
                .with(RS_CN_W, false).with(RS_CN_U, false).with(RS_CN_D, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return get_rsconnector_state(state, world, pos, null); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
  {
    update_te(world, state, pos);
    world.notifyNeighborsOfStateChange(pos,this);
  }

  @Deprecated
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  {
    Direction fc = state.get(FACING);
    if(fromPos.equals(pos.offset(fc)) || fromPos.equals(pos.offset(fc.getOpposite()))) {
      update_te(world, state, pos);
    } else {
      BlockState connector_state = get_rsconnector_state(state, world, pos, fromPos);
      if(!state.equals(connector_state)) world.setBlockState(pos, connector_state);
    }
  }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BlockDecorPipeValve.BTileEntity(); }

  @Override
  public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
  { return (side!=null) && (side!=state.get(FACING)) && (side!=state.get(FACING).getOpposite()); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity // implements IFluidHandler, IFluidTankProperties, ICapabilityProvider, IFluidPipe
  {
    protected static int fluid_maxflow_mb = 1000;
    protected static int redstone_flow_slope_mb = 1000/15;
    // private final IFluidTankProperties[] fluid_props_ = {this};
    private Direction block_facing_ = Direction.NORTH;
    private boolean filling_ = false;
    private boolean getlocked_ = false;
    private boolean filling_enabled_ = true;
    private long block_config_ = 0;


    public BTileEntity()
    { this(ModContent.TET_STRAIGHT_PIPE_VALVE); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); }


    public void block_reconfigure(Direction facing, long block_config)
    {
      block_facing_ = facing;
      block_config_ = block_config;
      filling_enabled_ = false;
//      IFluidHandler fh = forward_fluid_handler();
//      if(fh!=null) {
//        if(fh.getTankProperties().length==0) {
//          filling_enabled_ = true; // we don't know, so in doubt try filling.
//        } else {
//          for(IFluidTankProperties fp:fh.getTankProperties()) {
//            if((fp!=null) && (fp.canFill())) { filling_enabled_ = true; break; }
//          }
//        }
//      }
    }

    // TileEntity ------------------------------------------------------------------------------
//
//    @Override
//    public void onLoad()
//    {
//      if(!hasWorld()) return;
//      final BlockState state = world.getBlockState(pos);
//      if((!(state.getBlock() instanceof BlockDecorPipeValve))) return;
//      block_reconfigure(state.get(FACING), block_config_);
//      world.notifyNeighborsOfStateChange(pos, state.getBlock());
//    }
//
//    @Override
//    public void read(CompoundNBT nbt)
//    {
//      super.read(nbt);
//      int i = nbt.getInt("facing");
//      if((i>=0) || (i<6)) block_facing_ = Direction.byIndex(i);
//      block_config_ = nbt.getLong("conf");
//    }
//
//    @Override
//    public CompoundNBT write(CompoundNBT nbt)
//    {
//      super.write(nbt);
//      nbt.putInt("facing", block_facing_.getIndex());
//      nbt.putLong("conf", block_config_);
//      return nbt;
//    }
//
//    // ICapabilityProvider --------------------------------------------------------------------
//
//    private static final BackFlowHandler back_flow_handler_singleton_ = new BackFlowHandler();
//    private LazyOptional<IFluidHandler> back_flow_handler_ = LazyOptional.of(() -> (IFluidHandler)back_flow_handler_singleton_);
//    private LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)this);
//
//    @Override
//    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
//    {
//      if(!this.removed && (facing != null)) {
//        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
//          if(facing == block_facing_) return fluid_handler_.cast();
//          if(facing == block_facing_.getOpposite()) return back_flow_handler_.cast();
//        }
//      }
//      return super.getCapability(capability, facing);
//    }
//
//    // IFluidHandler/IFluidTankProperties ---------------------------------------------------------------
//
//    @Nullable
//    private IFluidHandler forward_fluid_handler()
//    {
//      final TileEntity te = world.getTileEntity(pos.offset(block_facing_));
//      if(te == null) return null;
//      return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing_.getOpposite()).orElse(null);
//    }
//
//    @Override
//    public int fill(FluidStack resource, boolean doFill)
//    {
//      if((filling_) || (!filling_enabled_)) return 0;
//      if((block_config_ & CFG_REDSTONE_CONTROLLED) != 0) {
//        int rs = world.getRedstonePowerFromNeighbors(pos);
//        if(rs <= 0) return 0;
//        if(((block_config_ & CFG_ANALOG) != 0) && (rs < 15)) resource.amount = MathHelper.clamp(rs * redstone_flow_slope_mb, 1, resource.amount);
//      }
//      FluidStack res = resource.copy();
//      if(res.amount > fluid_maxflow_mb) res.amount = fluid_maxflow_mb;
//      final IFluidHandler fh = forward_fluid_handler();
//      if(fh==null) return 0;
//      filling_ = true; // in case someone does not check the cap, but just "forwards back" what is beeing filled right now.
//      if(res.amount > 50) {
//        final TileEntity te = world.getTileEntity(pos.offset(block_facing_));
//        if(te instanceof IFluidPipe) {
//          // forward pressureized tag
//          if(res.tag == null) res.tag = new CompoundNBT();
//          res.tag.putBoolean("pressurized", true);
//        }
//      }
//      int n_filled = forward_fluid_handler().fill(res, doFill);
//      filling_ = false;
//      return n_filled;
//    }
//
//    @Override
//    @Nullable
//    public FluidStack drain(FluidStack resource, boolean doDrain)
//    { return null; }
//
//    @Override
//    @Nullable
//    public FluidStack drain(int maxDrain, boolean doDrain)
//    { return null; }
//
//    @Override
//    public IFluidTankProperties[] getTankProperties()
//    { return fluid_props_; }
//
//    // IFluidTankProperties --
//
//    @Override
//    @Nullable
//    public FluidStack getContents()
//    { return null; }
//
//    public int getCapacity()
//    { return 1000; }
//
//    @Override
//    public boolean canFill()
//    { return true; }
//
//    @Override
//    public boolean canDrain()
//    { return false; }
//
//    @Override
//    public boolean canFillFluidType(FluidStack fluidStack)
//    { return true; }
//
//    @Override
//    public boolean canDrainFluidType(FluidStack fluidStack)
//    { return false; }
//
//    // Back flow prevention handler --
//
//    private static class BackFlowHandler implements IFluidHandler, IFluidTankProperties
//    {
//      private final IFluidTankProperties[] props_ = {this};
//      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
//      @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
//      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain) { return null; }
//      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain) { return null; }
//      @Override @Nullable public FluidStack getContents() { return null; }
//      @Override public int getCapacity() { return 0; }
//      @Override public boolean canFill() { return false; }
//      @Override public boolean canDrain() { return false; }
//      @Override public boolean canFillFluidType(FluidStack fluidStack) { return false; }
//      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return false; }
//    }
//
//    // IFluidPipe
//
//    @Override
//    public boolean hasOutputConnection(Direction side)
//    { return (side == block_facing_); }
//
//    @Override
//    public boolean canOutputPressurized(boolean consumePower)
//    {
//      if(getlocked_ || (!filling_enabled_)) return false;
//      final TileEntity te = world.getTileEntity(pos.offset(block_facing_));
//      if(!(te instanceof IFluidPipe)) return false;
//      getlocked_ = true; // not sure if IE explicitly pre-detects loops, so let's lock recurion here, too.
//      boolean r = ((IFluidPipe)te).canOutputPressurized(consumePower);
//      getlocked_ = false;
//      return r;
//    }

  }
}
