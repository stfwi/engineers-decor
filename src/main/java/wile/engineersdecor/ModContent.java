/*
 * @file ModContent.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Definition and initialisation of blocks of this
 * module, along with their tile entities if applicable.
 *
 * Note: Straight forward definition of different blocks/entities
 *       to make recipes, models and texture definitions easier.
 */
package wile.engineersdecor;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.lang3.ArrayUtils;
import wile.engineersdecor.blocks.*;
import wile.engineersdecor.detail.ModRenderers;
import wile.engineersdecor.items.EdItem;
import wile.engineersdecor.libmc.blocks.*;
import wile.engineersdecor.libmc.blocks.StandardBlocks.IStandardBlock;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@SuppressWarnings("unused")
public class ModContent
{
  private static final String MODID = ModEngineersDecor.MODID;

  //--------------------------------------------------------------------------------------------------------------------

  private static Boolean disallowSpawn(BlockState state, BlockGetter reader, BlockPos pos, EntityType<?> entity) { return false; }

  //--------------------------------------------------------------------------------------------------------------------
  // Registry auxiliary functions.
  //--------------------------------------------------------------------------------------------------------------------

  private static class ModRegistry
  {
    private static <T extends StandardEntityBlocks.StandardBlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> ctor, Block... blocks)
    {
      final BlockEntityType<T> tet =  BlockEntityType.Builder.of(ctor, blocks).build(null);
      tet.setRegistryName(MODID, name);
      return tet;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<?> builder)
    {
      final EntityType<T> et = (EntityType<T>)builder.build(new ResourceLocation(MODID, name).toString());
      et.setRegistryName(MODID, name);
      return et;
    }
  }


  //--------------------------------------------------------------------------------------------------------------------
  // Blocks
  //--------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock CLINKER_BRICK_BLOCK = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_block"));

  public static final VariantSlabBlock CLINKER_BRICK_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_slab"));

  public static final StandardStairsBlock CLINKER_BRICK_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_stairs"));

  public static final EdWallBlock CLINKER_BRICK_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_wall"));

  public static final StandardBlocks.BaseBlock CLINKER_BRICK_STAINED_BLOCK = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_stained_block"));

  public static final VariantSlabBlock CLINKER_BRICK_STAINED_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_stained_slab"));

  public static final StandardStairsBlock CLINKER_BRICK_STAINED_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_stained_stairs"));

  public static final EdCornerOrnamentedBlock CLINKER_BRICK_SASTOR_CORNER = (EdCornerOrnamentedBlock)(new EdCornerOrnamentedBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE),
    new Block[]{CLINKER_BRICK_BLOCK, CLINKER_BRICK_STAINED_BLOCK, CLINKER_BRICK_SLAB, CLINKER_BRICK_STAIRS, CLINKER_BRICK_STAINED_SLAB, CLINKER_BRICK_STAINED_STAIRS}
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_sastor_corner_block"));

  public static final StandardBlocks.HorizontalWaterLoggable CLINKER_BRICK_RECESSED = (StandardBlocks.HorizontalWaterLoggable)(new StandardBlocks.HorizontalWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE),
    new AABB[] {
      Auxiliaries.getPixeledAABB( 3,0, 0, 13,16, 1),
      Auxiliaries.getPixeledAABB( 0,0, 1, 16,16,11),
      Auxiliaries.getPixeledAABB( 4,0,11, 12,16,13)
    }
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_recessed"));

  public static final StandardBlocks.HorizontalWaterLoggable CLINKER_BRICK_SASTOR_VERTICAL_SLOTTED = (StandardBlocks.HorizontalWaterLoggable)(new StandardBlocks.HorizontalWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE),
    new AABB[] {
      Auxiliaries.getPixeledAABB( 3,0, 0, 13,16, 1),
      Auxiliaries.getPixeledAABB( 3,0,15, 13,16,16),
      Auxiliaries.getPixeledAABB( 0,0, 1, 16,16,15)
    }
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_vertically_slit"));

  public static final StandardBlocks.HorizontalWaterLoggable CLINKER_BRICK_VERTICAL_SLAB_STRUCTURED = (StandardBlocks.HorizontalWaterLoggable)(new StandardBlocks.HorizontalWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE),
    new AABB[] {
      Auxiliaries.getPixeledAABB( 0,0, 0, 16,16, 8),
    }
  )).setRegistryName(new ResourceLocation(MODID, "clinker_brick_vertical_slab_structured"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock SLAG_BRICK_BLOCK = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "slag_brick_block"));

  public static final VariantSlabBlock SLAG_BRICK_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "slag_brick_slab"));

  public static final StandardStairsBlock SLAG_BRICK_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    SLAG_BRICK_BLOCK.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "slag_brick_stairs"));

  public static final EdWallBlock SLAG_BRICK_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(3f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "slag_brick_wall"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock REBAR_CONCRETE_BLOCK = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete"));

  public static final VariantSlabBlock REBAR_CONCRETE_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_slab"));

  public static final StandardStairsBlock REBAR_CONCRETE_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_stairs"));

  public static final EdWallBlock REBAR_CONCRETE_WALL = (EdWallBlock)(new EdWallBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_wall"));

  public static final SlabSliceBlock HALFSLAB_REBARCONCRETE = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_rebar_concrete"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock REBAR_CONCRETE_TILE = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_tile"));

  public static final VariantSlabBlock REBAR_CONCRETE_TILE_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_tile_slab"));

  public static final StandardStairsBlock REBAR_CONCRETE_TILE_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    REBAR_CONCRETE_TILE.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 2000f).sound(SoundType.STONE).isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "rebar_concrete_tile_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdGlassBlock PANZERGLASS_BLOCK = (EdGlassBlock)(new EdGlassBlock(
    DecorBlock.CFG_TRANSLUCENT,
    BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.NONE).strength(0.7f, 2000f).sound(SoundType.METAL).noOcclusion().isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "panzerglass_block"));

  public static final VariantSlabBlock PANZERGLASS_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_TRANSLUCENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.7f, 2000f).sound(SoundType.METAL).noOcclusion().isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "panzerglass_slab"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdRoofBlock DARK_CERAMIC_SHINGLE_ROOF = (EdRoofBlock)(new EdRoofBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof"));

  public static final EdRoofBlock DARK_CERAMIC_SHINGLE_ROOF_METALIZED = (EdRoofBlock)(new EdRoofBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_metallized"));

  public static final EdRoofBlock DARK_CERAMIC_SHINGLE_ROOF_SKYLIGHT = (EdRoofBlock)(new EdRoofBlock(
    DecorBlock.CFG_TRANSLUCENT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(ModContent::disallowSpawn)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_skylight"));

  public static final EdChimneyTrunkBlock DARK_CERAMIC_SHINGLE_ROOF_CHIMNEYTRUNK = (EdChimneyTrunkBlock)(new EdChimneyTrunkBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(ModContent::disallowSpawn),
    Shapes.create(Auxiliaries.getPixeledAABB(3, 0, 3, 13, 16, 13)),
    Shapes.create(Auxiliaries.getPixeledAABB(5, 0, 5, 11, 16, 11))
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_chimneytrunk"));

  public static final EdChimneyTrunkBlock DARK_CERAMIC_SHINGLE_ROOF_WIRECONDUIT = (EdChimneyTrunkBlock)(new EdChimneyTrunkBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(ModContent::disallowSpawn),
    Shapes.join(
      Shapes.create(Auxiliaries.getPixeledAABB(3,  0, 3, 13, 13, 13)),
      Shapes.create(Auxiliaries.getPixeledAABB(5, 13, 5, 11, 16, 11)),
      BooleanOp.OR
    ),
    Shapes.join(
      Shapes.create(Auxiliaries.getPixeledAABB(5,  0, 5, 11, 15, 11)),
      Shapes.create(Auxiliaries.getPixeledAABB(7, 15, 7,  9, 16,  9)),
      BooleanOp.OR
    )
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_wireconduit"));

  public static final EdChimneyBlock DARK_CERAMIC_SHINGLE_ROOF_CHIMNEY = (EdChimneyBlock)(new EdChimneyBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(5f, 6f).sound(SoundType.STONE).dynamicShape().isValidSpawn(ModContent::disallowSpawn),
    Auxiliaries.getPixeledAABB(3, 0, 3, 13, 6, 13)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_chimney"));

  public static final StandardBlocks.BaseBlock DARK_CERAMIC_SHINGLE_ROOF_BLOCK = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_block"));

  public static final VariantSlabBlock DARK_CERAMIC_SHINGLE_ROOF_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 6f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_slab"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock DENSE_GRIT_SAND = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.DIRT, MaterialColor.DIRT).strength(0.1f, 3f).sound(SoundType.GRAVEL)
  )).setRegistryName(new ResourceLocation(MODID, "dense_grit_sand_block"));

  public static final StandardBlocks.BaseBlock DENSE_GRIT_DIRT = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.DIRT, MaterialColor.DIRT).strength(0.1f, 3f).sound(SoundType.GRAVEL)
  )).setRegistryName(new ResourceLocation(MODID, "dense_grit_dirt_block"));

  public static final SlabSliceBlock HALFSLAB_DARK_CERAMIC_SHINGLE_ROOF = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(2f, 15f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(MODID, "dark_shingle_roof_slabslice"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdLadderBlock METAL_RUNG_LADDER = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1.0f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "metal_rung_ladder"));

  public static final EdLadderBlock METAL_RUNG_STEPS = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1.0f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "metal_rung_steps"));

  public static final EdLadderBlock TREATED_WOOD_LADDER = (EdLadderBlock)(new EdLadderBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.0f, 8f).sound(SoundType.WOOD).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_ladder"));

  public static final EdHatchBlock IRON_HATCH = (EdHatchBlock)(new EdHatchBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 2000f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0.5,1,0, 15.5,3,14),
    Auxiliaries.getPixeledAABB(0.5,1,0, 15.5,14.,2)
  )).setRegistryName(new ResourceLocation(MODID, "iron_hatch"));

  public static final StandardDoorBlock METAL_SLIDING_DOOR = (StandardDoorBlock)(new StandardDoorBlock(
    DecorBlock.CFG_TRANSLUCENT|DecorBlock.CFG_HORIZIONTAL,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1.5f, 8f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(15, 0.0,6, 16,16.0,10),
      Auxiliaries.getPixeledAABB( 0,15.5,6, 16,16.0,10),
    },
    new AABB[]{
      Auxiliaries.getPixeledAABB(15, 0.0,6, 16,16.0,10),
      Auxiliaries.getPixeledAABB( 0, 0.0,6, 16, 0.3,10),
    },
    new AABB[]{
      Auxiliaries.getPixeledAABB( 0,0,7, 16,16,9)
    },
    new AABB[]{
      Auxiliaries.getPixeledAABB( 0,0,7, 16,16,9)
    },
    SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE
  )).setRegistryName(new ResourceLocation(MODID, "metal_sliding_door"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.BaseBlock OLD_INDUSTRIAL_PLANKS = (StandardBlocks.BaseBlock)(new StandardBlocks.BaseBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5f, 6f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(MODID, "old_industrial_wood_planks"));

  public static final VariantSlabBlock OLD_INDUSTRIAL_SLAB = (VariantSlabBlock)(new VariantSlabBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5f, 6f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(MODID, "old_industrial_wood_slab"));

  public static final StandardStairsBlock OLD_INDUSTRIAL_STAIRS = (StandardStairsBlock)(new StandardStairsBlock(
    DecorBlock.CFG_DEFAULT,
    OLD_INDUSTRIAL_PLANKS.defaultBlockState(),
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5f, 6f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(MODID, "old_industrial_wood_stairs"));

  public static final SlabSliceBlock OLD_INDUSTRIAL_SLABSLICE = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5f, 6f).sound(SoundType.WOOD).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "old_industrial_wood_slabslice"));

  public static final StandardDoorBlock OLD_INDUSTRIAL_WOOD_DOOR = (StandardDoorBlock)(new StandardDoorBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1.5f, 6f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(15,0, 0, 16,16,16),
    Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
    SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE
  )).setRegistryName(new ResourceLocation(MODID, "old_industrial_wood_door"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.WaterLoggable TREATED_WOOD_TABLE = (StandardBlocks.WaterLoggable)(new StandardBlocks.WaterLoggable(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(1,0,1, 15,15.9,15)
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_table"));

  public static final EdChair.ChairBlock TREATED_WOOD_STOOL = (EdChair.ChairBlock)(new EdChair.ChairBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(4,7,4, 12,8.8,12),
      Auxiliaries.getPixeledAABB(7,0,7, 9,7,9),
      Auxiliaries.getPixeledAABB(4,0,7, 12,1,9),
      Auxiliaries.getPixeledAABB(7,0,4, 9,1,12),
    }
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_stool"));

  public static final StandardBlocks.DirectedWaterLoggable TREATED_WOOD_WINDOWSILL = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(0.5,15,10.5, 15.5,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_windowsill"));

  public static final StandardBlocks.DirectedWaterLoggable TREATED_WOOD_BROAD_WINDOWSILL = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_FACING_PLACEMENT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,14.5,4, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_broad_windowsill"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.DirectedWaterLoggable INSET_LIGHT_IRON = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
    Auxiliaries.getPixeledAABB(5.2,5.2,0, 10.8,10.8,0.3)
  )).setRegistryName(new ResourceLocation(MODID, "iron_inset_light"));

  public static final StandardBlocks.DirectedWaterLoggable FLOOR_EDGE_LIGHT_IRON = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
    Auxiliaries.getPixeledAABB(5,0,0, 11,2,0.5)
  )).setRegistryName(new ResourceLocation(MODID, "iron_floor_edge_light"));

  public static final StandardBlocks.DirectedWaterLoggable CEILING_EDGE_LIGHT_IRON = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB( 0,15.5,0, 16,16,2.0),
      Auxiliaries.getPixeledAABB( 0,14.0,0, 16,16,0.5),
      Auxiliaries.getPixeledAABB( 0,14.0,0,  1,16,2.0),
      Auxiliaries.getPixeledAABB(15,14.0,0, 16,16,2.0),
    }
  )).setRegistryName(new ResourceLocation(MODID, "iron_ceiling_edge_light"));

  public static final StandardBlocks.DirectedWaterLoggable BULB_LIGHT_IRON = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(6.5,6.5,1, 9.5,9.5,4),
      Auxiliaries.getPixeledAABB(6.0,6.0,0, 10.0,10.0,1.0)
    }
  )).setRegistryName(new ResourceLocation(MODID, "iron_bulb_light"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.WaterLoggable STEEL_TABLE = (StandardBlocks.WaterLoggable)(new StandardBlocks.WaterLoggable(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "steel_table"));

  public static final EdFloorGratingBlock STEEL_FLOOR_GRATING = (EdFloorGratingBlock)(new EdFloorGratingBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,14,0, 16,15.5,16)
  )).setRegistryName(new ResourceLocation(MODID, "steel_floor_grating"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdWindowBlock TREATED_WOOD_WINDOW = (EdWindowBlock)(new EdWindowBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 8f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,7, 16,16,9)
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_window"));

  public static final EdWindowBlock STEEL_FRAMED_WINDOW = (EdWindowBlock)(new EdWindowBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,7.5, 16,16,8.5)
  )).setRegistryName(new ResourceLocation(MODID, "steel_framed_window"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdStraightPoleBlock TREATED_WOOD_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
    null
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_pole"));

  public static final EdStraightPoleBlock TREATED_WOOD_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
    TREATED_WOOD_POLE
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_pole_head"));

  public static final EdStraightPoleBlock TREATED_WOOD_POLE_SUPPORT = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(2f, 5f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
    TREATED_WOOD_POLE
  )).setRegistryName(new ResourceLocation(MODID, "treated_wood_pole_support"));

  public static final EdStraightPoleBlock THIN_STEEL_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 11f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(6,6,0, 10,10,16),
    null
  )).setRegistryName(new ResourceLocation(MODID, "thin_steel_pole"));

  public static final EdStraightPoleBlock THIN_STEEL_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 11f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(6,6,0, 10,10,16),
    THIN_STEEL_POLE
  )).setRegistryName(new ResourceLocation(MODID, "thin_steel_pole_head"));

  public static final EdStraightPoleBlock THICK_STEEL_POLE = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 11f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(5,5,0, 11,11,16),
    null
  )).setRegistryName(new ResourceLocation(MODID, "thick_steel_pole"));

  public static final EdStraightPoleBlock THICK_STEEL_POLE_HEAD = (EdStraightPoleBlock)(new EdStraightPoleBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 11f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(5,5,0, 11,11,16),
    THICK_STEEL_POLE
  )).setRegistryName(new ResourceLocation(MODID, "thick_steel_pole_head"));

  public static final EdHorizontalSupportBlock STEEL_DOUBLE_T_SUPPORT = (EdHorizontalSupportBlock)(new EdHorizontalSupportBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 11f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB( 5,11,0, 11,16,16), // main beam
    Auxiliaries.getPixeledAABB(10,11,5, 16,16,11), // east beam (also for west 180deg)
    Auxiliaries.getPixeledAABB( 6, 0,6, 10,16,10), // down thin
    Auxiliaries.getPixeledAABB( 5, 0,5, 11,16,11)  // down thick
  )).setRegistryName(new ResourceLocation(MODID, "steel_double_t_support"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final StandardBlocks.DirectedWaterLoggable SIGN_MODLOGO = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1000f).sound(SoundType.WOOD).lightLevel((state)->1).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,15.6, 16,16,16.0)
  )).setRegistryName(new ResourceLocation(MODID, "sign_decor"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_HOTWIRE = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_hotwire"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_DANGER = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_danger"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_DEFENSE = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_defense"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_FACTORY_AREA = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_factoryarea"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_EXIT = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(3,7,15.6, 13,13,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_exit"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_RADIOACTIVE = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_radioactive"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_LASER = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_laser"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_CAUTION = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_caution"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_MAGIC_HAZARD = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_magichazard"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_FIRE_HAZARD = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_firehazard"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_HOT_SURFACE = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_hotsurface"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_MAGNETIC_FIELD = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_magneticfield"));

  public static final StandardBlocks.DirectedWaterLoggable SIGN_FROST_WARNING = (StandardBlocks.DirectedWaterLoggable)(new StandardBlocks.DirectedWaterLoggable(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_AI_PASSABLE,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 1f).sound(SoundType.WOOD).noOcclusion(),
    Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
  )).setRegistryName(new ResourceLocation(MODID, "sign_frost"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdCraftingTable.CraftingTableBlock CRAFTING_TABLE = (EdCraftingTable.CraftingTableBlock)(new EdCraftingTable.CraftingTableBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 12f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(0,15,0, 16,16,16),
      Auxiliaries.getPixeledAABB(1, 0,1, 15,16,15)
    }
  )).setRegistryName(new ResourceLocation(MODID, "metal_crafting_table"));

  public static final EdFurnace.FurnaceBlock SMALL_LAB_FURNACE = (EdFurnace.FurnaceBlock)(new EdFurnace.FurnaceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 12f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(1,0,1, 15, 1,15),
      Auxiliaries.getPixeledAABB(0,1,1, 16,16,16),
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_lab_furnace"));

  public static final EdElectricalFurnace.ElectricalFurnaceBlock SMALL_ELECTRICAL_FURNACE = (EdElectricalFurnace.ElectricalFurnaceBlock)(new EdElectricalFurnace.ElectricalFurnaceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(0, 0,0, 16,11,16),
      Auxiliaries.getPixeledAABB(1,11,0, 15,12,16),
      Auxiliaries.getPixeledAABB(2,12,0, 14,13,16),
      Auxiliaries.getPixeledAABB(3,13,0, 13,14,16),
      Auxiliaries.getPixeledAABB(4,14,0, 12,16,16),
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_electrical_furnace"));

  public static final EdDropper.DropperBlock FACTORY_DROPPER = (EdDropper.DropperBlock)(new EdDropper.DropperBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,1, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "factory_dropper"));

  public static final EdPlacer.PlacerBlock FACTORY_PLACER = (EdPlacer.PlacerBlock)(new EdPlacer.PlacerBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(0,0,2, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,0, 1,16, 2),
      Auxiliaries.getPixeledAABB(15,0,0,16,16, 2)
    }
  )).setRegistryName(new ResourceLocation(MODID, "factory_placer"));

  public static final EdBreaker.BreakerBlock SMALL_BLOCK_BREAKER = (EdBreaker.BreakerBlock)(new EdBreaker.BreakerBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(1,0,0, 15, 4, 7),
      Auxiliaries.getPixeledAABB(1,0,7, 15,12,16),
      Auxiliaries.getPixeledAABB(0,0,0, 1, 5, 4),
      Auxiliaries.getPixeledAABB(0,0,4, 1,12,16),
      Auxiliaries.getPixeledAABB(15,0,0, 16, 5, 4),
      Auxiliaries.getPixeledAABB(15,0,4, 16,12,16)
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_block_breaker"));

  public static final EdHopper.HopperBlock FACTORY_HOPPER = (EdHopper.HopperBlock)(new EdHopper.HopperBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),()->{
      final AABB[] down_aabbs = new AABB[]{
        Auxiliaries.getPixeledAABB( 5, 0, 5, 11, 1,11),
        Auxiliaries.getPixeledAABB( 4, 1, 4, 12, 7,12),
        Auxiliaries.getPixeledAABB( 2, 7, 2, 14,10,14),
        Auxiliaries.getPixeledAABB( 0,10, 0, 16,16,16),
        Auxiliaries.getPixeledAABB( 0, 4, 5,  2,10,11),
        Auxiliaries.getPixeledAABB(14, 4, 5, 16,10,11),
        Auxiliaries.getPixeledAABB( 5, 4, 0, 11,10, 2),
        Auxiliaries.getPixeledAABB( 5, 4,14, 11,10,16),
      };
      final AABB[] up_aabbs = new AABB[]{
        Auxiliaries.getPixeledAABB( 5,15, 5, 11,16,11),
        Auxiliaries.getPixeledAABB( 4,14, 4, 12, 9,12),
        Auxiliaries.getPixeledAABB( 2, 9, 2, 14, 6,14),
        Auxiliaries.getPixeledAABB( 0, 6, 0, 16, 0,16),
        Auxiliaries.getPixeledAABB( 0,12, 5,  2, 6,11),
        Auxiliaries.getPixeledAABB(14,12, 5, 16, 6,11),
        Auxiliaries.getPixeledAABB( 5,12, 0, 11, 6, 2),
        Auxiliaries.getPixeledAABB( 5,12,14, 11, 6,16),
      };
      final AABB[] north_aabbs = new AABB[]{
        Auxiliaries.getPixeledAABB( 5, 0, 5, 11, 1,11),
        Auxiliaries.getPixeledAABB( 4, 1, 4, 12, 7,12),
        Auxiliaries.getPixeledAABB( 2, 7, 2, 14,10,14),
        Auxiliaries.getPixeledAABB( 0,10, 0, 16,16,16),
        Auxiliaries.getPixeledAABB( 0, 4, 5,  2,10,11),
        Auxiliaries.getPixeledAABB(14, 4, 5, 16,10,11),
        Auxiliaries.getPixeledAABB( 5, 1, 0, 11, 7, 4),
        Auxiliaries.getPixeledAABB( 5, 4,14, 11,10,16),
      };
      return new ArrayList<>(Arrays.asList(
        Auxiliaries.getUnionShape(down_aabbs),
        Auxiliaries.getUnionShape(up_aabbs),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.NORTH, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.SOUTH, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.WEST, false)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(north_aabbs, Direction.EAST, false)),
        Shapes.block(),
        Shapes.block()
      ));
    }
  )).setRegistryName(new ResourceLocation(MODID, "factory_hopper"));

  public static final EdWasteIncinerator.WasteIncineratorBlock SMALL_WASTE_INCINERATOR = (EdWasteIncinerator.WasteIncineratorBlock)(new EdWasteIncinerator.WasteIncineratorBlock(
    DecorBlock.CFG_DEFAULT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "small_waste_incinerator"));

  public static final EdMineralSmelter.MineralSmelterBlock SMALL_MINERAL_SMELTER = (EdMineralSmelter.MineralSmelterBlock)(new EdMineralSmelter.MineralSmelterBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
  )).setRegistryName(new ResourceLocation(MODID, "small_mineral_smelter"));

  public static final EdFreezer.FreezerBlock SMALL_FREEZER = (EdFreezer.FreezerBlock)(new EdFreezer.FreezerBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 12f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
  )).setRegistryName(new ResourceLocation(MODID, "small_freezer"));

  public static final EdSolarPanel.SolarPanelBlock SMALL_SOLAR_PANEL = (EdSolarPanel.SolarPanelBlock)(new EdSolarPanel.SolarPanelBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(0,0,0, 16,2,16),
      Auxiliaries.getPixeledAABB(6,1.5,3, 10,10.5,13),
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_solar_panel"));

  public static final EdMilker.MilkerBlock SMALL_MILKING_MACHINE = (EdMilker.MilkerBlock)(new EdMilker.MilkerBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB( 1, 1,0, 15,14,10),
      Auxiliaries.getPixeledAABB( 0,14,0, 16,16,13),
      Auxiliaries.getPixeledAABB( 0, 0,0, 16, 1,13),
      Auxiliaries.getPixeledAABB( 0, 1,1,  1,14,11),
      Auxiliaries.getPixeledAABB(15, 1,1, 16,14,11)
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_milking_machine"));

  public static final EdTreeCutter.TreeCutterBlock SMALL_TREE_CUTTER = (EdTreeCutter.TreeCutterBlock)(new EdTreeCutter.TreeCutterBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB( 0,0, 0, 16,3,16),
      Auxiliaries.getPixeledAABB( 0,3, 0,  3,8,16),
      Auxiliaries.getPixeledAABB( 3,7, 0,  5,8,16),
      Auxiliaries.getPixeledAABB(15,0, 0, 16,6,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,8,16),
      Auxiliaries.getPixeledAABB( 5,6,12, 16,8,13),
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_tree_cutter"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_CHECK_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT|DecorBlock.CFG_FLIP_PLACEMENT_SHIFTCLICK,
    EdPipeValve.CFG_CHECK_VALVE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(MODID, "straight_pipe_valve"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_REDSTONE_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(MODID, "straight_pipe_valve_redstone"));

  public static final EdPipeValve.PipeValveBlock STRAIGHT_REDSTONE_ANALOG_VALVE = (EdPipeValve.PipeValveBlock)(new EdPipeValve.PipeValveBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_FACING_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE|EdPipeValve.CFG_ANALOG_VALVE,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 8f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
      Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
      Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
      Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
    }
  )).setRegistryName(new ResourceLocation(MODID, "straight_pipe_valve_redstone_analog"));

  public static final EdFluidBarrel.FluidBarrelBlock FLUID_BARREL = (EdFluidBarrel.FluidBarrelBlock)(new EdFluidBarrel.FluidBarrelBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(2, 0,0, 14, 1,16),
      Auxiliaries.getPixeledAABB(1, 1,0, 15, 2,16),
      Auxiliaries.getPixeledAABB(0, 2,0, 16,14,16),
      Auxiliaries.getPixeledAABB(1,14,0, 15,15,16),
      Auxiliaries.getPixeledAABB(2,15,0, 14,16,16),
    }
  )).setRegistryName(new ResourceLocation(MODID, "fluid_barrel"));

  public static final EdFluidFunnel.FluidFunnelBlock SMALL_FLUID_FUNNEL = (EdFluidFunnel.FluidFunnelBlock)(new EdFluidFunnel.FluidFunnelBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[]{
      Auxiliaries.getPixeledAABB(0, 0,0, 16,14,16),
      Auxiliaries.getPixeledAABB(1,14,1, 15,15,15),
      Auxiliaries.getPixeledAABB(0,15,0, 16,16,16)
    }
  )).setRegistryName(new ResourceLocation(MODID, "small_fluid_funnel"));

  public static final EdLabeledCrate.LabeledCrateBlock LABELED_CRATE = (EdLabeledCrate.LabeledCrateBlock)(new EdLabeledCrate.LabeledCrateBlock(
    DecorBlock.CFG_HORIZIONTAL|DecorBlock.CFG_LOOK_PLACEMENT|DecorBlock.CFG_OPPOSITE_PLACEMENT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 32f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "labeled_crate"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final SlabSliceBlock HALFSLAB_TREATEDWOOD = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(1f, 4f).sound(SoundType.WOOD).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_treated_wood"));

  public static final SlabSliceBlock HALFSLAB_SHEETMETALIRON = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_sheetmetal_iron"));

  public static final SlabSliceBlock HALFSLAB_SHEETMETALSTEEL = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_sheetmetal_steel"));

  public static final SlabSliceBlock HALFSLAB_SHEETMETALCOPPER = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_sheetmetal_copper"));

  public static final SlabSliceBlock HALFSLAB_SHEETMETALGOLD = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_sheetmetal_gold"));

  public static final SlabSliceBlock HALFSLAB_SHEETMETALALUMINIUM = (SlabSliceBlock)(new SlabSliceBlock(
    DecorBlock.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 8f).sound(SoundType.METAL).noOcclusion()
  )).setRegistryName(new ResourceLocation(MODID, "halfslab_sheetmetal_aluminum"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdFenceBlock STEEL_MESH_FENCE = (EdFenceBlock)(new EdFenceBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    1.5, 16, 0.25, 0, 16, 16
  )).setRegistryName(new ResourceLocation(MODID, "steel_mesh_fence"));

  public static final EdDoubleGateBlock STEEL_MESH_FENCE_GATE = (EdDoubleGateBlock)(new EdDoubleGateBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,6.5, 16,16,9.5)
  )).setRegistryName(new ResourceLocation(MODID, "steel_mesh_fence_gate"));

  public static final EdRailingBlock STEEL_RAILING = (EdRailingBlock)(new EdRailingBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(1f, 10f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,0,  0, 0,0),
    Auxiliaries.getPixeledAABB(0,0,0, 16,15.9,1)
  )).setRegistryName(new ResourceLocation(MODID, "steel_railing"));

  public static final EdCatwalkBlock STEEL_CATWALK = (EdCatwalkBlock)(new EdCatwalkBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    Auxiliaries.getPixeledAABB(0,0,0, 16, 2,16),
    Auxiliaries.getPixeledAABB(0,0,0, 16,15.9, 1),
    STEEL_RAILING
  )).setRegistryName(new ResourceLocation(MODID, "steel_catwalk"));

  public static final EdCatwalkTopAlignedBlock STEEL_CATWALK_TOP_ALIGNED = (EdCatwalkTopAlignedBlock)(new EdCatwalkTopAlignedBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new VoxelShape[]{
      Shapes.create(Auxiliaries.getPixeledAABB(0,14,0, 16, 16,16)), // only base
      Auxiliaries.getUnionShape( // base with thick pole
        Auxiliaries.getPixeledAABB(0,14,0, 16, 16,16),
        Auxiliaries.getPixeledAABB(5, 0,5, 11,15, 11)
      ),
      Auxiliaries.getUnionShape( // base with thin pole
        Auxiliaries.getPixeledAABB(0,14,0, 16, 16,16),
        Auxiliaries.getPixeledAABB(6, 0,6, 10,15, 10)
      ),
      Auxiliaries.getUnionShape( // structure frame-like
        Auxiliaries.getPixeledAABB( 0, 0, 0, 16,  2,16),
        Auxiliaries.getPixeledAABB( 0,14, 0, 16, 16,16),
        Auxiliaries.getPixeledAABB( 0, 0, 0,  1, 16, 1),
        Auxiliaries.getPixeledAABB(15, 0, 0, 16, 16, 1),
        Auxiliaries.getPixeledAABB(15, 0,15, 16, 16,16),
        Auxiliaries.getPixeledAABB( 0, 0,15,  1, 16,16)
      )
    }
  )).setRegistryName(new ResourceLocation(MODID, "steel_catwalk_ta"));

  public static final EdCatwalkStairsBlock STEEL_CATWALK_STAIRS = (EdCatwalkStairsBlock)(new EdCatwalkStairsBlock(
    DecorBlock.CFG_CUTOUT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(2f, 10f).sound(SoundType.METAL).noOcclusion(),
    new AABB[] { // base
      Auxiliaries.getPixeledAABB( 1, 2, 8, 15,  4,  16),
      Auxiliaries.getPixeledAABB( 1,10, 0, 15, 12,   8),
    },
    new AABB[] { // railing left
      Auxiliaries.getPixeledAABB(0.4,  0, 15, 0.6, 15, 16),
      Auxiliaries.getPixeledAABB(0.4,  1, 14, 0.6, 16, 15),
      Auxiliaries.getPixeledAABB(0.4,  2, 13, 0.6, 17, 14),
      Auxiliaries.getPixeledAABB(0.4,  3, 12, 0.6, 18, 13),
      Auxiliaries.getPixeledAABB(0.4,  4, 11, 0.6, 19, 12),
      Auxiliaries.getPixeledAABB(0.4,  5, 10, 0.6, 20, 11),
      Auxiliaries.getPixeledAABB(0.4,  6,  9, 0.6, 21, 10),
      Auxiliaries.getPixeledAABB(0.4,  7,  8, 0.6, 22,  9),
      Auxiliaries.getPixeledAABB(0.4,  8,  7, 0.6, 23,  8),
      Auxiliaries.getPixeledAABB(0.4,  9,  6, 0.6, 24,  7),
      Auxiliaries.getPixeledAABB(0.4, 10,  5, 0.6, 25,  6),
      Auxiliaries.getPixeledAABB(0.4, 11,  4, 0.6, 26,  5),
      Auxiliaries.getPixeledAABB(0.4, 12,  3, 0.6, 27,  4),
      Auxiliaries.getPixeledAABB(0.4, 13,  2, 0.6, 28,  3),
      Auxiliaries.getPixeledAABB(0.4, 14,  1, 0.6, 29,  2),
      Auxiliaries.getPixeledAABB(0.4, 15,  0, 0.6, 30,  1)
    }
  )).setRegistryName(new ResourceLocation(MODID, "steel_catwalk_stairs"));

  // -------------------------------------------------------------------------------------------------------------------

  public static final EdTestBlock.TestBlock TEST_BLOCK = (EdTestBlock.TestBlock)(new EdTestBlock.TestBlock(
    DecorBlock.CFG_LOOK_PLACEMENT,
    BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0f, 32000f).sound(SoundType.METAL),
    Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
  )).setRegistryName(new ResourceLocation(MODID, "test_block"));

  // -------------------------------------------------------------------------------------------------------------------

  private static final Block[] modBlocks = {
    CRAFTING_TABLE,
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
//>>>> untested
    SMALL_MINERAL_SMELTER,
    SMALL_FREEZER,
    SMALL_MILKING_MACHINE,
//<<<< /untested
    FLUID_BARREL,
    STRAIGHT_CHECK_VALVE,
    STRAIGHT_REDSTONE_VALVE,
    STRAIGHT_REDSTONE_ANALOG_VALVE,
    SMALL_FLUID_FUNNEL,
    DENSE_GRIT_SAND,
    DENSE_GRIT_DIRT,
    CLINKER_BRICK_BLOCK,
    CLINKER_BRICK_SLAB,
    CLINKER_BRICK_STAIRS,
    CLINKER_BRICK_WALL,
    CLINKER_BRICK_SASTOR_CORNER,
    CLINKER_BRICK_STAINED_BLOCK,
    CLINKER_BRICK_STAINED_SLAB,
    CLINKER_BRICK_STAINED_STAIRS,
    CLINKER_BRICK_SASTOR_VERTICAL_SLOTTED,
    CLINKER_BRICK_RECESSED,
    CLINKER_BRICK_VERTICAL_SLAB_STRUCTURED,
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
    HALFSLAB_REBARCONCRETE,
    HALFSLAB_TREATEDWOOD,
    HALFSLAB_SHEETMETALIRON,
    HALFSLAB_SHEETMETALSTEEL,
    HALFSLAB_SHEETMETALCOPPER,
    HALFSLAB_SHEETMETALGOLD,
    HALFSLAB_SHEETMETALALUMINIUM,
    PANZERGLASS_BLOCK,
    PANZERGLASS_SLAB,
    DARK_CERAMIC_SHINGLE_ROOF,
    DARK_CERAMIC_SHINGLE_ROOF_METALIZED,
    DARK_CERAMIC_SHINGLE_ROOF_SKYLIGHT,
    DARK_CERAMIC_SHINGLE_ROOF_CHIMNEYTRUNK,
    DARK_CERAMIC_SHINGLE_ROOF_WIRECONDUIT,
    DARK_CERAMIC_SHINGLE_ROOF_BLOCK,
    DARK_CERAMIC_SHINGLE_ROOF_SLAB,
    HALFSLAB_DARK_CERAMIC_SHINGLE_ROOF,
    DARK_CERAMIC_SHINGLE_ROOF_CHIMNEY,
    METAL_RUNG_LADDER,
    METAL_RUNG_STEPS,
    TREATED_WOOD_LADDER,
    METAL_SLIDING_DOOR,
    IRON_HATCH,
    OLD_INDUSTRIAL_PLANKS,
    OLD_INDUSTRIAL_SLAB,
    OLD_INDUSTRIAL_STAIRS,
    OLD_INDUSTRIAL_SLABSLICE,
    OLD_INDUSTRIAL_WOOD_DOOR,
    TREATED_WOOD_TABLE,
    TREATED_WOOD_STOOL,
    TREATED_WOOD_WINDOWSILL,
    TREATED_WOOD_BROAD_WINDOWSILL,
    TREATED_WOOD_WINDOW,
    STEEL_FRAMED_WINDOW,
    STEEL_TABLE,
    INSET_LIGHT_IRON,
    FLOOR_EDGE_LIGHT_IRON,
    CEILING_EDGE_LIGHT_IRON,
    BULB_LIGHT_IRON,
    STEEL_FLOOR_GRATING,
    STEEL_MESH_FENCE,
    STEEL_MESH_FENCE_GATE,
    STEEL_CATWALK,
    STEEL_RAILING,
    STEEL_CATWALK_TOP_ALIGNED,
    STEEL_CATWALK_STAIRS,
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
    SIGN_RADIOACTIVE,
    SIGN_LASER,
    SIGN_CAUTION,
    SIGN_MAGIC_HAZARD,
    SIGN_FIRE_HAZARD,
    SIGN_HOT_SURFACE,
    SIGN_MAGNETIC_FIELD,
    SIGN_FROST_WARNING,
    SIGN_MODLOGO,
  };

  private static final Block[] devBlocks = {
    TEST_BLOCK
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final BlockEntityType<EdCraftingTable.CraftingTableTileEntity> TET_CRAFTING_TABLE = ModRegistry.register("te_treated_wood_crafting_table", EdCraftingTable.CraftingTableTileEntity::new, CRAFTING_TABLE);
  public static final BlockEntityType<EdLabeledCrate.LabeledCrateTileEntity> TET_LABELED_CRATE = ModRegistry.register("te_labeled_crate", EdLabeledCrate.LabeledCrateTileEntity::new, LABELED_CRATE);
  public static final BlockEntityType<EdFurnace.FurnaceTileEntity> TET_SMALL_LAB_FURNACE = ModRegistry.register("te_small_lab_furnace", EdFurnace.FurnaceTileEntity::new, SMALL_LAB_FURNACE);
  public static final BlockEntityType<EdElectricalFurnace.ElectricalFurnaceTileEntity> TET_SMALL_ELECTRICAL_FURNACE = ModRegistry.register("te_small_electrical_furnace", EdElectricalFurnace.ElectricalFurnaceTileEntity::new, SMALL_ELECTRICAL_FURNACE);
  public static final BlockEntityType<EdDropper.DropperTileEntity> TET_FACTORY_DROPPER = ModRegistry.register("te_factory_dropper", EdDropper.DropperTileEntity::new, FACTORY_DROPPER);
  public static final BlockEntityType<EdPlacer.PlacerTileEntity> TET_FACTORY_PLACER = ModRegistry.register("te_factory_placer", EdPlacer.PlacerTileEntity::new, FACTORY_PLACER);
  public static final BlockEntityType<EdBreaker.BreakerTileEntity> TET_SMALL_BLOCK_BREAKER = ModRegistry.register("te_small_block_breaker", EdBreaker.BreakerTileEntity::new, SMALL_BLOCK_BREAKER);
  public static final BlockEntityType<EdHopper.HopperTileEntity> TET_FACTORY_HOPPER = ModRegistry.register("te_factory_hopper", EdHopper.HopperTileEntity::new, FACTORY_HOPPER);
  public static final BlockEntityType<EdWasteIncinerator.WasteIncineratorTileEntity> TET_WASTE_INCINERATOR = ModRegistry.register("te_small_waste_incinerator", EdWasteIncinerator.WasteIncineratorTileEntity::new, SMALL_WASTE_INCINERATOR);
  public static final BlockEntityType<EdPipeValve.PipeValveTileEntity> TET_STRAIGHT_PIPE_VALVE = ModRegistry.register("te_pipe_valve", EdPipeValve.PipeValveTileEntity::new, STRAIGHT_CHECK_VALVE, STRAIGHT_REDSTONE_VALVE, STRAIGHT_REDSTONE_ANALOG_VALVE);
  public static final BlockEntityType<EdFluidBarrel.FluidBarrelTileEntity> TET_FLUID_BARREL = ModRegistry.register("te_fluid_barrel", EdFluidBarrel.FluidBarrelTileEntity::new, FLUID_BARREL);
  public static final BlockEntityType<EdFluidFunnel.FluidFunnelTileEntity> TET_SMALL_FLUID_FUNNEL = ModRegistry.register("te_small_fluid_funnel", EdFluidFunnel.FluidFunnelTileEntity::new, SMALL_FLUID_FUNNEL);
  public static final BlockEntityType<EdMineralSmelter.MineralSmelterTileEntity> TET_MINERAL_SMELTER = ModRegistry.register("te_small_mineral_smelter", EdMineralSmelter.MineralSmelterTileEntity::new, SMALL_MINERAL_SMELTER);
  public static final BlockEntityType<EdFreezer.FreezerTileEntity> TET_FREEZER = ModRegistry.register("te_small_freezer", EdFreezer.FreezerTileEntity::new, SMALL_FREEZER);
  public static final BlockEntityType<EdSolarPanel.SolarPanelTileEntity> TET_SMALL_SOLAR_PANEL = ModRegistry.register("te_small_solar_panel", EdSolarPanel.SolarPanelTileEntity::new, SMALL_SOLAR_PANEL);
  public static final BlockEntityType<EdMilker.MilkerTileEntity> TET_SMALL_MILKING_MACHINE = ModRegistry.register("te_small_milking_machine", EdMilker.MilkerTileEntity::new, SMALL_MILKING_MACHINE);
  public static final BlockEntityType<EdTreeCutter.TreeCutterTileEntity> TET_SMALL_TREE_CUTTER = ModRegistry.register("te_small_tree_cutter", EdTreeCutter.TreeCutterTileEntity::new, SMALL_TREE_CUTTER);
  public static final BlockEntityType<EdTestBlock.TestTileEntity> TET_TEST_BLOCK = ModRegistry.register("te_test_block", EdTestBlock.TestTileEntity::new, TEST_BLOCK);

  private static final BlockEntityType<?>[] tile_entity_types = {
    TET_CRAFTING_TABLE,
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
    TET_FREEZER,
    TET_SMALL_SOLAR_PANEL,
    TET_SMALL_MILKING_MACHINE,
    TET_STRAIGHT_PIPE_VALVE,
    TET_FLUID_BARREL,
    TET_SMALL_FLUID_FUNNEL,
    TET_TEST_BLOCK
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Items
  //--------------------------------------------------------------------------------------------------------------------

  private static Item.Properties default_item_properties()
  { return (new Item.Properties()).tab(ModEngineersDecor.ITEMGROUP); }

  public static final EdItem METAL_BAR_ITEM = (EdItem)((new EdItem(default_item_properties()).setRegistryName(MODID, "metal_bar")));

  private static final EdItem[] modItems = {
    METAL_BAR_ITEM
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Entities bound exclusively to the blocks above
  //--------------------------------------------------------------------------------------------------------------------

  public static final EntityType<EdChair.EntityChair> ET_CHAIR = ModRegistry.register("et_chair",
    EntityType.Builder.of(EdChair.EntityChair::new, MobCategory.MISC)
      .fireImmune().sized(1e-3f, 1e-3f).noSave()
      .setShouldReceiveVelocityUpdates(false).setUpdateInterval(4)
      .setCustomClientFactory(EdChair.EntityChair::customClientFactory)
  );

  private static final EntityType<?>[] entity_types = {
    ET_CHAIR
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Container registration
  //--------------------------------------------------------------------------------------------------------------------

  public static final MenuType<EdCraftingTable.CraftingTableUiContainer> CT_TREATED_WOOD_CRAFTING_TABLE;
  public static final MenuType<EdDropper.DropperUiContainer> CT_FACTORY_DROPPER;
  public static final MenuType<EdPlacer.PlacerContainer> CT_FACTORY_PLACER;
  public static final MenuType<EdHopper.HopperContainer> CT_FACTORY_HOPPER;
  public static final MenuType<EdFurnace.FurnaceContainer> CT_SMALL_LAB_FURNACE;
  public static final MenuType<EdElectricalFurnace.ElectricalFurnaceContainer> CT_SMALL_ELECTRICAL_FURNACE;
  public static final MenuType<EdWasteIncinerator.WasteIncineratorContainer> CT_WASTE_INCINERATOR;
  public static final MenuType<EdLabeledCrate.LabeledCrateContainer> CT_LABELED_CRATE;

  static {
    CT_TREATED_WOOD_CRAFTING_TABLE = (new MenuType<>(EdCraftingTable.CraftingTableUiContainer::new));
    CT_TREATED_WOOD_CRAFTING_TABLE.setRegistryName(MODID,"ct_treated_wood_crafting_table");
    CT_FACTORY_DROPPER = (new MenuType<>(EdDropper.DropperUiContainer::new));
    CT_FACTORY_DROPPER.setRegistryName(MODID,"ct_factory_dropper");
    CT_FACTORY_PLACER = (new MenuType<>(EdPlacer.PlacerContainer::new));
    CT_FACTORY_PLACER.setRegistryName(MODID,"ct_factory_placer");
    CT_FACTORY_HOPPER = (new MenuType<>(EdHopper.HopperContainer::new));
    CT_FACTORY_HOPPER.setRegistryName(MODID,"ct_factory_hopper");
    CT_SMALL_LAB_FURNACE = (new MenuType<>(EdFurnace.FurnaceContainer::new));
    CT_SMALL_LAB_FURNACE.setRegistryName(MODID,"ct_small_lab_furnace");
    CT_SMALL_ELECTRICAL_FURNACE = (new MenuType<>(EdElectricalFurnace.ElectricalFurnaceContainer::new));
    CT_SMALL_ELECTRICAL_FURNACE.setRegistryName(MODID,"ct_small_electrical_furnace");
    CT_WASTE_INCINERATOR = (new MenuType<>(EdWasteIncinerator.WasteIncineratorContainer::new));
    CT_WASTE_INCINERATOR.setRegistryName(MODID,"ct_small_waste_incinerator");
    CT_LABELED_CRATE = (new MenuType<>(EdLabeledCrate.LabeledCrateContainer::new));
    CT_LABELED_CRATE.setRegistryName(MODID,"ct_labeled_crate");
  }

  private static final MenuType<?>[] container_types = {
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

  private static final ArrayList<Block> registeredBlocks = new ArrayList<>();

  public static ArrayList<Block> allBlocks()
  {
    ArrayList<Block> blocks = new ArrayList<>();
    Collections.addAll(blocks, modBlocks);
    Collections.addAll(blocks, devBlocks);
    return blocks;
  }

  @SuppressWarnings("deprecation")
  public static boolean isExperimentalBlock(Block block)
  { return ArrayUtils.contains(devBlocks, block) || ((block instanceof StandardBlocks.IStandardBlock) && ((((StandardBlocks.IStandardBlock)block).config() & DecorBlock.CFG_EXPERIMENTAL))!=0); }

  @Nonnull
  public static List<Block> getRegisteredBlocks()
  { return Collections.unmodifiableList(registeredBlocks); }

  @Nonnull
  public static List<Item> getRegisteredItems()
  { return new ArrayList<>(); }

  public static void registerBlocks(final RegistryEvent.Register<Block> event)
  {
    boolean ie_available = Auxiliaries.isModLoaded("immersiveengineering");
    if(ie_available) {
      Auxiliaries.logInfo("Immersive Engineering also installed ...");
      registeredBlocks.addAll(allBlocks());
    } else {
      registeredBlocks.addAll(allBlocks().stream()
        .filter(block->
          ((!(block instanceof StandardBlocks.IStandardBlock)) || ((((StandardBlocks.IStandardBlock)block).config() & DecorBlock.CFG_HARD_IE_DEPENDENT)==0))
        )
        .collect(Collectors.toList())
      );
    }
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    Auxiliaries.logInfo("Registered " + (registeredBlocks.size()) + " blocks.");
  }

  public static void registerBlockItems(final RegistryEvent.Register<Item> event)
  {
    int n = 0;
    for(Block e:registeredBlocks) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      if(e instanceof StandardBlocks.IBlockItemFactory) {
        event.getRegistry().register(((StandardBlocks.IBlockItemFactory)e).getBlockItem(e, (new Item.Properties().tab(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      } else {
        event.getRegistry().register(new BlockItem(e, (new Item.Properties().tab(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      }
      ++n;
    }
  }

  public static void registerItems(final RegistryEvent.Register<Item> event)
  { for(Item e:modItems) event.getRegistry().register(e); }

  public static void registerTileEntities(final RegistryEvent.Register<BlockEntityType<?>> event)
  {
    int n_registered = 0;
    for(final BlockEntityType<?> e:tile_entity_types) {
      event.getRegistry().register(e);
      ++n_registered;
    }
    Auxiliaries.logInfo("Registered " + (n_registered) + " tile entities.");
  }

  public static void registerEntities(final RegistryEvent.Register<EntityType<?>> event)
  {
    int n_registered = 0;
    for(final EntityType<?> e:entity_types) {
      if((e==ET_CHAIR) && (!registeredBlocks.contains(TREATED_WOOD_STOOL))) continue;
      event.getRegistry().register(e);
      ++n_registered;
    }
    Auxiliaries.logInfo("Registered " + (n_registered) + " entities bound to blocks.");
  }

  public static void registerContainers(final RegistryEvent.Register<MenuType<?>> event)
  {
    int n_registered = 0;
    for(final MenuType<?> e:container_types) {
      event.getRegistry().register(e);
      ++n_registered;
    }
    Auxiliaries.logInfo("Registered " + (n_registered) + " containers bound to tile entities.");
  }

  @OnlyIn(Dist.CLIENT)
  public static void registerContainerGuis(final FMLClientSetupEvent event)
  {
    MenuScreens.register(CT_TREATED_WOOD_CRAFTING_TABLE, EdCraftingTable.CraftingTableGui::new);
    MenuScreens.register(CT_LABELED_CRATE, EdLabeledCrate.LabeledCrateGui::new);
    MenuScreens.register(CT_FACTORY_DROPPER, EdDropper.DropperGui::new);
    MenuScreens.register(CT_FACTORY_PLACER, EdPlacer.PlacerGui::new);
    MenuScreens.register(CT_FACTORY_HOPPER, EdHopper.HopperGui::new);
    MenuScreens.register(CT_SMALL_LAB_FURNACE, EdFurnace.FurnaceGui::new);
    MenuScreens.register(CT_SMALL_ELECTRICAL_FURNACE, EdElectricalFurnace.ElectricalFurnaceGui::new);
    MenuScreens.register(CT_WASTE_INCINERATOR, EdWasteIncinerator.WasteIncineratorGui::new);
  }

  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("unchecked")
  public static void registerTileEntityRenderers(final FMLClientSetupEvent event)
  {
    BlockEntityRenderers.register(TET_CRAFTING_TABLE, wile.engineersdecor.detail.ModRenderers.CraftingTableTer::new);
    BlockEntityRenderers.register(TET_LABELED_CRATE, wile.engineersdecor.detail.ModRenderers.DecorLabeledCrateTer::new);
  }

  @OnlyIn(Dist.CLIENT)
  public static void processContentClientSide(final FMLClientSetupEvent event)
  {
    // Block renderer selection
    for(Block block: getRegisteredBlocks()) {
      if(block instanceof IStandardBlock) {
        switch(((IStandardBlock)block).getRenderTypeHint()) {
          case CUTOUT: ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutout()); break;
          case CUTOUT_MIPPED: ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped()); break;
          case TRANSLUCENT: ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucent()); break;
          case TRANSLUCENT_NO_CRUMBLING: ItemBlockRenderTypes.setRenderLayer(block, RenderType.translucentNoCrumbling()); break;
          case SOLID: break;
        }
      }
    }
    // Entity renderers
    EntityRenderers.register(ET_CHAIR, ModRenderers.InvisibleEntityRenderer::new);
  }

}
