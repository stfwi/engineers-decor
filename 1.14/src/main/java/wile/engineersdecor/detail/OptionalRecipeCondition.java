/*
 * @file OptionalRecipeCondition.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Recipe condition to enable opt'ing out JSON based recipes.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class OptionalRecipeCondition implements ICondition
{
  private static final ResourceLocation NAME = new ResourceLocation(ModEngineersDecor.MODID, "optional");

  private final List<ResourceLocation> all_required;
  private final List<ResourceLocation> any_missing;
  private final @Nullable ResourceLocation result;
  private final boolean experimental;

  public OptionalRecipeCondition(ResourceLocation result, List<ResourceLocation> required, List<ResourceLocation> missing, boolean isexperimental)
  { all_required = required; any_missing = missing; this.result = result; experimental=isexperimental; }

  @Override
  public ResourceLocation getID()
  { return NAME; }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Optional recipe, all-required: [");
    for(ResourceLocation e:all_required) sb.append(e.toString()).append(",");
    sb.delete(sb.length()-1, sb.length()).append("], any-missing: [");
    for(ResourceLocation e:any_missing) sb.append(e.toString()).append(",");
    sb.delete(sb.length()-1, sb.length()).append("]");
    if(experimental) sb.append(" EXPERIMENTAL");
    return sb.toString();
  }

  @Override
  public boolean test()
  {
    if((experimental) && (!ModConfig.withExperimental())) return false;
    final IForgeRegistry<Block> block_registry = ForgeRegistries.BLOCKS;
    final IForgeRegistry<Item> item_registry = ForgeRegistries.ITEMS;
    if(result != null) {
      if((!block_registry.containsKey(result)) && (!item_registry.containsKey(result))) return false; // required result not registered
    }
    if(!all_required.isEmpty()) {
      for(ResourceLocation rl:all_required) {
        if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return false;
      }
    }
    if(!any_missing.isEmpty()) {
      for(ResourceLocation rl:any_missing) {
        // At least one item missing, enable this recipe as alternative recipe for another one that check the missing item as required item.
        // --> e.g. if IE not installed there is no slag. One recipe requires slag, and another one (for the same result) is used if there
        //     is no slag.
        if((!block_registry.containsKey(rl)) && (!item_registry.containsKey(rl))) return true;
      }
      return false;
    }
    return true;
  }

  public static class Serializer implements IConditionSerializer<OptionalRecipeCondition>
  {
    public static final Serializer INSTANCE = new Serializer();

    @Override
    public ResourceLocation getID()
    { return OptionalRecipeCondition.NAME; }

    @Override
    public void write(JsonObject json, OptionalRecipeCondition condition)
    {
      JsonArray required = new JsonArray();
      JsonArray missing = new JsonArray();
      for(ResourceLocation e:condition.all_required) required.add(e.toString());
      for(ResourceLocation e:condition.any_missing) missing.add(e.toString());
      json.add("required", required);
      json.add("missing", missing);
      if(condition.result != null) json.addProperty("result", condition.result.toString());
    }

    @Override
    public OptionalRecipeCondition read(JsonObject json)
    {
      List<ResourceLocation> required = new ArrayList<>();
      List<ResourceLocation> missing = new ArrayList<>();
      ResourceLocation result = null;
      boolean experimental = false;
      if(json.has("result")) result = new ResourceLocation(json.get("result").getAsString());
      if(json.has("required")) {
        for(JsonElement e:JSONUtils.getJsonArray(json, "required")) required.add(new ResourceLocation(e.getAsString()));
      }
      if(json.has("missing")) {
        for(JsonElement e:JSONUtils.getJsonArray(json, "missing")) missing.add(new ResourceLocation(e.getAsString()));
      }
      if(json.has("experimental")) experimental = json.get("experimental").getAsBoolean();
      return new OptionalRecipeCondition(result, required, missing, experimental);
    }
  }
}
