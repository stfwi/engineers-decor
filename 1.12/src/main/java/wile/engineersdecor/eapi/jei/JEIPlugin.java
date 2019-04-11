/*
 * @file JEIPlugin.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * JEI plugin (see https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins)
 */
package wile.engineersdecor.eapi.jei;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.ModBlocks;
import wile.engineersdecor.detail.ModConfig;

@mezz.jei.api.JEIPlugin
public class JEIPlugin implements mezz.jei.api.IModPlugin
{
  @Override
  @SuppressWarnings("deprecation")
  public void register(mezz.jei.api.IModRegistry registry)
  {
    try {
      for(Block e:ModBlocks.getRegisteredBlocks()) {
        if(ModConfig.isOptedOut(e)) {
          ItemStack stack = new ItemStack(Item.getItemFromBlock(e));
          if(stack != null) {
            if(!registry.getJeiHelpers().getIngredientBlacklist().isIngredientBlacklisted(stack)) {
              registry.getJeiHelpers().getIngredientBlacklist().addIngredientToBlacklist(stack);
            }
            if(!registry.getJeiHelpers().getItemBlacklist().isItemBlacklisted(stack)) {
              registry.getJeiHelpers().getItemBlacklist().addItemToBlacklist(stack);
            }
          }
        }
      }
    } catch(Throwable e) {
      ModEngineersDecor.logger.warn("Exception in JEI opt-out processing: '" + e.getMessage() + "', skipping further JEI processing.");
    }
  }
}
