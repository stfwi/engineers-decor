/*
 * @file BlockDecorWindowSill.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block for windowsills to allow placing things on top
 * (top side solid).
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorWindowSill extends BlockDecorDirected
{
  public BlockDecorWindowSill(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config|CFG_HORIZIONTAL, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return (face==EnumFacing.UP) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
  { return side==EnumFacing.UP; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isTopSolid(IBlockState state)
  { return true; }
}
