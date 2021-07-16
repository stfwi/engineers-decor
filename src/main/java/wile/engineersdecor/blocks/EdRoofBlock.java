/*
 * @file EdRoofBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Roof blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.StairsShape;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;


public class EdRoofBlock extends StandardBlocks.HorizontalWaterLoggable implements IDecorBlock
{
  public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
  public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
  private final VoxelShape[][][] shape_cache_;

  public EdRoofBlock(long config, AbstractBlock.Properties properties)
  { this(config, properties.dynamicShape(), VoxelShapes.empty(), VoxelShapes.empty()); }

  public EdRoofBlock(long config, AbstractBlock.Properties properties, VoxelShape add, VoxelShape cut)
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
  public float getShadeBrightness(BlockState state, IBlockReader world, BlockPos pos)
  { return 0.98f; }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightBlock(BlockState state, IBlockReader world, BlockPos pos)
  { return 1; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
  { return shape_cache_[state.getValue(HALF).ordinal()][state.getValue(HORIZONTAL_FACING).get3DDataValue()][state.getValue(SHAPE).ordinal()]; }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(SHAPE, HALF); }

  @Override
  public FluidState getFluidState(BlockState state)
  { return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }

  @Override
  public boolean isPathfindable(BlockState state, IBlockReader world, BlockPos pos, PathType type)
  { return false; }

  @Override
  public BlockState getStateForPlacement(BlockItemUseContext context)
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
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  {
    if(state.getValue(WATERLOGGED)) world.getLiquidTicks().scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
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
      switch(state.getValue(SHAPE)) {
        case INNER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
        case INNER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
        case OUTER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
        default:          return state.rotate(Rotation.CLOCKWISE_180);
      }
    } else if((where==Mirror.FRONT_BACK) && (state.getValue(HORIZONTAL_FACING).getAxis() == Direction.Axis.X)) {
      switch(state.getValue(SHAPE)) {
        case INNER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
        case INNER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
        case OUTER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
        case STRAIGHT:    return state.rotate(Rotation.CLOCKWISE_180);
      }
    }
    return super.mirror(state, where);
  }

  private static boolean isRoofBlock(BlockState state)
  { return (state.getBlock() instanceof EdRoofBlock); }

  private static boolean isOtherRoofState(BlockState state, IBlockReader world, BlockPos pos, Direction facing)
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
            if(!add.isEmpty()) shape = VoxelShapes.joinUnoptimized(shape, add, IBooleanFunction.OR);
            if(!cut.isEmpty()) shape = VoxelShapes.joinUnoptimized(shape, cut, IBooleanFunction.ONLY_FIRST);
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
    AxisAlignedBB[] straight = new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB( 0,  0, 0, 16,   4, 16),
      Auxiliaries.getPixeledAABB( 4,  4, 0, 16,   8, 16),
      Auxiliaries.getPixeledAABB( 8,  8, 0, 16,  12, 16),
      Auxiliaries.getPixeledAABB(12, 12, 0, 16,  16, 16)
    };
    AxisAlignedBB[] pyramid = new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB( 0,  0,  0, 16,   4, 16),
      Auxiliaries.getPixeledAABB( 4,  4,  4, 16,   8, 16),
      Auxiliaries.getPixeledAABB( 8,  8,  8, 16,  12, 16),
      Auxiliaries.getPixeledAABB(12, 12, 12, 16,  16, 16)
    };
    final Half half = Half.values()[half_index];
    if(half==Half.TOP) {
      straight = Auxiliaries.getMirroredAABB(straight, Axis.Y);
      pyramid = Auxiliaries.getMirroredAABB(pyramid, Axis.Y);
    }
    Direction direction = Direction.from3DDataValue(direction_index);
    if((direction==Direction.UP) || (direction==Direction.DOWN)) return VoxelShapes.block();
    direction_index = (direction.get2DDataValue()+1) & 0x03; // ref NORTH -> EAST for stairs compliancy.
    final StairsShape stairs = StairsShape.values()[stairs_shape_index];
    switch(stairs) {
      case STRAIGHT:
        return Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(straight, direction_index));
      case OUTER_LEFT:
        return Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(pyramid, direction_index-1));
      case OUTER_RIGHT:
        return Auxiliaries.getUnionShape(Auxiliaries.getYRotatedAABB(pyramid, direction_index));
      case INNER_LEFT:
        return Auxiliaries.getUnionShape(
          Auxiliaries.getYRotatedAABB(straight, direction_index),
          Auxiliaries.getYRotatedAABB(straight, direction_index-1)
        );
      case INNER_RIGHT:
        return Auxiliaries.getUnionShape(
          Auxiliaries.getYRotatedAABB(straight, direction_index),
          Auxiliaries.getYRotatedAABB(straight, direction_index+1)
        );
      default:
        return VoxelShapes.block();
    }
  }

  private static StairsShape getStairsShapeProperty(BlockState state, IBlockReader world, BlockPos pos)
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
