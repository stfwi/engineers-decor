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
import net.minecraft.block.Blocks;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdRailingBlock extends DecorBlock.HorizontalFourWayWaterLoggable implements IDecorBlock
{
  public EdRailingBlock(long config, Block.Properties properties, final AxisAlignedBB base_aabb, final AxisAlignedBB railing_aabb)
  { super(config, properties, base_aabb, railing_aabb); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isReplaceable(BlockState state, BlockItemUseContext useContext)
  { return (useContext.getItem().getItem() == asItem()) ? true : super.isReplaceable(state, useContext); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    if(context.getFace() != Direction.UP) return null;
    BlockState state = context.getWorld().getBlockState(context.getPos());
    if(state.getBlock() != this) state = super.getStateForPlacement(context);
    final Vector3d rhv = context.getHitVec().subtract(Vector3d.copyCentered(context.getPos()));
    BooleanProperty side = getDirectionProperty(Direction.getFacingFromVector(rhv.x, 0, rhv.z));
    return state.with(side, true);
  }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if(player.getHeldItem(hand).getItem() != asItem()) return ActionResultType.PASS;
    Direction face = hit.getFace();
    if(!face.getAxis().isHorizontal()) return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
    final Vector3d rhv = hit.getHitVec().subtract(Vector3d.copyCentered(hit.getPos()));
    if(rhv.mul(Vector3d.copy(face.getDirectionVec())).scale(2).lengthSquared() < 0.99) face = face.getOpposite(); // click on railing, not the outer side.
    BooleanProperty railing = getDirectionProperty(face);
    boolean add = (!state.get(railing));
    state = state.with(railing, add);
    if((!state.get(NORTH)) && (!state.get(EAST)) && (!state.get(SOUTH)) && (!state.get(WEST))) {
      state = (world.getFluidState(pos).getFluid() == Fluids.WATER) ? Blocks.WATER.getDefaultState() : (Blocks.AIR.getDefaultState());
      EdCatwalkBlock.place_consume(state, world, pos, player, hand, add ? 1 : -1);
    } else {
      EdCatwalkBlock.place_consume(state, world, pos, player, hand, add ? 1 : -1);
    }
    return world.isRemote() ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
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
    int n = (state.get(NORTH)?1:0)+(state.get(EAST)?1:0)+(state.get(SOUTH)?1:0)+(state.get(WEST)?1:0);
    drops.add(new ItemStack(state.getBlock().asItem(), Math.max(n, 1)));
    return drops;
  }

}
