/*
 * @file BlockDecorDirectedHorizontal.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller directed block with direction set narrowed
 * to horizontal directions.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.entity.EntityType;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.detail.ModAuxiliaries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class BlockDecorDirectedHorizontal extends BlockDecor
{
  public static final DirectionProperty HORIZONTAL_FACING = HorizontalBlock.HORIZONTAL_FACING;
  protected final ArrayList<VoxelShape> AABBs;

  public BlockDecorDirectedHorizontal(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  {
    super(config|CFG_HORIZIONTAL, builder, unrotatedAABB);
    setDefaultState(stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    AABBs = new ArrayList<VoxelShape>(Arrays.asList(
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.DOWN, true)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.UP, true)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.NORTH, true)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.SOUTH, true)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.WEST, true)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.EAST, true)),
      VoxelShapes.create(unrotatedAABB),
      VoxelShapes.create(unrotatedAABB)
    ));
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return ((config & CFG_CUTOUT)!=0) ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs.get((state.get(HORIZONTAL_FACING)).getIndex() & 0x7); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(HORIZONTAL_FACING); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getFace();
    if((config & CFG_LOOK_PLACEMENT) != 0) {
      // horizontal placement in direction the player is looking
      facing = context.getPlacementHorizontalFacing();
    } else {
      // horizontal placement on a face
      facing = ((facing==Direction.UP)||(facing==Direction.DOWN)) ? (context.getPlacementHorizontalFacing()) : facing;
    }
    if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
    if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().isSneaking())) facing = facing.getOpposite();
    return super.getStateForPlacement(context).with(HORIZONTAL_FACING, facing);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState rotate(BlockState state, Rotation rot)
  { return state.with(HORIZONTAL_FACING, rot.rotate(state.get(HORIZONTAL_FACING))); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror mirrorIn)
  { return state.rotate(mirrorIn.toRotation(state.get(HORIZONTAL_FACING))); }

  /**
   * Water loggable version of directed blocks.
   */
  public static class WaterLoggable extends BlockDecorDirectedHorizontal implements IWaterLoggable
  {
    public WaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

}
