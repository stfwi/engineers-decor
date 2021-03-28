/*
 * @file EdCatwalkTopAlignedBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Top aligned platforms, down-connection to poles.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import wile.engineersdecor.ModContent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class EdCatwalkTopAlignedBlock extends DecorBlock.WaterLoggable implements IDecorBlock
{
  public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
  protected final List<VoxelShape> variant_shapes;

  public EdCatwalkTopAlignedBlock(long config, Block.Properties properties, final VoxelShape[] variant_shapes)
  {
    super(config, properties, variant_shapes[0]);
    setDefaultState(super.getDefaultState().with(VARIANT, 0));
    this.variant_shapes = VARIANT.getAllowedValues().stream().map(i->(i<variant_shapes.length) ? (variant_shapes[i]) : (VoxelShapes.fullCube())).collect(Collectors.toList());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return variant_shapes.get(state.get(VARIANT)); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(VARIANT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    BlockState state = adapted_state(super.getStateForPlacement(context), context.getWorld(), context.getPos());
    if(context.getFace() != Direction.UP) return state;
    BlockState below = context.getWorld().getBlockState(context.getPos().down());
    if((state.get(VARIANT)==0) && (below.isSolidSide(context.getWorld(), context.getPos().down(), Direction.UP))) return state.with(VARIANT, 3);
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getHeldItem(hand).getItem();
    if(item != this.asItem()) return ActionResultType.PASS;
    if(hit.getFace().getAxis().isHorizontal()) return ActionResultType.PASS;
    BlockPos adjacent_pos = pos.offset(player.getHorizontalFacing());
    BlockState adjacent_state = world.getBlockState(adjacent_pos);
    if(adjacent_state.isReplaceable(new DirectionalPlaceContext(world, adjacent_pos, hit.getFace().getOpposite(), player.getHeldItem(hand), hit.getFace()))) {
      BlockState place_state = getDefaultState();
      place_state = place_state.with(WATERLOGGED,adjacent_state.getFluidState().getFluid()==Fluids.WATER);
      EdCatwalkBlock.place_consume(adapted_state(place_state, world, adjacent_pos), world, adjacent_pos, player, hand, 1);
    }
    return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
  }

  @Override
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return adapted_state(super.updatePostPlacement(state, facing, facingState, world, pos, facingPos), world, pos); }

  // ---

  private BlockState adapted_state(BlockState state, IWorld world, BlockPos pos)
  {
    BlockState below = world.getBlockState(pos.down());
    if((below == null) || (state == null)) return state;
    if((below.getBlock() == ModContent.THICK_STEEL_POLE) || (below.getBlock() == ModContent.THICK_STEEL_POLE_HEAD)) return state.with(VARIANT, 1);
    if((below.getBlock() == ModContent.THIN_STEEL_POLE) || (below.getBlock() == ModContent.THIN_STEEL_POLE_HEAD)) return state.with(VARIANT, 2);
    return state;
  }

}
