/*
 * @file ModConfig.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main class for module settings. Handles reading and
 * saving the config file.
 */
package wile.engineersdecor;

import wile.engineersdecor.blocks.*;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;

public class ModConfig
{
  //--------------------------------------------------------------------------------------------------------------------
  private static final Logger LOGGER = ModEngineersDecor.logger();
  private static final String MODID = ModEngineersDecor.MODID;
  public static final CommonConfig COMMON;
  public static final ServerConfig SERVER;
  public static final ClientConfig CLIENT;
  public static final ForgeConfigSpec COMMON_CONFIG_SPEC;
  public static final ForgeConfigSpec SERVER_CONFIG_SPEC;
  public static final ForgeConfigSpec CLIENT_CONFIG_SPEC;

  static {
    final Pair<CommonConfig, ForgeConfigSpec> common_ = (new ForgeConfigSpec.Builder()).configure(CommonConfig::new);
    COMMON_CONFIG_SPEC = common_.getRight();
    COMMON = common_.getLeft();
    final Pair<ServerConfig, ForgeConfigSpec> server_ = (new ForgeConfigSpec.Builder()).configure(ServerConfig::new);
    SERVER_CONFIG_SPEC = server_.getRight();
    SERVER = server_.getLeft();
    final Pair<ClientConfig, ForgeConfigSpec> client_ = (new ForgeConfigSpec.Builder()).configure(ClientConfig::new);
    CLIENT_CONFIG_SPEC = client_.getRight();
    CLIENT = client_.getLeft();
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class ClientConfig
  {
    public final ForgeConfigSpec.BooleanValue without_tooltips;
    public final ForgeConfigSpec.BooleanValue without_ters;

    ClientConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings not loaded on servers.")
             .push("client");
      // --- OPTOUTS ------------------------------------------------------------
      {
        builder.comment("Opt-out settings")
               .push("optout");
        without_tooltips = builder
          .translation(MODID + ".config.without_tooltips")
          .comment("Disable CTRL-SHIFT item tooltip display.")
          .define("without_tooltips", false);
        without_ters = builder
          .translation(MODID + ".config.without_ters")
          .comment("Disable all TERs (tile entity renderers).")
          .define("without_ters", false);
      }
      builder.pop();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class ServerConfig
  {
    ServerConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings not loaded on clients.")
             .push("server");
      builder.pop();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class CommonConfig
  {
    // Optout
    public final ForgeConfigSpec.ConfigValue<String> pattern_excludes;
    public final ForgeConfigSpec.ConfigValue<String> pattern_includes;
    public final ForgeConfigSpec.BooleanValue without_clinker_bricks;
    public final ForgeConfigSpec.BooleanValue without_slag_bricks;
    public final ForgeConfigSpec.BooleanValue without_rebar_concrete;
    public final ForgeConfigSpec.BooleanValue without_gas_concrete;
    public final ForgeConfigSpec.BooleanValue without_walls;
    public final ForgeConfigSpec.BooleanValue without_stairs;
    public final ForgeConfigSpec.BooleanValue without_ie_concrete_wall;
    public final ForgeConfigSpec.BooleanValue without_panzer_glass;
    public final ForgeConfigSpec.BooleanValue without_ladders;
    public final ForgeConfigSpec.BooleanValue without_treated_wood_furniture;
    public final ForgeConfigSpec.BooleanValue without_metal_furniture;
    public final ForgeConfigSpec.BooleanValue without_windows;
    public final ForgeConfigSpec.BooleanValue without_light_sources;
    public final ForgeConfigSpec.BooleanValue without_slabs;
    public final ForgeConfigSpec.BooleanValue without_halfslabs;
    public final ForgeConfigSpec.BooleanValue without_poles;
    public final ForgeConfigSpec.BooleanValue without_hsupports;
    public final ForgeConfigSpec.BooleanValue without_sign_plates;
    public final ForgeConfigSpec.BooleanValue without_floor_grating;
    public final ForgeConfigSpec.BooleanValue without_crafting_table;
    public final ForgeConfigSpec.BooleanValue without_lab_furnace;
    public final ForgeConfigSpec.BooleanValue without_electrical_furnace;
    public final ForgeConfigSpec.BooleanValue without_valves;
    public final ForgeConfigSpec.BooleanValue without_passive_fluid_accumulator;
    public final ForgeConfigSpec.BooleanValue without_waste_incinerator;
    public final ForgeConfigSpec.BooleanValue without_factory_dropper;
    public final ForgeConfigSpec.BooleanValue without_factory_hopper;
    public final ForgeConfigSpec.BooleanValue without_factory_placer;
    public final ForgeConfigSpec.BooleanValue without_block_breaker;
    public final ForgeConfigSpec.BooleanValue without_solar_panel;
    public final ForgeConfigSpec.BooleanValue without_fluid_funnel;
    public final ForgeConfigSpec.BooleanValue without_mineral_smelter;
    public final ForgeConfigSpec.BooleanValue without_milking_machine;
    public final ForgeConfigSpec.BooleanValue without_tree_cutter;
    public final ForgeConfigSpec.BooleanValue without_labeled_crate;
    public final ForgeConfigSpec.BooleanValue without_fences;
    public final ForgeConfigSpec.BooleanValue without_chair_sitting;
    public final ForgeConfigSpec.BooleanValue without_mob_chair_sitting;
    public final ForgeConfigSpec.BooleanValue without_ladder_speed_boost;
    public final ForgeConfigSpec.BooleanValue without_crafting_table_history;
    public final ForgeConfigSpec.BooleanValue without_direct_slab_pickup;
    public final ForgeConfigSpec.BooleanValue with_creative_mode_device_drops;
    // Misc
    public final ForgeConfigSpec.BooleanValue with_experimental;
    public final ForgeConfigSpec.BooleanValue without_recipes;
    // Tweaks
    public final ForgeConfigSpec.IntValue furnace_smelting_speed_percent;
    public final ForgeConfigSpec.IntValue furnace_fuel_efficiency_percent;
    public final ForgeConfigSpec.IntValue furnace_boost_energy_consumption;
    public final ForgeConfigSpec.IntValue e_furnace_speed_percent;
    public final ForgeConfigSpec.IntValue e_furnace_power_consumption;
    public final ForgeConfigSpec.IntValue small_solar_panel_peak_production;
    public final ForgeConfigSpec.BooleanValue e_furnace_automatic_pulling;
    public final ForgeConfigSpec.DoubleValue chair_mob_sitting_probability_percent;
    public final ForgeConfigSpec.DoubleValue chair_mob_standup_probability_percent;
    public final ForgeConfigSpec.BooleanValue with_crafting_quickmove_buttons;
    public final ForgeConfigSpec.BooleanValue without_crafting_mouse_scrolling;
    public final ForgeConfigSpec.IntValue pipevalve_max_flowrate;
    public final ForgeConfigSpec.IntValue pipevalve_redstone_gain;
    public final ForgeConfigSpec.IntValue block_breaker_power_consumption;
    public final ForgeConfigSpec.IntValue block_breaker_reluctance;
    public final ForgeConfigSpec.IntValue block_breaker_min_breaking_time;
    public final ForgeConfigSpec.BooleanValue block_breaker_requires_power;
    public final ForgeConfigSpec.IntValue tree_cuttter_energy_consumption;
    public final ForgeConfigSpec.IntValue tree_cuttter_cutting_time_needed;
    public final ForgeConfigSpec.BooleanValue tree_cuttter_requires_power;
    public final ForgeConfigSpec.IntValue milking_machine_energy_consumption;
    public final ForgeConfigSpec.IntValue milking_machine_milking_delay;

    CommonConfig(ForgeConfigSpec.Builder builder)
    {
      builder.comment("Settings affecting the logical server side, but are also configurable in single player.")
             .push("server");
      // --- OPTOUTS ------------------------------------------------------------
      {
        builder.comment("Opt-out settings")
               .push("optout");
        pattern_excludes = builder
          .translation(MODID + ".config.pattern_excludes")
          .comment("Opt-out any block by its registry name ('*' wildcard matching, "
            + "comma separated list, whitespaces ignored. You must match the whole name, "
            + "means maybe add '*' also at the begin and end. Example: '*wood*,*steel*' "
            + "excludes everything that has 'wood' or 'steel' in the registry name. "
            + "The matching result is also traced in the log file. ")
          .define("pattern_excludes", "");
        pattern_includes = builder
          .translation(MODID + ".config.pattern_includes")
          .comment("Prevent blocks from being opt'ed by registry name ('*' wildcard matching, "
            + "comma separated list, whitespaces ignored. Evaluated before all other opt-out checks. "
            + "You must match the whole name, means maybe add '*' also at the begin and end. Example: "
            + "'*wood*,*steel*' includes everything that has 'wood' or 'steel' in the registry name."
            + "The matching result is also traced in the log file.")
          .define("pattern_includes", "");
        without_clinker_bricks = builder
          .translation(MODID + ".config.without_clinker_bricks")
          .comment("Disable clinker bricks and derived blocks.")
          .define("without_clinker_bricks", false);
        without_slag_bricks = builder
          .translation(MODID + ".config.without_slag_bricks")
          .comment("Disable slag bricks and derived blocks.")
          .define("without_slag_bricks", false);
        without_rebar_concrete = builder
          .translation(MODID + ".config.without_rebar_concrete")
          .comment("Disable rebar concrete and derived blocks.")
          .define("without_rebar_concrete", false);
        without_gas_concrete = builder
          .translation(MODID + ".config.without_gas_concrete")
          .comment("Disable gas concrete and derived blocks.")
          .define("without_gas_concrete", false);
        without_walls = builder
          .translation(MODID + ".config.without_walls")
          .comment("Disable all mod wall blocks.")
          .define("without_walls", false);
        without_stairs = builder
          .translation(MODID + ".config.without_stairs")
          .comment("Disable all mod stairs blocks.")
          .define("without_stairs", false);
        without_ie_concrete_wall = builder
          .translation(MODID + ".config.without_ie_concrete_wall")
          .comment("Disable IE concrete wall.")
          .define("without_ie_concrete_wall", false);
        without_panzer_glass = builder
          .translation(MODID + ".config.without_panzer_glass")
          .comment("Disable panzer glass and derived blocks.")
          .define("without_panzer_glass", false);
        without_crafting_table = builder
          .translation(MODID + ".config.without_crafting_table")
          .comment("Disable treated wood crafting table.")
          .define("without_crafting_table", false);
        without_lab_furnace = builder
          .translation(MODID + ".config.without_lab_furnace")
          .comment("Disable small lab furnace.")
          .define("without_lab_furnace", false);
        without_electrical_furnace = builder
          .translation(MODID + ".config.without_electrical_furnace")
          .comment("Disable small electrical pass-through furnace.")
          .define("without_electrical_furnace", false);
        without_treated_wood_furniture = builder
          .translation(MODID + ".config.without_treated_wood_furniture")
          .comment("Disable treated wood table, stool, windowsill, etc.")
          .define("without_treated_wood_furniture", false);
        without_metal_furniture = builder
          .translation(MODID + ".config.without_metal_furniture")
          .comment("Disable metal tables, etc.")
          .define("without_metal_furniture", false);
        without_windows = builder
          .translation(MODID + ".config.without_windows")
          .comment("Disable treated wood window, etc.")
          .define("without_windows", false);
        without_light_sources = builder
          .translation(MODID + ".config.without_light_sources")
          .comment("Disable light sources")
          .define("without_light_sources", false);
        without_ladders = builder
          .translation(MODID + ".config.without_ladders")
          .comment("Disable ladders")
          .define("without_ladders", false);
        without_chair_sitting = builder
          .translation(MODID + ".config.without_chair_sitting")
          .comment("Disable possibility to sit on stools and chairs.")
          .define("without_chair_sitting", false);
        without_mob_chair_sitting = builder
          .translation(MODID + ".config.without_mob_chair_sitting")
          .comment("Disable that mobs will sit on chairs and stools.")
          .define("without_mob_chair_sitting", false);
        without_ladder_speed_boost = builder
          .translation(MODID + ".config.without_ladder_speed_boost")
          .comment("Disable the speed boost of ladders in this mod.")
          .define("without_ladder_speed_boost", false);
        without_crafting_table_history = builder
          .translation(MODID + ".config.without_crafting_table_history")
          .comment("Disable history refabrication feature of the treated wood crafting table.")
          .define("without_crafting_table_history", false);
        without_valves = builder
          .translation(MODID + ".config.without_valves")
          .comment("Disable check valve, and redstone controlled valves.")
          .define("without_valves", false);
        without_passive_fluid_accumulator = builder
          .translation(MODID + ".config.without_passive_fluid_accumulator")
          .comment("Disable the passive fluid accumulator.")
          .define("without_passive_fluid_accumulator", false);
        without_waste_incinerator = builder
          .translation(MODID + ".config.without_waste_incinerator")
          .comment("Disable item disposal/trash/void incinerator device.")
          .define("without_waste_incinerator", false);
        without_sign_plates = builder
          .translation(MODID + ".config.without_sign_plates")
          .comment("Disable decorative sign plates (caution, hazards, etc).")
          .define("without_sign_plates", false);
        without_floor_grating = builder
          .translation(MODID + ".config.without_floor_grating")
          .comment("Disable floor gratings.")
          .define("without_floor_grating", false);
        without_factory_dropper = builder
          .translation(MODID + ".config.without_factory_dropper")
          .comment("Disable the factory dropper.")
          .define("without_factory_dropper", false);
        without_factory_hopper = builder
          .translation(MODID + ".config.without_factory_hopper")
          .comment("Disable the factory hopper.")
          .define("without_factory_hopper", false);
        without_factory_placer = builder
          .translation(MODID + ".config.without_factory_placer")
          .comment("Disable the factory placer.")
          .define("without_factory_placer", false);
        without_block_breaker = builder
          .translation(MODID + ".config.without_block_breaker")
          .comment("Disable the small block breaker.")
          .define("without_block_breaker", false);
        without_solar_panel = builder
          .translation(MODID + ".config.without_solar_panel")
          .comment("Disable the small solar panel.")
          .define("without_solar_panel", false);
        without_fluid_funnel = builder
          .translation(MODID + ".config.without_fluid_funnel")
          .comment("Disable the small fluid collection funnel.")
          .define("without_fluid_funnel", false);
        without_mineral_smelter = builder
          .translation(MODID + ".config.without_mineral_smelter")
          .comment("Disable the small mineral smelter.")
          .define("without_mineral_smelter", false);
        without_milking_machine = builder
          .translation(MODID + ".config.without_milking_machine")
          .comment("Disable the small milking machine.")
          .define("without_milking_machine", false);
        without_tree_cutter = builder
          .translation(MODID + ".config.without_tree_cutter")
          .comment("Disable the small tree cutter.")
          .define("without_tree_cutter", false);
        without_labeled_crate = builder
          .translation(MODID + ".config.without_labeled_crate")
          .comment("Disable labeled crate.")
          .define("without_labeled_crate", false);
        without_slabs = builder
          .translation(MODID + ".config.without_slabs")
          .comment("Disable horizontal half-block slab.")
          .define("without_slabs", false);
        without_halfslabs = builder
          .translation(MODID + ".config.without_halfslabs")
          .comment("Disable stackable 1/8 block slices.")
          .define("without_halfslabs", false);
        without_poles = builder
          .translation(MODID + ".config.without_poles")
          .comment("Disable poles of any material.")
          .define("without_poles", false);
        without_hsupports = builder
          .translation(MODID + ".config.without_hsupports")
          .comment("Disable horizontal supports like the double-T support.")
          .define("without_hsupports", false);
        without_recipes = builder
          .translation(MODID + ".config.without_recipes")
          .comment("Disable all internal recipes, allowing to use alternative pack recipes.")
          .define("without_recipes", false);
        without_fences = builder
          .translation(MODID + ".config.without_fences")
          .comment("Disable all fences and fence gates.")
          .define("without_fences", false);
        builder.pop();
      }
      // --- MISC ---------------------------------------------------------------
      {
        builder.comment("Miscellaneous settings")
               .push("miscellaneous");
        with_experimental = builder
          .translation(MODID + ".config.with_experimental")
          .comment("Enables experimental features. Use at own risk.")
          .define("with_experimental", false);
        without_direct_slab_pickup = builder
          .translation(MODID + ".config.without_direct_slab_pickup")
          .comment("Disable directly picking up layers from slabs and slab " +
            " slices by left clicking while looking up/down.")
          .define("without_direct_slab_pickup", false);
        with_creative_mode_device_drops = builder
          .translation(MODID + ".config.with_creative_mode_device_drops")
          .comment("Enable that devices are dropped as item also in creative mode, allowing " +
            " to relocate them with contents and settings.")
          .define("with_creative_mode_device_drops", false);
        builder.pop();
      }
      // --- TWEAKS -------------------------------------------------------------
      {
        builder.comment("Tweaks")
               .push("tweaks");
        furnace_smelting_speed_percent = builder
          .translation(MODID + ".config.furnace_smelting_speed_percent")
          .comment("Defines, in percent, how fast the lab furnace smelts compared to " +
            "a vanilla furnace. 100% means vanilla furnace speed, 150% means the " +
            "lab furnace is faster. The value can be changed on-the-fly for tuning.")
          .defineInRange("furnace_smelting_speed_percent", 130, 50, 800);
        furnace_fuel_efficiency_percent = builder
          .translation(MODID + ".config.furnace_fuel_efficiency_percent")
          .comment("Defines, in percent, how fuel efficient the lab furnace is, compared " +
            "to a vanilla furnace. 100% means vanilla furnace consumiton, 200% means " +
            "the lab furnace needs about half the fuel of a vanilla furnace, " +
            "The value can be changed on-the-fly for tuning.")
          .defineInRange("furnace_fuel_efficiency_percent", 100, 50, 400);
        furnace_boost_energy_consumption = builder
          .translation(MODID + ".config.furnace_boost_energy_consumption")
          .comment("Defines the energy consumption (per tick) for speeding up the smelting process. " +
            "If IE is installed, an external heater has to be inserted into an auxiliary slot " +
            "of the lab furnace. The power source needs to be able to provide at least 4 times " +
            "this consumption (fixed threshold value). The value can be changed on-the-fly for tuning. " +
            "The default value corresponds to the IE heater consumption.")
          .defineInRange("furnace_boost_energy_consumption", 24, 2, 1024);
        chair_mob_sitting_probability_percent = builder
          .translation(MODID + ".config.chair_mob_sitting_probability_percent")
          .comment("Defines, in percent, how high the probability is that a mob sits on a chair " +
            "when colliding with it. Can be changed on-the-fly for tuning.")
          .defineInRange("chair_mob_sitting_probability_percent", 10.0, 0.0, 80.0);
        chair_mob_standup_probability_percent = builder
          .translation(MODID + ".config.chair_mob_standup_probability_percent")
          .comment("Defines, in percent, probable it is that a mob leaves a chair when sitting " +
            "on it. The 'dice is rolled' about every 20 ticks. There is also a minimum " +
            "Sitting time of about 3s. The config value can be changed on-the-fly for tuning.")
          .defineInRange("chair_mob_standup_probability_percent", 1.0, 1e-3, 10.0);
        with_crafting_quickmove_buttons = builder
          .translation(MODID + ".config.with_crafting_quickmove_buttons")
          .comment("Enables small quick-move arrows from/to player/block storage. " +
            "Makes the UI a bit too busy, therefore disabled by default.")
          .define("with_crafting_quickmove_buttons", false);
        without_crafting_mouse_scrolling = builder
          .translation(MODID + ".config.without_crafting_mouse_scrolling")
          .comment("Disables increasing/decreasing the crafting grid items by scrolling over the crafting result slot.")
          .define("without_crafting_mouse_scrolling", false);
        pipevalve_max_flowrate = builder
          .translation(MODID + ".config.pipevalve_max_flowrate")
          .comment("Defines how many millibuckets can be transferred (per tick) through the valves. " +
            "That is technically the 'storage size' specified for blocks that want to fill " +
            "fluids into the valve (the valve has no container and forward that to the output " +
            "block), The value can be changed on-the-fly for tuning. ")
          .defineInRange("pipevalve_max_flowrate", 1000, 1, 32000);
        pipevalve_redstone_gain = builder
          .translation(MODID + ".config.pipevalve_redstone_gain")
          .comment("Defines how many millibuckets per redstone signal strength can be transferred per tick " +
            "through the analog redstone controlled valves. Note: power 0 is always off, power 15 is always " +
            "the max flow rate. Between power 1 and 14 this scaler will result in a flow = 'redstone slope' * 'current redstone power'. " +
            "The value can be changed on-the-fly for tuning. ")
          .defineInRange("pipevalve_redstone_gain", 20, 1, 32000);
        e_furnace_speed_percent = builder
          .translation(MODID + ".config.e_furnace_speed_percent")
          .comment("Defines, in percent, how fast the electrical furnace smelts compared to " +
            "a vanilla furnace. 100% means vanilla furnace speed, 150% means the " +
            "electrical furnace is faster. The value can be changed on-the-fly for tuning.")
          .defineInRange("e_furnace_speed_percent", EdElectricalFurnace.ElectricalFurnaceTileEntity.DEFAULT_SPEED_PERCENT, 50, 800);
        e_furnace_power_consumption = builder
          .translation(MODID + ".config.e_furnace_power_consumption")
          .comment("Defines how much RF per tick the the electrical furnace consumed (average) for smelting. " +
            "The feeders transferring items from/to adjacent have this consumption/8 for each stack transaction. " +
            "The default value is only slightly higher than a furnace with an IE external heater (and no burning fuel inside)." +
            "The config value can be changed on-the-fly for tuning.")
          .defineInRange("e_furnace_power_consumption", EdElectricalFurnace.ElectricalFurnaceTileEntity.DEFAULT_ENERGY_CONSUMPTION, 8, 4096);
        e_furnace_automatic_pulling = builder
          .translation(MODID + ".config.e_furnace_automatic_pulling")
          .comment("Defines if the electrical furnace automatically pulls items from an inventory at the input side." +
            "The config value can be changed on-the-fly for tuning.")
          .define("e_furnace_automatic_pulling", false);
        small_solar_panel_peak_production = builder
          .translation(MODID + ".config.small_solar_panel_peak_production")
          .comment("Defines the peak power production (at noon) of the Small Solar Panel. " +
            "Note that the agerage power is much less, as no power is produced at all during the night, " +
            "and the power curve is nonlinear rising/falling during the day. Bad weather conditions also " +
            "decrease the production. The config value can be changed on-the-fly for tuning.")
          .defineInRange("small_solar_panel_peak_production", EdSolarPanel.SolarPanelTileEntity.DEFAULT_PEAK_POWER, 2, 4096);
        block_breaker_power_consumption = builder
          .translation(MODID + ".config.block_breaker_power_consumption")
          .comment("Defines how much RF power the Small Block Breaker requires to magnificently increase the processing speed. " +
            "The config value can be changed on-the-fly for tuning.")
          .defineInRange("block_breaker_power_consumption", EdBreaker.BreakerTileEntity.DEFAULT_BOOST_ENERGY, 4, 1024);
        block_breaker_reluctance = builder
          .translation(MODID + ".config.block_breaker_reluctance")
          .comment("Defines how much time the Small Block Breaker needs per block hardness, " +
            "means: 'reluctance' * hardness + min_time, you change the 'reluctance' here." +
            "The unit is ticks/hardness. " + "The config value can be changed on-the-fly for tuning.")
          .defineInRange("block_breaker_reluctance", EdBreaker.BreakerTileEntity.DEFAULT_BREAKING_RELUCTANCE, 5, 50);
        block_breaker_min_breaking_time = builder
          .translation(MODID + ".config.block_breaker_min_breaking_time")
          .comment("Defines how much time the Small Block Breaker needs at least, better said it's an offset: " +
            "'reluctance' * hardness + min_time, you change the 'min_time' here, value " +
            "in ticks." + "The config value can be changed on-the-fly for tuning.")
          .defineInRange("block_breaker_min_breaking_time", EdBreaker.BreakerTileEntity.DEFAULT_MIN_BREAKING_TIME, 10, 100);
        block_breaker_requires_power = builder
          .translation(MODID + ".config.block_breaker_requires_power")
          .comment("Defines if the Small Block Breaker does not work without RF power.")
          .define("block_breaker_requires_power", false);
        tree_cuttter_energy_consumption = builder
          .translation(MODID + ".config.tree_cuttter_energy_consumption")
          .comment("Defines how much RF power the Small Tree Cutter requires to magnificently increase the processing speed. " +
            "The config value can be changed on-the-fly for tuning.")
          .defineInRange("tree_cuttter_energy_consumption", EdTreeCutter.TreeCutterTileEntity.DEFAULT_BOOST_ENERGY, 4, 1024);
        tree_cuttter_cutting_time_needed = builder
          .translation(MODID + ".config.tree_cuttter_cutting_time_needed")
          .comment("Defines how much time the Small Tree Cutter needs to cut a tree without RF power. " +
            "The value is in seconds. With energy it is 6 times faster. " +
            "The config value can be changed on-the-fly for tuning.")
          .defineInRange("tree_cuttter_cutting_time_needed", EdTreeCutter.TreeCutterTileEntity.DEFAULT_CUTTING_TIME_NEEDED, 10, 240);
        tree_cuttter_requires_power = builder
          .translation(MODID + ".config.tree_cuttter_requires_power")
          .comment("Defines if the Small Tree Cutter does not work without RF power.")
          .define("tree_cuttter_requires_power", false);
        milking_machine_energy_consumption = builder
          .translation(MODID + ".config.milking_machine_energy_consumption")
          .comment("Defines how much time the Small Milking Machine needs work. " +
            "Note this is a permanent standby power, not only when the device does something. " +
            "Use zero to disable energy dependency and energy handling of the machine. " +
            "The config value can be changed on-the-fly for tuning.")
          .defineInRange("milking_machine_energy_consumption", EdMilker.MilkerTileEntity.DEFAULT_ENERGY_CONSUMPTION, 0, 1024);
        milking_machine_milking_delay = builder
          .translation(MODID + ".config.milking_machine_milking_delay")
          .comment("Defines (for each individual cow) the minimum time between milking." )
          .defineInRange("milking_machine_milking_delay", EdMilker.MilkerTileEntity.DEFAULT_MILKING_DELAY_PER_COW, 1000, 24000);
        builder.pop();
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Optout checks
  //--------------------------------------------------------------------------------------------------------------------

  public static final boolean isOptedOut(final @Nullable Block block)
  { return isOptedOut(block.asItem()); }

  public static final boolean isOptedOut(final @Nullable Item item)
  { return (item!=null) && optouts_.contains(item.getRegistryName().getPath()); }

  public static boolean withExperimental()
  { return with_experimental_features_; }

  public static boolean withoutRecipes()
  { return without_recipes_; }

  //--------------------------------------------------------------------------------------------------------------------
  // Cache
  //--------------------------------------------------------------------------------------------------------------------

  private static final CompoundNBT server_config_ = new CompoundNBT();
  private static HashSet<String> optouts_ = new HashSet<>();
  private static boolean with_experimental_features_ = false;
  private static boolean without_recipes_ = false;
  public static boolean without_crafting_table = false;
  public static boolean immersiveengineering_installed = false;
  public static boolean without_direct_slab_pickup = false;
  public static boolean with_creative_mode_device_drops = false;

  public static final CompoundNBT getServerConfig() // config that may be synchronized from server to client via net pkg.
  { return server_config_; }

  private static final void updateOptouts()
  {
    final ArrayList<String> includes_ = new ArrayList<String>();
    final ArrayList<String> excludes_ = new ArrayList<String>();
    {
      String inc = COMMON.pattern_includes.get().toLowerCase().replaceAll(MODID+":", "").replaceAll("[^*_,a-z0-9]", "");
      if(COMMON.pattern_includes.get() != inc) COMMON.pattern_includes.set(inc);
      if(!inc.isEmpty()) LOGGER.info("Config pattern includes: '" + inc + "'");
      String[] incl = inc.split(",");
      includes_.clear();
      for(int i=0; i< incl.length; ++i) {
        incl[i] = incl[i].replaceAll("[*]", ".*?");
        if(!incl[i].isEmpty()) includes_.add(incl[i]);
      }
    }
    {
      String exc = COMMON.pattern_excludes.get().toLowerCase().replaceAll(MODID+":", "").replaceAll("[^*_,a-z0-9]", "");
      if(!exc.isEmpty()) LOGGER.info("Config pattern excludes: '" + exc + "'");
      String[] excl = exc.split(",");
      excludes_.clear();
      for(int i=0; i< excl.length; ++i) {
        excl[i] = excl[i].replaceAll("[*]", ".*?");
        if(!excl[i].isEmpty()) excludes_.add(excl[i]);
      }
    }
    {
      boolean with_log_details = false;
      HashSet<String> optouts = new HashSet<>();
      ModContent.getRegisteredItems().stream().filter((Item item) -> {
        if(item == null) return true;
        if(SERVER == null) return false;
        return false;
      }).forEach(
        e -> optouts.add(e.getRegistryName().getPath())
      );
      ModContent.getRegisteredBlocks().stream().filter((Block block) -> {
        if(block==null) return true;
        if(block==ModContent.SIGN_MODLOGO) return true;
        if(COMMON==null) return false;
        try {
          if(!COMMON.with_experimental.get()) {
            if(block instanceof Auxiliaries.IExperimentalFeature) return true;
            if(ModContent.isExperimentalBlock(block)) return true;
          }
          final String rn = block.getRegistryName().getPath();
          // Hard IE dependent blocks
          if(!immersiveengineering_installed) {
            if(block==ModContent.CONCRETE_WALL) return true;
            if((block instanceof DecorBlock.Normal)&&((((DecorBlock.Normal)block).config&DecorBlock.CFG_HARD_IE_DEPENDENT)!=0))
              return true;
            if((block instanceof StandardBlocks.BaseBlock)&&((((StandardBlocks.BaseBlock)block).config&DecorBlock.CFG_HARD_IE_DEPENDENT)!=0))
              return true;
          }
          // Force-include/exclude pattern matching
          try {
            for(String e : includes_) {
              if(rn.matches(e)) {
                if(with_log_details) LOGGER.info("Optout force include: "+rn);
                return false;
              }
            }
            for(String e : excludes_) {
              if(rn.matches(e)) {
                if(with_log_details) LOGGER.info("Optout force exclude: "+rn);
                return true;
              }
            }
          } catch(Throwable ex) {
            LOGGER.error("optout include pattern failed, disabling.");
            includes_.clear();
            excludes_.clear();
          }
          // Early non-opt out type based evaluation
          if(block instanceof EdCraftingTable.CraftingTableBlock) return COMMON.without_crafting_table.get();
          if(block instanceof EdElectricalFurnace.ElectricalFurnaceBlock) return COMMON.without_electrical_furnace.get();
          if((block instanceof EdFurnace.FurnaceBlock)&&(!(block instanceof EdElectricalFurnace.ElectricalFurnaceBlock))) return COMMON.without_lab_furnace.get();
          if(block instanceof EdFluidAccumulator.FluidAccumulatorBlock) return COMMON.without_passive_fluid_accumulator.get();
          if(block instanceof EdWasteIncinerator.WasteIncineratorBlock) return COMMON.without_waste_incinerator.get();
          if(block instanceof EdDropper.DropperBlock) return COMMON.without_factory_dropper.get();
          if(block instanceof EdPlacer.PlacerBlock) return COMMON.without_factory_placer.get();
          if(block instanceof EdBreaker.BreakerBlock) return COMMON.without_block_breaker.get();
          if(block instanceof EdSlabSliceBlock) return COMMON.without_halfslabs.get();
          if(block instanceof EdLadderBlock) return COMMON.without_ladders.get();
          if(block instanceof EdWindowBlock) return COMMON.without_windows.get();
          if(block instanceof EdPipeValve.PipeValveBlock) return COMMON.without_valves.get();
          if(block instanceof EdHorizontalSupportBlock) return COMMON.without_hsupports.get();
          if(block instanceof EdFloorGratingBlock) return COMMON.without_floor_grating.get();
          if(block instanceof EdHopper.HopperBlock) return COMMON.without_factory_hopper.get();
          if(block instanceof EdFluidFunnel.FluidFunnelBlock) return COMMON.without_fluid_funnel.get();
          if(block instanceof EdSolarPanel.SolarPanelBlock) return COMMON.without_solar_panel.get();
          if(block instanceof EdMineralSmelter.MineralSmelterBlock) return COMMON.without_mineral_smelter.get();
          if(block instanceof EdMilker.MilkerBlock) return COMMON.without_milking_machine.get();
          if(block instanceof EdTreeCutter.TreeCutterBlock) return COMMON.without_tree_cutter.get();
          if(block instanceof EdLabeledCrate.LabeledCrateBlock) return COMMON.without_labeled_crate.get();
          // Type based evaluation where later filters may match, too
          if(COMMON.without_slabs.get()&&(block instanceof EdSlabBlock)) return true;
          if(COMMON.without_stairs.get()&&(block instanceof EdStairsBlock)) return true;
          if(COMMON.without_walls.get()&&(block instanceof EdWallBlock)) return true;
          if(COMMON.without_poles.get()&&(block instanceof EdStraightPoleBlock)) return true;
          // String matching based evaluation
          if(COMMON.without_clinker_bricks.get()&&(rn.startsWith("clinker_brick_"))) return true;
          if(COMMON.without_slag_bricks.get()&&rn.startsWith("slag_brick_")) return true;
          if(COMMON.without_rebar_concrete.get()&&rn.startsWith("rebar_concrete")) return true;
          if(COMMON.without_gas_concrete.get()&&rn.startsWith("gas_concrete")) return true;
          if(COMMON.without_ie_concrete_wall.get()&&rn.startsWith("concrete_wall")) return true;
          if(COMMON.without_panzer_glass.get()&&rn.startsWith("panzerglass_")) return true;
          if(COMMON.without_light_sources.get()&&rn.endsWith("_light")) return true;
          if(COMMON.without_sign_plates.get()&&rn.startsWith("sign_")) return true;
          if(COMMON.without_treated_wood_furniture.get()) {
            if(block instanceof EdChair.ChairBlock) return true;
            if(rn.equals("treated_wood_table")) return true;
            if(rn.equals("treated_wood_stool")) return true;
            if(rn.equals("treated_wood_windowsill")) return true;
            if(rn.equals("treated_wood_broad_windowsill")) return true;
            if(rn.equals("treated_wood_side_table")) return true;
          }
          if(COMMON.without_metal_furniture.get()) {
            if(rn.equals("steel_table")) return true;
          }
          if(COMMON.without_fences.get()) {
            if(block instanceof EdFenceBlock) return true;
            if(block instanceof EdDoubleGateBlock) return true;
          }
        } catch(Exception ex) {
          LOGGER.error("Exception evaluating the optout config: '"+ex.getMessage()+"'");
        }
        return false;
      }).forEach(
        e -> optouts.add(e.getRegistryName().getPath())
      );
      optouts_ = optouts;
    }
  }

  public static final void apply()
  {
    with_experimental_features_ = COMMON.with_experimental.get();
    if(with_experimental_features_) LOGGER.info("Config: EXPERIMENTAL FEATURES ENABLED.");
    immersiveengineering_installed = Auxiliaries.isModLoaded("immersiveengineering");
    updateOptouts();
    without_crafting_table = isOptedOut(ModContent.TREATED_WOOD_CRAFTING_TABLE);
    without_recipes_ = COMMON.without_recipes.get();
    without_direct_slab_pickup = COMMON.without_direct_slab_pickup.get();
    // -----------------------------------------------------------------------------------------------------------------
    EdFurnace.FurnaceTileEntity.on_config(COMMON.furnace_smelting_speed_percent.get(), COMMON.furnace_fuel_efficiency_percent.get(), COMMON.furnace_boost_energy_consumption.get());
    EdChair.on_config(COMMON.without_chair_sitting.get(), COMMON.without_mob_chair_sitting.get(), COMMON.chair_mob_sitting_probability_percent.get(), COMMON.chair_mob_standup_probability_percent.get());
    EdLadderBlock.on_config(COMMON.without_ladder_speed_boost.get());
    EdCraftingTable.on_config(COMMON.without_crafting_table_history.get(), false, COMMON.with_crafting_quickmove_buttons.get(), COMMON.without_crafting_mouse_scrolling.get());
    EdPipeValve.on_config(COMMON.pipevalve_max_flowrate.get(), COMMON.pipevalve_redstone_gain.get());
    EdElectricalFurnace.ElectricalFurnaceTileEntity.on_config(COMMON.e_furnace_speed_percent.get(), COMMON.e_furnace_power_consumption.get(), COMMON.e_furnace_automatic_pulling.get());
    EdSolarPanel.SolarPanelTileEntity.on_config(COMMON.small_solar_panel_peak_production.get());
    EdBreaker.BreakerTileEntity.on_config(COMMON.block_breaker_power_consumption.get(), COMMON.block_breaker_reluctance.get(), COMMON.block_breaker_min_breaking_time.get(), COMMON.block_breaker_requires_power.get());
    EdTreeCutter.TreeCutterTileEntity.on_config(COMMON.tree_cuttter_energy_consumption.get(), COMMON.tree_cuttter_cutting_time_needed.get(), COMMON.tree_cuttter_requires_power.get());
    EdMilker.MilkerTileEntity.on_config(COMMON.milking_machine_energy_consumption.get(), COMMON.milking_machine_milking_delay.get());
    EdSlabBlock.on_config(!COMMON.without_direct_slab_pickup.get());
    EdSlabSliceBlock.on_config(!COMMON.without_direct_slab_pickup.get());
    EdLabeledCrate.on_config(false);
    // -----------------------------------------------------------------------------------------------------------------
    {
      // Check if the config is already synchronized or has to be synchronised.
      server_config_.putBoolean("tree_cuttter_requires_power", COMMON.tree_cuttter_requires_power.get());
      server_config_.putBoolean("block_breaker_requires_power", COMMON.block_breaker_requires_power.get());
      {
        String s = String.join(",", optouts_);
        server_config_.putString("optout", s);
        if(!s.isEmpty()) LOGGER.info("Opt-outs:" + s);
      }
    }
  }
}
