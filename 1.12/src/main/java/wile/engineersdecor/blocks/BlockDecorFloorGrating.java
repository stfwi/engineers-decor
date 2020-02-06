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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity)
  {
    if(!(entity instanceof EntityItem)) return;
    final boolean colliding = ((entity.posY-pos.getY()) > 0.7);
    if(colliding || (entity.motionY > 0)) {
      double x = pos.getX() + 0.5;
      double y = MathHelper.clamp(entity.posY-0.3, pos.getY(), pos.getY()+0.6);
      double z = pos.getZ() + 0.5;
      if(colliding) {
        entity.motionX = 0;
        entity.motionZ = 0;
        entity.motionY = -0.3;
        if((entity.posY-pos.getY()) > 0.8) y = pos.getY() + 0.6;
        entity.prevPosX = x+0.1;
        entity.prevPosY = y+0.1;
        entity.prevPosZ = z+0.1;
      }
      entity.motionY = MathHelper.clamp(entity.motionY, -0.3, 0);
      entity.fallDistance = 0;
      entity.setPositionAndUpdate(x,y,z);
    }
  }
}
