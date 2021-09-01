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

import net.minecraft.world.IWorldReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.*;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.fluid.Fluids;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class EdMineralSmelter
{
  public static void on_config(int consumption, int heatup_per_second)
  { MineralSmelterTileEntity.on_config(consumption, heatup_per_second); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class MineralSmelterBlock extends DecorBlock.Horizontal implements IDecorBlock
  {
    public static final int PHASE_MAX = 3;
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, PHASE_MAX);

    public MineralSmelterBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(PHASE); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).setValue(PHASE, 0); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos)
    { return MathHelper.clamp((state.getValue(PHASE)*5), 0, 15); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdMineralSmelter.MineralSmelterTileEntity(); }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {}

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof MineralSmelterTileEntity)) return stacks;
      ((MineralSmelterTileEntity)te).reset_process();
      stacks.add(new ItemStack(this, 1));
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(player.isShiftKeyDown()) return ActionResultType.PASS;
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      MineralSmelterTileEntity te = getTe(world, pos);
      if(te==null) return ActionResultType.FAIL;
      final ItemStack stack = player.getItemInHand(hand);
      boolean dirty = false;
      if(te.accepts_lava_container(stack)) {
        if(stack.sameItemStackIgnoreDurability(MineralSmelterTileEntity.BUCKET_STACK)) { // check how this works with item capabilities or so
          if(te.fluid_level() >= MineralSmelterTileEntity.MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
            if(stack.getCount() > 1) {
              int target_stack_index = -1;
              for(int i=0; i<player.inventory.getContainerSize(); ++i) {
                if(player.inventory.getItem(i).isEmpty()) {
                  target_stack_index = i;
                  break;
                }
              }
              if(target_stack_index >= 0) {
                te.reset_process();
                stack.shrink(1);
                player.setItemInHand(hand, stack);
                player.inventory.setItem(target_stack_index, MineralSmelterTileEntity.LAVA_BUCKET_STACK.copy());
                world.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1f, 1f);
                dirty = true;
              }
            } else {
              te.reset_process();
              player.setItemInHand(hand, MineralSmelterTileEntity.LAVA_BUCKET_STACK.copy());
              world.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1f, 1f);
              dirty = true;
            }
          }
        }
      } else if(stack.isEmpty()) {
        final ItemStack istack = te.getItem(1).copy();
        if(te.phase() > MineralSmelterTileEntity.PHASE_WARMUP) player.setSecondsOnFire(1);
        if(!istack.isEmpty()) {
          istack.setCount(1);
          player.setItemInHand(hand, istack);
          te.reset_process();
          dirty = true;
        }
      } else if(te.insert(stack.copy(),false)) {
        stack.shrink(1);
        dirty = true;
      }
      if(dirty) player.inventory.setChanged();
      return ActionResultType.CONSUME;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
    {
      if(state.getBlock()!=this) return;
      IParticleData particle = ParticleTypes.SMOKE;
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
    private MineralSmelterTileEntity getTe(World world, BlockPos pos)
    { final TileEntity te=world.getBlockEntity(pos); return (!(te instanceof MineralSmelterTileEntity)) ? (null) : ((MineralSmelterTileEntity)te); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class MineralSmelterTileEntity extends TileEntity implements INameable, ITickableTileEntity, ISidedInventory, IEnergyStorage, ICapabilityProvider
  {
    public static final int TICK_INTERVAL = 20;
    public static final int MAX_FLUID_LEVEL = 1000;
    public static final int MAX_BUCKET_EXTRACT_FLUID_LEVEL = 900;
    public static final int MAX_ENERGY_BUFFER = 32000;
    public static final int MAX_ENERGY_TRANSFER = 8192;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 92;
    public static final int DEFAULT_HEATUP_RATE = 2; // -> 50s for one smelting process
    public static final int PHASE_WARMUP = 0;
    public static final int PHASE_HOT = 1;
    public static final int PHASE_MAGMABLOCK = 2;
    public static final int PHASE_LAVA = 3;
    private static final ItemStack MAGMA_STACK = new ItemStack(Blocks.MAGMA_BLOCK);
    private static final ItemStack BUCKET_STACK = new ItemStack(Items.BUCKET);
    private static final ItemStack LAVA_BUCKET_STACK = new ItemStack(Items.LAVA_BUCKET);
    private static final FluidStack LAVA_BUCKET_FLUID_STACK = new FluidStack(Fluids.LAVA, 1000);
    private static final Set<Item> accepted_minerals = new HashSet<Item>();
    private static final Set<Item> accepted_lava_contrainers = new HashSet<Item>();
    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;
    private static int heatup_rate = DEFAULT_HEATUP_RATE;
    private static int cooldown_rate = 1;
    private int tick_timer_;
    private int energy_stored_;
    private int progress_;
    private int fluid_level_;
    private boolean force_block_update_;
    private NonNullList<ItemStack> stacks_ = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);

    static {
      // Lava containers
      accepted_lava_contrainers.add(Items.BUCKET);
    }

    public static void on_config(int consumption, int heatup_per_second)
    {
      energy_consumption = MathHelper.clamp(consumption, 8, 4096);
      heatup_rate = MathHelper.clamp(heatup_per_second, 1, 5);
      cooldown_rate = MathHelper.clamp(heatup_per_second/2, 1, 5);
      ModConfig.log("Config mineal smelter: energy consumption:" + energy_consumption + "rf/t, heat-up rate: " + heatup_rate + "%/s.");
    }

    public MineralSmelterTileEntity()
    { this(ModContent.TET_MINERAL_SMELTER); }

    public MineralSmelterTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public int progress()
    { return progress_; }

    public int phase()
    {
      if(progress_ >= 100) return PHASE_LAVA;
      if(progress_ >=  90) return PHASE_MAGMABLOCK;
      if(progress_ >=   5) return PHASE_HOT;
      return PHASE_WARMUP;
    }

    public int fluid_level()
    { return fluid_level_; }

    public int comparator_signal()
    { return phase() * 5; }

    private boolean accepts_lava_container(ItemStack stack)
    { return accepted_lava_contrainers.contains(stack.getItem()); }

    private boolean accepts_input(ItemStack stack)
    {
      if(!stacks_.get(0).isEmpty()) return false;
      if(fluid_level() > MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
        return accepts_lava_container(stack);
      } else {
        if(stack.getItem().getTags().contains(new ResourceLocation(ModEngineersDecor.MODID, "accepted_mineral_smelter_input"))) return true;
        return accepted_minerals.contains(stack.getItem());
      }
    }

    public boolean insert(final ItemStack stack, boolean simulate)
    {
      if(stack.isEmpty() || (!accepts_input(stack))) return false;
      if(!simulate) {
        ItemStack st = stack.copy();
        st.setCount(st.getMaxStackSize());
        stacks_.set(0, st);
        if(!accepts_lava_container(stack)) progress_ = 0;
        force_block_update_ = true;
      }
      return true;
    }

    public ItemStack extract(boolean simulate)
    {
      ItemStack stack = stacks_.get(1).copy();
      if(stack.isEmpty()) return ItemStack.EMPTY;
      if(!simulate) reset_process();
      return stack;
    }

    protected void reset_process()
    {
      stacks_ = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);
      force_block_update_ = true;
      fluid_level_ = 0;
      tick_timer_ = 0;
      progress_ = 0;
    }

    public void readnbt(CompoundNBT nbt)
    {
      energy_stored_ = nbt.getInt("energy");
      progress_ = nbt.getInt("progress");
      fluid_level_ = nbt.getInt("fluidlevel");
      ItemStackHelper.loadAllItems(nbt, stacks_);
      if(stacks_.size() != 2) reset_process();
    }

    protected void writenbt(CompoundNBT nbt)
    {
      nbt.putInt("energy", MathHelper.clamp(energy_stored_,0 , MAX_ENERGY_BUFFER));
      nbt.putInt("progress", MathHelper.clamp(progress_,0 , 100));
      nbt.putInt("fluidlevel", MathHelper.clamp(fluid_level_,0 , MAX_FLUID_LEVEL));
      ItemStackHelper.saveAllItems(nbt, stacks_);
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
      fluid_handler_.invalidate();
      item_handler_.invalidate();
    }

    // INamedContainerProvider / INameable ------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getDescriptionId() : "Lab furnace"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public ITextComponent getCustomName()
    { return getName(); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public int getContainerSize()
    { return stacks_.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getItem(int index)
    { return ((index >= 0) && (index < getContainerSize())) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int index, int count)
    { return ItemStackHelper.removeItem(stacks_, index, count); }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    { return ItemStackHelper.takeItem(stacks_, index); }

    @Override
    public void setItem(int index, ItemStack stack)
    { if(stack.getCount()>getMaxStackSize()){stack.setCount(getMaxStackSize());} stacks_.set(index, stack); setChanged(); }

    @Override
    public int getMaxStackSize()
    { return 1; }

    @Override
    public void setChanged()
    { super.setChanged(); }

    @Override
    public boolean stillValid(PlayerEntity player)
    { return ((getLevel().getBlockEntity(getBlockPos()) == this)) && (getBlockPos().distSqr(player.blockPosition()) < 64); }

    @Override
    public void startOpen(PlayerEntity player)
    {}

    @Override
    public void stopOpen(PlayerEntity player)
    { setChanged(); }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    { return ((index==0) && accepts_input(stack)) || (index==1); }

    @Override
    public void clearContent()
    { reset_process(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS = new int[] {0,1};

    @Override
    public int[] getSlotsForFace(Direction side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction)
    { return (index==0) && canPlaceItem(index, stack); }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction)
    { return (index==1) && (!stacks_.get(1).isEmpty()); }

    // IItemHandler  --------------------------------------------------------------------------------

    private final LazyOptional<IItemHandler> item_handler_ = LazyOptional.of(() -> (IItemHandler)new BItemHandler(this));

    protected static class BItemHandler implements IItemHandler
    {
      private final MineralSmelterTileEntity te;

      BItemHandler(MineralSmelterTileEntity te)
      { this.te = te; }

      @Override
      public int getSlots()
      { return 2; }

      @Override
      public int getSlotLimit(int index)
      { return te.getMaxStackSize(); }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      { return te.canPlaceItem(slot, stack); }

      @Override
      @Nonnull
      public ItemStack insertItem(int index, @Nonnull ItemStack stack, boolean simulate)
      {
        ItemStack rstack = stack.copy();
        if((index!=0) || (!te.insert(stack.copy(), simulate))) return rstack;
        rstack.shrink(1);
        return rstack;
      }

      @Override
      @Nonnull
      public ItemStack extractItem(int index, int amount, boolean simulate)
      { return (index!=1) ? ItemStack.EMPTY : te.extract(simulate); }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      { return te.getItem(index); }
    }

    // IFluidHandler  --------------------------------------------------------------------------------

    private final LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)new BFluidHandler(this));

    private static class BFluidHandler implements IFluidHandler
    {
      private final FluidStack lava;
      private final MineralSmelterTileEntity te;

      BFluidHandler(MineralSmelterTileEntity te)
      { this.te = te; lava = new net.minecraftforge.fluids.FluidStack(net.minecraft.fluid.Fluids.LAVA, 1); }

      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return new FluidStack(lava, te.fluid_level()); }
      @Override public int getTankCapacity(int tank) { return 1000; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return (tank==0) && (stack.isFluidEqual(lava)); }
      @Override public int fill(FluidStack resource, FluidAction action)  { return 0; }

      @Override
      public FluidStack drain(FluidStack resource, FluidAction action)
      { return resource.isFluidEqual(lava) ? drain(resource.getAmount(), action) : FluidStack.EMPTY; }

      @Override
      public FluidStack drain(int maxDrain, FluidAction action)
      {
        maxDrain = Math.min(maxDrain, te.fluid_level());
        if(action == FluidAction.EXECUTE) te.fluid_level_ -= maxDrain;
        return (maxDrain > 0) ? (new FluidStack(lava, maxDrain)) : FluidStack.EMPTY;
      }
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
    { return MAX_ENERGY_BUFFER; }

    @Override
    public int getEnergyStored()
    { return energy_stored_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
      if(energy_stored_ >= MAX_ENERGY_BUFFER) return 0;
      int n = Math.min(maxReceive, (MAX_ENERGY_BUFFER - energy_stored_));
      if(n > MAX_ENERGY_TRANSFER) n = MAX_ENERGY_TRANSFER;
      if(!simulate) {energy_stored_ += n; setChanged(); }
      return n;
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
      if(level.isClientSide) return;
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof MineralSmelterBlock)) return;
      boolean dirty = false;
      final int last_phase = phase();
      final ItemStack istack = stacks_.get(0);
      if(istack.isEmpty() && (fluid_level()==0)) {
        progress_ = 0;
      } else if((energy_stored_ <= 0) || (level.hasNeighborSignal(worldPosition))) {
        progress_ = MathHelper.clamp(progress_-cooldown_rate, 0,100);
      } else if(progress_ >= 100) {
        progress_ = 100;
        energy_stored_ = MathHelper.clamp(energy_stored_-((energy_consumption*TICK_INTERVAL)/20), 0, MAX_ENERGY_BUFFER);
      } else {
        energy_stored_ = MathHelper.clamp(energy_stored_-(energy_consumption*TICK_INTERVAL), 0, MAX_ENERGY_BUFFER);
        progress_ = MathHelper.clamp(progress_+heatup_rate, 0, 100);
      }
      int new_phase = phase();
      boolean is_lava_container = accepts_lava_container(istack);
      if(is_lava_container || (new_phase != last_phase)) {
        if(is_lava_container)  {
          // That stays in the slot until its extracted or somone takes it out.
          if(istack.sameItem(BUCKET_STACK)) {
            if(!stacks_.get(1).sameItem(LAVA_BUCKET_STACK)) {
              if(fluid_level() >= MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
                stacks_.set(1, LAVA_BUCKET_STACK);
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 0.2f, 1.3f);
              } else {
                stacks_.set(1, istack.copy());
              }
              dirty = true;
            }
          } else {
            stacks_.set(1, istack.copy());
            // Out stack -> Somehow the filled container or container with fluid+fluid_level().
          }
        } else if(new_phase > last_phase) {
          switch(new_phase) {
            case PHASE_LAVA:
              fluid_level_ = MAX_FLUID_LEVEL;
              stacks_.set(1, ItemStack.EMPTY);
              stacks_.set(0, ItemStack.EMPTY);
              level.playSound(null, worldPosition, SoundEvents.LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2f, 1.0f);
              dirty = true;
              break;
            case PHASE_MAGMABLOCK:
              stacks_.set(1, MAGMA_STACK.copy());
              level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundCategory.BLOCKS, 0.2f, 0.8f);
              dirty = true;
              break;
            case PHASE_HOT:
              level.playSound(null, worldPosition, SoundEvents.FIRE_AMBIENT, SoundCategory.BLOCKS, 0.2f, 0.8f);
              break;
          }
        } else {
          switch(new_phase) {
            case PHASE_MAGMABLOCK:
              stacks_.set(0, (fluid_level_ >= MAX_BUCKET_EXTRACT_FLUID_LEVEL) ? (MAGMA_STACK.copy()) : (ItemStack.EMPTY));
              stacks_.set(1, stacks_.get(0).copy());
              fluid_level_ = 0;
              level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.1f);
              dirty = true;
              break;
            case PHASE_HOT:
              if(istack.sameItem(MAGMA_STACK)) {
                stacks_.set(1, new ItemStack(Blocks.OBSIDIAN));
              } else {
                stacks_.set(1, new ItemStack(Blocks.COBBLESTONE));
              }
              level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.9f);
              dirty = true;
              break;
            case PHASE_WARMUP:
              level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.7f);
              break;
          }
        }
      } else if((phase()==PHASE_LAVA) && (fluid_level() >= MAX_BUCKET_EXTRACT_FLUID_LEVEL)) {
        // Fluid transfer check
        final IFluidHandler fh = FluidUtil.getFluidHandler(level, getBlockPos().below(), Direction.UP).orElse(null);
        if(fh != null) {
          int n = fh.fill(LAVA_BUCKET_FLUID_STACK.copy(), FluidAction.SIMULATE);
          if(n >= LAVA_BUCKET_FLUID_STACK.getAmount()/2) {
            n = fh.fill(LAVA_BUCKET_FLUID_STACK.copy(), FluidAction.EXECUTE);
            if(n > 0) {
              reset_process();
              level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.7f);
            }
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
