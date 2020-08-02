/*
 * @file EdStairsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stairs and roof blocks, almost entirely based on vanilla stairs.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardStairsBlock;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;

public class EdStairsBlock extends StandardStairsBlock implements IDecorBlock
{
  public EdStairsBlock(long config, BlockState state, Block.Properties properties)
  { super(config, state, properties); }

  public EdStairsBlock(long config, java.util.function.Supplier<BlockState> state, Block.Properties properties)
  { super(config, state, properties); }
}
