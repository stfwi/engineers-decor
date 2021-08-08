/*
 * @file EdCatwalkBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Bottom aligned platforms with railings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EdCatwalkBlock extends StandardBlocks.HorizontalFourWayWaterLoggable
{
  final Block railing_block;
  final AABB base_aabb;

  public EdCatwalkBlock(long config, BlockBehaviour.Properties properties, final AABB base_aabb, final AABB railing_aabb, final Block railing_block)
  { super(config, properties, base_aabb, railing_aabb, 0); this.railing_block = railing_block; this.base_aabb=base_aabb; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  { return super.getStateForPlacement(context).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false); }

  public static boolean place_consume(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, int shrink)
  {
    if(!world.setBlock(pos, state, 1|2)) return false;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1f, 1f);
    if((!player.isCreative()) && (!world.isClientSide())) {
      ItemStack stack = player.getItemInHand(hand);
      if(shrink >= 0) {
        stack.shrink(shrink);
      } else if(stack.getCount() < stack.getMaxStackSize()) {
        stack.grow(Math.abs(shrink));
      } else {
        Inventories.give(player, new ItemStack(stack.getItem(), Math.abs(shrink)));
      }
      Inventories.setItemInPlayerHand(player, hand, stack);
    }
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if((!(item instanceof BlockItem))) return InteractionResult.PASS;
    final Block block = ((BlockItem)item).getBlock();
    if(block == this) {
      if(hit.getDirection().getAxis().isHorizontal()) return InteractionResult.PASS; // place new block on the clicked side.
      BlockPos adjacent_pos = pos.relative(player.getDirection());
      BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) {
        BlockState place_state = defaultBlockState();
        place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
        place_consume(place_state, world, adjacent_pos, player, hand, 1);
      }
      return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
    if(block == railing_block) {
      Direction face = hit.getDirection();
      final Vec3 rhv = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
      if(face.getAxis().isHorizontal()) {
        // Side or railing clicked
        if(rhv.multiply(Vec3.atLowerCornerOf(face.getNormal())).scale(2).lengthSqr() < 0.99) face = face.getOpposite(); // click on railing, not the outer side.
      } else if(player.distanceToSqr(Vec3.atCenterOf(pos)) < 3) {
        // near accurate placement
        face = Direction.getNearest(rhv.x, 0, rhv.z);
      } else {
        // far automatic placement
        face = Direction.getNearest(player.getLookAngle().x, 0, player.getLookAngle().z);
        List<Direction> free_sides = Arrays.stream(Direction.values()).filter(d->d.getAxis().isHorizontal() && (world.getBlockState(pos.relative(d)).getBlock()!=this)).collect(Collectors.toList());
        if(free_sides.isEmpty()) return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        if(!free_sides.contains(face)) face = free_sides.get(0);
      }
      BooleanProperty railing = getDirectionProperty(face);
      boolean add = (!state.getValue(railing));
      place_consume(state.setValue(railing, add), world, pos, player, hand, add ? 1 : -1);
      return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
    return InteractionResult.PASS;
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
    int n = (state.getValue(NORTH)?1:0)+(state.getValue(EAST)?1:0)+(state.getValue(SOUTH)?1:0)+(state.getValue(WEST)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.STEEL_RAILING, n));
    return drops;
  }

}
