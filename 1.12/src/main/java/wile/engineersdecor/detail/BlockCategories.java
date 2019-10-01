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
  { return (a.getBlock() == b.getBlock()); }

  public static final boolean isSameLog(IBlockState a, IBlockState b)
  {
    // very strange  ...
    if(a.getBlock()!=b.getBlock()) {
      return false;
    } else if(a.getBlock() instanceof BlockNewLog) {
      return a.getValue(BlockNewLog.VARIANT) == b.getValue(BlockNewLog.VARIANT);
    } else if(a.getBlock() instanceof BlockOldLog) {
      return a.getValue(BlockOldLog.VARIANT) == b.getValue(BlockOldLog.VARIANT);
    } else {
      // Uagh, that hurts the heart of performance ...
      final IProperty<?> prop = a.getPropertyKeys().stream().filter( (IProperty<?> p) -> (p.getName().contains("variant") || p.getName().contains("type"))).findFirst().orElse(null);
      if(prop==null) return false;
      return a.getValue(prop).equals(b.getValue(prop));
    }
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
          logs.add(((ItemBlock)item).getBlock());
        }
      }
      logs_ = logs;
      ModEngineersDecor.logger.info("Found "+logs.size()+" types of 'choppable' log.");
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
    }
  }
}
