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

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class EdGlassBlock extends StainedGlassBlock implements StandardBlocks.IStandardBlock
{
  public EdGlassBlock(long config, BlockBehaviour.Properties properties)
  { super(DyeColor.BLACK, properties); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public StandardBlocks.IStandardBlock.RenderTypeHint getRenderTypeHint()
  { return StandardBlocks.IStandardBlock.RenderTypeHint.TRANSLUCENT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side)
  { return (adjacentBlockState.getBlock()==this) || (super.skipRendering(state, adjacentBlockState, side)); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

}
