/*
 * @file ModRecipes.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General handling auxiliaries for mod recipes.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.BlockDecorFurnace;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.item.ItemStack;
import java.util.*;


public class ModRecipes
{
  public static final void furnaceRecipeOverrideReset()
  { BlockDecorFurnace.BRecipes.instance().reset(); }

  public static final void furnaceRecipeOverrideSmeltsOresToNuggets()
  {
    try {
      ArrayList<String> ores    = new ArrayList<String>();
      ArrayList<String> ingots  = new ArrayList<String>();
      ArrayList<String> nuggets = new ArrayList<String>();
      String[] names = OreDictionary.getOreNames();
      for(String name:names) {
        if(name.startsWith("ore")) ores.add(name);
        if(name.startsWith("ingot")) ingots.add(name);
        if(name.startsWith("nugget")) nuggets.add(name);
      }
      for(String ore_name:ores) {
        final String ingot_name = ore_name.replace("ore", "ingot");
        if(!ingots.contains(ingot_name)) continue;
        final String nugget_name = ore_name.replace("ore", "nugget");
        if(!nuggets.contains(nugget_name)) continue;
        final NonNullList<ItemStack> ore_list = OreDictionary.getOres(ore_name, false);
        final NonNullList<ItemStack> ingot_list = OreDictionary.getOres(ingot_name, false);
        final NonNullList<ItemStack> nugget_list = OreDictionary.getOres(nugget_name, false);
        if(ore_list.isEmpty() || ingot_list.isEmpty() || nugget_list.isEmpty()) continue;
        final ItemStack ore_stack = ore_list.get(0);
        final ItemStack ingot_stack = ingot_list.get(0);
        ItemStack nugget_stack = nugget_list.get(0);
        for(ItemStack stack:nugget_list) {
          if(stack.getItem().getRegistryName().getNamespace() == "immersiveengineering") {
            nugget_stack = stack;
            break;
          }
        }
        if(ore_stack.isEmpty() || ingot_stack.isEmpty() || nugget_stack.isEmpty()) continue;
        if(FurnaceRecipes.instance().getSmeltingResult(ore_stack).getItem().equals(ingot_stack.getItem())) {
          final float xp = FurnaceRecipes.instance().getSmeltingExperience(ore_stack);
          BlockDecorFurnace.BRecipes.instance().add(ore_stack, nugget_stack, xp);
          ModEngineersDecor.logger.info("Lab furnace override: " + ore_name + " -> " + nugget_name);
        }
      }
    } catch(Throwable e) {
      ModEngineersDecor.logger.error("Lab furnace override failed with exception, skipping further override processing.");
      ModEngineersDecor.logger.error("Exception is: ", e);
    }
  }

}
