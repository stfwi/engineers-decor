/*
 * @file EdRoofBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Roof blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;


public class EdRoofBlock extends StandardBlocks.HorizontalWaterLoggable
{
  public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
  public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
  private final VoxelShape[][][] shape_cache_;

  public EdRoofBlock(long config, BlockBehaviour.Properties properties)
  { this(config, properties.dynamicShape(), Shapes.empty(), Shapes.empty()); }

  public EdRoofBlock(long config, BlockBehaviour.Properties properties, VoxelShape add, VoxelShape cut)
  {
    super(config, properties, Auxiliaries.getPixeledAABB(0, 0,0,16, 8, 16));
    registerDefaultState(super.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT));
    shape_cache_ = makeShapes(add, cut);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean useShapeForLightOcclusion(BlockState state)
  { return false; }

  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos)
  { return 0.98f; }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos)
  { return 1; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
  { return shape_cache_[state.getValue(HALF).ordinal()][state.getValue(HORIZONTAL_FACING).get3DDataValue()][state.getValue(SHAPE).ordinal()]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(SHAPE, HALF); }

  @Override
  public FluidState getFluidState(BlockState state)
  { return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }

  @Override
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type)
  { return false; }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    BlockPos pos = context.getClickedPos();
    Direction face = context.getClickedFace();
    BlockState state = defaultBlockState()
      .setValue(HORIZONTAL_FACING, context.getHorizontalDirection())
      .setValue(HALF, (face == Direction.DOWN) ? Half.TOP : Half.BOTTOM)
      .setValue(WATERLOGGED, context.getLevel().getFluidState(pos).getType()==Fluids.WATER);
    return state.setValue(SHAPE, getStairsShapeProperty(state, context.getLevel(), pos));
  }

  @Override
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  {
    if(state.getValue(WATERLOGGED)) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    return (facing.getAxis().isHorizontal()) ? (state.setValue(SHAPE, getStairsShapeProperty(state, world, pos))) : (super.updateShape(state, facing, facingState, world, pos, facingPos));
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rot)
  { return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING))); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror where)
  {
    if((where==Mirror.LEFT_RIGHT) && (state.getValue(HORIZONTAL_FACING).getAxis()==Direction.Axis.Z)) {
      return switch (state.getValue(SHAPE)) {
        case INNER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
        case INNER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
        case OUTER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
        default -> state.rotate(Rotation.CLOCKWISE_180);
      };
    } else if((where==Mirror.FRONT_BACK) && (state.getValue(HORIZONTAL_FACING).getAxis() == Direction.Axis.X)) {
      return switch (state.getValue(SHAPE)) {
        case INNER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
        case INNER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
        case OUTER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
        case STRAIGHT -> state.rotate(Rotation.CLOCKWISE_180);
      };
    }
    return super.mirror(state, where);
  }

  private static boolean isRoofBlock(BlockState state)
  { return (state.getBlock() instanceof EdRoofBlock); }

  private static boolean isOtherRoofState(BlockState state, BlockGetter world, BlockPos pos, Direction facing)
  {
    BlockState st = world.getBlockState(pos.relative(facing));
    return (!isRoofBlock(st)) || (st.getValue(HORIZONTAL_FACING) != state.getValue(HORIZONTAL_FACING));
  }

  private static VoxelShape[][][] makeShapes(VoxelShape add, VoxelShape cut)
  {
    VoxelShape[][][] shapes = new VoxelShape[2][6][5];
    for(int half_index=0; half_index<Half.values().length; ++half_index) {
      for(int direction_index=0; direction_index<Direction.values().length; ++direction_index) {
        for(int stairs_shape_index=0; stairs_shape_index<StairsShape.values().length; ++stairs_shape_index) {
          VoxelShape shape = makeShape(half_index, direction_index, stairs_shape_index);
          try {
            // Only in case something changes and this fails, log but do not prevent the game from starting.
            // Roof shapes are not the most important thing in the world.
            if(!add.isEmpty()) shape = Shapes.joinUnoptimized(shape, add, BooleanOp.OR);
            if(!cut.isEmpty()) shape = Shapes.joinUnoptimized(shape, cut, BooleanOp.ONLY_FIRST);
          } catch(Throwable ex) {
            Auxiliaries.logError("Failed to cut shape using Boolean function. This is bug.");
          }
          shapes[half_index][direction_index][stairs_shape_index] = shape;
        }
      }
    }
    return shapes;
  }

  private static VoxelShape makeShape(int half_index, int direction_index, int stairs_shape_index)
  {
    AABB[] straight = new AABB[]{
      Auxiliaries.getPixeledAABB( 0,  0, 0, 16,   4, 16),
      Auxiliaries.getPixeledAABB( 4,  4, 0, 16,   8, 16),
      Auxiliaries.getPixeledAABB( 8,  8, 0, 16,  12, 16),
      Auxiliaries.getPixeledAABB(12, 12, 0, 16,  16, 16)
    };
    AABB[] pyramid = new AABB[]{
      Auxiliaries.getPixeledAABB( 0,  0,  0, 16,   4, 16),
      Auxiliaries.getPixeledAABB( 4,  4,  4, 16,   8, 16),
      Auxiliaries.getPixeledAABB( 8,  8,  8, 16,  12, 16),
      Auxiliaries.getPixeledAABB(12, 12, 12, 16,  16, 16)
    };
    final Half half = Half.values()[half_index];
    if(half==Half.TOP) {
      straight = Auxiliaries.getMirroredAABB(straight, Direction.Axis.Y);
      pyramid = Auxiliaries.getMirroredAABB(pyramid, Direction.Axis.Y);
    }
    Direction direction = Direction.from3DDataValue(direction_index);
    if((direction==Direction.UP) || (direction==Direction.DOWN)) return Shapes.block();
    direction_index = (direction.get2DDataValue()+1) & 0x03; // ref NORTH -> EAST for stairs compliancy.
    final StairsShape stairs = StairsShape.values()[stairs_shape_index];
    return switch (stairs) {
      case STRAIGHT -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(straight, direction_index));
      case OUTER_LEFT -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(pyramid, direction_index - 1));
      case OUTER_RIGHT -> Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(pyramid, direction_index));
      case INNER_LEFT -> Auxiliaries.getUnionShape(
        Auxiliaries.getYRotatedAABB(straight, direction_index),
        Auxiliaries.getYRotatedAABB(straight, direction_index - 1)
      );
      case INNER_RIGHT -> Auxiliaries.getUnionShape(
        Auxiliaries.getYRotatedAABB(straight, direction_index),
        Auxiliaries.getYRotatedAABB(straight, direction_index + 1)
      );
    };
  }

  private static StairsShape getStairsShapeProperty(BlockState state, BlockGetter world, BlockPos pos)
  {
    Direction direction = state.getValue(HORIZONTAL_FACING);
    {
      BlockState ns = world.getBlockState(pos.relative(direction));
      if(isRoofBlock(ns) && (state.getValue(HALF) == ns.getValue(HALF))) {
        Direction nf = ns.getValue(HORIZONTAL_FACING);
        if(nf.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis() && isOtherRoofState(state, world, pos, nf.getOpposite())) {
          return (nf == direction.getCounterClockWise()) ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
      }
    }
    {
      BlockState ns = world.getBlockState(pos.relative(direction.getOpposite()));
      if(isRoofBlock(ns) && (state.getValue(HALF) == ns.getValue(HALF))) {
        Direction nf = ns.getValue(HORIZONTAL_FACING);
        if(nf.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis() && isOtherRoofState(state, world, pos, nf)) {
          return (nf == direction.getCounterClockWise()) ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
      }
    }
    return StairsShape.STRAIGHT;
  }
}
