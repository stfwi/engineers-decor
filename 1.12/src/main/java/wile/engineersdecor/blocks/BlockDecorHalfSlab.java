/*
 * @file BlockDecorHalfSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Half slab characteristics class.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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


public class BlockDecorHalfSlab extends BlockDecor
{
  public static final PropertyInteger PARTS = PropertyInteger.create("parts", 0, 14);

  protected static final AxisAlignedBB AABBs[] = {
    new AxisAlignedBB(0,  0./16, 0, 1,  2./16, 1), new AxisAlignedBB(0,  0./16, 0, 1,  4./16, 1),
    new AxisAlignedBB(0,  0./16, 0, 1,  6./16, 1), new AxisAlignedBB(0,  0./16, 0, 1,  8./16, 1),
    new AxisAlignedBB(0,  0./16, 0, 1, 10./16, 1), new AxisAlignedBB(0,  0./16, 0, 1, 12./16, 1),
    new AxisAlignedBB(0,  0./16, 0, 1, 14./16, 1), new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1),
    new AxisAlignedBB(0,  2./16, 0, 1, 16./16, 1), new AxisAlignedBB(0,  4./16, 0, 1, 16./16, 1),
    new AxisAlignedBB(0,  6./16, 0, 1, 16./16, 1), new AxisAlignedBB(0,  8./16, 0, 1, 16./16, 1),
    new AxisAlignedBB(0, 10./16, 0, 1, 16./16, 1), new AxisAlignedBB(0, 12./16, 0, 1, 16./16, 1),
    new AxisAlignedBB(0, 14./16, 0, 1, 16./16, 1), new AxisAlignedBB(0,0,0,1,1,1), // <- with 4bit fill
  };
  protected static final int num_slabs_contained_in_parts_[] = {
    1,2,3,4,5,6,7,8,7,6,5,4,3,2,1  ,0x1 // <- with 4bit fill
  };

  public BlockDecorHalfSlab(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound)
  { super(registryName, config, material, hardness, resistance, sound); }

  protected boolean is_cube(IBlockState state)
  { return state.getValue(PARTS) == 0x07; }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return (((config & CFG_TRANSLUCENT)!=0) ? (BlockRenderLayer.TRANSLUCENT) : (BlockRenderLayer.CUTOUT)); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(PARTS, MathHelper.clamp(meta, 0,14)); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return state.getValue(PARTS); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, PARTS); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isOpaqueCube(IBlockState state)
  { return ((config & CFG_TRANSLUCENT)==0) && is_cube(state); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  {
    final int parts = state.getValue(PARTS);
    switch(face) {
      case UP:
        if(parts >= 0x07) return BlockFaceShape.SOLID;
        break;
      case DOWN:
        if(parts <= 0x07) return BlockFaceShape.SOLID;
        break;
      default:
        if((parts > 0x05) && (parts < 0x0a)) return BlockFaceShape.SOLID;
    }
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
  { return AABBs[state.getValue(PARTS) & 0xf]; }

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
  { spawnAsEntity(world, pos, new ItemStack(Item.getItemFromBlock(this), num_slabs_contained_in_parts_[state.getValue(PARTS) & 0xf])); }

  @Override
  public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side)
  { return world.getBlockState(pos).getBlock() != this; }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(PARTS, ((facing==EnumFacing.UP) || ((facing!=EnumFacing.DOWN) && (hitY < 0.6))) ? 0 : 14); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    final ItemStack stack = player.getHeldItem(hand);
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return false;
    if((facing != EnumFacing.UP) && (facing != EnumFacing.DOWN)) return false;
    int parts = state.getValue(PARTS);
    if((facing != EnumFacing.UP) && (parts > 7)) {
      world.setBlockState(pos, state.withProperty(PARTS, parts-1), 3);
    } else if((facing != EnumFacing.DOWN) && (parts < 7)) {
      world.setBlockState(pos, state.withProperty(PARTS, parts+1), 3);
    } else {
      return (parts != 7);
    }
    if(world.isRemote) return true;
    if(!player.isCreative()) {
      stack.shrink(1);
      if(player.inventory != null) player.inventory.markDirty(); // @todo: check if inventory can actually be null
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(null, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
    return true;
  }

  @Override
  public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
  {
    if(world.isRemote) return;
    final ItemStack stack = player.getHeldItemMainhand();
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vec3d lv = player.getLookVec();
    EnumFacing facing = EnumFacing.getFacingFromVector((float)lv.x, (float)lv.y, (float)lv.z);
    if((facing != EnumFacing.UP) && (facing != EnumFacing.DOWN)) return;
    IBlockState state = world.getBlockState(pos);
    if(state.getBlock() != this) return;
    int parts = state.getValue(PARTS);
    if((facing == EnumFacing.DOWN) && (parts <= 7)) {
      if(parts > 0) {
        world.setBlockState(pos, state.withProperty(PARTS, parts-1), 3);
      } else {
        world.setBlockToAir(pos);
      }
    } else if((facing == EnumFacing.UP) && (parts >= 7)) {
      if(parts < 14) {
        world.setBlockState(pos, state.withProperty(PARTS, parts + 1), 3);
      } else {
        world.setBlockToAir(pos);
      }
    } else {
      return;
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.markDirty(); // @todo: check if inventory can actually be null
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }
}
