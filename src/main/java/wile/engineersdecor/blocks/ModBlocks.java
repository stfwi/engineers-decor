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
  public static final BlockDecorFull CLINKER_BRICK_WALL = new BlockDecorFull("clinker_brick_block", 0, Material.ROCK, 2f, 50f, SoundType.STONE);
  public static final BlockDecorFull SLAG_BRICK_WALL  = new BlockDecorFull("slag_brick_block", 0, Material.ROCK, 2f, 50f, SoundType.STONE);
  public static final BlockDecorFull IRON_SHEET_ROOF_FULLBLOCK = new BlockDecorFull("iron_sheet_roof_block", 0, Material.IRON, 1.8f, 25f, SoundType.METAL);
  public static final BlockDecorFull REBAR_CONCRETE = new BlockDecorFull("rebar_concrete", 0, Material.ROCK, 8f, 2000f, SoundType.STONE);

  public static final BlockDecorLadder METAL_RUNG_LADDER = new BlockDecorLadder("metal_rung_ladder", 0, Material.IRON, 1.8f, 25f, SoundType.METAL);
  public static final BlockDecorLadder METAL_RUNG_STEPS = new BlockDecorLadder("metal_rung_steps", 0, Material.IRON, 1.8f, 25f, SoundType.METAL);
  public static final BlockDecorLadder TREATED_WOOD_LADDER = new BlockDecorLadder("treated_wood_ladder", 0, Material.WOOD, 1.0f, 15f, SoundType.WOOD);

  public static final BlockDecorStairs CLINKER_BRICK_STAIRS = new BlockDecorStairs("clinker_brick_stairs", CLINKER_BRICK_WALL.getDefaultState());
  public static final BlockDecorStairs SLAG_BRICK_STAIRS = new BlockDecorStairs("slag_brick_stairs", SLAG_BRICK_WALL.getDefaultState());
  public static final BlockDecorStairs IRON_SHEET_ROOF = new BlockDecorStairs("iron_sheet_roof", IRON_SHEET_ROOF_FULLBLOCK.getDefaultState());

  public static final BlockDecorDirected TREATED_WOOD_POLE = new BlockDecorDirected(
    "treated_wood_pole",
    BlockDecor.CFG_CUTOUT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD,
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  );
  public static final BlockDecor TREATED_WOOD_TABLE = new BlockDecor(
    "treated_wood_table",
    BlockDecor.CFG_CUTOUT,
    Material.WOOD, 1.0f, 15f, SoundType.WOOD
  );

  private static final Block modBlocks[] = {
    SLAG_BRICK_WALL,
    CLINKER_BRICK_WALL,
    METAL_RUNG_LADDER,
    METAL_RUNG_STEPS,
    TREATED_WOOD_LADDER,
    CLINKER_BRICK_STAIRS,
    SLAG_BRICK_STAIRS,
    TREATED_WOOD_POLE,
    TREATED_WOOD_TABLE,
    REBAR_CONCRETE,
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
    if(ModConfig.zmisc.with_experimental) Collections.addAll(allBlocks, devBlocks);
    for(Block e:allBlocks) registeredBlocks.add(e);
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    ModEngineersDecor.logger.info("Registered " + Integer.toString(registeredBlocks.size()) + " blocks.");
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
