/*
 * @file BlockDecorDirected.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.state.StateContainer;
import net.minecraft.util.math.shapes.VoxelShapes;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class BlockDecorDirected extends BlockDecor
{
  public static final DirectionProperty FACING = BlockDirectional.FACING;
  protected final ArrayList<VoxelShape> AABBs;

  public BlockDecorDirected(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  {
    super(config, builder);
    setDefaultState(stateContainer.getBaseState().with(FACING, EnumFacing.UP));
    final boolean is_horizontal = ((config & BlockDecor.CFG_HORIZIONTAL)!=0);
    AABBs = new ArrayList<VoxelShape>(Arrays.asList(
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.DOWN, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.UP, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.NORTH, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.SOUTH, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.WEST, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST, is_horizontal)),
      VoxelShapes.create(unrotatedAABB),
      VoxelShapes.create(unrotatedAABB)
    ));
  }

  @Override
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockReader world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getShape(IBlockState state, IBlockReader source, BlockPos pos)
  { return AABBs.get(((EnumFacing)state.get(FACING)).getIndex() & 0x7); }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getCollisionShape(IBlockState state, IBlockReader world, BlockPos pos)
  { return getShape(state, world, pos); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder)
  { builder.add(FACING); }

  @Override
  @Nullable
  public IBlockState getStateForPlacement(BlockItemUseContext context) {
    if((config & CFG_HORIZIONTAL_PLACEMENT)!=0) {
      // placement in direction the player is facing
      return getDefaultState().with(FACING, context.getPlacementHorizontalFacing());
    } else {
      // default: placement on the face the player clicking
      return getDefaultState().with(FACING, context.getFace());
    }
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.CUTOUT; }

}
