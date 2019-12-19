/*
 * @file IDecorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Interface for tagging and common default behaviour.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.List;


public interface IDecorBlock
{
  enum RenderTypeHint { SOLID,CUTOUT,CUTOUT_MIPPED,TRANSLUCENT }

  default boolean hasDynamicDropList()
  { return false; }

  default List<ItemStack> dropList(BlockState state, World world, BlockPos pos, boolean explosion)
  { return Collections.singletonList((!world.isRemote()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY)); }

  default RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.SOLID; }

}
