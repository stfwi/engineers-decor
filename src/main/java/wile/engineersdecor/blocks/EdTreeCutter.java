/*
 * @file EdTreeCutter.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Tree Cutter
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.detail.TreeCutting;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
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

  public static class TreeCutterBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<TreeCutterTileEntity>
  {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public TreeCutterBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    @Nullable
    public BlockEntityType<EdTreeCutter.TreeCutterTileEntity> getBlockEntityType()
    { return ModContent.TET_SMALL_TREE_CUTTER; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(ACTIVE); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    { return super.getStateForPlacement(context).setValue(ACTIVE, false); }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level world, BlockPos pos, Random rnd)
    {
      if((state.getBlock()!=this) || (!state.getValue(ACTIVE))) return;
      // Sound
      if(true || (world.getGameTime() & 0x1) == 0) {
        world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.1f, 1.0f, false);
      }
      // Particles
      {
        final double rv = rnd.nextDouble();
        if(rv < 0.8) {
          final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
          final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
          switch(state.getValue(HORIZONTAL_FACING)) {
            case WEST -> world.addParticle(ParticleTypes.SMOKE, x - xc, yr, z + xr, 0.0, 0.0, 0.0);
            case EAST -> world.addParticle(ParticleTypes.SMOKE, x + xc, yr, z + xr, 0.0, 0.0, 0.0);
            case NORTH -> world.addParticle(ParticleTypes.SMOKE, x + xr, yr, z - xc, 0.0, 0.0, 0.0);
            default -> world.addParticle(ParticleTypes.SMOKE, x + xr, yr, z + xc, 0.0, 0.0, 0.0);
          }
        }
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      BlockEntity te = world.getBlockEntity(pos);
      if(te instanceof TreeCutterTileEntity) ((TreeCutterTileEntity)te).state_message(player);
      return InteractionResult.CONSUME;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TreeCutterTileEntity extends StandardEntityBlocks.StandardBlockEntity implements IEnergyStorage
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
      boost_energy_consumption = TICK_INTERVAL * Mth.clamp(boost_energy_per_tick, 4, 4096);
      energy_max = Math.max(boost_energy_consumption * 10, 10000);
      cutting_time_needed = 20 * Mth.clamp(cutting_time_seconds, 10, 240);
      requires_power = power_required;
      ModConfig.log("Config tree cutter: energy consumption:" + (boost_energy_consumption/TICK_INTERVAL) + "rf/t" + (requires_power?" (power required for operation) ":"") + ", cutting time:" + cutting_time_needed + "t." );
    }

    public TreeCutterTileEntity(BlockPos pos, BlockState state)
    { super(ModContent.TET_SMALL_TREE_CUTTER, pos, state); }

    public void readnbt(CompoundTag nbt)
    { energy_ = nbt.getInt("energy"); }

    private void writenbt(CompoundTag nbt)
    { nbt.putInt("energy", energy_); }

    public void state_message(Player player)
    {
      String progress = "0";
      if((active_timer_ > 0) && (cutting_time_needed > 0) && (active_timer_ > 0)) {
        progress = Integer.toString((int)Mth.clamp((((double)proc_time_elapsed_) / ((double)cutting_time_needed) * 100), 0, 100));
      }
      String soc = Integer.toString(Mth.clamp((energy_*100/energy_max),0,100));
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_tree_cutter.status", soc, energy_max, progress, (cutting_time_needed/20)));
    }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.save(nbt); writenbt(nbt); }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
    }

    // IEnergyStorage ----------------------------------------------------------------------------

    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> this);

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
      maxReceive = Mth.clamp(maxReceive, 0, Math.max((energy_max) - energy_, 0));
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
      tick_timer_ = TICK_INTERVAL;
      final BlockState device_state = level.getBlockState(worldPosition);
      if(!(device_state.getBlock() instanceof TreeCutterBlock)) return;
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
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        active = false;
      }
      if(device_state.getValue(TreeCutterBlock.ACTIVE) != active) {
        level.setBlock(worldPosition, device_state.setValue(TreeCutterBlock.ACTIVE, active), 1|2);
      }
    }
  }
}
