/*
 * @file EdDoubleGateBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Gate blocks that can be one or two segments high.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.block.*;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.pathfinding.PathType;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class EdDoubleGateBlock extends DecorBlock.HorizontalWaterLoggable implements IDecorBlock
{
  public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 1);
  public static final BooleanProperty OPEN = FenceGateBlock.OPEN;
  public static final int SEGMENT_LOWER = 0;
  public static final int SEGMENT_UPPER = 1;
  protected final ArrayList<VoxelShape> collision_shapes_;

  public EdDoubleGateBlock(long config, AbstractBlock.Properties properties, AxisAlignedBB aabb)
  { this(config, properties, new AxisAlignedBB[]{aabb}); }

  public EdDoubleGateBlock(long config, AbstractBlock.Properties properties, AxisAlignedBB[] aabbs)
  {
    super(config, properties, aabbs);
    AxisAlignedBB[] caabbs = new AxisAlignedBB[aabbs.length];
    for(int i=0; i<caabbs.length; ++i) caabbs[i] = aabbs[i].expandTowards(0, 0.5, 0);
    collision_shapes_ = new ArrayList<VoxelShape>(Arrays.asList(
      VoxelShapes.block(),
      VoxelShapes.block(),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.NORTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.SOUTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.WEST, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.EAST, true)),
      VoxelShapes.block(),
      VoxelShapes.block()
    ));
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return state.getValue(OPEN) ? VoxelShapes.empty() : collision_shapes_.get(state.getValue(HORIZONTAL_FACING).get3DDataValue() & 0x7); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(SEGMENT).add(OPEN); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return getInitialState(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos()); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return getInitialState(super.updateShape(state, facing, facingState, world, pos, facingPos), world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if((rayTraceResult.getDirection()==Direction.UP) || (rayTraceResult.getDirection()==Direction.DOWN) && (player.getItemInHand(hand).getItem()==this.asItem())) return ActionResultType.PASS;
    if(world.isClientSide()) return ActionResultType.SUCCESS;
    final boolean open = !state.getValue(OPEN);
    world.setBlock(pos, state.setValue(OPEN, open),2|8|16);
    if(state.getValue(SEGMENT) == SEGMENT_UPPER) {
      final BlockState adjacent = world.getBlockState(pos.below());
      if(adjacent.getBlock()==this) world.setBlock(pos.below(), adjacent.setValue(OPEN, open), 2|8|16);
    } else {
      final BlockState adjacent = world.getBlockState(pos.above());
      if(adjacent.getBlock()==this) world.setBlock(pos.above(), adjacent.setValue(OPEN, open), 2|8|16);
    }
    world.playSound(null, pos, open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    return ActionResultType.CONSUME;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isPathfindable(BlockState state, IBlockReader world, BlockPos pos, PathType type)
  { return state.getValue(OPEN); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    if(world.isClientSide) return;
    boolean powered = false;
    BlockState adjacent;
    BlockPos adjacent_pos;
    if(state.getValue(SEGMENT) == SEGMENT_UPPER) {
      adjacent_pos = pos.below();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.getSignal(pos.above(), Direction.UP) > 0) {
        powered = true;
      } else if((adjacent!=null) && (world.hasNeighborSignal(pos.below(2)))) {
        powered = true;
      }
    } else {
      adjacent_pos = pos.above();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.hasNeighborSignal(pos)) {
        powered = true;
      } else if((adjacent!=null) && (world.getSignal(pos.above(2), Direction.UP) > 0)) {
        powered = true;
      }
    }
    boolean sound = false;
    if(powered != state.getValue(OPEN)) {
      world.setBlock(pos, state.setValue(OPEN, powered), 2|8|16);
      sound = true;
    }
    if((adjacent != null) && (powered != adjacent.getValue(OPEN))) {
      world.setBlock(adjacent_pos, adjacent.setValue(OPEN, powered), 2|8|16);
      sound = true;
    }
    if(sound) {
      world.playSound(null, pos, powered?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  private BlockState getInitialState(BlockState state, IWorld world, BlockPos pos)
  {
    final BlockState down = world.getBlockState(pos.below());
    if(down.getBlock() == this) return state.setValue(SEGMENT, SEGMENT_UPPER).setValue(OPEN, down.getValue(OPEN)).setValue(HORIZONTAL_FACING, down.getValue(HORIZONTAL_FACING));
    final BlockState up = world.getBlockState(pos.above());
    if(up.getBlock() == this) return state.setValue(SEGMENT, SEGMENT_LOWER).setValue(OPEN, up.getValue(OPEN)).setValue(HORIZONTAL_FACING, up.getValue(HORIZONTAL_FACING));
    return state.setValue(SEGMENT, SEGMENT_LOWER).setValue(OPEN, false);
  }

}
