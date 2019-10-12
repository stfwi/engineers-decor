/*
 * @file ModAuxiliaries.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General commonly used functionality.
 */
package wile.engineersdecor.detail;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModAuxiliaries
{
  /**
   * Text localisation wrapper, implicitly prepends `ModEngineersDecor.MODID` to the
   * translation keys. Forces formatting argument, nullable if no special formatting shall be applied..
   */
  public static TextComponentTranslation localizable(String modtrkey, @Nullable TextFormatting color, Object... args)
  {
    TextComponentTranslation tr = new TextComponentTranslation(ModEngineersDecor.MODID+"."+modtrkey, args);
    if(color!=null) tr.getStyle().setColor(color);
    return tr;
  }

  @SideOnly(Side.CLIENT)
  public static String localize(String translationKey, Object... args)
  {
    TextComponentTranslation tr = new TextComponentTranslation(translationKey, args);
    tr.getStyle().setColor(TextFormatting.RESET);
    final String ft = tr.getFormattedText();
    if(ft.contains("${")) {
      // Non-recursive, non-argument lang file entry cross referencing.
      Pattern pt = Pattern.compile("\\$\\{([\\w\\.]+)\\}");
      Matcher mt = pt.matcher(ft);
      StringBuffer sb = new StringBuffer();
      while(mt.find()) mt.appendReplacement(sb, (new TextComponentTranslation(mt.group(1))).getFormattedText().trim());
      mt.appendTail(sb);
      return sb.toString();
    } else {
      return ft;
    }
  }

  /**
   * Returns true if a given key is translated for the current language.
   */
  @SideOnly(Side.CLIENT)
  public static boolean hasTranslation(String key)
  { return net.minecraft.client.resources.I18n.hasKey(key); }

  public static final class Tooltip
  {
    @SideOnly(Side.CLIENT)
    public static boolean extendedTipCondition()
    { return (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)); }

    @SideOnly(Side.CLIENT)
    public static boolean helpCondition()
    { return extendedTipCondition() && ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)||Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))); }

    /**
     * Adds an extended tooltip or help tooltip depending on the key states of CTRL and SHIFT.
     * Returns true if the localisable help/tip was added, false if not (either not CTL/SHIFT or
     * no translation found).
     */
    @SideOnly(Side.CLIENT)
    public static boolean addInformation(@Nullable String advancedTooltipTranslationKey, @Nullable String helpTranslationKey, List<String> tooltip, ITooltipFlag flag, boolean addAdvancedTooltipHints)
    {
      // Note: intentionally not using keybinding here, this must be `control` or `shift`. MC uses lwjgl Keyboard,
      //       so using this also here should be ok.
      if(ModConfig.noToolTips()) return false;
      final boolean help_available = (helpTranslationKey != null) && ModAuxiliaries.hasTranslation(helpTranslationKey + ".help");
      final boolean tip_available = (advancedTooltipTranslationKey != null) && ModAuxiliaries.hasTranslation(helpTranslationKey + ".tip");
      if((!help_available) && (!tip_available)) return false;
      if(helpCondition()) {
        if(!help_available) return false;
        String s = localize(helpTranslationKey + ".help");
        if(s.isEmpty()) return false;
        tooltip.add(s);
        return true;
      } else if(extendedTipCondition()) {
        if(!tip_available) return false;
        String s = localize(advancedTooltipTranslationKey + ".tip");
        if(s.isEmpty()) return false;
        tooltip.add(s);
        return true;
      } else if(addAdvancedTooltipHints) {
        String s = "";
        if(tip_available) s += localize(ModEngineersDecor.MODID + ".tooltip.hint.extended") + (help_available ? " " : "");
        if(help_available) s += localize(ModEngineersDecor.MODID + ".tooltip.hint.help");
        tooltip.add(s);
      }
      return false;
    }

    /**
     * Adds an extended tooltip or help tooltip for a given stack depending on the key states of CTRL and SHIFT.
     * Format in the lang file is (e.g. for items): "item.MODID.REGISTRYNAME.tip" and "item.MODID.REGISTRYNAME.help".
     * Return value see method pattern above.
     */
    @SideOnly(Side.CLIENT)
    public static boolean addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag, boolean addAdvancedTooltipHints)
    { return addInformation(stack.getTranslationKey(), stack.getTranslationKey(), tooltip, flag, addAdvancedTooltipHints); }
  }

  public static final AxisAlignedBB getPixeledAABB(double x0, double y0, double z0, double x1, double y1, double z1)
  { return new AxisAlignedBB(x0/16.0, y0/16.0, z0/16.0, x1/16.0, y1/16.0, z1/16.0); }

  public static final AxisAlignedBB getRotatedAABB(AxisAlignedBB bb, EnumFacing new_facing, boolean horizontal_rotation)
  {
    if(!horizontal_rotation) {
      switch(new_facing.getIndex()) {
        case 0: return new AxisAlignedBB(1-bb.maxX, 1-bb.maxZ, 1-bb.maxY, 1-bb.minX, 1-bb.minZ, 1-bb.minY); // D
        case 1: return new AxisAlignedBB(1-bb.maxX,   bb.minZ,   bb.minY, 1-bb.minX,   bb.maxZ,   bb.maxY); // U
        case 2: return new AxisAlignedBB(1-bb.maxX,   bb.minY, 1-bb.maxZ, 1-bb.minX,   bb.maxY, 1-bb.minZ); // N
        case 3: return new AxisAlignedBB(  bb.minX,   bb.minY,   bb.minZ,   bb.maxX,   bb.maxY,   bb.maxZ); // S --> bb
        case 4: return new AxisAlignedBB(1-bb.maxZ,   bb.minY,   bb.minX, 1-bb.minZ,   bb.maxY,   bb.maxX); // W
        case 5: return new AxisAlignedBB(  bb.minZ,   bb.minY, 1-bb.maxX,   bb.maxZ,   bb.maxY, 1-bb.minX); // E
      }
    } else {
      switch(new_facing.getIndex()) {
        case 0: return new AxisAlignedBB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // D --> bb
        case 1: return new AxisAlignedBB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // U --> bb
        case 2: return new AxisAlignedBB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // N --> bb
        case 3: return new AxisAlignedBB(1-bb.maxX, bb.minY, 1-bb.maxZ, 1-bb.minX, bb.maxY, 1-bb.minZ); // S
        case 4: return new AxisAlignedBB(  bb.minZ, bb.minY, 1-bb.maxX,   bb.maxZ, bb.maxY, 1-bb.minX); // W
        case 5: return new AxisAlignedBB(1-bb.maxZ, bb.minY,   bb.minX, 1-bb.minZ, bb.maxY,   bb.maxX); // E
      }
    }
    return bb;
  }

  @SuppressWarnings("unused")
  public static void playerChatMessage(EntityPlayer player, final String message)
  {
    String s = message.trim();
    if(!s.isEmpty()) player.sendMessage(new TextComponentTranslation(s));
  }

  public static boolean isModLoaded(String id)
  { return Loader.isModLoaded(id); }

  @SuppressWarnings("unused")
  public interface IExperimentalFeature{}
}
