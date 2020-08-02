/*
 * @file BlockCategories.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Oredict based block category cache.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class BlockCategories
{
  private static Set<Block> logs_ = new HashSet<Block>();
  private static Set<Block> leaves_ = new HashSet<Block>();
  private static Set<Block> variant_logs_ = new HashSet<Block>(); // logs that are not checked for log equivalence

  public static final Set<Block> logs()
  { return logs_; } // wrap in case immutable needed one time.

  public static final Set<Block> leaves()
  { return leaves_; }

  public static boolean isLog(IBlockState state)
  {
    final Block block = state.getBlock();
    return (block instanceof BlockLog) || (block instanceof BlockNewLog) || (logs().contains(block));
  }

  public static boolean isLeaves(IBlockState state)
  {
    if(state.getMaterial()==Material.LEAVES) return true;
    final Block block = state.getBlock();
    return (block instanceof BlockLeaves) || (leaves().contains(block));
  }

  public static final boolean isSameLeaves(IBlockState a, IBlockState b)
  {
    if(!isLeaves(a)) return false;
    final Block block = a.getBlock();
    if(block != b.getBlock()) return false;
    if(block instanceof BlockNewLeaf) return a.getValue(BlockNewLeaf.VARIANT) == b.getValue(BlockNewLeaf.VARIANT);
    if(block instanceof BlockOldLeaf) return a.getValue(BlockOldLeaf.VARIANT) == b.getValue(BlockOldLeaf.VARIANT);
    return true;
  }

  public static final boolean isSameLog(IBlockState a, IBlockState b)
  {
    if((!isLog(a)) || (!isLog(b))) return false;
    if(variant_logs_.contains(a.getBlock()) || (variant_logs_.contains(b.getBlock()))) return true;
    if(a.getBlock()!=b.getBlock()) return false;
    if(a.getBlock() instanceof BlockNewLog) return a.getValue(BlockNewLog.VARIANT) == b.getValue(BlockNewLog.VARIANT);
    if(a.getBlock() instanceof BlockOldLog) return a.getValue(BlockOldLog.VARIANT)==b.getValue(BlockOldLog.VARIANT);
    // Uagh, that hurts the heart of performance ...
    final IProperty<?> prop = a.getPropertyKeys().stream().filter( (IProperty<?> p) -> (p.getName().contains("variant") || p.getName().contains("type"))).findFirst().orElse(null);
    if(prop!=null) return a.getValue(prop).equals(b.getValue(prop));
    // All other: We have to assume that there are no variants for this block, and the block type denotes the log type unambiguously.
    return true;
  }

  public static final void reload()
  {
    {
      HashSet<Block> logs = new HashSet<Block>();
      for(final String ore_name : OreDictionary.getOreNames()) {
        if(!ore_name.startsWith("logWood")) continue;
        final List<ItemStack> stacks = OreDictionary.getOres(ore_name, false);
        for(ItemStack stack : stacks) {
          final Item item = stack.getItem();
          if(!(item instanceof ItemBlock)) continue;
          Block block = ((ItemBlock)item).getBlock();
          logs.add(block);
          // @todo: make this configurable
          if(block.getRegistryName().getPath().contains("menril")) variant_logs_.add(block);
        }
      }
      logs_ = logs;
      ModEngineersDecor.logger.info("Found "+logs.size()+" types of 'choppable' log.");
      if(ModConfig.zmisc.with_experimental) {
        for(Block b:logs_) ModEngineersDecor.logger.info(" - choppable log: " + b.getRegistryName());
      }
    }
    {
      HashSet<Block> leaves = new HashSet<Block>();
      for(final String ore_name : OreDictionary.getOreNames()) {
        if(!ore_name.startsWith("treeLeaves")) continue;
        final List<ItemStack> stacks = OreDictionary.getOres(ore_name, false);
        for(ItemStack stack : stacks) {
          final Item item = stack.getItem();
          if(!(item instanceof ItemBlock)) continue;
          leaves.add(((ItemBlock)item).getBlock());
        }
      }
      leaves_ = leaves;
      ModEngineersDecor.logger.info("Found "+leaves.size()+" types of leaves.");
      if(ModConfig.zmisc.with_experimental) {
        for(Block b:leaves_) ModEngineersDecor.logger.info(" - choppable leaf: " + b.getRegistryName());
      }
    }
  }
}
