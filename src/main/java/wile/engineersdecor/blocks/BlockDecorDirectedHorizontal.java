/*
 * @file BlockDecorDirectedHorizontal.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller directed block with direction set narrowed
 * to horizontal directions.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorDirectedHorizontal extends BlockDecorDirected
{
  public static final PropertyDirection FACING = BlockHorizontal.FACING;

  public BlockDecorDirectedHorizontal(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config|CFG_HORIZIONTAL, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 0x3)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return state.getValue(FACING).getHorizontalIndex(); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING); }

  @Override
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs.get(state.getValue(FACING).getIndex() & 0x7); }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  public IBlockState withRotation(IBlockState state, Rotation rot)
  { return state; }

  @Override
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  {
    if((config & CFG_LOOK_PLACEMENT) != 0) {
      // horizontal placement in direction the player is looking
      facing = placer.getHorizontalFacing();
    } else {
      // horizontal placement on a face
      facing = ((facing==EnumFacing.UP)||(facing==EnumFacing.DOWN)) ? (placer.getHorizontalFacing()) : facing;
    }
    if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
    if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (placer.isSneaking())) facing = facing.getOpposite();
    return getDefaultState().withProperty(FACING, facing);
  }

}
