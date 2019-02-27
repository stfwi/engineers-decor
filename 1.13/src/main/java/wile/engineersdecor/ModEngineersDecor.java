package wile.engineersdecor;

import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import wile.engineersdecor.blocks.ModBlocks;
import wile.engineersdecor.detail.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wile.engineersdecor.detail.RecipeCondModSpecific;


@Mod("engineersdecor")
public class ModEngineersDecor
{
  public static final String MODID = "engineersdecor"; // fixed name for double checks
  private static final Logger LOGGER = LogManager.getLogger();

  public ModEngineersDecor()
  {
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSendImc);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRecvImc);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    MinecraftForge.EVENT_BUS.register(this);
  }

  public static final Logger logger() { return LOGGER; }

  //
  // Events
  //

  private void onSetup(final FMLCommonSetupEvent event)
  {
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.conf_spec);
    LOGGER.info("Registering recipe condition processor ...");
    CraftingHelper.register(new ResourceLocation(MODID, "grc"), new RecipeCondModSpecific());
  }

  private void onClientSetup(final FMLClientSetupEvent event)
  {} // Currently not needed: OBJLoader.INSTANCE.addDomain(ModEngineersDecor.MODID);

  private void onSendImc(final InterModEnqueueEvent event)
  {}

  private void onRecvImc(final InterModProcessEvent event)
  {}

  @SubscribeEvent
  public void onServerStarting(FMLServerStartingEvent event)
  {}

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static class RegistryEvents
  {
    @SubscribeEvent
    public static void onBlocksRegistry(final RegistryEvent.Register<Block> event)
    { ModBlocks.registerBlocks(event); }

    @SubscribeEvent
    public static void onItemRegistry(final RegistryEvent.Register<Item> event)
    { ModBlocks.registerItemBlocks(event); }
  }

  //
  // Sided proxy functionality (skel)
  //
  public static ISidedProxy proxy = DistExecutor.runForDist(()->ClientProxy::new, ()->ServerProxy::new);
  public interface ISidedProxy {}
  public static final class ClientProxy implements ISidedProxy {}
  public static final class ServerProxy implements ISidedProxy {}

  //
  // Item group / creative tab
  //
  public static final ItemGroup ITEMGROUP = (new ItemGroup("tab" + MODID) {
    @OnlyIn(Dist.CLIENT)
    public ItemStack createIcon()
    { return new ItemStack(ModBlocks.TREATED_WOOD_LADDER); }
  });
}
