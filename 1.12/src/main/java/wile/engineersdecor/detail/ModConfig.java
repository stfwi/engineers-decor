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

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.*;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import javax.annotation.Nullable;
import java.util.ArrayList;

@Config(modid = ModEngineersDecor.MODID)
@Config.LangKey("engineersdecor.config.title")
public class ModConfig
{
  @Config.Comment({"Allows disabling specific features."})
  @Config.Name("Feature opt-outs")
  public static final SettingsOptouts optout = new SettingsOptouts();
  public static final class SettingsOptouts
  {
    @Config.Comment({"Opt-out any block by its registry name ('*' wildcard matching, "
      + "comma separated list, whitespaces ignored. You must match the whole name, "
      + "means maybe add '*' also at the begin and end. Example: '*wood*,*steel*' "
      + "excludes everything that has 'wood' or 'steel' in the registry name. "
      + "The matching result is also traced in the log file. "
    })
    @Config.Name("Pattern excludes")
    @Config.RequiresMcRestart
    public String excludes = "";

    @Config.Comment({"Prevent blocks from being opt'ed by registry name ('*' wildcard matching, "
      + "comma separated list, whitespaces ignored. Evaluated before all other opt-out checks. "
      + "You must match the whole name, means maybe add '*' also at the begin and end. Example: "
      + "'*wood*,*steel*' includes everything that has 'wood' or 'steel' in the registry name."
      + "The matching result is also traced in the log file."
    })
    @Config.Name("Pattern includes")
    @Config.RequiresMcRestart
    public String includes = "";

    @Config.Comment({"Disable clinker bricks and derived blocks."})
    @Config.Name("Without clinker bricks")
    @Config.RequiresMcRestart
    public boolean without_clinker_bricks = false;

    @Config.Comment({"Disable slag bricks and derived blocks."})
    @Config.Name("Without slag bricks")
    @Config.RequiresMcRestart
    public boolean without_slag_bricks = false;

    @Config.Comment({"Disable rebar concrete and derived blocks."})
    @Config.Name("Without rebar concrete")
    @Config.RequiresMcRestart
    public boolean without_rebar_concrete = false;

    @Config.Comment({"Disable all mod wall blocks."})
    @Config.Name("Without walls")
    @Config.RequiresMcRestart
    public boolean without_walls = false;

    @Config.Comment({"Disable all mod stairs blocks."})
    @Config.Name("Without stairs")
    @Config.RequiresMcRestart
    public boolean without_stairs = false;

    @Config.Comment({"Disable IE concrete wall."})
    @Config.Name("Without concrete wall")
    @Config.RequiresMcRestart
    public boolean without_ie_concrete_wall = false;

    @Config.Comment({"Disable panzer glass and derived blocks."})
    @Config.Name("Without panzer glass")
    @Config.RequiresMcRestart
    public boolean without_panzer_glass = false;

    @Config.Comment({"Disable treated wood crafting table."})
    @Config.Name("Without crafting table")
    @Config.RequiresMcRestart
    public boolean without_crafting_table = false;

    @Config.Comment({"Disable small lab furnace."})
    @Config.Name("Without lab furnace")
    @Config.RequiresMcRestart
    public boolean without_lab_furnace = false;

    @Config.Comment({"Disable small electrical pass-through furnace."})
    @Config.Name("Without electrical furnace")
    @Config.RequiresMcRestart
    public boolean without_electrical_furnace = false;

    @Config.Comment({"Disable treated wood table, stool, windowsill, etc."})
    @Config.Name("Without tr. wood furniture")
    @Config.RequiresMcRestart
    public boolean without_treated_wood_furniture = false;

    @Config.Comment({"Disable treated wood window, etc."})
    @Config.Name("Without windows")
    @Config.RequiresMcRestart
    public boolean without_windows = false;

    @Config.Comment({"Disable light sources"})
    @Config.Name("Without lights")
    @Config.RequiresMcRestart
    public boolean without_light_sources = false;

    @Config.Comment({"Disable ladders"})
    @Config.Name("Without ladders")
    @Config.RequiresMcRestart
    public boolean without_ladders = false;

    @Config.Comment({"Disable possibility to sit on stools and chairs."})
    @Config.Name("Without chair sitting")
    public boolean without_chair_sitting = false;

    @Config.Comment({"Disable that mobs will sit on chairs and stools."})
    @Config.Name("Without chair mob sitting")
    public boolean without_mob_chair_sitting = false;

    @Config.Comment({"Disable the speed boost of ladders in this mod."})
    @Config.Name("Without ladder speed boost")
    public boolean without_ladder_speed_boost = false;

    @Config.Comment({"Disable history refabrication feature of the treated wood crafting table."})
    @Config.Name("Without crafting table history")
    public boolean without_crafting_table_history = false;

    @Config.Comment({"Disable check valve, and redstone controlled valves."})
    @Config.Name("Without valves")
    @Config.RequiresMcRestart
    public boolean without_valves = false;

    @Config.Comment({"Disable the passive fluid accumulator."})
    @Config.Name("Without fluid accumulator")
    @Config.RequiresMcRestart
    public boolean without_passive_fluid_accumulator = false;

    @Config.Comment({"Disable item disposal/trash/void incinerator device."})
    @Config.Name("Without waste incinerator")
    @Config.RequiresMcRestart
    public boolean without_waste_incinerator = false;

    @Config.Comment({"Disable decorative sign plates (caution, hazards, etc)."})
    @Config.Name("Without signs")
    @Config.RequiresMcRestart
    public boolean without_sign_plates = false;

    @Config.Comment({"Disable the factory dropper."})
    @Config.Name("Without factory dropper")
    @Config.RequiresMcRestart
    public boolean without_factory_dropper = false;

    @Config.Comment({"Disable the factory hopper."})
    @Config.Name("Without factory hopper")
    @Config.RequiresMcRestart
    public boolean without_factory_hopper = false;

    @Config.Comment({"Disable the Factory Block Placer."})
    @Config.Name("Without factory placer")
    @Config.RequiresMcRestart
    public boolean without_factory_placer = false;

    @Config.Comment({"Disable horizontal half-block slab."})
    @Config.Name("Without slabs")
    @Config.RequiresMcRestart
    public boolean without_slabs = false;

    @Config.Comment({"Disable stackable 1/8 block slices."})
    @Config.Name("Without slab slices")
    @Config.RequiresMcRestart
    public boolean without_halfslabs = false;

    @Config.Comment({"Disable directly picking up layers from slabs and slab " +
                     "slices by left clicking while looking up/down."})
    @Config.Name("Without slab pickup")
    public boolean without_direct_slab_pickup = false;

    @Config.Comment({"Disable poles of any material."})
    @Config.Name("Without poles")
    @Config.RequiresMcRestart
    public boolean without_poles = false;

    @Config.Comment({"Disable horizontal supports like the double-T support."})
    @Config.Name("Without h. supports")
    @Config.RequiresMcRestart
    public boolean without_hsupports = false;

    @Config.Comment({"Disable CTRL-SHIFT item tooltip display."})
    @Config.Name("Without tooltips")
    public boolean without_tooltips = false;

    @Config.Comment({"Disable all tile entity special renderers."})
    @Config.Name("Without TESRs")
    public boolean without_tesrs = false;
  }

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
    @Config.RequiresMcRestart
    public boolean with_experimental = false;

    @Config.Comment({ "Disable all internal recipes, allowing to use alternative pack recipes." })
    @Config.Name("Without recipes")
    @Config.RequiresMcRestart
    public boolean without_recipes = false;

    @Config.Comment({"Disable registration of opt'ed out blocks. That is normally not a good idea. Your choice."})
    @Config.Name("Without opt-out registration")
    @Config.RequiresMcRestart
    public boolean without_optout_registration = false;
  }

  @Config.Comment({"Tweaks and block behaviour adaptions."})
  @Config.Name("Tweaks")
  public static final SettingsTweaks tweaks = new SettingsTweaks();
  public static final class SettingsTweaks
  {
    @Config.Comment({
      "Smelts ores to nuggets that are normally smelted to ingots, " +
      "if detectable in the Forge ore dict. Prefers IE recipe results. " +
      "The value can be changed on-the-fly for testing or age progression."
    })
    @Config.Name("Furnace: Nugget smelting")
    public boolean furnace_smelts_nuggets = false;

    @Config.Comment({
      "Defines, in percent, how fast the lab furnace smelts compared to " +
      "a vanilla furnace. 100% means vanilla furnace speed, 150% means the " +
      "lab furnace is faster. The value can be changed on-the-fly for tuning."
    })
    @Config.Name("Furnace: Smelting speed %")
    @Config.RangeInt(min=50, max=500)
    public int furnace_smelting_speed_percent = 130;

    @Config.Comment({
      "Defines, in percent, how fuel efficient the lab furnace is, compared " +
      "to a vanilla furnace. 100% means vanilla furnace consumiton, 200% means " +
      "the lab furnace needs about half the fuel of a vanilla furnace, " +
      "The value can be changed on-the-fly for tuning."
    })
    @Config.Name("Furnace: Fuel efficiency %")
    @Config.RangeInt(min=50, max=250)
    public int furnace_fuel_efficiency_percent = 100;

    @Config.Comment({
      "Defines the energy consumption (per tick) for speeding up the smelting process. " +
      "If IE is installed, an external heater has to be inserted into an auxiliary slot " +
      "of the lab furnace. The power source needs to be able to provide at least 4 times " +
      "this consumption (fixed threshold value). The value can be changed on-the-fly for tuning. " +
      "The default value corresponds to the IE heater consumption."
    })
    @Config.Name("Furnace: Boost energy")
    @Config.RangeInt(min=16, max=256)
    public int furnace_boost_energy_consumption = 24;

    @Config.Comment({
      "Defines, in percent, how high the probability is that a mob sits on a chair " +
      "when colliding with it. Can be changed on-the-fly for tuning."
    })
    @Config.Name("Chairs: Sitting chance %")
    @Config.RangeDouble(min=0.0, max=80)
    public double chair_mob_sitting_probability_percent = 10;

    @Config.Comment({
      "Defines, in percent, probable it is that a mob leaves a chair when sitting " +
      "on it. The 'dice is rolled' about every 20 ticks. There is also a minimum " +
      "Sitting time of about 3s. The config value can be changed on-the-fly for tuning."
    })
    @Config.Name("Chairs: Stand up chance %")
    @Config.RangeDouble(min=0.001, max=10)
    public double chair_mob_standup_probability_percent = 1;

    @Config.Comment({"Enables small quick-move arrows from/to player/block storage. " +
      "Makes the UI a bit too busy, therefore disabled by default."
    })
    @Config.Name("Crafting table: Move buttons")
    public boolean with_crafting_quickmove_buttons = false;

    @Config.Comment({
      "Defines how many millibuckets can be transferred (per tick) through the valves. " +
      "That is technically the 'storage size' specified for blocks that want to fill " +
      "fluids into the valve (the valve has no container and forward that to the output " +
      "block), The value can be changed on-the-fly for tuning. "
    })
    @Config.Name("Valves: Max flow rate")
    @Config.RangeInt(min=1, max=10000)
    public int pipevalve_max_flowrate = 1000;

    @Config.Comment({
      "Defines how many millibuckets per redstone signal strength can be transferred per tick " +
      "through the analog redstone controlled valves. Note: power 0 is always off, power 15 is always " +
      "the max flow rate. Between power 1 and 14 this scaler will result in a flow = 'redstone slope' * 'current redstone power'. " +
      "The value can be changed on-the-fly for tuning. "
    })
    @Config.Name("Valves: Redstone slope")
    @Config.RangeInt(min=1, max=10000)
    public int pipevalve_redstone_slope = 20;

    @Config.Comment({
      "Defines, in percent, how fast the electrical furnace smelts compared to " +
      "a vanilla furnace. 100% means vanilla furnace speed, 150% means the " +
      "electrical furnace is faster. The value can be changed on-the-fly for tuning."
    })
    @Config.Name("E-furnace: Smelting speed %")
    @Config.RangeInt(min=50, max=500)
    public int e_furnace_speed_percent = BlockDecorFurnaceElectrical.BTileEntity.DEFAULT_SPEED_PERCENT;

    @Config.Comment({
      "Defines how much RF per tick the the electrical furnace consumed (average) for smelting. " +
      "The feeders transferring items from/to adjacent have this consumption/8 for each stack transaction. " +
      "The default value is only slightly higher than a furnace with an IE external heater (and no burning fuel inside)." +
      "The config value can be changed on-the-fly for tuning."
    })
    @Config.Name("E-furnace: Power consumption")
    @Config.RangeInt(min=10, max=256)
    public int e_furnace_power_consumption = BlockDecorFurnaceElectrical.BTileEntity.DEFAULT_ENERGY_CONSUMPTION;

    @Config.Comment({
      "Defines the peak power production (at noon) of the Small Solar Panel. " +
      "Note that the agerage power is much less, as no power is produced at all during the night, " +
      "and the power curve is nonlinear rising/falling during the day. Bad weather conditions also " +
      "decrease the production. " +
      "The config value can be changed on-the-fly for tuning."
    })
    @Config.Name("Solar panel: Peak power")
    @Config.RangeInt(min=5, max=128)
    public int solar_panel_peak_power = BlockDecorSolarPanel.BTileEntity.DEFAULT_PEAK_POWER;
  }

  @SuppressWarnings("unused")
  @Mod.EventBusSubscriber(modid=ModEngineersDecor.MODID)
  private static final class EventHandler
  {
    @SubscribeEvent
    public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
      if(!event.getModID().equals(ModEngineersDecor.MODID)) return;
      ConfigManager.sync(ModEngineersDecor.MODID, Config.Type.INSTANCE);
      apply();
    }
  }

  @SuppressWarnings("unused")
  public static final void onPreInit()
  { apply(); }

  @SuppressWarnings("unused")
  public static final void onPostInit(FMLPostInitializationEvent event)
  { for(Block e: ModContent.getRegisteredBlocks()) ModConfig.isOptedOut(e, true); }

  private static final ArrayList<String> includes_ = new ArrayList<String>();
  private static final ArrayList<String> excludes_ = new ArrayList<String>();

  public static final boolean isWithoutOptOutRegistration()
  { return (zmisc!=null) && (zmisc.without_optout_registration); }

  public static final boolean isWithoutRecipes()
  { return (zmisc==null) || (zmisc.without_recipes); }

  public static boolean noToolTips()
  { return optout.without_tooltips; }

  public static final boolean isOptedOut(final @Nullable Block block)
  { return isOptedOut(block, false); }

  public static final boolean isOptedOut(final @Nullable Block block, boolean with_log_details)
  {
    if((block == null) || (optout==null)) return true;
    if(block == ModContent.SIGN_MODLOGO) return true;
    if((!zmisc.with_experimental) && (block instanceof ModAuxiliaries.IExperimentalFeature)) return true;
    final String rn = block.getRegistryName().getPath();
    // Force-include/exclude pattern matching
    try {
      for(String e:includes_) {
        if(rn.matches(e)) {
          if(with_log_details) ModEngineersDecor.logger.info("Optout force include: " + rn);
          return false;
        }
      }
      for(String e:excludes_) {
        if(rn.matches(e)) {
          if(with_log_details) ModEngineersDecor.logger.info("Optout force exclude: " + rn);
          return true;
        }
      }
    } catch(Throwable ex) {
      ModEngineersDecor.logger.error("optout include pattern failed, disabling.");
      includes_.clear();
      excludes_.clear();
    }
    // Early non-opt out type based evaluation
    if(block instanceof BlockDecorCraftingTable) return optout.without_crafting_table;
    if(block instanceof BlockDecorFurnaceElectrical) return optout.without_electrical_furnace;
    if((block instanceof BlockDecorFurnace) && (!(block instanceof BlockDecorFurnaceElectrical))) return optout.without_lab_furnace;
    if(block instanceof BlockDecorPassiveFluidAccumulator) return optout.without_passive_fluid_accumulator;
    if(block instanceof BlockDecorWasteIncinerator) return optout.without_waste_incinerator;
    if(block instanceof BlockDecorDropper) return optout.without_factory_dropper;
    if(block instanceof BlockDecorHopper) return optout.without_factory_hopper;
    if(block instanceof BlockDecorPlacer) return optout.without_factory_placer;
    if(block instanceof BlockDecorHalfSlab) return optout.without_halfslabs;
    if(block instanceof BlockDecorLadder) return optout.without_ladders;
    if(block instanceof BlockDecorWindow) return optout.without_windows;
    if(block instanceof BlockDecorPipeValve) return optout.without_valves;
    if(block instanceof BlockDecorHorizontalSupport) return optout.without_hsupports;
    // Type based evaluation where later filters may match, too
    if(optout.without_slabs && (block instanceof BlockDecorSlab)) return true;
    if(optout.without_stairs && (block instanceof BlockDecorStairs)) return true;
    if(optout.without_walls && (block instanceof BlockDecorWall)) return true;
    if(optout.without_poles && (block instanceof BlockDecorStraightPole)) return true;
    // String matching based evaluation
    if(optout.without_clinker_bricks && (rn.startsWith("clinker_brick_"))) return true;
    if(optout.without_slag_bricks && rn.startsWith("slag_brick_")) return true;
    if(optout.without_rebar_concrete && rn.startsWith("rebar_concrete")) return true;
    if(optout.without_ie_concrete_wall && rn.startsWith("concrete_wall")) return true;
    if(optout.without_panzer_glass && rn.startsWith("panzerglass_")) return true;
    if(optout.without_light_sources && rn.endsWith("_light")) return true;
    if(optout.without_sign_plates && rn.startsWith("sign_")) return true;
    if(optout.without_treated_wood_furniture) {
      if(block instanceof BlockDecorChair) return true;
      if(rn.equals("treated_wood_table")) return true;
      if(rn.equals("treated_wood_stool")) return true;
      if(rn.equals("treated_wood_windowsill")) return true;
    }
    return false;
  }

  public static final boolean isOptedOut(final @Nullable Item item)
  {
    if((item == null) || (optout == null)) return true;
    return false;
  }

  public static final void apply()
  {
    BlockDecorFurnace.BTileEntity.on_config(tweaks.furnace_smelting_speed_percent, tweaks.furnace_fuel_efficiency_percent, tweaks.furnace_boost_energy_consumption);
    ModRecipes.furnaceRecipeOverrideReset();
    if(tweaks.furnace_smelts_nuggets) ModRecipes.furnaceRecipeOverrideSmeltsOresToNuggets();
    BlockDecorChair.on_config(optout.without_chair_sitting, optout.without_mob_chair_sitting, tweaks.chair_mob_sitting_probability_percent, tweaks.chair_mob_standup_probability_percent);
    BlockDecorLadder.on_config(optout.without_ladder_speed_boost);
    BlockDecorCraftingTable.on_config(optout.without_crafting_table_history, false, tweaks.with_crafting_quickmove_buttons);
    BlockDecorPipeValve.on_config(tweaks.pipevalve_max_flowrate, tweaks.pipevalve_redstone_slope);
    BlockDecorFurnaceElectrical.BTileEntity.on_config(tweaks.e_furnace_speed_percent, tweaks.e_furnace_power_consumption);
    BlockDecorSolarPanel.BTileEntity.on_config(tweaks.solar_panel_peak_power);
    {
      optout.includes = optout.includes.toLowerCase().replaceAll(ModEngineersDecor.MODID+":", "").replaceAll("[^*_,a-z0-9]", "");
      if(!optout.includes.isEmpty()) ModEngineersDecor.logger.info("Pattern includes: '" + optout.includes + "'");
      String[] incl = optout.includes.split(",");
      includes_.clear();
      for(int i=0; i< incl.length; ++i) {
        incl[i] = incl[i].replaceAll("[*]", ".*?");
        if(!incl[i].isEmpty()) includes_.add(incl[i]);
      }
    }
    {
      optout.excludes = optout.excludes.toLowerCase().replaceAll(ModEngineersDecor.MODID+":", "").replaceAll("[^*_,a-z0-9]", "");
      if(!optout.excludes.isEmpty()) ModEngineersDecor.logger.info("Pattern excludes: '" + optout.excludes + "'");
      String[] excl = optout.excludes.split(",");
      excludes_.clear();
      for(int i=0; i< excl.length; ++i) {
        excl[i] = excl[i].replaceAll("[*]", ".*?");
        if(!excl[i].isEmpty()) excludes_.add(excl[i]);
      }
    }
  }

}
