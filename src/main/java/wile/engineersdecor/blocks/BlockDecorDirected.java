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
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class BlockDecorDirected extends BlockDecor
{
  public static final PropertyDirection FACING = BlockDirectional.FACING;
  protected final ArrayList<AxisAlignedBB> AABBs;

  public BlockDecorDirected(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound);
    AABBs = new ArrayList<AxisAlignedBB>(Arrays.asList(
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.DOWN),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.UP),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.NORTH),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.SOUTH),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.WEST),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST)
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
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs.get(((EnumFacing)state.getValue(FACING)).getIndex() & 0x7); }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.CUTOUT; }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return this.getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 0x7)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return state.getValue(FACING).getIndex(); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING); }

  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot)
  { return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING))); }

  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  { return state.withRotation(mirrorIn.toRotation((EnumFacing)state.getValue(FACING))); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return this.getDefaultState().withProperty(FACING, facing); }
}
