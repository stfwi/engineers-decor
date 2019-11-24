/*
 * @file BlockDecorTreeCutter.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Tree Cutter
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.TreeCutting;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;


public class BlockDecorTreeCutter extends BlockDecorDirectedHorizontal
{
  public static final PropertyBool ACTIVE = PropertyBool.create("active");

  public BlockDecorTreeCutter(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, ACTIVE); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 0x3)).withProperty(ACTIVE, (meta & 0x4)!=0); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return (state.getValue(FACING).getHorizontalIndex() & 0x3) | (state.getValue(ACTIVE) ? 4 : 0); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(ACTIVE, false); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorTreeCutter.BTileEntity(); }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rnd)
  {
    if((state.getBlock()!=this) || (!state.getValue(ACTIVE))) return;
    final double rv = rnd.nextDouble();
    if(rv > 0.8) return;
    final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
    final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
    switch(state.getValue(FACING)) {
      case WEST:  world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x-xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case EAST:  world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case NORTH: world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xr, yr, z-xc, 0.0, 0.0, 0.0); break;
      default:    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xr, yr, z+xc, 0.0, 0.0, 0.0); break;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable, IEnergyStorage
  {
    public static final int IDLE_TICK_INTERVAL = 40;
    public static final int TICK_INTERVAL = 5;
    public static final int BOOST_FACTOR = 6;
    public static final int DEFAULT_BOOST_ENERGY = 64;
    public static final int DEFAULT_CUTTING_TIME_NEEDED = 60; // 60 secs, so that people don't come to the bright idea to carry one with them.
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY;
    private static int cutting_time_needed = 20 * DEFAULT_CUTTING_TIME_NEEDED;
    private static boolean requires_power = false;

    private int tick_timer_;
    private int active_timer_;
    private int proc_time_elapsed_; // small, not saved in nbt.
    private int boost_energy_;      // small, not saved in nbt.

    public static void on_config(int boost_energy_per_tick, int cutting_time_seconds, boolean power_required)
    {
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 16, 512);
      cutting_time_needed = 20 * MathHelper.clamp(cutting_time_seconds, 10, 240);
      requires_power = power_required;
      ModEngineersDecor.logger.info("Config tree cutter: Boost energy consumption:" + boost_energy_consumption + "rf/t" + (requires_power?" (power required for operation) ":"") + ", cutting time " + cutting_time_needed + "t." );
    }

    public BTileEntity()
    {}

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorTreeCutter)); }

    // IEnergyStorage ----------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return false; }

    @Override
    public boolean canReceive()
    { return true; }

    @Override
    public int getMaxEnergyStored()
    { return boost_energy_consumption; }

    @Override
    public int getEnergyStored()
    { return boost_energy_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    { // only speedup support, no buffering, not in nbt -> no markdirty
      if((boost_energy_ >= boost_energy_consumption) || (maxReceive < boost_energy_consumption)) return 0;
      if(!simulate) boost_energy_ = boost_energy_consumption;
      return boost_energy_consumption;
    }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return ((cap==CapabilityEnergy.ENERGY)) || super.hasCapability(cap, facing); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability == CapabilityEnergy.ENERGY) {
        return (T)this;
      } else {
        return super.getCapability(capability, facing);
      }
    }

    // ITickable ------------------------------------------------------------------------------------

    @Override
    public void update()
    {
      if(--tick_timer_ > 0) return;
      if(world.isRemote) {
        if(!world.getBlockState(pos).getValue(ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.1f, 1.0f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
        final IBlockState device_state = world.getBlockState(pos);
        final BlockPos tree_pos = pos.offset(device_state.getValue(FACING));
        final IBlockState tree_state = world.getBlockState(tree_pos);
        if(!TreeCutting.canChop(tree_state) || (world.isBlockPowered(pos))) {
          if(device_state.getValue(ACTIVE)) world.setBlockState(pos, device_state.withProperty(ACTIVE, false), 1|2);
          proc_time_elapsed_ = 0;
          active_timer_ = 0;
          tick_timer_ = IDLE_TICK_INTERVAL;
          return;
        }
        proc_time_elapsed_ += TICK_INTERVAL;
        if(boost_energy_ >= boost_energy_consumption) {
          boost_energy_ = 0;
          proc_time_elapsed_ += TICK_INTERVAL*BOOST_FACTOR;
          active_timer_ = 2;
        } else if(!requires_power) {
          active_timer_ = 1024;
        } else if(active_timer_ > 0) {
          --active_timer_;
        }
        boolean active = (active_timer_ > 0);
        if(proc_time_elapsed_ >= cutting_time_needed) {
          proc_time_elapsed_ = 0;
          TreeCutting.chopTree(world, tree_state, tree_pos, 2048, false);
          world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
          active = false;
        }
        if(device_state.getValue(ACTIVE) != active) {
          world.setBlockState(pos, device_state.withProperty(ACTIVE, active), 1|2);
        }
      }
    }
  }
}
