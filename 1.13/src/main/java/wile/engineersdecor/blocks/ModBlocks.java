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

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class ModBlocks
{
  public static final BlockDecorFull CLINKER_BRICK_BLOCK = (BlockDecorFull)(new BlockDecorFull(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_block"));

  public static final BlockDecorStairs CLINKER_BRICK_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    CLINKER_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "clinker_brick_stairs"));

  public static final BlockDecorFull SLAG_BRICK_BLOCK = (BlockDecorFull)(new BlockDecorFull(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_block"));

  public static final BlockDecorStairs SLAG_BRICK_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    SLAG_BRICK_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "slag_brick_stairs"));

  public static final BlockDecorFull REBAR_CONCRETE_BLOCK = (BlockDecorFull)(new BlockDecorFull(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(6f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete"));

  public static final BlockDecorStairs REBAR_CONCRETE_STAIRS = (BlockDecorStairs)(new BlockDecorStairs(
    BlockDecor.CFG_DEFAULT,
    REBAR_CONCRETE_BLOCK.getDefaultState(),
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_stairs"));

  public static final BlockDecorWall REBAR_CONCRETE_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 2000f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "rebar_concrete_wall"));

  public static final BlockDecorWall CONCRETE_WALL = (BlockDecorWall)(new BlockDecorWall(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.ROCK, MaterialColor.STONE).hardnessAndResistance(2f, 50f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "concrete_wall"));

  public static final BlockDecorLadder METAL_RUNG_LADDER = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.8f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_ladder"));

  public static final BlockDecorLadder METAL_RUNG_STEPS = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.IRON, MaterialColor.IRON).hardnessAndResistance(1.8f, 25f).sound(SoundType.METAL)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "metal_rung_steps"));

  public static final BlockDecorLadder TREATED_WOOD_LADDER = (BlockDecorLadder)(new BlockDecorLadder(
    BlockDecor.CFG_DEFAULT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1.8f, 25f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_ladder"));

  public static final BlockDecor TREATED_WOOD_TABLE = (BlockDecor)(new BlockDecor(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1.0f, 15f).sound(SoundType.WOOD)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_table"));

  public static final BlockDecorDirected TREATED_WOOD_POLE = (BlockDecorDirected)(new BlockDecorDirected(
    BlockDecor.CFG_CUTOUT,
    Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(1.0f, 15f).sound(SoundType.WOOD),
    ModAuxiliaries.getPixeledAABB(5.8,5.8,0, 10.2,10.2,16)
  )).setRegistryName(new ResourceLocation(ModEngineersDecor.MODID, "treated_wood_pole"));

  //  public static final BlockDecorFull IRON_SHEET_ROOF_FULLBLOCK = new BlockDecorFull("iron_sheet_roof_block", 0, Material.IRON, 1.8f, 25f, SoundType.METAL);
  //  public static final BlockDecorStairs IRON_SHEET_ROOF = new BlockDecorStairs("iron_sheet_roof", IRON_SHEET_ROOF_FULLBLOCK.getDefaultState());

  private static final Block modBlocks[] = {
    CLINKER_BRICK_BLOCK,
    CLINKER_BRICK_STAIRS,
    SLAG_BRICK_BLOCK,
    SLAG_BRICK_STAIRS,
    REBAR_CONCRETE_BLOCK,
    REBAR_CONCRETE_STAIRS,
    REBAR_CONCRETE_WALL,

    METAL_RUNG_LADDER,
    METAL_RUNG_STEPS,
    TREATED_WOOD_LADDER,

    TREATED_WOOD_POLE,
    TREATED_WOOD_TABLE,
  };

  private static final Block ieDependentBlocks[] = {
    CONCRETE_WALL
  };

  private static final Block devBlocks[] = {
//    IRON_SHEET_ROOF, // model looks not good enough yet
  };

  private static ArrayList<Block> registeredBlocks = new ArrayList<>();

  @Nonnull
  public static List getRegisteredBlocks()
  { return Collections.unmodifiableList(registeredBlocks); }

  public static final void registerBlocks(RegistryEvent.Register<Block> event)
  {
    ArrayList<Block> allBlocks = new ArrayList<>();
    Collections.addAll(allBlocks, modBlocks);
    // @todo: find way to remove items from JEI, creative tab, etc instead of omitting registration.
    if(ModAuxiliaries.isModLoaded("immersiveengineering")) ModAuxiliaries.logInfo("Immersive Engineering also installed ...");
    Collections.addAll(allBlocks, ieDependentBlocks);
    // @todo: config not available yet, other registration control for experimental features needed.
    //if(ModConfig.MISC.with_experimental.get()) Collections.addAll(allBlocks, devBlocks);
    registeredBlocks.addAll(allBlocks);
    for(Block e:registeredBlocks) event.getRegistry().register(e);
    ModAuxiliaries.logInfo("Registered " + Integer.toString(registeredBlocks.size()) + " blocks.");
  }

  /**
   * Registers items for all blocks. Requires registerBlocks() event to be received first.
   */
  public static final void registerItemBlocks(RegistryEvent.Register<Item> event)
  {
    int n = 0;
    for(Block e:registeredBlocks) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      event.getRegistry().register(new ItemBlock(e, (new ItemBlock.Properties().group(ModEngineersDecor.ITEMGROUP))).setRegistryName(rl));
      ++n;
    }
  }

}
