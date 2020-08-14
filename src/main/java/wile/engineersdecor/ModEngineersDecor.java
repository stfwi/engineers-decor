/*
 * @file ModEngineersDecor.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main mod class.
 */
package wile.engineersdecor;

import wile.engineersdecor.detail.*;
import wile.engineersdecor.blocks.*;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;


@Mod(
  modid = ModEngineersDecor.MODID,
  name = ModEngineersDecor.MODNAME,
  version = ModEngineersDecor.MODVERSION,
  dependencies = "required-after:forge@[14.23.5.2768,);before:immersiveengineering",
  useMetadata = true,
  updateJSON = "https://raw.githubusercontent.com/stfwi/engineers-decor/develop/1.12/meta/update.json",
  certificateFingerprint = ((ModEngineersDecor.MODFINGERPRINT==("@"+"MOD_SIGNSHA1"+"@")) ? "" : ModEngineersDecor.MODFINGERPRINT)
)
@SuppressWarnings({"unused", "ConstantConditions"})
public class ModEngineersDecor
{
  public static final String MODID = "engineersdecor";
  public static final String MODNAME = "Engineer's Decor";
  public static final String MODVERSION = "@MOD_VERSION@";
  public static final String MODMCVERSION = "@MOD_MCVERSION@";
  public static final String MODFINGERPRINT = "@MOD_SIGNSHA1@";
  public static final String MODBUILDID = "@MOD_BUILDID@";
  public static Logger logger;

  @Mod.Instance
  public static ModEngineersDecor instance;

  //--------------------------------------------------------------------------------------------------------------------
  // Side handling
  //--------------------------------------------------------------------------------------------------------------------

  @SidedProxy(clientSide = "wile.engineersdecor.detail.ClientProxy", serverSide = "wile.engineersdecor.detail.ServerProxy")
  public static IProxy proxy;

  public interface IProxy
  {
    default void preInit(final FMLPreInitializationEvent e) {}
    default void init(final FMLInitializationEvent e) {}
    default void postInit(final FMLPostInitializationEvent e) {}
    default World getWorlClientSide() { return null; }
    default EntityPlayer getPlayerClientSide() { return null; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Init
  //--------------------------------------------------------------------------------------------------------------------

  @Mod.EventHandler
  public void preInit(final FMLPreInitializationEvent event)
  {
    logger = event.getModLog();
    logger.info(MODNAME + ": Version " + MODMCVERSION + "-" + MODVERSION + ( (MODBUILDID=="@"+"MOD_BUILDID"+"@") ? "" : (" "+MODBUILDID) ) + ".");
    if(MODFINGERPRINT=="@"+"MOD_SIGNSHA1"+"@") {
      logger.warn(MODNAME + ": Mod is NOT signed by the author.");
    } else {
      logger.info(MODNAME + ": Found valid fingerprint " + MODFINGERPRINT + ".");
    }
    proxy.preInit(event);
    MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
    Networking.init();
    ModConfig.onPreInit();
  }

  @Mod.EventHandler
  public void init(final FMLInitializationEvent event)
  {
    proxy.init(event);
    NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModEngineersDecor.GuiHandler());
    EntityRegistry.registerModEntity(new ResourceLocation(ModEngineersDecor.MODID, "chair_entity"), BlockDecorChair.EntityChair.class,"DecorChair",0,this,80,1,false);
  }

  @Mod.EventHandler
  public void postInit(final FMLPostInitializationEvent event)
  {
    ModConfig.onPostInit(event);
    proxy.postInit(event);
    if(RecipeCondModSpecific.num_skipped > 0) logger.info("Excluded " + RecipeCondModSpecific.num_skipped + " recipes due to config opt-out.");
    if(ModConfig.zmisc.with_experimental) logger.info("Included experimental features due to mod config.");
    ExtItems.onPostInit();
    BlockCategories.reload();
    TreeCutting.reload();
  }

  @Mod.EventBusSubscriber
  public static final class RegistrationSubscriptions
  {
    @SubscribeEvent
    public static void registerBlocks(final RegistryEvent.Register<Block> event)
    { ModContent.registerBlocks(event); }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event)
    { ModContent.registerItemBlocks(event); ModContent.registerItems(event); }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event)
    { ModRecipes.registerRecipes(event); }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event)
    { ModContent.initModels(); }
  }

  public static final CreativeTabs CREATIVE_TAB_ENGINEERSDECOR = (new CreativeTabs("tabengineersdecor") {
    @Override
    @SideOnly(Side.CLIENT)
    public @Nonnull ItemStack createIcon()
    { return new ItemStack(ModContent.SIGN_MODLOGO); }
  });

  //--------------------------------------------------------------------------------------------------------------------
  // Player interaction/notification
  //--------------------------------------------------------------------------------------------------------------------

  public static final class GuiHandler implements IGuiHandler
  {
    public static final int GUIID_CRAFTING_TABLE = 213101;
    public static final int GUIID_SMALL_LAB_FURNACE = 213102;
    public static final int GUIID_ELECTRICAL_LAB_FURNACE = 213103;
    public static final int GUIID_SMALL_WASTE_INCINERATOR = 213104;
    public static final int GUIID_FACTORY_DROPPER = 213105;
    public static final int GUIID_FACTORY_HOPPER = 213106;
    public static final int GUIID_FACTORY_PLACER = 213107;
    public static final int GUIID_LABELED_CRATE = 213108;

    @Override
    public Object getServerGuiElement(final int guiid, final EntityPlayer player, final World world, int x, int y, int z)
    {
      final BlockPos pos = new BlockPos(x,y,z);
      final TileEntity te = world.getTileEntity(pos);
      switch(guiid) {
        case GUIID_CRAFTING_TABLE: return BlockDecorCraftingTable.getServerGuiElement(player, world, pos, te);
        case GUIID_SMALL_LAB_FURNACE: return BlockDecorFurnace.getServerGuiElement(player, world, pos, te);
        case GUIID_ELECTRICAL_LAB_FURNACE: return BlockDecorFurnaceElectrical.getServerGuiElement(player, world, pos, te);
        case GUIID_SMALL_WASTE_INCINERATOR: return BlockDecorWasteIncinerator.getServerGuiElement(player, world, pos, te);
        case GUIID_FACTORY_DROPPER: return BlockDecorDropper.getServerGuiElement(player, world, pos, te);
        case GUIID_FACTORY_HOPPER: return BlockDecorHopper.getServerGuiElement(player, world, pos, te);
        case GUIID_FACTORY_PLACER: return BlockDecorPlacer.getServerGuiElement(player, world, pos, te);
        case GUIID_LABELED_CRATE: return BlockDecorLabeledCrate.getServerGuiElement(player, world, pos, te);
      }
      return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Object getClientGuiElement(final int guiid, final EntityPlayer player, final World world, int x, int y, int z)
    {
      final BlockPos pos = new BlockPos(x,y,z);
      final TileEntity te = (world instanceof WorldClient) ? world.getTileEntity(pos) : null;
      switch(guiid) {
        case GUIID_CRAFTING_TABLE: return BlockDecorCraftingTable.getClientGuiElement(player, world, pos, te);
        case GUIID_SMALL_LAB_FURNACE: return BlockDecorFurnace.getClientGuiElement(player, world, pos, te);
        case GUIID_ELECTRICAL_LAB_FURNACE: return BlockDecorFurnaceElectrical.getClientGuiElement(player, world, pos, te);
        case GUIID_SMALL_WASTE_INCINERATOR: return BlockDecorWasteIncinerator.getClientGuiElement(player, world, pos, te);
        case GUIID_FACTORY_DROPPER: return BlockDecorDropper.getClientGuiElement(player, world, pos, te);
        case GUIID_FACTORY_HOPPER: return BlockDecorHopper.getClientGuiElement(player, world, pos, te);
        case GUIID_FACTORY_PLACER: return BlockDecorPlacer.getClientGuiElement(player, world, pos, te);
        case GUIID_LABELED_CRATE: return BlockDecorLabeledCrate.getClientGuiElement(player, world, pos, te);
      }
      return null;
    }
  }

  @Mod.EventBusSubscriber
  public static class PlayerEventHandler
  {
    @SubscribeEvent
    public void update(final LivingEvent.LivingUpdateEvent event)
    {
      if(!(event.getEntity() instanceof EntityPlayer)) return;
      final EntityPlayer player = (EntityPlayer)event.getEntity();
      if(player.world == null) return;
      if(player.isOnLadder()) BlockDecorLadder.onPlayerUpdateEvent(player);
    }
  }

}
