/*
 * @file EdCatwalkStairsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stair version of the catwalk block, optional left/right railings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.*;



public class EdCatwalkStairsBlock extends DecorBlock.HorizontalWaterLoggable implements IDecorBlock
{
  public static final BooleanProperty RIGHT_RAILING = BooleanProperty.create("right_railing");
  public static final BooleanProperty LEFT_RAILING = BooleanProperty.create("left_railing");
  protected final Map<BlockState, VoxelShape> shapes;
  protected final Map<BlockState, VoxelShape> collision_shapes;
  protected final Map<Direction, Integer> y_rotations;

  public EdCatwalkStairsBlock(long config, AbstractBlock.Properties properties, final AxisAlignedBB[] base_aabb, final AxisAlignedBB[] railing_aabbs)
  {
    super(config, properties, base_aabb);
    Map<BlockState, VoxelShape> sh = new HashMap<>();
    Map<BlockState, VoxelShape> csh = new HashMap<>();
    getStateDefinition().getPossibleStates().forEach(state->{
      Direction facing = state.getValue(HORIZONTAL_FACING);
      VoxelShape base_shape  = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(base_aabb, facing, true));
      if(state.getValue(RIGHT_RAILING)) {
        VoxelShape right_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(Auxiliaries.getMirroredAABB(railing_aabbs, Axis.X), facing, true));
        base_shape = VoxelShapes.joinUnoptimized(base_shape, right_shape, IBooleanFunction.OR);
      }
      if(state.getValue(LEFT_RAILING)) {
        VoxelShape left_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(railing_aabbs, facing, true));
        base_shape = VoxelShapes.joinUnoptimized(base_shape, left_shape, IBooleanFunction.OR);
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
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
  { return shapes.getOrDefault(state, VoxelShapes.block()); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
  { return collision_shapes.getOrDefault(state, VoxelShapes.block()); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(RIGHT_RAILING, LEFT_RAILING); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context); }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if((!(item instanceof BlockItem))) return ActionResultType.PASS;
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
        return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
      }
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
      if(!adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) return ActionResultType.CONSUME;
      BlockState place_state = defaultBlockState().setValue(HORIZONTAL_FACING, facing);
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    } else if((block == ModContent.STEEL_CATWALK) || (block == ModContent.STEEL_CATWALK_TOP_ALIGNED)) {
      BlockPos adjacent_pos;
      adjacent_pos = pos.relative(facing);
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return ActionResultType.CONSUME;
      if(!adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) return ActionResultType.CONSUME;
      BlockState place_state = ModContent.STEEL_CATWALK_TOP_ALIGNED.defaultBlockState();
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    } else if(block == ModContent.STEEL_RAILING) {
      Direction face = hit.getDirection();
      int shrink = 0;
      BlockState place_state = state;
      if(face == Direction.UP) {
        Vector3d rhv = hit.getLocation().subtract(Vector3d.atCenterOf(hit.getBlockPos())).multiply(new Vector3d(1,0,1)).cross(Vector3d.atLowerCornerOf(facing.getNormal()));
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
      return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }
    return ActionResultType.PASS;
  }

  // -- IDecorBlock

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, World world, @Nullable TileEntity te, boolean explosion)
  {
    if(world.isClientSide()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    int n = (state.getValue(LEFT_RAILING)?1:0)+(state.getValue(RIGHT_RAILING)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.STEEL_RAILING, n));
    return drops;
  }

}
