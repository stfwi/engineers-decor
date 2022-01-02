/*
 * @file StandardStairsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stairs and roof blocks, almost entirely based on vanilla stairs.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class StandardStairsBlock extends StairBlock implements StandardBlocks.IStandardBlock
{
  private final long config;

  public StandardStairsBlock(long config, BlockState state, BlockBehaviour.Properties properties)
  { super(()->state, properties); this.config = config; }

  public StandardStairsBlock(long config, java.util.function.Supplier<BlockState> state, BlockBehaviour.Properties properties)
  { super(state, properties); this.config = config; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPistonPushReaction(BlockState state)
  { return PushReaction.NORMAL; }
}
