/*
 * @file EdChimneyBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block type for smoking chimneys.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.StandardBlocks;

import javax.annotation.Nullable;

public class EdChimneyBlock extends StandardBlocks.Cutout
{
  public static final IntegerProperty POWER = BlockStateProperties.POWER;

  public EdChimneyBlock(long config, BlockBehaviour.Properties properties, AABB aabb)
  { super(config, properties, aabb); }

  public EdChimneyBlock(long config, BlockBehaviour.Properties builder)
  {
    this(config, builder, new AABB(0,0,0,1,1,1));
    registerDefaultState(super.defaultBlockState().setValue(POWER, 0)); // no smoke in JEI
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(POWER); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    BlockState state = super.getStateForPlacement(context);
    if(state==null) return state;
    int p = context.getLevel().getBestNeighborSignal(context.getClickedPos());
    return state.setValue(POWER, p==0 ? 5 : p);
  }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
  { world.setBlock(pos, state.setValue(POWER, (state.getValue(POWER)+1) & 0xf), 1|2); return InteractionResult.sidedSuccess(world.isClientSide()); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
  {
    int p = world.getBestNeighborSignal(pos);
    if(p != state.getValue(POWER)) world.setBlock(pos, state.setValue(POWER, p), 2);
  }

  @Override
  public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
  { return false; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rnd)
  {
    if(state.getBlock() != this) return;
    final int p = state.getValue(POWER);
    if(p==0) return;
    int end = 1+rnd.nextInt(10) * p / 15;
    for(int i=0; i<end; ++i) {
      double rv = rnd.nextDouble() * p / 5;
      world.addParticle(
        (rv > 0.7 ? ParticleTypes.LARGE_SMOKE : (rv>0.4 ? ParticleTypes.SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE)),
        0.5+pos.getX()+(rnd.nextDouble()*0.2),
        0.9+pos.getY()+(rnd.nextDouble()*0.1),
        0.5+pos.getZ()+(rnd.nextDouble()*0.2),
        -0.02 + rnd.nextDouble()*0.04,
        +0.05 + rnd.nextDouble()*0.1,
        -0.02 + rnd.nextDouble()*0.04
      );
    }
  }

}
