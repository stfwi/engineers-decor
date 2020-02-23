/*
 * @file BlockDecorFloorGrating.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;


public class BlockDecorFloorGrating extends BlockDecor.WaterLoggable implements IDecorBlock
{
  public BlockDecorFloorGrating(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity)
  {
    if(!(entity instanceof ItemEntity)) return;
    final boolean colliding = ((entity.getPositionVec().y-pos.getY()) > 0.7);
    if(colliding || (entity.getMotion().getY() > 0)) {
      double x = pos.getX() + 0.5;
      double y = MathHelper.clamp(entity.getPositionVec().y-0.3, pos.getY(), pos.getY()+0.6);
      double z = pos.getZ() + 0.5;
      double vx = entity.getMotion().getX();
      double vy = entity.getMotion().getY();
      double vz = entity.getMotion().getZ();
      if(colliding) {
        vx = 0;
        vy = -0.3;
        vz = 0;
        if((entity.getPositionVec().y-pos.getY()) > 0.8) y = pos.getY() + 0.6;
        entity.prevPosX = x+0.1;
        entity.prevPosY = y+0.1;
        entity.prevPosZ = z+0.1;
      }
      vy = MathHelper.clamp(vy, -0.3, 0);
      entity.setMotion(vx, vy, vz);
      entity.fallDistance = 0;
      entity.setPositionAndUpdate(x,y,z);
    }
  }

}
