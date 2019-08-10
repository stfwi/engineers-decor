/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class. Explicitly overrides some
 * `Block` methods to return faster due to exclusive block properties.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;

public class BlockDecorFull extends BlockDecor
{
  public BlockDecorFull(long config, Block.Properties properties)
  { super(config, properties); }
}
