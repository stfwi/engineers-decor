/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 * Mainly needed for:
 * - MC block defaults.
 * - Tooltip functionality
 * - Model initialisation
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.util.*;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockDecor extends Block
{

  public final long config; // the config combines some aspects of blocks, allowing to define different behaviour at construction time, without excessive polymorphy.
  public static final long CFG_CUTOUT                 = 0x0000000000000001L; // cutout rendering
  public static final long CFG_HORIZIONTAL            = 0x0000000000000002L; // horizontal block, affects bounding box calculation at construction time
  public static final long CFG_HORIZIONTAL_PLACEMENT  = 0x0000000000000004L; // placed in the horizontzal direction the player is looking when placing.

  public BlockDecor(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  {
    super((material!=null) ? (material) : (Material.IRON));
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setRegistryName(ModEngineersDecor.MODID, registryName);
    setTranslationKey(ModEngineersDecor.MODID + "." + registryName);
    setTickRandomly(false);
    setHardness((hardness > 0) ? hardness : 5.0f);
    setResistance((resistance > 0) ? resistance : 10.0f);
    setSoundType((sound==null) ? SoundType.STONE : sound);
    this.config = config;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return ((config & CFG_CUTOUT)!=0) ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(IBlockState state)
  { return EnumPushReaction.NORMAL; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState(); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return 0; }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  { return state; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState withRotation(IBlockState state, Rotation rot)
  { return state; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return false; }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {}

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  { return false; }

  @Override
  public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
  {}

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
  {}

  @Override
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return super.canPlaceBlockOnSide(world, pos, side); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand); }

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  { return super.removedByPlayer(state, world, pos, player, willHarvest); }

}
