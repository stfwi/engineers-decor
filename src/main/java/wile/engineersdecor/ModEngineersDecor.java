package wile.engineersdecor;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import wile.engineersdecor.blocks.EdLadderBlock;
import wile.engineersdecor.libmc.Auxiliaries;
import wile.engineersdecor.libmc.OptionalRecipeCondition;
import wile.engineersdecor.libmc.Overlay;
import wile.engineersdecor.libmc.Registries;


@Mod("engineersdecor")
public class ModEngineersDecor
{
  public static final String MODID = "engineersdecor";
  public static final String MODNAME = "Engineer's Decor";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

  public ModEngineersDecor()
  {
    Auxiliaries.init(MODID, LOGGER, ModConfig::getServerConfig);
    Auxiliaries.logGitVersion(MODNAME);
    Registries.init(MODID, "sign_decor", (reg)->reg.register(FMLJavaModLoadingContext.get().getModEventBus()));
    ModContent.init(MODID);
    OptionalRecipeCondition.init(MODID, LOGGER);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SERVER_CONFIG_SPEC);
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG_SPEC);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    MinecraftForge.EVENT_BUS.register(this);
  }

  private void onSetup(final FMLCommonSetupEvent event)
  {
    CraftingHelper.register(OptionalRecipeCondition.Serializer.INSTANCE);
    wile.engineersdecor.libmc.Networking.init(MODID);
  }

  private void onClientSetup(final FMLClientSetupEvent event)
  {
    Overlay.TextOverlayGui.on_config(0.75, 0x00ffaa00, 0x55333333, 0x55333333, 0x55444444);
    wile.engineersdecor.libmc.Networking.OverlayTextMessage.setHandler(Overlay.TextOverlayGui::show);
    ModContent.registerMenuGuis(event);
    ModContent.registerBlockEntityRenderers(event);
    ModContent.processContentClientSide(event);
  }

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static class ForgeEvents
  {
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

  @SubscribeEvent
  public void onPlayerEvent(final LivingEvent.LivingTickEvent event)
  {
    if(!(event.getEntity() instanceof final Player player)) return;
    if(player.onClimbable()) EdLadderBlock.onPlayerUpdateEvent(player);
  }

  @OnlyIn(Dist.CLIENT)
  @Mod.EventBusSubscriber(Dist.CLIENT)
  public static class ForgeClientEvents
  {
    @SubscribeEvent
    public static void onRenderGui(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event)
    { Overlay.TextOverlayGui.INSTANCE.onRenderGui(event.getPoseStack()); }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderWorldOverlay(net.minecraftforge.client.event.RenderLevelStageEvent event)
    {
      if(event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER) {
        Overlay.TextOverlayGui.INSTANCE.onRenderWorldOverlay(event.getPoseStack(), event.getPartialTick());
      }
    }
  }

}
