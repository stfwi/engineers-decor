/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks. This block is derived from vanilla BlockWall
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * As strange as it is, I could not properly get a block derived from BlockWall working,
 * either the VARIANT made issues, or the item model was duplicated (using state mapper),
 * so, this is now basically a BlockWall without the VARIANT propery. Anyway a solved issue
 * in mc1.13+. Deriving from BlockDecor to have the tooltip, creativetab etc already set up.
 */
public class BlockDecorWall extends BlockDecor
{
  public static final PropertyBool UP = BlockWall.UP;
  public static final PropertyBool NORTH = BlockWall.NORTH;
  public static final PropertyBool EAST = BlockWall.EAST;
  public static final PropertyBool SOUTH = BlockWall.SOUTH;
  public static final PropertyBool WEST = BlockWall.WEST;

  private static final double d_0 = 0.0d;
  private static final double d_1 = 1.0d;
  private static final double d_a = 0.25d;
  private static final double d_b = 1.0d-d_a;
  private static final double d_k = 0.26d; // 0.3125D;
  private static final double d_l = 1.0d-d_k;
  protected static final AxisAlignedBB[] AABB_BY_INDEX = new AxisAlignedBB[] {
    new AxisAlignedBB(d_a, d_0, d_a, d_b, d_1, d_b),
    new AxisAlignedBB(d_a, d_0, d_a, d_b, d_1, d_1),
    new AxisAlignedBB(d_0, d_0, d_a, d_b, d_1, d_b),
    new AxisAlignedBB(d_0, d_0, d_a, d_b, d_1, d_1),
    new AxisAlignedBB(d_a, d_0, d_0, d_b, d_1, d_b),
    new AxisAlignedBB(d_k, d_0, d_0, d_l, d_1, d_1),
    new AxisAlignedBB(d_0, d_0, d_0, d_b, d_1, d_b),
    new AxisAlignedBB(d_0, d_0, d_0, d_b, d_1, d_1),
    new AxisAlignedBB(d_a, d_0, d_a, d_1, d_1, d_b),
    new AxisAlignedBB(d_a, d_0, d_a, d_1, d_1, d_1),
    new AxisAlignedBB(d_0, d_0, d_k, d_1, d_1, d_l),
    new AxisAlignedBB(d_0, d_0, d_a, d_1, d_1, d_1),
    new AxisAlignedBB(d_a, d_0, d_0, d_1, d_1, d_b),
    new AxisAlignedBB(d_a, d_0, d_0, d_1, d_1, d_1),
    new AxisAlignedBB(d_0, d_0, d_0, d_1, d_1, d_b),
    new AxisAlignedBB(d_0, d_0, d_0, d_1, d_1, d_1)
  };
  private static final double clip_height = 1.5d;
  protected static final AxisAlignedBB[] CLIP_AABB_BY_INDEX = new AxisAlignedBB[] { AABB_BY_INDEX[0].setMaxY(clip_height), AABB_BY_INDEX[1].setMaxY(clip_height), AABB_BY_INDEX[2].setMaxY(clip_height), AABB_BY_INDEX[3].setMaxY(clip_height), AABB_BY_INDEX[4].setMaxY(clip_height), AABB_BY_INDEX[5].setMaxY(clip_height), AABB_BY_INDEX[6].setMaxY(clip_height), AABB_BY_INDEX[7].setMaxY(clip_height), AABB_BY_INDEX[8].setMaxY(clip_height), AABB_BY_INDEX[9].setMaxY(clip_height), AABB_BY_INDEX[10].setMaxY(clip_height), AABB_BY_INDEX[11].setMaxY(clip_height), AABB_BY_INDEX[12].setMaxY(clip_height), AABB_BY_INDEX[13].setMaxY(clip_height), AABB_BY_INDEX[14].setMaxY(clip_height), AABB_BY_INDEX[15].setMaxY(clip_height) };

  public BlockDecorWall(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super(registryName, config, material, hardness, resistance, sound);
    setDefaultState(blockState.getBaseState().withProperty(UP, false).withProperty(NORTH, false).withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
  }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABB_BY_INDEX[getAABBIndex(getActualState(state, source, pos))]; }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
  { addCollisionBoxToList(pos, entityBox, collidingBoxes, CLIP_AABB_BY_INDEX[getAABBIndex(isActualState ? state : getActualState(state, world, pos))]); }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return CLIP_AABB_BY_INDEX[getAABBIndex(getActualState(state, world, pos))]; }

  private static int getAABBIndex(IBlockState state)
  { return ((!(state.getValue(NORTH))) ? 0 : (1<<EnumFacing.NORTH.getHorizontalIndex()))
         | ((!(state.getValue( EAST))) ? 0 : (1<<EnumFacing.EAST.getHorizontalIndex()))
         | ((!(state.getValue(SOUTH))) ? 0 : (1<<EnumFacing.SOUTH.getHorizontalIndex()))
         | ((!(state.getValue( WEST))) ? 0 : (1<<EnumFacing.WEST.getHorizontalIndex()));
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isPassable(IBlockAccess world, BlockPos pos)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return false; }

  private boolean canConnectTo(IBlockAccess world, BlockPos pos, BlockPos other, EnumFacing facing)
  {
    final IBlockState state = world.getBlockState(other);
    final Block block = state.getBlock();
    if((block instanceof BlockDecorWall) || (block instanceof BlockFenceGate)) return true;
    if(world.getBlockState(pos.offset(facing)).getBlock()!=this) return false;
    if(block instanceof BlockFence) return true;
    final BlockFaceShape shp = state.getBlockFaceShape(world, other, facing);
    return (shp == BlockFaceShape.SOLID) && (!isExcepBlockForAttachWithPiston(block));
  }

  protected static boolean isExcepBlockForAttachWithPiston(Block b)
  { return Block.isExceptBlockForAttachWithPiston(b) || (b==Blocks.BARRIER) || (b==Blocks.MELON_BLOCK) || (b==Blocks.PUMPKIN) || (b==Blocks.LIT_PUMPKIN); }

  @Override
  @SuppressWarnings("deprecation")
  @SideOnly(Side.CLIENT)
  public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
  { return (side!=EnumFacing.DOWN) || (super.shouldSideBeRendered(blockState, blockAccess, pos, side)); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState(); }

  @Override
  @SuppressWarnings("deprecation")
  public int getMetaFromState(IBlockState state)
  { return 0; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    boolean n = canWallConnectTo(world, pos, EnumFacing.NORTH);
    boolean e = canWallConnectTo(world, pos, EnumFacing.EAST);
    boolean s = canWallConnectTo(world, pos, EnumFacing.SOUTH);
    boolean w = canWallConnectTo(world, pos, EnumFacing.WEST);
    boolean nopole = (n && s && !e && !w) || (!n && !s && e && w);
    return state.withProperty(UP,!nopole).withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s).withProperty(WEST, w);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, new IProperty[] {UP, NORTH, EAST, WEST, SOUTH}); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return (face==EnumFacing.UP) ? (BlockFaceShape.SOLID) : ((face!=EnumFacing.DOWN) ? (BlockFaceShape.MIDDLE_POLE_THICK) : (BlockFaceShape.CENTER_BIG)); }

  @Override
  public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing)
  { return canConnectTo(world, pos, pos.offset(facing), facing.getOpposite()); }

  private boolean canWallConnectTo(IBlockAccess world, BlockPos pos, EnumFacing facing)
  { return canConnectTo(world, pos, pos.offset(facing), facing.getOpposite()); }

  @Override
  public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos)
  { return true; }

}
