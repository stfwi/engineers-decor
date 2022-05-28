/*
 * @file EdLadderBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Ladder block. The implementation is based on the vanilla
 * net.minecraft.block.BlockLadder. Minor changes to enable
 * later configuration (for block list based construction
 * time configuration), does not drop when the block behind
 * is broken, etc.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class EdLadderBlock extends LadderBlock implements StandardBlocks.IStandardBlock
{
  protected static final AABB EDLADDER_UNROTATED_AABB = Auxiliaries.getPixeledAABB(3, 0, 0, 13, 16, 3);
  protected static final VoxelShape EDLADDER_SOUTH_AABB =  Shapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.SOUTH, false));
  protected static final VoxelShape EDLADDER_EAST_AABB  = Shapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.EAST, false));
  protected static final VoxelShape EDLADDER_WEST_AABB  = Shapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.WEST, false));
  protected static final VoxelShape EDLADDER_NORTH_AABB = Shapes.create(Auxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, Direction.NORTH, false));
  private static boolean without_speed_boost_ = false;

  public static void on_config(boolean without_speed_boost)
  {
    without_speed_boost_ = without_speed_boost;
    ModConfig.log("Config ladder: without-speed-boost:" + without_speed_boost_);
  }

  public EdLadderBlock(long config, BlockBehaviour.Properties builder)
  { super(builder); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos)
  {
    return switch(state.getValue(FACING)) {
      case NORTH -> EDLADDER_NORTH_AABB;
      case SOUTH -> EDLADDER_SOUTH_AABB;
      case WEST -> EDLADDER_WEST_AABB;
      default -> EDLADDER_EAST_AABB;
    };
  }

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

  @Override
  public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity)
  { return true; }

  // Player update event, forwarded from the main mod instance.
  public static void onPlayerUpdateEvent(final Player player)
  {
    if((without_speed_boost_) || (player.isOnGround()) || (!player.onClimbable()) || (player.isSteppingCarefully()) || (player.isSpectator())) return;
    double lvy = player.getLookAngle().y;
    if(Math.abs(lvy) < 0.92) return;
    final BlockPos pos = player.blockPosition();
    final BlockState state = player.level.getBlockState(pos);
    final Block block = state.getBlock();
    if(!(block instanceof EdLadderBlock || block instanceof EdHatchBlock && state.getValue(EdHatchBlock.OPEN))) return;
    player.fallDistance = 0;
    if((player.getDeltaMovement().y() < 0) == (player.getLookAngle().y < 0)) {
      player.makeStuckInBlock(state, new Vec3(0.2, (lvy>0)?(3):(6), 0.2));
      if(Math.abs(player.getDeltaMovement().y()) > 0.1) {
        Vec3 vdiff = Vec3.atBottomCenterOf(pos).subtract(player.position()).scale(1);
        vdiff.add(Vec3.atBottomCenterOf(state.getValue(FACING).getNormal()).scale(0.5));
        vdiff = new Vec3(vdiff.x, player.getDeltaMovement().y, vdiff.z);
        player.setDeltaMovement(vdiff);
      }
    } else if(player.getLookAngle().y > 0) {
      player.makeStuckInBlock(state, new Vec3(1, 0.05, 1));
    }
  }

}
