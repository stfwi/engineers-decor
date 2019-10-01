/*
 * @file BlockDecorTest.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.util.math.AxisAlignedBB;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorTest extends BlockDecorDirected implements ModAuxiliaries.IExperimentalFeature
{
  public BlockDecorTest(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Override
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BTileEntity(); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  { return clicked(world, pos, player, false); }

  @Override
  public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
  { clicked(world, pos, player, true); }

  private boolean clicked(World world, BlockPos pos, EntityPlayer player, boolean lclick)
  { TileEntity te = world.getTileEntity(pos); return (te instanceof BTileEntity) && ((BTileEntity)te).clicked(player, lclick); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable
  {
    public static double increment = 0.008;
    private double progress_ = 0;
    private double incr_ = increment;

    public BTileEntity()
    {}

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorTest)); }



    public double progress()
    { return progress_; }

    public boolean clicked(EntityPlayer player, boolean lclicked)
    {
      progress_ = 0;
      incr_ = increment;
      return true;
    }

    @Override
    public void update()
    {
      progress_ += incr_;
      if(progress_ < 0) {
        incr_ = increment;
        progress_ = 0;
      } else if(progress_ > 1.0) {
        progress_ = 1.0;
        incr_ = -increment;
      }
    }
  }
}
