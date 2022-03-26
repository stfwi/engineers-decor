/*
 * @file EdCatwalkTopAlignedBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Top aligned platforms, down-connection to poles.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;


public class EdCatwalkTopAlignedBlock extends StandardBlocks.WaterLoggable
{
  public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
  protected final List<VoxelShape> variant_shapes;

  public EdCatwalkTopAlignedBlock(long config, BlockBehaviour.Properties properties, final VoxelShape[] variant_shapes)
  {
    super(config, properties, variant_shapes[0]);
    registerDefaultState(super.defaultBlockState().setValue(VARIANT, 0));
    this.variant_shapes = VARIANT.getPossibleValues().stream().map(i->(i<variant_shapes.length) ? (variant_shapes[i]) : (Shapes.block())).collect(Collectors.toList());
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return true; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return variant_shapes.get(state.getValue(VARIANT)); }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(VARIANT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    BlockState state = adapted_state(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    if(context.getClickedFace() != Direction.UP) return state;
    BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
    if((state.getValue(VARIANT)==0) && (below.isFaceSturdy(context.getLevel(), context.getClickedPos().below(), Direction.UP))) return state.setValue(VARIANT, 3);
    return state;
  }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if(item != this.asItem()) return InteractionResult.PASS;
    if(hit.getDirection().getAxis().isHorizontal()) return InteractionResult.PASS;
    BlockPos adjacent_pos = pos.relative(player.getDirection());
    BlockState adjacent_state = world.getBlockState(adjacent_pos);
    if(adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) {
      BlockState place_state = defaultBlockState();
      place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
      EdCatwalkBlock.place_consume(adapted_state(place_state, world, adjacent_pos), world, adjacent_pos, player, hand, 1);
    }
    return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
  }

  @Override
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  { return adapted_state(super.updateShape(state, facing, facingState, world, pos, facingPos), world, pos); }

  // ---

  private BlockState adapted_state(BlockState state, LevelAccessor world, BlockPos pos)
  {
    BlockState below = world.getBlockState(pos.below());
    if((below == null) || (state == null)) return state;
    if((below.getBlock() == ModContent.getBlock("thick_steel_pole")) || (below.getBlock() == ModContent.getBlock("thick_steel_pole_head"))) return state.setValue(VARIANT, 1);
    if((below.getBlock() == ModContent.getBlock("thin_steel_pole")) || (below.getBlock() == ModContent.getBlock("thin_steel_pole_head"))) return state.setValue(VARIANT, 2);
    return state;
  }

}
