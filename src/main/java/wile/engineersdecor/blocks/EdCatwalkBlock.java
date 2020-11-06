/*
 * @file EdCatwalkBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Bottom aligned platforms with railings.
 */
package wile.engineersdecor.blocks;

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

  public EdCatwalkBlock(long config, Block.Properties properties, final AxisAlignedBB base_aabb, final AxisAlignedBB railing_aabb, final Block railing_block)
  { super(config, properties, base_aabb, railing_aabb); this.railing_block = railing_block; this.base_aabb=base_aabb; }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context).with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false); }

  public static boolean place_consume(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, int shrink)
  {
    if(!world.setBlockState(pos, state, 1|2)) return false;
    world.playSound(player, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 1f, 1f);
    if(!player.isCreative()) {
      ItemStack stack = player.getHeldItem(hand);
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
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    final Item item = player.getHeldItem(hand).getItem();
    if((!(item instanceof BlockItem))) return ActionResultType.PASS;
    final Block block = ((BlockItem)item).getBlock();
    if(block == this) {
      if(hit.getFace().getAxis().isHorizontal()) return ActionResultType.PASS; // place new block on the clicked side.
      BlockPos adjacent_pos = pos.offset(player.getHorizontalFacing());
      BlockState adjacent_state = world.getBlockState(adjacent_pos);
      if(adjacent_state.isReplaceable(new DirectionalPlaceContext(world, adjacent_pos, hit.getFace().getOpposite(), player.getHeldItem(hand), hit.getFace()))) {
        BlockState place_state = getDefaultState();
        place_state = place_state.with(WATERLOGGED,adjacent_state.getFluidState().getFluid()==Fluids.WATER);
        place_consume(place_state, world, adjacent_pos, player, hand, 1);
      }
      return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    }
    if(block == railing_block) {
      Direction face = hit.getFace();
      final Vector3d rhv = hit.getHitVec().subtract(Vector3d.copyCentered(hit.getPos()));
      if(face.getAxis().isHorizontal()) {
        // Side or railing clicked
        if(rhv.mul(Vector3d.copy(face.getDirectionVec())).scale(2).lengthSquared() < 0.99) face = face.getOpposite(); // click on railing, not the outer side.
      } else if(player.getDistanceSq(Vector3d.copyCentered(pos)) < 3) {
        // near accurate placement
        face = Direction.getFacingFromVector(rhv.x, 0, rhv.z);
      } else {
        // far automatic placement
        face = Direction.getFacingFromVector(player.getLookVec().x, 0, player.getLookVec().z);
        List<Direction> free_sides = Arrays.stream(Direction.values()).filter(d->d.getAxis().isHorizontal() && (world.getBlockState(pos.offset(d)).getBlock()!=this)).collect(Collectors.toList());
        if(free_sides.isEmpty()) return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
        if(!free_sides.contains(face)) face = free_sides.get(0);
      }
      BooleanProperty railing = getDirectionProperty(face);
      boolean add = (!state.get(railing));
      place_consume(state.with(railing, add), world, pos, player, hand, add ? 1 : -1);
      return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
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
    if(world.isRemote()) return Collections.singletonList(ItemStack.EMPTY);
    List<ItemStack> drops = new ArrayList<>();
    drops.add(new ItemStack(state.getBlock().asItem()));
    int n = (state.get(NORTH)?1:0)+(state.get(EAST)?1:0)+(state.get(SOUTH)?1:0)+(state.get(WEST)?1:0);
    if(n > 0) drops.add(new ItemStack(ModContent.STEEL_RAILING, n));
    return drops;
  }

}
