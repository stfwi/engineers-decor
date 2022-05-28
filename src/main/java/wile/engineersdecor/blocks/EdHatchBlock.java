/*
 * @file EdFloorGratingBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class EdHatchBlock extends StandardBlocks.HorizontalWaterLoggable
{
  public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
  public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
  protected final List<VoxelShape> vshapes_open;

  public EdHatchBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABBClosed, final AABB unrotatedAABBOpen)
  {
    super(config, builder, unrotatedAABBClosed); vshapes_open = makeHorizontalShapeLookup(new AABB[]{unrotatedAABBOpen});
    registerDefaultState(super.defaultBlockState().setValue(OPEN, false).setValue(POWERED, false));
  }

  public EdHatchBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABBsClosed, final AABB[] unrotatedAABBsOpen)
  { super(config, builder, unrotatedAABBsClosed); vshapes_open = makeHorizontalShapeLookup(unrotatedAABBsOpen); }

  protected static List<VoxelShape> makeHorizontalShapeLookup(final AABB[] unrotatedAABBs)
  {
    return List.of(
      Shapes.block(),
      Shapes.block(),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.NORTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.SOUTH, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.WEST, true)),
      Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.EAST, true))
    );
  }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
  { return state.getValue(OPEN) ? vshapes_open.get((state.getValue(HORIZONTAL_FACING)).get3DDataValue()) : super.getShape(state, source, pos, selectionContext); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, source, pos, selectionContext); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return state.getValue(OPEN); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type)
  { return !state.getValue(OPEN); }

  @Override
  public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity)
  {
    if(!state.getValue(OPEN)) return false;
    {
      final BlockState up_state = world.getBlockState(pos.above());
      if(up_state.is(this) && (up_state.getValue(OPEN))) return true;
      if(up_state.isLadder(world, pos.above(), entity)) return true;
    }
    {
      final BlockState down_state = world.getBlockState(pos.below());
      if(down_state.is(this) && (down_state.getValue(OPEN))) return true;
      if(down_state.isLadder(world, pos.below(), entity)) return true;
    }
    return false;
  }

  @Override
  public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(OPEN, POWERED); }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
  {
    if(world.isClientSide()) return InteractionResult.SUCCESS;
    boolean open = !state.getValue(OPEN);
    world.setBlock(pos, state.setValue(OPEN, open), 1|2);
    world.playSound(null, pos, open?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.4f);
    return InteractionResult.CONSUME;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    if((world.isClientSide) || (!(state.getBlock() instanceof EdHatchBlock))) return;
    boolean powered = world.hasNeighborSignal(pos);
    if(powered == state.getValue(POWERED)) return;
    if(powered != state.getValue(OPEN)) world.playSound(null, pos, powered?SoundEvents.IRON_DOOR_OPEN:SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.4f);
    world.setBlock(pos, state.setValue(OPEN, powered).setValue(POWERED, powered), 1|2);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean useShapeForLightOcclusion(BlockState state) {
    return !state.getValue(OPEN);
  }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
    return state.getValue(OPEN) ? Shapes.empty() : super.getOcclusionShape(state, world, pos);
  }

  @Override
  public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)
  {
    if((!state.getValue(OPEN)) || (!(entity instanceof final Player player))) return;
    if(player.isSteppingCarefully()) return;
    if(entity.getLookAngle().y() > -0.75) return;
    if(player.getDirection() != state.getValue(HORIZONTAL_FACING)) return;
    Vec3 ppos = player.position();
    Vec3 centre = Vec3.atBottomCenterOf(pos);
    Vec3 v = centre.subtract(ppos);
    if(ppos.y() < (centre.y()-0.1) || (v.lengthSqr() > 0.3)) return;
    v = v.scale(0.2);
    player.push(v.x, -0.1, v.z);
  }
}
