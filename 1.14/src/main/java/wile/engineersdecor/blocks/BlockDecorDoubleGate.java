/*
 * @file BlockDecorDoubleGate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Gate blocks that can be one or two segments high.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.pathfinding.PathType;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class BlockDecorDoubleGate extends BlockDecor.HorizontalWaterLoggable implements IDecorBlock
{
  public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 1);
  public static final BooleanProperty OPEN = FenceGateBlock.OPEN;
  public static final int SEGMENT_LOWER = 0;
  public static final int SEGMENT_UPPER = 1;
  protected final ArrayList<VoxelShape> collision_shapes_;

  public BlockDecorDoubleGate(long config, Block.Properties properties, AxisAlignedBB aabb)
  { this(config, properties, new AxisAlignedBB[]{aabb}); }

  public BlockDecorDoubleGate(long config, Block.Properties properties, AxisAlignedBB[] aabbs)
  {
    super(config, properties, aabbs);
    AxisAlignedBB[] caabbs = new AxisAlignedBB[aabbs.length];
    for(int i=0; i<caabbs.length; ++i) caabbs[i] = aabbs[i].expand(0, 0.5, 0);
    collision_shapes_ = new ArrayList<VoxelShape>(Arrays.asList(
      VoxelShapes.fullCube(),
      VoxelShapes.fullCube(),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.NORTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.SOUTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.WEST, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(caabbs, Direction.EAST, true)),
      VoxelShapes.fullCube(),
      VoxelShapes.fullCube()
    ));
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return state.get(OPEN) ? VoxelShapes.empty() : collision_shapes_.get(state.get(HORIZONTAL_FACING).getIndex() & 0x7); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(SEGMENT).add(OPEN); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return getInitialState(super.getStateForPlacement(context), context.getWorld(), context.getPos()); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return getInitialState(super.updatePostPlacement(state, facing, facingState, world, pos, facingPos), world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if((rayTraceResult.getFace()==Direction.UP) || (rayTraceResult.getFace()==Direction.DOWN) && (player.getHeldItem(hand).getItem()==this.asItem())) return false;
    if(world.isRemote) return true;
    final boolean open = !state.get(OPEN);
    world.setBlockState(pos, state.with(OPEN, open),2|8|16);
    if(state.get(SEGMENT) == SEGMENT_UPPER) {
      final BlockState adjacent = world.getBlockState(pos.down());
      if(adjacent.getBlock()==this) world.setBlockState(pos.down(), adjacent.with(OPEN, open), 2|8|16);
    } else {
      final BlockState adjacent = world.getBlockState(pos.up());
      if(adjacent.getBlock()==this) world.setBlockState(pos.up(), adjacent.with(OPEN, open), 2|8|16);
    }
    world.playSound(null, pos, open?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type)
  { return state.get(OPEN); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    if(world.isRemote) return;
    boolean powered = false;
    BlockState adjacent;
    BlockPos adjacent_pos;
    if(state.get(SEGMENT) == SEGMENT_UPPER) {
      adjacent_pos = pos.down();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.getRedstonePower(pos.up(), Direction.UP) > 0) {
        powered = true;
      } else if((adjacent!=null) && (world.isBlockPowered(pos.down(2)))) {
        powered = true;
      }
    } else {
      adjacent_pos = pos.up();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.isBlockPowered(pos)) {
        powered = true;
      } else if((adjacent!=null) && (world.getRedstonePower(pos.up(2), Direction.UP) > 0)) {
        powered = true;
      }
    }
    boolean sound = false;
    if(powered != state.get(OPEN)) {
      world.setBlockState(pos, state.with(OPEN, powered), 2|8|16);
      sound = true;
    }
    if((adjacent != null) && (powered != adjacent.get(OPEN))) {
      world.setBlockState(adjacent_pos, adjacent.with(OPEN, powered), 2|8|16);
      sound = true;
    }
    if(sound) {
      world.playSound(null, pos, powered?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  private BlockState getInitialState(BlockState state, IWorld world, BlockPos pos)
  {
    final BlockState down = world.getBlockState(pos.down());
    if(down.getBlock() == this) return state.with(SEGMENT, SEGMENT_UPPER).with(OPEN, down.get(OPEN)).with(HORIZONTAL_FACING, down.get(HORIZONTAL_FACING));
    final BlockState up = world.getBlockState(pos.up());
    if(up.getBlock() == this) return state.with(SEGMENT, SEGMENT_LOWER).with(OPEN, up.get(OPEN)).with(HORIZONTAL_FACING, up.get(HORIZONTAL_FACING));
    return state.with(SEGMENT, SEGMENT_LOWER).with(OPEN, false);
  }

}
