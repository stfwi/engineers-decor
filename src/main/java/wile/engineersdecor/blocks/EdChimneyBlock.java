/*
 * @file EdChimneyBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block type for smoking chimneys.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Random;

public class EdChimneyBlock extends DecorBlock.Normal implements IDecorBlock
{
  public static final IntegerProperty POWER = BlockStateProperties.POWER_0_15;

  public EdChimneyBlock(long config, Block.Properties properties, AxisAlignedBB aabb)
  { super(config, properties, aabb); }

  public EdChimneyBlock(long config, Block.Properties builder)
  {
    this(config, builder, new AxisAlignedBB(0,0,0,1,1,1));
    setDefaultState(super.getDefaultState().with(POWER, 0)); // no smoke in JEI
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(POWER); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    int p = context.getWorld().getRedstonePowerFromNeighbors(context.getPos());
    return super.getStateForPlacement(context).with(POWER, p==0 ? 5 : p);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  { world.setBlockState(pos, state.with(POWER, (state.get(POWER)+1) & 0xf), 1|2); return ActionResultType.SUCCESS; }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
  {
    int p = world.getRedstonePowerFromNeighbors(pos);
    if(p != state.get(POWER)) world.setBlockState(pos, state.with(POWER, p), 2);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
  {
    if(state.getBlock() != this) return;
    final int p = state.get(POWER);
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
