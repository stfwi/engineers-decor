package wile.engineersdecor;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wile.engineersdecor.blocks.EdLadderBlock;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.OptionalRecipeCondition;
import wile.engineersdecor.libmc.detail.Registries;


@Mod("engineersdecor")
public class ModEngineersDecor
{
  public static final String MODID = "engineersdecor";
  public static final String MODNAME = "Engineer's Decor";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = LogManager.getLogger();

  public ModEngineersDecor()
  {
    Auxiliaries.init(MODID, LOGGER, ModConfig::getServerConfig);
    Auxiliaries.logGitVersion(MODNAME);
    Registries.init(MODID, "sign_decor");
    ModContent.init(MODID);
    OptionalRecipeCondition.init(MODID, LOGGER);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SERVER_CONFIG_SPEC);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG_SPEC);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    MinecraftForge.EVENT_BUS.register(this);
  }

  //
  // Events
  //

  private void onSetup(final FMLCommonSetupEvent event)
  {
    CraftingHelper.register(OptionalRecipeCondition.Serializer.INSTANCE);
    wile.engineersdecor.libmc.detail.Networking.init(MODID);
  }

  private void onClientSetup(final FMLClientSetupEvent event)
  {
    ModContent.registerMenuGuis(event);
    ModContent.registerBlockEntityRenderers(event);
    ModContent.processContentClientSide(event);
    wile.engineersdecor.libmc.detail.Overlay.register();
  }

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static class ForgeEvents
  {
    @SubscribeEvent
    public static void onRegisterBlocks(final RegistryEvent.Register<Block> event)
    { ModContent.registerBlocks(event); }

    @SubscribeEvent
    public static void onRegisterItems(final RegistryEvent.Register<Item> event)
    { ModContent.registerItems(event); }

    @SubscribeEvent
    public static void onRegisterBlockEntityTypes(final RegistryEvent.Register<BlockEntityType<?>> event)
    { ModContent.registerBlockEntityTypes(event); }

    @SubscribeEvent
    public static void onRegisterEntityTypes(final RegistryEvent.Register<EntityType<?>> event)
    { ModContent.registerEntityTypes(event); }

    @SubscribeEvent
    public static void onRegisterMenuTypes(final RegistryEvent.Register<MenuType<?>> event)
    { ModContent.registerMenuTypes(event); }

    @SubscribeEvent
    public static void onConfigLoad(final ModConfigEvent.Loading event)
    { ModConfig.apply(); }

    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event)
    {
      try {
        Auxiliaries.logger().info("Config file changed {}", event.getConfig().getFileName());
        ModConfig.apply();
      } catch(Throwable e) {
        Auxiliaries.logger().error("Failed to load changed config: " + e.getMessage());
      }
    }
  }

  //
  // Player update event
  //
  @SubscribeEvent
  public void onPlayerEvent(final LivingEvent.LivingUpdateEvent event)
  {
    if(!(event.getEntity() instanceof final Player player)) return;
    if(player.onClimbable()) EdLadderBlock.onPlayerUpdateEvent(player);
  }

}
