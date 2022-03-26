/*
 * @file TreeCutting.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Simple tree cutting algorithm.
 */
package wile.engineersdecor.detail;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import java.util.*;


public class TreeCutting
{
  private static final org.apache.logging.log4j.Logger LOGGER = ModEngineersDecor.logger();

  public static boolean canChop(BlockState state)
  { return isLog(state); }

  // -------------------------------------------------------------------------------------------------------------------

  private static final List<Vec3i> hoffsets = ImmutableList.of(
    new Vec3i( 1,0, 0), new Vec3i( 1,0, 1), new Vec3i( 0,0, 1),
    new Vec3i(-1,0, 1), new Vec3i(-1,0, 0), new Vec3i(-1,0,-1),
    new Vec3i( 0,0,-1), new Vec3i( 1,0,-1)
  );

  private static boolean isLog(BlockState state)
  { return (state.is(BlockTags.LOGS)); }

  private static boolean isSameLog(BlockState a, BlockState b)
  { return (a.getBlock()==b.getBlock()); }

  private static boolean isLeaves(BlockState state)
  {
    if(state.getBlock() instanceof LeavesBlock) return true;
    if(state.is(BlockTags.LEAVES)) return true;
    return false;
  }

  private static List<BlockPos> findBlocksAround(final Level world, final BlockPos centerPos, final BlockState leaf_type_state, final Set<BlockPos> checked, int recursion_left)
  {
    ArrayList<BlockPos> to_decay = new ArrayList<>();
    for(int y=-1; y<=1; ++y) {
      final BlockPos layer = centerPos.offset(0,y,0);
      for(Vec3i v:hoffsets) {
        BlockPos pos = layer.offset(v);
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

  private static void breakBlock(Level world, BlockPos pos)
  {
    Block.dropResources(world.getBlockState(pos), world, pos);
    world.setBlock(pos, world.getFluidState(pos).createLegacyBlock(), 1|2|8);
  }

  public static int chopTree(Level world, BlockState broken_state, BlockPos startPos, int max_blocks_to_break, boolean without_target_block)
  {
    if(world.isClientSide || !isLog(broken_state)) return 0;
    final long ymin = startPos.getY();
    final long max_leaf_distance = 8;
    Set<BlockPos> checked = new HashSet<>();
    ArrayList<BlockPos> to_break = new ArrayList<>();
    ArrayList<BlockPos> to_decay = new ArrayList<>();
    checked.add(startPos);
    // Initial simple layer-up search of same logs. This forms the base corpus, and only leaves and
    // leaf-enclosed logs attached to this corpus may be broken/decayed.
    {
      LinkedList<BlockPos> queue = new LinkedList<>();
      LinkedList<BlockPos> upqueue = new LinkedList<>();
      queue.add(startPos);
      int cutlevel = 0;
      int steps_left = 128;
      while(!queue.isEmpty() && (--steps_left >= 0)) {
        final BlockPos pos = queue.removeFirst();
        // Vertical search
        final BlockPos uppos = pos.above();
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
            if(isleaf || world.isEmptyBlock(uppos) || (upstate.getBlock() instanceof VineBlock)) {
              if(isleaf) to_decay.add(uppos);
              // Up is air, check adjacent for diagonal up (e.g. Accacia)
              for(Vec3i v:hoffsets) {
                final BlockPos p = uppos.offset(v);
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
        for(Vec3i v:hoffsets) {
          final BlockPos p = pos.offset(v);
          if(checked.contains(p)) continue;
          checked.add(p);
          if(p.distSqr(new BlockPos(startPos.getX(), p.getY(), startPos.getZ())) > (cutlevel > 2 ? 256 : 9)) continue;
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
          upqueue = new LinkedList<>();
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
      to_decay = new ArrayList<>();
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
      return Mth.clamp(((to_break.size()*6/5)+(to_decay.size()/10)-1), 1, 65535);
    }
  }
}
