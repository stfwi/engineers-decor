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


import wile.engineersdecor.blocks.*;
import wile.engineersdecor.libmc.blocks.StandardBlocks.BaseBlock;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
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
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class ModContent
{
  //--------------------------------------------------------------------------------------------------------------------
  // Blocks
  //--------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal CLINKER_BRICK_BLOCK = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_block"));

  public static final EdSlabBlock CLINKER_BRICK_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_slab"));

  public static final EdStairsBlock CLINKER_BRICK_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stairs"));

  public static final EdWallBlock CLINKER_BRICK_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_wall"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal CLINKER_BRICK_STAINED_BLOCK = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_block"));

  public static final EdSlabBlock CLINKER_BRICK_STAINED_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_slab"));

  public static final EdStairsBlock CLINKER_BRICK_STAINED_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stained_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal SLAG_BRICK_BLOCK = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_block"));

  public static final EdSlabBlock SLAG_BRICK_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_slab"));

  public static final EdStairsBlock SLAG_BRICK_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    SLAG_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_stairs"));

  public static final EdWallBlock SLAG_BRICK_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(3f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_wall"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal REBAR_CONCRETE_BLOCK = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete"));

  public static final EdSlabBlock REBAR_CONCRETE_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_slab"));

  public static final EdStairsBlock REBAR_CONCRETE_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_stairs"));

  public static final EdWallBlock REBAR_CONCRETE_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_wall"));

  public static final EdSlabSliceBlock HALFSLAB_REBARCONCRETE = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_rebar_concrete"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal GAS_CONCRETE_BLOCK = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete"));

  public static final EdSlabBlock GAS_CONCRETE_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_slab"));

  public static final EdStairsBlock GAS_CONCRETE_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_stairs"));

  public static final EdWallBlock GAS_CONCRETE_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "gas_concrete_wall"));

  public static final EdSlabSliceBlock HALFSLAB_GASCONCRETE = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1.5f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_gas_concrete"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal REBAR_CONCRETE_TILE = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile"));

  public static final EdSlabBlock REBAR_CONCRETE_TILE_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile_slab"));

  public static final EdStairsBlock REBAR_CONCRETE_TILE_STAIRS = (EdStairsBlock)(new EdStairsBlock(
    DecorBlock.CFG_DEFAULT,
    REBAR_CONCRETE_TILE.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(5f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_tile_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdGlassBlock PANZERGLASS_BLOCK = (EdGlassBlock)(new EdGlassBlock(
    DecorBlock.CFG_TRANSLUCENT,
    Block.Properties.create(Material.GLASS, MaterialColor.AIR).hardnessAndResistance(5f, 2000f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "panzerglass_block"));

  public static final EdSlabBlock PANZERGLASS_SLAB = (EdSlabBlock)(new EdSlabBlock(
    DecorBlock.CFG_TRANSLUCENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(5f, 2000f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "panzerglass_slab"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.Normal DENSE_GRIT_SAND = (DecorBlock.Normal)(new DecorBlock.Normal(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.EARTH, MaterialColor.DIRT).hardnessAndResistance(0.5f, 3f).sound(SoundType.GROUND).harvestTool(ToolType.SHOVEL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "dense_grit_sand_block"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdLadderBlock METAL_RUNG_LADDER = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.0f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_ladder"));

  public static final EdLadderBlock METAL_RUNG_STEPS = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.0f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_steps"));

  public static final EdLadderBlock TREATED_WOOD_LADDER = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1.0f, 25f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_ladder"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.WaterLoggable TREATED_WOOD_TABLE = (DecorBlock.WaterLoggable)(new DecorBlock.WaterLoggable(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(1,0,1, 15,15.9,15)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_table"));

  public static final EdChair.ChairBlock TREATED_WOOD_STOOL = (EdChair.ChairBlock)(new EdChair.ChairBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(4,7,4, 12,8.8,12),
      Auxiliaries.getPixeledAABB(7,0,7, 9,7,9),
      Auxiliaries.getPixeledAABB(4,0,7, 12,1,9),
      Auxiliaries.getPixeledAABB(7,0,4, 9,1,12),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_stool"));

  public static final DecorBlock.WaterLoggable TREATED_WOOD_SIDE_TABLE = (DecorBlock.WaterLoggable)(new DecorBlock.WaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(2,0,2, 14,15.9,14)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_side_table"));

  public static final DecorBlock.DirectedWaterLoggable TREATED_WOOD_WINDOWSILL = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(0.5,15,10.5, 15.5,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_windowsill"));

  public static final DecorBlock.DirectedWaterLoggable TREATED_WOOD_BROAD_WINDOWSILL = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(0,14.5,4, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_broad_windowsill"));

  public static final DecorBlock.DirectedWaterLoggable INSET_LIGHT_IRON = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL).lightValue(15),
    Auxiliaries.getPixeledAABB(5.2,5.2,0, 10.8,10.8,0.3)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "iron_inset_light"));

  public static final DecorBlock.DirectedWaterLoggable FLOOR_EDGE_LIGHT_IRON = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL).lightValue(15),
    Auxiliaries.getPixeledAABB(5,0,0, 11,2,0.5)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "iron_floor_edge_light"));

  public static final DecorBlock.WaterLoggable STEEL_TABLE = (DecorBlock.WaterLoggable)(new DecorBlock.WaterLoggable(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_table"));

  public static final EdFloorGratingBlock STEEL_FLOOR_GRATING = (EdFloorGratingBlock)(new EdFloorGratingBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,14,0, 16,15.9,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_floor_grating"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdWindowBlock TREATED_WOOD_WINDOW = (EdWindowBlock)(new EdWindowBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.GLASS),
    Auxiliaries.getPixeledAABB(0,0,7, 16,16,9)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_window"));

  public static final EdWindowBlock STEEL_FRAMED_WINDOW = (EdWindowBlock)(new EdWindowBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.GLASS),
    Auxiliaries.getPixeledAABB(0,0,7.5, 16,16,8.5)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_framed_window"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdStraightPoleBlock TREATED_WOOD_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole"));

  public static final EdStraightPoleBlock TREATED_WOOD_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole_head"));

  public static final EdStraightPoleBlock TREATED_WOOD_POLE_SUPPORT = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2f, 15f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole_support"));

  public static final EdStraightPoleBlock THIN_STEEL_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(6,6,0, 10,10,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thin_steel_pole"));

  public static final EdStraightPoleBlock THIN_STEEL_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(6,6,0, 10,10,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thin_steel_pole_head"));

  public static final EdStraightPoleBlock THICK_STEEL_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(5,5,0, 11,11,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thick_steel_pole"));

  public static final EdStraightPoleBlock THICK_STEEL_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(5,5,0, 11,11,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "thick_steel_pole_head"));

  public static final EdHorizontalSupportBlock STEEL_DOUBLE_T_SUPPORT = (EdHorizontalSupportBlock)(new EdHorizontalSupportBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(5,11,0, 11,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_double_t_support"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final DecorBlock.DirectedWaterLoggable SIGN_MODLOGO = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1000f).sound(SoundType.WOOD).lightValue(1),
    Auxiliaries.getPixeledAABB(0,0,15.6, 16,16,16.0)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_decor"));

  public static final DecorBlock.DirectedWaterLoggable SIGN_HOTWIRE = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_hotwire"));

  public static final DecorBlock.DirectedWaterLoggable SIGN_DANGER = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_danger"));

  public static final DecorBlock.DirectedWaterLoggable SIGN_DEFENSE = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_defense"));

  public static final DecorBlock.DirectedWaterLoggable SIGN_FACTORY_AREA = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_factoryarea"));

  public static final DecorBlock.DirectedWaterLoggable SIGN_EXIT = (DecorBlock.DirectedWaterLoggable)(new DecorBlock.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 1f).sound(SoundType.WOOD),
    Auxiliaries.getPixeledAABB(3,7,15.6, 13,13,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "sign_exit"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdCraftingTable.CraftingTableBlock TREATED_WOOD_CRAFTING_TABLE = (EdCraftingTable.CraftingTableBlock)(new EdCraftingTable.CraftingTableBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 15f).sound(SoundType.WOOD),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(0,13,0, 16,16,16),
      Auxiliaries.getPixeledAABB(1, 0,1, 15,13,15)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_crafting_table"));

  public static final EdFurnace.FurnaceBlock SMALL_LAB_FURNACE = (EdFurnace.FurnaceBlock)(new EdFurnace.FurnaceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(1,0,1, 15, 1,15),
      Auxiliaries.getPixeledAABB(0,1,1, 16,16,16),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_lab_furnace"));

  public static final EdElectricalFurnace.ElectricalFurnaceBlock SMALL_ELECTRICAL_FURNACE = (EdElectricalFurnace.ElectricalFurnaceBlock)(new EdElectricalFurnace.ElectricalFurnaceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(0, 0,0, 16,11,16),
      Auxiliaries.getPixeledAABB(1,11,0, 15,12,16),
      Auxiliaries.getPixeledAABB(2,12,0, 14,13,16),
      Auxiliaries.getPixeledAABB(3,13,0, 13,14,16),
      Auxiliaries.getPixeledAABB(4,14,0, 12,16,16),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_electrical_furnace"));

  public static final EdDropper.DropperBlock FACTORY_DROPPER = (EdDropper.DropperBlock)(new EdDropper.DropperBlock(
    DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,1, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_dropper"));

  public static final EdPlacer.PlacerBlock FACTORY_PLACER = (EdPlacer.PlacerBlock)(new EdPlacer.PlacerBlock(
    DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(0,0,2, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,0, 1,16, 2),
      Auxiliaries.getPixeledAABB(15,0,0,16,16, 2)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_placer"));

  public static final EdBreaker.BreakerBlock SMALL_BLOCK_BREAKER = (EdBreaker.BreakerBlock)(new EdBreaker.BreakerBlock(
    DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(1,0,0, 15, 4, 7),
      Auxiliaries.getPixeledAABB(1,0,7, 15,12,16),
      Auxiliaries.getPixeledAABB(0,0,0, 1, 5, 4),
      Auxiliaries.getPixeledAABB(0,0,4, 1,12,16),
      Auxiliaries.getPixeledAABB(15,0,0, 16, 5, 4),
      Auxiliaries.getPixeledAABB(15,0,4, 16,12,16)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_block_breaker"));

  public static final EdHopper.HopperBlock FACTORY_HOPPER = (EdHopper.HopperBlock)(new EdHopper.HopperBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL), ()->{
      final AxisAlignedBB[] down_aabbs = new AxisAlignedBB[]{
        Auxiliaries.getPixeledAABB( 5, 0, 5, 11, 1,11),
        Auxiliaries.getPixeledAABB( 4, 1, 4, 12, 7,12),
        Auxiliaries.getPixeledAABB( 2, 7, 2, 14,10,14),
        Auxiliaries.getPixeledAABB( 0,10, 0, 16,16,16),
        Auxiliaries.getPixeledAABB( 0, 4, 5,  2,10,11),
        Auxiliaries.getPixeledAABB(14, 4, 5, 16,10,11),
        Auxiliaries.getPixeledAABB( 5, 4, 0, 11,10, 2),
        Auxiliaries.getPixeledAABB( 5, 4,14, 11,10,16),
      };
      final AxisAlignedBB[] up_aabbs = new AxisAlignedBB[]{
        Auxiliaries.getPixeledAABB( 5,15, 5, 11,16,11),
        Auxiliaries.getPixeledAABB( 4,14, 4, 12, 9,12),
        Auxiliaries.getPixeledAABB( 2, 9, 2, 14, 6,14),
        Auxiliaries.getPixeledAABB( 0, 6, 0, 16, 0,16),
        Auxiliaries.getPixeledAABB( 0,12, 5,  2, 6,11),
        Auxiliaries.getPixeledAABB(14,12, 5, 16, 6,11),
        Auxiliaries.getPixeledAABB( 5,12, 0, 11, 6, 2),
        Auxiliaries.getPixeledAABB( 5,12,14, 11, 6,16),
      };
      final AxisAlignedBB[] north_aabbs = new AxisAlignedBB[]{
        Auxiliaries.getPixeledAABB( 5, 0, 5, 11, 1,11),
        Auxiliaries.getPixeledAABB( 4, 1, 4, 12, 7,12),
        Auxiliaries.getPixeledAABB( 2, 7, 2, 14,10,14),
        Auxiliaries.getPixeledAABB( 0,10, 0, 16,16,16),
        Auxiliaries.getPixeledAABB( 0, 4, 5,  2,10,11),
        Auxiliaries.getPixeledAABB(14, 4, 5, 16,10,11),
        Auxiliaries.getPixeledAABB( 5, 1, 0, 11, 7, 4),
        Auxiliaries.getPixeledAABB( 5, 4,14, 11,10,16),
      };
      return new ArrayList<VoxelShape>(Arrays.asList(
        Auxiliaries.getUnionShape(down_aabbs),
        Auxiliaries.getUnionShape(up_aabbs),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.NORTH, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.SOUTH, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.WEST, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.EAST, false)),
        VoxelShapes.fullCube(),
        VoxelShapes.fullCube()
      ));
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "factory_hopper"));

  public static final EdWasteIncinerator.WasteIncineratorBlock SMALL_WASTE_INCINERATOR = (EdWasteIncinerator.WasteIncineratorBlock)(new EdWasteIncinerator.WasteIncineratorBlock(
    DecorBlock.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_waste_incinerator"));

  public static final EdMineralSmelter.MineralSmelterBlock SMALL_MINERAL_SMELTER = (EdMineralSmelter.MineralSmelterBlock)(new EdMineralSmelter.MineralSmelterBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_mineral_smelter"));

  public static final EdSolarPanel.SolarPanelBlock SMALL_SOLAR_PANEL = (EdSolarPanel.SolarPanelBlock)(new EdSolarPanel.SolarPanelBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(0,0,0, 16,2,16),
      Auxiliaries.getPixeledAABB(6,1.5,3, 10,10.5,13),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_solar_panel"));

  public static final EdMilker.MilkerBlock SMALL_MILKING_MACHINE = (EdMilker.MilkerBlock)(new EdMilker.MilkerBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB( 1, 1,0, 15,14,10),
      Auxiliaries.getPixeledAABB( 0,14,0, 16,16,13),
      Auxiliaries.getPixeledAABB( 0, 0,0, 16, 1,13),
      Auxiliaries.getPixeledAABB( 0, 1,1,  1,14,11),
      Auxiliaries.getPixeledAABB(15, 1,1, 16,14,11)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_milking_machine"));

  public static final EdTreeCutter.TreeCutterBlock SMALL_TREE_CUTTER = (EdTreeCutter.TreeCutterBlock)(new EdTreeCutter.TreeCutterBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB( 0,0, 0, 16,3,16),
      Auxiliaries.getPixeledAABB( 0,3, 0,  3,8,16),
      Auxiliaries.getPixeledAABB( 3,7, 0,  5,8,16),
      Auxiliaries.getPixeledAABB(15,0, 0, 16,6,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,8,16),
      Auxiliaries.getPixeledAABB( 5,6,12, 16,8,13),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_tree_cutter"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_CHECK_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    EdPipeValve.CFG_CHECK_VALVE,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_REDSTONE_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve_redstone"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_REDSTONE_ANALOG_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE|EdPipeValve.CFG_ANALOG_VALVE,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "straight_pipe_valve_redstone_analog"));

  public static final EdFluidAccumulator.FluidAccumulatorBlock PASSIVE_FLUID_ACCUMULATOR = (EdFluidAccumulator.FluidAccumulatorBlock)(new EdFluidAccumulator.FluidAccumulatorBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(3,3,0, 13,13, 1),
      Auxiliaries.getPixeledAABB(0,0,1, 16,16,16)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "passive_fluid_accumulator"));

  public static final EdFluidFunnel.FluidFunnelBlock SMALL_FLUID_FUNNEL = (EdFluidFunnel.FluidFunnelBlock)(new EdFluidFunnel.FluidFunnelBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    new AxisAlignedBB[]{
      Auxiliaries.getPixeledAABB(0, 0,0, 16,14,16),
      Auxiliaries.getPixeledAABB(1,14,1, 15,15,15),
      Auxiliaries.getPixeledAABB(0,15,0, 16,16,16)
    }
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "small_fluid_funnel"));

  public static final EdLabeledCrate.LabeledCrateBlock LABELED_CRATE = (EdLabeledCrate.LabeledCrateBlock)(new EdLabeledCrate.LabeledCrateBlock(
    DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(0.5f, 128f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "labeled_crate"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdWallBlock CONCRETE_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "concrete_wall"));

  public static final EdSlabSliceBlock HALFSLAB_CONCRETE = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(1f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_concrete"));

  public static final EdSlabSliceBlock HALFSLAB_TREATEDWOOD = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1f, 4f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_treated_wood"));

  public static final EdSlabSliceBlock HALFSLAB_SHEETMETALIRON = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_iron"));

  public static final EdSlabSliceBlock HALFSLAB_SHEETMETALSTEEL = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_steel"));

  public static final EdSlabSliceBlock HALFSLAB_SHEETMETALCOPPER = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_copper"));

  public static final EdSlabSliceBlock HALFSLAB_SHEETMETALGOLD = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_gold"));

  public static final EdSlabSliceBlock HALFSLAB_SHEETMETALALUMINIUM = (EdSlabSliceBlock)(new EdSlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1f, 10f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "halfslab_sheetmetal_aluminum"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdFenceBlock STEEL_MESH_FENCE = (EdFenceBlock)(new EdFenceBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    1.5, 16, 0.25, 0, 16
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_mesh_fence"));

  public static final EdDoubleGateBlock STEEL_MESH_FENCE_GATE = (EdDoubleGateBlock)(new EdDoubleGateBlock(
    DecorBlock.CFG_CUTOUT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(2f, 15f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,6.5, 16,16,9.5)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "steel_mesh_fence_gate"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdTestBlock TEST_BLOCK = (EdTestBlock)(new EdTestBlock(
    DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(0f, 32000f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "test_block"));

  // -------------------------------------------------------------------------------------------------------------------

  private static final Block modBlocks[] = {
    TREATED_WOOD_CRAFTING_TABLE,
    LABELED_CRATE,
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
    SMALL_MILKING_MACHINE,
    STRAIGHT_CHECK_VALVE,
    STRAIGHT_REDSTONE_VALVE,
    STRAIGHT_REDSTONE_ANALOG_VALVE,
    PASSIVE_FLUID_ACCUMULATOR,
    SMALL_FLUID_FUNNEL,
    DENSE_GRIT_SAND,
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
    HALFSLAB_TREATEDWOOD,
    HALFSLAB_SHEETMETALIRON,
    HALFSLAB_SHEETMETALSTEEL,
    HALFSLAB_SHEETMETALCOPPER,
    HALFSLAB_SHEETMETALGOLD,
    HALFSLAB_SHEETMETALALUMINIUM,
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
    STEEL_MESH_FENCE_GATE,
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
    //TEST_BLOCK
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final TileEntityType<?> TET_TREATED_WOOD_CRAFTING_TABLE = TileEntityType.Builder
    .create(EdCraftingTable.CraftingTableTileEntity::new, TREATED_WOOD_CRAFTING_TABLE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_treated_wood_crafting_table");

  public static final TileEntityType<?> TET_LABELED_CRATE = TileEntityType.Builder
    .create(EdLabeledCrate.LabeledCrateTileEntity::new, LABELED_CRATE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_labeled_crate");

  public static final TileEntityType<?> TET_SMALL_LAB_FURNACE = TileEntityType.Builder
    .create(EdFurnace.FurnaceTileEntity::new, SMALL_LAB_FURNACE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_lab_furnace");

  public static final TileEntityType<?> TET_SMALL_ELECTRICAL_FURNACE = TileEntityType.Builder
    .create(EdElectricalFurnace.ElectricalFurnaceTileEntity::new, SMALL_ELECTRICAL_FURNACE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_electrical_furnace");

  public static final TileEntityType<?> TET_FACTORY_DROPPER = TileEntityType.Builder
    .create(EdDropper.DropperTileEntity::new, FACTORY_DROPPER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_dropper");

  public static final TileEntityType<?> TET_FACTORY_PLACER = TileEntityType.Builder
    .create(EdPlacer.PlacerTileEntity::new, FACTORY_PLACER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_placer");

  public static final TileEntityType<?> TET_SMALL_BLOCK_BREAKER = TileEntityType.Builder
    .create(EdBreaker.BreakerTileEntity::new, SMALL_BLOCK_BREAKER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_block_breaker");

  public static final TileEntityType<?> TET_FACTORY_HOPPER = TileEntityType.Builder
    .create(EdHopper.HopperTileEntity::new, FACTORY_HOPPER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_factory_hopper");

  public static final TileEntityType<?> TET_WASTE_INCINERATOR = TileEntityType.Builder
    .create(EdWasteIncinerator.WasteIncineratorTileEntity::new, SMALL_WASTE_INCINERATOR)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_waste_incinerator");

  public static final TileEntityType<?> TET_STRAIGHT_PIPE_VALVE = TileEntityType.Builder
    .create(EdPipeValve.PipeValveTileEntity::new, STRAIGHT_CHECK_VALVE, STRAIGHT_REDSTONE_VALVE, STRAIGHT_REDSTONE_ANALOG_VALVE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_pipe_valve");

  public static final TileEntityType<?> TET_PASSIVE_FLUID_ACCUMULATOR = TileEntityType.Builder
    .create(EdFluidAccumulator.FluidAccumulatorTileEntity::new, PASSIVE_FLUID_ACCUMULATOR)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_passive_fluid_accumulator");

  public static final TileEntityType<?> TET_SMALL_FLUID_FUNNEL = TileEntityType.Builder
    .create(EdFluidFunnel.FluidFunnelTileEntity::new, SMALL_FLUID_FUNNEL)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_fluid_funnel");

  public static final TileEntityType<?> TET_MINERAL_SMELTER = TileEntityType.Builder
    .create(EdMineralSmelter.MineralSmelterTileEntity::new, SMALL_MINERAL_SMELTER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_mineral_smelter");

  public static final TileEntityType<?> TET_SMALL_SOLAR_PANEL = TileEntityType.Builder
    .create(EdSolarPanel.SolarPanelTileEntity::new, SMALL_SOLAR_PANEL)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_solar_panel");

  public static final TileEntityType<?> TET_SMALL_MILKING_MACHINE = TileEntityType.Builder
    .create(EdMilker.MilkerTileEntity::new, SMALL_MILKING_MACHINE)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_milking_machine");

  public static final TileEntityType<?> TET_SMALL_TREE_CUTTER = TileEntityType.Builder
    .create(EdTreeCutter.TreeCutterTileEntity::new, SMALL_TREE_CUTTER)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_small_tree_cutter");

  public static final TileEntityType<?> TET_TEST_BLOCK = TileEntityType.Builder
    .create(EdTestBlock.TestTileEntity::new, TEST_BLOCK)
    .build(null)
    .setRegistryName(ModEngineersDecor.MODID, "te_test_block");

  private static final TileEntityType<?> tile_entity_types[] = {
    TET_TREATED_WOOD_CRAFTING_TABLE,
    TET_LABELED_CRATE,
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
    TET_SMALL_MILKING_MACHINE,
    TET_STRAIGHT_PIPE_VALVE,
    TET_PASSIVE_FLUID_ACCUMULATOR,
    TET_SMALL_FLUID_FUNNEL,
    TET_TEST_BLOCK
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final EntityType<? extends Entity> ET_CHAIR = EntityType.Builder
      .create(EdChair.ChairEntity::new, EntityClassification.MISC)
      .immuneToFire().size(1e-3f, 1e-3f).disableSerialization()
      .setShouldReceiveVelocityUpdates(false).setUpdateInterval(4)
      .setCustomClientFactory(EdChair.ChairEntity::customClientFactory)
      .build(new ResourceLocation(ModEngineersDecor.MODID, "et_chair").toString())
      .setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "et_chair"))
      ;

  private static final EntityType<?> entity_types[] = {
    ET_CHAIR
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Container registration
  //--------------------------------------------------------------------------------------------------------------------

  public static final ContainerType<EdCraftingTable.CraftingTableContainer> CT_TREATED_WOOD_CRAFTING_TABLE;
  public static final ContainerType<EdDropper.DropperContainer> CT_FACTORY_DROPPER;
  public static final ContainerType<EdPlacer.PlacerContainer> CT_FACTORY_PLACER;
  public static final ContainerType<EdHopper.HopperContainer> CT_FACTORY_HOPPER;
  public static final ContainerType<EdFurnace.FurnaceContainer> CT_SMALL_LAB_FURNACE;
  public static final ContainerType<EdElectricalFurnace.ElectricalFurnaceContainer> CT_SMALL_ELECTRICAL_FURNACE;
  public static final ContainerType<EdWasteIncinerator.WasteIncineratorContainer> CT_WASTE_INCINERATOR;
  public static final ContainerType<EdLabeledCrate.LabeledCrateContainer> CT_LABELED_CRATE;

  static {
    CT_TREATED_WOOD_CRAFTING_TABLE = (new ContainerType<EdCraftingTable.CraftingTableContainer>(EdCraftingTable.CraftingTableContainer::new));
    CT_TREATED_WOOD_CRAFTING_TABLE.setRegistryName(ModEngineersDecor.MODID,"ct_treated_wood_crafting_table");
    CT_FACTORY_DROPPER = (new ContainerType<EdDropper.DropperContainer>(EdDropper.DropperContainer::new));
    CT_FACTORY_DROPPER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_dropper");
    CT_FACTORY_PLACER = (new ContainerType<EdPlacer.PlacerContainer>(EdPlacer.PlacerContainer::new));
    CT_FACTORY_PLACER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_placer");
    CT_FACTORY_HOPPER = (new ContainerType<EdHopper.HopperContainer>(EdHopper.HopperContainer::new));
    CT_FACTORY_HOPPER.setRegistryName(ModEngineersDecor.MODID,"ct_factory_hopper");
    CT_SMALL_LAB_FURNACE = (new ContainerType<EdFurnace.FurnaceContainer>(EdFurnace.FurnaceContainer::new));
    CT_SMALL_LAB_FURNACE.setRegistryName(ModEngineersDecor.MODID,"ct_small_lab_furnace");
    CT_SMALL_ELECTRICAL_FURNACE = (new ContainerType<EdElectricalFurnace.ElectricalFurnaceContainer>(EdElectricalFurnace.ElectricalFurnaceContainer::new));
    CT_SMALL_ELECTRICAL_FURNACE.setRegistryName(ModEngineersDecor.MODID,"ct_small_electrical_furnace");
    CT_WASTE_INCINERATOR = (new ContainerType<EdWasteIncinerator.WasteIncineratorContainer>(EdWasteIncinerator.WasteIncineratorContainer::new));
    CT_WASTE_INCINERATOR.setRegistryName(ModEngineersDecor.MODID,"ct_small_waste_incinerator");
    CT_LABELED_CRATE = (new ContainerType<EdLabeledCrate.LabeledCrateContainer>(EdLabeledCrate.LabeledCrateContainer::new));
    CT_LABELED_CRATE.setRegistryName(ModEngineersDecor.MODID,"ct_labeled_crate");
  }

  // DON'T FORGET TO REGISTER THE GUI in registerContainerGuis(), no list/map format found yet for that.
  private static final ContainerType<?> container_types[] = {
    CT_TREATED_WOOD_CRAFTING_TABLE,
    CT_LABELED_CRATE,
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

  @Nonnull
  public static List<Item> getRegisteredItems()
  { return new ArrayList<>(); }

  public static final void registerBlocks(final RegistryEvent.Register<Block> event)
  {
    boolean ie_available = Auxiliaries.isModLoaded("immersiveengineering");
    if(ie_available) {
      Auxiliaries.logInfo("Immersive Engineering also installed ...");
      registeredBlocks.addAll(allBlocks());
    } else {
      registeredBlocks.addAll(allBlocks().stream()
        .filter(block->
          ((!(block instanceof BaseBlock)) || ((((BaseBlock)block).config & DecorBlock.CFG_HARD_IE_DEPENDENT)==0))
          || (block == HALFSLAB_CONCRETE) // special case for 1.14 to prevent mod update issues
        )
        .collect(Collectors.toList())
      );
    }
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    Auxiliaries.logInfo("Registered " + Integer.toString(registeredBlocks.size()) + " blocks.");
  }

  public static final void registerBlockItems(final RegistryEvent.Register<Item> event)
  {
    int n = 0;
    for(Block e:registeredBlocks) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      if(e instanceof StandardBlocks.IBlockItemFactory) {
        event.getRegistry().register(((StandardBlocks.IBlockItemFactory)e).getBlockItem(e, (new BlockItem.Properties().group(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      } else {
        event.getRegistry().register(new BlockItem(e, (new BlockItem.Properties().group(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      }
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
    Auxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " tile entities.");
  }

  public static final void registerEntities(final RegistryEvent.Register<EntityType<?>> event)
  {
    int n_registered = 0;
    for(final EntityType<?> e:entity_types) {
      if((e==ET_CHAIR) && (!registeredBlocks.contains(TREATED_WOOD_STOOL))) continue;
      event.getRegistry().register(e);
      ++n_registered;
    }
    Auxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " entities bound to blocks.");
  }

  public static final void registerContainers(final RegistryEvent.Register<ContainerType<?>> event)
  {
    int n_registered = 0;
    for(final ContainerType<?> e:container_types) {
      event.getRegistry().register(e);
      ++n_registered;
    }
    Auxiliaries.logInfo("Registered " + Integer.toString(n_registered) + " containers bound to tile entities.");
  }

  @OnlyIn(Dist.CLIENT)
  public static final void registerContainerGuis(final FMLClientSetupEvent event)
  {
    ScreenManager.registerFactory(CT_TREATED_WOOD_CRAFTING_TABLE, EdCraftingTable.CraftingTableGui::new);
    ScreenManager.registerFactory(CT_LABELED_CRATE, EdLabeledCrate.LabeledCrateGui::new);
    ScreenManager.registerFactory(CT_FACTORY_DROPPER, EdDropper.DropperGui::new);
    ScreenManager.registerFactory(CT_FACTORY_PLACER, EdPlacer.PlacerGui::new);
    ScreenManager.registerFactory(CT_FACTORY_HOPPER, EdHopper.HopperGui::new);
    ScreenManager.registerFactory(CT_SMALL_LAB_FURNACE, EdFurnace.FurnaceGui::new);
    ScreenManager.registerFactory(CT_SMALL_ELECTRICAL_FURNACE, EdElectricalFurnace.ElectricalFurnaceGui::new);
    ScreenManager.registerFactory(CT_WASTE_INCINERATOR, EdWasteIncinerator.WasteIncineratorGui::new);
  }

  @OnlyIn(Dist.CLIENT)
  public static final void registerTileEntityRenderers(final FMLClientSetupEvent event)
  {
    ClientRegistry.bindTileEntitySpecialRenderer(EdCraftingTable.CraftingTableTileEntity.class, new wile.engineersdecor.detail.ModTesrs.TesrDecorCraftingTable());
    ClientRegistry.bindTileEntitySpecialRenderer(EdLabeledCrate.LabeledCrateTileEntity.class, new wile.engineersdecor.detail.ModTesrs.TesrDecorLabeledCrate());
  }
}
