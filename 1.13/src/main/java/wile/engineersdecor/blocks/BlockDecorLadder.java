/*
 * @file BlockDecorLadder.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Ladder block. The implementation is based on the vanilla
 * net.minecraft.block.BlockLadder. Minor changes to enable
 * later configuration (for block list based construction
 * time configuration), does not drop when the block behind
 * is broken, etc.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.*;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorLadder extends BlockLadder
{
  protected static final AxisAlignedBB EDLADDER_UNROTATED_AABB = ModAuxiliaries.getPixeledAABB(3, 0, 0, 13, 16, 2);
  protected static final VoxelShape EDLADDER_SOUTH_AABB =  VoxelShapes.create(ModAuxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, EnumFacing.SOUTH, false));
  protected static final VoxelShape EDLADDER_EAST_AABB  = VoxelShapes.create(ModAuxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, EnumFacing.EAST, false));
  protected static final VoxelShape EDLADDER_WEST_AABB  = VoxelShapes.create(ModAuxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, EnumFacing.WEST, false));
  protected static final VoxelShape EDLADDER_NORTH_AABB = VoxelShapes.create(ModAuxiliaries.getRotatedAABB(EDLADDER_UNROTATED_AABB, EnumFacing.NORTH, false));

  public BlockDecorLadder(long config, Block.Properties builder)
  { super(builder); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  public VoxelShape getShape(IBlockState state, IBlockReader worldIn, BlockPos pos)
  {
    switch ((EnumFacing)state.get(FACING)) {
      case NORTH: return EDLADDER_NORTH_AABB;
      case SOUTH: return EDLADDER_SOUTH_AABB;
      case WEST: return EDLADDER_WEST_AABB;
      default: return EDLADDER_EAST_AABB;
    }
  }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(IBlockState state)
  { return EnumPushReaction.NORMAL; }

}
