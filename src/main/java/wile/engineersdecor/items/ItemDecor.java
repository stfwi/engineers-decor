/*
 * @file ItemDecor.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Basic item functionality for mod items.
 */
package wile.engineersdecor.items;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import wile.engineersdecor.detail.ModConfig;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDecor extends ItemBlock
{
  public ItemDecor(Block block)
  {
    super(block);
    setRegistryName(block.getRegistryName());
    setTranslationKey(ModEngineersDecor.MODID + "." + block.getRegistryName().getPath());
    setMaxStackSize(64);
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setHasSubtypes(false);
  }

  @SideOnly(Side.CLIENT)
  public void initModel()
  {
    ModelResourceLocation rc = new ModelResourceLocation(getRegistryName(),"inventory");
    ModelBakery.registerItemVariants(this, rc);
    ModelLoader.setCustomMeshDefinition(this, stack->rc);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Nullable
  public CreativeTabs getCreativeTab()
  { return ModConfig.isOptedOut(this) ? null : super.getCreativeTab(); }
}
