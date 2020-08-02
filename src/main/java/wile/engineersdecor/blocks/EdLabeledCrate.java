/*
 * @file EdLabeledCrate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Storage crate with a content hint.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories.InventoryRange;
import wile.engineersdecor.libmc.detail.Inventories.SlotRange;
import wile.engineersdecor.libmc.detail.Networking;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.block.material.PushReaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import com.mojang.blaze3d.platform.GlStateManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class EdLabeledCrate
{
  private static boolean with_gui_mouse_handling = true;
  private static final HashSet<Item> unstorable_containers = new HashSet<Item>();

  public static void on_config(boolean without_gui_mouse_handling)
  {
    with_gui_mouse_handling = !without_gui_mouse_handling;
    // Currently no config, using a tag for this small feature may be uselessly stressing the registry.
    unstorable_containers.clear();
    unstorable_containers.add(ModContent.LABELED_CRATE.asItem());
    unstorable_containers.add(Items.SHULKER_BOX);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateBlock extends StandardBlocks.Horizontal implements IDecorBlock
  {
    public LabeledCrateBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos)
    { return Container.calcRedstone(world.getTileEntity(pos)); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new LabeledCrateTileEntity(); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isRemote) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundNBT te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof LabeledCrateTileEntity)) return;
      ((LabeledCrateTileEntity)te).readnbt(te_nbt);
      ((LabeledCrateTileEntity)te).markDirty();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, BlockPos pos, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isRemote) return stacks;
      if(!(te instanceof LabeledCrateTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT te_nbt = ((LabeledCrateTileEntity) te).reset_getnbt();
        if(!te_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((LabeledCrateTileEntity)te).stacks_) stacks.add(stack);
        ((LabeledCrateTileEntity)te).reset_getnbt();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return ActionResultType.SUCCESS;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof LabeledCrateTileEntity)) return ActionResultType.SUCCESS;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.SUCCESS;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      return ActionResultType.SUCCESS;
    }

    @Override
    public PushReaction getPushReaction(BlockState state)
    { return PushReaction.BLOCK; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(final ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
      if(!Auxiliaries.Tooltip.extendedTipCondition() || Auxiliaries.Tooltip.helpCondition()) {
        super.addInformation(stack, world, tooltip, flag);
        return;
      }
      NonNullList<ItemStack> items = NonNullList.withSize(LabeledCrateTileEntity.NUM_OF_SLOTS, ItemStack.EMPTY);
      int num_used_slots = 0;
      int total_items = 0;
      if(stack.hasTag() && stack.getTag().contains("tedata")) {
        final CompoundNBT nbt = stack.getTag().getCompound("tedata");
        if(nbt.contains("Items")) {
          ItemStackHelper.loadAllItems(nbt, items);
          for(int i=0; i<LabeledCrateTileEntity.ITEMFRAME_SLOTNO; ++i) {
            final ItemStack st = items.get(i);
            if(st.isEmpty()) continue;
            ++num_used_slots;
            total_items += st.getCount();
          }
        }
      }
      int num_free_slots = LabeledCrateTileEntity.ITEMFRAME_SLOTNO - num_used_slots;
      ItemStack frameStack = items.get(LabeledCrateTileEntity.ITEMFRAME_SLOTNO);
      tooltip.add(Auxiliaries.localizable(getTranslationKey()+".tip", null, new Object[] {
        (frameStack.isEmpty() ? (new StringTextComponent("-/-")) : (new TranslationTextComponent(frameStack.getTranslationKey()))),
        num_used_slots,
        num_free_slots,
        total_items
      }));
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateTileEntity extends TileEntity implements INameable, IInventory, INamedContainerProvider, ISidedInventory
  {
    public static final int NUM_OF_FIELDS = 1;
    public static final int NUM_OF_SLOTS = 55;
    public static final int ITEMFRAME_SLOTNO = 54;

    // BTileEntity -----------------------------------------------------------------------------

    protected NonNullList<ItemStack> stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);

    public LabeledCrateTileEntity()
    { this(ModContent.TET_LABELED_CRATE); }

    public LabeledCrateTileEntity(TileEntityType<?> te_type)
    { super(te_type); reset(); }

    public CompoundNBT reset_getnbt()
    {
      CompoundNBT nbt = new CompoundNBT();
      writenbt(nbt);
      reset();
      return nbt;
    }

    protected void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
    }

    public void readnbt(CompoundNBT compound)
    {
      NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      if(!compound.isEmpty()) ItemStackHelper.loadAllItems(compound, stacks);
      while(stacks.size() < NUM_OF_SLOTS) stacks.add(ItemStack.EMPTY);
      stacks_ = stacks;
    }

    protected void writenbt(CompoundNBT compound)
    {
      if(!stacks_.stream().allMatch(ItemStack::isEmpty)) ItemStackHelper.saveAllItems(compound, stacks_);
    }

    public ItemStack getItemFrameStack()
    { return (stacks_.size() > ITEMFRAME_SLOTNO) ? (stacks_.get(ITEMFRAME_SLOTNO)) : (ItemStack.EMPTY); }

    protected static boolean inacceptable(ItemStack stack)
    { return (stack.hasTag() && (!stack.getTag().isEmpty()) && (unstorable_containers.contains(stack.getItem()))); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void func_230337_a_(BlockState state, CompoundNBT nbt)
    { super.func_230337_a_(state, nbt); readnbt(nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      item_handler_.invalidate();
    }

    @Override
    public CompoundNBT getUpdateTag()
    { CompoundNBT nbt = super.getUpdateTag(); writenbt(nbt); return nbt; }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    { return new SUpdateTileEntityPacket(pos, 1, getUpdateTag()); }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) // on client
    {
      //@todo: check if needed: super.read(pkt.getNbtCompound());
      readnbt(pkt.getNbtCompound());
      super.onDataPacket(net, pkt);
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) // on client
    { func_230337_a_/*read*/(state, tag); }

    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistanceSquared()
    { return 1600; }

    // INameable  ---------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Small Waste Incinerator"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public ITextComponent getCustomName()
    { return getName(); }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public ITextComponent getDisplayName()
    { return INameable.super.getDisplayName(); }

    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new LabeledCrateContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public int getSizeInventory()
    { return stacks_.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return ((index >= 0) && (index < getSizeInventory())) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks_, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks_, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      stacks_.set(index, stack);
      markDirty();
      if(getWorld() instanceof ServerWorld) {
        // This should result in sending TE data (getUpdateTag etc) to the client for the TER.
        BlockState state = world.getBlockState(getPos());
        getWorld().notifyBlockUpdate(getPos(), state, state, 2|16|32);
      }
    }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player)
    { return getPos().distanceSq(player.func_233580_cy_()) < 36; }

    @Override
    public void openInventory(PlayerEntity player)
    {}

    @Override
    public void closeInventory(PlayerEntity player)
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return (index != ITEMFRAME_SLOTNO) && (!inacceptable(stack)); }

    @Override
    public void clear()
    { stacks_.clear(); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(LabeledCrateTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          default: break;
        }
      }
    };

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS;
    static {
      // that useless unoptimised language ... no proper inline conv to int[]?
      // private static final int[] SIDED_INV_SLOTS = IntStream.rangeClosed(0, BTileEntity.NUM_OF_SLOTS-2).boxed().collect(Collectors.toList()).toArray();
      SIDED_INV_SLOTS = new int[LabeledCrateTileEntity.NUM_OF_SLOTS-1];
      for(int i=0; i<SIDED_INV_SLOTS.length; ++i) SIDED_INV_SLOTS[i] = i;
    }

    @Override
    public int[] getSlotsForFace(Direction side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, Direction direction)
    { return true; }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction)
    { return true; }

    // IItemHandler  --------------------------------------------------------------------------------

    protected static class BItemHandler implements IItemHandler
    {
      private LabeledCrateTileEntity te;

      BItemHandler(LabeledCrateTileEntity te)
      { this.te = te; }

      @Override
      public int getSlots()
      { return ITEMFRAME_SLOTNO; } // iframe slot is the last

      @Override
      public int getSlotLimit(int index)
      { return te.getInventoryStackLimit(); }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      { return te.isItemValidForSlot(slot, stack); }

      @Override
      @Nonnull
      public ItemStack insertItem(int slotno, @Nonnull ItemStack stack, boolean simulate)
      {
        if(stack.isEmpty()) return ItemStack.EMPTY;
        if((slotno < 0) || ((slotno >= NUM_OF_SLOTS)) || ((slotno == ITEMFRAME_SLOTNO)) ) return stack;
        if((!isItemValid(slotno, stack))) return stack;
        ItemStack slotstack = getStackInSlot(slotno);
        if(!slotstack.isEmpty()) {
          if(slotstack.getCount() >= Math.min(slotstack.getMaxStackSize(), getSlotLimit(slotno))) return stack;
          if(!ItemHandlerHelper.canItemStacksStack(stack, slotstack)) return stack;
          if(!te.canInsertItem(slotno, stack, Direction.UP) || (!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(slotno)) - slotstack.getCount();
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
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(slotno));
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
        if((index < 0) || ((index >= NUM_OF_SLOTS)) || ((index == ITEMFRAME_SLOTNO)) ) return ItemStack.EMPTY;
        if(!simulate) return ItemStackHelper.getAndSplit(te.stacks_, index, amount);
        ItemStack stack = te.stacks_.get(index).copy();
        if(stack.getCount() > amount) stack.setCount(amount);
        return stack;
      }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      { return te.getStackInSlot(index); }
    }

    // Capability export ----------------------------------------------------------------------------

    protected LazyOptional<IItemHandler> item_handler_ = LazyOptional.of(() -> new LabeledCrateTileEntity.BItemHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      return super.getCapability(capability, facing);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    protected static final String QUICK_MOVE_ALL = "quick-move-all";
    protected static final String INCREASE_STACK = "increase-stack";
    protected static final String DECREASE_STACK = "decrease-stack";

    //------------------------------------------------------------------------------------------------------------------
    protected static class StorageSlot extends Slot
    {
      StorageSlot(IInventory inventory, int index, int x, int y)
      { super(inventory, index, x, y); }

      @Override
      public int getSlotStackLimit()
      { return 64; }

      @Override
      public boolean isItemValid(ItemStack stack)
      { return !LabeledCrateTileEntity.inacceptable(stack); }
    }

    //------------------------------------------------------------------------------------------------------------------
    private static final int PLAYER_INV_START_SLOTNO = LabeledCrateTileEntity.NUM_OF_SLOTS;
    private static final int NUM_OF_CONTAINER_SLOTS = LabeledCrateTileEntity.NUM_OF_SLOTS + 36;
    protected static final int STORAGE_SLOT_BEGIN = 0;
    protected static final int STORAGE_SLOT_END = LabeledCrateTileEntity.ITEMFRAME_SLOTNO;
    protected static final int PLAYER_SLOT_BEGIN = LabeledCrateTileEntity.NUM_OF_SLOTS;
    protected static final int PLAYER_SLOT_END = LabeledCrateTileEntity.NUM_OF_SLOTS+36;
    protected final PlayerEntity player_;
    protected final IInventory inventory_;
    protected final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final SlotRange player_inventory_slot_range;
    private final SlotRange crate_slot_range;
    //------------------------------------------------------------------------------------------------------------------
    public int field(int index) { return fields_.get(index); }
    public PlayerEntity player() { return player_ ; }
    public IInventory inventory() { return inventory_ ; }
    public World world() { return player_.world; }
    //------------------------------------------------------------------------------------------------------------------

    public LabeledCrateContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(LabeledCrateTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new IntArray(LabeledCrateTileEntity.NUM_OF_FIELDS)); }

    private LabeledCrateContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_LABELED_CRATE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      crate_slot_range = new SlotRange(inventory_, 0, LabeledCrateTileEntity.ITEMFRAME_SLOTNO);
      player_inventory_slot_range = new SlotRange(player_inventory, 0, 36);
      int i=-1;
      // storage slots (stacks 0 to 53)
      for(int y=0; y<6; ++y) {
        for(int x=0; x<9; ++x) {
          int xpos = 28+x*18, ypos = 10+y*18;
          addSlot(new StorageSlot(inventory_, ++i, xpos, ypos));
        }
      }
      // picture frame slot (54)
      addSlot(new Slot(new InventoryRange(inventory_, 54, 1), 0, 191, 100) {
        @Override public int getSlotStackLimit(){return 1;}
      });
      // player slots
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 28+x*18, 183)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 28+x*18, 125+y*18)); // player slots: 9..35
        }
      }
    }

    @Override
    public boolean canInteractWith(PlayerEntity player)
    { return inventory_.isUsableByPlayer(player); }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot)
    { return (slot.getSlotStackLimit() > 1); }

    @Override
    public void onContainerClosed(PlayerEntity player)
    { super.onContainerClosed(player); }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Crate slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!mergeItemStack(slot_stack, 0, PLAYER_INV_START_SLOTNO-1, false)) return ItemStack.EMPTY;
      } else {
        // Invalid slot
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

    // Container client/server synchronisation --------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String message, CompoundNBT nbt)
    {
      nbt.putString("action", message);
      Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt);
    }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      boolean changed = false;
      if(!nbt.contains("action")) return;
      final int slotId = nbt.contains("slot") ? nbt.getInt("slot") : -1;
      switch(nbt.getString("action")) {
        case QUICK_MOVE_ALL: {
          if((slotId >= STORAGE_SLOT_BEGIN) && (slotId < STORAGE_SLOT_END) && (getSlot(slotId).getHasStack())) {
            final Slot slot = getSlot(slotId);
            ItemStack remaining = slot.getStack();
            slot.putStack(ItemStack.EMPTY);
            final ItemStack ref_stack = remaining.copy();
            ref_stack.setCount(ref_stack.getMaxStackSize());
            for(int i=crate_slot_range.end_slot-crate_slot_range.start_slot; (i>0) && (!remaining.isEmpty()); --i) {
              remaining = player_inventory_slot_range.insert(remaining, false, 0, true, true);
              if(!remaining.isEmpty()) break;
              remaining = crate_slot_range.extract(ref_stack);
            }
            if(!remaining.isEmpty()) {
              slot.putStack(remaining); // put back
            }
          } else if((slotId >= PLAYER_SLOT_BEGIN) && (slotId < PLAYER_SLOT_END) && (getSlot(slotId).getHasStack())) {
            final Slot slot = getSlot(slotId);
            ItemStack remaining = slot.getStack();
            slot.putStack(ItemStack.EMPTY);
            final ItemStack ref_stack = remaining.copy();
            ref_stack.setCount(ref_stack.getMaxStackSize());
            for(int i=player_inventory_slot_range.end_slot-player_inventory_slot_range.start_slot; (i>0) && (!remaining.isEmpty()); --i) {
              remaining = crate_slot_range.insert(remaining, false, 0, false, true);
              if(!remaining.isEmpty()) break;
              remaining = player_inventory_slot_range.extract(ref_stack);
            }
            if(!remaining.isEmpty()) {
              slot.putStack(remaining); // put back
            }
          }
          changed = true;
        } break;
        case INCREASE_STACK: {
        } break;
        case DECREASE_STACK: {
        } break;
      }
      if(changed) {
        inventory_.markDirty();
        player.inventory.markDirty();
        detectAndSendChanges();
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class LabeledCrateGui extends ContainerScreen<LabeledCrateContainer>
  {
    protected final PlayerEntity player_;

    public LabeledCrateGui(LabeledCrateContainer container, PlayerInventory player_inventory, ITextComponent title)
    {
      super(container, player_inventory, title);
      player_ = player_inventory.player;
      xSize = 213;
      ySize = 206;
    }

    @Override
    public void func_231160_c_/*init*/()
    { super.func_231160_c_(); }

    @Override
    public void func_230430_a_/*render*/(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
    {
      func_230446_a_/*renderBackground*/(mx);
      super.func_230430_a_(mx, mouseX, mouseY, partialTicks);
      func_230459_a_/*renderHoveredToolTip*/(mx, mouseX, mouseY);
    }

    @Override
    protected void func_230451_b_(MatrixStack mx, int x, int y)
    {}

    @Override
    @SuppressWarnings("deprecation")
    protected void func_230450_a_/*drawGuiContainerBackgroundLayer*/(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      getMinecraft().getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/labeled_crate_gui.png"));
      final int x0=guiLeft, y0=this.guiTop, w=xSize, h=ySize;
      func_238474_b_(mx, x0, y0, 0, 0, w, h);
    }

    //------------------------------------------------------------------------------------------------------------------

    protected void action(String message)
    { action(message, new CompoundNBT()); }

    protected void action(String message, CompoundNBT nbt)
    { getContainer().onGuiAction(message, nbt); }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int button, ClickType type)
    {
      if(!with_gui_mouse_handling) {
        super.handleMouseClick(slot, slotId, button, type);
      } else if((type == ClickType.QUICK_MOVE) && (slot!=null) && slot.getHasStack() && Auxiliaries.isShiftDown() && Auxiliaries.isCtrlDown()) {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("slot", slotId);
        action(LabeledCrateContainer.QUICK_MOVE_ALL, nbt);
      } else {
        super.handleMouseClick(slot, slotId, button, type);
      }
    }

    @Override
    public boolean func_231043_a_/*mouseScrolled*/(double mouseX, double mouseY, double wheel_inc)
    {
      if(!with_gui_mouse_handling) return super.func_231043_a_/*mouseScrolled*/(mouseX, mouseY, wheel_inc);
      final Slot slot = getSlotUnderMouse();
      if(!slot.getHasStack()) return true;
      final int count = slot.getStack().getCount();
      int limit = (Auxiliaries.isShiftDown() ? 2 : 1) * (Auxiliaries.isCtrlDown() ? 4 : 1);
      if(wheel_inc > 0.1) {
        if(count > 0) {
          if((count < slot.getStack().getMaxStackSize()) && (count < slot.getSlotStackLimit())) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("slot", slot.slotNumber);
            if(limit > 1) nbt.putInt("limit", limit);
            action(LabeledCrateContainer.INCREASE_STACK, nbt);
          }
        }
      } else if(wheel_inc < -0.1) {
        if(count > 0) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.putInt("slot", slot.slotNumber);
          if(limit > 1) nbt.putInt("limit", limit);
          action(LabeledCrateContainer.DECREASE_STACK, nbt);
        }
      }
      return true;
    }

  }
}
