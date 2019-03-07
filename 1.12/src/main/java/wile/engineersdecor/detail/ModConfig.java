/*
 * @file ModConfig.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main class for module settings. Handles reading and
 * saving the config file.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;

@Config(modid = ModEngineersDecor.MODID)
@Config.LangKey("engineersdecor.config.title")
public class ModConfig
{
  @Config.Comment({
    "Settings for beta testing and trouble shooting. Some of the settings " +
    "may be moved to other categories after testing."
  })
  @Config.Name("Miscellaneous")
  public static final SettingsZTesting zmisc = new SettingsZTesting();
  public static final class SettingsZTesting
  {
    @Config.Comment({ "Enables experimental features. Use at own risk." })
    @Config.Name("With experimental")
    public boolean with_experimental = false;
  }

  @SuppressWarnings("unused")
  @Mod.EventBusSubscriber(modid=ModEngineersDecor.MODID)
  private static final class EventHandler
  {
    @SubscribeEvent
    public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
      if(!event.getModID().equals(ModEngineersDecor.MODID)) return;
      ConfigManager.sync(ModEngineersDecor.MODID, Config.Type.INSTANCE);
    }
  }

  @SuppressWarnings("unused")
  public static final void onPostInit(FMLPostInitializationEvent event)
  {}

}
