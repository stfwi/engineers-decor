/*
 * @file EdPipeValve.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basically a piece of pipe that does not connect to
 * pipes on the side, and conducts fluids only in one way.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.RsSignals;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class EdPipeValve
{
  public static final int CFG_CHECK_VALVE                = 0x1;
  public static final int CFG_ANALOG_VALVE               = 0x2;
  public static final int CFG_REDSTONE_CONTROLLED_VALVE  = 0x4;

  public static void on_config(int container_size_decl, int redstone_slope)
  {
    PipeValveTileEntity.fluid_maxflow_mb = Mth.clamp(container_size_decl, 1, 10000);
    PipeValveTileEntity.redstone_flow_slope_mb = Mth.clamp(redstone_slope, 1, 10000);
    ModConfig.log("Config pipe valve: maxflow:" + PipeValveTileEntity.fluid_maxflow_mb + "mb, redstone amp:" + PipeValveTileEntity.redstone_flow_slope_mb + "mb/sig.");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class PipeValveBlock extends StandardBlocks.DirectedWaterLoggable implements StandardEntityBlocks.IStandardEntityBlock<PipeValveTileEntity>
  {
    public static final BooleanProperty RS_CN_N = BooleanProperty.create("rs_n");
    public static final BooleanProperty RS_CN_S = BooleanProperty.create("rs_s");
    public static final BooleanProperty RS_CN_E = BooleanProperty.create("rs_e");
    public static final BooleanProperty RS_CN_W = BooleanProperty.create("rs_w");
    public static final BooleanProperty RS_CN_U = BooleanProperty.create("rs_u");
    public static final BooleanProperty RS_CN_D = BooleanProperty.create("rs_d");
    public final int valve_config;

    public PipeValveBlock(long config, int valve_config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); this.valve_config = valve_config; }

    @Override
    @Nullable
    public BlockEntityType<EdPipeValve.PipeValveTileEntity> getBlockEntityType()
    { return ModContent.TET_STRAIGHT_PIPE_VALVE; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return Shapes.block(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(RS_CN_N, RS_CN_S, RS_CN_E, RS_CN_W, RS_CN_U, RS_CN_D); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      return super.getStateForPlacement(context).setValue(RS_CN_N, false).setValue(RS_CN_S, false).setValue(RS_CN_E, false)
                  .setValue(RS_CN_W, false).setValue(RS_CN_U, false).setValue(RS_CN_D, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
    { return get_rsconnector_state(state, world, pos, null); }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    { world.updateNeighborsAt(pos,this); }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction)
    { return get_rsconnector_state(state, world, pos, null); } // don't rotate at all

    @Override
    public boolean hasSignalConnector(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side)
    { return (side!=null) && (side!=state.getValue(FACING)) && (side!=state.getValue(FACING).getOpposite()); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    @SuppressWarnings("deprecation") // public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) { return true; }
    public boolean isSignalSource(BlockState p_60571_)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
    { return 0; }

    private BlockState get_rsconnector_state(BlockState state, LevelAccessor world, BlockPos pos, @Nullable BlockPos fromPos)
    {
      if((valve_config & (CFG_REDSTONE_CONTROLLED_VALVE))==0) return state;
      Direction.Axis bfa = state.getValue(FACING).getAxis();
      for(Direction f:Direction.values()) {
        boolean cn = (f.getAxis() != bfa);
        if(cn) {
          BlockPos nbp = pos.relative(f);
          if((fromPos != null) && (!nbp.equals(fromPos))) continue; // do not change connectors except form the frompos.
          BlockState nbs = world.getBlockState(nbp);
          if((nbs.getBlock() instanceof PipeValveBlock) || (!nbs.isSignalSource()) && (RsSignals.hasSignalConnector(nbs, world, nbp, f.getOpposite()))) cn = false;
        }
        switch (f) {
          case NORTH -> state = state.setValue(RS_CN_N, cn);
          case SOUTH -> state = state.setValue(RS_CN_S, cn);
          case EAST -> state = state.setValue(RS_CN_E, cn);
          case WEST -> state = state.setValue(RS_CN_W, cn);
          case UP -> state = state.setValue(RS_CN_U, cn);
          case DOWN -> state = state.setValue(RS_CN_D, cn);
        }
      }
      return state;
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class PipeValveTileEntity extends StandardEntityBlocks.StandardBlockEntity
  {
    protected static int fluid_maxflow_mb = 1000;
    protected static int redstone_flow_slope_mb = 1000/15;
    private final Direction block_facing_ = null;
    private boolean filling_ = false;
    private int valve_config_;

    public PipeValveTileEntity(BlockPos pos, BlockState state)
    { super(ModContent.TET_STRAIGHT_PIPE_VALVE, pos, state); }

    private Direction block_facing()
    {
      BlockState st = getLevel().getBlockState(getBlockPos());
      return (st.getBlock() instanceof PipeValveBlock) ? st.getValue(PipeValveBlock.FACING) : Direction.NORTH;
    }

    private long valve_config()
    {
      if(valve_config_ <= 0) {
        final Block block = getLevel().getBlockState(getBlockPos()).getBlock();
        if(block instanceof PipeValveBlock) valve_config_ = ((PipeValveBlock)block).valve_config;
      }
      return valve_config_;
    }

    // BlockEntity -----------------------------------------------------------------------------

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      back_flow_handler_.invalidate();
      fluid_handler_.invalidate();
    }

    // ICapabilityProvider --------------------------------------------------------------------

    private final LazyOptional<IFluidHandler> back_flow_handler_ = LazyOptional.of(BackFlowHandler::new);
    private final LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> new MainFlowHandler(this));

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
      final BlockEntity te = level.getBlockEntity(worldPosition.relative(block_facing()));
      if(te == null) return null;
      return te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, block_facing().getOpposite()).orElse(null);
    }

    // Forward flow handler --

    private static class MainFlowHandler implements IFluidHandler
    {
      private final PipeValveTileEntity te;
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
          int rs = te.level.getBestNeighborSignal(te.worldPosition);
          if(rs <= 0) return 0;
          if(((te.valve_config() & CFG_ANALOG_VALVE) != 0) && (rs < 15)) res.setAmount(Mth.clamp(rs * redstone_flow_slope_mb, 1, res.getAmount()));
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
