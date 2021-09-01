/*
 * @file EdTreeCutter.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Tree Cutter
 */
package wile.engineersdecor.blocks;

import net.minecraft.world.IWorldReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.detail.TreeCutting;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Overlay;

import javax.annotation.Nullable;
import java.util.Random;



public class EdTreeCutter
{
  public static void on_config(int boost_energy_per_tick, int cutting_time_seconds, boolean power_required)
  { TreeCutterTileEntity.on_config(boost_energy_per_tick, cutting_time_seconds,power_required); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class TreeCutterBlock extends DecorBlock.Horizontal implements IDecorBlock
  {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public TreeCutterBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(ACTIVE); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).setValue(ACTIVE, false); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new TreeCutterTileEntity(); }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
    {
      if((state.getBlock()!=this) || (!state.getValue(ACTIVE))) return;
      final double rv = rnd.nextDouble();
      if(rv > 0.8) return;
      final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
      final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
      switch(state.getValue(HORIZONTAL_FACING)) {
        case WEST:  world.addParticle(ParticleTypes.SMOKE, x-xc, yr, z+xr, 0.0, 0.0, 0.0); break;
        case EAST:  world.addParticle(ParticleTypes.SMOKE, x+xc, yr, z+xr, 0.0, 0.0, 0.0); break;
        case NORTH: world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z-xc, 0.0, 0.0, 0.0); break;
        default:    world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z+xc, 0.0, 0.0, 0.0); break;
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      TileEntity te = world.getBlockEntity(pos);
      if(te instanceof TreeCutterTileEntity) ((TreeCutterTileEntity)te).state_message(player);
      return ActionResultType.CONSUME;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TreeCutterTileEntity extends TileEntity implements ITickableTileEntity, IEnergyStorage
  {
    public static final int IDLE_TICK_INTERVAL = 40;
    public static final int TICK_INTERVAL = 5;
    public static final int BOOST_FACTOR = 6;
    public static final int DEFAULT_BOOST_ENERGY = 64;
    public static final int DEFAULT_CUTTING_TIME_NEEDED = 60; // 60 secs, so that people don't come to the bright idea to carry one with them.
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY;
    private static int energy_max = DEFAULT_BOOST_ENERGY * 20;
    private static int cutting_time_needed = 20 * DEFAULT_CUTTING_TIME_NEEDED;
    private static boolean requires_power = false;

    private int tick_timer_;
    private int active_timer_;
    private int proc_time_elapsed_;
    private int energy_;

    public static void on_config(int boost_energy_per_tick, int cutting_time_seconds, boolean power_required)
    {
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 4, 4096);
      energy_max = Math.max(boost_energy_consumption * 10, 10000);
      cutting_time_needed = 20 * MathHelper.clamp(cutting_time_seconds, 10, 240);
      requires_power = power_required;
      ModConfig.log("Config tree cutter: energy consumption:" + (boost_energy_consumption/TICK_INTERVAL) + "rf/t" + (requires_power?" (power required for operation) ":"") + ", cutting time:" + cutting_time_needed + "t." );
    }

    public TreeCutterTileEntity()
    { super(ModContent.TET_SMALL_TREE_CUTTER); }

    public TreeCutterTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void readnbt(CompoundNBT nbt)
    { energy_ = nbt.getInt("energy"); }

    private void writenbt(CompoundNBT nbt)
    { nbt.putInt("energy", energy_); }

    public void state_message(PlayerEntity player)
    {
      String progress = "0";
      if((active_timer_ > 0) && (cutting_time_needed > 0) && (active_timer_ > 0)) {
        progress = Integer.toString((int)MathHelper.clamp((((double)proc_time_elapsed_) / ((double)cutting_time_needed) * 100), 0, 100));
      }
      String soc = Integer.toString(MathHelper.clamp((energy_*100/energy_max),0,100));
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_tree_cutter.status", new Object[]{soc, energy_max, progress, (cutting_time_needed/20) }));
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void load(BlockState state, CompoundNBT nbt)
    { super.load(state, nbt); readnbt(nbt); }

    @Override
    public CompoundNBT save(CompoundNBT nbt)
    { super.save(nbt); writenbt(nbt); return nbt; }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
    }

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
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability== CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickable ------------------------------------------------------------------------------------

    @Override
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      final BlockState device_state = level.getBlockState(worldPosition);
      if(!(device_state.getBlock() instanceof TreeCutterBlock)) { tick_timer_ = TICK_INTERVAL; return; }
      if(level.isClientSide) {
        if(!device_state.getValue(TreeCutterBlock.ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.WOOD_HIT, SoundCategory.BLOCKS, 0.1f, 1.0f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
        final BlockPos tree_pos = worldPosition.relative(device_state.getValue(TreeCutterBlock.HORIZONTAL_FACING));
        final BlockState tree_state = level.getBlockState(tree_pos);
        if(!TreeCutting.canChop(tree_state) || (level.hasNeighborSignal(worldPosition))) {
          if(device_state.getValue(TreeCutterBlock.ACTIVE)) level.setBlock(worldPosition, device_state.setValue(TreeCutterBlock.ACTIVE, false), 1|2);
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
          TreeCutting.chopTree(level, tree_state, tree_pos, 512, false);
          level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
          active = false;
        }
        if(device_state.getValue(TreeCutterBlock.ACTIVE) != active) {
          level.setBlock(worldPosition, device_state.setValue(TreeCutterBlock.ACTIVE, active), 1|2);
        }
      }
    }
  }
}
