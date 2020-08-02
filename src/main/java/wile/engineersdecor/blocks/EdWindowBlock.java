/*
 * @file EdWindowBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.blocks.StandardBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;


public class EdWindowBlock extends StandardBlocks.DirectedWaterLoggable implements IDecorBlock
{
  public EdWindowBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.TRANSLUCENT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer)
  { return (layer==BlockRenderLayer.CUTOUT) || (layer==BlockRenderLayer.TRANSLUCENT); }

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
}
