/*
 * @file BlockDecorWindow.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod windows.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockDecorWindow extends BlockDecorDirected
{
  public BlockDecorWindow(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  @SideOnly(Side.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.TRANSLUCENT; }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
  { return (layer==BlockRenderLayer.CUTOUT_MIPPED) || (layer==BlockRenderLayer.TRANSLUCENT); }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
  { return true; }

  @Override
  public boolean isFullCube(IBlockState state)
  { return false; }

  @Override
  public boolean isNormalCube(IBlockState state)
  { return false; }

  @Override
  public boolean isOpaqueCube(IBlockState state)
  { return false; }

  @Override
  public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face)
  { return false; }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  {
    facing = placer.getHorizontalFacing();
    if(Math.abs(placer.getLookVec().y) > 0.9) {
      facing = EnumFacing.getDirectionFromEntityLiving(pos, placer);
    } else {
      for(EnumFacing f: EnumFacing.values()) {
        IBlockState st = world.getBlockState(pos.offset(f));
        if(st.getBlock() == this) {
          facing = st.getValue(FACING);
          break;
        }
      }
    }
    return getDefaultState().withProperty(FACING, facing);
  }

}
