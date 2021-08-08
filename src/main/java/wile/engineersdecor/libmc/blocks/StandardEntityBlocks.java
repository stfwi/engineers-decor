/*
 * @file StandardEntityBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for blocks with block entities.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;


public class StandardEntityBlocks
{
  public interface IStandardEntityBlock<ET extends StandardBlockEntity> extends EntityBlock
  {
    @Nullable
    BlockEntityType<ET> getBlockEntityType();

    @Override
    @Nullable
    default BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    { return (getBlockEntityType()==null) ? null : getBlockEntityType().create(pos, state); }

    @Override
    @Nullable
    default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> te_type)
    { return (world.isClientSide) ? (null) : ((Level w, BlockPos p, BlockState s, T te) -> ((StandardBlockEntity)te).tick()); } // To be evaluated if

    @Override
    @Nullable
    default <T extends BlockEntity> GameEventListener getListener(Level world, T te)
    { return null; }

    default InteractionResult useOpenGui(BlockState state, Level world, BlockPos pos, Player player)
    {
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof MenuProvider) || ((player instanceof FakePlayer))) return InteractionResult.FAIL;
      player.openMenu((MenuProvider)te);
      return InteractionResult.CONSUME;
    }
  }

  public static abstract class StandardBlockEntity extends BlockEntity
  {
    public StandardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    { super(type, pos, state); }

    public void tick()
    {}
  }

}
