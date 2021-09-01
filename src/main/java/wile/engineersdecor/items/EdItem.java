/*
 * @file EdItem.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basic item functionality for mod items.
 */
package wile.engineersdecor.items;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EdItem extends Item
{
  public static final Collection<ItemGroup> ENABLED_TABS  = Collections.singletonList(ModEngineersDecor.ITEMGROUP);
  public static final Collection<ItemGroup> DISABLED_TABS = new ArrayList<ItemGroup>();

  public EdItem(Item.Properties properties)
  { super(properties.tab(ModEngineersDecor.ITEMGROUP)); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public Collection<ItemGroup> getCreativeTabs()
  { return ModConfig.isOptedOut(this) ? (DISABLED_TABS) : (ENABLED_TABS); }

}
