/*
 * @file ModBlocks.java
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
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import wile.engineersdecor.detail.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class ModBlocks
{
  public static final BlockDecorFull CLINKER_BRICK_BLOCK = new BlockDecorFull("clinker_brick_block", 0, Material.ROCK, 2f, 50f, SoundType.STONE);
  public static final BlockDecorStairs CLINKER_BRICK_STAIRS = new BlockDecorStairs("clinker_brick_stairs", CLINKER_BRICK_BLOCK.getDefaultState());
  public static final BlockDecorWall CLINKER_BRICK_WALL = new BlockDecorWall("clinker_brick_wall", BlockDecor.CFG_DEFAULT, Material.ROCK, 8f, 50f, SoundType.STONE);

  public static final BlockDecorFull SLAG_BRICK_BLOCK  = new BlockDecorFull("slag_brick_block", 0, Material.ROCK, 2f, 50f, SoundType.STONE);
  public static final BlockDecorStairs SLAG_BRICK_STAIRS = new BlockDecorStairs("slag_brick_stairs", SLAG_BRICK_BLOCK.getDefaultState());

  public static final BlockDecorFull REBAR_CONCRETE_BLOCK = new BlockDecorFull("rebar_concrete", 0, Material.ROCK, 8f, 2000f, SoundType.STONE);
  public static final BlockDecorStairs REBAR_CONCRETE_STAIRS = new BlockDecorStairs("rebar_concrete_stairs", REBAR_CONCRETE_BLOCK.getDefaultState());
  public static final BlockDecorWall REBAR_CONCRETE_WALL = new BlockDecorWall("rebar_concrete_wall", BlockDecor.CFG_DEFAULT, Material.ROCK, 8f, 2000f, SoundType.STONE);
  public static final BlockDecorFull REBAR_CONCRETE_TILE = new BlockDecorFull("rebar_concrete_tile", 0, Material.ROCK, 8f, 2000f, SoundType.STONE);
  public static final BlockDecorStairs REBAR_CONCRETE_TILE_STAIRS = new BlockDecorStairs("rebar_concrete_tile_stairs", REBAR_CONCRETE_TILE.getDefaultState());

  public static final BlockDecorWall CONCRETE_WALL = new BlockDecorWall("concrete_wall", BlockDecor.CFG_DEFAULT, Material.ROCK, 8f, 50f, SoundType.STONE);

  public static final BlockDecorLadder METAL_RUNG_LADDER = new BlockDecorLadder("metal_rung_ladder", 0, Material.IRON, 1.0f, 25f, SoundType.METAL);
  public static final BlockDecorLadder METAL_RUNG_STEPS = new BlockDecorLadder("metal_rung_steps", 0, Material.IRON, 1.0f, 25f, SoundType.METAL);
  public static final BlockDecorLadder TREATED_WOOD_LADDER = new BlockDecorLadder("treated_wood_ladder", 0, Material.WOOD, 1.0f, 15f, SoundType.WOOD);

  public static final BlockDecorGlassBlock PANZERGLASS_BLOCK = new BlockDecorGlassBlock("panzerglass_block", 0, Material.ROCK, 3f, 2000f, SoundType.GLASS);

  public static final BlockDecorDirected TREATED_WOOD_POLE = new BlockDecorDirected(
    "treated_wood_pole",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_FACING_PLACEMENT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD,
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  );

  public static final BlockDecor TREATED_WOOD_TABLE = new BlockDecor(
    "treated_wood_table",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD
  );
  public static final BlockDecorChair TREATED_WOOD_STOOL = new BlockDecorChair(
    "treated_wood_stool",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD,
    ModAuxiliaries.getPixeledAABB(4.1,0,4.1, 11.8,8.8,11.8)
  );

  public static final BlockDecorCraftingTable TREATED_WOOD_CRAFTING_TABLE = new BlockDecorCraftingTable(
    "treated_wood_crafting_table",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_LOOK_PLACEMENT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD,
    ModAuxiliaries.getPixeledAABB(0.0,0,0, 16,15.9,16)
  );

  public static final BlockDecorDirected INSET_LIGHT_IRON = new BlockDecorDirected(
    "iron_inset_light",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_OPPOSITE_PLACEMENT|(14<<BlockDecor.CFG_LIGHT_VALUE_SHIFT),
    Material.IRON, 0.3f, 15f, SoundType.METAL,
    ModAuxiliaries.getPixeledAABB(5.2,5.2,15.7, 10.8,10.8,16.0)
  );

  public static final BlockDecorDirected TREATED_WOOD_WINDOWSILL = new BlockDecorDirected(
    "treated_wood_windowsill",
    BlockDecor.CFG_CUTOUT|BlockDecor.CFG_HORIZIONTAL|BlockDecor.CFG_FACING_PLACEMENT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD,
    ModAuxiliaries.getPixeledAABB(0.5,15,10.5, 15.5,16,16)
  );

  public static final BlockDecorFull IRON_SHEET_ROOF_FULLBLOCK = new BlockDecorFull("iron_sheet_roof_block", 0, Material.IRON, 1.8f, 25f, SoundType.METAL);
  public static final BlockDecorStairs IRON_SHEET_ROOF = new BlockDecorStairs("iron_sheet_roof", IRON_SHEET_ROOF_FULLBLOCK.getDefaultState());

  private static final Block modBlocks[] = {
    TREATED_WOOD_CRAFTING_TABLE,
    CLINKER_BRICK_BLOCK,
    CLINKER_BRICK_STAIRS,
    CLINKER_BRICK_WALL,
    SLAG_BRICK_BLOCK,
    SLAG_BRICK_STAIRS,
    REBAR_CONCRETE_BLOCK,
    REBAR_CONCRETE_STAIRS,
    REBAR_CONCRETE_WALL,
    REBAR_CONCRETE_TILE,
    REBAR_CONCRETE_TILE_STAIRS,
    CONCRETE_WALL,
    PANZERGLASS_BLOCK,
    METAL_RUNG_LADDER,
    METAL_RUNG_STEPS,
    TREATED_WOOD_LADDER,
    TREATED_WOOD_POLE,
    TREATED_WOOD_TABLE,
    TREATED_WOOD_STOOL,
    TREATED_WOOD_WINDOWSILL,
    INSET_LIGHT_IRON,
  };

  private static final Block devBlocks[] = {
    IRON_SHEET_ROOF, // model looks not good enough yet
  };

  private static ArrayList<Block> registeredBlocks = new ArrayList<>();

  @Nonnull
  public static List getRegisteredBlocks()
  { return Collections.unmodifiableList(registeredBlocks); }

  // Invoked from CommonProxy.registerBlocks()
  public static final void registerBlocks(RegistryEvent.Register<Block> event)
  {
    // Config based registry selection
    ArrayList<Block> allBlocks = new ArrayList<>();
    Collections.addAll(allBlocks, modBlocks);
    //if(Loader.isModLoaded("immersiveengineering")){}
    if(ModConfig.zmisc.with_experimental) Collections.addAll(allBlocks, devBlocks);
    for(Block e:allBlocks) registeredBlocks.add(e);
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    ModEngineersDecor.logger.info("Registered " + Integer.toString(registeredBlocks.size()) + " blocks.");
    // TEs
    GameRegistry.registerTileEntity(BlockDecorCraftingTable.BTileEntity.class, new ResourceLocation(ModEngineersDecor.MODID, "te_crafting_table"));
  }

  // Invoked from ClientProxy.registerModels()
  @SideOnly(Side.CLIENT)
  public static final void initModels()
  {
    for(Block e:registeredBlocks) {
      ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(e), 0, new ModelResourceLocation(e.getRegistryName(), "inventory"));
    }
  }

  // Invoked from CommonProxy.registerItems()
  public static final void registerItemBlocks(RegistryEvent.Register<Item> event)
  {
    int n = 0;
    for(Block e:registeredBlocks) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      event.getRegistry().register(new ItemBlock(e).setRegistryName(rl));
      ++n;
    }
  }

}
