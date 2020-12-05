/*
 * @file JEIPlugin.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * JEI plugin (see https://github.com/mezz/JustEnoughItems/wiki/Creating-Plugins)
 */
package wile.engineersdecor.eapi.jei;
//public class JEIPlugin {}

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.blocks.EdCraftingTable;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import wile.engineersdecor.blocks.EdCraftingTable.CraftingTableTileEntity;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


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
          EdCraftingTable.CraftingTableContainer.class,
          VanillaRecipeCategoryUid.CRAFTING,
          1, 9, 10, 36+CraftingTableTileEntity.NUM_OF_STORAGE_SLOTS
        );
      } catch(Throwable e) {
        ModEngineersDecor.logger().warn("Exception in JEI crafting table handler registration: '" + e.getMessage() + "'.");
      }
    }
  }

  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
  {
    HashSet<Item> blacklisted = new HashSet<>();
    for(Block e: ModContent.getRegisteredBlocks()) {
      if(ModConfig.isOptedOut(e) && (e.asItem().getRegistryName().getPath()).equals((e.getRegistryName().getPath()))) {
        blacklisted.add(e.asItem());
      }
    }
    for(Item e: ModContent.getRegisteredItems()) {
      if(ModConfig.isOptedOut(e) && (!(e instanceof BlockItem))) {
        blacklisted.add(e);
      }
    }
    if(!blacklisted.isEmpty()) {
      List<ItemStack> blacklist = blacklisted.stream().map(ItemStack::new).collect(Collectors.toList());
      try {
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM, blacklist);
      } catch(Exception e) {
        ModEngineersDecor.logger().warn("Exception in JEI opt-out processing: '" + e.getMessage() + "', skipping further JEI optout processing.");
      }
    }
  }
}
