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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class BlockDecorFloorGrating extends BlockDecor
{

  public BlockDecorFloorGrating(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }


//  @Override
//  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState)
//  { if(!(entity instanceof EntityItem)) super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState); }
//
//  @Override
//  public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance)
//  {
//    if(!(entity instanceof EntityItem)) {
//      entity.fall(fallDistance, 1.0F);
//    } else {
//      entity.setVelocity(0,-0.2,0);
//    }
//  }
//
//  @Override
//  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity)
//  {
//    if(!(entity instanceof EntityItem)) return;
//    if((entity.posY-pos.getY()) > 0.7) {
//      if(entity.motionY > -0.2) entity.motionY = -0.2;
//      entity.setVelocity(0,-0.1,0);
//      entity.setPositionAndUpdate(entity.posX, entity.posY-0.3, entity.posZ);
//    } else {
//      entity.setVelocity(0,entity.motionY,0);
//    }
//  }
}
