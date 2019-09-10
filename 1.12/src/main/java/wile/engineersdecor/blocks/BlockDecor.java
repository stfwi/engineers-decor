/*
 * @file BlockDecor.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 * Mainly needed for:
 * - MC block defaults.
 * - Tooltip functionality
 * - Model initialisation
 * - Accumulating "deprecated" warnings from Block where "overriding/implementing is fine".
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecor extends Block
{

  // The config combines some aspects of blocks, allowing to define different behaviour at construction time, without excessive polymorphy.
  // It's an old school flag set as it is used internally only and shall not have as littlt impact on performance as possible.
  public final long config;
  public static final long CFG_DEFAULT                    = 0x0000000000000000L; // no special config
  public static final long CFG_CUTOUT                     = 0x0000000000000001L; // cutout rendering
  public static final long CFG_HORIZIONTAL                = 0x0000000000000002L; // horizontal block, affects bounding box calculation at construction time and placement
  public static final long CFG_LOOK_PLACEMENT             = 0x0000000000000004L; // placed in direction the player is looking when placing.
  public static final long CFG_FACING_PLACEMENT           = 0x0000000000000008L; // placed on the facing the player has clicked.
  public static final long CFG_OPPOSITE_PLACEMENT         = 0x0000000000000010L; // placed placed in the opposite direction of the face the player clicked.
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = 0x0000000000000020L; // placement direction flipped if an instance of the same class was clicked
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = 0x0000000000000040L; // placement direction flipped if player is sneaking
  public static final long CFG_TRANSLUCENT                = 0x0000000000000080L; // indicates a block/pane is glass like (transparent, etc)
  public static final long CFG_LIGHT_VALUE_MASK           = 0x0000000000000f00L; // fixed value for getLightValue()
  public static final long CFG_LIGHT_VALUE_SHIFT          = 8L;
  public static final long CFG_ELECTRICAL                 = 0x0000000000010000L; // Denotes if a component is mainly flux driven.
  public static final long CFG_REDSTONE_CONTROLLED        = 0x0000000000020000L; // Denotes if a component has somehow a redstone control input
  public static final long CFG_ANALOG                     = 0x0000000000040000L; // Denotes if a component has analog behaviour
  public static final long CFG_HARD_IE_DEPENDENT          = 0x8000000000000000L; // Defines that this block abolutely needs IE to be installed.

  protected final AxisAlignedBB[] aabb;

  public BlockDecor(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  { this(registryName, config, material, hardness, resistance, sound, (new AxisAlignedBB[]{FULL_BLOCK_AABB})); }

  public BlockDecor(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nullable AxisAlignedBB boundingbox)
  { this(registryName, config, material, hardness, resistance, sound, (boundingbox==null) ? (new AxisAlignedBB[]{FULL_BLOCK_AABB}) : (new AxisAlignedBB[]{boundingbox})); }

  public BlockDecor(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB[] boundingboxes)
  {
    super((material!=null) ? (material) : (Material.IRON));
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setRegistryName(ModEngineersDecor.MODID, registryName);
    setTranslationKey(ModEngineersDecor.MODID + "." + registryName);
    setTickRandomly(false);
    setHardness((hardness > 0) ? hardness : 5.0f);
    setResistance((resistance > 0) ? resistance : 10.0f);
    setSoundType((sound==null) ? SoundType.STONE : sound);
    if(((config & (CFG_CUTOUT|CFG_TRANSLUCENT))!=0)) setLightOpacity(0);
    if((config & CFG_TRANSLUCENT) != 0) this.translucent = true;
    this.config = config;
    this.aabb = boundingboxes;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return ((config & CFG_CUTOUT)!=0) ? (BlockRenderLayer.CUTOUT) : ( ((config & CFG_TRANSLUCENT)!=0) ? (BlockRenderLayer.TRANSLUCENT) : (BlockRenderLayer.SOLID)); }

  @Override
  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.SOLID; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return ((config & (CFG_CUTOUT|CFG_TRANSLUCENT))==0); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return ((config & (CFG_CUTOUT|CFG_TRANSLUCENT))==0); }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player)
  { return true; }

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
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return aabb[0]; }

  @SideOnly(Side.CLIENT)
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos)
  { return state.getBoundingBox(world, pos).offset(pos); }

  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
  {
    addCollisionBoxToList(pos, entityBox, collidingBoxes, getBoundingBox(state, world, pos));
    for(int i=1; i<aabb.length; ++i) addCollisionBoxToList(pos, entityBox, collidingBoxes, aabb[i]);
  }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
  {}
}
