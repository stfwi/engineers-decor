/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.world.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.state.StateContainer;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorFence extends WallBlock implements IDecorBlock
{
  private final VoxelShape[] shape_voxels;
  private final VoxelShape[] collision_shape_voxels;

  public BlockDecorFence(long config, Block.Properties builder)
  {
    super(builder);
    this.shape_voxels = buildWallShapes(1.5f, 1.5f, 16f, 0f, 16f);
    this.collision_shape_voxels = buildWallShapes(1.5f, 1.5f, 24f, 0f, 24f);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  protected VoxelShape[] buildWallShapes(float pole_width_x, float pole_width_z, float pole_height, float side_min_y, float side_max_y)
  { return super.makeShapes(pole_width_x, pole_width_z, pole_height, side_min_y, side_max_y); }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return shape_voxels[this.getIndex(state)]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return collision_shape_voxels[this.getIndex(state)]; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); }

  private boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  {
    final Block block = facingState.getBlock();
    if((block instanceof FenceGateBlock) || (block instanceof BlockDecorFence) || (block instanceof BlockDecorWall)) return true;
    final BlockState oppositeState = world.getBlockState(facingPos.offset(side, 2));
    if(!(oppositeState.getBlock() instanceof BlockDecorFence)) return false;
    return facingState.isNormalCube(world, facingPos) && hasSolidSide(facingState, world, facingPos, side);
  }

  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    IWorldReader world = context.getWorld();
    BlockPos pos = context.getPos();
    IFluidState fs = context.getWorld().getFluidState(context.getPos());
    boolean n = attachesTo(world.getBlockState(pos.north()), world, pos.north(), Direction.SOUTH);
    boolean e = attachesTo(world.getBlockState(pos.east()), world, pos.east(), Direction.WEST);
    boolean s = attachesTo(world.getBlockState(pos.south()), world, pos.south(), Direction.NORTH);
    boolean w = attachesTo(world.getBlockState(pos.west()), world, pos.west(), Direction.EAST);
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return getDefaultState().with(UP, not_straight).with(NORTH, n).with(EAST, e).with(SOUTH, s).with(WEST, w).with(WATERLOGGED, fs.getFluid() == Fluids.WATER);
  }

  @Override
  public BlockState updatePostPlacement(BlockState state, Direction side, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos)
  {
    if(state.get(WATERLOGGED)) world.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    if(side == Direction.DOWN) return super.updatePostPlacement(state, side, facingState, world, currentPos, facingPos);
    boolean n = (side==Direction.NORTH) ? attachesTo(facingState, world, facingPos, side) : state.get(NORTH);
    boolean e = (side==Direction.EAST) ? attachesTo(facingState, world, facingPos, side) : state.get(EAST);
    boolean s = (side==Direction.SOUTH) ? attachesTo(facingState, world, facingPos, side) : state.get(SOUTH);
    boolean w = (side==Direction.WEST) ? attachesTo(facingState, world, facingPos, side) : state.get(WEST);
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return state.with(UP, not_straight).with(NORTH, n).with(EAST, e).with(SOUTH, s).with(WEST, w);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> entityType)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.NORMAL; }
}
