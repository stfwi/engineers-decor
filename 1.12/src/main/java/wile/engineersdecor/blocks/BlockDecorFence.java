/*
 * @file BlockDecorFence.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Fence blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorFence extends BlockDecorWall
{
  protected static final AxisAlignedBB[]   AABB_BY_INDEX           = mkAABBs (1.5d, 0.5d, 16d);
  protected static final AxisAlignedBB[]   CLIP_AABB_BY_INDEX      = mkAABBs (1.5d, 0.5d, 24d);
  protected static final AxisAlignedBB[][] AABB_LIST_BY_INDEX      = mkCAABBs(1.5d, 0.5d, 16d);
  protected static final AxisAlignedBB[][] CLIP_AABB_LIST_BY_INDEX = mkCAABBs(1.5d, 0.5d, 24d);

  public BlockDecorFence(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super(registryName, config, material, hardness, resistance, sound);
    setDefaultState(blockState.getBaseState().withProperty(UP, false).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
  }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABB_BY_INDEX[getAABBIndex(getActualState(state, source, pos))]; }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
  {
    final AxisAlignedBB[] bbs = CLIP_AABB_LIST_BY_INDEX[getAABBIndex(isActualState ? state : getActualState(state, world, pos))];
    for(int i=0; i<bbs.length;++i) addCollisionBoxToList(pos, entityBox, collidingBoxes, bbs[i]);
  }

  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return CLIP_AABB_BY_INDEX[getAABBIndex(getActualState(state, world, pos))]; }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return (face==EnumFacing.UP) ? (BlockFaceShape.SOLID) : ((face!=EnumFacing.DOWN) ? (BlockFaceShape.MIDDLE_POLE_THIN) : (BlockFaceShape.CENTER_SMALL)); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, new IProperty[] {UP, NORTH, EAST, WEST, SOUTH}); }

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    boolean n = canWallConnectTo(world, pos, EnumFacing.NORTH);
    boolean e = canWallConnectTo(world, pos, EnumFacing.EAST);
    boolean s = canWallConnectTo(world, pos, EnumFacing.SOUTH);
    boolean w = canWallConnectTo(world, pos, EnumFacing.WEST);
    boolean nopole = (n && s && !e && !w) || (!n && !s && e && w);
    return state.withProperty(UP,!nopole).withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s).withProperty(WEST, w);
  }
}
