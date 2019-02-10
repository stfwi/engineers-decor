/*
 * @file BlockDecorLadder.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Ladder block. The implementation is based on the vanilla
 * net.minecraft.block.BlockLadder. Minor changes to enable
 * later configuration (for block list based construction
 * time configuration), does not drop when the block behind
 * is broken, etc.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorLadder extends BlockDecor
{
  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  protected static final AxisAlignedBB EDLADDER_SOUTH_AABB = ModAuxiliaries.getPixeledAABB(3, 0, 0, 13, 16, 2);
  protected static final AxisAlignedBB EDLADDER_EAST_AABB  = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.EAST);
  protected static final AxisAlignedBB EDLADDER_WEST_AABB  = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.WEST);
  protected static final AxisAlignedBB EDLADDER_NORTH_AABB = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.NORTH);


  public BlockDecorLadder(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super(registryName, config, material, hardness, resistance, sound);
    setLightOpacity(0);
    setResistance(2.0f);
    setHardness(0.3f);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.CUTOUT; }

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
  public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  {
    switch ((EnumFacing)state.getValue(FACING)) {
      case NORTH: return EDLADDER_NORTH_AABB;
      case SOUTH: return EDLADDER_SOUTH_AABB;
      case WEST: return EDLADDER_WEST_AABB;
      default: return EDLADDER_EAST_AABB;
    }
  }

  @Override
  public IBlockState getStateFromMeta(int meta)
  {
    final EnumFacing facing = EnumFacing.byIndex(meta & 0x7);
    return this.getDefaultState().withProperty(FACING, (facing.getAxis()==EnumFacing.Axis.Y) ? EnumFacing.NORTH : facing);
  }

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
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return canAttachTo(world, pos.west(), side) || canAttachTo(world, pos.east(), side) || canAttachTo(world, pos.north(), side) || canAttachTo(world, pos.south(), side); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    if(facing.getAxis().isHorizontal() && canAttachTo(world, pos.offset(facing.getOpposite()), facing)) return this.getDefaultState().withProperty(FACING, facing);
    for(EnumFacing e:EnumFacing.Plane.HORIZONTAL) {
      if(this.canAttachTo(world, pos.offset(e.getOpposite()), e)) return this.getDefaultState().withProperty(FACING, e);
    }
    return this.getDefaultState();
  }

  private boolean canAttachTo(World world, BlockPos pos, EnumFacing side)
  {
    final IBlockState state = world.getBlockState(pos);
    return (!isExceptBlockForAttachWithPiston(state.getBlock())) && (state.getBlockFaceShape(world, pos, side) == BlockFaceShape.SOLID);
  }

}
