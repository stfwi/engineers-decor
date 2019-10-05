/*
 * @file ModLootTables.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Loot table generator.
 */
package wile.engineersdecor.datagen;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.blocks.IDecorBlock;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.Block;
import net.minecraft.data.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.functions.CopyName;
import net.minecraft.world.storage.loot.functions.CopyName.Source;
import net.minecraft.world.storage.loot.functions.CopyNbt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ModLootTables extends LootTableProvider
{
  private static final Logger LOGGER = LogManager.getLogger();
  private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
  private final DataGenerator generator;

  //--------------------------------------------------------------------------------------------------------------------

  public ModLootTables(DataGenerator gen)
  { super(gen); generator=gen; }

  //-- LootTableProvider -----------------------------------------------------------------------------------------------

  @Override
  public String getName()
  { return ModAuxiliaries.MODID + " Loot Tables"; }

  @Override
  public void act(DirectoryCache cache)
  { save(cache, generate()); }

  //--------------------------------------------------------------------------------------------------------------------

  private Map<ResourceLocation, LootTable> generate()
  {
    final HashMap<ResourceLocation, LootTable> tables = new HashMap<ResourceLocation, LootTable>();
    final List<Block> blocks = ModContent.allBlocks();
    blocks.forEach((block)->{
      if((!(block instanceof IDecorBlock)) || (!(((IDecorBlock)block).hasDynamicDropList()))) {
        tables.put(
          block.getLootTable(),
          defaultBlockDrops(block.getRegistryName().getPath() + "_dlt", block)
            .setParameterSet(LootParameterSets.BLOCK).build());
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
    iltb.acceptFunction(CopyName.func_215893_a(Source.BLOCK_ENTITY));
    if(block.hasTileEntity(block.getDefaultState())) {
      iltb.acceptFunction(CopyNbt.func_215881_a(CopyNbt.Source.BLOCK_ENTITY));
    }
    return LootTable.builder().addLootPool(LootPool.builder().name(rl_path).rolls(ConstantRange.of(1)).addEntry(iltb));
  }

}
