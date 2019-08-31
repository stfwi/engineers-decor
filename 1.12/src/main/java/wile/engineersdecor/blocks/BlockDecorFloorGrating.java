/*
 * @file BlockDecorFloorGrating.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockDecorFloorGrating extends BlockDecor
{
  public BlockDecorFloorGrating(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nullable AxisAlignedBB boundingbox)
  { super(registryName, config, material, hardness, resistance, sound, (boundingbox==null) ? (new AxisAlignedBB[]{FULL_BLOCK_AABB}) : (new AxisAlignedBB[]{boundingbox})); }

  @Override
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState)
  { if(!(entity instanceof EntityItem)) super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState); }

  @Override
  public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance)
  {
    if(!(entity instanceof EntityItem)) {
      entity.fall(fallDistance, 1.0F);
    } else {
      entity.motionX = 0;
      entity.motionY = -0.1;
      entity.motionZ = 0;
      entity.setPositionAndUpdate(pos.getX()+0.5, entity.posY-0.3, pos.getZ()+0.5);
    }
  }

  @Override
  public void onLanded(World world, Entity entity)
  {
    if(!(entity instanceof EntityItem)) {
      super.onLanded(world, entity);
    } else {
      entity.motionX = 0;
      entity.motionY = -0.1;
      entity.motionZ = 0;
      entity.setPositionAndUpdate(entity.posX, entity.posY-0.3, entity.posZ);
    }
  }

  @Override
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity)
  {
    if(!(entity instanceof EntityItem)) return;
    entity.motionX = 0;
    entity.motionZ = 0;
    if((entity.posY-pos.getY()) > 0.7) {
      if(entity.motionY > -0.2) entity.motionY = -0.2;
      entity.setPositionAndUpdate(pos.getX()+0.5, entity.posY-0.3, pos.getZ()+0.5);
    }
  }
}
