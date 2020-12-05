/*
 * @file EdCatwalkStairsBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stair version of the catwalk block, optional left/right railings.
 */
package wile.engineersdecor.blocks;

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

  public EdCatwalkStairsBlock(long config, Block.Properties properties, final AxisAlignedBB[] base_aabb, final AxisAlignedBB[] railing_aabbs)
  {
    super(config, properties, base_aabb);
    Map<BlockState, VoxelShape> sh = new HashMap<>();
    Map<BlockState, VoxelShape> csh = new HashMap<>();
    getStateContainer().getValidStates().forEach(state->{
      Direction facing = state.get(HORIZONTAL_FACING);
      VoxelShape base_shape  = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(base_aabb, facing, true));
      if(state.get(RIGHT_RAILING)) {
        VoxelShape right_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(Auxiliaries.getMirroredAABB(railing_aabbs, Axis.X), facing, true));
        base_shape = VoxelShapes.combine(base_shape, right_shape, IBooleanFunction.OR);
      }
      if(state.get(LEFT_RAILING)) {
        VoxelShape left_shape = Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(railing_aabbs, facing, true));
        base_shape = VoxelShapes.combine(base_shape, left_shape, IBooleanFunction.OR);
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
    setDefaultState(super.getDefaultState().with(LEFT_RAILING, false).with(RIGHT_RAILING, false));
  }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
  { return shapes.getOrDefault(state, VoxelShapes.fullCube()); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
  { return collision_shapes.getOrDefault(state, VoxelShapes.fullCube()); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(RIGHT_RAILING, LEFT_RAILING); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context); }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getHeldItem(hand).getItem();
    if((!(item instanceof BlockItem))) return ActionResultType.PASS;
    final Block block = ((BlockItem)item).getBlock();
    final Direction facing = state.get(HORIZONTAL_FACING);
    if(block == this) {
      final Direction hlv = Arrays.stream(Direction.getFacingDirections(player)).filter(d->d.getAxis().isHorizontal()).findFirst().orElse(Direction.NORTH);
      BlockPos adjacent_pos;
      if(hlv == facing) {
        adjacent_pos = pos.up().offset(hlv);
      } else if(hlv == facing.getOpposite()) {
        adjacent_pos = pos.down().offset(hlv);
      } else {
        return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
      }
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
      if(!adjacent_state.isReplaceable(new DirectionalPlaceContext(world, adjacent_pos, hit.getFace().getOpposite(), player.getHeldItem(hand), hit.getFace()))) return ActionResultType.CONSUME;
      BlockState place_state = getDefaultState().with(HORIZONTAL_FACING, facing);
      place_state = place_state.with(WATERLOGGED,adjacent_state.getFluidState().getFluid()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    } else if((block == ModContent.STEEL_CATWALK) || (block == ModContent.STEEL_CATWALK_TOP_ALIGNED)) {
      BlockPos adjacent_pos;
      adjacent_pos = pos.offset(facing);
      final BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state == null) return ActionResultType.CONSUME;
      if(!adjacent_state.isReplaceable(new DirectionalPlaceContext(world, adjacent_pos, hit.getFace().getOpposite(), player.getHeldItem(hand), hit.getFace()))) return ActionResultType.CONSUME;
      BlockState place_state = ModContent.STEEL_CATWALK_TOP_ALIGNED.getDefaultState();
      place_state = place_state.with(WATERLOGGED,adjacent_state.getFluidState().getFluid()==Fluids.WATER);
      EdCatwalkBlock.place_consume(place_state, world, adjacent_pos, player, hand, 1);
      return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    } else if(block == ModContent.STEEL_RAILING) {
      Direction face = hit.getFace();
      int shrink = 0;
      BlockState place_state = state;
      if(face == Direction.UP) {
        Vector3d rhv = hit.getHitVec().subtract(Vector3d.copyCentered(hit.getPos())).mul(new Vector3d(1,0,1)).crossProduct(Vector3d.copy(facing.getDirectionVec()));
        face = (rhv.y > 0) ? (facing.rotateY()) : (facing.rotateYCCW());
      }
      if(face == facing.rotateY()) {
        if(state.get(RIGHT_RAILING)) {
          place_state = state.with(RIGHT_RAILING, false);
          shrink = -1;
        } else {
          place_state = state.with(RIGHT_RAILING, true);
          shrink = 1;
        }
      } else if(face == facing.rotateYCCW()) {
        if(state.get(LEFT_RAILING)) {
          place_state = state.with(LEFT_RAILING, false);
          shrink = -1;
        } else {
          place_state = state.with(LEFT_RAILING, true);
          shrink = 1;
        }
      }
      if(shrink != 0) EdCatwalkBlock.place_consume(place_state, world, pos, player, hand, shrink);
      return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
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
    if(world.isRemote()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    int n = (state.get(LEFT_RAILING)?1:0)+(state.get(RIGHT_RAILING)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.STEEL_RAILING, n));
    return drops;
  }

}
