/*
 * @file EdItem.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basic item functionality for mod items.
 */
package wile.engineersdecor.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EdItem extends Item
{
  public static final Collection<CreativeModeTab> ENABLED_TABS  = Collections.singletonList(ModEngineersDecor.ITEMGROUP);
  public static final Collection<CreativeModeTab> DISABLED_TABS = new ArrayList<CreativeModeTab>();

  public EdItem(Item.Properties properties)
  { super(properties.tab(ModEngineersDecor.ITEMGROUP)); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public Collection<CreativeModeTab> getCreativeTabs()
  { return ModConfig.isOptedOut(this) ? (DISABLED_TABS) : (ENABLED_TABS); }

}
