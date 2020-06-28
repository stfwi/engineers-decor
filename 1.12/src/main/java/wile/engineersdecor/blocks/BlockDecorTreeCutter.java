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
import wile.engineersdecor.detail.ModAuxiliaries;
import wile.engineersdecor.detail.TreeCutting;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraft.nbt.NBTTagCompound;
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

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return true;
    ((BTileEntity)te).state_message(player);
    return true;
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
    private static int energy_max = DEFAULT_BOOST_ENERGY * 20;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY;
    private static int cutting_time_needed = 20 * DEFAULT_CUTTING_TIME_NEEDED;
    private static boolean requires_power = false;

    private int tick_timer_;
    private int active_timer_;
    private int proc_time_elapsed_; // small, not saved in nbt.
    private int energy_;            // small, not saved in nbt.

    public static void on_config(int boost_energy_per_tick, int cutting_time_seconds, boolean power_required)
    {
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 16, 512);
      energy_max = Math.max(boost_energy_consumption * 10, 10000);
      cutting_time_needed = 20 * MathHelper.clamp(cutting_time_seconds, 10, 240);
      requires_power = power_required;
      ModEngineersDecor.logger.info("Config tree cutter: Boost energy consumption:" + boost_energy_consumption + "rf/t" + (requires_power?" (power required for operation) ":"") + ", cutting time " + cutting_time_needed + "t." );
    }

    public BTileEntity()
    {}

    public void state_message(EntityPlayer player)
    {
      String soc = Integer.toString(MathHelper.clamp((energy_*100/energy_max),0,100));
      String progress = "";
      if((active_timer_ > 0) && (cutting_time_needed > 0)) {
        progress = " | " + Integer.toString((int)MathHelper.clamp((((double)proc_time_elapsed_) / ((double)cutting_time_needed) * 100), 0, 100)) + "%%";
      }
      ModAuxiliaries.playerChatMessage(player, soc + "%%/" + energy_max + "RF" + progress);
    }

    public void readnbt(NBTTagCompound nbt)
    { energy_ = nbt.getInteger("energy"); }

    private void writenbt(NBTTagCompound nbt)
    { nbt.setInteger("energy", energy_); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorTreeCutter)); }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    { super.readFromNBT(nbt); readnbt(nbt); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    { super.writeToNBT(nbt); writenbt(nbt); return nbt; }

    // IEnergyStorage ----------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return false; }

    @Override
    public boolean canReceive()
    { return true; }

    @Override
    public int getMaxEnergyStored()
    { return boost_energy_consumption*2; }

    @Override
    public int getEnergyStored()
    { return energy_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
      maxReceive = MathHelper.clamp(maxReceive, 0, Math.max((energy_max) - energy_, 0));
      if(!simulate) energy_ += maxReceive;
      return maxReceive;
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
      final IBlockState device_state = world.getBlockState(pos);
      if(!(device_state.getBlock() instanceof BlockDecorTreeCutter)) { tick_timer_ = TICK_INTERVAL; return; }
      if(world.isRemote) {
        if(!device_state.getValue(ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.1f, 1.0f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
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
        if(energy_ >= boost_energy_consumption) {
          energy_ -= boost_energy_consumption;
          proc_time_elapsed_ += TICK_INTERVAL*BOOST_FACTOR;
          active_timer_ = 2;
        } else if(!requires_power) {
          active_timer_ = 1024;
        } else if(active_timer_ > 0) {
          --active_timer_;
        }
        boolean active = (active_timer_ > 0);
        if(requires_power && !active) {
          proc_time_elapsed_ = Math.max(0, proc_time_elapsed_ - 2*TICK_INTERVAL);
        }
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
