/*
 * @file EdBreaker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small Block Breaker
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Overlay;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import wile.engineersdecor.libmc.detail.RfEnergy;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;



public class EdBreaker
{
  public static void on_config(int boost_energy_per_tick, int breaking_time_per_hardness, int min_breaking_time_ticks, boolean power_required)
  { BreakerTileEntity.on_config(boost_energy_per_tick, breaking_time_per_hardness, min_breaking_time_ticks, power_required); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class BreakerBlock extends DecorBlock.HorizontalWaterLoggable implements IDecorBlock
  {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public BreakerBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

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
    { return new BreakerTileEntity(); }

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
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
    {
      if(!(world instanceof World) || (((World) world).isClientSide)) return;
      TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof BreakerTileEntity)) return;
      ((BreakerTileEntity)te).block_updated();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getDirectSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      TileEntity te = world.getBlockEntity(pos);
      if(te instanceof BreakerTileEntity) ((BreakerTileEntity)te).state_message(player);
      return ActionResultType.CONSUME;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BreakerTileEntity extends TileEntity implements ITickableTileEntity
  {
    public static final int IDLE_TICK_INTERVAL = 40;
    public static final int TICK_INTERVAL = 5;
    public static final int BOOST_FACTOR = 8;
    public static final int DEFAULT_BOOST_ENERGY = 64;
    public static final int DEFAULT_BREAKING_RELUCTANCE = 17;
    public static final int DEFAULT_MIN_BREAKING_TIME = 15;
    public static final int MAX_BREAKING_TIME = 800;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY;
    private static int energy_max = 32000;
    private static int breaking_reluctance = DEFAULT_BREAKING_RELUCTANCE;
    private static int min_breaking_time = DEFAULT_MIN_BREAKING_TIME;
    private static boolean requires_power = false;
    private int tick_timer_;
    private int active_timer_;
    private int proc_time_elapsed_;
    private int time_needed_;
    private final RfEnergy.Battery battery_;
    private final LazyOptional<IEnergyStorage> energy_handler_;

    public static void on_config(int boost_energy_per_tick, int breaking_time_per_hardness, int min_breaking_time_ticks, boolean power_required)
    {
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 4, 4096);
      energy_max = Math.max(boost_energy_consumption * 10, 100000);
      breaking_reluctance = MathHelper.clamp(breaking_time_per_hardness, 5, 50);
      min_breaking_time = MathHelper.clamp(min_breaking_time_ticks, 10, 100);
      requires_power = power_required;
      ModConfig.log("Config block breaker: Boost energy consumption:" + (boost_energy_consumption/TICK_INTERVAL) + "rf/t, reluctance=" + breaking_reluctance + "t/hrdn, break time offset=" + min_breaking_time + "t.");
    }

    public BreakerTileEntity()
    { this(ModContent.TET_SMALL_BLOCK_BREAKER); }

    public BreakerTileEntity(TileEntityType<?> te_type)
    {
      super(te_type);
      battery_ = new RfEnergy.Battery(energy_max, boost_energy_consumption, 0);
      energy_handler_ = battery_.createEnergyHandler();
    }

    public void block_updated()
    { if(tick_timer_ > 2) tick_timer_ = 2; }

    public void readnbt(CompoundNBT nbt)
    { battery_.load(nbt); }

    private void writenbt(CompoundNBT nbt)
    { battery_.save(nbt); }

    public void state_message(PlayerEntity player)
    {
      String progress = "0";
      if((proc_time_elapsed_ > 0) && (time_needed_ > 0)) {
        progress = Integer.toString((int)MathHelper.clamp((((double)proc_time_elapsed_) / ((double)time_needed_) * 100), 0, 100));
      }
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_block_breaker.status", new Object[]{battery_.getSOC(), energy_max, progress }));
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

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickable ------------------------------------------------------------------------------------

    private static final HashSet<Block> blacklist = new HashSet<>();
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
      float bh = state.getDestroySpeed(world, pos);
      if((bh<0) || (bh>55)) return false;
      return true;
    }

    private static void spawnBlockAsEntity(World world, BlockPos pos, ItemStack stack) {
      if(world.isClientSide || stack.isEmpty() || (!world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) || world.restoringBlockSnapshots) return;
      ItemEntity e = new ItemEntity(world,
        ((world.random.nextFloat()*0.1)+0.5) + pos.getX(),
        ((world.random.nextFloat()*0.1)+0.5) + pos.getY(),
        ((world.random.nextFloat()*0.1)+0.5) + pos.getZ(),
        stack
      );
      e.setDefaultPickUpDelay();
      e.setDeltaMovement((world.random.nextFloat()*0.1-0.05), (world.random.nextFloat()*0.1-0.03), (world.random.nextFloat()*0.1-0.05));
      world.addFreshEntity(e);
    }

    private static boolean canInsertInto(World world, BlockPos pos)
    {
      // Maybe make a tag for that. The question is if it is actually worth it, or if that would be only
      // tag spamming the game. So for now only FH and VH.
      final BlockState state = world.getBlockState(pos);
      return (state.getBlock() == ModContent.FACTORY_HOPPER) || (state.getBlock() == Blocks.HOPPER);
    }

    private boolean breakBlock(BlockState state, BlockPos pos, World world)
    {
      if(world.isClientSide  || (!(world instanceof ServerWorld)) || world.restoringBlockSnapshots) return false; // retry next cycle
      List<ItemStack> drops;
      final Block block = state.getBlock();
      final boolean insert = canInsertInto(world, getBlockPos().below());
      drops = Block.getDrops(state, (ServerWorld)world, pos, world.getBlockEntity(pos));
      world.removeBlock(pos, false);
      for(ItemStack drop:drops) {
        if(!insert) {
          spawnBlockAsEntity(world, pos, drop);
        } else {
          final ItemStack remaining = Inventories.insert(world, getBlockPos().below(), Direction.UP, drop, false);
          if(!remaining.isEmpty()) spawnBlockAsEntity(world, pos, remaining);
        }
      }
      SoundType stype = state.getBlock().getSoundType(state, world, pos, null);
      if(stype != null) world.playSound(null, pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
      return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      final BlockState device_state = level.getBlockState(worldPosition);
      if(!(device_state.getBlock() instanceof BreakerBlock)) return;
      if(level.isClientSide) {
        if(!device_state.getValue(BreakerBlock.ACTIVE)) {
          tick_timer_ = TICK_INTERVAL;
        } else {
          tick_timer_ = 1;
          // not sure if is so cool to do this each tick ... may be simplified/removed again.
          SoundEvent sound = SoundEvents.WOOD_HIT;
          BlockState target_state = level.getBlockState(worldPosition.relative(device_state.getValue(BreakerBlock.HORIZONTAL_FACING)));
          SoundType stype = target_state.getBlock().getSoundType(target_state);
          if((stype == SoundType.WOOL) || (stype == SoundType.GRASS) || (stype == SoundType.SNOW)) {
            sound = SoundEvents.WOOL_HIT;
          } else if((stype == SoundType.GRAVEL) || (stype == SoundType.SAND)) {
            sound = SoundEvents.GRAVEL_HIT;
          }
          level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sound, SoundCategory.BLOCKS, 0.1f, 1.2f, false);
        }
      } else {
        tick_timer_ = TICK_INTERVAL;
        final BlockPos target_pos = worldPosition.relative(device_state.getValue(BreakerBlock.HORIZONTAL_FACING));
        final BlockState target_state = level.getBlockState(target_pos);
        if((level.hasNeighborSignal(worldPosition)) || (!isBreakable(target_state, target_pos, level))) {
          if(device_state.getValue(BreakerBlock.ACTIVE)) level.setBlock(worldPosition, device_state.setValue(BreakerBlock.ACTIVE, false), 1|2);
          proc_time_elapsed_ = 0;
          tick_timer_ = IDLE_TICK_INTERVAL;
          return;
        }
        time_needed_ = MathHelper.clamp((int)(target_state.getDestroySpeed(level, worldPosition) * breaking_reluctance) + min_breaking_time, min_breaking_time, MAX_BREAKING_TIME);
        if(battery_.draw(boost_energy_consumption)) {
          proc_time_elapsed_ += TICK_INTERVAL * (1+BOOST_FACTOR);
          time_needed_ += min_breaking_time * (3*BOOST_FACTOR/5);
          active_timer_ = 2;
        } else if(!requires_power) {
          proc_time_elapsed_ += TICK_INTERVAL;
          active_timer_ = 1024;
        } else if(active_timer_ > 0) {
          --active_timer_;
        }
        boolean active = (active_timer_ > 0);
        if(requires_power && !active) {
          proc_time_elapsed_ = Math.max(0, proc_time_elapsed_ - 2*TICK_INTERVAL);
        }
        if(proc_time_elapsed_ >= time_needed_) {
          proc_time_elapsed_ = 0;
          breakBlock(target_state, target_pos, level);
          active = false;
        }
        if(device_state.getValue(BreakerBlock.ACTIVE) != active) {
          level.setBlock(worldPosition, device_state.setValue(BreakerBlock.ACTIVE, active), 1|2);
        }
      }
    }
  }
}
