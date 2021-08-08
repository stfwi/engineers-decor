/*
 * @file DecorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 * Mainly needed for:
 * - MC block defaults.
 * - Tooltip functionality
 * - Model initialisation
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardBlocks;

public class DecorBlock
{
  public static final long CFG_DEFAULT                    = StandardBlocks.CFG_DEFAULT;
  public static final long CFG_CUTOUT                     = StandardBlocks.CFG_CUTOUT;
  public static final long CFG_MIPPED                     = StandardBlocks.CFG_MIPPED;
  public static final long CFG_TRANSLUCENT                = StandardBlocks.CFG_TRANSLUCENT;
  public static final long CFG_WATERLOGGABLE              = StandardBlocks.CFG_WATERLOGGABLE;
  public static final long CFG_HORIZIONTAL                = StandardBlocks.CFG_HORIZIONTAL;
  public static final long CFG_LOOK_PLACEMENT             = StandardBlocks.CFG_LOOK_PLACEMENT;
  public static final long CFG_FACING_PLACEMENT           = StandardBlocks.CFG_FACING_PLACEMENT;
  public static final long CFG_OPPOSITE_PLACEMENT         = StandardBlocks.CFG_OPPOSITE_PLACEMENT;
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME;
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK;
  public static final long CFG_STRICT_CONNECTIONS         = StandardBlocks.CFG_STRICT_CONNECTIONS;
  public static final long CFG_AI_PASSABLE                = StandardBlocks.CFG_AI_PASSABLE;
  public static final long CFG_HARD_IE_DEPENDENT          = 0x8000000000000000L;
  @Deprecated public static final long CFG_EXPERIMENTAL   = 0x4000000000000000L;
}
