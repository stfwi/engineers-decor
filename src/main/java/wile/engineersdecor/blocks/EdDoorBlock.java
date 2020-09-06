/*
 * @file EdDoorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Door blocks.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardDoorBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.block.*;


public class EdDoorBlock extends StandardDoorBlock implements IDecorBlock
{
  public EdDoorBlock(long config, Block.Properties properties)
  { super(config, properties); }
}
