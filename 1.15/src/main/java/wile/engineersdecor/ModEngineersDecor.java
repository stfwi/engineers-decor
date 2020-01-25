package wile.engineersdecor;

import wile.engineersdecor.detail.ModAuxiliaries;
import wile.engineersdecor.detail.ModConfig;
import wile.engineersdecor.detail.Networking;
import wile.engineersdecor.blocks.*;
import wile.engineersdecor.detail.OptionalRecipeCondition.Serializer;
import wile.engineersdecor.datagen.ModLootTables;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;


@Mod("engineersdecor")
public class ModEngineersDecor
{
  public static final String MODID = "engineersdecor";
  public static final String MODNAME = "Engineer's Decor";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = LogManager.getLogger();
  private static boolean config_loaded = false;

  public ModEngineersDecor()
  {
    ModAuxiliaries.logGitVersion(MODNAME);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSendImc);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRecvImc);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG_SPEC);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_CONFIG_SPEC);
    MinecraftForge.EVENT_BUS.register(this);
  }

  public static final Logger logger() { return LOGGER; }

  //
  // Events
  //

  private void onSetup(final FMLCommonSetupEvent event)
  {
    LOGGER.info("Registering recipe condition processor ...");
    CraftingHelper.register(Serializer.INSTANCE);
    Networking.init();
    if(config_loaded) {
      try {
        logger().info("Applying loaded config file.");
        ModConfig.apply();
      } catch(Throwable e) {
        logger().error("Failed to apply config: " + e.getMessage());
      }
    } else {
      logger().info("Cannot apply config, load event was not casted yet.");
    }
  }

  private void onClientSetup(final FMLClientSetupEvent event)
  {
    ModContent.registerContainerGuis(event);
    ModContent.registerTileEntityRenderers(event);
    ModContent.processContentClientSide(event);
  }

  private void onSendImc(final InterModEnqueueEvent event)
  {}

  private void onRecvImc(final InterModProcessEvent event)
  {}

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static class ForgeEvents
  {
    @SubscribeEvent
    public static void onBlocksRegistry(final RegistryEvent.Register<Block> event)
    { ModContent.registerBlocks(event); }

    @SubscribeEvent
    public static void onItemRegistry(final RegistryEvent.Register<Item> event)
    { ModContent.registerBlockItems(event); }

    @SubscribeEvent
    public static void onTileEntityRegistry(final RegistryEvent.Register<TileEntityType<?>> event)
    { ModContent.registerTileEntities(event); }

    @SubscribeEvent
    public static void onRegisterEntityTypes(final RegistryEvent.Register<EntityType<?>> event)
    { ModContent.registerEntities(event); }

    @SubscribeEvent
    public static void onRegisterContainerTypes(final RegistryEvent.Register<ContainerType<?>> event)
    { ModContent.registerContainers(event); }

    // @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event)
    {}

    @SubscribeEvent
    public static void onConfigLoad(net.minecraftforge.fml.config.ModConfig.Loading configEvent)
    { config_loaded = true; }

    @SubscribeEvent
    public static void onConfigReload(net.minecraftforge.fml.config.ModConfig.Reloading configEvent)
    {
      try {
        ModEngineersDecor.logger().info("Config file changed {}", configEvent.getConfig().getFileName());
        ModConfig.apply();
      } catch(Throwable e) {
        ModEngineersDecor.logger().error("Failed to load changed config: " + e.getMessage());
      }
    }

    @SubscribeEvent
    public static void onDataGeneration(GatherDataEvent event)
    {
      event.getGenerator().addProvider(new ModLootTables(event.getGenerator()));
    }
  }

  //
  // Sided proxy functionality (skel)
  //
  public static ISidedProxy proxy = DistExecutor.runForDist(()->ClientProxy::new, ()->ServerProxy::new);
  public interface ISidedProxy
  {
    default @Nullable PlayerEntity getPlayerClientSide() { return null; }
    default @Nullable World getWorldClientSide() { return null; }
    default @Nullable Minecraft mc() { return null; }
  }
  public static final class ClientProxy implements ISidedProxy
  {
    public @Nullable PlayerEntity getPlayerClientSide() { return Minecraft.getInstance().player; }
    public @Nullable World getWorldClientSide() { return Minecraft.getInstance().world; }
    public @Nullable Minecraft mc() { return Minecraft.getInstance(); }
  }
  public static final class ServerProxy implements ISidedProxy
  {
    public @Nullable PlayerEntity getPlayerClientSide() { return null; }
    public @Nullable World getWorldClientSide() { return null; }
    public @Nullable Minecraft mc() { return null; }
  }

  //
  // Item group / creative tab
  //
  public static final ItemGroup ITEMGROUP = (new ItemGroup("tab" + MODID) {
    @OnlyIn(Dist.CLIENT)
    public ItemStack createIcon()
    { return new ItemStack(ModContent.SIGN_MODLOGO); }
  });

  //
  // Player update event
  //
  @SubscribeEvent
  public void onPlayerEvent(final LivingEvent.LivingUpdateEvent event)
  {
    if(!(event.getEntity() instanceof PlayerEntity)) return;
    final PlayerEntity player = (PlayerEntity)event.getEntity();
    if(player.world == null) return;
    if(player.isOnLadder()) BlockDecorLadder.onPlayerUpdateEvent(player);
  }

}
