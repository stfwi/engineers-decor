/*
 * @file EdSlabBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.VariantSlabBlock;
import net.minecraft.block.*;

public class EdSlabBlock extends VariantSlabBlock implements IDecorBlock
{
  public EdSlabBlock(long config, AbstractBlock.Properties builder)
  { super(config, builder); }
}
