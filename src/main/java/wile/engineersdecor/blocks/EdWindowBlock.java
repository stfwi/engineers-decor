/*
 * @file EdWindowBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;



public class EdWindowBlock extends DecorBlock.DirectedWaterLoggable implements IDecorBlock
{
  public EdWindowBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.TRANSLUCENT; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
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
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if(player.getItemInHand(hand).getItem() != asItem()) return ActionResultType.PASS;
    final Direction facing = state.getValue(FACING);
    if(facing.getAxis() != hit.getDirection().getAxis()) return ActionResultType.PASS;
    Arrays.stream(Direction.orderedByNearest(player))
      .filter(d->d.getAxis() != facing.getAxis())
      .filter(d->world.getBlockState(pos.relative(d)).canBeReplaced((new DirectionalPlaceContext(world, pos.relative(d), facing.getOpposite(), player.getItemInHand(hand), facing))))
      .findFirst().ifPresent((d)->{
        BlockState st = defaultBlockState()
          .setValue(FACING, facing)
          .setValue(WATERLOGGED,world.getBlockState(pos.relative(d)).getFluidState().getType()==Fluids.WATER);
        world.setBlock(pos.relative(d), st, 1|2);
        world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
        player.getItemInHand(hand).shrink(1);
      }
    );
    return ActionResultType.sidedSuccess(world.isClientSide());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean useShapeForLightOcclusion(BlockState state)
  { return true; }

}
