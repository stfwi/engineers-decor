/*
 * @file BlockDecorSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import wile.engineersdecor.detail.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;


public class BlockDecorSlab extends BlockDecor
{
  public static final PropertyInteger PARTS = PropertyInteger.create("parts", 0, 2);
  public static final PropertyInteger TEXTURE_VARIANT = PropertyInteger.create("tvariant", 0, 7);

  protected static final AxisAlignedBB AABBs[] = {
    new AxisAlignedBB(0,  0./16, 0, 1,  8./16, 1), // bottom slab
    new AxisAlignedBB(0,  8./16, 0, 1, 16./16, 1), // top slab
    new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1), // both slabs
    new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1)  // << 2bit fill
  };
  protected static final int num_slabs_contained_in_parts_[] = { 1,1,2,2 };

  public BlockDecorSlab(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  { super(registryName, config, material, hardness, resistance, sound); }

  protected boolean is_cube(IBlockState state)
  { return state.getValue(PARTS) >= 2; }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
  {
    if(!ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    if(!ModConfig.optout.without_direct_slab_pickup) {
      ModAuxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", "engineersdecor.tooltip.slabpickup", tooltip, flag, true);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return (((config & CFG_TRANSLUCENT)!=0) ? (BlockRenderLayer.TRANSLUCENT) : (BlockRenderLayer.CUTOUT)); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(PARTS, MathHelper.clamp(meta, 0, 2)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return state.getValue(PARTS); }

  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
  {
    long prnd = pos.toLong(); prnd = (prnd>>29) ^ (prnd>>17) ^ (prnd>>9) ^ (prnd>>4) ^ pos.getX() ^ pos.getY() ^ pos.getZ();
    return state.withProperty(TEXTURE_VARIANT, ((int)prnd) & 0x7);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, PARTS, TEXTURE_VARIANT); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return ((config & CFG_TRANSLUCENT)==0) && is_cube(state); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return (state.getValue(PARTS) >= 2) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED; }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs[state.getValue(PARTS) & 0x3]; }

  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos)
  { return getBoundingBox(state, world, pos); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isFullCube(IBlockState state)
  { return is_cube(state); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return is_cube(state); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(IBlockState state, Entity entity)
  { return false; }

  @Override
  public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack)
  { spawnAsEntity(world, pos, new ItemStack(Item.getItemFromBlock(this), num_slabs_contained_in_parts_[state.getValue(PARTS) & 0x3])); }

  @Override
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return world.getBlockState(pos).getBlock() != this; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(PARTS, ((facing==EnumFacing.UP) || ((facing!=EnumFacing.DOWN) && (hitY < 0.6))) ? 0 : 1); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    final ItemStack stack = player.getHeldItem(hand);
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return false;
    int parts = state.getValue(PARTS);
    if(((facing == EnumFacing.UP) && (parts == 0)) || ((facing == EnumFacing.DOWN) && (parts == 1))) {
      world.setBlockState(pos, state.withProperty(PARTS, 2), 3);
    } else {
      return false; // "not handled" -> let parent decide if a new slab has to be placed on top/bottom.
    }
    if(world.isRemote) return true;
    if(!player.isCreative()) {
      stack.shrink(1);
      if(player.inventory != null) player.inventory.markDirty();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(null, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
    return true;
  }

  @Override
  public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
  {
    if((world.isRemote) || (ModConfig.optout.without_direct_slab_pickup)) return;
    final ItemStack stack = player.getHeldItemMainhand();
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vec3d lv = player.getLookVec();
    EnumFacing facing = EnumFacing.getFacingFromVector((float)lv.x, (float)lv.y, (float)lv.z);
    if((facing != EnumFacing.UP) && (facing != EnumFacing.DOWN)) return;
    IBlockState state = world.getBlockState(pos);
    if(state.getBlock() != this) return;
    int parts = state.getValue(PARTS);
    if(facing == EnumFacing.DOWN) {
      if(parts == 2) {
        world.setBlockState(pos, state.withProperty(PARTS, 0), 3);
      } else {
        world.setBlockToAir(pos);
      }
    } else if(facing == EnumFacing.UP) {
      if(parts == 2) {
        world.setBlockState(pos, state.withProperty(PARTS, 1), 3);
      } else {
        world.setBlockToAir(pos);
      }
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.markDirty(); // @todo: check if inventory can actually be null
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }
}
