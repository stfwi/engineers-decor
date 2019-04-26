/*
 * @file BlockDecorGlassBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block-size glass blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorGlassBlock extends BlockDecor
{
  public BlockDecorGlassBlock(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super(registryName, config, material, hardness, resistance, sound);
    setLightOpacity(0);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.TRANSLUCENT; }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
  {
    final IBlockState neighbourState = world.getBlockState(pos.offset(side));
    return ((neighbourState==null) || (!(neighbourState.getBlock() instanceof BlockDecorGlassBlock)));
  }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public float getAmbientOcclusionLightValue(IBlockState state)
  { return 0.9F; }

  @Override
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos)
  { return 2; }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
  { return 0; }

  @Override
  public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.SOLID; }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return FULL_BLOCK_AABB; }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return FULL_BLOCK_AABB; }

}
