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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
    final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
    AABBs = new ArrayList<AxisAlignedBB>(Arrays.asList(
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.DOWN, is_horizontal),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.UP, is_horizontal),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.NORTH, is_horizontal),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.SOUTH, is_horizontal),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.WEST, is_horizontal),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST, is_horizontal),
      unrotatedAABB, unrotatedAABB // Array fill to ensure that the array size covers 4 bit (meta & 0x07).
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
  { return AABBs.get(((EnumFacing)state.getValue(FACING)).getIndex() & 0x7); }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

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
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  {
    if(!super.canPlaceBlockOnSide(world, pos, side)) return false;
    return !(((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT))==(CFG_HORIZIONTAL)) && ((side==EnumFacing.UP)||(side==EnumFacing.DOWN)));
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) {
      // horizontal placement in direction the player is looking
      facing = placer.getHorizontalFacing();
    } else if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL)) {
      // horizontal placement on a face
      facing = ((facing==EnumFacing.UP)||(facing==EnumFacing.DOWN)) ? (EnumFacing.NORTH) : facing;
    } else if((config & CFG_LOOK_PLACEMENT)!=0) {
      // placement in direction the player is looking, with up and down
      facing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
    } else {
      // default: placement on the face the player clicking
    }
    if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
    return getDefaultState().withProperty(FACING, facing);
  }
}
