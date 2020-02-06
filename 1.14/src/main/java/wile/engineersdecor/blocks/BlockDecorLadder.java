/*
 * @file BlockDecorLadder.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Ladder block. The implementation is based on the vanilla
 * net.minecraft.block.BlockLadder. Minor changes to enable
 * later configuration (for block list based construction
 * time configuration), does not drop when the block behind
 * is broken, etc.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.fluid.IFluidState;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;



public class BlockDecorLadder extends LadderBlock implements IDecorBlock
{
  protected static final AxisAlignedBB EDLADDER_UNROTATED_AABB = Auxiliaries.getPixeledAABB(3, 0, 0, 13, 16, 3);
  protected static final VoxelShape EDLADDER_SOUTH_AABB =  VoxelShapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.SOUTH, false));
  protected static final VoxelShape EDLADDER_EAST_AABB  = VoxelShapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.EAST, false));
  protected static final VoxelShape EDLADDER_WEST_AABB  = VoxelShapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.WEST, false));
  protected static final VoxelShape EDLADDER_NORTH_AABB = VoxelShapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.NORTH, false));
  private static boolean without_speed_boost_ = false;

  public static void on_config(boolean without_speed_boost)
  { without_speed_boost_ = without_speed_boost; }

  public BlockDecorLadder(long config, Block.Properties builder)
  { super(builder); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos)
  {
    switch ((Direction)state.get(FACING)) {
      case NORTH: return EDLADDER_NORTH_AABB;
      case SOUTH: return EDLADDER_SOUTH_AABB;
      case WEST: return EDLADDER_WEST_AABB;
      default: return EDLADDER_EAST_AABB;
    }
  }

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

  // Player update event, forwarded from the main mod instance.
  public static void onPlayerUpdateEvent(final PlayerEntity player)
  {
    if((without_speed_boost_) || (player.onGround) || (!player.isOnLadder()) || (player.isSneaking()) || (player.isSpectator())) return;
    double lvy = player.getLookVec().y;
    if(Math.abs(lvy) < 0.94) return;
    final BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
    final BlockState state = player.world.getBlockState(pos);
    if(!(state.getBlock() instanceof BlockDecorLadder)) return;
    player.fallDistance = 0;
    player.setMotionMultiplier(state, new Vec3d(0.2, (lvy>0)?(3):(6), 0.2));
  }

}
