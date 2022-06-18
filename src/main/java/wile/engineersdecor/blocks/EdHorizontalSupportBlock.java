/*
 * @file EdHorizontalSupportBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Horizontal ceiling support. Symmetric x axis, fixed in
 * xz plane, therefore boolean placement state.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;



public class EdHorizontalSupportBlock extends StandardBlocks.WaterLoggable
{
  public static final BooleanProperty EASTWEST  = BooleanProperty.create("eastwest");
  public static final BooleanProperty LEFTBEAM  = BooleanProperty.create("leftbeam");
  public static final BooleanProperty RIGHTBEAM = BooleanProperty.create("rightbeam");
  public static final IntegerProperty DOWNCONNECT = IntegerProperty.create("downconnect", 0, 2);
  protected final Map<BlockState, VoxelShape> AABBs;

  public EdHorizontalSupportBlock(long config, BlockBehaviour.Properties builder, final AABB mainBeamAABB, final AABB eastBeamAABB, final AABB thinDownBeamAABB, final AABB thickDownBeamAABB)
  {
    super(config|StandardBlocks.CFG_HORIZIONTAL, builder);
    Map<BlockState, VoxelShape> aabbs = new HashMap<>();
    for(boolean eastwest:EASTWEST.getPossibleValues()) {
      for(boolean leftbeam:LEFTBEAM.getPossibleValues()) {
        for(boolean rightbeam:RIGHTBEAM.getPossibleValues()) {
          for(int downconnect:DOWNCONNECT.getPossibleValues()) {
            final BlockState state = defaultBlockState().setValue(EASTWEST, eastwest).setValue(LEFTBEAM, leftbeam).setValue(RIGHTBEAM, rightbeam).setValue(DOWNCONNECT, downconnect);
            VoxelShape shape = Shapes.create(Auxiliaries.getRotatedAABB(mainBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true));
            if(rightbeam) shape = Shapes.joinUnoptimized(shape, Shapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true)), BooleanOp.OR);
            if(leftbeam) shape = Shapes.joinUnoptimized(shape, Shapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.WEST:Direction.SOUTH, true)), BooleanOp.OR);
            if(downconnect==1) shape = Shapes.joinUnoptimized(shape, Shapes.create(thinDownBeamAABB), BooleanOp.OR);
            if(downconnect==2) shape = Shapes.joinUnoptimized(shape, Shapes.create(thickDownBeamAABB), BooleanOp.OR);
            aabbs.put(state.setValue(WATERLOGGED, false), shape);
            aabbs.put(state.setValue(WATERLOGGED, true), shape);
          }
        }
      }
    }
    AABBs = aabbs;
  }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
  { return AABBs.get(state); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(EASTWEST, RIGHTBEAM, LEFTBEAM, DOWNCONNECT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  { return temp_block_update_until_better(super.getStateForPlacement(context).setValue(EASTWEST, context.getHorizontalDirection().getAxis()==Direction.Axis.X), context.getLevel(), context.getClickedPos()); }

  private BlockState temp_block_update_until_better(BlockState state, LevelAccessor world, BlockPos pos)
  {
    boolean ew = state.getValue(EASTWEST);
    final BlockState rstate = world.getBlockState((!ew) ? (pos.east()) : (pos.south()) );
    final BlockState lstate = world.getBlockState((!ew) ? (pos.west()) : (pos.north()) );
    final BlockState dstate = world.getBlockState(pos.below());
    int down_connector = 0;
    if((dstate.getBlock() instanceof final EdStraightPoleBlock pole)) {
      final Direction dfacing = dstate.getValue(EdStraightPoleBlock.FACING);
      if((dfacing.getAxis() == Direction.Axis.Y)) {
        if((pole==ModContent.getBlock("thick_steel_pole")) || ((pole==ModContent.getBlock("thick_steel_pole_head")) && (dfacing==Direction.UP))) {
          down_connector = 2;
        } else if((pole==ModContent.getBlock("thin_steel_pole")) || ((pole==ModContent.getBlock("thin_steel_pole_head")) && (dfacing==Direction.UP))) {
          down_connector = 1;
        }
      }
    }
    return state.setValue(RIGHTBEAM, (rstate.getBlock()==this) && (rstate.getValue(EASTWEST) != ew))
      .setValue(LEFTBEAM , (lstate.getBlock()==this) && (lstate.getValue(EASTWEST) != ew))
      .setValue(DOWNCONNECT , down_connector);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  { return temp_block_update_until_better(state, world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    ItemStack held_stack = player.getItemInHand(hand);
    if((held_stack.isEmpty()) || (held_stack.getItem() != this.asItem())) return InteractionResult.PASS;
    if(!(hit.getDirection().getAxis().isVertical())) return InteractionResult.PASS;
    final Direction placement_direction = player.getDirection();
    final BlockPos adjacent_pos = pos.relative(placement_direction);
    final BlockState adjacent = world.getBlockState(adjacent_pos);
    final BlockPlaceContext ctx = new DirectionalPlaceContext(world, adjacent_pos, placement_direction, player.getItemInHand(hand), placement_direction.getOpposite());
    if(!adjacent.canBeReplaced(ctx)) return InteractionResult.sidedSuccess(world.isClientSide());
    final BlockState new_state = getStateForPlacement(ctx);
    if(new_state == null) return InteractionResult.FAIL;
    if(!world.setBlock(adjacent_pos, new_state, 1|2)) return InteractionResult.FAIL;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      held_stack.shrink(1);
      Inventories.setItemInPlayerHand(player, hand, held_stack);
    }
    return InteractionResult.sidedSuccess(world.isClientSide());
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState rotate(BlockState state, Rotation rot)
  { return (rot==Rotation.CLOCKWISE_180) ? state : state.setValue(EASTWEST, !state.getValue(EASTWEST)); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror mirrorIn)
  { return state; }

}
