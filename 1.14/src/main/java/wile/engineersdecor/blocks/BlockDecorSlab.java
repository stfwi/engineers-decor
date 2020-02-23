/*
 * @file BlockDecorSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.VariantSlabBlock;
import net.minecraft.block.*;

public class BlockDecorSlab extends VariantSlabBlock implements IDecorBlock
{
  public BlockDecorSlab(long config, Block.Properties builder)
  { super(config, builder); }
}
