/*
 * @file TreeCutting.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Simple tree cutting algorithm.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.init.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import com.google.common.collect.ImmutableList;
import java.util.*;


public class TreeCutting
{
  private static org.apache.logging.log4j.Logger LOGGER = ModEngineersDecor.logger;
  private static int max_log_tracing_steps = 128;
  private static int max_cutting_height = 128;
  private static int max_cutting_radius = 12;

  private static class Compat
  {
    enum ChoppingMethod { None, RootBlockBreaking }
    private static final HashMap<IBlockState, ChoppingMethod> choppable_states = new HashMap<IBlockState, ChoppingMethod>();
    public static long num_breaking_exceptions = 0;

    static void reload()
    {
      try {
        choppable_states.clear();
        if(ModAuxiliaries.isModLoaded("dynamictrees")) {
          ForgeRegistries.BLOCKS.getKeys().forEach((regname)->{
            if(!regname.getPath().contains("branch")) return;
            try {
              Block block = ForgeRegistries.BLOCKS.getValue(regname);
              IBlockState state = block.getDefaultState();
              for(IProperty<?> vaprop: state.getProperties().keySet()) {
                if(!("radius".equals(vaprop.getName())) || (vaprop.getValueClass() != Integer.class)) continue;
                @SuppressWarnings("unchecked")
                IProperty<Integer> prop = (IProperty<Integer>)vaprop;
                Integer max = ((Collection<Integer>)prop.getAllowedValues()).stream().max(Integer::compare).orElse(0);
                if(max<7) continue;
                for(int r=7; r<=max; ++r) choppable_states.put(state.withProperty(prop, r), ChoppingMethod.RootBlockBreaking);
              }
            } catch(Throwable e) {
              LOGGER.warn("Failed to register chopping for " + regname.toString());
              return;
            }
          });
        }
        LOGGER.info("Dynamic Trees chopping compat: " + choppable_states.size() + " choppable states found.");
        if(ModConfig.zmisc.with_experimental) {
          for(IBlockState b:choppable_states.keySet()) ModEngineersDecor.logger.info(" - dynamic tree state: " + b);
        }
      } catch(Throwable e) {
        choppable_states.clear();
        LOGGER.warn("Failed to determine choppings for dynamic trees compat, skipping that:" + e);
      }
    }

    private static boolean canChop(IBlockState state)
    { return choppable_states.containsKey(state); }

    private static int chop(World world, IBlockState state, BlockPos pos, int max_blocks_to_break, boolean without_target_block)
    {
      final int default_expense = 5;
      switch(choppable_states.getOrDefault(state, ChoppingMethod.None)) {
        case None:
          return 0;
        case RootBlockBreaking: {
          if(num_breaking_exceptions < 16) {
            try {
              world.setBlockState(pos, Blocks.AIR.getDefaultState(), 1);
              state.getBlock().breakBlock(world, pos, state);
            } catch(Throwable e) {
              if(++num_breaking_exceptions == 1) LOGGER.warn("Tree Chopper: There was an exception while trying to break a tree trunk ("+state.getBlock().getRegistryName()+"): " + e);
              if(num_breaking_exceptions == 16) LOGGER.warn("Tree Chopper: There were 16 exceptions in total trying to chop modded trees. Feature has been disabled.");
            }
          }
          return 5;
        }
        default:
          return 0;
      }
    }
  }

  private static final List<Vec3i> hoffsets = ImmutableList.of(
    new Vec3i( 1,0, 0), new Vec3i( 1,0, 1), new Vec3i( 0,0, 1),
    new Vec3i(-1,0, 1), new Vec3i(-1,0, 0), new Vec3i(-1,0,-1),
    new Vec3i( 0,0,-1), new Vec3i( 1,0,-1)
  );

  public static void reload()
  {
    Compat.reload();
    // later config, now keep IJ from suggesting that a private variable should be used.
    max_log_tracing_steps = 128;
    max_cutting_height = 128;
    max_cutting_radius = 12;
  }

  private static List<BlockPos> findBlocksAround(final World world, final BlockPos centerPos, final IBlockState leaf_type_state, final Set<BlockPos> checked, int recursion_left)
  {
    ArrayList<BlockPos> to_decay = new ArrayList<BlockPos>();
    for(int y=-1; y<=1; ++y) {
      final BlockPos layer = centerPos.add(0,y,0);
      for(Vec3i v:hoffsets) {
        BlockPos pos = layer.add(v);
        if((!checked.contains(pos)) && BlockCategories.isSameLeaves(leaf_type_state, world.getBlockState(pos))) {
          checked.add(pos);
          to_decay.add(pos);
          if(recursion_left > 0) {
            to_decay.addAll(findBlocksAround(world, pos, leaf_type_state, checked, recursion_left-1));
          }
        }
      }
    }
    return to_decay;
  }

  private static boolean too_far(BlockPos start, BlockPos pos)
  {
    int dy = pos.getY()-start.getY();
    if((dy < 0) || (dy>max_cutting_height)) return true;
    final int dx = Math.abs(pos.getX()-start.getX());
    final int dz = Math.abs(pos.getZ()-start.getZ());
    if(dy > max_cutting_radius) dy = max_cutting_radius;
    if((dx >= dy+4) || (dz >= dy+4)) return true;
    return false;
  }

  public static boolean canChop(IBlockState state)
  { return BlockCategories.isLog(state) || Compat.canChop(state); }

  /**
   * Chops a tree, returns the damage that the cutting tool shall take
   */
  public static int chopTree(World world, IBlockState broken_state, BlockPos startPos, int max_blocks_to_break, boolean without_target_block)
  {
    if((Compat.canChop(broken_state))) return Compat.chop(world, broken_state, startPos, max_blocks_to_break, without_target_block);
    if(!BlockCategories.isLog(broken_state)) return 0;
    Set<BlockPos> checked = new HashSet<BlockPos>();
    ArrayList<BlockPos> to_break = new ArrayList<BlockPos>();
    ArrayList<BlockPos> to_decay = new ArrayList<BlockPos>();
    {
      LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
      queue.add(startPos);
      int steps_left = max_log_tracing_steps;
      IBlockState tracked_leaves_state = null;
      while(!queue.isEmpty() && (--steps_left >= 0) && (to_break.size()<max_blocks_to_break)) {
        final BlockPos pos = queue.removeFirst();
        if(checked.contains(pos)) continue;
        checked.add(pos);
        if(too_far(startPos, pos)) continue;
        // Vertical search
        final BlockPos uppos = pos.up();
        final IBlockState upstate = world.getBlockState(uppos);
        if(BlockCategories.isSameLog(upstate, broken_state)) {
          queue.add(uppos);
          to_break.add(uppos);
          steps_left = max_log_tracing_steps;
        } else {
          boolean isleaf = BlockCategories.isLeaves(upstate);
          if(isleaf || world.isAirBlock(uppos) || (upstate.getBlock() instanceof BlockVine)) {
            if(isleaf) {
              if(tracked_leaves_state==null) {
                tracked_leaves_state=upstate;
                to_decay.add(uppos);
                queue.add(uppos);
              } else if(BlockCategories.isSameLeaves(upstate, tracked_leaves_state)) {
                to_decay.add(uppos);
                queue.add(uppos);
              } else {
                checked.add(uppos); // no block of interest
              }
            } else {
              checked.add(uppos);
            }
            // Up is air, check adjacent for diagonal up (e.g. Accacia)
            for(Vec3i v:hoffsets) {
              final BlockPos p = uppos.add(v);
              final IBlockState st = world.getBlockState(p);
              if(BlockCategories.isSameLog(st, broken_state)) {
                queue.add(p);
                to_break.add(p);
              } else if(BlockCategories.isLeaves(st)) {
                if(tracked_leaves_state==null) {
                  tracked_leaves_state=st;
                  to_decay.add(p);
                } else if(BlockCategories.isSameLeaves(st, tracked_leaves_state)) {
                  to_decay.add(p);
                } else {
                  checked.add(uppos);
                }
              } else {
                checked.add(uppos);
              }
            }
          }
        }
        // Lateral search
        for(Vec3i v:hoffsets) {
          final BlockPos p = pos.add(v);
          if(checked.contains(p)) continue;
          final IBlockState st = world.getBlockState(p);
          if(BlockCategories.isSameLog(st, broken_state)) {
            queue.add(p);
            to_break.add(p);
          } else if(BlockCategories.isLeaves(st)) {
            if((tracked_leaves_state==null) || (BlockCategories.isSameLeaves(st, tracked_leaves_state))) {
              to_decay.add(p);
            } else {
              checked.add(p);
            }
          } else {
            checked.add(p);
          }
        }
      }
    }
    // Determine lose logs between the leafs
    {
      for(BlockPos pos:to_decay) {
        int distance = 2;
        to_break.addAll(findBlocksAround(world, pos, broken_state, checked, distance));
      }
      if(!to_decay.isEmpty()) {
        final IBlockState leaf_type_state = world.getBlockState(to_decay.get(0));
        final ArrayList<BlockPos> leafs = to_decay;
        to_decay = new ArrayList<BlockPos>();
        for(BlockPos pos:leafs) {
          int dist = 3;
          to_decay.add(pos);
          to_decay.addAll(findBlocksAround(world, pos, leaf_type_state, checked, dist));
        }
      }
    }
    // Break blocks
    {
      if(without_target_block) {
        checked.remove(startPos);
      } else {
        to_break.add(startPos);
      }
      int num_broken = 0;
      Collections.reverse(to_break);
      for(BlockPos pos:to_break) {
        if(++num_broken > max_blocks_to_break) break;
        IBlockState state = world.getBlockState(pos);
        world.setBlockToAir(pos);
        state.getBlock().dropBlockAsItem(world, pos, state, 0);
      }
      for(BlockPos pos:to_decay) {
        if(++num_broken > max_blocks_to_break) break;
        IBlockState state = world.getBlockState(pos);
        world.setBlockToAir(pos);
        state.getBlock().dropBlockAsItem(world, pos, state, 0);
      }
    }
    // And the bill.
    {
      return MathHelper.clamp(((to_break.size()*6/5)+(to_decay.size()/10)-1), 1, 65535);
    }
  }
}
