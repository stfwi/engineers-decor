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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
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
import wile.engineersdecor.blocks.*;
import wile.engineersdecor.detail.ModRenderers;
import wile.engineersdecor.items.EdItem;
import wile.engineersdecor.libmc.blocks.*;
import wile.engineersdecor.libmc.blocks.StandardBlocks.IStandardBlock;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Registries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("unused")
public class ModContent
{
  private static class detail {
    public static String MODID = "";
    public static Boolean disallowSpawn(BlockState state, BlockGetter reader, BlockPos pos, EntityType<?> entity) { return false; }
  }

  public static void init(String modid)
  {
    detail.MODID = modid;
    initTags();
    initBlocks();
    initItems();
    initEntities();
  }

  public static void initTags()
  {
    Registries.addOptionalBlockTag("accepted_mineral_smelter_input", new ResourceLocation("minecraft", "diorite"), new ResourceLocation("minecraft", "cobblestone"));
  }

  public static void initBlocks()
  {
    Registries.addBlock("clinker_brick_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("clinker_brick_block").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_wall", ()->new EdWallBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_stained_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_stained_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_stained_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("clinker_brick_stained_block").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("clinker_brick_sastor_corner_block", ()->new EdCornerOrnamentedBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE),
      new Block[]{
        Registries.getBlock("clinker_brick_block"),
        Registries.getBlock("clinker_brick_slab"),
        Registries.getBlock("clinker_brick_stairs"),
        Registries.getBlock("clinker_brick_stained_block"),
        Registries.getBlock("clinker_brick_stained_slab"),
        Registries.getBlock("clinker_brick_stained_stairs")
      }
    ));
    Registries.addBlock("clinker_brick_recessed", ()->new StandardBlocks.HorizontalWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE),
      new AABB[] {
        Auxiliaries.getPixeledAABB( 3,0, 0, 13,16, 1),
        Auxiliaries.getPixeledAABB( 0,0, 1, 16,16,11),
        Auxiliaries.getPixeledAABB( 4,0,11, 12,16,13)
      }
    ));
    Registries.addBlock("clinker_brick_vertically_slit", ()->new StandardBlocks.HorizontalWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE),
      new AABB[] {
        Auxiliaries.getPixeledAABB( 3,0, 0, 13,16, 1),
        Auxiliaries.getPixeledAABB( 3,0,15, 13,16,16),
        Auxiliaries.getPixeledAABB( 0,0, 1, 16,16,15)
      }
    ));
    Registries.addBlock("clinker_brick_vertical_slab_structured", ()->new StandardBlocks.HorizontalWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE),
      new AABB[] {
        Auxiliaries.getPixeledAABB( 0,0, 0, 16,16, 8),
      }
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("slag_brick_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("slag_brick_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("slag_brick_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("slag_brick_block").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));
    Registries.addBlock("slag_brick_wall", ()->new EdWallBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 7f).sound(SoundType.STONE)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("rebar_concrete", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("rebar_concrete_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("rebar_concrete_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("rebar_concrete").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("rebar_concrete_wall", ()->new EdWallBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("halfslab_rebar_concrete", ()->new SlabSliceBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("rebar_concrete_tile", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("rebar_concrete_tile_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("rebar_concrete_tile_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("rebar_concrete_tile").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(1.0f, 2000f).sound(SoundType.STONE).isValidSpawn(detail::disallowSpawn)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("panzerglass_block", ()->new EdGlassBlock(
      StandardBlocks.CFG_TRANSLUCENT,
      BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.NONE).strength(0.5f, 2000f).sound(SoundType.METAL).noOcclusion().isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("panzerglass_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_TRANSLUCENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 2000f).sound(SoundType.METAL).noOcclusion().isValidSpawn(detail::disallowSpawn)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("dark_shingle_roof", ()->new EdRoofBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("dark_shingle_roof_metallized", ()->new EdRoofBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("dark_shingle_roof_skylight", ()->new EdRoofBlock(
      StandardBlocks.CFG_TRANSLUCENT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(detail::disallowSpawn)
    ));
    Registries.addBlock("dark_shingle_roof_chimneytrunk", ()->new EdChimneyTrunkBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(detail::disallowSpawn),
      Shapes.create(Auxiliaries.getPixeledAABB(3, 0, 3, 13, 16, 13)),
      Shapes.create(Auxiliaries.getPixeledAABB(5, 0, 5, 11, 16, 11))
    ));
    Registries.addBlock("dark_shingle_roof_wireconduit", ()->new EdChimneyTrunkBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).noOcclusion().dynamicShape().isValidSpawn(detail::disallowSpawn),
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
    ));
    Registries.addBlock("dark_shingle_roof_chimney", ()->new EdChimneyBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE).dynamicShape().isValidSpawn(detail::disallowSpawn),
      Auxiliaries.getPixeledAABB(3, 0, 3, 13, 6, 13)
    ));
    Registries.addBlock("dark_shingle_roof_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE)
    ));
    Registries.addBlock("dark_shingle_roof_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("dense_grit_sand_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.DIRT, MaterialColor.DIRT).strength(0.1f, 3f).sound(SoundType.GRAVEL)
    ));
    Registries.addBlock("dense_grit_dirt_block", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.DIRT, MaterialColor.DIRT).strength(0.1f, 3f).sound(SoundType.GRAVEL)
    ));
    Registries.addBlock("dark_shingle_roof_slabslice", ()->new SlabSliceBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE).strength(0.5f, 6f).sound(SoundType.STONE)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("metal_rung_ladder", ()->new EdLadderBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
    ));
    Registries.addBlock("metal_rung_steps", ()->new EdLadderBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
    ));
    Registries.addBlock("treated_wood_ladder", ()->new EdLadderBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.3f, 8f).sound(SoundType.WOOD).noOcclusion()
    ));
    Registries.addBlock("iron_hatch", ()->new EdHatchBlock(
      StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 2000f).sound(SoundType.METAL),
      new AABB[] { Auxiliaries.getPixeledAABB(0,0,0, 16,3,16) },
      new AABB[] {
        Auxiliaries.getPixeledAABB( 0,0, 0, 16,16, 3),
        Auxiliaries.getPixeledAABB( 0,0, 3,  1, 1,16),
        Auxiliaries.getPixeledAABB(15,0, 3, 16, 1,16),
        Auxiliaries.getPixeledAABB( 1,0,14, 15, 1,16)
      }
    ));
    Registries.addBlock("metal_sliding_door", ()->new StandardDoorBlock(
      StandardBlocks.CFG_TRANSLUCENT|StandardBlocks.CFG_HORIZIONTAL,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).noOcclusion(),
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
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("old_industrial_wood_planks", ()->new StandardBlocks.BaseBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 6f).sound(SoundType.WOOD)
    ));
    Registries.addBlock("old_industrial_wood_slab", ()->new VariantSlabBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 6f).sound(SoundType.WOOD)
    ));
    Registries.addBlock("old_industrial_wood_stairs", ()->new StandardStairsBlock(
      StandardBlocks.CFG_DEFAULT,
      ()->Registries.getBlock("old_industrial_wood_planks").defaultBlockState(),
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 6f).sound(SoundType.WOOD)
    ));
    Registries.addBlock("old_industrial_wood_slabslice", ()->new SlabSliceBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 6f).sound(SoundType.WOOD).noOcclusion()
    ));
    Registries.addBlock("old_industrial_wood_door", ()->new StandardDoorBlock(
      StandardBlocks.CFG_DEFAULT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 6f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(15,0, 0, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
      SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_DOOR_CLOSE
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("treated_wood_table", ()->new StandardBlocks.WaterLoggable(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(1,0,1, 15,15.9,15)
    ));
    Registries.addBlock("treated_wood_stool", ()->new EdChair.ChairBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      new AABB[]{
        Auxiliaries.getPixeledAABB(4,7,4, 12,8.8,12),
        Auxiliaries.getPixeledAABB(7,0,7, 9,7,9),
        Auxiliaries.getPixeledAABB(4,0,7, 12,1,9),
        Auxiliaries.getPixeledAABB(7,0,4, 9,1,12),
      }
    ));
    Registries.addBlock("treated_wood_windowsill", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(0.5,15,10.5, 15.5,16,16)
    ));
    Registries.addBlock("treated_wood_broad_windowsill", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_FACING_PLACEMENT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,14.5,4, 16,16,16)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("iron_inset_light", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
      new AABB[] {
        Auxiliaries.getPixeledAABB( 5,7,0, 11, 9,0.375),
        Auxiliaries.getPixeledAABB( 6,6,0, 10,10,0.375),
        Auxiliaries.getPixeledAABB( 7,5,0,  9,11,0.375)
      }
    ));
    Registries.addBlock("iron_floor_edge_light", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
      Auxiliaries.getPixeledAABB(5,0,0, 11,1.8125,0.375)
    ));
    Registries.addBlock("iron_ceiling_edge_light", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
      new AABB[]{
        Auxiliaries.getPixeledAABB( 0,15.5,0, 16,16,2.0),
        Auxiliaries.getPixeledAABB( 0,14.0,0, 16,16,0.5),
        Auxiliaries.getPixeledAABB( 0,14.0,0,  1,16,2.0),
        Auxiliaries.getPixeledAABB(15,14.0,0, 16,16,2.0),
      }
    ));
    Registries.addBlock("iron_bulb_light", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).lightLevel((state)->15).noOcclusion(),
      new AABB[]{
        Auxiliaries.getPixeledAABB(6.5,6.5,1, 9.5,9.5,4),
        Auxiliaries.getPixeledAABB(6.0,6.0,0, 10.0,10.0,1.0)
      }
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("steel_table", ()->new StandardBlocks.WaterLoggable(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
    ));
    Registries.addBlock("steel_floor_grating", ()->new EdFloorGratingBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,14,0, 16,15.5,16)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("treated_wood_window", ()->new EdWindowBlock(
      StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 8f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,7, 16,16,9)
    ));
    Registries.addBlock("steel_framed_window", ()->new EdWindowBlock(
      StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 8f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,7.5, 16,16,8.5)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("treated_wood_pole", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
      null
    ));
    Registries.addBlock("treated_wood_pole_head", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
      (EdStraightPoleBlock)Registries.getBlock("treated_wood_pole") // TREATED_WOOD_POLE
    ));
    Registries.addBlock("treated_wood_pole_support", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.5f, 5f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16),
      (EdStraightPoleBlock)Registries.getBlock("treated_wood_pole") // TREATED_WOOD_POLE
    ));
    Registries.addBlock("thin_steel_pole", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 11f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(6,6,0, 10,10,16),
      null
    ));
    Registries.addBlock("thin_steel_pole_head", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 11f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(6,6,0, 10,10,16),
      (EdStraightPoleBlock)Registries.getBlock("thin_steel_pole") // THIN_STEEL_POLE
    ));
    Registries.addBlock("thick_steel_pole", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 11f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(5,5,0, 11,11,16),
      null
    ));
    Registries.addBlock("thick_steel_pole_head", ()->new EdStraightPoleBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 11f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(5,5,0, 11,11,16),
      (EdStraightPoleBlock)Registries.getBlock("thin_steel_pole")
    ));
    Registries.addBlock("steel_double_t_support", ()->new EdHorizontalSupportBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.5f, 11f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB( 5,11,0, 11,16,16), // main beam
      Auxiliaries.getPixeledAABB(10,11,5, 16,16,11), // east beam (also for west 180deg)
      Auxiliaries.getPixeledAABB( 6, 0,6, 10,16,10), // down thin
      Auxiliaries.getPixeledAABB( 5, 0,5, 11,16,11)  // down thick
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("steel_mesh_fence", ()->new EdFenceBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
      1.5, 16, 0.25, 0, 16, 16
    ));
    Registries.addBlock("steel_mesh_fence_gate", ()->new EdDoubleGateBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,6.5, 16,16,9.5)
    ));
    Registries.addBlock("steel_railing", ()->new EdRailingBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,0,  0, 0,0),
      Auxiliaries.getPixeledAABB(0,0,0, 16,15.9,1)
    ));
    Registries.addBlock("steel_catwalk", ()->new EdCatwalkBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,0, 16, 2,16),
      Auxiliaries.getPixeledAABB(0,0,0, 16,15.9, 1),
      Registries.getBlock("steel_railing")
    ));
    Registries.addBlock("steel_catwalk_ta", ()->new EdCatwalkTopAlignedBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
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
        ),
        Auxiliaries.getUnionShape( // base with inset light
          Auxiliaries.getPixeledAABB( 0,14,0, 16,16,16)
        )
      },
      Registries.getBlock("iron_inset_light")
    ));
    Registries.addBlock("steel_catwalk_stairs", ()->new EdCatwalkStairsBlock(
      StandardBlocks.CFG_CUTOUT,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
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
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("sign_decor", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.3f, 1000f).sound(SoundType.WOOD).lightLevel((state)->1).noOcclusion(),
      Auxiliaries.getPixeledAABB(0,0,15.6, 16,16,16.0)
    ));
    Registries.addBlock("sign_hotwire", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_danger", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_radioactive", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_laser", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_caution", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_magichazard", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_firehazard", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_hotsurface", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_magneticfield", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_frost", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_exit", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(3,7,15.6, 13,13,16)
    ));
    Registries.addBlock("sign_defense", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));
    Registries.addBlock("sign_factoryarea", ()->new StandardBlocks.DirectedWaterLoggable(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_AI_PASSABLE,
      BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.2f, 1f).sound(SoundType.WOOD).noOcclusion(),
      Auxiliaries.getPixeledAABB(2,2,15.6, 14,14,16)
    ));

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("labeled_crate",
      ()->new EdLabeledCrate.LabeledCrateBlock(
        StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.3f, 32f).sound(SoundType.METAL).noOcclusion(),
        Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
      ),
      EdLabeledCrate.LabeledCrateTileEntity::new,
      EdLabeledCrate.LabeledCrateContainer::new
    );
    Registries.addBlock("metal_crafting_table",
      ()->new EdCraftingTable.CraftingTableBlock(
    StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(0,15,0, 16,16,16),
          Auxiliaries.getPixeledAABB(1, 0,1, 15,16,15)
        }
      ),
      EdCraftingTable.CraftingTableTileEntity::new,
      EdCraftingTable.CraftingTableUiContainer::new
      //MenuScreens.register(CT_TREATED_WOOD_CRAFTING_TABLE, EdCraftingTable.CraftingTableGui::new);
    );

    Registries.addBlock("small_lab_furnace",
      ()->new EdFurnace.FurnaceBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(1,0,1, 15, 1,15),
          Auxiliaries.getPixeledAABB(0,1,1, 16,16,16),
        }
      ),
      EdFurnace.FurnaceTileEntity::new,
      EdFurnace.FurnaceContainer::new
    );
    Registries.addBlock("small_electrical_furnace",
      ()->new EdElectricalFurnace.ElectricalFurnaceBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(0, 0,0, 16,11,16),
          Auxiliaries.getPixeledAABB(1,11,0, 15,12,16),
          Auxiliaries.getPixeledAABB(2,12,0, 14,13,16),
          Auxiliaries.getPixeledAABB(3,13,0, 13,14,16),
          Auxiliaries.getPixeledAABB(4,14,0, 12,16,16),
        }
      ),
      EdElectricalFurnace.ElectricalFurnaceTileEntity::new,
      EdElectricalFurnace.ElectricalFurnaceContainer::new
    );
    Registries.addBlock("factory_dropper",
      ()->new EdDropper.DropperBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        Auxiliaries.getPixeledAABB(0,0,1, 16,16,16)
      ),
      EdDropper.DropperTileEntity::new,
      EdDropper.DropperUiContainer::new
    );
    Registries.addBlock("factory_placer",
      ()->new EdPlacer.PlacerBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(0,0,2, 16,16,16),
          Auxiliaries.getPixeledAABB( 0,0,0, 1,16, 2),
          Auxiliaries.getPixeledAABB(15,0,0,16,16, 2)
        }
      ),
      EdPlacer.PlacerTileEntity::new,
      EdPlacer.PlacerContainer::new
    );
    Registries.addBlock("small_block_breaker",
      ()->new EdBreaker.BreakerBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(1,0,0, 15, 4, 7),
          Auxiliaries.getPixeledAABB(1,0,7, 15,12,16),
          Auxiliaries.getPixeledAABB(0,0,0, 1, 5, 4),
          Auxiliaries.getPixeledAABB(0,0,4, 1,12,16),
          Auxiliaries.getPixeledAABB(15,0,0, 16, 5, 4),
          Auxiliaries.getPixeledAABB(15,0,4, 16,12,16)
        }
      ),
      EdBreaker.BreakerTileEntity::new
    );
    Registries.addBlock("factory_hopper",
      ()->new EdHopper.HopperBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),()->{
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
      ),
      EdHopper.HopperTileEntity::new,
      EdHopper.HopperContainer::new
    );
    Registries.addBlock("small_waste_incinerator",
      ()->new EdWasteIncinerator.WasteIncineratorBlock(
        StandardBlocks.CFG_DEFAULT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL),
        Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
      ),
      EdWasteIncinerator.WasteIncineratorTileEntity::new,
      EdWasteIncinerator.WasteIncineratorContainer::new
    );
    Registries.addBlock("small_mineral_smelter",
      ()->new EdMineralSmelter.MineralSmelterBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        Auxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
      ),
      EdMineralSmelter.MineralSmelterTileEntity::new
    );
    Registries.addBlock("small_freezer",
      ()->new EdFreezer.FreezerBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 12f).sound(SoundType.METAL).noOcclusion(),
        Auxiliaries.getPixeledAABB(1.1,0,1.1, 14.9,16,14.9)
      ),
      EdFreezer.FreezerTileEntity::new
    );
    Registries.addBlock("small_solar_panel",
      ()->new EdSolarPanel.SolarPanelBlock(
        StandardBlocks.CFG_CUTOUT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(0,0,0, 16,2,16),
          Auxiliaries.getPixeledAABB(6,1.5,3, 10,10.5,13),
        }
      ),
      EdSolarPanel.SolarPanelTileEntity::new
    );
    Registries.addBlock("small_milking_machine",
      ()->new EdMilker.MilkerBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB( 1, 1,0, 15,14,10),
          Auxiliaries.getPixeledAABB( 0,14,0, 16,16,13),
          Auxiliaries.getPixeledAABB( 0, 0,0, 16, 1,13),
          Auxiliaries.getPixeledAABB( 0, 1,1,  1,14,11),
          Auxiliaries.getPixeledAABB(15, 1,1, 16,14,11)
        }
      ),
      EdMilker.MilkerTileEntity::new
    );
    Registries.addBlock("small_tree_cutter",
      ()->new EdTreeCutter.TreeCutterBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_HORIZIONTAL|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB( 0,0, 0, 16,3,16),
          Auxiliaries.getPixeledAABB( 0,3, 0,  3,8,16),
          Auxiliaries.getPixeledAABB( 3,7, 0,  5,8,16),
          Auxiliaries.getPixeledAABB(15,0, 0, 16,6,16),
          Auxiliaries.getPixeledAABB( 0,0,13, 16,8,16),
          Auxiliaries.getPixeledAABB( 5,6,12, 16,8,13),
        }
      ),
      EdTreeCutter.TreeCutterTileEntity::new
    );
    Registries.addBlock("straight_pipe_valve", ()->new EdPipeValve.PipeValveBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT|StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK,
      EdPipeValve.CFG_CHECK_VALVE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion(),
      new AABB[]{
        Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
        Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
        Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
        Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
      }
    ));
    Registries.addBlock("straight_pipe_valve_redstone", ()->new EdPipeValve.PipeValveBlock(
      StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
      EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE,
      BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion(),
      new AABB[]{
        Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
        Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
        Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
        Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
      }
    ));
    Registries.addBlock("straight_pipe_valve_redstone_analog",
      ()->new EdPipeValve.PipeValveBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_FACING_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        EdPipeValve.CFG_REDSTONE_CONTROLLED_VALVE|EdPipeValve.CFG_ANALOG_VALVE,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(2,2, 0, 14,14, 2),
          Auxiliaries.getPixeledAABB(2,2,14, 14,14,16),
          Auxiliaries.getPixeledAABB(3,3, 5, 13,13,11),
          Auxiliaries.getPixeledAABB(4,4, 2, 12,12,14),
        }
      )
    );
    Registries.addBlockEntityType("tet_straight_pipe_valve",
      EdPipeValve.PipeValveTileEntity::new,
      "straight_pipe_valve", "straight_pipe_valve_redstone", "straight_pipe_valve_redstone_analog"
    );
    Registries.addBlock("fluid_barrel",
      ()->new EdFluidBarrel.FluidBarrelBlock(
        StandardBlocks.CFG_CUTOUT|StandardBlocks.CFG_LOOK_PLACEMENT|StandardBlocks.CFG_OPPOSITE_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(2, 0,0, 14, 1,16),
          Auxiliaries.getPixeledAABB(1, 1,0, 15, 2,16),
          Auxiliaries.getPixeledAABB(0, 2,0, 16,14,16),
          Auxiliaries.getPixeledAABB(1,14,0, 15,15,16),
          Auxiliaries.getPixeledAABB(2,15,0, 14,16,16),
        }
      ),
      EdFluidBarrel.FluidBarrelTileEntity::new
    );
    Registries.addBlock("small_fluid_funnel",
      ()->new EdFluidFunnel.FluidFunnelBlock(
        StandardBlocks.CFG_CUTOUT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 10f).sound(SoundType.METAL).noOcclusion(),
        new AABB[]{
          Auxiliaries.getPixeledAABB(0, 0,0, 16,14,16),
          Auxiliaries.getPixeledAABB(1,14,1, 15,15,15),
          Auxiliaries.getPixeledAABB(0,15,0, 16,16,16)
        }
      ),
      EdFluidFunnel.FluidFunnelTileEntity::new
    );

    // -------------------------------------------------------------------------------------------------------------------

    if(Auxiliaries.isModLoaded("immersiveengineeringharddependent")) {
      //Registries.addBlock("halfslab_treated_wood", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD).strength(0.3f, 4f).sound(SoundType.WOOD).noOcclusion()
      //));
      //Registries.addBlock("halfslab_sheetmetal_iron", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
      //));
      //Registries.addBlock("halfslab_sheetmetal_steel", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
      //));
      //Registries.addBlock("halfslab_sheetmetal_copper", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
      //));
      //Registries.addBlock("halfslab_sheetmetal_gold", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
      //));
      //Registries.addBlock("halfslab_sheetmetal_aluminum", ()->new SlabSliceBlock(
      //  StandardBlocks.CFG_CUTOUT|DecorBlock.CFG_HARD_IE_DEPENDENT,
      //  BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0.3f, 8f).sound(SoundType.METAL).noOcclusion()
      //));
    }

    // -------------------------------------------------------------------------------------------------------------------

    Registries.addBlock("test_block",
        ()->new EdTestBlock.TestBlock(
        StandardBlocks.CFG_LOOK_PLACEMENT,
        BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL).strength(0f, 32000f).sound(SoundType.METAL),
        Auxiliaries.getPixeledAABB(0,0,0, 16,16,16)
      ),
      EdTestBlock.TestTileEntity::new
    );

  }

  public static void initItems()
  {
    Registries.addItem("metal_bar", ()->new EdItem((new Item.Properties()).tab(Registries.getCreativeModeTab())));
  }

  public static void initEntities()
  {
    Registries.addEntityType("et_chair", ()->
      EntityType.Builder.of(EdChair.EntityChair::new, MobCategory.MISC)
        .fireImmune().sized(1e-3f, 1e-3f).noSave()
        .setShouldReceiveVelocityUpdates(false).setUpdateInterval(4)
        .setCustomClientFactory(EdChair.EntityChair::customClientFactory)
        .build(new ResourceLocation(Auxiliaries.modid(), "et_chair").toString())
    );
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Registry wrappers
  //--------------------------------------------------------------------------------------------------------------------

  public static Block getBlock(String name)
  { return Registries.getBlock(name); }

  public static Item getItem(String name)
  { return Registries.getItem(name); }

  public static TagKey<Block> getBlockTagKey(String name)
  { return Registries.getBlockTagKey(name); }

  public static TagKey<Item> getItemTagKey(String name)
  { return Registries.getItemTagKey(name); }

  public static BlockEntityType<?> getBlockEntityTypeOfBlock(String block_name)
  { return Registries.getBlockEntityTypeOfBlock(block_name); }

  public static EntityType<?> getEntityType(String name)
  { return Registries.getEntityType(name); }

  public static MenuType<?> getMenuType(String block_name)
  { return Registries.getMenuTypeOfBlock(block_name); }

  @SuppressWarnings("deprecation")
  public static boolean isExperimentalBlock(Block block)
  { return (block instanceof StandardBlocks.IStandardBlock) && (((((StandardBlocks.IStandardBlock)block).config() & DecorBlock.CFG_EXPERIMENTAL))!=0); }

  @Nonnull
  public static List<Block> getRegisteredBlocks()
  { return Registries.getRegisteredBlocks(); }

  @Nonnull
  public static List<Item> getRegisteredItems()
  { return Registries.getRegisteredItems(); }

  //--------------------------------------------------------------------------------------------------------------------
  // Initialisation events
  //--------------------------------------------------------------------------------------------------------------------

  public static void registerBlocks(final RegistryEvent.Register<Block> event)
  {
    final boolean ie_available = Auxiliaries.isModLoaded("immersiveengineering");
    if(ie_available) Auxiliaries.logInfo("Immersive Engineering also installed ...");
    Registries.onBlockRegistry((rl, block)->event.getRegistry().register(block));
  }

  public static void registerItems(final RegistryEvent.Register<Item> event)
  { Registries.onItemRegistry((rl, item)->event.getRegistry().register(item)); }

  public static void registerBlockEntityTypes(final RegistryEvent.Register<BlockEntityType<?>> event)
  { Registries.onBlockEntityRegistry((rl, tet)->event.getRegistry().register(tet)); }

  public static void registerEntityTypes(final RegistryEvent.Register<EntityType<?>> event)
  { Registries.onEntityRegistry((rl, et)->event.getRegistry().register(et)); }

  public static void registerMenuTypes(final RegistryEvent.Register<MenuType<?>> event)
  { Registries.onMenuTypeRegistry((rl, ct)->event.getRegistry().register(ct)); }

  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("unchecked")
  public static void registerMenuGuis(final FMLClientSetupEvent event)
  {
    MenuScreens.register((MenuType<EdCraftingTable.CraftingTableUiContainer>)Registries.getMenuTypeOfBlock("metal_crafting_table"), EdCraftingTable.CraftingTableGui::new);
    MenuScreens.register((MenuType<EdLabeledCrate.LabeledCrateContainer>)Registries.getMenuTypeOfBlock("labeled_crate"), EdLabeledCrate.LabeledCrateGui::new);
    MenuScreens.register((MenuType<EdDropper.DropperUiContainer>)Registries.getMenuTypeOfBlock("factory_dropper"), EdDropper.DropperGui::new);
    MenuScreens.register((MenuType<EdPlacer.PlacerContainer>)Registries.getMenuTypeOfBlock("factory_placer"), EdPlacer.PlacerGui::new);
    MenuScreens.register((MenuType<EdHopper.HopperContainer>)Registries.getMenuTypeOfBlock("factory_hopper"), EdHopper.HopperGui::new);
    MenuScreens.register((MenuType<EdFurnace.FurnaceContainer>)Registries.getMenuTypeOfBlock("small_lab_furnace"), EdFurnace.FurnaceGui::new);
    MenuScreens.register((MenuType<EdElectricalFurnace.ElectricalFurnaceContainer>)Registries.getMenuTypeOfBlock("small_electrical_furnace"), EdElectricalFurnace.ElectricalFurnaceGui::new);
    MenuScreens.register((MenuType<EdWasteIncinerator.WasteIncineratorContainer>)Registries.getMenuTypeOfBlock("small_waste_incinerator"), EdWasteIncinerator.WasteIncineratorGui::new);
  }

  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("unchecked")
  public static void registerBlockEntityRenderers(final FMLClientSetupEvent event)
  {
    BlockEntityRenderers.register((BlockEntityType<EdCraftingTable.CraftingTableTileEntity>)Registries.getBlockEntityTypeOfBlock("metal_crafting_table"), wile.engineersdecor.detail.ModRenderers.CraftingTableTer::new);
    BlockEntityRenderers.register((BlockEntityType<EdLabeledCrate.LabeledCrateTileEntity>)Registries.getBlockEntityTypeOfBlock("labeled_crate"), wile.engineersdecor.detail.ModRenderers.DecorLabeledCrateTer::new);
  }

  @OnlyIn(Dist.CLIENT)
  public static void processContentClientSide(final FMLClientSetupEvent event)
  {
    // Block renderer selection
    for(Block block: Registries.getRegisteredBlocks()) {
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
    EntityRenderers.register(Registries.getEntityType("et_chair"), ModRenderers.InvisibleEntityRenderer::new);
  }

}
