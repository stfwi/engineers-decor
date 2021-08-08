/*
 * @file EdStraightPoleBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.Arrays;


public class EdStraightPoleBlock extends StandardBlocks.DirectedWaterLoggable
{
  private final EdStraightPoleBlock default_pole;

  public EdStraightPoleBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB, @Nullable EdStraightPoleBlock defaultPole)
  { super(config, builder, unrotatedAABB); default_pole=(defaultPole==null) ? (this) : (defaultPole); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    Direction facing = context.getClickedFace();
    BlockState state = super.getStateForPlacement(context).setValue(FACING, facing);
    if((config & DecorBlock.CFG_FLIP_PLACEMENT_IF_SAME) != 0) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if(world.getBlockState(pos.relative(facing.getOpposite())).getBlock() instanceof EdStraightPoleBlock) {
        state = state.setValue(FACING, state.getValue(FACING).getOpposite());
      }
    }
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    if((hit.getDirection().getAxis() == state.getValue(FACING).getAxis())) return InteractionResult.PASS;
    final ItemStack held_stack = player.getItemInHand(hand);
    if((held_stack.isEmpty()) || (!(held_stack.getItem() instanceof BlockItem))) return InteractionResult.PASS;
    if(!(((BlockItem)(held_stack.getItem())).getBlock() instanceof EdStraightPoleBlock)) return InteractionResult.PASS;
    if(held_stack.getItem() != default_pole.asItem()) return InteractionResult.sidedSuccess(world.isClientSide());
    final Block held_block = ((BlockItem)(held_stack.getItem())).getBlock();
    final Direction block_direction = state.getValue(FACING);
    final Vec3 block_vec = Vec3.atLowerCornerOf(state.getValue(FACING).getNormal());
    final double colinearity = 1.0-block_vec.cross(player.getLookAngle()).length();
    final Direction placement_direction = Arrays.stream(Direction.orderedByNearest(player)).filter(d->d.getAxis()==block_direction.getAxis()).findFirst().orElse(Direction.NORTH);
    final BlockPos adjacent_pos = pos.relative(placement_direction);
    final BlockState adjacent = world.getBlockState(adjacent_pos);
    final BlockPlaceContext ctx = new DirectionalPlaceContext(world, adjacent_pos, placement_direction, player.getItemInHand(hand), placement_direction.getOpposite());
    if(!adjacent.canBeReplaced(ctx)) return InteractionResult.sidedSuccess(world.isClientSide());
    final BlockState new_state = held_block.getStateForPlacement(ctx);
    if(new_state == null) return InteractionResult.FAIL;
    if(!world.setBlock(adjacent_pos, new_state, 1|2)) return InteractionResult.FAIL;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      held_stack.shrink(1);
      Inventories.setItemInPlayerHand(player, hand, held_stack);
    }
    return InteractionResult.sidedSuccess(world.isClientSide());
  }

}
