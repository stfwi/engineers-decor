/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import wile.engineersdecor.libmc.blocks.StandardFenceBlock;
import net.minecraft.block.*;

public class BlockDecorFence extends StandardFenceBlock implements IDecorBlock
{
  public BlockDecorFence(long config, Block.Properties properties)
  { super(config, properties); }

  public BlockDecorFence(long config, Block.Properties properties, double pole_width, double pole_height, double side_width, double side_max_y, double side_min_y)
  { super(config, properties, pole_width, pole_height, side_width, side_max_y, side_min_y); }

  @Override
  protected boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  { return ((facingState.getBlock()) instanceof BlockDecorDoubleGate) || super.attachesTo(facingState, world, facingPos, side); }

}
