/*
 * @file EdCatwalkStairsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stair version of the catwalk block, optional left/right railings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.StandardBlocks;
import wile.engineersdecor.libmc.Auxiliaries;

import javax.annotation.Nullable;
import java.util.*;


public class EdCatwalkStairsBlock extends StandardBlocks.HorizontalWaterLoggable
{
  public static final BooleanProperty RIGHT_RAILING = BooleanProperty.create("right_railing");
  public static final BooleanProperty LEFT_RAILING = BooleanProperty.create("left_railing");
  protected final Map<BlockState, VoxelShape> shapes;
  protected final Map<BlockState, VoxelShape> collision_shapes;
  protected final Map<Direction, Integer> y_rotations;

  public EdCatwalkStairsBlock(long config, BlockBehaviour.Properties properties, final AABB[] base_aabb, final AABB[] railing_aabbs)
  {
    super(config, properties, base_aabb);
    Map<BlockState, VoxelShape> sh = new HashMap<>();
    Map<BlockState, VoxelShape> csh = new HashMap<>();
    getStateDefinition().getPossibleStates().forEach(state->{
      Direction facing = state.getValue(HORIZONTAL_FACING);
      VoxelShape base_shape  = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(base_aabb, facing, true));
      if(state.getValue(RIGHT_RAILING)) {
        VoxelShape right_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(Auxiliaries.getMirroredAABB(railing_aabbs, Direction.Axis.X), facing, true));
        base_shape = Shapes.joinUnoptimized(base_shape, right_shape, BooleanOp.OR);
      }
      if(state.getValue(LEFT_RAILING)) {
        VoxelShape left_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(railing_aabbs, facing, true));
        base_shape = Shapes.joinUnoptimized(base_shape, left_shape, BooleanOp.OR);
      }
      sh.put(state, base_shape);
      csh.put(state, base_shape);
    });
    shapes = sh;
    collision_shapes = csh;
    y_rotations = new HashMap<>();
    y_rotations.put(Direction.NORTH, 0);
    y_rotations.put(Direction.EAST, 1);
    y_rotations.put(Direction.SOUTH, 2);
    y_rotations.put(Direction.WEST, 3);
    y_rotations.put(Direction.UP, 0);
    y_rotations.put(Direction.DOWN, 0);
    registerDefaultState(super.defaultBlockState().setValue(LEFT_RAILING, false).setValue(RIGHT_RAILING, false));
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
  { return shapes.getOrDefault(state, Shapes.block()); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
  { return collision_shapes.getOrDefault(state, Shapes.block()); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(RIGHT_RAILING, LEFT_RAILING); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  { return super.getStateForPlacement(context); }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if((!(item instanceof BlockItem))) return InteractionResult.PASS;
    final Block block = ((BlockItem)item).getBlock();
    final Direction facing = state.getValue(HORIZONTAL_FACING);
    if(block == this) {
      final Direction hlv = Arrays.stream(Direction.orderedByNearest(player)).filter(d->d.getAxis().isHorizontal()).findFirst().orElse(Direction.NORTH);
      BlockPos adjacent_pos;
      if(hlv == facing) {
        adjacent_pos = pos.above().relative(hlv);
      } else if(hlv == facing.getOpposite()) {
        adjacent_pos = pos.below().relative(hlv);
      } else {
        return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      }
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      if(!adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) return InteractionResult.CONSUME;
      BlockState place_state = defaultBlockState().setValue(HORIZONTAL_FACING, facing);
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    } else if((block == ModContent.getBlock("steel_catwalk")) || (block == ModContent.getBlock("steel_catwalk_ta"))) {
      BlockPos adjacent_pos;
      adjacent_pos = pos.relative(facing);
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return InteractionResult.CONSUME;
      if(!adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) return InteractionResult.CONSUME;
      BlockState place_state = ModContent.getBlock("steel_catwalk_ta").defaultBlockState(); // ModContent.STEEL_CATWALK_TOP_ALIGNED
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    } else if(block == ModContent.getBlock("steel_railing")) {
      Direction face = hit.getDirection();
      int shrink = 0;
      BlockState place_state = state;
      if(face == Direction.UP) {
        Vec3 rhv = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos())).multiply(new Vec3(1,0,1)).cross(Vec3.atLowerCornerOf(facing.getNormal()));
        face = (rhv.y > 0) ? (facing.getClockWise()) : (facing.getCounterClockWise());
      }
      if(face == facing.getClockWise()) {
        if(state.getValue(RIGHT_RAILING)) {
          place_state = state.setValue(RIGHT_RAILING, false);
          shrink = -1;
        } else {
          place_state = state.setValue(RIGHT_RAILING, true);
          shrink = 1;
        }
      } else if(face == facing.getCounterClockWise()) {
        if(state.getValue(LEFT_RAILING)) {
          place_state = state.setValue(LEFT_RAILING, false);
          shrink = -1;
        } else {
          place_state = state.setValue(LEFT_RAILING, true);
          shrink = 1;
        }
      }
      if(shrink != 0) EdCatwalkBlock.place_consume(place_state, world, pos, player, hand, shrink);
      return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
  }

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
  {
    if(world.isClientSide()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    int n = (state.getValue(LEFT_RAILING)?1:0)+(state.getValue(RIGHT_RAILING)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.getBlock("steel_railing"), n));
    return drops;
  }

}
