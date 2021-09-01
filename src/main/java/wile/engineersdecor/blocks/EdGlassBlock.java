/*
 * @file EdGlassBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class. Explicitly overrides some
 * `Block` methods to return faster due to exclusive block properties.
 */
package wile.engineersdecor.blocks;

import net.minecraft.world.IBlockReader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class EdGlassBlock extends StainedGlassBlock implements IDecorBlock
{
  public EdGlassBlock(long config, AbstractBlock.Properties properties)
  { super(DyeColor.BLACK, properties); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.TRANSLUCENT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side)
  { return (adjacentBlockState.getBlock()==this) ? true : super.skipRendering(state, adjacentBlockState, side); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

}
