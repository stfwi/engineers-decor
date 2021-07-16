/*
 * @file EdCatwalkTopAlignedBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Top aligned platforms, down-connection to poles.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
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

  public EdCatwalkTopAlignedBlock(long config, AbstractBlock.Properties properties, final VoxelShape[] variant_shapes)
  {
    super(config, properties, variant_shapes[0]);
    registerDefaultState(super.defaultBlockState().setValue(VARIANT, 0));
    this.variant_shapes = VARIANT.getPossibleValues().stream().map(i->(i<variant_shapes.length) ? (variant_shapes[i]) : (VoxelShapes.block())).collect(Collectors.toList());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return variant_shapes.get(state.getValue(VARIANT)); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(VARIANT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    BlockState state = adapted_state(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    if(context.getClickedFace() != Direction.UP) return state;
    BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
    if((state.getValue(VARIANT)==0) && (below.isFaceSturdy(context.getLevel(), context.getClickedPos().below(), Direction.UP))) return state.setValue(VARIANT, 3);
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if(item != this.asItem()) return ActionResultType.PASS;
    if(hit.getDirection().getAxis().isHorizontal()) return ActionResultType.PASS;
    BlockPos adjacent_pos = pos.relative(player.getDirection());
    BlockState adjacent_state = world.getBlockState(adjacent_pos);
    if(adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) {
      BlockState place_state = defaultBlockState();
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(adapted_state(place_state, world, adjacent_pos), world, adjacent_pos, player, hand, 1);
    }
    return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
  }

  @Override
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
  { return adapted_state(super.updateShape(state, facing, facingState, world, pos, facingPos), world, pos); }

  // ---

  private BlockState adapted_state(BlockState state, IWorld world, BlockPos pos)
  {
    BlockState below = world.getBlockState(pos.below());
    if((below == null) || (state == null)) return state;
    if((below.getBlock() == ModContent.THICK_STEEL_POLE) || (below.getBlock() == ModContent.THICK_STEEL_POLE_HEAD)) return state.setValue(VARIANT, 1);
    if((below.getBlock() == ModContent.THIN_STEEL_POLE) || (below.getBlock() == ModContent.THIN_STEEL_POLE_HEAD)) return state.setValue(VARIANT, 2);
    return state;
  }

}
