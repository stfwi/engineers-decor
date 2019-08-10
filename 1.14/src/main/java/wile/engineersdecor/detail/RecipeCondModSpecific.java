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

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IConditionSerializer;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.*;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;


public class RecipeCondModSpecific implements IConditionSerializer
{
  public static final BooleanSupplier RECIPE_INCLUDE = ()->true;
  public static final BooleanSupplier RECIPE_EXCLUDE = ()->false;

  @Override
  public @Nonnull BooleanSupplier parse(@Nullable JsonObject json) {
    try {
      if(json==null) return RECIPE_EXCLUDE;
      final IForgeRegistry<Block> block_registry = ForgeRegistries.BLOCKS;
      final IForgeRegistry<Item> item_registry = ForgeRegistries.ITEMS;
      final JsonArray items = json.getAsJsonArray("required");
      if(items!=null) {
        for(JsonElement e: items) {
          if(!e.isJsonPrimitive()) continue;
          final ResourceLocation rl = new ResourceLocation(((JsonPrimitive)e).getAsString());
          if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return RECIPE_EXCLUDE; // required item not registered
        }
      }
      final JsonPrimitive result = json.getAsJsonPrimitive("result");
      if(result != null) {
        final ResourceLocation rl = new ResourceLocation(result.getAsString());
        if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return RECIPE_EXCLUDE; // required result not registered
      }
      final JsonArray missing = json.getAsJsonArray("missing");
      if((missing!=null) && (missing.size() > 0)) {
        for(JsonElement e: missing) {
          if(!e.isJsonPrimitive()) continue;
          final ResourceLocation rl = new ResourceLocation(((JsonPrimitive)e).getAsString());
          // At least one item missing, enable this recipe as alternative recipe for another one that check the missing item as required item.
          // --> e.g. if IE not installed there is no slag. One recipe requires slag, and another one (for the same result) is used if there
          //     is no slag.
          if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return RECIPE_INCLUDE;
        }
        return RECIPE_EXCLUDE; // all required there, but there is no item missing, so another recipe
      } else {
        return RECIPE_INCLUDE; // no missing given, means include if result and required are all there.
      }
    } catch(Throwable ex) {
      ModAuxiliaries.logError("rsgauges::ResultRegisteredCondition failed: " + ex.toString());
    }
    return RECIPE_EXCLUDE; // skip on exception.
  }
}
