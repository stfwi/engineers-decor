/*
 * @file BlockDecorWindow.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.state.StateContainer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockDecorWindow extends BlockDecorDirected implements IWaterLoggable
{
  public BlockDecorWindow(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config|CFG_WATERLOGGABLE, builder, unrotatedAABB); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.TRANSLUCENT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer)
  { return (layer==BlockRenderLayer.CUTOUT) || (layer==BlockRenderLayer.TRANSLUCENT); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
}
