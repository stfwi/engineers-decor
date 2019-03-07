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

import net.minecraft.block.BlockLadder;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorLadder extends BlockLadder
{
  protected static final AxisAlignedBB EDLADDER_SOUTH_AABB = ModAuxiliaries.getPixeledAABB(3, 0, 0, 13, 16, 3);
  protected static final AxisAlignedBB EDLADDER_EAST_AABB  = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.EAST, false);
  protected static final AxisAlignedBB EDLADDER_WEST_AABB  = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.WEST, false);
  protected static final AxisAlignedBB EDLADDER_NORTH_AABB = ModAuxiliaries.getRotatedAABB(EDLADDER_SOUTH_AABB, EnumFacing.NORTH, false);


  public BlockDecorLadder(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super();
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setRegistryName(ModEngineersDecor.MODID, registryName);
    setTranslationKey(ModEngineersDecor.MODID + "." + registryName);
    setTickRandomly(false);
    setHardness((hardness > 0) ? hardness : 5.0f);
    setResistance((resistance > 0) ? resistance : 10.0f);
    setSoundType((sound==null) ? SoundType.STONE : sound);
    setLightOpacity(0);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  {
    switch ((EnumFacing)state.getValue(FACING)) {
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

  @Override
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return canAttachTo(world, pos.west(), side) || canAttachTo(world, pos.east(), side) || canAttachTo(world, pos.north(), side) || canAttachTo(world, pos.south(), side); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  {
    if(facing.getAxis().isHorizontal() && canAttachTo(world, pos.offset(facing.getOpposite()), facing)) return this.getDefaultState().withProperty(FACING, facing);
    for(EnumFacing e:EnumFacing.Plane.HORIZONTAL) {
      if(this.canAttachTo(world, pos.offset(e.getOpposite()), e)) return this.getDefaultState().withProperty(FACING, e);
    }
    return this.getDefaultState();
  }

  private boolean canAttachTo(World world, BlockPos pos, EnumFacing side)
  {
    final IBlockState state = world.getBlockState(pos);
    return (!isExceptBlockForAttachWithPiston(state.getBlock())) && (state.getBlockFaceShape(world, pos, side) == BlockFaceShape.SOLID);
  }

}
