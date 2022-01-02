/*
 * @file EdFloorGratingBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import wile.engineersdecor.libmc.blocks.StandardBlocks;

import javax.annotation.Nullable;


public class EdFloorGratingBlock extends StandardBlocks.WaterLoggable
{
  public EdFloorGratingBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return true; }

  @Override
  public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)
  {
    if(!(entity instanceof ItemEntity)) return;
    final boolean colliding = ((entity.position().y-pos.getY()) > 0.7);
    if(colliding || (entity.getDeltaMovement().y() > 0)) {
      double x = pos.getX() + 0.5;
      double y = Mth.clamp(entity.position().y-0.3, pos.getY(), pos.getY()+0.6);
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
      vy = Mth.clamp(vy, -0.3, 0);
      entity.setDeltaMovement(vx, vy, vz);
      entity.fallDistance = 0;
      entity.teleportTo(x,y,z);
    }
  }

}
