/*
 * @file BlockDecorWall.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardFenceBlock;
import net.minecraft.block.*;

public class BlockDecorFence extends StandardFenceBlock implements IDecorBlock
{
  public BlockDecorFence(long config, Block.Properties builder)
  { super(config, builder); }
}
