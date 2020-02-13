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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Direction;
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


public class BlockDecorPipeValve extends BlockDecor.DirectedWaterLoggable implements IDecorBlock
{
  public static final BooleanProperty RS_CN_N = BooleanProperty.create("rs_n");
  public static final BooleanProperty RS_CN_S = BooleanProperty.create("rs_s");
  public static final BooleanProperty RS_CN_E = BooleanProperty.create("rs_e");
  public static final BooleanProperty RS_CN_W = BooleanProperty.create("rs_w");
  public static final BooleanProperty RS_CN_U = BooleanProperty.create("rs_u");
  public static final BooleanProperty RS_CN_D = BooleanProperty.create("rs_d");

  public static final int CFG_CHECK_VALVE                = 0x0;
  public static final int CFG_ANALOG_VALVE               = 0x1;
  public static final int CFG_REDSTONE_CONTROLLED_VALVE  = 0x2;
  public final int valve_config;

  public static void on_config(int container_size_decl, int redstone_slope)
  {
    BTileEntity.fluid_maxflow_mb = MathHelper.clamp(container_size_decl, 1, 10000);
    BTileEntity.redstone_flow_slope_mb = MathHelper.clamp(redstone_slope, 1, 10000);
    ModEngineersDecor.logger().info("Config pipe valve: maxflow:" + BTileEntity.fluid_maxflow_mb + "mb, redstone amp:" + BTileEntity.redstone_flow_slope_mb + "mb/sig");
  }

  public BlockDecorPipeValve(long config, int valve_config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABB)
  { super(config, builder, unrotatedAABB); this.valve_config = valve_config; }

  private BlockState get_rsconnector_state(BlockState state, IWorld world, BlockPos pos, @Nullable BlockPos fromPos)
  {
    if((valve_config & (CFG_REDSTONE_CONTROLLED_VALVE))==0) return state;
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

  private void update_te(IWorld world, BlockState state, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te instanceof BlockDecorPipeValve.BTileEntity) ((BlockDecorPipeValve.BTileEntity)te).block_reconfigure(state.get(FACING), config, valve_config);
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
  {
    update_te(world, state, pos);
    return get_rsconnector_state(state, world, pos, null);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
  {
    update_te(world, state, pos);
    world.notifyNeighborsOfStateChange(pos,this);
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

  public static class BTileEntity extends TileEntity implements ICapabilityProvider //, IFluidPipe
  {
    protected static int fluid_maxflow_mb = 1000;
    protected static int redstone_flow_slope_mb = 1000/15;
    private Direction block_facing_ = null;
    private boolean filling_ = false;
    private boolean getlocked_ = false;
    private long block_config_ = 0;
    private int valve_config_;

    public BTileEntity()
    { this(ModContent.TET_STRAIGHT_PIPE_VALVE); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void block_reconfigure(Direction facing, long block_config, int valve_config)
    {
      block_facing_ = facing;
      block_config_ = block_config;
      valve_config_ = valve_config;
    }

    private Direction block_facing()
    {
      if(block_facing_ == null) {
        BlockState st = getWorld().getBlockState(getPos());
        block_facing_ = (st.getBlock() instanceof BlockDecorPipeValve) ? st.get(FACING) : Direction.NORTH;
      }
      return block_facing_;
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    {
      super.read(nbt);
      int i = nbt.getInt("facing");
      if((i>=0) || (i<6)) block_facing_ = Direction.byIndex(i);
      block_config_ = nbt.getLong("conf");
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
      super.write(nbt);
      if(block_facing_!=null) nbt.putInt("facing", block_facing_.getIndex());
      nbt.putLong("conf", block_config_);
      return nbt;
    }

    // ICapabilityProvider --------------------------------------------------------------------

    private LazyOptional<IFluidHandler> back_flow_handler_ = LazyOptional.of(() -> (IFluidHandler)new BackFlowHandler());
    private LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)new MainFlowHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(!this.removed && (facing != null)) {
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
          if(facing == block_facing()) return back_flow_handler_.cast();
          if(facing == block_facing().getOpposite()) return fluid_handler_.cast();
        }
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
      private  BTileEntity te;
      public MainFlowHandler(BTileEntity te)  { this.te = te; }
      @Override public int getTanks() { return 0; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return fluid_maxflow_mb; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return true; }

      @Override public int fill(FluidStack resource, FluidAction action)
      {
        if(te.filling_) return 0;
        final IFluidHandler fh = te.forward_fluid_handler();
        if(fh==null) return 0;
        if((te.valve_config_ & CFG_REDSTONE_CONTROLLED_VALVE) != 0) {
          int rs = te.world.getRedstonePowerFromNeighbors(te.pos);
          if(rs <= 0) return 0;
          if(((te.valve_config_ & CFG_ANALOG_VALVE) != 0) && (rs < 15)) resource.setAmount(MathHelper.clamp(rs * redstone_flow_slope_mb, 1, resource.getAmount()));
        }
        FluidStack res = resource.copy();
        if(res.getAmount() > fluid_maxflow_mb) res.setAmount(fluid_maxflow_mb);
        te.filling_ = true;
        // IE fluid pipe not available yet
        //  if(res.getAmount() > 50) {
        //    final TileEntity fte = te.world.getTileEntity(te.pos.offset(te.block_facing()));
        //    if(!(fte instanceof IFluidPipe)) {
        //      CompoundNBT tag = res.getTag();
        //      if((tag != null) && (tag.contains("pressurized"))) tag.remove("pressurized"); // remove pressureized tag if no IFluidPipe
        //    }
        //  }
        int n_filled = fh.fill(res, action);
        te.filling_ = false;
        return n_filled;
      }
    }

    // Back flow prevention handler --

    private static class BackFlowHandler implements IFluidHandler
    {
      @Override public int getTanks() { return 0; }
      @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
      @Override public int getTankCapacity(int tank) { return 0; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return false; }
      @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    // IE IFluidPipe

    //    @Override
    //    public boolean hasOutputConnection(Direction side)
    //    { return (side == block_facing()); }
    //
    //    @Override
    //    public boolean canOutputPressurized(boolean consumePower)
    //    {
    //      if(getlocked_ || (!filling_enabled_)) return false;
    //      final TileEntity te = world.getTileEntity(pos.offset(block_facing()));
    //      if(!(te instanceof IFluidPipe)) return false;
    //      getlocked_ = true; // not sure if IE explicitly pre-detects loops, so let's lock recurion here, too.
    //      boolean r = ((IFluidPipe)te).canOutputPressurized(consumePower);
    //      getlocked_ = false;
    //      return r;
    //    }

  }
}
