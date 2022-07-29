/*
 * @file StandardBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 */
package wile.engineersdecor.libmc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


@SuppressWarnings("deprecation")
public class StandardBlocks
{
  public static final long CFG_DEFAULT                    = 0x0000000000000000L; // no special config
  public static final long CFG_CUTOUT                     = 0x0000000000000001L; // cutout rendering
  public static final long CFG_MIPPED                     = 0x0000000000000002L; // cutout mipped rendering
  public static final long CFG_TRANSLUCENT                = 0x0000000000000004L; // indicates a block/pane is glass like (transparent, etc)
  public static final long CFG_WATERLOGGABLE              = 0x0000000000000008L; // The derived block extends IWaterLoggable
  public static final long CFG_HORIZIONTAL                = 0x0000000000000010L; // horizontal block, affects bounding box calculation at construction time and placement
  public static final long CFG_LOOK_PLACEMENT             = 0x0000000000000020L; // placed in direction the player is looking when placing.
  public static final long CFG_FACING_PLACEMENT           = 0x0000000000000040L; // placed on the facing the player has clicked.
  public static final long CFG_OPPOSITE_PLACEMENT         = 0x0000000000000080L; // placed placed in the opposite direction of the face the player clicked.
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = 0x0000000000000100L; // placement direction flipped if an instance of the same class was clicked
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = 0x0000000000000200L; // placement direction flipped if player is sneaking
  public static final long CFG_STRICT_CONNECTIONS         = 0x0000000000000400L; // blocks do not connect to similar blocks around (implementation details may vary a bit)
  public static final long CFG_AI_PASSABLE                = 0x0000000000000800L; // does not block movement path for AI, needed for non-opaque blocks with collision shapes not thin at the bottom or one side.

  public interface IStandardBlock
  {
    default long config()
    { return 0; }

    default boolean hasDynamicDropList()
    { return false; }

    default List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
    { return Collections.singletonList((!world.isClientSide()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY)); }

    enum RenderTypeHint { SOLID,CUTOUT,CUTOUT_MIPPED,TRANSLUCENT,TRANSLUCENT_NO_CRUMBLING }

    default RenderTypeHint getRenderTypeHint()
    { return getRenderTypeHint(config()); }

    default RenderTypeHint getRenderTypeHint(long config)
    {
      if((config & CFG_CUTOUT)!=0) return RenderTypeHint.CUTOUT;
      if((config & CFG_MIPPED)!=0) return RenderTypeHint.CUTOUT_MIPPED;
      if((config & CFG_TRANSLUCENT)!=0) return RenderTypeHint.TRANSLUCENT;
      return RenderTypeHint.SOLID;
    }
  }

  public static class BaseBlock extends Block implements IStandardBlock, SimpleWaterloggedBlock
  {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public final long config;

    public BaseBlock(long conf, BlockBehaviour.Properties properties)
    {
      super(properties);
      config = conf;
      BlockState state = getStateDefinition().any();
      if((conf & CFG_WATERLOGGABLE)!=0) state = state.setValue(WATERLOGGED, false);
      registerDefaultState(state);
    }

    @Override
    public long config()
    { return config; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
    { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

    @Override
    public RenderTypeHint getRenderTypeHint()
    { return getRenderTypeHint(config); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type)
    { return ((config & CFG_AI_PASSABLE)!=0) && (super.isPathfindable(state, world, pos, type)); }

    public boolean hasSignalConnector(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side)
    { return state.isSignalSource(); }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving)
    {
      final boolean rsup = (state.hasBlockEntity() && (state.getBlock() != newState.getBlock()));
      super.onRemove(state, world, pos, newState, isMoving);
      if(rsup) world.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
    {
      final ServerLevel world = builder.getLevel();
      final Float explosion_radius = builder.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
      final BlockEntity te = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if((!hasDynamicDropList()) || (world==null)) return super.getDrops(state, builder);
      boolean is_explosion = (explosion_radius!=null) && (explosion_radius > 0);
      return dropList(state, world, te, is_explosion);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
    { return (((config & CFG_WATERLOGGABLE)==0) || (!state.getValue(WATERLOGGED))) && super.propagatesSkylightDown(state, reader, pos); }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state)
    { return (((config & CFG_WATERLOGGABLE)!=0) && state.getValue(WATERLOGGED)) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
    {
      if(((config & CFG_WATERLOGGABLE)!=0) && (state.getValue(WATERLOGGED))) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      return state;
    }

    @Override // SimpleWaterloggedBlock
    public boolean canPlaceLiquid(BlockGetter world, BlockPos pos, BlockState state, Fluid fluid)
    { return ((config & CFG_WATERLOGGABLE)!=0) && SimpleWaterloggedBlock.super.canPlaceLiquid(world, pos, state, fluid); }

    @Override // SimpleWaterloggedBlock
    public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState)
    { return ((config & CFG_WATERLOGGABLE)!=0) && SimpleWaterloggedBlock.super.placeLiquid(world, pos, state, fluidState); }

    @Override // SimpleWaterloggedBlock
    public ItemStack pickupBlock(LevelAccessor world, BlockPos pos, BlockState state)
    { return ((config & CFG_WATERLOGGABLE)!=0) ? (SimpleWaterloggedBlock.super.pickupBlock(world, pos, state)) : (ItemStack.EMPTY); }

    @Override // SimpleWaterloggedBlock
    public Optional<SoundEvent> getPickupSound()
    { return ((config & CFG_WATERLOGGABLE)!=0) ? (SimpleWaterloggedBlock.super.getPickupSound()) : Optional.empty(); }
  }

  public static class Cutout extends BaseBlock implements IStandardBlock
  {
    private final VoxelShape vshape;

    public Cutout(long conf, BlockBehaviour.Properties properties)
    { this(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

    public Cutout(long conf, BlockBehaviour.Properties properties, AABB aabb)
    { this(conf, properties, Shapes.create(aabb)); }

    public Cutout(long conf, BlockBehaviour.Properties properties, AABB[] aabbs)
    { this(conf, properties, Arrays.stream(aabbs).map(Shapes::create).reduce(Shapes.empty(), (shape, aabb)->Shapes.joinUnoptimized(shape, aabb, BooleanOp.OR))); }

    public Cutout(long conf, BlockBehaviour.Properties properties, VoxelShape voxel_shape)
    { super(conf, properties); vshape = voxel_shape; }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshape; }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos,  CollisionContext selectionContext)
    { return vshape; }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if((config & CFG_WATERLOGGABLE)!=0) {
        FluidState fs = context.getLevel().getFluidState(context.getClickedPos());
        state = state.setValue(WATERLOGGED,fs.getType()==Fluids.WATER);
      }
      return state;
    }

    @Override
    public boolean isPossibleToRespawnInThis()
    { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public PushReaction getPistonPushReaction(BlockState state)
    { return PushReaction.NORMAL; }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.getValue(WATERLOGGED)) return false;
      }
      return super.propagatesSkylightDown(state, reader, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
      }
      return super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.getValue(WATERLOGGED)) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }
      return state;
    }
  }

  public static class WaterLoggable extends Cutout implements IStandardBlock
  {
    public WaterLoggable(long config, BlockBehaviour.Properties properties)
    { super(config|CFG_WATERLOGGABLE, properties); }

    public WaterLoggable(long config, BlockBehaviour.Properties properties, AABB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    public WaterLoggable(long config, BlockBehaviour.Properties properties, VoxelShape voxel_shape)
    { super(config|CFG_WATERLOGGABLE, properties, voxel_shape);  }

    public WaterLoggable(long config, BlockBehaviour.Properties properties, AABB[] aabbs)
    { super(config|CFG_WATERLOGGABLE, properties, aabbs); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(WATERLOGGED); }
  }

  public static class Directed extends Cutout implements IStandardBlock
  {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    protected final Map<BlockState,VoxelShape> vshapes;

    public Directed(long config, BlockBehaviour.Properties properties, final Function<List<BlockState>, Map<BlockState,VoxelShape>> shape_supplier)
    {
      super(config, properties);
      registerDefaultState(super.defaultBlockState().setValue(FACING, Direction.UP));
      vshapes = shape_supplier.apply(getStateDefinition().getPossibleStates());
    }

    public Directed(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    {
      this(config, properties, (states)->{
        final Map<BlockState,VoxelShape> vshapes = new HashMap<>();
        final ArrayList<VoxelShape> indexed_shapes = shape_supplier.get();
        for(BlockState state:states) vshapes.put(state, indexed_shapes.get(state.getValue(FACING).get3DDataValue()));
        return vshapes;
      });
    }

    public Directed(long config, BlockBehaviour.Properties properties, final AABB[] unrotatedAABBs)
    {
      this(config, properties, (states)->{
        final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
        Map<BlockState,VoxelShape> vshapes = new HashMap<>();
        for(BlockState state:states) {
          vshapes.put(state, Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, state.getValue(FACING), is_horizontal)));
        }
        return vshapes;
      });
    }

    public Directed(long config, BlockBehaviour.Properties properties, final AABB unrotatedAABB)
    { this(config, properties, new AABB[]{unrotatedAABB}); }

    @Override
    public boolean isPossibleToRespawnInThis()
    { return false; }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
    { return false; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshapes.get(state); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return getShape(state, world, pos, selectionContext); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(FACING); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(state == null) return null;
      Direction facing = context.getClickedFace();
      if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) {
        // horizontal placement in direction the player is looking
        facing = context.getHorizontalDirection();
      } else if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL)) {
        // horizontal placement on a face
        if(((facing==Direction.UP)||(facing==Direction.DOWN))) return null;
      } else if((config & CFG_LOOK_PLACEMENT)!=0) {
        // placement in direction the player is looking, with up and down
        facing = context.getNearestLookingDirection();
      }
      if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer()!=null) &&  (context.getPlayer().isShiftKeyDown())) facing = facing.getOpposite();
      return state.setValue(FACING, facing);
    }
  }

  public static class AxisAligned extends Cutout implements IStandardBlock
  {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    protected final ArrayList<VoxelShape> vshapes;

    public AxisAligned(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    {
      super(config, properties);
      registerDefaultState(super.defaultBlockState().setValue(AXIS, Direction.Axis.X));
      vshapes = shape_supplier.get();
    }

    public AxisAligned(long config, BlockBehaviour.Properties properties, final AABB[] unrotatedAABBs)
    {
      this(config, properties, ()-> new ArrayList<>(Arrays.asList(
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.EAST, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.UP, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.SOUTH, false)),
        Shapes.block()
      )));
    }

    public AxisAligned(long config, BlockBehaviour.Properties properties, final AABB unrotatedAABB)
    { this(config, properties, new AABB[]{unrotatedAABB}); }

    @Override
    public boolean isPossibleToRespawnInThis()
    { return false; }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
    { return false; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshapes.get((state.getValue(AXIS)).ordinal() & 0x3); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return getShape(state, world, pos, selectionContext); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(AXIS); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      Direction facing;
      if((config & CFG_LOOK_PLACEMENT)!=0) {
        facing = context.getNearestLookingDirection();
      } else {
        facing = context.getClickedFace();
      }
      return super.getStateForPlacement(context).setValue(AXIS, facing.getAxis());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation)
    {
      switch(rotation) {
        case CLOCKWISE_90:
        case COUNTERCLOCKWISE_90:
          switch(state.getValue(AXIS)) {
            case X: return state.setValue(AXIS, Direction.Axis.Z);
            case Z: return state.setValue(AXIS, Direction.Axis.X);
          }
      }
      return state;
    }
  }

  public static class Horizontal extends Cutout implements IStandardBlock
  {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected final Map<BlockState,VoxelShape> vshapes;
    protected final Map<BlockState,VoxelShape> cshapes;

    public Horizontal(long config, BlockBehaviour.Properties properties, final Function<List<BlockState>, Map<BlockState,VoxelShape>> shape_supplier)
    {
      super(config|CFG_HORIZIONTAL, properties);
      registerDefaultState(super.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
      vshapes = shape_supplier.apply(getStateDefinition().getPossibleStates());
      cshapes = shape_supplier.apply(getStateDefinition().getPossibleStates());
    }

    public Horizontal(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    {
      this(config, properties, (states)->{
        final Map<BlockState,VoxelShape> vshapes = new HashMap<>();
        final ArrayList<VoxelShape> indexed_shapes = shape_supplier.get();
        for(BlockState state:states) vshapes.put(state, indexed_shapes.get(state.getValue(HORIZONTAL_FACING).get3DDataValue()));
        return vshapes;
      });
    }

    public Horizontal(long config, BlockBehaviour.Properties properties, final AABB unrotatedAABB)
    { this(config, properties, new AABB[]{unrotatedAABB}); }

    public Horizontal(long config, BlockBehaviour.Properties properties, final AABB[] unrotatedAABBs)
    {
      this(config, properties, (states)->{
        Map<BlockState,VoxelShape> vshapes = new HashMap<>();
        for(BlockState state:states) {
          vshapes.put(state, Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, state.getValue(HORIZONTAL_FACING), true)));
        }
        return vshapes;
      });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(HORIZONTAL_FACING); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
    { return vshapes.get(state); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return cshapes.get(state); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(state == null) return null;
      Direction facing = context.getClickedFace();
      if((config & CFG_LOOK_PLACEMENT) != 0) {
        // horizontal placement in direction the player is looking
        facing = context.getHorizontalDirection();
      } else {
        // horizontal placement on a face
        facing = ((facing==Direction.UP)||(facing==Direction.DOWN)) ? (context.getHorizontalDirection()) : facing;
      }
      if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer()!=null) && (context.getPlayer().isShiftKeyDown())) facing = facing.getOpposite();
      return state.setValue(HORIZONTAL_FACING, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot)
    { return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING))); }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn)
    { return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING))); }
  }

  public static class DirectedWaterLoggable extends Directed implements IStandardBlock
  {
    public DirectedWaterLoggable(long config, BlockBehaviour.Properties properties, AABB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    public DirectedWaterLoggable(long config, BlockBehaviour.Properties properties, AABB[] aabbs)
    { super(config|CFG_WATERLOGGABLE, properties, aabbs); }

    public DirectedWaterLoggable(long config, BlockBehaviour.Properties properties, final Function<List<BlockState>, Map<BlockState,VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE, properties, shape_supplier); }

    public DirectedWaterLoggable(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE, properties, shape_supplier); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(WATERLOGGED); }
  }

  public static class AxisAlignedWaterLoggable extends AxisAligned implements IStandardBlock
  {
    public AxisAlignedWaterLoggable(long config, BlockBehaviour.Properties properties, AABB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    public AxisAlignedWaterLoggable(long config, BlockBehaviour.Properties properties, AABB[] aabbs)
    { super(config|CFG_WATERLOGGABLE, properties, aabbs); }

    public AxisAlignedWaterLoggable(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE, properties, shape_supplier); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(WATERLOGGED); }
  }

  public static class HorizontalWaterLoggable extends Horizontal implements IStandardBlock
  {
    public HorizontalWaterLoggable(long config, BlockBehaviour.Properties properties, AABB aabb)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, aabb); }

    public HorizontalWaterLoggable(long config, BlockBehaviour.Properties properties, AABB[] aabbs)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, aabbs); }

    public HorizontalWaterLoggable(long config, BlockBehaviour.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, shape_supplier); }

    public HorizontalWaterLoggable(long config, BlockBehaviour.Properties properties, final Function<List<BlockState>, Map<BlockState,VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, shape_supplier); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(WATERLOGGED); }
  }

  static public class HorizontalFourWayWaterLoggable extends WaterLoggable implements IStandardBlock
  {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    protected final Map<BlockState, VoxelShape> shapes;
    protected final Map<BlockState, VoxelShape> collision_shapes;

    public HorizontalFourWayWaterLoggable(long config, BlockBehaviour.Properties properties, AABB base_aabb, final AABB[] side_aabb, int railing_height_extension)
    {
      super(config, properties, base_aabb);
      Map<BlockState, VoxelShape> build_shapes = new HashMap<>();
      Map<BlockState, VoxelShape> build_collision_shapes = new HashMap<>();
      for(BlockState state:getStateDefinition().getPossibleStates()) {
        {
          VoxelShape shape = ((base_aabb.getXsize()==0) || (base_aabb.getYsize()==0) || (base_aabb.getZsize()==0)) ? Shapes.empty() : Shapes.create(base_aabb);
          if(state.getValue(NORTH)) shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(side_aabb, Direction.NORTH, true)), BooleanOp.OR);
          if(state.getValue(EAST))  shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(side_aabb, Direction.EAST, true)), BooleanOp.OR);
          if(state.getValue(SOUTH)) shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(side_aabb, Direction.SOUTH, true)), BooleanOp.OR);
          if(state.getValue(WEST))  shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(side_aabb, Direction.WEST, true)), BooleanOp.OR);
          if(shape.isEmpty()) shape = Shapes.block();
          build_shapes.put(state.setValue(WATERLOGGED, false), shape);
          build_shapes.put(state.setValue(WATERLOGGED, true), shape);
        }
        {
          // how the hack to extend a shape, these are the above with y+4px.
          VoxelShape shape = ((base_aabb.getXsize()==0) || (base_aabb.getYsize()==0) || (base_aabb.getZsize()==0)) ? Shapes.empty() : Shapes.create(base_aabb);
          if(state.getValue(NORTH)) shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getMappedAABB(Auxiliaries.getRotatedAABB(side_aabb,
            Direction.NORTH, true), bb->bb.expandTowards(0, railing_height_extension, 0))), BooleanOp.OR);
          if(state.getValue(EAST))  shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getMappedAABB(Auxiliaries.getRotatedAABB(side_aabb,
            Direction.EAST, true), bb->bb.expandTowards(0, railing_height_extension, 0))), BooleanOp.OR);
          if(state.getValue(SOUTH)) shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getMappedAABB(Auxiliaries.getRotatedAABB(side_aabb,
            Direction.SOUTH, true), bb->bb.expandTowards(0, railing_height_extension, 0))), BooleanOp.OR);
          if(state.getValue(WEST))  shape = Shapes.joinUnoptimized(shape,Auxiliaries.getUnionShape(Auxiliaries.getMappedAABB(Auxiliaries.getRotatedAABB(side_aabb,
            Direction.WEST, true), bb->bb.expandTowards(0, railing_height_extension, 0))), BooleanOp.OR);
          if(shape.isEmpty()) shape = Shapes.block();
          build_collision_shapes.put(state.setValue(WATERLOGGED, false), shape);
          build_collision_shapes.put(state.setValue(WATERLOGGED, true), shape);
        }
      }
      shapes = build_shapes;
      collision_shapes = build_collision_shapes;
      registerDefaultState(super.defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false));
    }

    public HorizontalFourWayWaterLoggable(long config, BlockBehaviour.Properties properties, AABB base_aabb, final AABB side_aabb, int railing_height_extension)
    { this(config, properties, base_aabb, new AABB[]{side_aabb}, railing_height_extension); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(NORTH,EAST,SOUTH,WEST); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    { return super.getStateForPlacement(context).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    { return shapes.getOrDefault(state, Shapes.block()); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    { return collision_shapes.getOrDefault(state, Shapes.block()); }

    public static BooleanProperty getDirectionProperty(Direction face)
    {
      return switch (face) {
        case EAST -> HorizontalFourWayWaterLoggable.EAST;
        case SOUTH -> HorizontalFourWayWaterLoggable.SOUTH;
        case WEST -> HorizontalFourWayWaterLoggable.WEST;
        default -> HorizontalFourWayWaterLoggable.NORTH;
      };
    }
  }

}
