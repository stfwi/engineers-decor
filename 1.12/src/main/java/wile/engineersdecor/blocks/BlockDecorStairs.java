/*
 * @file BlockDecorStairs.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stairs and roof blocks, almost entirely based on vanilla stairs.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorStairs extends net.minecraft.block.BlockStairs
{
  public BlockDecorStairs(@Nonnull String registryName, IBlockState modelState)
  {
    super(modelState);
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setRegistryName(ModEngineersDecor.MODID, registryName);
    setTranslationKey(ModEngineersDecor.MODID + "." + registryName);
    setLightLevel(0);
    setLightOpacity(64);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
  { return 0; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(IBlockState state)
  { return EnumPushReaction.NORMAL; }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return false; }

}
