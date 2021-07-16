/*
 * @file EdCatwalkBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Bottom aligned platforms with railings.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Inventories;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class EdCatwalkBlock extends DecorBlock.HorizontalFourWayWaterLoggable implements IDecorBlock
{
  final Block railing_block;
  final AxisAlignedBB base_aabb;

  public EdCatwalkBlock(long config, AbstractBlock.Properties properties, final AxisAlignedBB base_aabb, final AxisAlignedBB railing_aabb, final Block railing_block)
  { super(config, properties, base_aabb, railing_aabb, 0); this.railing_block = railing_block; this.base_aabb=base_aabb; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false); }

  public static boolean place_consume(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, int shrink)
  {
    if(!world.setBlock(pos, state, 1|2)) return false;
    world.playSound(player, pos, SoundEvents.METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
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
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getItemInHand(hand).getItem();
    if((!(item instanceof BlockItem))) return ActionResultType.PASS;
    final Block block = ((BlockItem)item).getBlock();
    if(block == this) {
      if(hit.getDirection().getAxis().isHorizontal()) return ActionResultType.PASS; // place new block on the clicked side.
      BlockPos adjacent_pos = pos.relative(player.getDirection());
      BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state.canBeReplaced(new DirectionalPlaceContext(world, adjacent_pos, hit.getDirection().getOpposite(), player.getItemInHand(hand), hit.getDirection()))) {
        BlockState place_state = defaultBlockState();
        place_state = place_state.setValue(WATERLOGGED,adjacent_state.getFluidState().getType()==Fluids.WATER);
        place_consume(place_state, world, adjacent_pos, player, hand, 1);
      }
      return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }
    if(block == railing_block) {
      Direction face = hit.getDirection();
      final Vector3d rhv = hit.getLocation().subtract(Vector3d.atCenterOf(hit.getBlockPos()));
      if(face.getAxis().isHorizontal()) {
        // Side or railing clicked
        if(rhv.multiply(Vector3d.atLowerCornerOf(face.getNormal())).scale(2).lengthSqr() < 0.99) face = face.getOpposite(); // click on railing, not the outer side.
      } else if(player.distanceToSqr(Vector3d.atCenterOf(pos)) < 3) {
        // near accurate placement
        face = Direction.getNearest(rhv.x, 0, rhv.z);
      } else {
        // far automatic placement
        face = Direction.getNearest(player.getLookAngle().x, 0, player.getLookAngle().z);
        List<Direction> free_sides = Arrays.stream(Direction.values()).filter(d->d.getAxis().isHorizontal() && (world.getBlockState(pos.relative(d)).getBlock()!=this)).collect(Collectors.toList());
        if(free_sides.isEmpty()) return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        if(!free_sides.contains(face)) face = free_sides.get(0);
      }
      BooleanProperty railing = getDirectionProperty(face);
      boolean add = (!state.getValue(railing));
      place_consume(state.setValue(railing, add), world, pos, player, hand, add ? 1 : -1);
      return world.isClientSide() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }
    return ActionResultType.PASS;
  }

  // -- IDecorBlock

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, World world, @Nullable TileEntity te, boolean explosion)
  {
    if(world.isClientSide()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    int n = (state.getValue(NORTH)?1:0)+(state.getValue(EAST)?1:0)+(state.getValue(SOUTH)?1:0)+(state.getValue(WEST)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.STEEL_RAILING, n));
    return drops;
  }

}
