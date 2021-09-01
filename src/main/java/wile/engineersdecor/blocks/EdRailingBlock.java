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
  public EdRailingBlock(long config, AbstractBlock.Properties properties, final AxisAlignedBB base_aabb, final AxisAlignedBB railing_aabb)
  { super(config, properties, base_aabb, railing_aabb, 0); }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canBeReplaced(BlockState state, BlockItemUseContext useContext)
  { return (useContext.getItemInHand().getItem() == asItem()) ? true : super.canBeReplaced(state, useContext); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    if(context.getClickedFace() != Direction.UP) return null;
    BlockState state = context.getLevel().getBlockState(context.getClickedPos());
    if(state.getBlock() != this) state = super.getStateForPlacement(context);
    final Vector3d rhv = context.getClickLocation().subtract(Vector3d.atCenterOf(context.getClickedPos()));
    BooleanProperty side = getDirectionProperty(Direction.getNearest(rhv.x, 0, rhv.z));
    return state.setValue(side, true);
  }

  @Override
  @SuppressWarnings("deprecation")
  public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if(player.getItemInHand(hand).getItem() != asItem()) return ActionResultType.PASS;
    Direction face = hit.getDirection();
    if(!face.getAxis().isHorizontal()) return ActionResultType.sidedSuccess(world.isClientSide());
    final Vector3d rhv = hit.getLocation().subtract(Vector3d.atCenterOf(hit.getBlockPos()));
    if(rhv.multiply(Vector3d.atLowerCornerOf(face.getNormal())).scale(2).lengthSqr() < 0.99) face = face.getOpposite(); // click on railing, not the outer side.
    BooleanProperty railing = getDirectionProperty(face);
    boolean add = (!state.getValue(railing));
    state = state.setValue(railing, add);
    if((!state.getValue(NORTH)) && (!state.getValue(EAST)) && (!state.getValue(SOUTH)) && (!state.getValue(WEST))) {
      state = (world.getFluidState(pos).getType() == Fluids.WATER) ? Blocks.WATER.defaultBlockState() : (Blocks.AIR.defaultBlockState());
      EdCatwalkBlock.place_consume(state, world, pos, player, hand, add ? 1 : -1);
    } else {
      EdCatwalkBlock.place_consume(state, world, pos, player, hand, add ? 1 : -1);
    }
    return ActionResultType.sidedSuccess(world.isClientSide());
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
    int n = (state.getValue(NORTH)?1:0)+(state.getValue(EAST)?1:0)+(state.getValue(SOUTH)?1:0)+(state.getValue(WEST)?1:0);
    drops.add(new ItemStack(state.getBlock().asItem(), Math.max(n, 1)));
    return drops;
  }

}
