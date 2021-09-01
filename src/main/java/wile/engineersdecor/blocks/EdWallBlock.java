/*
 * @file EdWallBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import wile.engineersdecor.libmc.blocks.VariantWallBlock;


public class EdWallBlock extends VariantWallBlock implements IDecorBlock
{
  public EdWallBlock(long config, AbstractBlock.Properties builder)
  { super(config, builder); }

  protected boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  {
    if(facingState==null) return false;
    if(super.attachesTo(facingState, world, facingPos, side)) return true;
    if(facingState.getBlock() instanceof EdWindowBlock) return true;
    if(facingState.getBlock() instanceof PaneBlock) return true;
    return false;
  }
}
