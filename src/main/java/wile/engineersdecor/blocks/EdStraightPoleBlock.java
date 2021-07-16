/*
 * @file EdStraightPoleBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.Arrays;


import net.minecraft.block.AbstractBlock;

public class EdStraightPoleBlock extends DecorBlock.DirectedWaterLoggable implements IDecorBlock
{
  private final EdStraightPoleBlock default_pole;

  public EdStraightPoleBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB unrotatedAABB, @Nullable EdStraightPoleBlock defaultPole)
  { super(config, builder, unrotatedAABB); default_pole=(defaultPole==null) ? (this) : (defaultPole); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getClickedFace();
    BlockState state = super.getStateForPlacement(context).setValue(FACING, facing);
    if((config & DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME) != 0) {
      World world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if(world.getBlockState(pos.relative(facing.getOpposite())).getBlock() instanceof EdStraightPoleBlock) {
        state = state.setValue(FACING, state.getValue(FACING).getOpposite());
      }
    }
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if((hit.getDirection().getAxis() == state.getValue(FACING).getAxis())) return ActionResultType.PASS;
    final ItemStack held_stack = player.getItemInHand(hand);
    if((held_stack.isEmpty()) || (!(held_stack.getItem() instanceof BlockItem))) return ActionResultType.PASS;
    if(!(((BlockItem)(held_stack.getItem())).getBlock() instanceof EdStraightPoleBlock)) return ActionResultType.PASS;
    if(held_stack.getItem() != default_pole.asItem()) return ActionResultType.sidedSuccess(world.isClientSide());
    final Block held_block = ((BlockItem)(held_stack.getItem())).getBlock();
    final Direction block_direction = state.getValue(FACING);
    final Vector3d block_vec = Vector3d.atLowerCornerOf(state.getValue(FACING).getNormal());
    final double colinearity = 1.0-block_vec.cross(player.getLookAngle()).length();
    final Direction placement_direction = Arrays.stream(Direction.orderedByNearest(player)).filter(d->d.getAxis()==block_direction.getAxis()).findFirst().orElse(Direction.NORTH);
    final BlockPos adjacent_pos = pos.relative(placement_direction);
    final BlockState adjacent = world.getBlockState(adjacent_pos);
    final BlockItemUseContext ctx = new DirectionalPlaceContext(world, adjacent_pos, placement_direction, player.getItemInHand(hand), placement_direction.getOpposite());
    if(!adjacent.canBeReplaced(ctx)) return ActionResultType.sidedSuccess(world.isClientSide());
    final BlockState new_state = held_block.getStateForPlacement(ctx);
    if(new_state == null) return ActionResultType.FAIL;
    if(!world.setBlock(adjacent_pos, new_state, 1|2)) return ActionResultType.FAIL;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      held_stack.shrink(1);
      Inventories.setItemInPlayerHand(player, hand, held_stack);
    }
    return ActionResultType.sidedSuccess(world.isClientSide());
  }

}
