/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class. Explicitly overrides some
 * `Block` methods to return faster due to exclusive block properties.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.Block;
import net.minecraft.block.material.PushReaction;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorGlassBlock extends BlockDecor
{
  public BlockDecorGlassBlock(long config, Block.Properties properties)
  { super(config, properties); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.TRANSLUCENT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side)
  { return (adjacentBlockState.getBlock()==this) ? true : super.isSideInvisible(state, adjacentBlockState, side); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> entityType)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.NORMAL; }

}
