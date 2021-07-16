/*
 * @file EdDoorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Blocks representing centered doors opening by sliding
 * to the sides.
 */
package wile.engineersdecor.blocks;

import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.block.*;
import wile.engineersdecor.libmc.blocks.StandardDoorBlock;

public class EdDoorBlock extends StandardDoorBlock implements IDecorBlock
{
  public EdDoorBlock(long config, AbstractBlock.Properties properties, AxisAlignedBB[] open_aabbs_top, AxisAlignedBB[] open_aabbs_bottom, AxisAlignedBB[] closed_aabbs_top, AxisAlignedBB[] closed_aabbs_bottom, SoundEvent open_sound, SoundEvent close_sound)
  { super(config, properties, open_aabbs_top, open_aabbs_bottom, closed_aabbs_top, closed_aabbs_bottom, open_sound, close_sound); }

  public EdDoorBlock(long config, AbstractBlock.Properties properties, AxisAlignedBB open_aabb, AxisAlignedBB closed_aabb, SoundEvent open_sound, SoundEvent close_sound)
  { super(config, properties, open_aabb, closed_aabb, open_sound, close_sound); }

  public EdDoorBlock(long config, AbstractBlock.Properties properties, SoundEvent open_sound, SoundEvent close_sound)
  { super(config, properties, open_sound, close_sound); }

  public EdDoorBlock(long config, AbstractBlock.Properties properties)
  { super(config, properties); }

}
