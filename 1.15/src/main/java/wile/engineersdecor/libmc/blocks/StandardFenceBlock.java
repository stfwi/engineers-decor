/*
 * @file StandardFenceBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.libmc.blocks;

import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.world.*;
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
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;


public class StandardFenceBlock extends WallBlock implements StandardBlocks.IStandardBlock
{
  private final VoxelShape[] shape_voxels;
  private final VoxelShape[] collision_shape_voxels;


  public StandardFenceBlock(long config, Block.Properties properties)
  { this(config, properties, 1.5,16, 1.5, 0, 16); }

  public StandardFenceBlock(long config, Block.Properties properties, double pole_width, double pole_height, double side_width, double side_max_y, double side_min_y)
  {
    super(properties);
    this.shape_voxels = buildShapes(pole_width, pole_height, side_width, side_max_y, side_min_y);
    this.collision_shape_voxels = buildShapes(pole_width,24, pole_width, 0, 24);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }


  protected VoxelShape[] buildShapes(double pole_width, double pole_height, double side_width, double side_min_y, double side_max_y)
  {
    final double px0=8d-pole_width, px1=8d+pole_width, sx0=8d-side_width, sx1=8d+side_width;
    VoxelShape vp  = Block.makeCuboidShape(px0, 0, px0, px1, pole_height, px1);
    VoxelShape vs1 = Block.makeCuboidShape(sx0, side_min_y, 0, sx1, side_max_y, sx1);
    VoxelShape vs2 = Block.makeCuboidShape(sx0, side_min_y, sx0, sx1, side_max_y, 16);
    VoxelShape vs3 = Block.makeCuboidShape(0, side_min_y, sx0, sx1, side_max_y, sx1);
    VoxelShape vs4 = Block.makeCuboidShape(sx0, side_min_y, sx0, 16, side_max_y, sx1);
    VoxelShape vs5 = VoxelShapes.or(vs1, vs4);
    VoxelShape vs6 = VoxelShapes.or(vs2, vs3);
    return new VoxelShape[] {
      vp,
      VoxelShapes.or(vp, vs2),
      VoxelShapes.or(vp, vs3),
      VoxelShapes.or(vp, vs6),
      VoxelShapes.or(vp, vs1),
        VoxelShapes.or(vs2, vs1),
      VoxelShapes.or(vp, VoxelShapes.or(vs3, vs1)),
      VoxelShapes.or(vp, VoxelShapes.or(vs6, vs1)),
      VoxelShapes.or(vp, vs4),
      VoxelShapes.or(vp, VoxelShapes.or(vs2, vs4)),
        VoxelShapes.or(vs3, vs4),
      VoxelShapes.or(vp, VoxelShapes.or(vs6, vs4)),
      VoxelShapes.or(vp, vs5),
      VoxelShapes.or(vp, VoxelShapes.or(vs2, vs5)),
      VoxelShapes.or(vp, VoxelShapes.or(vs3, vs5)),
      VoxelShapes.or(vp, VoxelShapes.or(vs6, vs5))
    };
  }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return shape_voxels[this.getIndex(state)]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return collision_shape_voxels[this.getIndex(state)]; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); }

  protected boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  {
    final Block block = facingState.getBlock();
    if((block instanceof FenceGateBlock) || (block instanceof StandardFenceBlock) || (block instanceof VariantWallBlock)) return true;
    final BlockState oppositeState = world.getBlockState(facingPos.offset(side, 2));
    if(!(oppositeState.getBlock() instanceof StandardFenceBlock)) return false;
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
