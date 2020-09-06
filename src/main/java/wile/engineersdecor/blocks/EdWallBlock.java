/*
 * @file EdWallBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.VariantWallBlock;
import net.minecraft.block.*;

public class EdWallBlock extends VariantWallBlock implements IDecorBlock
{
  public EdWallBlock(long config, Block.Properties builder)
  { super(config, builder); }
}
