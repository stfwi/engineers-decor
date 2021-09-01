/*
 * @file EdSlabSliceBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Half slab ("slab slices") characteristics class. Actually
 * it's now a quater slab, but who cares.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.SlabSliceBlock;
import net.minecraft.block.AbstractBlock;

public class EdSlabSliceBlock extends SlabSliceBlock implements IDecorBlock
{
  public EdSlabSliceBlock(long config, AbstractBlock.Properties builder)
  { super(config, builder); }
}
