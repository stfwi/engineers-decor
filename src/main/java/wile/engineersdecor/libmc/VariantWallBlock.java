/*
 * @file VariantWallBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.libmc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;


public class VariantWallBlock extends WallBlock implements StandardBlocks.IStandardBlock
{
  public static final BooleanProperty UP = BlockStateProperties.UP;
  public static final EnumProperty<WallSide> WALL_EAST = BlockStateProperties.EAST_WALL;
  public static final EnumProperty<WallSide> WALL_NORTH = BlockStateProperties.NORTH_WALL;
  public static final EnumProperty<WallSide> WALL_SOUTH = BlockStateProperties.SOUTH_WALL;
  public static final EnumProperty<WallSide> WALL_WEST = BlockStateProperties.WEST_WALL;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final IntegerProperty TEXTURE_VARIANT = IntegerProperty.create("tvariant", 0, 7);
  private final Map<BlockState, VoxelShape> shape_voxels;
  private final Map<BlockState, VoxelShape> collision_shape_voxels;
  private final long config;

  public VariantWallBlock(long config, BlockBehaviour.Properties builder)
  {
    super(builder);
    shape_voxels = buildWallShapes(4, 16, 4, 0, 16, 16);
    collision_shape_voxels = buildWallShapes(6, 16, 5, 0, 24, 24);
    this.config = config;
  }

  @Override
  public long config()
  { return config; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  private static VoxelShape combinedShape(VoxelShape pole, WallSide height, VoxelShape low, VoxelShape high)
  {
    if(height == WallSide.TALL) return Shapes.or(pole, high);
    if(height == WallSide.LOW)  return Shapes.or(pole, low);
    return pole;
  }

  protected Map<BlockState, VoxelShape> buildWallShapes(double pole_width, double pole_height, double side_width, double side_min_y, double side_max_low_y, double side_max_tall_y)
  {
    final double px0=8.0-pole_width, px1=8.0+pole_width, sx0=8.0-side_width, sx1=8.0+side_width;
    VoxelShape vp  = Block.box(px0, 0, px0, px1, pole_height, px1);
    VoxelShape vs1 = Block.box(sx0, side_min_y, 0, sx1, side_max_low_y, sx1);
    VoxelShape vs2 = Block.box(sx0, side_min_y, sx0, sx1, side_max_low_y, 16);
    VoxelShape vs3 = Block.box(0, side_min_y, sx0, sx1, side_max_low_y, sx1);
    VoxelShape vs4 = Block.box(sx0, side_min_y, sx0, 16, side_max_low_y, sx1);
    VoxelShape vs5 = Block.box(sx0, side_min_y, 0, sx1, side_max_tall_y, sx1);
    VoxelShape vs6 = Block.box(sx0, side_min_y, sx0, sx1, side_max_tall_y, 16);
    VoxelShape vs7 = Block.box(0, side_min_y, sx0, sx1, side_max_tall_y, sx1);
    VoxelShape vs8 = Block.box(sx0, side_min_y, sx0, 16, side_max_tall_y, sx1);
    Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
    for(Boolean up : UP.getPossibleValues()) {
      for(WallSide wh_east : WALL_EAST.getPossibleValues()) {
        for(WallSide wh_north : WALL_NORTH.getPossibleValues()) {
          for(WallSide wh_west : WALL_WEST.getPossibleValues()) {
            for(WallSide wh_south : WALL_SOUTH.getPossibleValues()) {
              VoxelShape shape = Shapes.empty();
              shape = combinedShape(shape, wh_east, vs4, vs8);
              shape = combinedShape(shape, wh_west, vs3, vs7);
              shape = combinedShape(shape, wh_north, vs1, vs5);
              shape = combinedShape(shape, wh_south, vs2, vs6);
              if(up) shape = Shapes.or(shape, vp);
              BlockState bs = defaultBlockState().setValue(UP, up)
                .setValue(WALL_EAST, wh_east)
                .setValue(WALL_NORTH, wh_north)
                .setValue(WALL_WEST, wh_west)
                .setValue(WALL_SOUTH, wh_south);
              final VoxelShape tvs = shape;
              TEXTURE_VARIANT.getPossibleValues().forEach((tv)->{
                builder.put(bs.setValue(TEXTURE_VARIANT, tv).setValue(WATERLOGGED, false), tvs);
                builder.put(bs.setValue(TEXTURE_VARIANT, tv).setValue(WATERLOGGED, true), tvs);
              });
            }
          }
        }
      }
    }
    return builder.build();
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return shape_voxels.getOrDefault(state, Shapes.block()); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return collision_shape_voxels.getOrDefault(state, Shapes.block()); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(TEXTURE_VARIANT); }

  protected boolean attachesTo(BlockState facingState, LevelReader world, BlockPos facingPos, Direction side)
  {
    final Block block = facingState.getBlock();
    if((block instanceof FenceGateBlock) || (block instanceof WallBlock)) return true;
    final BlockState oppositeState = world.getBlockState(facingPos.relative(side, 2));
    if(!(oppositeState.getBlock() instanceof VariantWallBlock)) return false;
    return facingState.isRedstoneConductor(world, facingPos) && Block.canSupportCenter(world, facingPos, side);
  }

  protected WallSide selectWallHeight(LevelReader world, BlockPos pos, Direction direction)
  { return WallSide.LOW; }

  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    LevelReader world = context.getLevel();
    BlockPos pos = context.getClickedPos();
    FluidState fs = context.getLevel().getFluidState(context.getClickedPos());
    boolean n = attachesTo(world.getBlockState(pos.north()), world, pos.north(), Direction.SOUTH);
    boolean e = attachesTo(world.getBlockState(pos.east()), world, pos.east(), Direction.WEST);
    boolean s = attachesTo(world.getBlockState(pos.south()), world, pos.south(), Direction.NORTH);
    boolean w = attachesTo(world.getBlockState(pos.west()), world, pos.west(), Direction.EAST);
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return defaultBlockState().setValue(UP, not_straight)
      .setValue(WALL_NORTH, n ? selectWallHeight(world, pos, Direction.NORTH) : WallSide.NONE)
      .setValue(WALL_EAST , e ? selectWallHeight(world, pos, Direction.EAST)  : WallSide.NONE)
      .setValue(WALL_SOUTH, s ? selectWallHeight(world, pos, Direction.SOUTH) : WallSide.NONE)
      .setValue(WALL_WEST , w ? selectWallHeight(world, pos, Direction.WEST)  : WallSide.NONE)
      .setValue(WATERLOGGED, fs.getType()==Fluids.WATER);
  }

  @Override
  public BlockState updateShape(BlockState state, Direction side, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  {
    if(state.getValue(WATERLOGGED)) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
    if(side == Direction.DOWN) return super.updateShape(state, side, facingState, world, pos, facingPos);
    boolean n = (side==Direction.NORTH) ? this.attachesTo(facingState, world, facingPos, side) : state.getValue(WALL_NORTH)!=WallSide.NONE;
    boolean e = (side==Direction.EAST) ? this.attachesTo(facingState, world, facingPos, side) : state.getValue(WALL_EAST)!=WallSide.NONE;
    boolean s = (side==Direction.SOUTH) ? this.attachesTo(facingState, world, facingPos, side) : state.getValue(WALL_SOUTH)!=WallSide.NONE;
    boolean w = (side==Direction.WEST) ? this.attachesTo(facingState, world, facingPos, side) : state.getValue(WALL_WEST)!=WallSide.NONE;
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return state.setValue(UP, not_straight)
      .setValue(WALL_NORTH, n ? selectWallHeight(world, pos, Direction.NORTH) : WallSide.NONE)
      .setValue(WALL_EAST , e ? selectWallHeight(world, pos, Direction.EAST)  : WallSide.NONE)
      .setValue(WALL_SOUTH, s ? selectWallHeight(world, pos, Direction.SOUTH) : WallSide.NONE)
      .setValue(WALL_WEST , w ? selectWallHeight(world, pos, Direction.WEST)  : WallSide.NONE)
      .setValue(TEXTURE_VARIANT, ((int)Mth.getSeed(pos)) & 0x7);
  }

  @Override
  public boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPistonPushReaction(BlockState state)
  { return PushReaction.NORMAL; }
}
