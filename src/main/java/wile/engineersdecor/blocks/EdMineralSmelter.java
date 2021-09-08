/*
 * @file EdMineralSmelter.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small highly insulated stone liquification furnace
 * (magmatic phase).
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Fluidics;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.RfEnergy;

import javax.annotation.Nullable;
import java.util.*;

public class EdMineralSmelter
{
  public static void on_config(int consumption, int heatup_per_second)
  { MineralSmelterTileEntity.on_config(consumption, heatup_per_second); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class MineralSmelterBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<MineralSmelterTileEntity>
  {
    public static final int PHASE_MAX = 3;
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, PHASE_MAX);

    public MineralSmelterBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    @Nullable
    public BlockEntityType<EdMineralSmelter.MineralSmelterTileEntity> getBlockEntityType()
    { return ModContent.TET_MINERAL_SMELTER; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return Shapes.block(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(PHASE); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    { return super.getStateForPlacement(context).setValue(PHASE, 0); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos)
    { return Mth.clamp((state.getValue(PHASE)*5), 0, 15); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {}

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, BlockEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof MineralSmelterTileEntity)) return stacks;
      ((MineralSmelterTileEntity)te).reset_process();
      stacks.add(new ItemStack(this, 1));
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    {
      if(player.isShiftKeyDown()) return InteractionResult.PASS;
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      MineralSmelterTileEntity te = getTe(world, pos);
      if(te==null) return InteractionResult.FAIL;
      final ItemStack stack = player.getItemInHand(hand);
      boolean dirty = false;
      if(te.accepts_lava_container(stack)) {
        if(stack.sameItemStackIgnoreDurability(MineralSmelterTileEntity.BUCKET_STACK)) { // check how this works with item capabilities or so
          if(te.fluid_extraction_possible()) {
            if(stack.getCount() > 1) {
              int target_stack_index = -1;
              for(int i=0; i<player.getInventory().getContainerSize(); ++i) {
                if(player.getInventory().getItem(i).isEmpty()) {
                  target_stack_index = i;
                  break;
                }
              }
              if(target_stack_index >= 0) {
                te.reset_process();
                stack.shrink(1);
                player.setItemInHand(hand, stack);
                player.getInventory().setItem(target_stack_index, MineralSmelterTileEntity.LAVA_BUCKET_STACK.copy());
                world.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1f, 1f);
                dirty = true;
              }
            } else {
              te.reset_process();
              player.setItemInHand(hand, MineralSmelterTileEntity.LAVA_BUCKET_STACK.copy());
              world.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1f, 1f);
              dirty = true;
            }
          }
        }
      } else if(stack.isEmpty()) {
        final ItemStack istack = te.extract(true);
        if(te.phase() > MineralSmelterTileEntity.PHASE_WARMUP) player.setSecondsOnFire(1);
        if(!istack.isEmpty()) {
          player.setItemInHand(hand, te.extract(false));
          dirty = true;
        }
      } else if(te.insert(stack,false)) {
        stack.shrink(1);
        dirty = true;
      }
      if(dirty) player.getInventory().setChanged();
      return InteractionResult.CONSUME;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level world, BlockPos pos, Random rnd)
    {
      if(state.getBlock()!=this) return;
      ParticleOptions particle = ParticleTypes.SMOKE;
      switch(state.getValue(PHASE)) {
        case MineralSmelterTileEntity.PHASE_WARMUP:
          return;
        case MineralSmelterTileEntity.PHASE_HOT:
          if(rnd.nextInt(10) > 4) return;
          break;
        case MineralSmelterTileEntity.PHASE_MAGMABLOCK:
          if(rnd.nextInt(10) > 7) return;
          particle = ParticleTypes.LARGE_SMOKE;
          break;
        case MineralSmelterTileEntity.PHASE_LAVA:
          if(rnd.nextInt(10) > 2) return;
          particle = ParticleTypes.LAVA;
          break;
        default:
          return;
      }
      final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
      final double xr=rnd.nextDouble()*0.4-0.2, yr=rnd.nextDouble()*0.5, zr=rnd.nextDouble()*0.4-0.2;
      world.addParticle(particle, x+xr, y+yr, z+zr, 0.0, 0.0, 0.0);
    }

    @Nullable
    private MineralSmelterTileEntity getTe(Level world, BlockPos pos)
    { final BlockEntity te=world.getBlockEntity(pos); return (!(te instanceof MineralSmelterTileEntity)) ? (null) : ((MineralSmelterTileEntity)te); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class MineralSmelterTileEntity extends StandardEntityBlocks.StandardBlockEntity
  {
    public static final int NUM_OF_SLOTS = 2;
    public static final int TICK_INTERVAL = 20;
    public static final int MAX_FLUID_LEVEL = 2000;
    public static final int MAX_BUCKET_EXTRACT_FLUID_LEVEL = 900;
    public static final int MAX_ENERGY_BUFFER = 32000;
    public static final int MAX_ENERGY_TRANSFER = 8192;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 92;
    public static final int DEFAULT_HEATUP_RATE = 10; //2; // -> 50s for one smelting process
    public static final int PHASE_WARMUP = 0;
    public static final int PHASE_HOT = 1;
    public static final int PHASE_MAGMABLOCK = 2;
    public static final int PHASE_LAVA = 3;
    private static final ItemStack MAGMA_STACK = new ItemStack(Blocks.MAGMA_BLOCK);
    private static final ItemStack BUCKET_STACK = new ItemStack(Items.BUCKET);
    private static final ItemStack LAVA_BUCKET_STACK = new ItemStack(Items.LAVA_BUCKET);
    private static final FluidStack LAVA_BUCKET_FLUID_STACK = new FluidStack(Fluids.LAVA, 1000);
    private static final Set<Item> accepted_minerals = new HashSet<>();
    private static final Set<Item> accepted_lava_contrainers = new HashSet<>();
    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;
    private static int heatup_rate = DEFAULT_HEATUP_RATE;
    private static int cooldown_rate = 1;
    private int tick_timer_;
    private int progress_;
    private boolean force_block_update_;

    private final RfEnergy.Battery battery_ = new RfEnergy.Battery(MAX_ENERGY_BUFFER, MAX_ENERGY_TRANSFER, 0);
    private final LazyOptional<IEnergyStorage> energy_handler_ = battery_.createEnergyHandler();
    private final Fluidics.Tank tank_ = new Fluidics.Tank(MAX_FLUID_LEVEL, 0, 100);
    private final LazyOptional<? extends IFluidHandler> fluid_handler_ = tank_.createOutputFluidHandler();
    private final Inventories.StorageInventory main_inventory_;
    private final LazyOptional<? extends IItemHandler> item_handler_;

    static {
      accepted_lava_contrainers.add(Items.BUCKET);
    }

    public static void on_config(int consumption, int heatup_per_second)
    {
      energy_consumption = Mth.clamp(consumption, 8, 4096);
      heatup_rate = Mth.clamp(heatup_per_second, 1, 5);
      cooldown_rate = Mth.clamp(heatup_per_second/2, 1, 5);
      ModConfig.log("Config mineal smelter: energy consumption:" + energy_consumption + "rf/t, heat-up rate: " + heatup_rate + "%/s.");
    }

    public MineralSmelterTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.TET_MINERAL_SMELTER, pos, state);
      main_inventory_ = (new Inventories.StorageInventory(this, NUM_OF_SLOTS, 1))
        .setStackLimit(1)
        .setValidator((index,stack)-> ((index==1) || ((index==0) && accepts_input(stack))))
        .setSlotChangeAction((slot,stack)->{
          //System.out.println("slot"+slot+"<<"+stack);
        });
      item_handler_ = Inventories.MappedItemHandler.createGenericHandler(
        main_inventory_,
        (index,stack)->((index==1) && (phase()!=PHASE_LAVA)),
        (index,stack)->((index==0) && accepts_input(stack))
      );
    }

    public int progress()
    { return progress_; }

    public int phase()
    {
      if(progress_ >= 100) return PHASE_LAVA;
      if(progress_ >=  90) return PHASE_MAGMABLOCK;
      if(progress_ >=   5) return PHASE_HOT;
      return PHASE_WARMUP;
    }

    public boolean fluid_extraction_possible()
    { return tank_.getFluidAmount() >= MAX_BUCKET_EXTRACT_FLUID_LEVEL; }

    public int comparator_signal()
    { return phase() * 5; }

    private boolean accepts_lava_container(ItemStack stack)
    { return accepted_lava_contrainers.contains(stack.getItem()); }

    private boolean accepts_input(ItemStack stack)
    {
      if(!main_inventory_.isEmpty()) {
        return false;
      } else if(fluid_extraction_possible()) {
        return accepts_lava_container(stack);
      } else {
        if(stack.getItem().getTags().contains(new ResourceLocation(Auxiliaries.modid(), "accepted_mineral_smelter_input"))) return true;
        return accepted_minerals.contains(stack.getItem());
      }
    }

    public boolean insert(final ItemStack stack, boolean simulate)
    {
      if(stack.isEmpty() || (!accepts_input(stack))) return false;
      if(!simulate) {
        final ItemStack st = stack.copy();
        st.setCount(1);
        main_inventory_.setItem(0, st);
        if(!accepts_lava_container(stack)) progress_ = 0;
        force_block_update_ = true;
      }
      return true;
    }

    public ItemStack extract(boolean simulate)
    {
      final ItemStack stack = main_inventory_.getItem(1).copy();
      if(stack.isEmpty()) return ItemStack.EMPTY;
      if(!simulate) reset_process();
      return stack;
    }

    protected void drain_lava_bucket()
    {
    }

    protected void reset_process()
    {
      main_inventory_.setItem(0, ItemStack.EMPTY);
      main_inventory_.setItem(1, ItemStack.EMPTY);
      tank_.clear();
      force_block_update_ = true;
      tick_timer_ = 0;
      progress_ = 0;
    }

    public void readnbt(CompoundTag nbt)
    {
      main_inventory_.load(nbt);
      battery_.load(nbt);
      tank_.load(nbt);
      progress_ = nbt.getInt("progress");
    }

    protected void writenbt(CompoundTag nbt)
    {
      main_inventory_.save(nbt);
      battery_.save(nbt);
      tank_.save(nbt);
      nbt.putInt("progress", Mth.clamp(progress_,0 , 100));
    }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt); }

    @Override
    public CompoundTag save(CompoundTag nbt)
    { super.save(nbt); writenbt(nbt); return nbt; }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
      fluid_handler_.invalidate();
      item_handler_.invalidate();
    }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluid_handler_.cast();
      if(capability == CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickable ------------------------------------------------------------------------------------

    @Override
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof MineralSmelterBlock)) return;
      boolean dirty = false;
      final int last_phase = phase();
      final ItemStack istack = main_inventory_.getItem(0);
      if(istack.isEmpty() && (tank_.getFluidAmount()<1000)) {
        progress_ = 0;
      } else if((battery_.isEmpty()) || (level.hasNeighborSignal(worldPosition))) {
        progress_ = Mth.clamp(progress_-cooldown_rate, 0,100);
      } else if(progress_ >= 100) {
        progress_ = 100;
        if(!battery_.draw(energy_consumption*TICK_INTERVAL/20)) battery_.clear();
      } else if((phase()>=PHASE_LAVA) || (!istack.isEmpty())) {
        if(!battery_.draw(energy_consumption*TICK_INTERVAL)) battery_.clear();
        progress_ = Mth.clamp(progress_+heatup_rate, 0, 100);
      }
      int new_phase = phase();
      if(accepts_lava_container(istack)) {
        // That stays in the slot until its extracted or somone takes it out.
        if(istack.sameItem(BUCKET_STACK)) {
          if(!main_inventory_.getItem(1).sameItem(LAVA_BUCKET_STACK)) {
            if(fluid_extraction_possible()) {
              main_inventory_.setItem(1, LAVA_BUCKET_STACK);
              level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.2f, 1.3f);
            } else {
              main_inventory_.setItem(1, istack.copy());
            }
            dirty = true;
          }
        } else {
          main_inventory_.setItem(1, istack.copy());
          // Out stack -> Somehow the filled container or container with fluid+fluid_level().
        }
      } else if(new_phase > last_phase) {
        // Heat-up to next phase happened.
        switch (new_phase) {
          case PHASE_LAVA -> {
            tank_.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
            main_inventory_.setItem(1, ItemStack.EMPTY);
            main_inventory_.setItem(0, ItemStack.EMPTY);
            level.playSound(null, worldPosition, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2f, 1.0f);
            dirty = true;
          }
          case PHASE_MAGMABLOCK -> {
            main_inventory_.setItem(1, MAGMA_STACK.copy());
            level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.2f, 0.8f);
            dirty = true;
          }
          case PHASE_HOT -> {
            level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.2f, 0.8f);
          }
        }
      } else if(new_phase < last_phase) {
        // Cool-down to prev phase happened.
        switch(new_phase) {
          case PHASE_MAGMABLOCK -> {
            if(tank_.getFluidAmount() < 1000) {
              reset_process();
            } else {
              main_inventory_.setItem(0, (tank_.getFluidAmount() >= MAX_BUCKET_EXTRACT_FLUID_LEVEL) ? (MAGMA_STACK.copy()) : (ItemStack.EMPTY));
              main_inventory_.setItem(1, main_inventory_.getItem(0).copy());
              tank_.clear();
            }
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.1f);
            dirty = true;
          }
          case PHASE_HOT -> {
            if(istack.sameItem(MAGMA_STACK)) {
              main_inventory_.setItem(1, new ItemStack(Blocks.OBSIDIAN));
            } else {
              main_inventory_.setItem(1, new ItemStack(Blocks.COBBLESTONE));
            }
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 0.9f);
            dirty = true;
          }
          case PHASE_WARMUP -> {
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 0.7f);
          }
        }
      } else if((phase()==PHASE_LAVA) && (tank_.getFluidAmount()>0)) {
        // Phase unchanged, fluid transfer check.
        FluidStack fs = tank_.getFluid().copy();
        if(fs.getAmount() > 100) fs.setAmount(100);
        final int n = Fluidics.fill(level, getBlockPos().below(), Direction.UP, fs);
        if(n > 0) {
          tank_.drain(n);
          if(tank_.isEmpty()) {
            reset_process();
            level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 0.7f);
          }
        }
      }
      // Block state
      if((force_block_update_ || (state.getValue(MineralSmelterBlock.PHASE) != new_phase))) {
        state = state.setValue(MineralSmelterBlock.PHASE, new_phase);
        level.setBlock(worldPosition, state,3|16);
        level.updateNeighborsAt(getBlockPos(), state.getBlock());
        force_block_update_ = false;
      }
      if(dirty) setChanged();
    }
  }
}
