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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EdCatwalkTopAlignedBlock extends StandardBlocks.WaterLoggable
{
  public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);
  protected final List<VoxelShape> variant_shapes;
  protected final Block inset_light_block;

  public EdCatwalkTopAlignedBlock(long config, BlockBehaviour.Properties properties, final VoxelShape[] variant_shapes, final Block inset_light_block)
  {
    super(config, properties, variant_shapes[0]);
    registerDefaultState(super.defaultBlockState().setValue(VARIANT, 0));
    this.variant_shapes = VARIANT.getPossibleValues().stream().map(i->(i<variant_shapes.length) ? (variant_shapes[i]) : (Shapes.block())).collect(Collectors.toList());
    this.inset_light_block = inset_light_block;
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
    if((!(item instanceof BlockItem))) return InteractionResult.PASS;
    final Block block = ((BlockItem)item).getBlock();
    if(block == this) {
      if (hit.getDirection().getAxis().isHorizontal()) return InteractionResult.PASS;
      BlockPos adjacent_pos = pos.relative(player.getDirection());
      BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if (adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) {
        BlockState place_state = defaultBlockState();
        place_state = place_state.setValue(WATERLOGGED, adjacent_state.getFluidState().getType() == Fluids.WATER);
        EdCatwalkBlock.place_consume(adapted_state(place_state, world, adjacent_pos), world, adjacent_pos, player, hand, 1);
      }
      return InteractionResult.sidedSuccess(world.isClientSide());
    }
    if(block == inset_light_block && hit.getDirection() == Direction.DOWN) {
      int currentVariant = state.getValue(VARIANT);
      if (!(currentVariant == 0 || currentVariant == 4)) return InteractionResult.PASS;
      boolean add = currentVariant == 0;
      EdCatwalkBlock.place_consume(adapted_state(state.setValue(VARIANT, add ? 4 : 0), world, pos), world, pos, player, hand, add ? 1 : -1);
      return InteractionResult.sidedSuccess(world.isClientSide());
    }
    return InteractionResult.PASS;
  }

  @Override
  public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos pos, BlockPos facingPos)
  { return adapted_state(super.updateShape(state, facing, facingState, world, pos, facingPos), world, pos); }

  // ---

  private BlockState adapted_state(BlockState state, LevelAccessor world, BlockPos pos)
  {
    BlockState below = world.getBlockState(pos.below());
    if (state.getValue(VARIANT) == 4) return state;
    if((below.getBlock() == ModContent.getBlock("thick_steel_pole")) || (below.getBlock() == ModContent.getBlock("thick_steel_pole_head"))) return state.setValue(VARIANT, 1);
    if((below.getBlock() == ModContent.getBlock("thin_steel_pole")) || (below.getBlock() == ModContent.getBlock("thin_steel_pole_head"))) return state.setValue(VARIANT, 2);
    return state;
  }

  @Override
  public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
    return state.getValue(VARIANT) == 4
            ? inset_light_block.getLightEmission(inset_light_block.defaultBlockState().setValue(StandardBlocks.Directed.FACING, Direction.UP), level, pos)
            : super.getLightEmission(state, level, pos);
  }

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, Level world, @Nullable BlockEntity te, boolean explosion)
  {
    if(world.isClientSide()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    if (state.getValue(VARIANT) == 4) drops.add(new ItemStack(inset_light_block, 1));
    return drops;
  }

}
