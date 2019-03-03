/*
 * @file ModEngineersDecor.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main mod class.
 */
package wile.engineersdecor;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import wile.engineersdecor.blocks.BlockDecorCraftingTable;
import wile.engineersdecor.detail.ModConfig;
import wile.engineersdecor.blocks.ModBlocks;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;

@Mod(
  modid = ModEngineersDecor.MODID,
  name = ModEngineersDecor.MODNAME,
  version = ModEngineersDecor.MODVERSION,
  dependencies = "required-after:forge@[14.23.5.2768,);before:immersiveengineering",
  useMetadata = true,
  updateJSON = "https://raw.githubusercontent.com/stfwi/engineersdecor/develop/meta/update.json",
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

  @SidedProxy(clientSide = "wile.engineersdecor.detail.ClientProxy", serverSide = "wile.engineersdecor.detail.ServerProxy")
  public static IProxy proxy;

  public interface IProxy
  {
    default void preInit(FMLPreInitializationEvent e) {}
    default void init(FMLInitializationEvent e) {}
    default void postInit(FMLPostInitializationEvent e) {}
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event)
  {
    logger = event.getModLog();
    logger.info(MODNAME + ": Version " + MODMCVERSION + "-" + MODVERSION + ( (MODBUILDID=="@"+"MOD_BUILDID"+"@") ? "" : (" "+MODBUILDID) ) + ".");
    if(MODFINGERPRINT=="@"+"MOD_SIGNSHA1"+"@") {
      logger.warn(MODNAME + ": Mod is NOT signed by the author.");
    } else {
      logger.info(MODNAME + ": Found valid fingerprint " + MODFINGERPRINT + ".");
    }
    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event)
  {
    proxy.init(event);
    NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModEngineersDecor.GuiHandler());
  }

  @Mod.EventHandler
  public void postInit(FMLPostInitializationEvent event)
  { ModConfig.onPostInit(event); proxy.postInit(event); }

  @Mod.EventBusSubscriber
  public static final class RegistrationSubscriptions
  {
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    { ModBlocks.registerBlocks(event); }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    { ModBlocks.registerItemBlocks(event); }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    { ModBlocks.initModels(); }
  }

  public static final CreativeTabs CREATIVE_TAB_ENGINEERSDECOR = (new CreativeTabs("tabengineersdecor") {
    @Override
    @SideOnly(Side.CLIENT)
    public @Nonnull ItemStack createIcon()
    { return new ItemStack(ModBlocks.TREATED_WOOD_LADDER); }
  });

  public static final class GuiHandler implements IGuiHandler
  {
    public static final int GUIID_CRAFTING_TABLE = 213101;

    @Override
    public Object getServerGuiElement(final int guiid, final EntityPlayer player, final World world, int x, int y, int z)
    {
      final BlockPos pos = new BlockPos(x,y,z);
      final TileEntity te = world.getTileEntity(pos);
      switch(guiid) {
        case GUIID_CRAFTING_TABLE:
          return (te instanceof BlockDecorCraftingTable.BEntity) ? (new BlockDecorCraftingTable.BContainer(player.inventory, world, pos)) : null;
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
        case GUIID_CRAFTING_TABLE:
          return (te instanceof BlockDecorCraftingTable.BEntity) ? (new BlockDecorCraftingTable.BGuiCrafting(player.inventory, world, pos)) : null;
      }
      return null;
    }

  }

}
