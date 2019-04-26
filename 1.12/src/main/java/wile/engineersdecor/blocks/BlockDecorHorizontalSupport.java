/*
 * @file BlockDecorDirected.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class BlockDecorHorizontalSupport extends BlockDecor
{
  public static final PropertyBool EASTWEST = PropertyBool.create("eastwest");
  protected final ArrayList<AxisAlignedBB> AABBs;

  public BlockDecorHorizontalSupport(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config|CFG_HORIZIONTAL, material, hardness, resistance, sound);
    final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
    AABBs = new ArrayList<AxisAlignedBB>(Arrays.asList(
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.NORTH, true),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.WEST, true)
    ));

  }

  @Override
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs.get(state.getValue(EASTWEST) ? 0x1 : 0x0); }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return this.getDefaultState().withProperty(EASTWEST, ((meta & 0x1) != 0)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return state.getValue(EASTWEST) ? 0x1 : 0x0; }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, EASTWEST); }

  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot)
  { return (rot==Rotation.CLOCKWISE_180) ? state : state.withProperty(EASTWEST, !state.getValue(EASTWEST)); }

  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return super.canPlaceBlockOnSide(world, pos, side); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    facing = placer.getHorizontalFacing();
    return getDefaultState().withProperty(EASTWEST, (facing==EnumFacing.EAST)||(facing==EnumFacing.WEST));
  }

}
