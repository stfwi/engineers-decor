/*
 * @file Inventories.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General inventory item handling functionality.
 */
package wile.engineersdecor.libmc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class Inventories
{
  public static boolean areItemStacksIdentical(ItemStack a, ItemStack b)
  { return (a.getItem()==b.getItem()) && ItemStack.tagMatches(a, b); }

  public static boolean areItemStacksDifferent(ItemStack a, ItemStack b)
  { return (a.getItem()!=b.getItem()) || (!ItemStack.tagMatches(a, b)); }

  public static IItemHandler itemhandler(Level world, BlockPos pos, @Nullable Direction side)
  {
    BlockEntity te = world.getBlockEntity(pos);
    if(te==null) return null;
    IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    if(ih!=null) return ih;
    if((side!=null) && (te instanceof WorldlyContainer)) return new SidedInvWrapper((WorldlyContainer)te, side);
    if(te instanceof Container) return new InvWrapper((Container)te);
    return null;
  }

  public static IItemHandler itemhandler(Level world, BlockPos pos, @Nullable Direction side, boolean including_entities)
  {
    IItemHandler ih = itemhandler(world, pos, side);
    if(ih != null) return ih;
    if(!including_entities) return null;
    Entity entity = world.getEntitiesOfClass(Entity.class, new AABB(pos), (e)->(e instanceof Container)).stream().findFirst().orElse(null);
    return (entity==null) ? (null) : (itemhandler(entity,side));
  }

  public static IItemHandler itemhandler(Entity entity)
  { return (entity instanceof Container) ? (new InvWrapper((Container)entity)) : null; }

  public static IItemHandler itemhandler(Player player)
  { return new PlayerMainInvWrapper(player.getInventory()); }

  public static IItemHandler itemhandler(Entity entity, @Nullable Direction side)
  { return (entity instanceof Container) ? (new InvWrapper((Container)entity)) : null; }

  public static boolean insertionPossible(Level world, BlockPos pos, @Nullable Direction side, boolean including_entities)
  { return itemhandler(world, pos, side, including_entities) != null; }

  public static ItemStack insert(IItemHandler handler, ItemStack stack , boolean simulate)
  { return ItemHandlerHelper.insertItemStacked(handler, stack, simulate); }

  public static ItemStack insert(BlockEntity te, @Nullable Direction side, ItemStack stack, boolean simulate)
  {
    if(te == null) return stack;
    IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).orElse(null);
    if(hnd == null) {
      if((side != null) && (te instanceof WorldlyContainer)) {
        hnd = new SidedInvWrapper((WorldlyContainer)te, side);
      } else if(te instanceof Container) {
        hnd = new InvWrapper((Container)te);
      }
    }
    return (hnd==null) ? stack : insert(hnd, stack, simulate);
  }

  public static ItemStack insert(Level world, BlockPos pos, @Nullable Direction side, ItemStack stack, boolean simulate, boolean including_entities)
  { return insert(itemhandler(world, pos, side, including_entities), stack, simulate); }

  public static ItemStack insert(Level world, BlockPos pos, @Nullable Direction side, ItemStack stack, boolean simulate)
  { return insert(world, pos, side, stack, simulate, false); }

  public static ItemStack extract(IItemHandler inventory, @Nullable ItemStack match, int amount, boolean simulate)
  {
    if((inventory==null) || (amount<=0) || ((match!=null) && (match.isEmpty()))) return ItemStack.EMPTY;
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

  public static Container copyOf(Container src)
  {
    final int size = src.getContainerSize();
    SimpleContainer dst = new SimpleContainer(size);
    for(int i=0; i<size; ++i) dst.setItem(i, src.getItem(i).copy());
    return dst;
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static ItemStack insert(InventoryRange[] to_ranges, ItemStack stack)
  {
    ItemStack remaining = stack.copy();
    for(InventoryRange range:to_ranges) {
      remaining = range.insert(remaining, false, 0, false, true);
      if(remaining.isEmpty()) return remaining;
    }
    return remaining;
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class MappedItemHandler implements IItemHandler
  {
    private final BiPredicate<Integer, ItemStack> extraction_predicate_;
    private final BiPredicate<Integer, ItemStack> insertion_predicate_;
    private final BiConsumer<Integer, ItemStack> insertion_notifier_;
    private final BiConsumer<Integer, ItemStack> extraction_notifier_;
    private final List<Integer> slot_map_;
    private final Container inv_;

    public MappedItemHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate, BiConsumer<Integer, ItemStack> insertion_notifier, BiConsumer<Integer, ItemStack> extraction_notifier)
    { inv_ = inv; extraction_predicate_ = extraction_predicate; insertion_predicate_ = insertion_predicate; insertion_notifier_=insertion_notifier; extraction_notifier_=extraction_notifier;  slot_map_ = IntStream.range(0, inv.getContainerSize()).boxed().collect(Collectors.toList()); }

    public MappedItemHandler(Container inv, List<Integer> slot_map, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate, BiConsumer<Integer, ItemStack> insertion_notifier, BiConsumer<Integer, ItemStack> extraction_notifier)
    { inv_ = inv; extraction_predicate_ = extraction_predicate; insertion_predicate_ = insertion_predicate; insertion_notifier_=insertion_notifier; extraction_notifier_=extraction_notifier;  slot_map_ = slot_map; }

    public MappedItemHandler(Container inv, List<Integer> slot_map, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate)
    { this(inv, slot_map, extraction_predicate, insertion_predicate, (i,s)->{}, (i,s)->{}); }

    public MappedItemHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate)
    { this(inv, IntStream.range(0, inv.getContainerSize()).boxed().collect(Collectors.toList()), extraction_predicate, insertion_predicate); }

    public MappedItemHandler(Container inv)
    { this(inv, (i,s)->true, (i,s)->true); }

    @Override
    public int hashCode()
    { return inv_.hashCode(); }

    @Override
    public boolean equals(Object o)
    { return (o==this) || ((o!=null) && (getClass()==o.getClass()) && (inv_.equals(((MappedItemHandler)o).inv_))); }

    // IItemHandler -----------------------------------------------------------------------------------------------

    @Override
    public int getSlots()
    { return slot_map_.size(); }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int slot)
    { return (slot >= slot_map_.size()) ? ItemStack.EMPTY : inv_.getItem(slot_map_.get(slot)); }

    @Override
    public int getSlotLimit(int slot)
    { return inv_.getMaxStackSize(); }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    {
      if(slot >= slot_map_.size()) return false;
      slot = slot_map_.get(slot);
      return insertion_predicate_.test(slot, stack) && inv_.canPlaceItem(slot, stack);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
    {
      if(stack.isEmpty()) return ItemStack.EMPTY;
      if(slot >= slot_map_.size()) return stack;
      slot = slot_map_.get(slot);
      if(!insertion_predicate_.test(slot, stack)) return stack;
      if(!inv_.canPlaceItem(slot, stack)) return stack;
      ItemStack sst = inv_.getItem(slot);
      final int slot_limit = inv_.getMaxStackSize();
      if(!sst.isEmpty()) {
        if(sst.getCount() >= Math.min(sst.getMaxStackSize(), slot_limit)) return stack;
        if(!ItemHandlerHelper.canItemStacksStack(stack, sst)) return stack;
        final int limit = Math.min(stack.getMaxStackSize(), slot_limit) - sst.getCount();
        if(stack.getCount() <= limit) {
          if(!simulate) {
            stack = stack.copy();
            stack.grow(sst.getCount());
            inv_.setItem(slot, stack);
            inv_.setChanged();
            insertion_notifier_.accept(slot, sst);
          }
          return ItemStack.EMPTY;
        } else {
          stack = stack.copy();
          if(simulate) {
            stack.shrink(limit);
          } else {
            final ItemStack diff = stack.split(limit);
            sst.grow(diff.getCount());
            inv_.setItem(slot, sst);
            inv_.setChanged();
            insertion_notifier_.accept(slot, diff);
          }
          return stack;
        }
      } else {
        final int limit = Math.min(slot_limit, stack.getMaxStackSize());
        if(stack.getCount() >= limit) {
          stack = stack.copy();
          final ItemStack ins = stack.split(limit);
          if(!simulate) {
            inv_.setItem(slot, ins);
            inv_.setChanged();
            insertion_notifier_.accept(slot, ins.copy());
          }
          if(stack.isEmpty()) {
            stack = ItemStack.EMPTY;
          }
          return stack;
        } else {
          if(!simulate) {
            inv_.setItem(slot, stack.copy());
            inv_.setChanged();
            insertion_notifier_.accept(slot, stack.copy());
          }
          return ItemStack.EMPTY;
        }
      }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
      if(amount <= 0) return ItemStack.EMPTY;
      if(slot >= slot_map_.size()) return ItemStack.EMPTY;
      slot = slot_map_.get(slot);
      ItemStack stack = inv_.getItem(slot);
      if(!extraction_predicate_.test(slot, stack)) return ItemStack.EMPTY;
      if(simulate) {
        stack = stack.copy();
        if(amount < stack.getCount()) stack.setCount(amount);
      } else {
        stack = inv_.removeItem(slot, Math.min(stack.getCount(), amount));
        inv_.setChanged();
        extraction_notifier_.accept(slot, stack.copy());
      }
      return stack;
    }

    // Factories --------------------------------------------------------------------------------------------

    public static LazyOptional<IItemHandler> createGenericHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate, BiConsumer<Integer, ItemStack> insertion_notifier, BiConsumer<Integer, ItemStack> extraction_notifier)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, extraction_predicate, insertion_predicate, insertion_notifier, extraction_notifier)); }

    public static LazyOptional<IItemHandler> createGenericHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate, BiConsumer<Integer, ItemStack> insertion_notifier, BiConsumer<Integer, ItemStack> extraction_notifier, List<Integer> slot_map)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, slot_map, extraction_predicate, insertion_predicate, insertion_notifier, extraction_notifier)); }

    public static LazyOptional<IItemHandler> createGenericHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate, List<Integer> slot_map)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, slot_map, extraction_predicate, insertion_predicate)); }

    public static LazyOptional<IItemHandler> createGenericHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, BiPredicate<Integer, ItemStack> insertion_predicate)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, extraction_predicate, insertion_predicate)); }

    public static LazyOptional<IItemHandler> createGenericHandler(Container inv)
    { return LazyOptional.of(() -> new MappedItemHandler(inv)); }

    public static LazyOptional<IItemHandler> createExtractionHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate, List<Integer> slot_map)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, slot_map, extraction_predicate, (i, s)->false)); }

    public static LazyOptional<IItemHandler> createExtractionHandler(Container inv, BiPredicate<Integer, ItemStack> extraction_predicate)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, extraction_predicate, (i, s)->false)); }

    public static LazyOptional<IItemHandler> createExtractionHandler(Container inv, Integer... slots)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, Arrays.asList(slots), (i, s)->true, (i, s)->false)); }

    public static LazyOptional<IItemHandler> createExtractionHandler(Container inv)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, (i, s)->true, (i, s)->false)); }

    public static LazyOptional<IItemHandler> createInsertionHandler(Container inv, BiPredicate<Integer, ItemStack> insertion_predicate, List<Integer> slot_map)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, slot_map, (i, s)->false, insertion_predicate)); }

    public static LazyOptional<IItemHandler> createInsertionHandler(Container inv, Integer... slots)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, Arrays.asList(slots), (i, s)->false, (i, s)->true)); }

    public static LazyOptional<IItemHandler> createInsertionHandler(Container inv, BiPredicate<Integer, ItemStack> insertion_predicate)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, (i, s)->false, insertion_predicate)); }

    public static LazyOptional<IItemHandler> createInsertionHandler(Container inv)
    { return LazyOptional.of(() -> new MappedItemHandler(inv, (i, s)->false, (i, s)->true)); }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class InventoryRange implements Container, Iterable<ItemStack>
  {
    protected final Container inventory_;
    protected final int offset_, size_, num_rows;
    protected int max_stack_size_ = 64;
    protected BiPredicate<Integer, ItemStack> validator_ = (index, stack)->true;

    public static InventoryRange fromPlayerHotbar(Player player)
    { return new InventoryRange(player.getInventory(), 0, 9, 1); }

    public static InventoryRange fromPlayerStorage(Player player)
    { return new InventoryRange(player.getInventory(), 9, 27, 3); }

    public static InventoryRange fromPlayerInventory(Player player)
    { return new InventoryRange(player.getInventory(), 0, 36, 4); }

    public InventoryRange(Container inventory, int offset, int size, int num_rows)
    {
      this.inventory_ = inventory;
      this.offset_ = Mth.clamp(offset, 0, inventory.getContainerSize()-1);
      this.size_ = Mth.clamp(size, 0, inventory.getContainerSize()-this.offset_);
      this.num_rows = num_rows;
    }

    public InventoryRange(Container inventory, int offset, int size)
    { this(inventory, offset, size, 1); }

    public InventoryRange(Container inventory)
    { this(inventory, 0, inventory.getContainerSize(), 1); }

    public final Container inventory()
    { return inventory_; }

    public final int size()
    { return size_; }

    public final int offset()
    { return offset_; }

    public final ItemStack get(int index)
    { return inventory_.getItem(offset_+index); }

    public final void set(int index, ItemStack stack)
    { inventory_.setItem(offset_+index, stack); }

    public final InventoryRange setValidator(BiPredicate<Integer, ItemStack> validator)
    { validator_ = validator; return this; }

    public final BiPredicate<Integer, ItemStack> getValidator()
    { return validator_; }

    public final InventoryRange setMaxStackSize(int count)
    { max_stack_size_ = Math.max(count, 1) ; return this; }

    // Container ------------------------------------------------------------------------------------------------------

    @Override
    public void clearContent()
    { for(int i=0; i<size_; ++i) setItem(i, ItemStack.EMPTY); }

    @Override
    public int getContainerSize()
    { return size_; }

    @Override
    public boolean isEmpty()
    { for(int i=0; i<size_; ++i) if(!inventory_.getItem(offset_+i).isEmpty()){return false;} return true; }

    @Override
    public ItemStack getItem(int index)
    { return inventory_.getItem(offset_+index); }

    @Override
    public ItemStack removeItem(int index, int count)
    { return inventory_.removeItem(offset_+index, count); }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    { return inventory_.removeItemNoUpdate(offset_+index); }

    @Override
    public void setItem(int index, ItemStack stack)
    { inventory_.setItem(offset_+index, stack); }

    @Override
    public int getMaxStackSize()
    { return Math.min(max_stack_size_, inventory_.getMaxStackSize()); }

    @Override
    public void setChanged()
    { inventory_.setChanged(); }

    @Override
    public boolean stillValid(Player player)
    { return inventory_.stillValid(player); }

    @Override
    public void startOpen(Player player)
    { inventory_.startOpen(player); }

    @Override
    public void stopOpen(Player player)
    { inventory_.stopOpen(player); }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    { return validator_.test(offset_+index, stack) && inventory_.canPlaceItem(offset_+index, stack); }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Iterates using a function (slot, stack) -> bool until the function matches (returns true).
     */
    public boolean iterate(BiPredicate<Integer,ItemStack> fn)
    { for(int i=0; i<size_; ++i) { if(fn.test(i, getItem(i))) { return true; } } return false; }

    public boolean contains(ItemStack stack)
    { for(int i=0; i<size_; ++i) { if(areItemStacksIdentical(stack, getItem(i))) { return true; } } return false; }

    public int indexOf(ItemStack stack)
    { for(int i=0; i<size_; ++i) { if(areItemStacksIdentical(stack, getItem(i))) { return i; } } return -1; }

    public <T> Optional<T> find(BiFunction<Integer,ItemStack, Optional<T>> fn)
    {
      for(int i=0; i<size_; ++i) {
        Optional<T> r = fn.apply(i,getItem(i));
        if(r.isPresent()) return r;
      }
      return Optional.empty();
    }

    public <T> List<T> collect(BiFunction<Integer,ItemStack, Optional<T>> fn)
    {
      List<T> data = new ArrayList<>();
      for(int i=0; i<size_; ++i) {
        fn.apply(i, getItem(i)).ifPresent(data::add);
      }
      return data;
    }

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
      { return index < parent_.size_; }

      public ItemStack next()
      {
        if(index >= parent_.size_) throw new NoSuchElementException();
        return parent_.getItem(index++);
      }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the number of stacks that match the given stack with NBT.
     */
    public int stackMatchCount(final ItemStack ref_stack)
    {
      int n = 0; // ... std::accumulate() the old school way.
      for(int i=0; i<size_; ++i) {
        if(areItemStacksIdentical(ref_stack, getItem(i))) ++n;
      }
      return n;
    }

    public int totalMatchingItemCount(final ItemStack ref_stack)
    {
      int n = 0;
      for(int i=0; i<size_; ++i) {
        ItemStack stack = getItem(i);
        if(areItemStacksIdentical(ref_stack, stack)) n += stack.getCount();
      }
      return n;
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Moves as much items from the stack to the slots in range [offset_, end_slot] of the inventory_,
     * filling up existing stacks first, then (player inventory_ only) checks appropriate empty slots next
     * to stacks that have that item already, and last uses any empty slot that can be found.
     * Returns the stack that is still remaining in the referenced `stack`.
     */
    public ItemStack insert(final ItemStack input_stack, boolean only_fillup, int limit, boolean reverse, boolean force_group_stacks)
    {
      final ItemStack mvstack = input_stack.copy();
      //final int end_slot = offset_ + size;
      if(mvstack.isEmpty()) return checked(mvstack);
      int limit_left = (limit>0) ? (Math.min(limit, mvstack.getMaxStackSize())) : (mvstack.getMaxStackSize());
      boolean[] matches = new boolean[size_];
      boolean[] empties = new boolean[size_];
      int num_matches = 0;
      for(int i=0; i < size_; ++i) {
        final int sno = reverse ? (size_-1-i) : (i);
        final ItemStack stack = getItem(sno);
        if(stack.isEmpty()) {
          empties[sno] = true;
        } else if(areItemStacksIdentical(stack, mvstack)) {
          matches[sno] = true;
          ++num_matches;
        }
      }
      // first iteration: fillup existing stacks
      for(int i=0; i<size_; ++i) {
        final int sno = reverse ? (size_-1-i) : (i);
        if((empties[sno]) || (!matches[sno])) continue;
        final ItemStack stack = getItem(sno);
        int nmax = Math.min(limit_left, stack.getMaxStackSize() - stack.getCount());
        if(mvstack.getCount() <= nmax) {
          stack.setCount(stack.getCount()+mvstack.getCount());
          setItem(sno, stack);
          return ItemStack.EMPTY;
        } else {
          stack.grow(nmax);
          mvstack.shrink(nmax);
          setItem(sno, stack);
          limit_left -= nmax;
        }
      }
      if(only_fillup) return checked(mvstack);
      if((num_matches>0) && ((force_group_stacks) || (inventory_ instanceof Inventory))) {
        // second iteration: use appropriate empty slots,
        // a) between
        {
          int insert_start = -1;
          int insert_end = -1;
          int i = 1;
          for(;i<size_-1; ++i) {
            final int sno = reverse ? (size_-1-i) : (i);
            if(insert_start < 0) {
              if(matches[sno]) insert_start = sno;
            } else if(matches[sno]) {
              insert_end = sno;
            }
          }
          for(i=insert_start;i < insert_end; ++i) {
            final int sno = reverse ? (size_-1-i) : (i);
            if((!empties[sno]) || (!canPlaceItem(sno, mvstack))) continue;
            int nmax = Math.min(limit_left, mvstack.getCount());
            ItemStack moved = mvstack.copy();
            moved.setCount(nmax);
            mvstack.shrink(nmax);
            setItem(sno, moved);
            return checked(mvstack);
          }
        }
        // b) before/after
        {
          for(int i=1; i<size_-1; ++i) {
            final int sno = reverse ? (size_-1-i) : (i);
            if(!matches[sno]) continue;
            int ii = (empties[sno-1]) ? (sno-1) : (empties[sno+1] ? (sno+1) : -1);
            if((ii >= 0) && (canPlaceItem(ii, mvstack))) {
              int nmax = Math.min(limit_left, mvstack.getCount());
              ItemStack moved = mvstack.copy();
              moved.setCount(nmax);
              mvstack.shrink(nmax);
              setItem(ii, moved);
              return checked(mvstack);
            }
          }
        }
      }
      // third iteration: use any empty slots
      for(int i=0; i<size_; ++i) {
        final int sno = reverse ? (size_-1-i) : (i);
        if((!empties[sno]) || (!canPlaceItem(sno, mvstack))) continue;
        int nmax = Math.min(limit_left, mvstack.getCount());
        ItemStack placed = mvstack.copy();
        placed.setCount(nmax);
        mvstack.shrink(nmax);
        setItem(sno, placed);
        return checked(mvstack);
      }
      return checked(mvstack);
    }

    public ItemStack insert(final ItemStack stack_to_move)
    { return insert(stack_to_move, false, 0, false, true); }

    public ItemStack insert(final int index, final ItemStack stack_to_move)
    {
      if(stack_to_move.isEmpty()) return stack_to_move;
      final ItemStack stack = getItem(index);
      final int limit = Math.min(getMaxStackSize(), stack.getMaxStackSize());
      if(stack.isEmpty()) {
        setItem(index, stack_to_move.copy());
        return ItemStack.EMPTY;
      } else if((stack.getCount() >= limit) || !areItemStacksIdentical(stack, stack_to_move)) {
        return stack_to_move;
      } else {
        final int amount = Math.min(limit-stack.getCount(), stack_to_move.getCount());
        ItemStack remaining = stack_to_move.copy();
        remaining.shrink(amount);
        stack.grow(amount);
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
      }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Extracts maximum amount of items from the inventory_.
     * The first non-empty stack defines the item.
     */
    public ItemStack extract(int amount)
    { return extract(amount, false); }

    public ItemStack extract(int amount, boolean random)
    {
      ItemStack out_stack = ItemStack.EMPTY;
      int offset = random ? (int)(Math.random()*size_) : 0;
      for(int k=0; k<size_; ++k) {
        int i = (offset+k) % size_;
        final ItemStack stack = getItem(i);
        if(stack.isEmpty()) continue;
        if(out_stack.isEmpty()) {
          if(stack.getCount() < amount) {
            out_stack = stack;
            setItem(i, ItemStack.EMPTY);
            if(!out_stack.isStackable()) break;
            amount -= out_stack.getCount();
          } else {
            out_stack = stack.split(amount);
            break;
          }
        } else if(areItemStacksIdentical(stack, out_stack)) {
          if(stack.getCount() <= amount) {
            out_stack.grow(stack.getCount());
            amount -= stack.getCount();
            setItem(i, ItemStack.EMPTY);
          } else {
            out_stack.grow(amount);
            stack.shrink(amount);
            if(stack.isEmpty()) setItem(i, ItemStack.EMPTY);
            break;
          }
        }
      }
      if(!out_stack.isEmpty()) setChanged();
      return out_stack;
    }

    /**
     * Moves as much items from the slots in range [offset_, end_slot] of the inventory_ into a new stack.
     * Implicitly shrinks the inventory_ stacks and the `request_stack`.
     */
    public ItemStack extract(final ItemStack request_stack)
    {
      if(request_stack.isEmpty()) return ItemStack.EMPTY;
      List<ItemStack> matches = new ArrayList<>();
      for(int i=0; i<size_; ++i) {
        final ItemStack stack = getItem(i);
        if((!stack.isEmpty()) && (areItemStacksIdentical(stack, request_stack))) {
          if(stack.hasTag()) {
            final CompoundTag nbt = stack.getOrCreateTag();
            int n = nbt.size();
            if((n > 0) && (nbt.contains("Damage"))) --n;
            if(n > 0) continue;
          }
          matches.add(stack);
        }
      }
      matches.sort(Comparator.comparingInt(ItemStack::getCount));
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

    //------------------------------------------------------------------------------------------------------------------

    /**
     * Moves items from this inventory_ range to another. Returns true if something was moved
     * (if the inventories should be marked dirty).
     */
    public boolean move(int index, final InventoryRange target_range, boolean all_identical_stacks, boolean only_fillup, boolean reverse, boolean force_group_stacks)
    {
      final ItemStack source_stack = getItem(index);
      if(source_stack.isEmpty()) return false;
      if(!all_identical_stacks) {
        ItemStack remaining = target_range.insert(source_stack, only_fillup, 0, reverse, force_group_stacks);
        setItem(index, remaining);
        return (remaining.getCount() != source_stack.getCount());
      } else {
        ItemStack remaining = source_stack.copy();
        setItem(index, ItemStack.EMPTY);
        final ItemStack ref_stack = remaining.copy();
        ref_stack.setCount(ref_stack.getMaxStackSize());
        for(int i=size_; (i>0) && (!remaining.isEmpty()); --i) {
          remaining = target_range.insert(remaining, only_fillup, 0, reverse, force_group_stacks);
          if(!remaining.isEmpty()) break;
          remaining = this.extract(ref_stack);
        }
        if(!remaining.isEmpty()) {
          setItem(index, remaining); // put back
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
      for(int i=0; i<size_; ++i) changed |= move(i, target_range, false, only_fillup, reverse, force_group_stacks);
      return changed;
    }

    public boolean move(final InventoryRange target_range, boolean only_fillup)
    { return move(target_range, only_fillup, false, true); }

    public boolean move(final InventoryRange target_range)
    { return move(target_range, false, false, true); }

  }

  //--------------------------------------------------------------------------------------------------------------------

  public static class StorageInventory implements Container, Iterable<ItemStack>
  {
    protected final NonNullList<ItemStack> stacks_;
    protected final BlockEntity te_;
    protected final int size_;
    protected final int num_rows_;
    protected int stack_limit_ = 64;
    protected BiPredicate<Integer, ItemStack> validator_ = (index, stack)->true;
    protected Consumer<Player> open_action_ = (player)->{};
    protected Consumer<Player> close_action_ = (player)->{};
    protected BiConsumer<Integer,ItemStack> slot_set_action_ = (index, stack)->{};

    public StorageInventory(BlockEntity te, int size)
    { this(te, size, 1); }

    public StorageInventory(BlockEntity te, int size, int num_rows)
    {
      te_ = te;
      size_ = Math.max(size, 1);
      stacks_ = NonNullList.withSize(size_, ItemStack.EMPTY);
      num_rows_ = Mth.clamp(num_rows, 1, size_);
    }
    public CompoundTag save(CompoundTag nbt, String key)
    { nbt.put(key, save(new CompoundTag(), false)); return nbt; }

    public CompoundTag save(CompoundTag nbt)
    { return ContainerHelper.saveAllItems(nbt, stacks_); }

    public CompoundTag save(CompoundTag nbt, boolean save_empty)
    { return ContainerHelper.saveAllItems(nbt, stacks_, save_empty); }

    public CompoundTag save(boolean save_empty)
    { return save(new CompoundTag(), save_empty); }

    public StorageInventory load(CompoundTag nbt, String key)
    {
      if(!nbt.contains("key", Tag.TAG_COMPOUND)) {
        stacks_.clear();
        return this;
      } else {
        return load(nbt.getCompound(key));
      }
    }

    public StorageInventory load(CompoundTag nbt)
    { stacks_.clear(); ContainerHelper.loadAllItems(nbt, stacks_); return this; }

    public List<ItemStack> stacks()
    { return stacks_; }

    public BlockEntity getBlockEntity()
    { return te_; }

    public StorageInventory setOpenAction(Consumer<Player> fn)
    { open_action_ = fn; return this; }

    public StorageInventory setCloseAction(Consumer<Player> fn)
    { close_action_ = fn; return this; }

    public StorageInventory setSlotChangeAction(BiConsumer<Integer,ItemStack> fn)
    { slot_set_action_ = fn; return this; }

    public StorageInventory setStackLimit(int max_slot_stack_size)
    { stack_limit_ = Math.max(max_slot_stack_size, 1); return this; }

    public StorageInventory setValidator(BiPredicate<Integer, ItemStack> validator)
    { validator_ = validator; return this; }

    public BiPredicate<Integer, ItemStack> getValidator()
    { return validator_; }

    // Iterable<ItemStack> ---------------------------------------------------------------------

    public Iterator<ItemStack> iterator()
    { return stacks_.iterator(); }

    public Stream<ItemStack> stream()
    { return stacks_.stream(); }

    // Container ------------------------------------------------------------------------------

    @Override
    public int getContainerSize()
    { return size_; }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getItem(int index)
    { return (index < size_) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int index, int count)
    { return ContainerHelper.removeItem(stacks_, index, count); }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    { return ContainerHelper.takeItem(stacks_, index); }

    @Override
    public void setItem(int index, ItemStack stack)
    {
      stacks_.set(index, stack);
      if((stack.getCount() != stacks_.get(index).getCount()) || !areItemStacksDifferent(stacks_.get(index),stack)) {
        slot_set_action_.accept(index, stack);
      }
    }

    @Override
    public int getMaxStackSize()
    { return stack_limit_; }

    @Override
    public void setChanged()
    { te_.setChanged(); }

    @Override
    public boolean stillValid(Player player)
    { return ((te_.getLevel().getBlockEntity(te_.getBlockPos()) == te_)) && (te_.getBlockPos().distSqr(player.blockPosition()) < 64); }

    @Override
    public void startOpen(Player player)
    { open_action_.accept(player); }

    @Override
    public void stopOpen(Player player)
    { setChanged(); close_action_.accept(player); }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    { return validator_.test(index, stack); }

    @Override
    public void clearContent()
    { stacks_.clear(); setChanged(); }

  }

  //--------------------------------------------------------------------------------------------------------------------

  public static void give(Player entity, ItemStack stack)
  { ItemHandlerHelper.giveItemToPlayer(entity, stack); }

  public static void setItemInPlayerHand(Player player, InteractionHand hand, ItemStack stack)
  {
    if(stack.isEmpty()) stack = ItemStack.EMPTY;
    if(hand == InteractionHand.MAIN_HAND) {
      player.getInventory().items.set(player.getInventory().selected, stack);
    } else {
      player.getInventory().offhand.set(0, stack);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------

  public static Container readNbtStacks(CompoundTag nbt, String key, Container target)
  {
    NonNullList<ItemStack> stacks = Inventories.readNbtStacks(nbt, key, target.getContainerSize());
    for(int i=0; i<stacks.size(); ++i) target.setItem(i, stacks.get(i));
    return target;
  }

  public static NonNullList<ItemStack> readNbtStacks(CompoundTag nbt, String key, int size)
  {
    NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    if((nbt == null) || (!nbt.contains(key, Tag.TAG_COMPOUND))) return stacks;
    CompoundTag stacknbt = nbt.getCompound(key);
    ContainerHelper.loadAllItems(stacknbt, stacks);
    return stacks;
  }

  public static NonNullList<ItemStack> readNbtStacks(CompoundTag nbt, int size)
  {
    NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    if((nbt == null) || (!nbt.contains("Items", Tag.TAG_LIST))) return stacks;
    ContainerHelper.loadAllItems(nbt, stacks);
    return stacks;
  }

  public static CompoundTag writeNbtStacks(CompoundTag nbt, String key, NonNullList<ItemStack> stacks, boolean omit_trailing_empty)
  {
    CompoundTag stacknbt = new CompoundTag();
    if(omit_trailing_empty) {
      for(int i=stacks.size()-1; i>=0; --i) {
        if(!stacks.get(i).isEmpty()) break;
        stacks.remove(i);
      }
    }
    ContainerHelper.saveAllItems(stacknbt, stacks);
    if(nbt == null) nbt = new CompoundTag();
    nbt.put(key, stacknbt);
    return nbt;
  }

  public static CompoundTag writeNbtStacks(CompoundTag nbt, String key, NonNullList<ItemStack> stacks)
  { return writeNbtStacks(nbt, key, stacks, false); }

  //--------------------------------------------------------------------------------------------------------------------

  public static void dropStack(Level world, Vec3 pos, ItemStack stack, Vec3 velocity, double position_noise, double velocity_noise)
  {
    if(stack.isEmpty()) return;
    if(position_noise > 0) {
      position_noise = Math.min(position_noise, 0.8);
      pos = pos.add(
        position_noise * (world.getRandom().nextDouble()-.5),
        position_noise * (world.getRandom().nextDouble()-.5),
        position_noise * (world.getRandom().nextDouble()-.5)
      );
    }
    if(velocity_noise > 0) {
      velocity_noise = Math.min(velocity_noise, 1.0);
      velocity = velocity.add(
        (velocity_noise) * (world.getRandom().nextDouble()-.5),
        (velocity_noise) * (world.getRandom().nextDouble()-.5),
        (velocity_noise) * (world.getRandom().nextDouble()-.5)
      );
    }
    ItemEntity e = new ItemEntity(world, pos.x, pos.y, pos.z, stack);
    e.setDeltaMovement((float)velocity.x, (float)velocity.y, (float)velocity.z);
    e.setDefaultPickUpDelay();
    world.addFreshEntity(e);
  }

  public static void dropStack(Level world, Vec3 pos, ItemStack stack, Vec3 velocity)
  { dropStack(world, pos, stack, velocity, 0.3, 0.2); }

  public static void dropStack(Level world, Vec3 pos, ItemStack stack)
  { dropStack(world, pos, stack, Vec3.ZERO, 0.3, 0.2); }

}
