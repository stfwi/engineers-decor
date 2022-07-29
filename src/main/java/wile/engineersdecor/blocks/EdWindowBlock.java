/*
 * @file EdWindowBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import wile.engineersdecor.libmc.StandardBlocks;

import javax.annotation.Nullable;
import java.util.Arrays;



public class EdWindowBlock extends StandardBlocks.DirectedWaterLoggable
{
  public EdWindowBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.TRANSLUCENT; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    Direction facing = context.getHorizontalDirection();
    if(Math.abs(context.getPlayer().getLookAngle().y) > 0.9) {
      facing = context.getNearestLookingDirection();
    } else {
      for(Direction f: Direction.values()) {
        BlockState st = context.getLevel().getBlockState(context.getClickedPos().relative(f));
        if(st.getBlock() == this) {
          facing = st.getValue(FACING);
          break;
        }
      }
    }
    return super.getStateForPlacement(context).setValue(FACING, facing);
  }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    if(player.getItemInHand(hand).getItem() != asItem()) return InteractionResult.PASS;
    final Direction facing = state.getValue(FACING);
    if(facing.getAxis() != hit.getDirection().getAxis()) return InteractionResult.PASS;
    Arrays.stream(Direction.orderedByNearest(player))
      .filter(d->d.getAxis() != facing.getAxis())
      .filter(d->world.getBlockState(pos.relative(d)).canBeReplaced((new DirectionalPlaceContext(world, pos.relative(d), facing.getOpposite(), player.getItemInHand(hand), facing))))
      .findFirst().ifPresent((d)->{
        BlockState st = defaultBlockState()
          .setValue(FACING, facing)
          .setValue(WATERLOGGED,world.getBlockState(pos.relative(d)).getFluidState().getType()==Fluids.WATER);
        world.setBlock(pos.relative(d), st, 1|2);
        world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
        player.getItemInHand(hand).shrink(1);
      }
    );
    return InteractionResult.sidedSuccess(world.isClientSide());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean useShapeForLightOcclusion(BlockState state)
  { return true; }

}
