/*
 * @file Auxiliaries.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General commonly used functionality.
 */
package wile.engineersdecor.libmc.detail;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Auxiliaries
{
  private static String modid;
  private static Logger logger;
  private static Supplier<CompoundTag> server_config_supplier = CompoundTag::new;

  public static void init(String modid, Logger logger, Supplier<CompoundTag> server_config_supplier)
  {
    Auxiliaries.modid = modid;
    Auxiliaries.logger = logger;
    Auxiliaries.server_config_supplier = server_config_supplier;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Mod specific exports
  // -------------------------------------------------------------------------------------------------------------------

  public static String modid()
  { return modid; }

  public static Logger logger()
  { return logger; }

  // -------------------------------------------------------------------------------------------------------------------
  // Sideness, system/environment, tagging interfaces
  // -------------------------------------------------------------------------------------------------------------------

  public interface IExperimentalFeature {}

  public static boolean isModLoaded(final String registry_name)
  { return ModList.get().isLoaded(registry_name); }

  public static boolean isDevelopmentMode()
  { return SharedConstants.IS_RUNNING_IN_IDE; }

  @OnlyIn(Dist.CLIENT)
  public static boolean isShiftDown()
  {
    return (InputConstants.isKeyDown(SidedProxy.mc().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
      InputConstants.isKeyDown(SidedProxy.mc().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT));
  }

  @OnlyIn(Dist.CLIENT)
  public static boolean isCtrlDown()
  {
    return (InputConstants.isKeyDown(SidedProxy.mc().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) ||
      InputConstants.isKeyDown(SidedProxy.mc().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL));
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Logging
  // -------------------------------------------------------------------------------------------------------------------

  public static void logInfo(final String msg)
  { logger.info(msg); }

  public static void logWarn(final String msg)
  { logger.warn(msg); }

  public static void logError(final String msg)
  { logger.error(msg); }

  // -------------------------------------------------------------------------------------------------------------------
  // Localization, text formatting
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Text localization wrapper, implicitly prepends `MODID` to the
   * translation keys. Forces formatting argument, nullable if no special formatting shall be applied..
   */
  public static MutableComponent localizable(String modtrkey, Object... args)
  { return Component.translatable((modtrkey.startsWith("block.") || (modtrkey.startsWith("item."))) ? (modtrkey) : (modid+"."+modtrkey), args); }

  public static MutableComponent localizable(String modtrkey, @Nullable ChatFormatting color, Object... args)
  {
    final MutableComponent tr = Component.translatable(modid+"."+modtrkey, args);
    if(color!=null) tr.getStyle().applyFormat(color);
    return tr;
  }

  public static Component localizable(String modtrkey)
  { return localizable(modtrkey, new Object[]{}); }

  public static Component localizable_block_key(String blocksubkey)
  { return Component.translatable("block."+modid+"."+blocksubkey); }

  @OnlyIn(Dist.CLIENT)
  public static String localize(String translationKey, Object... args)
  {
    Component tr = Component.translatable(translationKey, args);
    tr.getStyle().applyFormat(ChatFormatting.RESET);
    final String ft = tr.getString();
    if(ft.contains("${")) {
      // Non-recursive, non-argument lang file entry cross referencing.
      Pattern pt = Pattern.compile("\\$\\{([^}]+)\\}");
      Matcher mt = pt.matcher(ft);
      StringBuffer sb = new StringBuffer();
      while(mt.find()) {
        String m = mt.group(1);
        if(m.contains("?")) {
          String[] kv = m.split("\\?", 2);
          String key = kv[0].trim();
          boolean not = key.startsWith("!");
          if(not) key = key.replaceFirst("!", "");
          m = kv[1].trim();
          if(!server_config_supplier.get().contains(key)) {
            m = "";
          } else {
            boolean r = server_config_supplier.get().getBoolean(key);
            if(not) r = !r;
            if(!r) m = "";
          }
        }
        mt.appendReplacement(sb, Matcher.quoteReplacement((Component.translatable(m)).getString().trim()));
      }
      mt.appendTail(sb);
      return sb.toString();
    } else {
      return ft;
    }
  }

  /**
   * Returns true if a given key is translated for the current language.
   */
  @OnlyIn(Dist.CLIENT)
  public static boolean hasTranslation(String key)
  { return net.minecraft.client.resources.language.I18n.exists(key); }

  public static MutableComponent join(Collection<? extends Component> components, String separator)
  { return ComponentUtils.formatList(components, Component.literal(separator), Function.identity()); }

  public static MutableComponent join(Component... components)
  { final MutableComponent tc = Component.empty(); for(Component c:components) { tc.append(c); } return tc; }

  public static boolean isEmpty(Component component)
  { return component.getSiblings().isEmpty() && component.getString().isEmpty(); }

  public static final class Tooltip
  {
    @OnlyIn(Dist.CLIENT)
    public static boolean extendedTipCondition()
    { return isShiftDown(); }

    @OnlyIn(Dist.CLIENT)
    public static boolean helpCondition()
    { return isShiftDown() && isCtrlDown(); }

    /**
     * Adds an extended tooltip or help tooltip depending on the key states of CTRL and SHIFT.
     * Returns true if the localisable help/tip was added, false if not (either not CTL/SHIFT or
     * no translation found).
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean addInformation(@Nullable String advancedTooltipTranslationKey, @Nullable String helpTranslationKey, List<Component> tooltip, TooltipFlag flag, boolean addAdvancedTooltipHints)
    {
      // Note: intentionally not using keybinding here, this must be `control` or `shift`.
      final boolean help_available = (helpTranslationKey != null) && Auxiliaries.hasTranslation(helpTranslationKey + ".help");
      final boolean tip_available = (advancedTooltipTranslationKey != null) && Auxiliaries.hasTranslation(helpTranslationKey + ".tip");
      if((!help_available) && (!tip_available)) return false;
      String tip_text = "";
      if(helpCondition()) {
        if(help_available) tip_text = localize(helpTranslationKey + ".help");
      } else if(extendedTipCondition()) {
        if(tip_available) tip_text = localize(advancedTooltipTranslationKey + ".tip");
      } else if(addAdvancedTooltipHints) {
        if(tip_available) tip_text += localize(modid + ".tooltip.hint.extended") + (help_available ? " " : "");
        if(help_available) tip_text += localize(modid + ".tooltip.hint.help");
      }
      if(tip_text.isEmpty()) return false;
      String[] tip_list = tip_text.split("\\r?\\n");
      for(String tip:tip_list) {
        tooltip.add(Component.literal(tip.replaceAll("\\s+$","").replaceAll("^\\s+", "")).withStyle(ChatFormatting.GRAY));
      }
      return true;
    }

    /**
     * Adds an extended tooltip or help tooltip for a given stack depending on the key states of CTRL and SHIFT.
     * Format in the lang file is (e.g. for items): "item.MODID.REGISTRYNAME.tip" and "item.MODID.REGISTRYNAME.help".
     * Return value see method pattern above.
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean addInformation(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag, boolean addAdvancedTooltipHints)
    { return addInformation(stack.getDescriptionId(), stack.getDescriptionId(), tooltip, flag, addAdvancedTooltipHints); }

    @OnlyIn(Dist.CLIENT)
    public static boolean addInformation(String translation_key, List<Component> tooltip)
    {
      if(!Auxiliaries.hasTranslation(translation_key)) return false;
      tooltip.add(Component.literal(localize(translation_key).replaceAll("\\s+$","").replaceAll("^\\s+", "")).withStyle(ChatFormatting.GRAY));
      return true;
    }

  }

  @SuppressWarnings("unused")
  public static void playerChatMessage(final Player player, final String message)
  { player.displayClientMessage(Component.translatable(message.trim()), true); }

  public static @Nullable Component unserializeTextComponent(String serialized)
  { return Component.Serializer.fromJson(serialized); }

  public static String serializeTextComponent(Component tc)
  { return (tc==null) ? ("") : (Component.Serializer.toJson(tc)); }

  // -------------------------------------------------------------------------------------------------------------------
  // Tag Handling
  // -------------------------------------------------------------------------------------------------------------------

  @SuppressWarnings("deprecation")
  public static boolean isInItemTag(Item item, ResourceLocation tag)
  { return ForgeRegistries.ITEMS.tags().stream().filter(tg->tg.getKey().location().equals(tag)).anyMatch(tk->tk.contains(item)); }

  @SuppressWarnings("deprecation")
  public static boolean isInBlockTag(Block block, ResourceLocation tag)
  { return ForgeRegistries.BLOCKS.tags().stream().filter(tg->tg.getKey().location().equals(tag)).anyMatch(tk->tk.contains(block)); }

  @SuppressWarnings("deprecation")
  public static ResourceLocation getResourceLocation(Item item)
  { return Registry.ITEM.getKey(item); }

  @SuppressWarnings("deprecation")
  public static ResourceLocation getResourceLocation(Block block)
  { return Registry.BLOCK.getKey(block); }

  @SuppressWarnings("deprecation")
  public static ResourceLocation getResourceLocation(net.minecraft.world.inventory.MenuType<?> menu)
  { return Registry.MENU.getKey(menu); }

  @SuppressWarnings("deprecation")
  public static ResourceLocation getResourceLocation(net.minecraft.world.level.material.Fluid fluid)
  { return Registry.FLUID.getKey(fluid); }

  // -------------------------------------------------------------------------------------------------------------------
  // Item NBT data
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Equivalent to getDisplayName(), returns null if no custom name is set.
   */
  public static @Nullable Component getItemLabel(ItemStack stack)
  {
    CompoundTag nbt = stack.getTagElement("display");
    if(nbt != null && nbt.contains("Name", 8)) {
      try {
        Component tc = unserializeTextComponent(nbt.getString("Name"));
        if(tc != null) return tc;
        nbt.remove("Name");
      } catch(Exception e) {
        nbt.remove("Name");
      }
    }
    return null;
  }

  public static ItemStack setItemLabel(ItemStack stack, @Nullable Component name)
  {
    if(name != null) {
      CompoundTag nbt = stack.getOrCreateTagElement("display");
      nbt.putString("Name", serializeTextComponent(name));
    } else {
      if(stack.hasTag()) stack.removeTagKey("display");
    }
    return stack;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Block handling
  // -------------------------------------------------------------------------------------------------------------------

  public static boolean isWaterLogged(BlockState state)
  { return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED); }

  public static AABB getPixeledAABB(double x0, double y0, double z0, double x1, double y1, double z1)
  { return new AABB(x0/16.0, y0/16.0, z0/16.0, x1/16.0, y1/16.0, z1/16.0); }

  public static AABB getRotatedAABB(AABB bb, Direction new_facing)
  { return getRotatedAABB(bb, new_facing, false); }

  public static AABB[] getRotatedAABB(AABB[] bb, Direction new_facing)
  { return getRotatedAABB(bb, new_facing, false); }

  public static AABB getRotatedAABB(AABB bb, Direction new_facing, boolean horizontal_rotation)
  {
    if(!horizontal_rotation) {
      switch(new_facing.get3DDataValue()) {
        case 0: return new AABB(1-bb.maxX,   bb.minZ,   bb.minY, 1-bb.minX,   bb.maxZ,   bb.maxY); // D
        case 1: return new AABB(1-bb.maxX, 1-bb.maxZ, 1-bb.maxY, 1-bb.minX, 1-bb.minZ, 1-bb.minY); // U
        case 2: return new AABB(  bb.minX,   bb.minY,   bb.minZ,   bb.maxX,   bb.maxY,   bb.maxZ); // N --> bb
        case 3: return new AABB(1-bb.maxX,   bb.minY, 1-bb.maxZ, 1-bb.minX,   bb.maxY, 1-bb.minZ); // S
        case 4: return new AABB(  bb.minZ,   bb.minY, 1-bb.maxX,   bb.maxZ,   bb.maxY, 1-bb.minX); // W
        case 5: return new AABB(1-bb.maxZ,   bb.minY,   bb.minX, 1-bb.minZ,   bb.maxY,   bb.maxX); // E
      }
    } else {
      switch(new_facing.get3DDataValue()) {
        case 0: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // D --> bb
        case 1: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // U --> bb
        case 2: return new AABB(  bb.minX, bb.minY,   bb.minZ,   bb.maxX, bb.maxY,   bb.maxZ); // N --> bb
        case 3: return new AABB(1-bb.maxX, bb.minY, 1-bb.maxZ, 1-bb.minX, bb.maxY, 1-bb.minZ); // S
        case 4: return new AABB(  bb.minZ, bb.minY, 1-bb.maxX,   bb.maxZ, bb.maxY, 1-bb.minX); // W
        case 5: return new AABB(1-bb.maxZ, bb.minY,   bb.minX, 1-bb.minZ, bb.maxY,   bb.maxX); // E
      }
    }
    return bb;
  }

  public static AABB[] getRotatedAABB(AABB[] bbs, Direction new_facing, boolean horizontal_rotation)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getRotatedAABB(bbs[i], new_facing, horizontal_rotation);
    return transformed;
  }

  public static AABB getYRotatedAABB(AABB bb, int clockwise_90deg_steps)
  {
    final Direction[] direction_map = new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    return getRotatedAABB(bb, direction_map[(clockwise_90deg_steps+4096) & 0x03], true);
  }

  public static AABB[] getYRotatedAABB(AABB[] bbs, int clockwise_90deg_steps)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getYRotatedAABB(bbs[i], clockwise_90deg_steps);
    return transformed;
  }

  public static AABB getMirroredAABB(AABB bb, Direction.Axis axis)
  {
    return switch (axis) {
      case X -> new AABB(1 - bb.maxX, bb.minY, bb.minZ, 1 - bb.minX, bb.maxY, bb.maxZ);
      case Y -> new AABB(bb.minX, 1 - bb.maxY, bb.minZ, bb.maxX, 1 - bb.minY, bb.maxZ);
      case Z -> new AABB(bb.minX, bb.minY, 1 - bb.maxZ, bb.maxX, bb.maxY, 1 - bb.minZ);
    };
  }

  public static AABB[] getMirroredAABB(AABB[] bbs, Direction.Axis axis)
  {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = getMirroredAABB(bbs[i], axis);
    return transformed;
  }

  public static VoxelShape getUnionShape(AABB ... aabbs)
  {
    VoxelShape shape = Shapes.empty();
    for(AABB aabb: aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR);
    return shape;
  }

  public static VoxelShape getUnionShape(AABB[] ... aabb_list)
  {
    VoxelShape shape = Shapes.empty();
    for(AABB[] aabbs:aabb_list) {
      for(AABB aabb: aabbs) shape = Shapes.joinUnoptimized(shape, Shapes.create(aabb), BooleanOp.OR);
    }
    return shape;
  }

  public static AABB[] getMappedAABB(AABB[] bbs, Function<AABB,AABB> mapper) {
    final AABB[] transformed = new AABB[bbs.length];
    for(int i=0; i<bbs.length; ++i) transformed[i] = mapper.apply(bbs[i]);
    return transformed;
  }

  public static final class BlockPosRange implements Iterable<BlockPos>
  {
    private final int x0, x1, y0, y1, z0, z1;

    public BlockPosRange(int x0, int y0, int z0, int x1, int y1, int z1)
    {
      this.x0 = Math.min(x0,x1); this.x1 = Math.max(x0,x1);
      this.y0 = Math.min(y0,y1); this.y1 = Math.max(y0,y1);
      this.z0 = Math.min(z0,z1); this.z1 = Math.max(z0,z1);
    }

    public static BlockPosRange of(AABB range)
    {
      return new BlockPosRange(
        (int)Math.floor(range.minX),
        (int)Math.floor(range.minY),
        (int)Math.floor(range.minZ),
        (int)Math.floor(range.maxX-.0625),
        (int)Math.floor(range.maxY-.0625),
        (int)Math.floor(range.maxZ-.0625)
      );
    }

    public int getXSize()
    { return x1-x0+1; }

    public int getYSize()
    { return y1-y0+1; }

    public int getZSize()
    { return z1-z0+1; }

    public int getArea()
    { return getXSize() * getZSize(); }

    public int getHeight()
    { return getYSize(); }

    public int getVolume()
    { return getXSize() * getYSize() * getZSize(); }

    public BlockPos byXZYIndex(int xyz_index)
    {
      final int xsz=getXSize(), ysz=getYSize(), zsz=getZSize();
      xyz_index = xyz_index % (xsz*ysz*zsz);
      final int y = xyz_index / (xsz*zsz);
      xyz_index -= y * (xsz*zsz);
      final int z = xyz_index / xsz;
      xyz_index -= z * xsz;
      final int x = xyz_index;
      return new BlockPos(x0+x, y0+y, z0+z);
    }

    public BlockPos byXZIndex(int xz_index, int y_offset)
    {
      final int xsz=getXSize(), zsz=getZSize();
      xz_index = xz_index % (xsz*zsz);
      final int z = xz_index / xsz;
      xz_index -= z * xsz;
      final int x = xz_index;
      return new BlockPos(x0+x, y0+y_offset, z0+z);
    }

    public static final class BlockRangeIterator implements Iterator<BlockPos>
    {
      private final BlockPosRange range_;
      private int x,y,z;

      public BlockRangeIterator(BlockPosRange range)
      { range_ = range; x=range.x0; y=range.y0; z=range.z0; }

      @Override
      public boolean hasNext()
      { return (z <= range_.z1); }

      @Override
      public BlockPos next()
      {
        if(!hasNext()) throw new NoSuchElementException();
        final BlockPos pos = new BlockPos(x,y,z);
        ++x;
        if(x > range_.x1) {
          x = range_.x0;
          ++y;
          if(y > range_.y1) {
            y = range_.y0;
            ++z;
          }
        }
        return pos;
      }
    }

    @Override
    public BlockRangeIterator iterator()
    { return new BlockRangeIterator(this); }

    public Stream<BlockPos> stream()
    { return java.util.stream.StreamSupport.stream(spliterator(), false); }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // JAR resource related
  // -------------------------------------------------------------------------------------------------------------------

  public static String loadResourceText(InputStream is)
  {
    try {
      if(is==null) return "";
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      return br.lines().collect(Collectors.joining("\n"));
    } catch(Throwable e) {
      return "";
    }
  }

  public static String loadResourceText(String path)
  { return loadResourceText(Auxiliaries.class.getResourceAsStream(path)); }

  public static void logGitVersion(String mod_name)
  {
    try {
      // Done during construction to have an exact version in case of a crash while registering.
      String version = Auxiliaries.loadResourceText("/.gitversion-" + modid).trim();
      logInfo(mod_name+((version.isEmpty())?(" (dev build)"):(" GIT id #"+version)) + ".");
    } catch(Throwable e) {
      // (void)e; well, then not. Priority is not to get unneeded crashes because of version logging.
    }
  }
}
