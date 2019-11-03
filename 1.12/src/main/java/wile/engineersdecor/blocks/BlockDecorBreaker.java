/*
 * @file BlockDecorBreaker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Block Breaker
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.Blocks;
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
import java.util.HashSet;
import java.util.Random;


public class BlockDecorBreaker extends BlockDecorDirected
{
  public static final PropertyBool ACTIVE = PropertyBool.create("active");

  public BlockDecorBreaker(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, ACTIVE); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 0x7)).withProperty(ACTIVE, (meta & 0x8)!=0); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return (state.getValue(FACING).getHorizontalIndex() & 0x7) | (state.getValue(ACTIVE) ? 8 : 0); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(ACTIVE, false); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BTileEntity(); }

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
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos neighborPos)
  {
    if(!(world instanceof World) || (((World) world).isRemote)) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).block_updated();
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable, IEnergyStorage
  {
    public static final int IDLE_TICK_INTERVAL = 40;
    public static final int TICK_INTERVAL = 5;
    public static final int BOOST_FACTOR = 8;
    public static final int DEFAULT_BOOST_ENERGY = 64;
    public static final int DEFAULT_BREAKING_RELUCTANCE = 17;
    public static final int DEFAULT_MIN_BREAKING_TIME = 15;
    public static final int MAX_BREAKING_TIME = 800;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY;
    private static int breaking_reluctance = DEFAULT_BREAKING_RELUCTANCE;
    private static int min_breaking_time = DEFAULT_MIN_BREAKING_TIME;
    private int tick_timer_;
    private int proc_time_elapsed_;
    private int boost_energy_;

    public static void on_config(int boost_energy_per_tick, int breaking_time_per_hardness, int min_breaking_time_ticks)
    {
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 16, 512);
      breaking_reluctance = MathHelper.clamp(breaking_time_per_hardness, 5, 50);
      min_breaking_time = MathHelper.clamp(min_breaking_time_ticks, 10, 100);
      ModEngineersDecor.logger.info("Config block breaker: Boost energy consumption:" + boost_energy_consumption + "rf/t, reluctance=" + breaking_reluctance + "/hrdn, break time offset=" + min_breaking_time );
    }

    public BTileEntity()
    {}

    public void block_updated()
    { if(tick_timer_ > 2) tick_timer_ = 2; }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorBreaker)); }

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
    {
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

    private static HashSet<Block> blacklist = new HashSet<>();
    static {
      blacklist.add(Blocks.AIR);
      blacklist.add(Blocks.BEDROCK);
      blacklist.add(Blocks.FIRE);
      blacklist.add(Blocks.END_PORTAL);
      blacklist.add(Blocks.END_GATEWAY);
      blacklist.add(Blocks.END_PORTAL_FRAME);
    }

    private static boolean isBreakable(IBlockState state, BlockPos pos, World world)
    {
      final Block block = state.getBlock();
      if(blacklist.contains(block)) return false;
      if(state.getMaterial().isLiquid()) return false;
      if(block.isAir(state, world, pos)) return false;
      float bh = state.getBlockHardness(world, pos);
      if((bh<0) || (bh>55)) return false;
      return true;
    }

    private boolean breakBlock(IBlockState state, BlockPos pos, World world)
    {
      if(world.isRemote || world.restoringBlockSnapshots) return false; // retry next cycle
      NonNullList<ItemStack> drops = NonNullList.create();
      state.getBlock().getDrops(drops, world, pos, state, 0);
      world.setBlockToAir(pos);
      for(ItemStack drop:drops) spawnAsEntity(world, pos, drop);
      SoundType stype = state.getBlock().getSoundType(state, world, pos, null);
      if(stype != null) world.playSound(null, pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
      return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void update()
    {
      if(--tick_timer_ > 0) return;
      if(world.isRemote) {
        IBlockState state = world.getBlockState(pos);
        if(!state.getValue(ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          // not sure if is so cool to do this each tick ... may be simplified/removed again.
          SoundEvent sound = SoundEvents.BLOCK_WOOD_HIT;
          SoundType stype = world.getBlockState(pos.offset(state.getValue(FACING))).getBlock().getSoundType();
          if((stype == SoundType.CLOTH) || (stype == SoundType.PLANT) || (stype == SoundType.SNOW)) {
            sound = SoundEvents.BLOCK_CLOTH_HIT;
          } else if((stype == SoundType.GROUND) || (stype == SoundType.SAND)) {
            sound = SoundEvents.BLOCK_GRAVEL_HIT;
          }
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundCategory.BLOCKS, 0.1f, 1.2f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
        final IBlockState device_state = world.getBlockState(pos);
        final BlockPos target_pos = pos.offset(device_state.getValue(FACING));
        final IBlockState target_state = world.getBlockState(target_pos);
        if((world.isBlockPowered(pos)) || (!isBreakable(target_state, target_pos, world))) {
          if(device_state.getValue(ACTIVE)) world.setBlockState(pos, device_state.withProperty(ACTIVE, false), 1|2);
          proc_time_elapsed_ = 0;
          tick_timer_ = IDLE_TICK_INTERVAL;
          return;
        }
        proc_time_elapsed_ += TICK_INTERVAL;
        boolean active = true;
        int time_needed = (int)(target_state.getBlockHardness(world, pos) * breaking_reluctance) + min_breaking_time;
        if(boost_energy_ >= boost_energy_consumption) {
          boost_energy_ = 0;
          proc_time_elapsed_ += TICK_INTERVAL * BOOST_FACTOR;
          time_needed += min_breaking_time * (3*BOOST_FACTOR/5);
        }
        time_needed = MathHelper.clamp(time_needed, min_breaking_time, MAX_BREAKING_TIME);
        if(proc_time_elapsed_ >= time_needed) {
          proc_time_elapsed_ = 0;
          breakBlock(target_state, target_pos, world);
          active = false;
        }
        if(device_state.getValue(ACTIVE) != active) {
          world.setBlockState(pos, device_state.withProperty(ACTIVE, active), 1|2);
        }
      }
    }
  }
}
