/*
 * @file JEIPlugin.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * JEI plugin (see https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins)
 */
package wile.engineersdecor.eapi.jei;

import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.BlockDecorCraftingTable;
import wile.engineersdecor.detail.ModConfig;
import wile.engineersdecor.ModContent;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

@mezz.jei.api.JeiPlugin
public class JEIPlugin implements mezz.jei.api.IModPlugin
{
  @Override
  public ResourceLocation getPluginUid()
  { return new ResourceLocation(ModEngineersDecor.MODID, "jei_plugin_uid"); }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration)
  {
    if(!ModConfig.without_crafting_table) {
      try {
        registration.addRecipeTransferHandler(
          BlockDecorCraftingTable.BContainer.class,
          VanillaRecipeCategoryUid.CRAFTING,
          1, 9, 10, 44
        );
      } catch(Throwable e) {
        ModEngineersDecor.logger().warn("Exception in JEI crafting table handler registration: '" + e.getMessage() + "'.");
      }
    }
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
  {
    List<ItemStack> blacklisted = new ArrayList<>();
    for(Block e: ModContent.getRegisteredBlocks()) {
      if(ModConfig.isOptedOut(e)) {
        blacklisted.add(new ItemStack(e.asItem()));
      }
    }
    if(!blacklisted.isEmpty()) {
      try {
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM, blacklisted);
      } catch(Exception e) {
        ModEngineersDecor.logger().warn("Exception in JEI opt-out processing: '" + e.getMessage() + "', skipping further JEI optout processing.");
      }
    }
  }
}
