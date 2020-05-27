/*
 * @file Inventories.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General inventory item handling functionality.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


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

  public static ItemStack insert(IItemHandler inventory, ItemStack stack , boolean simulate)
  { return ItemHandlerHelper.insertItemStacked(inventory, stack, simulate); }

  public static ItemStack insert(TileEntity te, @Nullable Direction side, ItemStack stack, boolean simulate)
  {
    if(te==null) return stack;
    IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    if(hnd != null) {
      hnd = (IItemHandler)te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    } else if((side!=null) &&  (te instanceof ISidedInventory)) {
      hnd = new SidedInvWrapper((ISidedInventory)te, side);
    } else if(te instanceof IInventory) {
      hnd = new InvWrapper((IInventory)te);
    }
    return (hnd==null) ? stack : ItemHandlerHelper.insertItemStacked(hnd, stack, simulate);
  }

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

  public static class SlotRange
  {
    public final IInventory inventory;
    public final int start_slot, end_slot;

    public SlotRange(IInventory inv, int start, int end)
    { inventory=inv; start_slot=start; end_slot=end; }

    /**
     * Returns the number of stacks that match the given stack with NBT.
     */
    public int stackMatchCount(final ItemStack ref_stack)
    {
      int n = 0; // ... std::accumulate() the old school way.
      for(int i = start_slot; i < end_slot; ++i) {
        if(areItemStacksIdentical(ref_stack, inventory.getStackInSlot(i))) ++n;
      }
      return n;
    }

    public int totalMatchingItemCount(final ItemStack ref_stack)
    {
      int n = 0;
      for(int i = start_slot; i < end_slot; ++i) {
        ItemStack stack = inventory.getStackInSlot(i);
        if(areItemStacksIdentical(ref_stack, stack)) n += stack.getCount();
      }
      return n;
    }

    /**
     * Moves as much items from the stack to the slots in range [start_slot, end_slot] of the inventory,
     * filling up existing stacks first, then (player inventory only) checks appropriate empty slots next
     * to stacks that have that item already, and last uses any empty slot that can be found.
     * Returns the stack that is still remaining in the referenced `stack`.
     */
    public ItemStack insert(final ItemStack stack_to_move, boolean only_fillup, int limit)
    { return insert(stack_to_move, only_fillup, limit, false, false); }

    public ItemStack insert(final ItemStack stack_to_move, boolean only_fillup, int limit, boolean reverse, boolean force_group_stacks)
    {
      final ItemStack mvstack = stack_to_move.copy();
      if((mvstack.isEmpty()) || (start_slot < 0) || (end_slot > inventory.getSizeInventory())) return checked(mvstack);
      int limit_left = (limit>0) ? (Math.min(limit, mvstack.getMaxStackSize())) : (mvstack.getMaxStackSize());
      boolean matches[] = new boolean[end_slot];
      boolean empties[] = new boolean[end_slot];
      int num_matches = 0;
      for(int i = start_slot; i < end_slot; ++i) {
        final int sno = reverse ? (end_slot-1-i) : (i);
        final ItemStack stack = inventory.getStackInSlot(sno);
        if(stack.isEmpty()) {
          empties[sno] = true;
        } else if(areItemStacksIdentical(stack, mvstack)) {
          matches[sno] = true;
          ++num_matches;
        }
      }
      // first iteration: fillup existing stacks
      for(int i = start_slot; i < end_slot; ++i) {
        final int sno = reverse ? (end_slot-1-i) : (i);
        if((empties[sno]) || (!matches[sno])) continue;
        final ItemStack stack = inventory.getStackInSlot(sno);
        int nmax = Math.min(limit_left, stack.getMaxStackSize() - stack.getCount());
        if(mvstack.getCount() <= nmax) {
          stack.setCount(stack.getCount()+mvstack.getCount());
          inventory.setInventorySlotContents(sno, stack);
          return ItemStack.EMPTY;
        } else {
          stack.grow(nmax);
          mvstack.shrink(nmax);
          inventory.setInventorySlotContents(sno, stack);
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
          int i = start_slot+1;
          for(;i < end_slot-1; ++i) {
            final int sno = reverse ? (end_slot-1-i) : (i);
            if(insert_start < 0) {
              if(matches[sno]) insert_start = sno;
            } else if(matches[sno]) {
              insert_end = sno;
            }
          }
          for(i=insert_start;i < insert_end; ++i) {
            final int sno = reverse ? (end_slot-1-i) : (i);
            if((!empties[sno]) || (!inventory.isItemValidForSlot(sno, mvstack))) continue;
            int nmax = Math.min(limit_left, mvstack.getCount());
            ItemStack moved = mvstack.copy();
            moved.setCount(nmax);
            mvstack.shrink(nmax);
            inventory.setInventorySlotContents(sno, moved);
            return checked(mvstack);
          }
        }
        // b) before/after
        {
          for(int i = start_slot+1; i < end_slot-1; ++i) {
            final int sno = reverse ? (end_slot-1-i) : (i);
            if(!matches[sno]) continue;
            int ii = (empties[sno-1]) ? (sno-1) : (empties[sno+1] ? (sno+1) : -1);
            if((ii >= 0) && (inventory.isItemValidForSlot(ii, mvstack))) {
              int nmax = Math.min(limit_left, mvstack.getCount());
              ItemStack moved = mvstack.copy();
              moved.setCount(nmax);
              mvstack.shrink(nmax);
              inventory.setInventorySlotContents(ii, moved);
              return checked(mvstack);
            }
          }
        }
      }
      // third iteration: use any empty slots
      for(int i = start_slot; i < end_slot; ++i) {
        final int sno = reverse ? (end_slot-1-i) : (i);
        if((!empties[sno]) || (!inventory.isItemValidForSlot(sno, mvstack))) continue;
        int nmax = Math.min(limit_left, mvstack.getCount());
        ItemStack placed = mvstack.copy();
        placed.setCount(nmax);
        mvstack.shrink(nmax);
        inventory.setInventorySlotContents(sno, placed);
        return checked(mvstack);
      }
      return checked(mvstack);
    }

    /**
     * Moves as much items from the slots in range [start_slot, end_slot] of the inventory into a new stack.
     * Implicitly shrinks the inventory stacks and the `request_stack`.
     */
    public ItemStack extract(final ItemStack request_stack)
    {
      if(request_stack.isEmpty()) return ItemStack.EMPTY;
      final IInventory inventory = this.inventory;
      List<ItemStack> matches = new ArrayList<>();
      for(int i = start_slot; i < end_slot; ++i) {
        final ItemStack stack = inventory.getStackInSlot(i);
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
  }

  public static class InventoryRange implements IInventory
  {
    public final IInventory inventory;
    public final int offset, size;

    public InventoryRange(IInventory inventory, int offset, int size)
    { this.inventory = inventory; this.offset = offset; this.size = size; }

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
