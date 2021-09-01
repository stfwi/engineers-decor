/*
 * @file EdFloorGratingBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;


public class EdFloorGratingBlock extends DecorBlock.WaterLoggable implements IDecorBlock
{
  public EdFloorGratingBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void entityInside(BlockState state, World world, BlockPos pos, Entity entity)
  {
    if(!(entity instanceof ItemEntity)) return;
    final boolean colliding = ((entity.position().y-pos.getY()) > 0.7);
    if(colliding || (entity.getDeltaMovement().y() > 0)) {
      double x = pos.getX() + 0.5;
      double y = MathHelper.clamp(entity.position().y-0.3, pos.getY(), pos.getY()+0.6);
      double z = pos.getZ() + 0.5;
      double vx = entity.getDeltaMovement().x();
      double vy = entity.getDeltaMovement().y();
      double vz = entity.getDeltaMovement().z();
      if(colliding) {
        vx = 0;
        vy = -0.3;
        vz = 0;
        if((entity.position().y-pos.getY()) > 0.8) y = pos.getY() + 0.6;
        entity.xo = x+0.1;
        entity.yo = y+0.1;
        entity.zo = z+0.1;
      }
      vy = MathHelper.clamp(vy, -0.3, 0);
      entity.setDeltaMovement(vx, vy, vz);
      entity.fallDistance = 0;
      entity.teleportTo(x,y,z);
    }
  }

}
