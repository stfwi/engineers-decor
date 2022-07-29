/*
 * @file EdWallBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import wile.engineersdecor.libmc.VariantWallBlock;


public class EdWallBlock extends VariantWallBlock
{
  public EdWallBlock(long config, BlockBehaviour.Properties builder)
  { super(config, builder); }

  protected boolean attachesTo(BlockState facingState, LevelReader world, BlockPos facingPos, Direction side)
  {
    if(facingState==null) return false;
    if(super.attachesTo(facingState, world, facingPos, side)) return true;
    if(facingState.getBlock() instanceof EdWindowBlock) return true;
    if(facingState.getBlock() instanceof IronBarsBlock) return true;
    if(facingState.getBlock() instanceof StainedGlassPaneBlock) return true;
    return false;
  }
}
