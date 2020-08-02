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
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;


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
    private static int tick_interval_ = 40;
    private int tick_timer_ = 0;

    public BTileEntity()
    {}

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorTest)); }

    //------------------------------------------------------------------------------------------------------------------

    public static double increment = 0.008;
    private boolean TESR_TEST = false;
    private double progress_ = -1;
    private double incr_ = increment;
    public double progress() { return progress_; }
    private void tesr_basic_test(boolean reset)
    {
      if(!TESR_TEST || !world.isRemote) return;
      if(reset) {
        progress_ = 0; incr_ = increment;
      } else {
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


    //------------------------------------------------------------------------------------------------------------------

    public boolean clicked(EntityPlayer player, boolean lclicked)
    {
      tesr_basic_test(true);
      return true;
    }

    @Override
    public void update()
    {
      tesr_basic_test(false);
      if(++tick_timer_ < tick_interval_) return;
      tick_timer_ = tick_interval_;
    }
  }
}
