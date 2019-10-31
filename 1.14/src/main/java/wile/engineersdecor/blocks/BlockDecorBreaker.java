/*
 * @file BlockDecorBreaker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Block Breaker
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;


public class BlockDecorBreaker extends BlockDecorDirectedHorizontal
{
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

  public BlockDecorBreaker(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(ACTIVE); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context).with(ACTIVE, false); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BTileEntity(); }

  @OnlyIn(Dist.CLIENT)
  public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
  {
    if((state.getBlock()!=this) || (!state.get(ACTIVE))) return;
    final double rv = rnd.nextDouble();
    if(rv > 0.8) return;
    final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
    final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
    switch(state.get(HORIZONTAL_FACING)) {
      case WEST:  world.addParticle(ParticleTypes.SMOKE, x-xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case EAST:  world.addParticle(ParticleTypes.SMOKE, x+xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case NORTH: world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z-xc, 0.0, 0.0, 0.0); break;
      default:    world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z+xc, 0.0, 0.0, 0.0); break;
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
  {
    if(!(world instanceof World) || (((World) world).isRemote)) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).block_updated();
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(BlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
  { return 0; }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
  { return 0; }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickableTileEntity, IEnergyStorage
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
      ModEngineersDecor.logger().info("Config block breaker: Boost energy consumption:" + boost_energy_consumption + "rf/t, reluctance=" + breaking_reluctance + "/hrdn, break time offset=" + min_breaking_time );
    }

    public BTileEntity()
    { super(ModContent.TET_SMALL_BLOCK_BREAKER); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void block_updated()
    { if(tick_timer_ > 2) tick_timer_ = 2; }


    // IEnergyStorage ----------------------------------------------------------------------------

    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

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
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(!this.removed && (facing != null)) {
        if(capability== CapabilityEnergy.ENERGY) {
          return energy_handler_.cast();
        }
      }
      return super.getCapability(capability, facing);
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
      blacklist.add(Blocks.NETHER_PORTAL);
      blacklist.add(Blocks.BARRIER);
    }

    private static boolean isBreakable(BlockState state, BlockPos pos, World world)
    {
      final Block block = state.getBlock();
      if(blacklist.contains(block)) return false;
      if(state.getMaterial().isLiquid()) return false;
      if(block.isAir(state, world, pos)) return false;
      float bh = state.getBlockHardness(world, pos);
      if((bh<0) || (bh>55)) return false;
      return true;
    }

    private boolean breakBlock(BlockState state, BlockPos pos, World world)
    {
      if(world.isRemote  || (!(world instanceof ServerWorld)) || world.restoringBlockSnapshots) return false; // retry next cycle
      List<ItemStack> drops = Block.getDrops(state, (ServerWorld)world, pos, world.getTileEntity(pos));
      world.removeBlock(pos, false);
      for(ItemStack drop:drops) spawnAsEntity(world, pos, drop);
      SoundType stype = state.getBlock().getSoundType(state, world, pos, null);
      if(stype != null) world.playSound(null, pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
      return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      if(world.isRemote) {
        BlockState state = world.getBlockState(pos);
        if(!state.get(ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          // not sure if is so cool to do this each tick ... may be simplified/removed again.
          SoundEvent sound = SoundEvents.BLOCK_WOOD_HIT;
          BlockState target_state = world.getBlockState(pos.offset(state.get(HORIZONTAL_FACING)));
          SoundType stype = target_state.getBlock().getSoundType(target_state);
          if((stype == SoundType.CLOTH) || (stype == SoundType.PLANT) || (stype == SoundType.SNOW)) {
            sound = SoundEvents.BLOCK_WOOL_HIT;
          } else if((stype == SoundType.GROUND) || (stype == SoundType.SAND)) {
            sound = SoundEvents.BLOCK_GRAVEL_HIT;
          }
          world.playSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundCategory.BLOCKS, 0.1f, 1.2f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
        final BlockState device_state = world.getBlockState(pos);
        final BlockPos target_pos = pos.offset(device_state.get(HORIZONTAL_FACING));
        final BlockState target_state = world.getBlockState(target_pos);
        if((world.isBlockPowered(pos)) || (!isBreakable(target_state, target_pos, world))) {
          if(device_state.get(ACTIVE)) world.setBlockState(pos, device_state.with(ACTIVE, false), 1|2);
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
        if(device_state.get(ACTIVE) != active) {
          world.setBlockState(pos, device_state.with(ACTIVE, active), 1|2);
        }
      }
    }
  }
}
