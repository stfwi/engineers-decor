/*
 * @file RecipeCondRegistered.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Recipe condition to enable opt'ing out JSON based recipes, referenced
 * in assets/engineersdecor/recipes/_factories.json with full path (therefore
 * I had to make a separate file for that instead of a few lines in
 * ModAuxiliaries).
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import com.google.gson.*;
import java.util.function.BooleanSupplier;


public class RecipeCondModSpecific implements IConditionFactory
{
  public static final BooleanSupplier RECIPE_INCLUDE = ()->true;
  public static final BooleanSupplier RECIPE_EXCLUDE = ()->false;

  public static int num_skipped = 0;

  private static BooleanSupplier exclude()
  { ++num_skipped; return RECIPE_EXCLUDE; }

  @Override
  public BooleanSupplier parse(JsonContext context, JsonObject json) {
    if(ModConfig.isWithoutRecipes()) return exclude();
    try {
      final JsonPrimitive experimental = json.getAsJsonPrimitive("experimental");
      if((experimental!=null) && (experimental.getAsBoolean())) {
        if(!ModConfig.zmisc.with_experimental) return exclude();
      }
      final IForgeRegistry<Block> block_registry = ForgeRegistries.BLOCKS;
      final IForgeRegistry<Item> item_registry = ForgeRegistries.ITEMS;
      final JsonArray items = json.getAsJsonArray("required");
      if(items!=null) {
        for(JsonElement e: items) {
          if(!e.isJsonPrimitive()) continue;
          final ResourceLocation rl = new ResourceLocation(((JsonPrimitive)e).getAsString());
          if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return exclude(); // required item not registered
        }
      }
      final JsonPrimitive result = json.getAsJsonPrimitive("result");
      if(result != null) {
        final ResourceLocation rl = new ResourceLocation(result.getAsString());
        if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return exclude(); // required result not registered
        if(ModConfig.isOptedOut(block_registry.getValue(rl))) return exclude(); // disabled in mod config
      }
      final JsonArray missing = json.getAsJsonArray("missing");
      if((missing!=null) && (missing.size() > 0)) {
        int num_missing = 0;
        for(JsonElement e: missing) {
          if(!e.isJsonPrimitive()) continue;
          final ResourceLocation rl = new ResourceLocation(((JsonPrimitive)e).getAsString());
          if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) ++num_missing;
        }
        // At least one item missing, enable this recipe as alternative recipe for another one that check the missing item as required item.
        // --> e.g. if IE not installed there is no slag. One recipe requires slag, and another one (for the same result) is used if there
        //     is no slag.
        return (num_missing == 0) ? (exclude()) : (RECIPE_INCLUDE);
      } else {
        return RECIPE_INCLUDE; // no missing given, means include if result and required are all there.
      }
    } catch(Throwable ex) {
      ModEngineersDecor.logger.error("ResultRegisteredCondition failed: " + ex.toString());
    }
    return exclude(); // skip on exception.
  }
}
