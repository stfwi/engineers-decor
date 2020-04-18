/*
 * @file BlockDecorDoubleGate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Gate blocks that can be one or two segments high.
 */
package wile.engineersdecor.blocks;

import net.minecraft.entity.Entity;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.pathfinding.PathNodeType;

import net.minecraft.block.BlockFenceGate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockDecorDoubleGate extends BlockDecorDirectedHorizontal
{
  private static final AxisAlignedBB AABB_EMPTY = new AxisAlignedBB(0,0,0, 0,0,0.1);

  public static final PropertyInteger SEGMENT = PropertyInteger.create("segment", 0, 1);
  public static final PropertyBool OPEN = BlockFenceGate.OPEN;
  public static final int SEGMENT_LOWER = 0;
  public static final int SEGMENT_UPPER = 1;

  private final ArrayList<AxisAlignedBB> collision_shapes_;
  private final ArrayList<AxisAlignedBB> shapes_;

  public BlockDecorDoubleGate(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    AxisAlignedBB caabb = unrotatedAABB.expand(0, 0.5, 0);
    collision_shapes_ = new ArrayList<AxisAlignedBB>(Arrays.asList(
      NULL_AABB,
      NULL_AABB,
      ModAuxiliaries.getRotatedAABB(caabb, EnumFacing.NORTH, true),
      ModAuxiliaries.getRotatedAABB(caabb, EnumFacing.SOUTH, true),
      ModAuxiliaries.getRotatedAABB(caabb, EnumFacing.WEST, true),
      ModAuxiliaries.getRotatedAABB(caabb, EnumFacing.EAST, true),
      NULL_AABB,
      NULL_AABB
    ));
    shapes_ = new ArrayList<AxisAlignedBB>(Arrays.asList(
      AABB_EMPTY,
      AABB_EMPTY,
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.NORTH, true),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.SOUTH, true),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.WEST, true),
      ModAuxiliaries.getRotatedAABB(unrotatedAABB, EnumFacing.EAST, true),
      AABB_EMPTY,
      AABB_EMPTY
    ));
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta)
  { return super.getStateFromMeta(meta).withProperty(OPEN, (meta&0x4)!=0).withProperty(SEGMENT, ((meta&0x8)!=0) ? SEGMENT_UPPER:SEGMENT_LOWER); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return super.getMetaFromState(state) | (state.getValue(OPEN)?0x4:0x0) | (state.getValue(SEGMENT)==SEGMENT_UPPER ? 0x8:0x0); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, OPEN, SEGMENT); }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return shapes_.get(state.getValue(FACING).getIndex() & 0x7); }

  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return state.getValue(OPEN) ? NULL_AABB : collision_shapes_.get(state.getValue(FACING).getIndex() & 0x7); }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
  { if(!state.getValue(OPEN)) addCollisionBoxToList(pos, entityBox, collidingBoxes, collision_shapes_.get(state.getValue(FACING).getIndex() & 0x7)); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return getInitialState(super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand), world, pos); }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos)
  { return world.getBlockState(pos).getValue(OPEN); }

  @Override
  public net.minecraft.pathfinding.PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos)
  { return state.getValue(OPEN) ? PathNodeType.OPEN : PathNodeType.FENCE; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing face, float hitX, float hitY, float hitZ)
  {
    if((face==EnumFacing.UP) || (face==EnumFacing.DOWN) && (player.getHeldItem(hand).getItem()==Item.getItemFromBlock(this))) return false;
    if(world.isRemote) return true;
    final boolean open = !state.getValue(OPEN);
    world.setBlockState(pos, state.withProperty(OPEN, open),2|8|16);
    if(state.getValue(SEGMENT) == SEGMENT_UPPER) {
      final IBlockState adjacent = world.getBlockState(pos.down());
      if(adjacent.getBlock()==this) world.setBlockState(pos.down(), adjacent.withProperty(OPEN, open), 2|8|16);
    } else {
      final IBlockState adjacent = world.getBlockState(pos.up());
      if(adjacent.getBlock()==this) world.setBlockState(pos.up(), adjacent.withProperty(OPEN, open), 2|8|16);
    }
    world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  {
    if(world.isRemote) return;
    boolean powered = false;
    IBlockState adjacent;
    BlockPos adjacent_pos;
    if(state.getValue(SEGMENT) == SEGMENT_UPPER) {
      adjacent_pos = pos.down();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.getRedstonePower(pos.up(), EnumFacing.UP) > 0) {
        powered = true;
      } else if((adjacent!=null) && (world.isBlockPowered(pos.down(2)))) {
        powered = true;
      }
    } else {
      adjacent_pos = pos.up();
      adjacent = world.getBlockState(adjacent_pos);
      if(adjacent.getBlock()!=this) adjacent = null;
      if(world.isBlockPowered(pos)) {
        powered = true;
      } else if((adjacent!=null) && (world.getRedstonePower(pos.up(2), EnumFacing.UP) > 0)) {
        powered = true;
      }
    }
    boolean sound = false;
    if(powered != state.getValue(OPEN)) {
      world.setBlockState(pos, state.withProperty(OPEN, powered), 2|8|16);
      sound = true;
    }
    if((adjacent != null) && (powered != adjacent.getValue(OPEN))) {
      world.setBlockState(adjacent_pos, adjacent.withProperty(OPEN, powered), 2|8|16);
      sound = true;
    }
    if(sound) {
      world.playSound(null, pos, powered?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  private IBlockState getInitialState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    final IBlockState down = world.getBlockState(pos.down());
    if(down.getBlock() == this) return state.withProperty(SEGMENT, SEGMENT_UPPER).withProperty(OPEN, down.getValue(OPEN)).withProperty(FACING, down.getValue(FACING));
    final IBlockState up = world.getBlockState(pos.up());
    if(up.getBlock() == this) return state.withProperty(SEGMENT, SEGMENT_LOWER).withProperty(OPEN, up.getValue(OPEN)).withProperty(FACING, up.getValue(FACING));
    return state.withProperty(SEGMENT, SEGMENT_LOWER).withProperty(OPEN, false);
  }

}
