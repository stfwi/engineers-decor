/*
 * @file DataFixing.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Data fixing and mapping correction functionality encapsulation.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class DataFixing
{
  private static String modid = "";
  private static Map<String, String> item_registry_renaming = new HashMap<>();
  private static Map<String, String> block_registry_renaming = new HashMap<>();

  public static void init(String mod_id, @Nullable Map<String, String> item_renaming, @Nullable Map<String, String> block_renaming)
  {
    modid = mod_id;
    block_registry_renaming = new HashMap<>();
    item_registry_renaming = new HashMap<>();
    if(item_renaming!=null) item_registry_renaming.putAll(item_renaming);
    if(block_renaming!=null) { block_registry_renaming.putAll(block_renaming); item_registry_renaming.putAll(block_renaming); }
  }

  public static void onDataFixMissingItemMapping(net.minecraftforge.event.RegistryEvent.MissingMappings<Item> event)
  {
    // Handler registered in main mod event subscription.
    for(Mapping<Item> mapping: event.getMappings()) {
      if(mapping.key.getNamespace() != modid) continue;
      final String rm =  item_registry_renaming.getOrDefault(mapping.key.getPath(), "");
      if(rm.isEmpty()) continue;
      final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, rm));
      if((item==null) || (item==Items.AIR)) continue;
      mapping.remap(item);
    }
  }

  public static void onDataFixMissingBlockMapping(net.minecraftforge.event.RegistryEvent.MissingMappings<Block> event)
  {
    // Handler registered in main mod event subscription.
    for(Mapping<Block> mapping: event.getMappings()) {
      if(mapping.key.getNamespace() != modid) continue;
      final String rm =  block_registry_renaming.getOrDefault(mapping.key.getPath(), "");
      if(rm.isEmpty()) continue;
      final Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(modid, rm));
      if((block==null) || (block==Blocks.AIR)) continue;
      mapping.remap(block);
    }
  }

  // @todo: Find a way to register blockstate data fixing.

}
