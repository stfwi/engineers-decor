/*
 * @file Inventories.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General inventory item handling functionality.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class Inventories
{
  public static boolean areItemStacksIdentical(ItemStack a, ItemStack b)
  { return (a.getItem()==b.getItem()) && ItemStack.areItemStackTagsEqual(a, b); }

  public static boolean areItemStacksDifferent(ItemStack a, ItemStack b)
  { return (a.getItem()!=b.getItem()) || (!ItemStack.areItemStackTagsEqual(a, b)); }

  public static IItemHandler itemhandler(World world, BlockPos pos, @Nullable Direction side)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te==null) return null;
    IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    if(ih!=null) return ih;
    if((side!=null) && (te instanceof ISidedInventory)) return new SidedInvWrapper((ISidedInventory)te, side);
    if(te instanceof IInventory) return new InvWrapper((IInventory)te);
    return null;
  }

  public static IItemHandler itemhandler(Entity entity)
  { return (entity instanceof IInventory) ? (new InvWrapper((IInventory)entity)) : null; }

  public static IItemHandler itemhandler(Entity entity, @Nullable Direction side)
  { return (entity instanceof IInventory) ? (new InvWrapper((IInventory)entity)) : null; } // in case a sided entity insertion pops up.

  public static ItemStack insert(IItemHandler handler, ItemStack stack , boolean simulate)
  { return ItemHandlerHelper.insertItemStacked(handler, stack, simulate); }

  public static ItemStack insert(TileEntity te, @Nullable Direction side, ItemStack stack, boolean simulate)
  {
    if(te == null) return stack;
    IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    if(hnd == null) {
      hnd = (IItemHandler)te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    } else if((side!=null) && (te instanceof ISidedInventory)) {
      hnd = new SidedInvWrapper((ISidedInventory)te, side);
    } else if(te instanceof IInventory) {
      hnd = new InvWrapper((IInventory)te);
    }
    return (hnd==null) ? stack : insert(hnd, stack, simulate);
  }

  public static ItemStack insert(World world, BlockPos pos, @Nullable Direction side, ItemStack stack, boolean simulate)
  { return insert(world.getTileEntity(pos), side, stack, simulate); }

  public static ItemStack extract(IItemHandler inventory, @Nullable ItemStack match, int amount, boolean simulate)
  {
    if((inventory==null) || (amount<=0)) return ItemStack.EMPTY;
    final int max = inventory.getSlots();
    ItemStack out_stack = ItemStack.EMPTY;
    for(int i=0; i<max; ++i) {
      final ItemStack stack = inventory.getStackInSlot(i);
      if(stack.isEmpty()) continue;
      if(out_stack.isEmpty()) {
        if((match!=null) && areItemStacksDifferent(stack, match)) continue;
        out_stack = inventory.extractItem(i, amount, simulate);
      } else if(areItemStacksIdentical(stack, out_stack)) {
        ItemStack es = inventory.extractItem(i, (amount-out_stack.getCount()), simulate);
        out_stack.grow(es.getCount());
      }
      if(out_stack.getCount() >= amount) break;
    }
    return out_stack;
  }

  private static ItemStack checked(ItemStack stack)
  { return stack.isEmpty() ? ItemStack.EMPTY : stack; }

  //--------------------------------------------------------------------------------------------------------------------

  public static ItemStack insert(InventoryRange to_ranges[], ItemStack stack)
  {
    ItemStack remaining = stack.copy();
    for(InventoryRange range:to_ranges) {
      remaining = range.insert(remaining, false, 0, false, true);
      if(remaining.isEmpty()) return remaining;
    }
    return remaining;
  }

  public static class InventoryRange implements IInventory, Iterable<ItemStack>
  {
    public final IInventory inventory;
    public final int offset, size, num_rows;

    public InventoryRange(IInventory inventory, int offset, int size, int num_rows)
    {
      this.inventory = inventory;
      this.offset = MathHelper.clamp(offset, 0, inventory.getSizeInventory()-1);
      this.size = MathHelper.clamp(size, 0, inventory.getSizeInventory()-this.offset);
      this.num_rows = num_rows;
    }

    public InventoryRange(IInventory inventory, int offset, int size)
    { this(inventory, offset, size, 1); }

    public static InventoryRange fromPlayerHotbar(PlayerEntity player)
    { return new InventoryRange(player.inventory, 0, 9, 1); }

    public static InventoryRange fromPlayerStorage(PlayerEntity player)
    { return new InventoryRange(player.inventory, 9, 27, 3); }

    public static InventoryRange fromPlayerInventory(PlayerEntity player)
    { return new InventoryRange(player.inventory, 0, 36, 4); }

    // IInventory ------------------------------------------------------------------------------------------------------

    public void clear()
    { inventory.clear(); }

    public int getSizeInventory()
    { return size; }

    public boolean isEmpty()
    { for(int i=0; i<size; ++i) if(!inventory.getStackInSlot(offset+i).isEmpty()){return false;} return true; }

    public ItemStack getStackInSlot(int index)
    { return inventory.getStackInSlot(offset+index); }

    public ItemStack decrStackSize(int index, int count)
    { return inventory.decrStackSize(offset+index, count); }

    public ItemStack removeStackFromSlot(int index)
    { return inventory.removeStackFromSlot(offset+index); }

    public void setInventorySlotContents(int index, ItemStack stack)
    { inventory.setInventorySlotContents(offset+index, stack); }

    public int getInventoryStackLimit()
    { return inventory.getInventoryStackLimit(); }

    public void markDirty()
    { inventory.markDirty(); }

    public boolean isUsableByPlayer(PlayerEntity player)
    { return inventory.isUsableByPlayer(player); }

    public void openInventory(PlayerEntity player)
    { inventory.openInventory(player); }

    public void closeInventory(PlayerEntity player)
    { inventory.closeInventory(player); }

    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return inventory.isItemValidForSlot(offset+index, stack); }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the number of stacks that match the given stack with NBT.
     */
    public int stackMatchCount(final ItemStack ref_stack)
    {
      int n = 0; // ... std::accumulate() the old school way.
      for(int i=0; i<size; ++i) {
        if(areItemStacksIdentical(ref_stack, getStackInSlot(i))) ++n;
      }
      return n;
    }

    public int totalMatchingItemCount(final ItemStack ref_stack)
    {
      int n = 0;
      for(int i=0; i<size; ++i) {
        ItemStack stack = getStackInSlot(i);
        if(areItemStacksIdentical(ref_stack, stack)) n += stack.getCount();
      }
      return n;
    }

    /**
     * Moves as much items from the stack to the slots in range [offset, end_slot] of the inventory,
     * filling up existing stacks first, then (player inventory only) checks appropriate empty slots next
     * to stacks that have that item already, and last uses any empty slot that can be found.
     * Returns the stack that is still remaining in the referenced `stack`.
     */
    public ItemStack insert(final ItemStack input_stack, boolean only_fillup, int limit, boolean reverse, boolean force_group_stacks)
    {
      final ItemStack mvstack = input_stack.copy();
      //final int end_slot = offset + size;
      if(mvstack.isEmpty()) return checked(mvstack);
      int limit_left = (limit>0) ? (Math.min(limit, mvstack.getMaxStackSize())) : (mvstack.getMaxStackSize());
      boolean matches[] = new boolean[size];
      boolean empties[] = new boolean[size];
      int num_matches = 0;
      for(int i=0; i < size; ++i) {
        final int sno = reverse ? (size-1-i) : (i);
        final ItemStack stack = getStackInSlot(sno);
        if(stack.isEmpty()) {
          empties[sno] = true;
        } else if(areItemStacksIdentical(stack, mvstack)) {
          matches[sno] = true;
          ++num_matches;
        }
      }
      // first iteration: fillup existing stacks
      for(int i=0; i<size; ++i) {
        final int sno = reverse ? (size-1-i) : (i);
        if((empties[sno]) || (!matches[sno])) continue;
        final ItemStack stack = getStackInSlot(sno);
        int nmax = Math.min(limit_left, stack.getMaxStackSize() - stack.getCount());
        if(mvstack.getCount() <= nmax) {
          stack.setCount(stack.getCount()+mvstack.getCount());
          setInventorySlotContents(sno, stack);
          return ItemStack.EMPTY;
        } else {
          stack.grow(nmax);
          mvstack.shrink(nmax);
          setInventorySlotContents(sno, stack);
          limit_left -= nmax;
        }
      }
      if(only_fillup) return checked(mvstack);
      if((num_matches>0) && ((force_group_stacks) || (inventory instanceof PlayerInventory))) {
        // second iteration: use appropriate empty slots,
        // a) between
        {
          int insert_start = -1;
          int insert_end = -1;
          int i = 1;
          for(;i<size-1; ++i) {
            final int sno = reverse ? (size-1-i) : (i);
            if(insert_start < 0) {
              if(matches[sno]) insert_start = sno;
            } else if(matches[sno]) {
              insert_end = sno;
            }
          }
          for(i=insert_start;i < insert_end; ++i) {
            final int sno = reverse ? (size-1-i) : (i);
            if((!empties[sno]) || (!isItemValidForSlot(sno, mvstack))) continue;
            int nmax = Math.min(limit_left, mvstack.getCount());
            ItemStack moved = mvstack.copy();
            moved.setCount(nmax);
            mvstack.shrink(nmax);
            setInventorySlotContents(sno, moved);
            return checked(mvstack);
          }
        }
        // b) before/after
        {
          for(int i=1; i<size-1; ++i) {
            final int sno = reverse ? (size-1-i) : (i);
            if(!matches[sno]) continue;
            int ii = (empties[sno-1]) ? (sno-1) : (empties[sno+1] ? (sno+1) : -1);
            if((ii >= 0) && (isItemValidForSlot(ii, mvstack))) {
              int nmax = Math.min(limit_left, mvstack.getCount());
              ItemStack moved = mvstack.copy();
              moved.setCount(nmax);
              mvstack.shrink(nmax);
              setInventorySlotContents(ii, moved);
              return checked(mvstack);
            }
          }
        }
      }
      // third iteration: use any empty slots
      for(int i=0; i<size; ++i) {
        final int sno = reverse ? (size-1-i) : (i);
        if((!empties[sno]) || (!isItemValidForSlot(sno, mvstack))) continue;
        int nmax = Math.min(limit_left, mvstack.getCount());
        ItemStack placed = mvstack.copy();
        placed.setCount(nmax);
        mvstack.shrink(nmax);
        setInventorySlotContents(sno, placed);
        return checked(mvstack);
      }
      return checked(mvstack);
    }

    public ItemStack insert(final ItemStack stack_to_move)
    { return insert(stack_to_move, false, 0, false, true); }

    /**
     * Moves as much items from the slots in range [offset, end_slot] of the inventory into a new stack.
     * Implicitly shrinks the inventory stacks and the `request_stack`.
     */
    public ItemStack extract(final ItemStack request_stack)
    {
      if(request_stack.isEmpty()) return ItemStack.EMPTY;
      List<ItemStack> matches = new ArrayList<>();
      for(int i=0; i<size; ++i) {
        final ItemStack stack = getStackInSlot(i);
        if((!stack.isEmpty()) && (areItemStacksIdentical(stack, request_stack))) {
          if(stack.hasTag()) {
            final CompoundNBT nbt = stack.getTag();
            int n = nbt.size();
            if((n > 0) && (nbt.contains("Damage"))) --n;
            if(n > 0) continue;
          }
          matches.add(stack);
        }
      }
      matches.sort((a,b) -> Integer.compare(a.getCount(), b.getCount()));
      if(matches.isEmpty()) return ItemStack.EMPTY;
      int n_left = request_stack.getCount();
      ItemStack fetched_stack = matches.get(0).split(n_left);
      n_left -= fetched_stack.getCount();
      for(int i=1; (i<matches.size()) && (n_left>0); ++i) {
        ItemStack stack = matches.get(i).split(n_left);
        n_left -= stack.getCount();
        fetched_stack.grow(stack.getCount());
      }
      return checked(fetched_stack);
    }

    /**
     * Moves items from this inventory range to another. Returns true if something was moved
     * (if the inventories should be marked dirty).
     */
    public boolean move(int index, final InventoryRange target_range, boolean all_identical_stacks, boolean only_fillup, boolean reverse, boolean force_group_stacks)
    {
      final ItemStack source_stack = getStackInSlot(index);
      if(source_stack.isEmpty()) return false;
      if(!all_identical_stacks) {
        ItemStack remaining = target_range.insert(source_stack, only_fillup, 0, reverse, force_group_stacks);
        setInventorySlotContents(index, remaining);
        return (remaining.getCount() != source_stack.getCount());
      } else {
        ItemStack remaining = source_stack.copy();
        setInventorySlotContents(index, ItemStack.EMPTY);
        final ItemStack ref_stack = remaining.copy();
        ref_stack.setCount(ref_stack.getMaxStackSize());
        for(int i=size; (i>0) && (!remaining.isEmpty()); --i) {
          remaining = target_range.insert(remaining, only_fillup, 0, reverse, force_group_stacks);
          if(!remaining.isEmpty()) break;
          remaining = this.extract(ref_stack);
        }
        if(!remaining.isEmpty()) {
          setInventorySlotContents(index, remaining); // put back
        }
        return (remaining.getCount() != source_stack.getCount());
      }
    }

    public boolean move(int index, final InventoryRange target_range)
    { return move(index, target_range, false, false, false, true); }

    /**
     * Moves/clears the complete range to another range if possible. Returns true if something was moved
     * (if the inventories should be marked dirty).
     */
    public boolean move(final InventoryRange target_range, boolean only_fillup, boolean reverse, boolean force_group_stacks)
    {
      boolean changed = false;
      for(int i=0; i<size; ++i) changed |= move(i, target_range, false, only_fillup, reverse, force_group_stacks);
      return changed;
    }

    public boolean move(final InventoryRange target_range)
    { return move(target_range, false, false, true); }

    //------------------------------------------------------------------------------------------------------------------

    public Stream<ItemStack> stream()
    { return java.util.stream.StreamSupport.stream(this.spliterator(), false); }

    public Iterator<ItemStack> iterator()
    { return new InventoryRangeIterator(this); }

    public static class InventoryRangeIterator implements Iterator<ItemStack>
    {
      private final InventoryRange parent_;
      private int index = 0;

      public InventoryRangeIterator(InventoryRange range)
      { parent_ = range; }

      public boolean hasNext()
      { return index < parent_.size; }

      public ItemStack next()
      {
        if(index >= parent_.size) throw new NoSuchElementException();
        return parent_.getStackInSlot(index++);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class StorageInventory implements IInventory, Iterable<ItemStack>
  {
    protected final NonNullList<ItemStack> stacks_;
    protected final TileEntity te_;
    protected final int size_;
    protected final int num_rows_;
    protected int stack_limit_ = 64;
    protected BiPredicate<Integer, ItemStack> validator_ = (index, stack)->true;
    protected Consumer<PlayerEntity> open_action_ = (player)->{};
    protected Consumer<PlayerEntity> close_action_ = (player)->{};

    public StorageInventory(TileEntity te, int size, int num_rows)
    {
      te_ = te;
      size_ = Math.max(size, 1);
      stacks_ = NonNullList.<ItemStack>withSize(size_, ItemStack.EMPTY);
      num_rows_ = MathHelper.clamp(num_rows, 1, size_);
    }

    public CompoundNBT save(CompoundNBT nbt)
    { return ItemStackHelper.saveAllItems(nbt, stacks_); }

    public CompoundNBT save(CompoundNBT nbt, boolean save_empty)
    { return ItemStackHelper.saveAllItems(nbt, stacks_, save_empty); }

    public StorageInventory load(CompoundNBT nbt)
    {
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < size_) stacks_.add(ItemStack.EMPTY);
      return this;
    }

    public NonNullList<ItemStack> stacks()
    { return stacks_; }

    public StorageInventory setOpenAction(Consumer<PlayerEntity> fn)
    { open_action_ = fn; return this; }

    public StorageInventory setCloseAction(Consumer<PlayerEntity> fn)
    { close_action_ = fn; return this; }

    public StorageInventory setStackLimit(int max_slot_stack_size)
    { stack_limit_ = Math.max(max_slot_stack_size, 1); return this; }

    // Iterable<ItemStack> ---------------------------------------------------------------------

    public Iterator<ItemStack> iterator()
    { return stacks_.iterator(); }

    public Stream<ItemStack> stream()
    { return stacks_.stream(); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public int getSizeInventory()
    { return size_; }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return (index < size_) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks_, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks_, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    { stacks_.set(index, stack); }

    @Override
    public int getInventoryStackLimit()
    { return stack_limit_; }

    @Override
    public void markDirty()
    { te_.markDirty(); }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player)
    { return ((te_.getWorld().getTileEntity(te_.getPos()) == te_)) && (te_.getPos().distanceSq(player.getPosition()) < 64); }

    @Override
    public void openInventory(PlayerEntity player)
    { open_action_.accept(player); }

    @Override
    public void closeInventory(PlayerEntity player)
    { markDirty(); close_action_.accept(player); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return validator_.test(index, stack); }

    @Override
    public void clear()
    { stacks_.clear(); }

  }

  //--------------------------------------------------------------------------------------------------------------------

  public static void give(PlayerEntity entity, ItemStack stack)
  { ItemHandlerHelper.giveItemToPlayer(entity, stack); }

  public static void setItemInPlayerHand(PlayerEntity player, Hand hand, ItemStack stack) {
    if(stack.isEmpty()) stack = ItemStack.EMPTY;
    if(hand == Hand.MAIN_HAND) {
      player.inventory.mainInventory.set(player.inventory.currentItem, stack);
    } else {
      player.inventory.offHandInventory.set(0, stack);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static NonNullList<ItemStack> readNbtStacks(CompoundNBT nbt, String key, int size)
  {
    NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    if((nbt == null) || (!nbt.contains(key,10))) return stacks;
    CompoundNBT stacknbt = nbt.getCompound(key);
    ItemStackHelper.loadAllItems(stacknbt, stacks);
    return stacks;
  }

  public static CompoundNBT writeNbtStacks(CompoundNBT nbt, String key, NonNullList<ItemStack> stacks, boolean omit_trailing_empty)
  {
    CompoundNBT stacknbt = new CompoundNBT();
    if(omit_trailing_empty) {
      for(int i=stacks.size()-1; i>=0; --i) {
        if(!stacks.get(i).isEmpty()) break;
        stacks.remove(i);
      }
    }
    ItemStackHelper.saveAllItems(stacknbt, stacks);
    if(nbt == null) nbt = new CompoundNBT();
    nbt.put(key, stacknbt);
    return nbt;
  }

  public static CompoundNBT writeNbtStacks(CompoundNBT nbt, String key, NonNullList<ItemStack> stacks)
  { return writeNbtStacks(nbt, key, stacks, false); }

}
