/*
 * @file EdPipeValve.java
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
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EdPipeValve
{
  public static final int CFG_CHECK_VALVE                = 0x1;
  public static final int CFG_ANALOG_VALVE               = 0x2;
  public static final int CFG_REDSTONE_CONTROLLED_VALVE  = 0x4;

  public static void on_config(int container_size_decl, int redstone_slope)
  {
    PipeValveTileEntity.fluid_maxflow_mb = MathHelper.clamp(container_size_decl, 1, 10000);
    PipeValveTileEntity.redstone_flow_slope_mb = MathHelper.clamp(redstone_slope, 1, 10000);
    ModEngineersDecor.logger().info("Config pipe valve: maxflow:" + PipeValveTileEntity.fluid_maxflow_mb + "mb, redstone amp:" + PipeValveTileEntity.redstone_flow_slope_mb + "mb/sig");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class PipeValveBlock extends DecorBlock.DirectedWaterLoggable implements IDecorBlock
  {
    public static final BooleanProperty RS_CN_N = BooleanProperty.create("rs_n");
    public static final BooleanProperty RS_CN_S = BooleanProperty.create("rs_s");
    public static final BooleanProperty RS_CN_E = BooleanProperty.create("rs_e");
    public static final BooleanProperty RS_CN_W = BooleanProperty.create("rs_w");
    public static final BooleanProperty RS_CN_U = BooleanProperty.create("rs_u");
    public static final BooleanProperty RS_CN_D = BooleanProperty.create("rs_d");

    public final int valve_config;

    public static void on_config(int container_size_decl, int redstone_slope)
    {
      PipeValveTileEntity.fluid_maxflow_mb = MathHelper.clamp(container_size_decl, 1, 10000);
      PipeValveTileEntity.redstone_flow_slope_mb = MathHelper.clamp(redstone_slope, 1, 10000);
      ModEngineersDecor.logger().info("Config pipe valve: maxflow:" + PipeValveTileEntity.fluid_maxflow_mb + "mb, redstone amp:" + PipeValveTileEntity.redstone_flow_slope_mb + "mb/sig");
    }

    public PipeValveBlock(long config, int valve_config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); this.valve_config = valve_config; }

    private BlockState get_rsconnector_state(BlockState state, IWorld world, BlockPos pos, @Nullable BlockPos fromPos)
    {
      if((valve_config & (CFG_REDSTONE_CONTROLLED_VALVE))==0) return state;
      Direction.Axis bfa = state.get(FACING).getAxis();
      for(Direction f:Direction.values()) {
        boolean cn = (f.getAxis() != bfa);
        if(cn) {
          BlockPos nbp = pos.offset(f);
          if((fromPos != null) && (!nbp.equals(fromPos))) continue; // do not change connectors except form the frompos.
          BlockState nbs = world.getBlockState(nbp);
          if((nbs.getBlock() instanceof PipeValveBlock) || ((!nbs.canProvidePower()) && (!nbs.canConnectRedstone(world, nbp, f.getOpposite())))) cn = false;
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

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.fullCube(); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(RS_CN_N, RS_CN_S, RS_CN_E, RS_CN_W, RS_CN_U, RS_CN_D); }

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
    { world.notifyNeighborsOfStateChange(pos,this); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new PipeValveTileEntity(); }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction)
    { return get_rsconnector_state(state, world, pos, null); } // don't rotate at all

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
    { return (side!=null) && (side!=state.get(FACING)) && (side!=state.get(FACING).getOpposite()); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canProvidePower(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class PipeValveTileEntity extends TileEntity implements ICapabilityProvider //, IFluidPipe
  {
    protected static int fluid_maxflow_mb = 1000;
    protected static int redstone_flow_slope_mb = 1000/15;
    private Direction block_facing_ = null;
    private boolean filling_ = false;
    private int valve_config_;

    public PipeValveTileEntity()
    { this(ModContent.TET_STRAIGHT_PIPE_VALVE); }

    public PipeValveTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    private Direction block_facing()
    {
      BlockState st = getWorld().getBlockState(getPos());
      return (st.getBlock() instanceof PipeValveBlock) ? st.get(PipeValveBlock.FACING) : Direction.NORTH;
    }

    private long valve_config()
    {
      if(valve_config_ <= 0) {
        final Block block = getWorld().getBlockState(getPos()).getBlock();
        if(block instanceof PipeValveBlock) valve_config_ = ((PipeValveBlock)block).valve_config;
      }
      return valve_config_;
    }

    // TileEntity -----------------------------------------------------------------------------

    @Override
    public void remove()
    {
      super.remove();
      back_flow_handler_.invalidate();
      fluid_handler_.invalidate();
    }

    // ICapabilityProvider --------------------------------------------------------------------

    private LazyOptional<IFluidHandler> back_flow_handler_ = LazyOptional.of(() -> (IFluidHandler)new BackFlowHandler());
    private LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)new MainFlowHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
        Direction bf = block_facing();
        if(facing == bf) return back_flow_handler_.cast();
        if(facing == bf.getOpposite()) return fluid_handler_.cast();
        return LazyOptional.empty();
      }
      return super.getCapability(capability, facing);
    }

    // IFluidHandlers

    @Nullable
    private IFluidHandler forward_fluid_handler()
    {
      final TileEntity te = world.getTileEntity(pos.offset(block_facing()));
      if(te == null) return null;
      return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing().getOpposite()).orElse(null);
    }

    // Forward flow handler --

    private static class MainFlowHandler implements IFluidHandler
    {
      private PipeValveTileEntity te;
      public MainFlowHandler(PipeValveTileEntity te)  { this.te = te; }
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return fluid_maxflow_mb; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return true; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }

      @Override public int fill(FluidStack resource, FluidAction action)
      {
        if(te.filling_) return 0;
        final IFluidHandler fh = te.forward_fluid_handler();
        if(fh==null) return 0;
        FluidStack res = resource.copy();
        if((te.valve_config() & CFG_REDSTONE_CONTROLLED_VALVE) != 0) {
          int rs = te.world.getRedstonePowerFromNeighbors(te.pos);
          if(rs <= 0) return 0;
          if(((te.valve_config() & CFG_ANALOG_VALVE) != 0) && (rs < 15)) res.setAmount(MathHelper.clamp(rs * redstone_flow_slope_mb, 1, res.getAmount()));
        }
        if(res.getAmount() > fluid_maxflow_mb) res.setAmount(fluid_maxflow_mb);
        te.filling_ = true;
        int n_filled = fh.fill(res, action);
        te.filling_ = false;
        return n_filled;
      }
    }

    // Back flow prevention handler --

    private static class BackFlowHandler implements IFluidHandler
    {
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return 0; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return false; }
      @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }
  }
}
