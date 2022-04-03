/*
 * @file Registries.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common game registry handling.
 */
package wile.engineersdecor.libmc.detail;
import wile.engineersdecor.libmc.blocks.StandardBlocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Registries
{
  private static String modid = null;
  private static String creative_tab_icon = "";
  private static CreativeModeTab creative_tab = null;

  private static final List<Tuple<String, Supplier<? extends Block>>> block_suppliers = new ArrayList<>();
  private static final List<Tuple<String, Supplier<? extends Item>>> item_suppliers = new ArrayList<>();
  private static final List<Tuple<String, Supplier<? extends BlockEntityType<?>>>> block_entity_type_suppliers = new ArrayList<>();
  private static final List<Tuple<String, Supplier<? extends EntityType<?>>>> entity_type_suppliers = new ArrayList<>();
  private static final List<Tuple<String, Supplier<? extends MenuType<?>>>> menu_type_suppliers = new ArrayList<>();
  private static final List<String> block_item_order = new ArrayList<>();

  private static final Map<String, Block> registered_blocks = new HashMap<>();
  private static final Map<String, Item> registered_items = new HashMap<>();
  private static final Map<String, BlockEntityType<?>> registered_block_entity_types = new HashMap<>();
  private static final Map<String, EntityType<?>> registered_entity_types = new HashMap<>();
  private static final Map<String, MenuType<?>> registered_menu_types = new HashMap<>();
  private static final Map<String, TagKey<Block>> registered_block_tag_keys = new HashMap<>();
  private static final Map<String, TagKey<Item>> registered_item_tag_keys = new HashMap<>();

  public static void init(String mod_id, String creative_tab_icon_item_name)
  { modid = mod_id; creative_tab_icon=creative_tab_icon_item_name; }

  public static CreativeModeTab getCreativeModeTab()
  {
    if(creative_tab==null) {
      creative_tab = (new CreativeModeTab("tab" + modid) {
        public ItemStack makeIcon() { return new ItemStack(registered_items.get(creative_tab_icon)); }
      });
    }
    return creative_tab;
  }

  // -------------------------------------------------------------------------------------------------------------

  public static Block getBlock(String block_name)
  { return registered_blocks.get(block_name); }

  public static Item getItem(String name)
  { return registered_items.get(name); }

  public static EntityType<?> getEntityType(String name)
  { return registered_entity_types.get(name); }

  public static BlockEntityType<?> getBlockEntityType(String block_name)
  { return registered_block_entity_types.get(block_name); }

  public static MenuType<?> getMenuType(String name)
  { return registered_menu_types.get(name); }

  public static BlockEntityType<?> getBlockEntityTypeOfBlock(String block_name)
  { return getBlockEntityType("tet_"+block_name); }

  public static MenuType<?> getMenuTypeOfBlock(String name)
  { return getMenuType("ct_"+name); }

  public static TagKey<Block> getBlockTagKey(String name)
  { return registered_block_tag_keys.get(name); }

  public static TagKey<Item> getItemTagKey(String name)
  { return registered_item_tag_keys.get(name); }

  // -------------------------------------------------------------------------------------------------------------

  @Nonnull
  public static List<Block> getRegisteredBlocks()
  { return Collections.unmodifiableList(registered_blocks.values().stream().toList()); }

  @Nonnull
  public static List<Item> getRegisteredItems()
  { return Collections.unmodifiableList(registered_items.values().stream().toList()); }

  @Nonnull
  public static List<BlockEntityType<?>> getRegisteredBlockEntityTypes()
  { return Collections.unmodifiableList(registered_block_entity_types.values().stream().toList()); }

  @Nonnull
  public static List<EntityType<?>> getRegisteredEntityTypes()
  { return Collections.unmodifiableList(registered_entity_types.values().stream().toList()); }

  // -------------------------------------------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  public static <T extends Item> void addItem(String registry_name, Supplier<T> supplier)
  {
    item_suppliers.add(new Tuple<>(registry_name, ()->{
      final T instance = supplier.get();
      instance.setRegistryName(new ResourceLocation(modid, registry_name));
      return instance;
    }));
  }

  @SuppressWarnings("unchecked")
  public static <T extends Block> void addBlock(String registry_name, Supplier<T> block_supplier)
  {
    block_suppliers.add(new Tuple<>(registry_name, ()->{
      final T instance = block_supplier.get();
      instance.setRegistryName(new ResourceLocation(modid, registry_name));
      return instance;
    }));
  }

  @SuppressWarnings("unchecked")
  public static <T extends BlockEntity> void addBlockEntityType(String registry_name, BlockEntityType.BlockEntitySupplier<T> ctor, String... block_names)
  {
    block_entity_type_suppliers.add(new Tuple<>(registry_name, ()->{
      final Block[] blocks = Arrays.stream(block_names).map(s->{
        Block b = registered_blocks.get(s);
        if(b==null) Auxiliaries.logError("registered_blocks does not encompass '" + s + "'");
        return b;
      }).filter(Objects::nonNull).collect(Collectors.toList()).toArray(new Block[]{});
      final BlockEntityType<T> instance =  BlockEntityType.Builder.of(ctor, blocks).build(null);
      instance.setRegistryName(modid, registry_name);
      return instance;
    }));
  }

  @SuppressWarnings("unchecked")
  public static <T extends BlockEntity> void addBlockEntityType(String registry_name, BlockEntityType.BlockEntitySupplier<T> ctor, Class<? extends Block> block_clazz)
  {
    block_entity_type_suppliers.add(new Tuple<>(registry_name, ()->{
      final Block[] blocks = registered_blocks.values().stream().filter(block_clazz::isInstance).collect(Collectors.toList()).toArray(new Block[]{});
      final BlockEntityType<T> instance =  BlockEntityType.Builder.of(ctor, blocks).build(null);
      instance.setRegistryName(modid, registry_name);
      return instance;
    }));
  }

  @SuppressWarnings("unchecked")
  public static <T extends EntityType<?>> void addEntityType(String registry_name, Supplier<EntityType<?>> supplier)
  { entity_type_suppliers.add(new Tuple<>(registry_name, supplier)); }

  @SuppressWarnings("unchecked")
  public static <T extends MenuType<?>> void addMenuType(String registry_name, MenuType.MenuSupplier<?> supplier)
  {
    menu_type_suppliers.add(new Tuple<>(registry_name, ()->{
      final MenuType<?> instance = new MenuType<>(supplier);
      instance.setRegistryName(modid, registry_name);
      return instance;
    }));
  }

  @SuppressWarnings("unchecked")
  public static void addBlock(String registry_name, Supplier<? extends Block> block_supplier, BlockEntityType.BlockEntitySupplier<?> block_entity_ctor)
  {
    addBlock(registry_name, block_supplier);
    addBlockEntityType("tet_"+registry_name, block_entity_ctor, registry_name);
  }

  @SuppressWarnings("unchecked")
  public static void addBlock(String registry_name, Supplier<? extends Block> block_supplier, BlockEntityType.BlockEntitySupplier<?> block_entity_ctor, MenuType.MenuSupplier<?> menu_type_supplier)
  {
    addBlock(registry_name, block_supplier);
    addBlockEntityType("tet_"+registry_name, block_entity_ctor, registry_name);
    addMenuType("ct_"+registry_name, menu_type_supplier);
  }

  public static void addOptionalBlockTag(String tag_name, ResourceLocation... default_blocks)
  {
    final Set<Supplier<Block>> default_suppliers = new HashSet<>();
    for(ResourceLocation rl: default_blocks) default_suppliers.add(()->ForgeRegistries.BLOCKS.getValue(rl));
    final TagKey<Block> key = ForgeRegistries.BLOCKS.tags().createOptionalTagKey(new ResourceLocation(modid, tag_name), default_suppliers);
    registered_block_tag_keys.put(tag_name, key);
  }

  public static void addOptionalBlockTag(String tag_name, String... default_blocks)
  { addOptionalBlockTag(tag_name, Arrays.stream(default_blocks).map(ResourceLocation::new).collect(Collectors.toList()).toArray(new ResourceLocation[]{})); }

  public static void addOptionalItemTag(String tag_name, ResourceLocation... default_items)
  {
    final Set<Supplier<Item>> default_suppliers = new HashSet<>();
    for(ResourceLocation rl: default_items) default_suppliers.add(()->ForgeRegistries.ITEMS.getValue(rl));
    final TagKey<Item> key = ForgeRegistries.ITEMS.tags().createOptionalTagKey(new ResourceLocation(modid, tag_name), default_suppliers);
    registered_item_tag_keys.put(tag_name, key);
  }

  public static void addOptionalItemTag(String tag_name, String... default_items)
  { addOptionalBlockTag(tag_name, Arrays.stream(default_items).map(ResourceLocation::new).collect(Collectors.toList()).toArray(new ResourceLocation[]{})); }

  // -------------------------------------------------------------------------------------------------------------

  public static void onBlockRegistry(BiConsumer<ResourceLocation, Block> registration)
  {
    block_suppliers.forEach(e->{
      final Block block = e.getB().get();
      final ResourceLocation rl = new ResourceLocation(modid, e.getA());
      registration.accept(rl, block);
      registered_blocks.put(e.getA(), block);
      block_item_order.add(e.getA());
    });
    block_suppliers.clear();
  }

  public static void onItemRegistry(BiConsumer<ResourceLocation, Item> registration)
  {
    block_item_order.forEach(regname->{
      Block block = registered_blocks.get(regname);
      final ResourceLocation rl = block.getRegistryName();
      Item item;
      if(block instanceof StandardBlocks.IBlockItemFactory) {
        item = ((StandardBlocks.IBlockItemFactory)block).getBlockItem(block, (new Item.Properties().tab(getCreativeModeTab())));
      } else {
        item = new BlockItem(block, (new Item.Properties().tab(getCreativeModeTab())));
      }
      item.setRegistryName(rl);
      registration.accept(rl, item);
      registered_items.put(rl.getPath(), item);
    });
    item_suppliers.forEach(e->{
      final Item item = e.getB().get();
      registration.accept(new ResourceLocation(modid, e.getA()), item);
      registered_items.put(e.getA(), item);
    });
    item_suppliers.clear();
    block_item_order.clear();
  }

  public static void onBlockEntityRegistry(BiConsumer<ResourceLocation, BlockEntityType<?>> registration)
  {
    block_entity_type_suppliers.forEach(e->{
      final BlockEntityType<?> tet = e.getB().get();
      registration.accept(new ResourceLocation(modid, e.getA()), tet);
      registered_block_entity_types.put(e.getA(), tet);
    });
    block_entity_type_suppliers.clear();
  }

  public static void onMenuTypeRegistry(BiConsumer<ResourceLocation, MenuType<?>> registration)
  {
    menu_type_suppliers.forEach(e->{
      final MenuType<?> ct = e.getB().get();
      registration.accept(new ResourceLocation(modid, e.getA()), ct);
      registered_menu_types.put(e.getA(), ct);
    });
    menu_type_suppliers.clear();
  }

  public static void onEntityRegistry(BiConsumer<ResourceLocation, EntityType<?>> registration)
  {
    entity_type_suppliers.forEach(e->{
      final ResourceLocation rl = new ResourceLocation(modid, e.getA());
      final EntityType<?> et = e.getB().get();
      et.setRegistryName(rl);
      registration.accept(rl, et);
      registered_entity_types.put(e.getA(), et);
    });
    entity_type_suppliers.clear();
  }

}
