/*
 * @file BlockDecorDirected.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.entity.EntityType;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.block.Block;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.BlockState;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;


public class BlockDecorDirected extends BlockDecor
{
  public static final DirectionProperty FACING = DirectionalBlock.FACING;
  protected final ArrayList<VoxelShape> AABBs;

  public BlockDecorDirected(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  {
    super(config, builder);
    setDefaultState(stateContainer.getBaseState().with(FACING, Direction.UP));
    final boolean is_horizontal = ((config & BlockDecor.CFG_HORIZIONTAL)!=0);
    AABBs = new ArrayList<VoxelShape>(Arrays.asList(
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.DOWN, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.UP, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.NORTH, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.SOUTH, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.WEST, is_horizontal)),
      VoxelShapes.create(ModAuxiliaries.getRotatedAABB(unrotatedAABB, Direction.EAST, is_horizontal)),
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
  { return AABBs.get((state.get(FACING)).getIndex() & 0x7); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { builder.add(FACING); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getFace();
    if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) {
      // horizontal placement in direction the player is looking
      facing = context.getPlacementHorizontalFacing();
    } else if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL)) {
      // horizontal placement on a face
      if(((facing==Direction.UP)||(facing==Direction.DOWN))) return null;
    } else if((config & CFG_LOOK_PLACEMENT)!=0) {
      // placement in direction the player is looking, with up and down
      facing = context.getNearestLookingDirection();
    } else {
      // default: placement on the face the player clicking
    }
    if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
    if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().isSneaking())) facing = facing.getOpposite();
    return getDefaultState().with(FACING, facing);
  }

}
