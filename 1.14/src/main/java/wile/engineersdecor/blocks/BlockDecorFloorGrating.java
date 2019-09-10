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
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;


public class BlockDecorFloorGrating extends BlockDecor
{
  public BlockDecorFloorGrating(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  public void onLanded(IBlockReader world, Entity entity)
  {
    if(!(entity instanceof ItemEntity)) {
      super.onLanded(world, entity);
    } else {
      entity.setMotion(0, -0.1,0);
      entity.setPositionAndUpdate(entity.posX, entity.posY-0.3, entity.posZ);
    }
  }

  @Override
  public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance)
  {
    if(!(entity instanceof ItemEntity)) {
      super.onFallenUpon(world, pos, entity, fallDistance);
    } else {
      entity.setMotion(0, -0.1,0);
      entity.setPositionAndUpdate(entity.posX, entity.posY-0.3, entity.posZ);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity)
  {
    if(!(entity instanceof ItemEntity)) return;
    if((entity.posY-pos.getY()) < 0.7) return;
    double vy = MathHelper.clamp(entity.getMotion().y, -1.2, -0.2);
    entity.setMotion(0, vy, 0);
    entity.setPositionAndUpdate(pos.getX()+0.5, entity.posY-0.3, pos.getZ()+0.5);
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
  { return (context.getEntity() instanceof ItemEntity) ? VoxelShapes.empty() : super.getCollisionShape(state, world, pos, context); }
}
