/*
 * @file TreeCutting.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Simple tree cutting algorithm.
 */
package wile.engineersdecor.detail;

import net.minecraft.block.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import com.google.common.collect.ImmutableList;
import java.util.*;


public class TreeCutting
{
  private static org.apache.logging.log4j.Logger LOGGER = ModEngineersDecor.logger();

  public static boolean canChop(BlockState state)
  { return isLog(state); }

  // -------------------------------------------------------------------------------------------------------------------

  private static final List<Vector3i> hoffsets = ImmutableList.of(
    new Vector3i( 1,0, 0), new Vector3i( 1,0, 1), new Vector3i( 0,0, 1),
    new Vector3i(-1,0, 1), new Vector3i(-1,0, 0), new Vector3i(-1,0,-1),
    new Vector3i( 0,0,-1), new Vector3i( 1,0,-1)
  );

  private static boolean isLog(BlockState state)
  { return (state.getBlock().isIn(BlockTags.LOGS)) || (state.getBlock().getTags().contains(new ResourceLocation("minecraft","logs"))); }

  private static boolean isSameLog(BlockState a, BlockState b)
  { return (a.getBlock()==b.getBlock()); }

  private static boolean isLeaves(BlockState state)
  {
    if(state.getBlock() instanceof LeavesBlock) return true;
    if(state.getBlock().getTags().contains(new ResourceLocation("minecraft","leaves"))) return true;
    return false;
  }

  private static List<BlockPos> findBlocksAround(final World world, final BlockPos centerPos, final BlockState leaf_type_state, final Set<BlockPos> checked, int recursion_left)
  {
    ArrayList<BlockPos> to_decay = new ArrayList<BlockPos>();
    for(int y=-1; y<=1; ++y) {
      final BlockPos layer = centerPos.add(0,y,0);
      for(Vector3i v:hoffsets) {
        BlockPos pos = layer.add(v);
        if((!checked.contains(pos)) && (world.getBlockState(pos).getBlock()==leaf_type_state.getBlock())) {
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

  private static void breakBlock(World world, BlockPos pos)
  {
    Block.spawnDrops(world.getBlockState(pos), world, pos);
    world.setBlockState(pos, world.getFluidState(pos).getBlockState(), 1|2|8);
  }

  public static int chopTree(World world, BlockState broken_state, BlockPos startPos, int max_blocks_to_break, boolean without_target_block)
  {
    if(world.isRemote || !isLog(broken_state)) return 0;
    final long ymin = startPos.getY();
    final long max_leaf_distance = 8;
    Set<BlockPos> checked = new HashSet<BlockPos>();
    ArrayList<BlockPos> to_break = new ArrayList<BlockPos>();
    ArrayList<BlockPos> to_decay = new ArrayList<BlockPos>();
    checked.add(startPos);
    // Initial simple layer-up search of same logs. This forms the base corpus, and only leaves and
    // leaf-enclosed logs attached to this corpus may be broken/decayed.
    {
      LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
      LinkedList<BlockPos> upqueue = new LinkedList<BlockPos>();
      queue.add(startPos);
      int cutlevel = 0;
      int steps_left = 128;
      while(!queue.isEmpty() && (--steps_left >= 0)) {
        final BlockPos pos = queue.removeFirst();
        // Vertical search
        final BlockPos uppos = pos.up();
        final BlockState upstate = world.getBlockState(uppos);
        if(!checked.contains(uppos)) {
          checked.add(uppos);
          if(isSameLog(upstate, broken_state)) {
            // Up is log
            upqueue.add(uppos);
            to_break.add(uppos);
            steps_left = 128;
          } else {
            boolean isleaf = isLeaves(upstate);
            if(isleaf || world.isAirBlock(uppos) || (upstate.getBlock() instanceof VineBlock)) {
              if(isleaf) to_decay.add(uppos);
              // Up is air, check adjacent for diagonal up (e.g. Accacia)
              for(Vector3i v:hoffsets) {
                final BlockPos p = uppos.add(v);
                if(checked.contains(p)) continue;
                checked.add(p);
                final BlockState st = world.getBlockState(p);
                final Block bl = st.getBlock();
                if(isSameLog(st, broken_state)) {
                  queue.add(p);
                  to_break.add(p);
                } else if(isLeaves(st)) {
                  to_decay.add(p);
                }
              }
            }
          }
        }
        // Lateral search
        for(Vector3i v:hoffsets) {
          final BlockPos p = pos.add(v);
          if(checked.contains(p)) continue;
          checked.add(p);
          if(p.distanceSq(new BlockPos(startPos.getX(), p.getY(), startPos.getZ())) > (cutlevel > 2 ? 256 : 9)) continue;
          final BlockState st = world.getBlockState(p);
          final Block bl = st.getBlock();
          if(isSameLog(st, broken_state)) {
            queue.add(p);
            to_break.add(p);
          } else if(isLeaves(st)) {
            queue.add(p);
            to_decay.add(p);
          }
        }
        if(queue.isEmpty() && (!upqueue.isEmpty())) {
          queue = upqueue;
          upqueue = new LinkedList<BlockPos>();
          ++cutlevel;
        }
      }
    }
    {
      // Determine lose logs between the leafs
      for(BlockPos pos:to_decay) {
        int dist = 1;
        to_break.addAll(findBlocksAround(world, pos, broken_state, checked, dist));
      }
    }
    if(!to_decay.isEmpty()) {
      final BlockState leaf_type_state = world.getBlockState(to_decay.get(0));
      final ArrayList<BlockPos> leafs = to_decay;
      to_decay = new ArrayList<BlockPos>();
      for(BlockPos pos:leafs) {
        int dist = 3;
        to_decay.add(pos);
        to_decay.addAll(findBlocksAround(world, pos, leaf_type_state, checked, dist));
      }
    }
    if(without_target_block) {
      checked.remove(startPos);
    } else {
      to_break.add(startPos);
    }
    for(BlockPos pos:to_break) breakBlock(world, pos);
    for(BlockPos pos:to_decay) breakBlock(world, pos);
    {
      // And now the bill.
      return MathHelper.clamp(((to_break.size()*6/5)+(to_decay.size()/10)-1), 1, 65535);
    }
  }
}
