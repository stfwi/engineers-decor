/*
 * @file LootTableGen.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Loot table generator.
 */
package wile.engineersdecor.libmc.datagen;

import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.block.Block;
import net.minecraft.data.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.loot.*;
import net.minecraft.loot.functions.CopyName;
import net.minecraft.loot.functions.CopyName.Source;
import net.minecraft.loot.functions.CopyNbt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class LootTableGen extends LootTableProvider
{
  private static final Logger LOGGER = LogManager.getLogger();
  private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
  private final DataGenerator generator;
  private final Supplier<List<Block>> block_listing;

  //--------------------------------------------------------------------------------------------------------------------

  public LootTableGen(DataGenerator gen, Supplier<List<Block>> block_list_supplier)
  { super(gen); generator=gen; block_listing = block_list_supplier; }

  //-- LootTableProvider -----------------------------------------------------------------------------------------------

  @Override
  public String getName()
  { return Auxiliaries.modid() + " Loot Tables"; }

  @Override
  public void act(DirectoryCache cache)
  { save(cache, generate()); }

  //--------------------------------------------------------------------------------------------------------------------

  private Map<ResourceLocation, LootTable> generate()
  {
    final HashMap<ResourceLocation, LootTable> tables = new HashMap<ResourceLocation, LootTable>();
    final List<Block> blocks = block_listing.get();
    blocks.forEach((block)->{
      LOGGER.info("Generating loot table for " + block.getRegistryName());
      if((!(block instanceof StandardBlocks.IStandardBlock)) || (!(((StandardBlocks.IStandardBlock)block).hasDynamicDropList()))) {
        tables.put(
          block.getLootTable(),
          defaultBlockDrops(block.getRegistryName().getPath() + "_dlt", block)
            .setParameterSet(LootParameterSets.BLOCK).build());
      } else {
        LOGGER.info("Dynamic drop list, skipping loot table for " + block.getRegistryName());
      }
    });
    return tables;
  }

  private void save(DirectoryCache cache, Map<ResourceLocation, LootTable> tables)
  {
    final Path root = generator.getOutputFolder();
    tables.forEach((rl,tab)->{
      Path fp = root.resolve("data/" + rl.getNamespace() + "/loot_tables/" + rl.getPath() + ".json");
      try {
        IDataProvider.save(GSON, cache, LootTableManager.toJson(tab), fp);
      } catch(Exception e) {
        LOGGER.error("Failed to save loottable '"+fp+"', exception: " + e);
      }
    });
  }

  private LootTable.Builder defaultBlockDrops(String rl_path, Block block)
  {
    ItemLootEntry.Builder iltb = ItemLootEntry.builder(block);
    iltb.acceptFunction(CopyName.builder(Source.BLOCK_ENTITY));
    if(block.hasTileEntity(block.getDefaultState())) {
      iltb.acceptFunction(CopyNbt.builder(CopyNbt.Source.BLOCK_ENTITY));
    }
    return LootTable.builder().addLootPool(LootPool.builder().name(rl_path).rolls(ConstantRange.of(1)).addEntry(iltb));
  }

}
