/*
 * @file JEIPlugin.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * JEI plugin (see https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins)
 */
package wile.engineersdecor.eapi.jei;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.blocks.*;
import wile.engineersdecor.detail.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@mezz.jei.api.JEIPlugin
public class JEIPlugin implements mezz.jei.api.IModPlugin
{
  @Override
  @SuppressWarnings("deprecation")
  public void register(mezz.jei.api.IModRegistry registry)
  {
    // Block/item hiding
    try {
      for(Block e:ModContent.getRegisteredBlocks()) {
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
      ModEngineersDecor.logger.warn("Exception in JEI opt-out processing: '" + e.getMessage() + "', skipping further JEI optout processing.");
    }
    // Crafting table registration
    if(!ModConfig.optout.without_crafting_table) {
      try {
        mezz.jei.api.recipe.transfer.IRecipeTransferRegistry recipeTranferRegistry = registry.getRecipeTransferRegistry();
        recipeTranferRegistry.addRecipeTransferHandler(
          BlockDecorCraftingTable.BContainer.class,
          mezz.jei.api.recipe.VanillaRecipeCategoryUid.CRAFTING,
          1, 9, 10, 44
        );
      } catch(Throwable e) {
        ModEngineersDecor.logger.warn("Exception in JEI crafting table handler registration: '" + e.getMessage() + "'.");
      }
    }
  }
}
