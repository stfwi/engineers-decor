/*
 * @file EdFloorGratingBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Floor gratings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;


public class EdHatchBlock extends DecorBlock.HorizontalWaterLoggable implements IDecorBlock
{
  public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
  public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
  protected final ArrayList<VoxelShape> vshapes_open;

  public EdHatchBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABBClosed, final AxisAlignedBB unrotatedAABBOpen)
  {
    super(config, builder, unrotatedAABBClosed); vshapes_open = makeHorizontalShapeLookup(new AxisAlignedBB[]{unrotatedAABBOpen});
    setDefaultState(super.getDefaultState().with(OPEN, false).with(POWERED, false));
  }

  public EdHatchBlock(long config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABBsClosed, final AxisAlignedBB[] unrotatedAABBsOpen)
  { super(config, builder, unrotatedAABBsClosed); vshapes_open = makeHorizontalShapeLookup(unrotatedAABBsOpen); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return RenderTypeHint.CUTOUT; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return state.get(OPEN) ? vshapes_open.get((state.get(HORIZONTAL_FACING)).getIndex() & 0x7) : super.getShape(state, source, pos, selectionContext); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return state.get(OPEN); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type)
  { return state.get(OPEN); }

  @Override
  public boolean isLadder(BlockState state, IWorldReader world, BlockPos pos, LivingEntity entity)
  { return state.get(OPEN); }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(OPEN, POWERED); }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if(world.isRemote) return ActionResultType.CONSUME;
    boolean open = !state.get(OPEN);
    world.setBlockState(pos, state.with(OPEN, open), 1|2);
    world.playSound(null, pos, open?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    return ActionResultType.SUCCESS;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    if((world.isRemote) || (!(state.getBlock() instanceof EdHatchBlock))) return;
    boolean powered = world.isBlockPowered(pos);
    if(powered == state.get(POWERED)) return;
    if(powered != state.get(OPEN)) world.playSound(null, pos, powered?SoundEvents.BLOCK_IRON_DOOR_OPEN:SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.7f, 1.4f);
    world.setBlockState(pos, state.with(OPEN, powered).with(POWERED, powered), 1|2);
  }

}
