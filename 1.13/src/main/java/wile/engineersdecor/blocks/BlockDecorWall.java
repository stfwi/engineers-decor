/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.detail.ModAuxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorWall extends BlockWall
{
  private final VoxelShape[] shape_voxels;
  private final VoxelShape[] collision_shape_voxels;

  public BlockDecorWall(long config, Block.Properties builder)
  {
    super(builder);
    this.shape_voxels = buildWallShapes(4.0F, 4.0F, 16.0F, 0.0F, 16.0F);
    this.collision_shape_voxels = buildWallShapes(4.0F, 4.0F, 24.0F, 0.0F, 24.0F);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  protected VoxelShape[] buildWallShapes(float pole_width_x, float pole_width_z, float pole_height, float side_min_y, float side_max_y)
  { return super.func_196408_a(pole_width_x, pole_width_z, pole_height, side_min_y, side_max_y); }

  @Override
  public VoxelShape getShape(IBlockState state, IBlockReader world, BlockPos pos)
  { return shape_voxels[this.getIndex(state)]; }

  @Override
  public VoxelShape getCollisionShape(IBlockState state, IBlockReader world, BlockPos pos)
  { return collision_shape_voxels[this.getIndex(state)]; }

  private boolean attachesTo(IBlockState state, BlockFaceShape shape)
  { final Block block = state.getBlock(); return (shape==BlockFaceShape.SOLID) && (!isExcepBlockForAttachWithPiston(block)) || (shape==BlockFaceShape.MIDDLE_POLE_THICK) || ((shape==BlockFaceShape.MIDDLE_POLE) && (block instanceof BlockFenceGate)); }

  @Override
  public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos)
  {
    if(state.get(WATERLOGGED)) world.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    if(facing == EnumFacing.DOWN) return super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
    boolean n = (facing==EnumFacing.NORTH) ? this.attachesTo(facingState, facingState.getBlockFaceShape(world, facingPos, facing.getOpposite())) : state.get(NORTH);
    boolean e = (facing==EnumFacing.EAST) ? this.attachesTo(facingState, facingState.getBlockFaceShape(world, facingPos, facing.getOpposite())) : state.get(EAST);
    boolean s = (facing==EnumFacing.SOUTH) ? this.attachesTo(facingState, facingState.getBlockFaceShape(world, facingPos, facing.getOpposite())) : state.get(SOUTH);
    boolean w = (facing==EnumFacing.WEST) ? this.attachesTo(facingState, facingState.getBlockFaceShape(world, facingPos, facing.getOpposite())) : state.get(WEST);
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return state.with(UP, not_straight).with(NORTH, n).with(EAST, e).with(SOUTH, s).with(WEST, w);
  }

}
