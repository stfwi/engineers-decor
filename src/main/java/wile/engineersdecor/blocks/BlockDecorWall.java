/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks. This block is derived from vanilla BlockWall
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
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
  public static final PropertyInteger TEXTURE_VARIANT = PropertyInteger.create("tvariant", 0, 7);

  protected static final AxisAlignedBB[] mkAABBs(double pole_thickness_px, double wall_thickness_px, double height_px)
  {
    final double d_0 =  0.0d;
    final double d_1 = 16.0d;
    final double d_a = (8d-pole_thickness_px);
    final double d_b = 16.0d-d_a;
    final double d_k = (8d-wall_thickness_px);
    final double d_l = 16.0d-d_k;
    return new AxisAlignedBB[] {                                          // ENWS P
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_a, d_b, height_px, d_b),  // 0000 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_a, d_b, height_px, d_1),  // 0001 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_a, d_b, height_px, d_b),  // 0010 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_a, d_b, height_px, d_1),  // 0011 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_0, d_b, height_px, d_b),  // 0100 1
      ModAuxiliaries.getPixeledAABB(d_k, d_0, d_0, d_l, height_px, d_1),  // 0101 0
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_0, d_b, height_px, d_b),  // 0110 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_0, d_b, height_px, d_1),  // 0111 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_a, d_1, height_px, d_b),  // 1000 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_a, d_1, height_px, d_1),  // 1001 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_k, d_1, height_px, d_l),  // 1010 0
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_a, d_1, height_px, d_1),  // 1011 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_0, d_1, height_px, d_b),  // 1100 1
      ModAuxiliaries.getPixeledAABB(d_a, d_0, d_0, d_1, height_px, d_1),  // 1101 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_0, d_1, height_px, d_b),  // 1110 1
      ModAuxiliaries.getPixeledAABB(d_0, d_0, d_0, d_1, height_px, d_1)   // 1111 1
    };
  }

  protected static final AxisAlignedBB[][] mkCAABBs(double pole_thickness_px, double wall_thickness_px, double height_px)
  {
    final double d_0 =  0.0d;
    final double d_1 = 16.0d;
    final double d_a = (8d-pole_thickness_px);
    final double d_b = 16.0d-d_a;
    final double d_k = (8d-wall_thickness_px);
    final double d_l = 16.0d-d_k;
    final AxisAlignedBB bb_p = ModAuxiliaries.getPixeledAABB(d_a, d_0, d_a, d_b, height_px, d_b); // 0000
    final AxisAlignedBB bb_s = ModAuxiliaries.getPixeledAABB(d_k, d_0, d_k, d_l, height_px, d_1); // 0001
    final AxisAlignedBB bb_w = ModAuxiliaries.getPixeledAABB(d_0, d_0, d_k, d_l, height_px, d_l); // 0010
    final AxisAlignedBB bb_n = ModAuxiliaries.getPixeledAABB(d_k, d_0, d_0, d_l, height_px, d_l); // 0100
    final AxisAlignedBB bb_e = ModAuxiliaries.getPixeledAABB(d_k, d_0, d_k, d_1, height_px, d_l); // 1000
    return new AxisAlignedBB[][] {                // ENWS P
      new AxisAlignedBB[]{ bb_p },                // 0000 1
      new AxisAlignedBB[]{ bb_s },                // 0001 1
      new AxisAlignedBB[]{ bb_w },                // 0010 1
      new AxisAlignedBB[]{ bb_s,bb_w },           // 0011 1
      new AxisAlignedBB[]{ bb_n },                // 0100 1
      new AxisAlignedBB[]{ bb_n,bb_s },           // 0101 0
      new AxisAlignedBB[]{ bb_n,bb_w },           // 0110 1
      new AxisAlignedBB[]{ bb_n,bb_w,bb_s },      // 0111 1
      new AxisAlignedBB[]{ bb_e },                // 1000 1
      new AxisAlignedBB[]{ bb_e,bb_s },           // 1001 1
      new AxisAlignedBB[]{ bb_e,bb_w },           // 1010 0
      new AxisAlignedBB[]{ bb_e,bb_w,bb_s },      // 1011 1
      new AxisAlignedBB[]{ bb_e,bb_n },           // 1100 1
      new AxisAlignedBB[]{ bb_e,bb_n,bb_s },      // 1101 1
      new AxisAlignedBB[]{ bb_e,bb_n,bb_w },      // 1110 1
      new AxisAlignedBB[]{ bb_e,bb_n,bb_w,bb_s }  // 1111 1
    };
  }

  protected static final AxisAlignedBB[] AABB_BY_INDEX             = mkAABBs(4d, 3.84, 16d);
  protected static final AxisAlignedBB[] CLIP_AABB_BY_INDEX        = mkAABBs(4d, 3.84, 24d);
  protected static final AxisAlignedBB[][] AABB_LIST_BY_INDEX      = mkCAABBs(4d, 3.84, 16d);
  protected static final AxisAlignedBB[][] CLIP_AABB_LIST_BY_INDEX = mkCAABBs(4d, 3.84, 24d);
  protected static final AxisAlignedBB SELECTION_AABB              = ModAuxiliaries.getPixeledAABB(0, 0, 0, 0.01, 0.01, 0.01);

  public BlockDecorWall(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
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

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return CLIP_AABB_BY_INDEX[getAABBIndex(getActualState(state, world, pos))]; }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos)
  { return SELECTION_AABB; }

  protected static int getAABBIndex(IBlockState state)
  { return ((!(state.getValue(SOUTH))) ? 0 : (1<<EnumFacing.SOUTH.getHorizontalIndex()))  // 0
         | ((!(state.getValue( WEST))) ? 0 : (1<<EnumFacing.WEST.getHorizontalIndex()))   // 1
         | ((!(state.getValue(NORTH))) ? 0 : (1<<EnumFacing.NORTH.getHorizontalIndex()))  // 2
         | ((!(state.getValue( EAST))) ? 0 : (1<<EnumFacing.EAST.getHorizontalIndex()));  // 3
  }

  @Override
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos)
  { return false; }

  @Override
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  protected boolean canConnectTo(IBlockAccess world, BlockPos pos, BlockPos other, EnumFacing facing)
  {
    final IBlockState state = world.getBlockState(other);
    final Block block = state.getBlock();
    if((block instanceof BlockDecorWall) || (block instanceof BlockFenceGate) || (block instanceof BlockDecorFence) || (block instanceof BlockDecorDoubleGate)) return true;
    if(world.getBlockState(pos.offset(facing)).getBlock()!=this) return false;
    if(block instanceof BlockFence) return true;
    final BlockFaceShape shp = state.getBlockFaceShape(world, other, facing);
    return (shp == BlockFaceShape.SOLID) && (!isExcepBlockForAttachWithPiston(block));
  }

  protected static boolean isExcepBlockForAttachWithPiston(Block b)
  { return Block.isExceptBlockForAttachWithPiston(b) || (b==Blocks.BARRIER) || (b==Blocks.MELON_BLOCK) || (b==Blocks.PUMPKIN) || (b==Blocks.LIT_PUMPKIN); }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
  { return (side!=EnumFacing.DOWN) || (super.shouldSideBeRendered(blockState, blockAccess, pos, side)); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState(); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return 0; }

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    boolean n = canWallConnectTo(world, pos, EnumFacing.NORTH);
    boolean e = canWallConnectTo(world, pos, EnumFacing.EAST);
    boolean s = canWallConnectTo(world, pos, EnumFacing.SOUTH);
    boolean w = canWallConnectTo(world, pos, EnumFacing.WEST);
    boolean nopole = (n && s && !e && !w) || (!n && !s && e && w);
    long prnd = pos.toLong(); prnd = (prnd>>29) ^ (prnd>>17) ^ (prnd>>9) ^ (prnd>>4) ^ pos.getX() ^ pos.getY() ^ pos.getZ();
    return state.withProperty(UP,!nopole).withProperty(NORTH, n).withProperty(EAST, e).withProperty(SOUTH, s).withProperty(WEST, w).withProperty(TEXTURE_VARIANT, ((int)prnd) & 0x7);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, new IProperty[] {UP, NORTH, EAST, WEST, SOUTH, TEXTURE_VARIANT}); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return (face==EnumFacing.UP) ? (BlockFaceShape.CENTER) : ((face!=EnumFacing.DOWN) ? (BlockFaceShape.MIDDLE_POLE_THICK) : (BlockFaceShape.CENTER_BIG)); }

  @Override
  public boolean canBeConnectedTo(IBlockAccess world, BlockPos pos, EnumFacing facing)
  { return canConnectTo(world, pos, pos.offset(facing), facing.getOpposite()); }

  protected boolean canWallConnectTo(IBlockAccess world, BlockPos pos, EnumFacing facing)
  { return canConnectTo(world, pos, pos.offset(facing), facing.getOpposite()); }

  @Override
  public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos)
  { return true; }

  @Override
  public net.minecraft.pathfinding.PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos)
  { return PathNodeType.FENCE; }

}
