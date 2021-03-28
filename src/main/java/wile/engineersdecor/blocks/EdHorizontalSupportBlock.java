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

  public EdHorizontalSupportBlock(long config, Block.Properties builder, final AxisAlignedBB mainBeamAABB, final AxisAlignedBB eastBeamAABB, final AxisAlignedBB thinDownBeamAABB, final AxisAlignedBB thickDownBeamAABB)
  {
    super(config|DecorBlock.CFG_HORIZIONTAL, builder);
    Map<BlockState, VoxelShape> aabbs = new HashMap<>();
    for(boolean eastwest:EASTWEST.getAllowedValues()) {
      for(boolean leftbeam:LEFTBEAM.getAllowedValues()) {
        for(boolean rightbeam:RIGHTBEAM.getAllowedValues()) {
          for(int downconnect:DOWNCONNECT.getAllowedValues()) {
            final BlockState state = getDefaultState().with(EASTWEST, eastwest).with(LEFTBEAM, leftbeam).with(RIGHTBEAM, rightbeam).with(DOWNCONNECT, downconnect);
            VoxelShape shape = VoxelShapes.create(Auxiliaries.getRotatedAABB(mainBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true));
            if(rightbeam) shape = VoxelShapes.combine(shape, VoxelShapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.EAST:Direction.NORTH, true)), IBooleanFunction.OR);
            if(leftbeam) shape = VoxelShapes.combine(shape, VoxelShapes.create(Auxiliaries.getRotatedAABB(eastBeamAABB, eastwest?Direction.WEST:Direction.SOUTH, true)), IBooleanFunction.OR);
            if(downconnect==1) shape = VoxelShapes.combine(shape, VoxelShapes.create(thinDownBeamAABB), IBooleanFunction.OR);
            if(downconnect==2) shape = VoxelShapes.combine(shape, VoxelShapes.create(thickDownBeamAABB), IBooleanFunction.OR);
            aabbs.put(state.with(WATERLOGGED, false), shape);
            aabbs.put(state.with(WATERLOGGED, true), shape);
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
  public boolean canSpawnInBlock()
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
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(EASTWEST, RIGHTBEAM, LEFTBEAM, DOWNCONNECT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return temp_block_update_until_better(super.getStateForPlacement(context).with(EASTWEST, context.getPlacementHorizontalFacing().getAxis()==Direction.Axis.X), context.getWorld(), context.getPos()); }

  private BlockState temp_block_update_until_better(BlockState state, IWorld world, BlockPos pos)
  {
    boolean ew = state.get(EASTWEST);
    final BlockState rstate = world.getBlockState((!ew) ? (pos.east()) : (pos.south()) );
    final BlockState lstate = world.getBlockState((!ew) ? (pos.west()) : (pos.north()) );
    final BlockState dstate = world.getBlockState(pos.down());
    int down_connector = 0;
    if((dstate.getBlock() instanceof EdStraightPoleBlock)) {
      final Direction dfacing = dstate.get(EdStraightPoleBlock.FACING);
      final EdStraightPoleBlock pole = (EdStraightPoleBlock)dstate.getBlock();
      if((dfacing.getAxis() == Direction.Axis.Y)) {
        if((pole== ModContent.THICK_STEEL_POLE) || ((pole==ModContent.THICK_STEEL_POLE_HEAD) && (dfacing==Direction.UP))) {
          down_connector = 2;
        } else if((pole==ModContent.THIN_STEEL_POLE) || ((pole==ModContent.THIN_STEEL_POLE_HEAD) && (dfacing==Direction.UP))) {
          down_connector = 1;
        }
      }
    }
    return state.with(RIGHTBEAM, (rstate.getBlock()==this) && (rstate.get(EASTWEST) != ew))
      .with(LEFTBEAM , (lstate.getBlock()==this) && (lstate.get(EASTWEST) != ew))
      .with(DOWNCONNECT , down_connector);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return temp_block_update_until_better(state, world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    ItemStack held_stack = player.getHeldItem(hand);
    if((held_stack.isEmpty()) || (held_stack.getItem() != this.asItem())) return ActionResultType.PASS;
    if(!(hit.getFace().getAxis().isVertical())) return ActionResultType.PASS;
    final Direction placement_direction = player.getHorizontalFacing();
    final BlockPos adjacent_pos = pos.offset(placement_direction);
    final BlockState adjacent = world.getBlockState(adjacent_pos);
    final BlockItemUseContext ctx = new DirectionalPlaceContext(world, adjacent_pos, placement_direction, player.getHeldItem(hand), placement_direction.getOpposite());
    if(!adjacent.isReplaceable(ctx)) return ActionResultType.func_233537_a_(world.isRemote());
    final BlockState new_state = getStateForPlacement(ctx);
    if(new_state == null) return ActionResultType.FAIL;
    if(!world.setBlockState(adjacent_pos, new_state, 1|2)) return ActionResultType.FAIL;
    world.playSound(player, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      held_stack.shrink(1);
      Inventories.setItemInPlayerHand(player, hand, held_stack);
    }
    return ActionResultType.func_233537_a_(world.isRemote());
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState rotate(BlockState state, Rotation rot)
  { return (rot==Rotation.CLOCKWISE_180) ? state : state.with(EASTWEST, !state.get(EASTWEST)); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror mirrorIn)
  { return state; }

}
