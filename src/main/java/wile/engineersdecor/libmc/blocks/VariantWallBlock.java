/*
 * @file VariantWallBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Wall blocks.
 */
package wile.engineersdecor.libmc.blocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.shapes.VoxelShapes;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.world.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;


public class VariantWallBlock extends WallBlock implements StandardBlocks.IStandardBlock
{
  public static final BooleanProperty UP = BlockStateProperties.UP;
  public static final EnumProperty<WallHeight> WALL_EAST = BlockStateProperties.WALL_HEIGHT_EAST;
  public static final EnumProperty<WallHeight> WALL_NORTH = BlockStateProperties.WALL_HEIGHT_NORTH;
  public static final EnumProperty<WallHeight> WALL_SOUTH = BlockStateProperties.WALL_HEIGHT_SOUTH;
  public static final EnumProperty<WallHeight> WALL_WEST = BlockStateProperties.WALL_HEIGHT_WEST;
  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
  public static final IntegerProperty TEXTURE_VARIANT = IntegerProperty.create("tvariant", 0, 7);
  private final Map<BlockState, VoxelShape> shape_voxels;
  private final Map<BlockState, VoxelShape> collision_shape_voxels;
  private final long config;

  public VariantWallBlock(long config, Block.Properties builder)
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
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  private static VoxelShape combinedShape(VoxelShape pole, WallHeight height, VoxelShape low, VoxelShape high)
  {
    if(height == WallHeight.TALL) return VoxelShapes.or(pole, high);
    if(height == WallHeight.LOW)  return VoxelShapes.or(pole, low);
    return pole;
  }

  protected Map<BlockState, VoxelShape> buildWallShapes(double pole_width, double pole_height, double side_width, double side_min_y, double side_max_low_y, double side_max_tall_y)
  {
    final double px0=8.0-pole_width, px1=8.0+pole_width, sx0=8.0-side_width, sx1=8.0+side_width;
    VoxelShape vp  = Block.makeCuboidShape(px0, 0, px0, px1, pole_height, px1);
    VoxelShape vs1 = Block.makeCuboidShape(sx0, side_min_y, 0, sx1, side_max_low_y, sx1);
    VoxelShape vs2 = Block.makeCuboidShape(sx0, side_min_y, sx0, sx1, side_max_low_y, 16);
    VoxelShape vs3 = Block.makeCuboidShape(0, side_min_y, sx0, sx1, side_max_low_y, sx1);
    VoxelShape vs4 = Block.makeCuboidShape(sx0, side_min_y, sx0, 16, side_max_low_y, sx1);
    VoxelShape vs5 = Block.makeCuboidShape(sx0, side_min_y, 0, sx1, side_max_tall_y, sx1);
    VoxelShape vs6 = Block.makeCuboidShape(sx0, side_min_y, sx0, sx1, side_max_tall_y, 16);
    VoxelShape vs7 = Block.makeCuboidShape(0, side_min_y, sx0, sx1, side_max_tall_y, sx1);
    VoxelShape vs8 = Block.makeCuboidShape(sx0, side_min_y, sx0, 16, side_max_tall_y, sx1);
    Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
    for(Boolean up : UP.getAllowedValues()) {
      for(WallHeight wh_east : WALL_EAST.getAllowedValues()) {
        for(WallHeight wh_north : WALL_NORTH.getAllowedValues()) {
          for(WallHeight wh_west : WALL_WEST.getAllowedValues()) {
            for(WallHeight wh_south : WALL_SOUTH.getAllowedValues()) {
              VoxelShape shape = VoxelShapes.empty();
              shape = combinedShape(shape, wh_east, vs4, vs8);
              shape = combinedShape(shape, wh_west, vs3, vs7);
              shape = combinedShape(shape, wh_north, vs1, vs5);
              shape = combinedShape(shape, wh_south, vs2, vs6);
              if(up) shape = VoxelShapes.or(shape, vp);
              BlockState bs = getDefaultState().with(UP, up)
                .with(WALL_EAST, wh_east)
                .with(WALL_NORTH, wh_north)
                .with(WALL_WEST, wh_west)
                .with(WALL_SOUTH, wh_south);
              final VoxelShape tvs = shape;
              TEXTURE_VARIANT.getAllowedValues().forEach((tv)->{
                builder.put(bs.with(TEXTURE_VARIANT, tv).with(WATERLOGGED, false), tvs);
                builder.put(bs.with(TEXTURE_VARIANT, tv).with(WATERLOGGED, true), tvs);
              });
            }
          }
        }
      }
    }
    return builder.build();
  }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return shape_voxels.getOrDefault(state, VoxelShapes.fullCube()); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return collision_shape_voxels.getOrDefault(state, VoxelShapes.fullCube()); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(TEXTURE_VARIANT); }

  protected boolean attachesTo(BlockState facingState, IWorldReader world, BlockPos facingPos, Direction side)
  {
    final Block block = facingState.getBlock();
    if((block instanceof FenceGateBlock) || (block instanceof WallBlock)) return true;
    final BlockState oppositeState = world.getBlockState(facingPos.offset(side, 2));
    if(!(oppositeState.getBlock() instanceof VariantWallBlock)) return false;
    return facingState.isNormalCube(world, facingPos) && Block.hasEnoughSolidSide(world, facingPos, side);
  }

  protected WallHeight selectWallHeight(IWorldReader world, BlockPos pos, Direction direction)
  {
    return WallHeight.LOW; // @todo: implement
  }

  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    IWorldReader world = context.getWorld();
    BlockPos pos = context.getPos();
    FluidState fs = context.getWorld().getFluidState(context.getPos());
    boolean n = attachesTo(world.getBlockState(pos.north()), world, pos.north(), Direction.SOUTH);
    boolean e = attachesTo(world.getBlockState(pos.east()), world, pos.east(), Direction.WEST);
    boolean s = attachesTo(world.getBlockState(pos.south()), world, pos.south(), Direction.NORTH);
    boolean w = attachesTo(world.getBlockState(pos.west()), world, pos.west(), Direction.EAST);
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return getDefaultState().with(UP, not_straight)
      .with(WALL_NORTH, n ? selectWallHeight(world, pos, Direction.NORTH) : WallHeight.NONE)
      .with(WALL_EAST , e ? selectWallHeight(world, pos, Direction.EAST)  : WallHeight.NONE)
      .with(WALL_SOUTH, s ? selectWallHeight(world, pos, Direction.SOUTH) : WallHeight.NONE)
      .with(WALL_WEST , w ? selectWallHeight(world, pos, Direction.WEST)  : WallHeight.NONE)
      .with(WATERLOGGED, fs.getFluid()==Fluids.WATER);
  }

  @Override
  public BlockState updatePostPlacement(BlockState state, Direction side, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  {
    if(state.get(WATERLOGGED)) world.getPendingFluidTicks().scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    if(side == Direction.DOWN) return super.updatePostPlacement(state, side, facingState, world, pos, facingPos);
    boolean n = (side==Direction.NORTH) ? this.attachesTo(facingState, world, facingPos, side) : state.get(WALL_NORTH)!=WallHeight.NONE;
    boolean e = (side==Direction.EAST) ? this.attachesTo(facingState, world, facingPos, side) : state.get(WALL_EAST)!=WallHeight.NONE;
    boolean s = (side==Direction.SOUTH) ? this.attachesTo(facingState, world, facingPos, side) : state.get(WALL_SOUTH)!=WallHeight.NONE;
    boolean w = (side==Direction.WEST) ? this.attachesTo(facingState, world, facingPos, side) : state.get(WALL_WEST)!=WallHeight.NONE;
    boolean not_straight = (!n || !s || e || w) && (n  || s || !e || !w);
    return state.with(UP, not_straight)
      .with(WALL_NORTH, n ? selectWallHeight(world, pos, Direction.NORTH) : WallHeight.NONE)
      .with(WALL_EAST , e ? selectWallHeight(world, pos, Direction.EAST)  : WallHeight.NONE)
      .with(WALL_SOUTH, s ? selectWallHeight(world, pos, Direction.SOUTH) : WallHeight.NONE)
      .with(WALL_WEST , w ? selectWallHeight(world, pos, Direction.WEST)  : WallHeight.NONE)
      .with(TEXTURE_VARIANT, ((int)MathHelper.getPositionRandom(pos)) & 0x7);
  }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.NORMAL; }
}
