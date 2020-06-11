/*
 * @file EdElectricalFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED small electrical pass-through furnace.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.TooltipDisplay;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.stats.Stats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import com.mojang.blaze3d.platform.GlStateManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;


public class EdElectricalFurnace
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceBlock extends EdFurnace.FurnaceBlock implements IDecorBlock
  {
    public ElectricalFurnaceBlock(long config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new ElectricalFurnaceTileEntity(); }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return true;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof ElectricalFurnaceTileEntity)) return true;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return true;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      player.addStat(Stats.INTERACT_WITH_FURNACE);
      return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      world.setBlockState(pos, state.with(LIT, false));
      if(world.isRemote) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundNBT inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof ElectricalFurnaceTileEntity)) return;
      ElectricalFurnaceTileEntity bte = (ElectricalFurnaceTileEntity)te;
      bte.readnbt(inventory_nbt);
      bte.markDirty();
      world.setBlockState(pos, state.with(LIT, bte.burning()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
    {}
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceTileEntity extends EdFurnace.FurnaceTileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider, ISidedInventory, IEnergyStorage
  {
    public static final IRecipeType<FurnaceRecipe> RECIPE_TYPE = IRecipeType.SMELTING;
    public static final int NUM_OF_FIELDS = 7;
    public static final int TICK_INTERVAL = 4;
    public static final int FIFO_INTERVAL = 20;
    public static final int HEAT_CAPACITY = 200;
    public static final int HEAT_INCREMENT = 20;
    public static final int MAX_ENERGY_TRANSFER = 1024;
    public static final int MAX_ENERGY_BUFFER = 32000;
    public static final int MAX_SPEED_SETTING = 3;
    public static final int NUM_OF_SLOTS = 7;
    public static final int SMELTING_INPUT_SLOT_NO = 0;
    public static final int SMELTING_AUX_SLOT_NO = 1;
    public static final int SMELTING_OUTPUT_SLOT_NO = 2;
    public static final int FIFO_INPUT_0_SLOT_NO = 3;
    public static final int FIFO_INPUT_1_SLOT_NO = 4;
    public static final int FIFO_OUTPUT_0_SLOT_NO = 5;
    public static final int FIFO_OUTPUT_1_SLOT_NO = 6;
    public static final int DEFAULT_SPEED_PERCENT = 200;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 16;
    public static final int DEFAULT_SCALED_ENERGY_CONSUMPTION = DEFAULT_ENERGY_CONSUMPTION * HEAT_INCREMENT * DEFAULT_SPEED_PERCENT / 100;

    // Config ----------------------------------------------------------------------------------

    private static boolean with_automatic_inventory_pulling_ = false;
    private static int energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION;
    private static int transfer_energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION / 8;
    private static int proc_speed_percent_ = DEFAULT_SPEED_PERCENT;
    private static double speed_setting_factor_[] = {0.0, 1.0, 1.5, 2.0};

    public static void on_config(int speed_percent, int standard_energy_per_tick, boolean with_automatic_inventory_pulling)
    {
      proc_speed_percent_ = MathHelper.clamp(speed_percent, 10, 500);
      energy_consumption_ = MathHelper.clamp(standard_energy_per_tick, 4, 4096) * HEAT_INCREMENT * proc_speed_percent_ / 100;
      transfer_energy_consumption_ = MathHelper.clamp(energy_consumption_ / 8, 8, HEAT_INCREMENT);
      with_automatic_inventory_pulling_ = with_automatic_inventory_pulling;
      ModEngineersDecor.logger().info("Config electrical furnace speed:" + proc_speed_percent_ + ", power consumption:" + energy_consumption_);
    }

    // DecorFurnaceElectrical -----------------------------------------------------------------------------

    private int burntime_left_ = 0;
    private int proc_time_elapsed_ = 0;
    private int proc_time_needed_ = 0;
    private int energy_stored_ = 0;
    private int field_max_energy_stored_ = 0;
    private int field_isburning_ = 0;
    private int speed_ = 1;
    private int tick_timer_ = 0;
    private int fifo_timer_ = 0;
    private boolean enabled_ = false;

    public ElectricalFurnaceTileEntity()
    { this(ModContent.TET_SMALL_ELECTRICAL_FURNACE); }

    public ElectricalFurnaceTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void reset()
    {
      super.reset();
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      burntime_left_ = 0;
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
      energy_stored_ = 0;
      speed_ = 1;
      field_max_energy_stored_ = getMaxEnergyStored();
      field_isburning_ = 0;
    }

    public void readnbt(CompoundNBT nbt)
    {
      ItemStackHelper.loadAllItems(nbt, this.stacks_);
      while(this.stacks_.size() < NUM_OF_SLOTS) this.stacks_.add(ItemStack.EMPTY);
      burntime_left_ = nbt.getInt("BurnTime");
      proc_time_elapsed_ = nbt.getInt("CookTime");
      proc_time_needed_ = nbt.getInt("CookTimeTotal");
      energy_stored_ = nbt.getInt("Energy");
      speed_ = nbt.getInt("SpeedSetting");
      speed_ = (speed_ < 0) ? (1) : ((speed_>MAX_SPEED_SETTING) ? MAX_SPEED_SETTING : speed_);
    }

    protected void writenbt(CompoundNBT nbt)
    {
      nbt.putInt("BurnTime", MathHelper.clamp(burntime_left_, 0, HEAT_CAPACITY));
      nbt.putInt("CookTime", MathHelper.clamp(proc_time_elapsed_, 0, MAX_BURNTIME));
      nbt.putInt("CookTimeTotal", MathHelper.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      nbt.putInt("Energy", MathHelper.clamp(energy_stored_, 0, MAX_ENERGY_BUFFER));
      nbt.putInt("SpeedSetting", MathHelper.clamp(speed_, 0, MAX_SPEED_SETTING));
      ItemStackHelper.saveAllItems(nbt, stacks_);
    }

    public int getComparatorOutput()
    {
      return (energy_stored_ <= 0) ? (0) : (
        (stacks_.get(FIFO_INPUT_1_SLOT_NO).isEmpty() ? 0 : 5) +
        (stacks_.get(FIFO_INPUT_0_SLOT_NO).isEmpty() ? 0 : 5) +
        (stacks_.get(SMELTING_INPUT_SLOT_NO).isEmpty() ? 0 : 5)
      );
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      Arrays.stream(item_handlers).forEach(LazyOptional::invalidate);
      item_handler_.invalidate();
      energy_handler_.invalidate();
    }

    // INameable -------------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Small electrical furnace"); }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new ElectricalFurnaceContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
      switch(index) {
        case SMELTING_INPUT_SLOT_NO:
        case FIFO_INPUT_0_SLOT_NO:
        case FIFO_INPUT_1_SLOT_NO:
          return true;
        default:
          return false;
      }
    }

    @Override
    public ItemStack getStackInSlot(int index)
    { return ((index < 0) || (index >= SIDED_INV_SLOTS.length)) ? ItemStack.EMPTY : stacks_.get(SIDED_INV_SLOTS[index]); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(ElectricalFurnaceTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          case 0: return ElectricalFurnaceTileEntity.this.burntime_left_;
          case 1: return ElectricalFurnaceTileEntity.this.energy_stored_;
          case 2: return ElectricalFurnaceTileEntity.this.proc_time_elapsed_;
          case 3: return ElectricalFurnaceTileEntity.this.proc_time_needed_;
          case 4: return ElectricalFurnaceTileEntity.this.speed_;
          case 5: return ElectricalFurnaceTileEntity.this.field_max_energy_stored_;
          case 6: return ElectricalFurnaceTileEntity.this.field_isburning_;
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0: ElectricalFurnaceTileEntity.this.burntime_left_ = value; break;
          case 1: ElectricalFurnaceTileEntity.this.energy_stored_ = value; break;
          case 2: ElectricalFurnaceTileEntity.this.proc_time_elapsed_ = value; break;
          case 3: ElectricalFurnaceTileEntity.this.proc_time_needed_ = value; break;
          case 4: ElectricalFurnaceTileEntity.this.speed_ = value; break;
          case 5: ElectricalFurnaceTileEntity.this.field_max_energy_stored_ = value; break;
          case 6: ElectricalFurnaceTileEntity.this.field_isburning_ = value; break;
        }
      }
    };

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS = new int[] {
      SMELTING_INPUT_SLOT_NO, SMELTING_AUX_SLOT_NO, SMELTING_OUTPUT_SLOT_NO,
      FIFO_INPUT_0_SLOT_NO, FIFO_INPUT_1_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO
    };

    @Override
    public int[] getSlotsForFace(Direction side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, Direction direction)
    { return ((index==FIFO_INPUT_0_SLOT_NO) || (index==FIFO_INPUT_1_SLOT_NO)) && isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction)
    { return (index==FIFO_OUTPUT_0_SLOT_NO) || (index==FIFO_OUTPUT_1_SLOT_NO); }

    // IEnergyStorage ----------------------------------------------------------------------------

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
      if(!simulate) {energy_stored_ += n; markDirty(); }
      return n;
    }

    // IItemHandler  --------------------------------------------------------------------------------

    protected static class BItemHandler implements IItemHandler
    {
      private ElectricalFurnaceTileEntity te;

      BItemHandler(ElectricalFurnaceTileEntity te)
      { this.te = te; }

      @Override
      public int getSlots()
      { return SIDED_INV_SLOTS.length; }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      { return te.getStackInSlot(index); }

      @Override
      public int getSlotLimit(int index)
      { return te.getInventoryStackLimit(); }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      { return true; }

      @Override
      @Nonnull
      public ItemStack insertItem(int index, @Nonnull ItemStack stack, boolean simulate)
      {
        if(stack.isEmpty()) return ItemStack.EMPTY;
        if((index < 0) || (index >= SIDED_INV_SLOTS.length)) return ItemStack.EMPTY;
        int slotno = SIDED_INV_SLOTS[index];
        ItemStack slotstack = getStackInSlot(slotno);
        if(!slotstack.isEmpty()) {
          if(slotstack.getCount() >= Math.min(slotstack.getMaxStackSize(), getSlotLimit(index))) return stack;
          if(!ItemHandlerHelper.canItemStacksStack(stack, slotstack)) return stack;
          if(!te.canInsertItem(slotno, stack, Direction.UP) || (!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index)) - slotstack.getCount();
          if(stack.getCount() <= n) {
            if(!simulate) {
              ItemStack copy = stack.copy();
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
            }
            return ItemStack.EMPTY;
          } else {
            stack = stack.copy();
            if(!simulate) {
              ItemStack copy = stack.split(n);
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
              return stack;
            } else {
              stack.shrink(n);
              return stack;
            }
          }
        } else {
          if(!te.canInsertItem(slotno, stack, Direction.UP) || (!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index));
          if(n < stack.getCount()) {
            stack = stack.copy();
            if(!simulate) {
              te.setInventorySlotContents(slotno, stack.split(n));
              return stack;
            } else {
              stack.shrink(n);
              return stack;
            }
          } else {
            if(!simulate) te.setInventorySlotContents(slotno, stack);
            return ItemStack.EMPTY;
          }
        }
      }

      @Override
      @Nonnull
      public ItemStack extractItem(int index, int amount, boolean simulate)
      {
        if(amount == 0) return ItemStack.EMPTY;
        if((index < 0) || (index >= SIDED_INV_SLOTS.length)) return ItemStack.EMPTY;
        int slotno = SIDED_INV_SLOTS[index];
        ItemStack stackInSlot = getStackInSlot(slotno);
        if(stackInSlot.isEmpty()) return ItemStack.EMPTY;
        if(!te.canExtractItem(slotno, stackInSlot, Direction.DOWN)) return ItemStack.EMPTY;
        if(simulate) {
          if(stackInSlot.getCount() < amount) return stackInSlot.copy();
          ItemStack ostack = stackInSlot.copy();
          ostack.setCount(amount);
          return ostack;
        } else {
          ItemStack ostack = te.decrStackSize(slotno, Math.min(stackInSlot.getCount(), amount));
          te.markDirty();
          return ostack;
        }
      }
    }

    // Capability export ----------------------------------------------------------------------------

    protected LazyOptional<IItemHandler> item_handler_ = LazyOptional.of(() -> new BItemHandler(this));
    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      if(capability == CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickableTileEntity -------------------------------------------------------------------------

    @Override
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final boolean was_burning = burning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(world.isRemote) return;
      boolean update_blockstate = (was_burning != burning());
      boolean dirty = update_blockstate;
      boolean shift_in = false;
      boolean shift_out = false;
      if(--fifo_timer_ <= 0) {
        fifo_timer_ = FIFO_INTERVAL/TICK_INTERVAL;
        if(transferItems(FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO, 64)) { dirty = true; } else { shift_out = true; }
        if(transferItems(SMELTING_OUTPUT_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, 64)) dirty = true;
        if(transferItems(FIFO_INPUT_0_SLOT_NO, SMELTING_INPUT_SLOT_NO, 64)) dirty = true;
        if(transferItems(FIFO_INPUT_1_SLOT_NO, FIFO_INPUT_0_SLOT_NO, 64)) { dirty = true; } else { shift_in = true; }
      }
      if(energy_stored_ < energy_consumption()) {
        enabled_ = false;
      } else if(energy_stored_ >= (MAX_ENERGY_BUFFER/2)) {
        enabled_ = true;
      }
      if((!(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty()) && (enabled_) && (speed_>0) && (speed_<=MAX_SPEED_SETTING)) {
        IRecipe last_recipe = currentRecipe();
        updateCurrentRecipe();
        if(currentRecipe() != last_recipe) {
          proc_time_elapsed_ = 0;
          proc_time_needed_ = getSmeltingTimeNeeded(world, stacks_.get(SMELTING_INPUT_SLOT_NO));
        }
        final boolean can_smelt = canSmeltCurrentItem();
        if((!can_smelt) && (getSmeltingResult(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
          // bypass
          if(transferItems(SMELTING_INPUT_SLOT_NO, SMELTING_OUTPUT_SLOT_NO, 1)) dirty = true;
        } else {
          // smelt
          if(!burning() && can_smelt) {
            if(heat_up()) { dirty = true; update_blockstate = true; }
          }
          if(burning() && can_smelt) {
            if(heat_up()) dirty = true;
            proc_time_elapsed_ += (int)(TICK_INTERVAL * proc_speed_percent_ * speed_setting_factor_[speed_] / 100);
            if(proc_time_elapsed_ >= proc_time_needed_) {
              proc_time_elapsed_ = 0;
              proc_time_needed_ = getSmeltingTimeNeeded(world, stacks_.get(SMELTING_INPUT_SLOT_NO));
              smeltCurrentItem();
              dirty = true;
              shift_out = true;
            }
          } else {
            proc_time_elapsed_ = 0;
          }
        }
      } else if(proc_time_elapsed_ > 0) {
        proc_time_elapsed_ -= ((stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty() ? 20 : 1);
        if(proc_time_elapsed_ < 0) { proc_time_elapsed_ = 0; shift_out = true; update_blockstate = true; }
      }
      if(update_blockstate) {
        dirty = true;
        sync_blockstate();
      }
      if(adjacent_inventory_shift(shift_in, shift_out)) dirty = true;
      if(dirty) markDirty();
      field_max_energy_stored_ = getMaxEnergyStored();
      field_isburning_ = burning() ? 1 : 0;
    }

    // Furnace --------------------------------------------------------------------------------------

    protected void updateCurrentRecipe()
    { setCurrentRecipe(getSmeltingResult(RECIPE_TYPE, world, stacks_.get(SMELTING_INPUT_SLOT_NO))); }

    public boolean burning()
    { return burntime_left_ > 0; }

    private boolean is_accepted_hopper(final ItemStack stack)
    { return (stack.getItem() == Blocks.HOPPER.asItem()) || (stack.getItem() == ModContent.FACTORY_HOPPER.asItem()); }

    private boolean transferItems(final int index_from, final int index_to, int count)
    {
      ItemStack from = stacks_.get(index_from);
      if(from.isEmpty()) return false;
      ItemStack to = stacks_.get(index_to);
      if(from.getCount() < count) count = from.getCount();
      if(count <= 0) return false;
      boolean changed = true;
      if(to.isEmpty()) {
        stacks_.set(index_to, from.split(count));
      } else if(to.getCount() >= to.getMaxStackSize()) {
        changed = false;
      } else if(Inventories.areItemStacksDifferent(from, to)) {
        changed = false;
      } else {
        if((to.getCount()+count) >= to.getMaxStackSize()) {
          from.shrink(to.getMaxStackSize()-to.getCount());
          to.setCount(to.getMaxStackSize());
        } else {
          from.shrink(count);
          to.grow(count);
        }
      }
      if(from.isEmpty() && from!=ItemStack.EMPTY) {
        stacks_.set(index_from, ItemStack.EMPTY);
        changed = true;
      }
      return changed;
    }

    private boolean adjacent_inventory_shift(boolean inp, boolean out)
    {
      boolean dirty = false;
      if(energy_stored_  < transfer_energy_consumption_) return false;
      final BlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof ElectricalFurnaceBlock)) return false;
      final Direction out_facing = state.get(ElectricalFurnaceBlock.HORIZONTAL_FACING);
      if(out && (!stacks_.get(FIFO_OUTPUT_1_SLOT_NO).isEmpty())) {
        TileEntity te = world.getTileEntity(pos.offset(out_facing));
        if(te!=null) {
          IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out_facing).orElse(null);
          if(hnd != null) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(hnd, stacks_.get(FIFO_OUTPUT_1_SLOT_NO).copy(), false);
            stacks_.set(FIFO_OUTPUT_1_SLOT_NO, remaining);
            energy_stored_ -= transfer_energy_consumption_;
            dirty = true;
          }
        }
      }
      if(with_automatic_inventory_pulling_ || is_accepted_hopper(stacks_.get(SMELTING_AUX_SLOT_NO))) {
        final Direction inp_facing = state.get(ElectricalFurnaceBlock.HORIZONTAL_FACING).getOpposite();
        if(inp && (stacks_.get(FIFO_INPUT_1_SLOT_NO).isEmpty())) {
          TileEntity te = world.getTileEntity(pos.offset(inp_facing));
          if(te!=null) {
            IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inp_facing).orElse(null);
            if(hnd != null) {
            for(int i=0; i< hnd.getSlots(); ++i) {
                ItemStack adj_stack = hnd.getStackInSlot(i);
                if(!adj_stack.isEmpty()) {
                  ItemStack my_stack = adj_stack.copy();
                  if(my_stack.getCount() > getInventoryStackLimit()) my_stack.setCount(getInventoryStackLimit());
                  adj_stack.shrink(my_stack.getCount());
                  stacks_.set(FIFO_INPUT_1_SLOT_NO, my_stack);
                  energy_stored_ -= transfer_energy_consumption_;
                  dirty = true;
                  break;
                }
              }
            }
          }
        }
      }
      return dirty;
    }

    int energy_consumption()
    {
      switch(speed_) {
        case 1: return energy_consumption_;
        case 2: return energy_consumption_ * 2;
        case 3: return energy_consumption_ * 4;
        default: return 0;
      }
    }

    private boolean heat_up()
    {
      int p = energy_consumption();
      if((p<=0) || (energy_stored_ < p)) return false;
      if(burntime_left_ >= (HEAT_CAPACITY-HEAT_INCREMENT)) return false;
      energy_stored_ -= p;
      burntime_left_ += HEAT_INCREMENT;
      this.markDirty();
      return true; // returns TE dirty
    }

    private void sync_blockstate()
    {
      final BlockState state = world.getBlockState(pos);
      if((state.getBlock() instanceof ElectricalFurnaceBlock) && (state.get(ElectricalFurnaceBlock.LIT) != burning())) {
        world.setBlockState(pos, state.with(ElectricalFurnaceBlock.LIT, burning()), 2);
      }
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    private static final int PLAYER_INV_START_SLOTNO = 7;
    protected final PlayerEntity player_;
    protected final IInventory inventory_;
    protected final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final IRecipeType<? extends AbstractCookingRecipe> recipe_type_;

    public int field(int index) { return fields_.get(index); }
    public PlayerEntity player() { return player_ ; }
    public IInventory inventory() { return inventory_ ; }
    public World world() { return player_.world; }

    public ElectricalFurnaceContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(ElectricalFurnaceTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new IntArray(ElectricalFurnaceTileEntity.NUM_OF_FIELDS)); }

    private ElectricalFurnaceContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_SMALL_ELECTRICAL_FURNACE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      recipe_type_ = ElectricalFurnaceTileEntity.RECIPE_TYPE;
      addSlot(new Slot(inventory_, 0, 59, 28)); // smelting input
      addSlot(new Slot(inventory_, 1, 16, 52)); // aux
      addSlot(new EdFurnace.FurnaceContainer.BSlotResult(player_, inventory_, 2, 101, 28)); // smelting result
      addSlot(new EdFurnace.FurnaceContainer.BSlotInpFifo(inventory_, 3, 34, 28)); // input fifo 0
      addSlot(new EdFurnace.FurnaceContainer.BSlotInpFifo(inventory_, 4, 16, 28)); // input fifo 1
      addSlot(new EdFurnace.FurnaceContainer.BSlotOutFifo(player_, inventory_, 5, 126, 28)); // out fifo 0
      addSlot(new EdFurnace.FurnaceContainer.BSlotOutFifo(player_, inventory_, 6, 144, 28)); // out fifo 1
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
      this.trackIntArray(fields_); // === Add reference holders
    }

    @Override
    public boolean canInteractWith(PlayerEntity player)
    { return inventory_.isUsableByPlayer(player); }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index==2) || (index==5) || (index==6)) {
        // Output slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if(index==1) {
        // Bypass slot
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(ElectricalFurnaceTileEntity.canSmelt(world(), slot_stack)) {
          if(
            (!mergeItemStack(slot_stack, 0, 1, false)) && // smelting input
            (!mergeItemStack(slot_stack, 3, 4, false)) && // fifo0
            (!mergeItemStack(slot_stack, 4, 5, false))    // fifo1
          ) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO) && (index < PLAYER_INV_START_SLOTNO+27)) {
          // player inventory --> player hotbar
          if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO+27, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO+27) && (index < PLAYER_INV_START_SLOTNO+36) && (!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+27, false))) {
          // player hotbar --> player inventory
          return ItemStack.EMPTY;
        }
      } else {
        // invalid slot
        return ItemStack.EMPTY;
      }
      if(slot_stack.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }
      if(slot_stack.getCount() == transferred.getCount()) return ItemStack.EMPTY;
      slot.onTake(player, slot_stack);
      return transferred;
    }

    // INetworkSynchronisableContainer ---------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(CompoundNBT nbt)
    { Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt); }

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String key, int value)
    { CompoundNBT nbt=new CompoundNBT(); nbt.putInt(key, value); Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt); }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      if(!(inventory_ instanceof ElectricalFurnaceTileEntity)) return;
      ElectricalFurnaceTileEntity te = (ElectricalFurnaceTileEntity)inventory_;
      if(nbt.contains("speed")) te.speed_  = MathHelper.clamp(nbt.getInt("speed"), 0, ElectricalFurnaceTileEntity.MAX_SPEED_SETTING);
      te.markDirty();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class ElectricalFurnaceGui extends ContainerScreen<ElectricalFurnaceContainer>
  {
    protected final PlayerEntity player_;
    protected final TooltipDisplay tooltip_ = new TooltipDisplay();

    public ElectricalFurnaceGui(ElectricalFurnaceContainer container, PlayerInventory player_inventory, ITextComponent title)
    { super(container, player_inventory, title); this.player_ = player_inventory.player; }

    @Override
    public void init()
    {
      super.init();
      {
        final String prefix = ModContent.SMALL_ELECTRICAL_FURNACE.getTranslationKey() + ".tooltips.";
        final int x0 = getGuiLeft(), y0 = getGuiTop();
        final Slot aux = container.getSlot(ElectricalFurnaceTileEntity.SMELTING_AUX_SLOT_NO);
        tooltip_.init(
          new TipRange(x0+135, y0+50, 25, 25, new TranslationTextComponent(prefix + "speed")),
          new TipRange(x0+aux.xPos, y0+aux.yPos, 16, 16, new TranslationTextComponent(prefix + "auxslot"))
        );
      }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
      renderBackground();
      super.render(mouseX, mouseY, partialTicks);
      if(!tooltip_.render(this, mouseX, mouseY)) renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      minecraft.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_electrical_furnace_gui.png"));
      final int x0=(width-xSize)/2, y0=(height-ySize)/2, w=xSize, h=ySize;
      blit(x0, y0, 0, 0, w, h);
      if(getContainer().field(6)!=0)  {
        final int hi = 13;
        final int k = heat_px(hi);
        blit(x0+61, y0+53+hi-k, 177, hi-k, 13, k);
      }
      blit(x0+79, y0+30, 176, 15, 1+progress_px(17), 15);
      int we = energy_px(32, 8);
      if(we>0) blit(x0+88, y0+53, 185, 30, we, 13);
      switch(getContainer().field(4)) {
        case 0: blit(x0+144, y0+57, 180, 57, 6, 9); break;
        case 1: blit(x0+142, y0+58, 190, 58, 9, 6); break;
        case 2: blit(x0+144, y0+56, 200, 57, 6, 9); break;
        case 3: blit(x0+143, y0+58, 210, 58, 9, 6); break;
        default: break;
      }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      tooltip_.resetTimer();
      ElectricalFurnaceContainer container = (ElectricalFurnaceContainer)getContainer();
      int mx = (int)(mouseX - getGuiLeft() + .5), my = (int)(mouseY - getGuiTop() + .5);
      if((!isPointInRegion(136, 48, 28, 28, mouseX, mouseY))) {
        return super.mouseClicked(mouseX, mouseY, mouseButton);
      } else if(isPointInRegion(144, 64, 6, 10, mouseX, mouseY)) {
        container.onGuiAction("speed", 0);
      } else if(isPointInRegion(134, 58, 10, 6, mouseX, mouseY)) {
        container.onGuiAction("speed", 1);
      } else if(isPointInRegion(144, 48, 6, 10, mouseX, mouseY)) {
        container.onGuiAction("speed", 2);
      } else if(isPointInRegion(150, 58, 10, 6, mouseX, mouseY)) {
        container.onGuiAction("speed", 3);
      }
      return true;
    }

    private int progress_px(int pixels)
    { final int tc=getContainer().field(2), T=getContainer().field(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int heat_px(int pixels)
    {
      int k = ((getContainer().field(0) * (pixels+1)) / (ElectricalFurnaceTileEntity.HEAT_CAPACITY));
      return (k < pixels) ? k : pixels;
    }

    private int energy_px(int maxwidth, int quantization)
    {
      int emax = getContainer().field(5);
      int k = ((maxwidth * getContainer().field(1) * 9) / 8) /((emax>0?emax:1)+1);
      k = (k >= maxwidth-2) ? maxwidth : k;
      if(quantization > 0) k = ((k+(quantization/2))/quantization) * quantization;
      return k;
    }
  }
}
