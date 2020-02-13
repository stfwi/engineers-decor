/*
 * @file BlockDecorDirected.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Smaller (cutout) block with a defined facing.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import wile.engineersdecor.libmc.blocks.StandardBlocks;

import javax.annotation.Nullable;


public class BlockDecorSolarPanel extends StandardBlocks.BaseBlock
{
  public static final IntegerProperty EXPOSITION = IntegerProperty.create("exposition", 0, 4);

  public BlockDecorSolarPanel(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  {
    super(config, builder, unrotatedAABB);
    setDefaultState(stateContainer.getBaseState().with(EXPOSITION, 1));
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(EXPOSITION); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BlockDecorSolarPanel.BTileEntity(); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickableTileEntity, ICapabilityProvider, IEnergyStorage
  {
    public static final int DEFAULT_PEAK_POWER = 45;
    public static final int TICK_INTERVAL = 8;
    public static final int ACCUMULATION_INTERVAL = 4;
    private static final Direction transfer_directions_[] = {Direction.DOWN, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH };
    private static int peak_power_per_tick_ = DEFAULT_PEAK_POWER;
    private static int max_power_storage_ = 10000;
    private int tick_timer_ = 0;
    private int recalc_timer_ = 0;
    private int accumulated_power_ = 0;

    public static void on_config(int peak_power_per_tick)
    {
      peak_power_per_tick_ = MathHelper.clamp(peak_power_per_tick, 2, 8192);
      ModEngineersDecor.logger().info("Config small solar panel: Peak production:" + peak_power_per_tick_ + "/tick");
    }

    //------------------------------------------------------------------------------------------------------------------

    public BTileEntity()
    { this(ModContent.TET_SMALL_SOLAR_PANEL); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void readnbt(CompoundNBT nbt, boolean update_packet)
    { accumulated_power_ = nbt.getInt("energy"); }

    protected void writenbt(CompoundNBT nbt, boolean update_packet)
    { nbt.putInt("energy", accumulated_power_); }

    // IEnergyStorage --------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return true; }

    @Override
    public boolean canReceive()
    { return false; }

    @Override
    public int getMaxEnergyStored()
    { return max_power_storage_; }

    @Override
    public int getEnergyStored()
    { return accumulated_power_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    {
      int p = Math.min(accumulated_power_, maxExtract);
      if(!simulate) accumulated_power_ -= p;
      return p;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    { return 0; }

    // ICapabilityProvider ---------------------------------------------------------------------

    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(!this.removed && (facing != null)) {
        if(capability== CapabilityEnergy.ENERGY) {
          return energy_handler_.cast();
        }
      }
      return super.getCapability(capability, facing);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt, false); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt, false); return nbt; }

    @Override
    public void tick()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      if(!world.canBlockSeeSky(pos)) { tick_timer_ = TICK_INTERVAL * 5; return; }
      if(accumulated_power_ > 0) {
        for(int i=0; (i<transfer_directions_.length) && (accumulated_power_>0); ++i) {
          final Direction f = transfer_directions_[i];
          TileEntity te = world.getTileEntity(pos.offset(f));
          if(te==null) continue;
          IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, f.getOpposite()).orElse(null);
          if((es==null) || (!es.canReceive())) continue;
          accumulated_power_ = MathHelper.clamp(accumulated_power_-es.receiveEnergy(accumulated_power_, false),0, accumulated_power_);
        }
      }
      if(--recalc_timer_ > 0) return;
      recalc_timer_ = ACCUMULATION_INTERVAL + ((int)(Math.random()+.5));
      BlockState state = world.getBlockState(pos);
      int theta = ((((int)(world.getCelestialAngleRadians(1f) * (180.0/Math.PI)))+90) % 360);
      int e = 2;
      if(theta > 340)      e = 2;
      else if(theta <  45) e = 0;
      else if(theta <  80) e = 1;
      else if(theta < 100) e = 2;
      else if(theta < 135) e = 3;
      else if(theta < 190) e = 4;
      BlockState nstate = state.with(EXPOSITION, e);
      if(nstate != state) world.setBlockState(pos, nstate, 1|2);
      final double sb = world.getSunBrightness(1f);
      double rf = Math.abs(1.0-(((double)Math.abs(MathHelper.clamp(theta, 0, 180)-90))/90));
      rf = Math.sqrt(rf) * sb * ((TICK_INTERVAL*ACCUMULATION_INTERVAL)+2) * peak_power_per_tick_;
      accumulated_power_ = Math.min(accumulated_power_+(int)rf, max_power_storage_);
    }
  }
}
