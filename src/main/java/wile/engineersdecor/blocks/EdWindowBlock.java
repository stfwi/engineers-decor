/*
 * @file EdWindowBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
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
  public EdWindowBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.TRANSLUCENT; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getPlacementHorizontalFacing();
    if(Math.abs(context.getPlayer().getLookVec().y) > 0.9) {
      facing = context.getNearestLookingDirection();
    } else {
      for(Direction f: Direction.values()) {
        BlockState st = context.getWorld().getBlockState(context.getPos().offset(f));
        if(st.getBlock() == this) {
          facing = st.get(FACING);
          break;
        }
      }
    }
    return super.getStateForPlacement(context).with(FACING, facing);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if(player.getHeldItem(hand).getItem() != asItem()) return ActionResultType.PASS;
    final Direction facing = state.get(FACING);
    if(facing.getAxis() != hit.getFace().getAxis()) return ActionResultType.PASS;
    Arrays.stream(Direction.getFacingDirections(player))
      .filter(d->d.getAxis() != facing.getAxis())
      .filter(d->world.getBlockState(pos.offset(d)).isReplaceable((new DirectionalPlaceContext(world, pos.offset(d), facing.getOpposite(), player.getHeldItem(hand), facing))))
      .findFirst().ifPresent((d)->{
        BlockState st = getDefaultState()
          .with(FACING, facing)
          .with(WATERLOGGED,world.getBlockState(pos.offset(d)).getFluidState().getFluid()==Fluids.WATER);
        world.setBlockState(pos.offset(d), st, 1|2);
        world.playSound(player, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
      }
    );
    return ActionResultType.func_233537_a_(world.isRemote());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isTransparent(BlockState state)
  { return true; }

}
