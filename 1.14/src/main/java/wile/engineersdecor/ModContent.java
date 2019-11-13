/*
 * @file ModContent.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Definition and initialisation of blocks of this
 * module, along with their tile entities if applicable.
 *
 * Note: Straight forward definition of different blocks/entities
 *       to make recipes, models and texture definitions easier.
 */
package wile.engineersdecor;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.apache.commons.lang3.ArrayUtils;
import wile.engineersdecor.blocks.*;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.RegistryEvent;
import wile.engineersdecor.detail.ModTesrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class ModContent
{
  //--------------------------------------------------------------------------------------------------------------------
  // Blocks
  //--------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor CLINKER_BRICK_BLOCK = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_block"));

  public static final BlockDecorSlab CLINKER_BRICK_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_slab"));

  public static final BlockDecorStairs CLINKER_BRICK_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stairs"));

  public static final BlockDecorWall CLINKER_BRICK_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_wall"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor CLINKER_BRICK_STAINED_BLOCK = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_block"));

  public static final BlockDecorSlab CLINKER_BRICK_STAINED_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_slab"));

  public static final BlockDecorStairs CLINKER_BRICK_STAINED_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor SLAG_BRICK_BLOCK = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_block"));

  public static final BlockDecorSlab SLAG_BRICK_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_slab"));

  public static final BlockDecorStairs SLAG_BRICK_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    SLAG_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_stairs"));

  public static final BlockDecorWall SLAG_BRICK_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_wall"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor REBAR_CONCRETE_BLOCK = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete"));

  public static final BlockDecorSlab REBAR_CONCRETE_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_slab"));

  public static final BlockDecorStairs REBAR_CONCRETE_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_stairs"));

  public static final BlockDecorWall REBAR_CONCRETE_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_wall"));

  public static final BlockDecorHalfSlab HALFSLAB_REBARCONCRETE = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_rebar_concrete"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor GAS_CONCRETE_BLOCK = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete"));

  public static final BlockDecorSlab GAS_CONCRETE_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_slab"));

  public static final BlockDecorStairs GAS_CONCRETE_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_stairs"));

  public static final BlockDecorWall GAS_CONCRETE_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_wall"));

  public static final BlockDecorHalfSlab HALFSLAB_GASCONCRETE = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_gas_concrete"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor REBAR_CONCRETE_TILE = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile"));

  public static final BlockDecorSlab REBAR_CONCRETE_TILE_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile_slab"));

  public static final BlockDecorStairs REBAR_CONCRETE_TILE_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    REBAR_CONCRETE_TILE.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorGlassBlock PANZERGLASS_BLOCK = (BlockDecorGlassBlock)(new BlockDecorGlassBlock(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(5f, 2000f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "panzerglass_block"));

  public static final BlockDecorSlab PANZERGLASS_SLAB = (BlockDecorSlab)(new BlockDecorSlab(
    BlockDecor.CFG_TRANSLUCENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(5f, 2000f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "panzerglass_slab"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorLadder METAL_RUNG_LADDER = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.0f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_ladder"));

  public static final BlockDecorLadder METAL_RUNG_STEPS = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.0f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_steps"));

  public static final BlockDecorLadder TREATED_WOOD_LADDER = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1.0f, 25f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_ladder"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecor.WaterLoggable TREATED_WOOD_TABLE = (BlockDecor.WaterLoggable)(new BlockDecor.WaterLoggable(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(1,0,1, 15,15.9,15)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_table"));

  public static final BlockDecorChair TREATED_WOOD_STOOL = (BlockDecorChair)(new BlockDecorChair(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(4.1,0,4.1, 11.8,8.8,11.8)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_stool"));

  public static final BlockDecor.WaterLoggable TREATED_WOOD_SIDE_TABLE = (BlockDecor.WaterLoggable)(new BlockDecor.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(2,0,2, 14,15.9,14)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_side_table"));

  public static final BlockDecorDirected.WaterLoggable TREATED_WOOD_WINDOWSILL = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(0.5,15,10.5, 15.5,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_windowsill"));

  public static final BlockDecorDirected.WaterLoggable TREATED_WOOD_BROAD_WINDOWSILL = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(0,14.5,4, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_broad_windowsill"));

  public static final BlockDecorDirected.WaterLoggable INSET_LIGHT_IRON = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL).lightValue(15),
    ModAuxiliaries.getPixeledAABB(5.2,5.2,15.7, 10.8,10.8,16.0)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "iron_inset_light"));

  public static final BlockDecorDirected.WaterLoggable FLOOR_EDGE_LIGHT_IRON = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL).lightValue(15),
    ModAuxiliaries.getPixeledAABB(5,0,0, 11,2,1)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "iron_floor_edge_light"));

  public static final BlockDecor.WaterLoggable STEEL_TABLE = (BlockDecor.WaterLoggable)(new BlockDecor.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_table"));

  public static final BlockDecor STEEL_FLOOR_GRATING = (BlockDecorFloorGrating)(new BlockDecorFloorGrating(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,14,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_floor_grating"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorWindow TREATED_WOOD_WINDOW = (BlockDecorWindow)(new BlockDecorWindow(
    BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.GLASS),
    ModAuxiliaries.getPixeledAABB(0,0,7, 16,16,9)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_window"));

  public static final BlockDecorWindow STEEL_FRAMED_WINDOW = (BlockDecorWindow)(new BlockDecorWindow(
    BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.GLASS),
    ModAuxiliaries.getPixeledAABB(0,0,7.5, 16,16,8.5)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_framed_window"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorStraightPole TREATED_WOOD_POLE = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole"));

  public static final BlockDecorStraightPole TREATED_WOOD_POLE_HEAD = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole_head"));

  public static final BlockDecorStraightPole TREATED_WOOD_POLE_SUPPORT = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole_support"));

  public static final BlockDecorStraightPole THIN_STEEL_POLE = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(6,6,0, 10,10,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thin_steel_pole"));

  public static final BlockDecorStraightPole THIN_STEEL_POLE_HEAD = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(6,6,0, 10,10,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thin_steel_pole_head"));

  public static final BlockDecorStraightPole THICK_STEEL_POLE = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(5,5,0, 11,11,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thick_steel_pole"));

  public static final BlockDecorStraightPole THICK_STEEL_POLE_HEAD = (BlockDecorStraightPole)(new BlockDecorStraightPole(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(5,5,0, 11,11,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thick_steel_pole_head"));

  public static final BlockDecorHorizontalSupport STEEL_DOUBLE_T_SUPPORT = (BlockDecorHorizontalSupport)(new BlockDecorHorizontalSupport(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(5,11,0, 11,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_double_t_support"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorDirected.WaterLoggable SIGN_MODLOGO = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1000f).sound(SoundType.WOOD).lightValue(1),
    ModAuxiliaries.getPixeledAABB(0,0,15.6, 16,16,16.0)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_decor"));

  public static final BlockDecorDirected.WaterLoggable SIGN_HOTWIRE = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_hotwire"));

  public static final BlockDecorDirected.WaterLoggable SIGN_DANGER = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_danger"));

  public static final BlockDecorDirected.WaterLoggable SIGN_DEFENSE = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_defense"));

  public static final BlockDecorDirected.WaterLoggable SIGN_FACTORY_AREA = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_factoryarea"));

  public static final BlockDecorDirected.WaterLoggable SIGN_EXIT = (BlockDecorDirected.WaterLoggable)(new BlockDecorDirected.WaterLoggable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(3,7,15.6, 13,13,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_exit"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorCraftingTable TREATED_WOOD_CRAFTING_TABLE = (BlockDecorCraftingTable)(new BlockDecorCraftingTable(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(1,0,1, 15,15.9,15)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_crafting_table"));

  public static final BlockDecorFurnace SMALL_LAB_FURNACE = (BlockDecorFurnace)(new BlockDecorFurnace(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(1,0,1, 15,15,16.0)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_lab_furnace"));

  public static final BlockDecorFurnaceElectrical SMALL_ELECTRICAL_FURNACE = (BlockDecorFurnaceElectrical)(new BlockDecorFurnaceElectrical(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_ELECTRICAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_electrical_furnace"));

  public static final BlockDecorDropper FACTORY_DROPPER = (BlockDecorDropper)(new BlockDecorDropper(
    BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_REDSTONE_CONTROLLED,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,15)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_dropper"));

  public static final BlockDecorPlacer FACTORY_PLACER = (BlockDecorPlacer)(new BlockDecorPlacer(
    BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_REDSTONE_CONTROLLED,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(2,2,2, 14,14,14)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_placer"));

  public static final BlockDecorBreaker SMALL_BLOCK_BREAKER = (BlockDecorBreaker)(new BlockDecorBreaker(
    BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,12,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_block_breaker"));

  public static final BlockDecorHopper FACTORY_HOPPER = (BlockDecorHopper)(new BlockDecorHopper(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_REDSTONE_CONTROLLED,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_hopper"));

  public static final BlockDecorWasteIncinerator SMALL_WASTE_INCINERATOR = (BlockDecorWasteIncinerator)(new BlockDecorWasteIncinerator(
    BlockDecor.CFG_DEFAULT|BlockDecor.CFG_ELECTRICAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_waste_incinerator"));

  public static final BlockDecorMineralSmelter SMALL_MINERAL_SMELTER = (BlockDecorMineralSmelter)(new BlockDecorMineralSmelter(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_ELECTRICAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_mineral_smelter"));

  public static final BlockDecorSolarPanel SMALL_SOLAR_PANEL = (BlockDecorSolarPanel)(new BlockDecorSolarPanel(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_ELECTRICAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,11.5,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_solar_panel"));

  public static final BlockDecorTreeCutter SMALL_TREE_CUTTER = (BlockDecorTreeCutter)(new BlockDecorTreeCutter(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,8,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_tree_cutter"));

  public static final BlockDecorPipeValve STRAIGHT_CHECK_VALVE = (BlockDecorPipeValve)(new BlockDecorPipeValve(
    BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK|BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(4,4,0, 12,12,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve"));

  public static final BlockDecorPipeValve STRAIGHT_REDSTONE_VALVE = (BlockDecorPipeValve)(new BlockDecorPipeValve(
    BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK|BlockDecor.CFG_CUTOUT|BlockDecor.CFG_REDSTONE_CONTROLLED,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(4,4,0, 12,12,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve_redstone"));

  public static final BlockDecorPipeValve STRAIGHT_REDSTONE_ANALOG_VALVE = (BlockDecorPipeValve)(new BlockDecorPipeValve(
    BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK|BlockDecor.CFG_CUTOUT|BlockDecor.CFG_REDSTONE_CONTROLLED|BlockDecor.CFG_ANALOG,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(4,4,0, 12,12,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve_redstone_analog"));

  public static final BlockDecorPassiveFluidAccumulator PASSIVE_FLUID_ACCUMULATOR = (BlockDecorPassiveFluidAccumulator)(new BlockDecorPassiveFluidAccumulator(
    BlockDecor.CFG_FACING_PLACEMENT|BlockDecor.CFG_OPPOSITE_PLACEMENT|BlockDecor.CFG_FLIP_PLACEMENT_SHIFTCLICK|BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "passive_fluid_accumulator"));

  public static final BlockDecorFluidFunnel SMALL_FLUID_FUNNEL = (BlockDecorFluidFunnel)(new BlockDecorFluidFunnel(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_REDSTONE_CONTROLLED,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    ModAuxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_fluid_funnel"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorWall CONCRETE_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "concrete_wall"));

  public static final BlockDecorHalfSlab HALFSLAB_CONCRETE = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_concrete"));

  public static final BlockDecorHalfSlab HALFSLAB_TREATEDWOOD = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 4f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_treated_wood"));

  public static final BlockDecorHalfSlab HALFSLAB_SHEETMETALIRON = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_iron"));

  public static final BlockDecorHalfSlab HALFSLAB_SHEETMETALSTEEL = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_steel"));

  public static final BlockDecorHalfSlab HALFSLAB_SHEETMETALCOPPER = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_copper"));

  public static final BlockDecorHalfSlab HALFSLAB_SHEETMETALGOLD = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_gold"));

  public static final BlockDecorHalfSlab HALFSLAB_SHEETMETALALUMINIUM = (BlockDecorHalfSlab)(new BlockDecorHalfSlab(
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_aluminum"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final BlockDecorFence STEEL_MESH_FENCE = (BlockDecorFence)(new BlockDecorFence(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_mesh_fence"));

  // -------------------------------------------------------------------------------------------------------------------

  private static final Block modBlocks[] = {
    TREATED_WOOD_CRAFTING_TABLE,
    SMALL_LAB_FURNACE,
    SMALL_ELECTRICAL_FURNACE,
    FACTORY_HOPPER,
    FACTORY_DROPPER,
    FACTORY_PLACER,
    SMALL_BLOCK_BREAKER,
    SMALL_TREE_CUTTER,
    SMALL_SOLAR_PANEL,
    SMALL_WASTE_INCINERATOR,
    SMALL_MINERAL_SMELTER,
    STRAIGHT_CHECK_VALVE,
    STRAIGHT_REDSTONE_VALVE,
    STRAIGHT_REDSTONE_ANALOG_VALVE,
    PASSIVE_FLUID_ACCUMULATOR,
    SMALL_FLUID_FUNNEL,
    CLINKER_BRICK_BLOCK,
    CLINKER_BRICK_SLAB,
    CLINKER_BRICK_STAIRS,
    CLINKER_BRICK_WALL,
    CLINKER_BRICK_STAINED_BLOCK,
    CLINKER_BRICK_STAINED_SLAB,
    CLINKER_BRICK_STAINED_STAIRS,
    SLAG_BRICK_BLOCK,
    SLAG_BRICK_SLAB,
    SLAG_BRICK_STAIRS,
    SLAG_BRICK_WALL,
    REBAR_CONCRETE_BLOCK,
    REBAR_CONCRETE_SLAB,
    REBAR_CONCRETE_STAIRS,
    REBAR_CONCRETE_WALL,
    REBAR_CONCRETE_TILE,
    REBAR_CONCRETE_TILE_SLAB,
    REBAR_CONCRETE_TILE_STAIRS,
    GAS_CONCRETE_BLOCK,
    GAS_CONCRETE_SLAB,
    GAS_CONCRETE_STAIRS,
    GAS_CONCRETE_WALL,
    HALFSLAB_REBARCONCRETE,
    HALFSLAB_GASCONCRETE,
    HALFSLAB_CONCRETE,
    //HALFSLAB_TREATEDWOOD,
    //HALFSLAB_SHEETMETALIRON
    //HALFSLAB_SHEETMETALSTEEL,
    //HALFSLAB_SHEETMETALCOPPER,
    //HALFSLAB_SHEETMETALGOLD,
    //HALFSLAB_SHEETMETALALUMINIUM,
    CONCRETE_WALL,
    PANZERGLASS_BLOCK,
    PANZERGLASS_SLAB,
    METAL_RUNG_LADDER,
    METAL_RUNG_STEPS,
    TREATED_WOOD_LADDER,
    TREATED_WOOD_TABLE,
    TREATED_WOOD_STOOL,
    TREATED_WOOD_SIDE_TABLE,
    TREATED_WOOD_WINDOWSILL,
    TREATED_WOOD_BROAD_WINDOWSILL,
    TREATED_WOOD_WINDOW,
    STEEL_FRAMED_WINDOW,
    STEEL_TABLE,
    INSET_LIGHT_IRON,
    FLOOR_EDGE_LIGHT_IRON,
    STEEL_FLOOR_GRATING,
    STEEL_MESH_FENCE,
    TREATED_WOOD_POLE,
    TREATED_WOOD_POLE_HEAD,
    TREATED_WOOD_POLE_SUPPORT,
    THIN_STEEL_POLE,
    THIN_STEEL_POLE_HEAD,
    THICK_STEEL_POLE,
    THICK_STEEL_POLE_HEAD,
    STEEL_DOUBLE_T_SUPPORT,
    SIGN_HOTWIRE,
    SIGN_DANGER,
    SIGN_DEFENSE,
    SIGN_FACTORY_AREA,
    SIGN_EXIT,
    SIGN_MODLOGO,
  };

  private static final Block devBlocks[] = {
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final TileEntityType<?> TET_TREATED_WOOD_CRAFTING_TABLE = TileEntityType.Builder
    .create(BlockDecorCraftingTable.BTileEntity::new, TREATED_WOOD_CRAFTING_TABLE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_treated_wood_crafting_table");

  public static final TileEntityType<?> TET_SMALL_LAB_FURNACE = TileEntityType.Builder
    .create(BlockDecorFurnace.BTileEntity::new, SMALL_LAB_FURNACE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_lab_furnace");

  public static final TileEntityType<?> TET_SMALL_ELECTRICAL_FURNACE = TileEntityType.Builder
    .create(BlockDecorFurnaceElectrical.BTileEntity::new, SMALL_ELECTRICAL_FURNACE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_electrical_furnace");

  public static final TileEntityType<?> TET_FACTORY_DROPPER = TileEntityType.Builder
    .create(BlockDecorDropper.BTileEntity::new, FACTORY_DROPPER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_dropper");

  public static final TileEntityType<?> TET_FACTORY_PLACER = TileEntityType.Builder
    .create(BlockDecorPlacer.BTileEntity::new, FACTORY_PLACER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_placer");

  public static final TileEntityType<?> TET_SMALL_BLOCK_BREAKER = TileEntityType.Builder
    .create(BlockDecorBreaker.BTileEntity::new, SMALL_BLOCK_BREAKER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_block_breaker");

  public static final TileEntityType<?> TET_FACTORY_HOPPER = TileEntityType.Builder
    .create(BlockDecorHopper.BTileEntity::new, FACTORY_HOPPER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_hopper");

  public static final TileEntityType<?> TET_WASTE_INCINERATOR = TileEntityType.Builder
    .create(BlockDecorWasteIncinerator.BTileEntity::new, SMALL_WASTE_INCINERATOR)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_waste_incinerator");

  public static final TileEntityType<?> TET_STRAIGHT_PIPE_VALVE = TileEntityType.Builder
    .create(BlockDecorPipeValve.BTileEntity::new, STRAIGHT_CHECK_VALVE, STRAIGHT_REDSTONE_VALVE, STRAIGHT_REDSTONE_ANALOG_VALVE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_pipe_valve");

  public static final TileEntityType<?> TET_PASSIVE_FLUID_ACCUMULATOR = TileEntityType.Builder
    .create(BlockDecorPassiveFluidAccumulator.BTileEntity::new, PASSIVE_FLUID_ACCUMULATOR)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_passive_fluid_accumulator");

  public static final TileEntityType<?> TET_SMALL_FLUID_FUNNEL = TileEntityType.Builder
    .create(BlockDecorFluidFunnel.BTileEntity::new, SMALL_FLUID_FUNNEL)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_fluid_funnel");

  public static final TileEntityType<?> TET_MINERAL_SMELTER = TileEntityType.Builder
    .create(BlockDecorMineralSmelter.BTileEntity::new, SMALL_MINERAL_SMELTER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_mineral_smelter");

  public static final TileEntityType<?> TET_SMALL_SOLAR_PANEL = TileEntityType.Builder
    .create(BlockDecorSolarPanel.BTileEntity::new, SMALL_SOLAR_PANEL)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_solar_panel");

  public static final TileEntityType<?> TET_SMALL_TREE_CUTTER = TileEntityType.Builder
    .create(BlockDecorTreeCutter.BTileEntity::new, SMALL_TREE_CUTTER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_tree_cutter");


  private static final TileEntityType<?> tile_entity_types[] = {
    TET_TREATED_WOOD_CRAFTING_TABLE,
    TET_SMALL_LAB_FURNACE,
    TET_SMALL_ELECTRICAL_FURNACE,
    TET_FACTORY_HOPPER,
    TET_FACTORY_DROPPER,
    TET_FACTORY_PLACER,
    TET_SMALL_BLOCK_BREAKER,
    TET_SMALL_TREE_CUTTER,
    TET_WASTE_INCINERATOR,
    TET_MINERAL_SMELTER,
    TET_SMALL_SOLAR_PANEL,
    TET_STRAIGHT_PIPE_VALVE,
    TET_PASSIVE_FLUID_ACCUMULATOR,
    TET_SMALL_FLUID_FUNNEL,
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final EntityType<? extends Entity> ET_CHAIR = EntityType.Builder
      .create(BlockDecorChair.EntityChair::new, EntityClassification.MISC)
      .immuneToFire().size(1e-3f, 1e-3f).disableSerialization()
      .setShouldReceiveVelocityUpdates(false).setUpdateInterval(4)
      .setCustomClientFactory(BlockDecorChair.EntityChair::customClientFactory)
      .build(new ResourceLocation(ModEngineersDecor.MODID, "et_chair").toString())
      .setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "et_chair"))
      ;

  private static final EntityType<?> entity_types[] = {
    ET_CHAIR
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Container registration
  //--------------------------------------------------------------------------------------------------------------------

  public static final ContainerType<BlockDecorCraftingTable.BContainer> CT_TREATED_WOOD_CRAFTING_TABLE;
  public static final ContainerType<BlockDecorDropper.BContainer> CT_FACTORY_DROPPER;
  public static final ContainerType<BlockDecorPlacer.BContainer> CT_FACTORY_PLACER;
  public static final ContainerType<BlockDecorHopper.BContainer> CT_FACTORY_HOPPER;
  public static final ContainerType<BlockDecorFurnace.BContainer> CT_SMALL_LAB_FURNACE;
  public static final ContainerType<BlockDecorFurnaceElectrical.BContainer> CT_SMALL_ELECTRICAL_FURNACE;
  public static final ContainerType<BlockDecorWasteIncinerator.BContainer> CT_WASTE_INCINERATOR;

  static {
    CT_TREATED_WOOD_CRAFTING_TABLE = (new ContainerType<BlockDecorCraftingTable.BContainer>(BlockDecorCraftingTable.BContainer::new));
    CT_TREATED_WOOD_CRAFTING_TABLE.setRegistryName(ModEngineersDecor.MODID,"ct_treated_wood_crafting_table");
    CT_FACTORY_DROPPER = (new ContainerType<BlockDecorDropper.BContainer>(BlockDecorDropper.BContainer::new));
    CT_FACTORY_DROPPER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_dropper");
    CT_FACTORY_PLACER = (new ContainerType<BlockDecorPlacer.BContainer>(BlockDecorPlacer.BContainer::new));
    CT_FACTORY_PLACER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_placer");
    CT_FACTORY_HOPPER = (new ContainerType<BlockDecorHopper.BContainer>(BlockDecorHopper.BContainer::new));
    CT_FACTORY_HOPPER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_hopper");
    CT_SMALL_LAB_FURNACE = (new ContainerType<BlockDecorFurnace.BContainer>(BlockDecorFurnace.BContainer::new));
    CT_SMALL_LAB_FURNACE.setRegistryName(ModEngineersDecor.MODID,"ct_small_lab_furnace");
    CT_SMALL_ELECTRICAL_FURNACE = (new ContainerType<BlockDecorFurnaceElectrical.BContainer>(BlockDecorFurnaceElectrical.BContainer::new));
    CT_SMALL_ELECTRICAL_FURNACE.setRegistryName(ModEngineersDecor.MODID,"ct_small_electrical_furnace");
    CT_WASTE_INCINERATOR = (new ContainerType<BlockDecorWasteIncinerator.BContainer>(BlockDecorWasteIncinerator.BContainer::new));
    CT_WASTE_INCINERATOR.setRegistryName(ModEngineersDecor.MODID,"ct_small_waste_incinerator");
  }

  // DON'T FORGET TO REGISTER THE GUI in registerContainerGuis(), no list/map format found yet for that.
  private static final ContainerType<?> container_types[] = {
    CT_TREATED_WOOD_CRAFTING_TABLE,
    CT_FACTORY_DROPPER,
    CT_FACTORY_PLACER,
    CT_FACTORY_HOPPER,
    CT_SMALL_LAB_FURNACE,
    CT_SMALL_ELECTRICAL_FURNACE,
    CT_WASTE_INCINERATOR
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Initialisation events
  //--------------------------------------------------------------------------------------------------------------------

  private static ArrayList<Block> registeredBlocks = new ArrayList<>();

  public static ArrayList<Block> allBlocks()
  {
    ArrayList<Block> blocks = new ArrayList<>();
    Collections.addAll(blocks, modBlocks);
    Collections.addAll(blocks, devBlocks);
    return blocks;
  }

  public static boolean isExperimentalBlock(Block block)
  { return ArrayUtils.contains(devBlocks, block); }

  @Nonnull
  public static List<Block> getRegisteredBlocks()
  { return Collections.unmodifiableList(registeredBlocks); }

  public static final void registerBlocks(final RegistryEvent.Register<Block> event)
  {
    if(ModAuxiliaries.isModLoaded("immersiveengineering")) ModAuxiliaries.logInfo("Immersive Engineering also installed ...");
    registeredBlocks.addAll(allBlocks());
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    ModAuxiliaries.logInfo("Registered " + Integer.toString(registeredBlocks.size()) + " blocks.");
  }

  public static final void registerBlockItems(final RegistryEvent.Register<Item> event)
  {
    int n = 0;
    for(Block e:registeredBlocks) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      event.getRegistry().register(new BlockItem(e, (new BlockItem.Properties().group(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      ++n;
    }
  }

  public static final void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event)
  {
    int n_registered = 0;
    for(final TileEntityType<?> e:tile_entity_types) {
      event.getRegistry().register(e);
      ++n_registered;
    }
    ModAuxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " tile entities.");
  }

  public static final void registerEntities(final RegistryEvent.Register<EntityType<?>> event)
  {
    int n_registered = 0;
    for(final EntityType<?> e:entity_types) {
      if((e==ET_CHAIR) && (!registeredBlocks.contains(TREATED_WOOD_STOOL))) continue;
      event.getRegistry().register(e);
      ++n_registered;
    }
    ModAuxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " entities bound to blocks.");
  }

  public static final void registerContainers(final RegistryEvent.Register<ContainerType<?>> event)
  {
    int n_registered = 0;
    for(final ContainerType<?> e:container_types) {
      event.getRegistry().register(e);
      ++n_registered;
    }
    ModAuxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " containers bound to tile entities.");
  }

  @OnlyIn(Dist.CLIENT)
  public static final void registerContainerGuis(final FMLClientSetupEvent event)
  {
    ScreenManager.registerFactory(CT_TREATED_WOOD_CRAFTING_TABLE, BlockDecorCraftingTable.BGui::new);
    ScreenManager.registerFactory(CT_FACTORY_DROPPER, BlockDecorDropper.BGui::new);
    ScreenManager.registerFactory(CT_FACTORY_PLACER, BlockDecorPlacer.BGui::new);
    ScreenManager.registerFactory(CT_FACTORY_HOPPER, BlockDecorHopper.BGui::new);
    ScreenManager.registerFactory(CT_SMALL_LAB_FURNACE, BlockDecorFurnace.BGui::new);
    ScreenManager.registerFactory(CT_SMALL_ELECTRICAL_FURNACE, BlockDecorFurnaceElectrical.BGui::new);
    ScreenManager.registerFactory(CT_WASTE_INCINERATOR, BlockDecorWasteIncinerator.BGui::new);
  }

  @OnlyIn(Dist.CLIENT)
  public static final void registerTileEntityRenderers(final FMLClientSetupEvent event)
  {
    ClientRegistry.bindTileEntitySpecialRenderer(BlockDecorCraftingTable.BTileEntity.class, new ModTesrs.TesrDecorCraftingTable());
  }
}
