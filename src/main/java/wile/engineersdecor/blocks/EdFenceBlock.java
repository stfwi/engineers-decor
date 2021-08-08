/*
 * @file EdFenceBlock.java
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import wile.engineersdecor.libmc.blocks.StandardFenceBlock;


public class EdFenceBlock extends StandardFenceBlock
{
  public EdFenceBlock(long config, BlockBehaviour.Properties properties)
  { super(config, properties); }

  public EdFenceBlock(long config, BlockBehaviour.Properties properties, double pole_width, double pole_height, double side_width, double side_min_y, double side_max_low_y, double side_max_tall_y)
  { super(config, properties, pole_width, pole_height, side_width, side_min_y, side_max_low_y, side_max_tall_y); }

  @Override
  protected boolean attachesTo(BlockState facingState, LevelReader world, BlockPos facingPos, Direction side)
  { return ((facingState.getBlock()) instanceof EdDoubleGateBlock) || super.attachesTo(facingState, world, facingPos, side); }
}
