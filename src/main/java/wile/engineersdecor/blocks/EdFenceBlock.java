/*
 * @file EdFenceBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardFenceBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.block.*;


public class EdFenceBlock extends StandardFenceBlock implements IDecorBlock
{
  public EdFenceBlock(long config, AbstractBlock.Properties properties)
  { super(config, properties); }

  public EdFenceBlock(long config, AbstractBlock.Properties properties, double pole_width, double pole_height, double side_width, double side_min_y, double side_max_low_y, double side_max_tall_y)
  { super(config, properties, pole_width, pole_height, side_width, side_min_y, side_max_low_y, side_max_tall_y); }

  @Override
  protected boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  { return ((facingState.getBlock()) instanceof EdDoubleGateBlock) || super.attachesTo(facingState, world, facingPos, side); }
}
