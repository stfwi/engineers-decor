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

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;



public class EdHorizontalSupportBlock extends DecorBlock.WaterLoggable implements IDecorBlock
{
  public static final BooleanProperty EASTWEST  = BooleanProperty.create("eastwest");
  public static final BooleanProperty LEFTBEAM  = BooleanProperty.create("leftbeam");
  public static final BooleanProperty RIGHTBEAM = BooleanProperty.create("rightbeam");
  public static final IntegerProperty DOWNCONNECT = IntegerProperty.create("downconnect", 0, 2);
  protected final Map<BlockState, VoxelShape> AABBs;

  public EdHorizontalSupportBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB mainBeamAABB, final AxisAlignedBB eastBeamAABB, final AxisAlignedBB thinDownBeamAABB, final AxisAlignedBB thickDownBeamAABB)
  {
    super(config|DecorBlock.CFG_HORIZIONTAL, builder);
    Map<BlockState, VoxelShape> aabbs = new HashMap<>();
    for(boolean eastwest:EASTWEST.getPossibleValues()) {
      for(boolean leftbeam:LEFTBEAM.getPossibleValues()) {
        for(boolean rightbeam:RIGHTBEAM.getPossibleValues()) {
          for(int downconnect:DOWNCONNECT.getPossibleValues()) {
            final BlockState state = defaultBlockState().setValue(EASTWEST, eastwest).setValue(LEFTBEAM, leftbeam).setValue(RIGHTBEAM, rightbeam).setValue(DOWNCONNECT, downconnect);
            VoxelShape shape = VoxelShapes.create(Auxiliaries.getRotatedAABB(mainBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true));
            if(rightbeam) shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true)), IBooleanFunction.OR);
            if(leftbeam) shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.WEST:Direction.SOUTH, true)), IBooleanFunction.OR);
            if(downconnect==1) shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(thinDownBeamAABB), IBooleanFunction.OR);
            if(downconnect==2) shape = VoxelShapes.joinUnoptimized(shape, VoxelShapes.create(thickDownBeamAABB), IBooleanFunction.OR);
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
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs.get(state); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(EASTWEST, RIGHTBEAM, LEFTBEAM, DOWNCONNECT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return temp_block_update_until_better(super.getStateForPlacement(context).setValue(EASTWEST, context.getHorizontalDirection().getAxis()==Direction.Axis.X), context.getLevel(), context.getClickedPos()); }

  private BlockState temp_block_update_until_better(BlockState state, IWorld world, BlockPos pos)
  {
    boolean ew = state.getValue(EASTWEST);
    final BlockState rstate = world.getBlockState((!ew) ? (pos.east()) : (pos.south()) );
    final BlockState lstate = world.getBlockState((!ew) ? (pos.west()) : (pos.north()) );
    final BlockState dstate = world.getBlockState(pos.below());
    int down_connector = 0;
    if((dstate.getBlock() instanceof EdStraightPoleBlock)) {
      final Direction dfacing = dstate.getValue(EdStraightPoleBlock.FACING);
      final EdStraightPoleBlock pole = (EdStraightPoleBlock)dstate.getBlock();
      if((dfacing.getAxis() == Direction.Axis.Y)) {
        if((pole== ModContent.THICK_STEEL_POLE) || ((pole==ModContent.THICK_STEEL_POLE_HEAD) && (dfacing==Direction.UP))) {
          down_connector = 2;
        } else if((pole==ModContent.THIN_STEEL_POLE) || ((pole==ModContent.THIN_STEEL_POLE_HEAD) && (dfacing==Direction.UP))) {
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
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return temp_block_update_until_better(state, world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    ItemStack held_stack = player.getItemInHand(hand);
    if((held_stack.isEmpty()) || (held_stack.getItem() != this.asItem())) return ActionResultType.PASS;
    if(!(hit.getDirection().getAxis().isVertical())) return ActionResultType.PASS;
    final Direction placement_direction = player.getDirection();
    final BlockPos adjacent_pos = pos.relative(placement_direction);
    final BlockState adjacent = world.getBlockState(adjacent_pos);
    final BlockItemUseContext ctx = new DirectionalPlaceContext(world, adjacent_pos, placement_direction, player.getItemInHand(hand), placement_direction.getOpposite());
    if(!adjacent.canBeReplaced(ctx)) return ActionResultType.sidedSuccess(world.isClientSide());
    final BlockState new_state = getStateForPlacement(ctx);
    if(new_state == null) return ActionResultType.FAIL;
    if(!world.setBlock(adjacent_pos, new_state, 1|2)) return ActionResultType.FAIL;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      held_stack.shrink(1);
      Inventories.setItemInPlayerHand(player, hand, held_stack);
    }
    return ActionResultType.sidedSuccess(world.isClientSide());
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
