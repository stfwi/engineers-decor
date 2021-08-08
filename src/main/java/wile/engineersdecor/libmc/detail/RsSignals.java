/*
 * @file RsSignals.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General redstone signal related functionality.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class RsSignals
{

  public static boolean hasSignalConnector(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction realSide)
  {
    return state.isSignalSource();
  }

  public static int fromContainer(@Nullable Container container)
  {
    if(container == null) return 0;
    final double max = container.getMaxStackSize();
    if(max <= 0) return 0;
    boolean nonempty = false;
    double fill_level = 0;
    for(int i=0; i<container.getContainerSize(); ++i) {
      ItemStack stack = container.getItem(i);
      if(stack.isEmpty() || (stack.getMaxStackSize()<=0)) continue;
      fill_level += ((double)stack.getCount()) / Math.min(max, stack.getMaxStackSize());
      nonempty = true;
    }
    fill_level /= container.getContainerSize();
    return (int)(Math.floor(fill_level * 14) + (nonempty?1:0)); // vanilla compliant calculation.
  }

}
