/*
 * @file BlockDecorHorizontalSupport.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Horizontal ceiling support. Symmetric x axis, fixed in
 * xz plane, therefore boolean placement state.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
  public static final PropertyBool EASTWEST  = PropertyBool.create("eastwest");
  public static final PropertyBool LEFTBEAM  = PropertyBool.create("leftbeam");
  public static final PropertyBool RIGHTBEAM = PropertyBool.create("rightbeam");
  public static final PropertyInteger DOWNCONNECT = PropertyInteger.create("downconnect", 0, 2);
  protected final ArrayList<AxisAlignedBB> AABBs;

  public BlockDecorHorizontalSupport(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config|CFG_HORIZIONTAL, material, hardness, resistance, sound);
    final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
    AABBs = new ArrayList<AxisAlignedBB>(Arrays.asList(
      // Effective bounding box
      ModAuxiliaries.getRotatedAABB(unrotatedAABB.grow(2.0/16, 0, 0), EnumFacing.NORTH, true),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB.grow(2.0/16, 0, 0), EnumFacing.WEST, true),
      // Displayed bounding box
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
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos)
  { return AABBs.get(state.getValue(EASTWEST) ? 0x3 : 0x2).offset(pos); }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs.get(state.getValue(EASTWEST) ? 0x1 : 0x0); }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(EASTWEST, ((meta & 0x1) != 0)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return (state.getValue(EASTWEST) ? 0x1:0x0); }

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    boolean ew = state.getValue(EASTWEST);
    final IBlockState rstate = world.getBlockState((!ew) ? (pos.east()) : (pos.south()) );
    final IBlockState lstate = world.getBlockState((!ew) ? (pos.west()) : (pos.north()) );
    final IBlockState dstate = world.getBlockState(pos.down());
    int down_connector = 0;
    if((dstate.getBlock() instanceof BlockDecorStraightPole)) {
      final EnumFacing dfacing = dstate.getValue(BlockDecorStraightPole.FACING);
      final BlockDecorStraightPole pole = (BlockDecorStraightPole)dstate.getBlock();
      if((dfacing.getAxis() == EnumFacing.Axis.Y)) {
        if((pole==ModBlocks.THICK_STEEL_POLE) || ((pole==ModBlocks.THICK_STEEL_POLE_HEAD) && (dfacing==EnumFacing.UP))) {
          down_connector = 2;
        } else if((pole==ModBlocks.THIN_STEEL_POLE) || ((pole==ModBlocks.THIN_STEEL_POLE_HEAD) && (dfacing==EnumFacing.UP))) {
          down_connector = 1;
        }
      }
    }
    return state.withProperty(RIGHTBEAM, (rstate.getBlock()==this) && (rstate.getValue(EASTWEST) != ew))
                .withProperty(LEFTBEAM , (lstate.getBlock()==this) && (lstate.getValue(EASTWEST) != ew))
                .withProperty(DOWNCONNECT , down_connector);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, EASTWEST, RIGHTBEAM, LEFTBEAM, DOWNCONNECT); }

  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot)
  { return (rot==Rotation.CLOCKWISE_180) ? state : state.withProperty(EASTWEST, !state.getValue(EASTWEST)); }

  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return super.canPlaceBlockOnSide(world, pos, side); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return getActualState(getDefaultState().withProperty(EASTWEST, (placer.getHorizontalFacing().getAxis()==EnumFacing.Axis.X)), world, pos); }

}
