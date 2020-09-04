/*
 * @file EdRoofBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
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
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;


public class EdRoofBlock extends StandardBlocks.HorizontalWaterLoggable implements IDecorBlock
{
  public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
  public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
  private final VoxelShape[][][] shape_cache_;

  public EdRoofBlock(long config, Block.Properties properties)
  { this(config, properties, VoxelShapes.empty(), VoxelShapes.empty()); }

  public EdRoofBlock(long config, Block.Properties properties, VoxelShape add, VoxelShape cut)
  {
    super(config, properties, Auxiliaries.getPixeledAABB(0, 0,0,16, 8, 16));
    setDefaultState(stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH).with(SHAPE, StairsShape.STRAIGHT).with(WATERLOGGED, false));
    shape_cache_ = makeShapes(add, cut);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isTransparent(BlockState state)
  { return true; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
  { return shape_cache_[state.get(HALF).ordinal()][state.get(HORIZONTAL_FACING).getIndex()][state.get(SHAPE).ordinal()]; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(SHAPE, HALF); }

  @Override
  public FluidState getFluidState(BlockState state)
  { return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state); }

  @Override
  public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type)
  { return false; }

  @Override
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    BlockPos pos = context.getPos();
    Direction face = context.getFace();
    BlockState state = getDefaultState()
      .with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing())
      .with(HALF, (face == Direction.DOWN) ? Half.TOP : Half.BOTTOM)
      .with(WATERLOGGED, context.getWorld().getFluidState(pos).getFluid()==Fluids.WATER);
    return state.with(SHAPE, getStairsShapeProperty(state, context.getWorld(), pos));
  }

  @Override
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  {
    if(state.get(WATERLOGGED)) world.getPendingFluidTicks().scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    return (facing.getAxis().isHorizontal()) ? (state.with(SHAPE, getStairsShapeProperty(state, world, pos))) : (super.updatePostPlacement(state, facing, facingState, world, pos, facingPos));
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rot)
  { return state.with(HORIZONTAL_FACING, rot.rotate(state.get(HORIZONTAL_FACING))); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror where)
  {
    if((where==Mirror.LEFT_RIGHT) && (state.get(HORIZONTAL_FACING).getAxis()==Direction.Axis.Z)) {
      switch(state.get(SHAPE)) {
        case INNER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.INNER_RIGHT);
        case INNER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.INNER_LEFT);
        case OUTER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.OUTER_LEFT);
        default:          return state.rotate(Rotation.CLOCKWISE_180);
      }
    } else if((where==Mirror.FRONT_BACK) && (state.get(HORIZONTAL_FACING).getAxis() == Direction.Axis.X)) {
      switch(state.get(SHAPE)) {
        case INNER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.INNER_LEFT);
        case INNER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.INNER_RIGHT);
        case OUTER_LEFT:  return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.OUTER_RIGHT);
        case OUTER_RIGHT: return state.rotate(Rotation.CLOCKWISE_180).with(SHAPE, StairsShape.OUTER_LEFT);
        case STRAIGHT:    return state.rotate(Rotation.CLOCKWISE_180);
      }
    }
    return super.mirror(state, where);
  }

  private static boolean isRoofBlock(BlockState state)
  { return (state.getBlock() instanceof EdRoofBlock); }

  private static boolean isOtherRoofState(BlockState state, IBlockReader world, BlockPos pos, Direction facing)
  {
    BlockState st = world.getBlockState(pos.offset(facing));
    return (!isRoofBlock(st)) || (st.get(HORIZONTAL_FACING) != state.get(HORIZONTAL_FACING));
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
            if(!add.isEmpty()) shape = VoxelShapes.combine(shape, add, IBooleanFunction.OR);
            if(!cut.isEmpty()) shape = VoxelShapes.combine(shape, cut, IBooleanFunction.ONLY_FIRST);
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
    Direction direction = Direction.byIndex(direction_index);
    if((direction==Direction.UP) || (direction==Direction.DOWN)) return VoxelShapes.fullCube();
    direction_index = (direction.getHorizontalIndex()+1) & 0x03; // ref NORTH -> EAST for stairs compliancy.
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
        return VoxelShapes.fullCube();
    }
  }

  private static StairsShape getStairsShapeProperty(BlockState state, IBlockReader world, BlockPos pos)
  {
    Direction direction = state.get(HORIZONTAL_FACING);
    {
      BlockState ns = world.getBlockState(pos.offset(direction));
      if(isRoofBlock(ns) && (state.get(HALF) == ns.get(HALF))) {
        Direction nf = ns.get(HORIZONTAL_FACING);
        if(nf.getAxis() != state.get(HORIZONTAL_FACING).getAxis() && isOtherRoofState(state, world, pos, nf.getOpposite())) {
          return (nf == direction.rotateYCCW()) ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
      }
    }
    {
      BlockState ns = world.getBlockState(pos.offset(direction.getOpposite()));
      if(isRoofBlock(ns) && (state.get(HALF) == ns.get(HALF))) {
        Direction nf = ns.get(HORIZONTAL_FACING);
        if(nf.getAxis() != state.get(HORIZONTAL_FACING).getAxis() && isOtherRoofState(state, world, pos, nf)) {
          return (nf == direction.rotateYCCW()) ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
      }
    }
    return StairsShape.STRAIGHT;
  }
}
