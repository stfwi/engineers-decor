/*
 * @file EdDoubleGateBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Gate blocks that can be one or two segments high.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.engineersdecor.libmc.StandardBlocks;
import wile.engineersdecor.libmc.Auxiliaries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class EdDoubleGateBlock extends StandardBlocks.HorizontalWaterLoggable
{
  public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 1);
  public static final BooleanProperty OPEN = FenceGateBlock.OPEN;
  public static final int SEGMENT_LOWER = 0;
  public static final int SEGMENT_UPPER = 1;
  protected final ArrayList<VoxelShape> collision_shapes_;

  public EdDoubleGateBlock(long config, BlockBehaviour.Properties properties, AABB aabb)
  { this(config, properties, new AABB[]{aabb}); }

  public EdDoubleGateBlock(long config, BlockBehaviour.Properties properties, AABB[] aabbs)
  {
    super(config, properties, aabbs);
    AABB[] caabbs = new AABB[aabbs.length];
    for(int i=0; i<caabbs.length; ++i) caabbs[i] = aabbs[i].expandTowards(0, 0.5, 0);
    collision_shapes_ = new ArrayList<>(Arrays.asList(
      Shapes.block(),
      Shapes.block(),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.NORTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.SOUTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.WEST, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.EAST, true)),
      Shapes.block(),
      Shapes.block()
    ));
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return state.getValue(OPEN) ? Shapes.empty() : collision_shapes_.get(state.getValue(HORIZONTAL_FACING).get3DDataValue() & 0x7); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(SEGMENT).add(OPEN); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  { return getInitialState(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos()); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  { return getInitialState(super.updateShape(state, facing, facingState, world, pos, facingPos), world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
  {
    if((rayTraceResult.getDirection()==Direction.UP) || (rayTraceResult.getDirection()==Direction.DOWN) && (player.getItemInHand(hand).getItem()==this.asItem())) return InteractionResult.PASS;
    if(world.isClientSide()) return InteractionResult.SUCCESS;
    final boolean open = !state.getValue(OPEN);
    world.setBlock(pos, state.setValue(OPEN, open),2|8|16);
    if(state.getValue(SEGMENT) == SEGMENT_UPPER) {
      final BlockState adjacent = world.getBlockState(pos.below());
      if(adjacent.getBlock()==this) world.setBlock(pos.below(), adjacent.setValue(OPEN, open), 2|8|16);
    } else {
      final BlockState adjacent = world.getBlockState(pos.above());
      if(adjacent.getBlock()==this) world.setBlock(pos.above(), adjacent.setValue(OPEN, open), 2|8|16);
    }
    world.playSound(null, pos, open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.4f);
    return InteractionResult.CONSUME;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type)
  { return state.getValue(OPEN); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
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
      world.playSound(null, pos, powered?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.4f);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  private BlockState getInitialState(BlockState state, LevelAccessor world, BlockPos pos)
  {
    final BlockState down = world.getBlockState(pos.below());
    if(down.getBlock() == this) return state.setValue(SEGMENT, SEGMENT_UPPER).setValue(OPEN, down.getValue(OPEN)).setValue(HORIZONTAL_FACING, down.getValue(HORIZONTAL_FACING));
    final BlockState up = world.getBlockState(pos.above());
    if(up.getBlock() == this) return state.setValue(SEGMENT, SEGMENT_LOWER).setValue(OPEN, up.getValue(OPEN)).setValue(HORIZONTAL_FACING, up.getValue(HORIZONTAL_FACING));
    return state.setValue(SEGMENT, SEGMENT_LOWER).setValue(OPEN, false);
  }

}
