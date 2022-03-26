/*
 * @file EdLabeledCrate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Storage crate with a content hint.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.RsSignals;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class EdLabeledCrate
{
  private static boolean with_gui_mouse_handling = true;
  private static final HashSet<Item> unstorable_containers = new HashSet<>();

  public static void on_config(boolean without_gui_mouse_handling)
  {
    with_gui_mouse_handling = !without_gui_mouse_handling;
    // Currently no config, using a tag for this small feature may be uselessly stressing the registry.
    unstorable_containers.clear();
    unstorable_containers.add(ModContent.getBlock("labeled_crate").asItem());
    unstorable_containers.add(Items.SHULKER_BOX);
    ModConfig.log("Config crate: unstorable:" + unstorable_containers.stream().map(e->e.getRegistryName().toString()).collect(Collectors.joining(",")));
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<LabeledCrateTileEntity>
  {
    public LabeledCrateBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public ResourceLocation getBlockRegistryName()
    { return getRegistryName(); }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos)
    { return (!(world.getBlockEntity(pos) instanceof LabeledCrateTileEntity te)) ? 0 : RsSignals.fromContainer(te.main_inventory_); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if((world.isClientSide) || (!stack.hasTag())) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof LabeledCrateTileEntity)) return;
      final CompoundTag nbt = stack.getTag();
      if(nbt.contains("tedata")) {
        CompoundTag te_nbt = nbt.getCompound("tedata");
        if(!te_nbt.isEmpty()) ((LabeledCrateTileEntity)te).readnbt(te_nbt);
      }
      ((LabeledCrateTileEntity)te).setCustomName(Auxiliaries.getItemLabel(stack));
      te.setChanged();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, final BlockEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<>();
      if(world.isClientSide()) return stacks;
      if(!(te instanceof LabeledCrateTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag te_nbt = ((LabeledCrateTileEntity)te).getnbt();
        CompoundTag nbt = new CompoundTag();
        if(!te_nbt.isEmpty()) nbt.put("tedata", te_nbt);
        if(!nbt.isEmpty()) stack.setTag(nbt);
        Auxiliaries.setItemLabel(stack, ((LabeledCrateTileEntity)te).getCustomName());
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((LabeledCrateTileEntity)te).main_inventory_) stacks.add(stack);
        ((LabeledCrateTileEntity)te).getnbt();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    { return useOpenGui(state, world, pos, player); }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    { return PushReaction.BLOCK; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
    {
      if(!Auxiliaries.Tooltip.extendedTipCondition() || Auxiliaries.Tooltip.helpCondition()) {
        super.appendHoverText(stack, world, tooltip, flag);
        return;
      }
      ItemStack frameStack = ItemStack.EMPTY;
      int num_used_slots = 0, total_items = 0;
      String stats = "";
      if(stack.hasTag() && stack.getTag().contains("tedata")) {
        final CompoundTag nbt = stack.getTag().getCompound("tedata");
        if(nbt.contains("Items")) {
          final NonNullList<ItemStack> all_items = Inventories.readNbtStacks(nbt, LabeledCrateTileEntity.NUM_OF_SLOTS);
          frameStack = all_items.get(LabeledCrateTileEntity.ITEMFRAME_SLOTNO);
          all_items.set(LabeledCrateTileEntity.ITEMFRAME_SLOTNO, ItemStack.EMPTY);
          Map<Item,Integer> item_map = new HashMap<>();
          for(ItemStack e:all_items) { // ok, the whole stream map collector seems to be actually slower than a simple loop.
            if(!e.isEmpty()) {
              item_map.put(e.getItem(), item_map.getOrDefault(e.getItem(), 0) + e.getCount());
              ++num_used_slots;
              total_items += e.getCount();
            }
          }
          List<Tuple<String,Integer>> itmes = new ArrayList<>();
          for(Map.Entry<Item,Integer> e:item_map.entrySet()) itmes.add(new Tuple<>(e.getKey().getDescriptionId(), e.getValue()));
          itmes.sort((a,b)->b.getB()-a.getB());
          boolean dotdotdot = false;
          if(itmes.size() > 8) { itmes.subList(8, itmes.size()).clear(); dotdotdot = true; }
          stats = itmes.stream().map(e->Auxiliaries.localize(e.getA())).collect(Collectors.joining(", "));
          if(dotdotdot) stats += "...";
        }
      }
      int num_free_slots = LabeledCrateTileEntity.ITEMFRAME_SLOTNO - num_used_slots;
      String[] lines = Auxiliaries.localize(getDescriptionId()+".tip",
        (frameStack.isEmpty() ? (new TextComponent("-/-")) : (new TranslatableComponent(frameStack.getDescriptionId()))),
        num_used_slots,
        num_free_slots,
        total_items,
        stats).split("\n");
      for(String line:lines) {
        tooltip.add(new TextComponent(line.trim()));
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable, Networking.IPacketTileNotifyReceiver
  {
    public static final int NUM_OF_FIELDS = 1;
    public static final int NUM_OF_SLOTS = 55;
    public static final int NUM_OF_STORAGE_SLOTS = 54;
    public static final int NUM_OF_STORAGE_ROWS = 6;
    public static final int ITEMFRAME_SLOTNO = NUM_OF_STORAGE_SLOTS;

    private final Inventories.StorageInventory main_inventory_ = new Inventories.StorageInventory(this, NUM_OF_SLOTS, 1);
    private final Inventories.InventoryRange storage_range_ = new Inventories.InventoryRange(main_inventory_, 0, NUM_OF_STORAGE_SLOTS, NUM_OF_STORAGE_ROWS);
    private final LazyOptional<IItemHandler> item_handler_;

    private @Nullable Component custom_name_;

    public LabeledCrateTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.getBlockEntityTypeOfBlock(state.getBlock().getRegistryName().getPath()), pos, state);
      main_inventory_.setCloseAction((player)->Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(new CompoundTag())));
      main_inventory_.setSlotChangeAction((index,stack)->{
        if(index==ITEMFRAME_SLOTNO) Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(new CompoundTag()));
      });
      item_handler_ = Inventories.MappedItemHandler.createGenericHandler(storage_range_,
        (slot,stack)->(slot!=ITEMFRAME_SLOTNO),
        (slot,stack)->(slot!=ITEMFRAME_SLOTNO),
        IntStream.range(0, NUM_OF_STORAGE_SLOTS).boxed().collect(Collectors.toList())
      );
    }

    public CompoundTag getnbt()
    { return writenbt(new CompoundTag()); }

    public CompoundTag readnbt(CompoundTag nbt)
    {
      if(nbt.contains("name", Tag.TAG_STRING)) custom_name_ = Auxiliaries.unserializeTextComponent(nbt.getString("name"));
      main_inventory_.load(nbt);
      return nbt;
    }

    protected CompoundTag writenbt(CompoundTag nbt)
    {
      if(custom_name_ != null) nbt.putString("name", Auxiliaries.serializeTextComponent(custom_name_));
      if(!main_inventory_.isEmpty()) main_inventory_.save(nbt);
      return nbt;
    }

    public ItemStack getItemFrameStack()
    { return main_inventory_.getItem(ITEMFRAME_SLOTNO); }

    protected static boolean inacceptable(ItemStack stack)
    { return (stack.hasTag() && (!stack.getTag().isEmpty()) && (unstorable_containers.contains(stack.getItem()))); }

    // IPacketTileNotifyReceiver ---------------------------------------------------------------

    @Override
    public void onServerPacketReceived(CompoundTag nbt)
    { readnbt(nbt); }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.saveAdditional(nbt); writenbt(nbt); }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      item_handler_.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag()
    { CompoundTag nbt = super.getUpdateTag(); writenbt(nbt); return nbt; }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) // on client
    { super.onDataPacket(net, pkt); if(pkt.getTag() != null) { readnbt(pkt.getTag()); } }

    @Override
    public void handleUpdateTag(CompoundTag tag) // on client
    { load(tag); }

    @OnlyIn(Dist.CLIENT)
    public double getViewDistance()
    { return 1600; }

    // INameable  ---------------------------------------------------------------------------

    @Override
    public Component getName()
    {
      if(custom_name_ != null) return custom_name_;
      final Block block = getBlockState().getBlock();
      if(block!=null) return new TranslatableComponent(block.getDescriptionId());
      return new TextComponent("Labeled Crate");
    }

    @Override
    @Nullable
    public Component getCustomName()
    { return custom_name_; }

    @Override
    public boolean hasCustomName()
    { return (custom_name_ != null); }

    public void setCustomName(Component name)
    { custom_name_ = name; }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public Component getDisplayName()
    { return Nameable.super.getDisplayName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
    { return new LabeledCrateContainer(id, inventory, main_inventory_, ContainerLevelAccess.create(level, worldPosition), fields); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final ContainerData fields = new ContainerData()
    {
      @Override
      public int getCount()
      { return LabeledCrateTileEntity.NUM_OF_FIELDS; }

      @Override
      public int get(int id)
      {
        return switch (id) {
          default -> 0;
        };
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          default: break;
        }
      }
    };

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
  {
    protected static final String QUICK_MOVE_ALL = "quick-move-all";
    protected static final String INCREASE_STACK = "increase-stack";
    protected static final String DECREASE_STACK = "decrease-stack";

    //------------------------------------------------------------------------------------------------------------------
    protected static class StorageSlot extends Slot
    {
      StorageSlot(Container inventory, int index, int x, int y)
      { super(inventory, index, x, y); }

      @Override
      public int getMaxStackSize()
      { return 64; }

      @Override
      public boolean mayPlace(ItemStack stack)
      { return !LabeledCrateTileEntity.inacceptable(stack); }
    }

    //------------------------------------------------------------------------------------------------------------------
    private static final int PLAYER_INV_START_SLOTNO = LabeledCrateTileEntity.NUM_OF_SLOTS;
    private static final int NUM_OF_CONTAINER_SLOTS = LabeledCrateTileEntity.NUM_OF_SLOTS + 36;
    protected static final int STORAGE_SLOT_BEGIN = 0;
    protected static final int STORAGE_SLOT_END = LabeledCrateTileEntity.ITEMFRAME_SLOTNO;
    protected static final int PLAYER_SLOT_BEGIN = LabeledCrateTileEntity.NUM_OF_SLOTS;
    protected static final int PLAYER_SLOT_END = LabeledCrateTileEntity.NUM_OF_SLOTS+36;
    protected final Player player_;
    protected final Container inventory_;
    protected final ContainerLevelAccess wpc_;
    private final ContainerData fields_;
    private final Inventories.InventoryRange player_inventory_range_;
    private final Inventories.InventoryRange block_storage_range_;
    private final Inventories.InventoryRange frame_slot_range_;
    //------------------------------------------------------------------------------------------------------------------
    public int field(int index) { return fields_.get(index); }
    public Player player() { return player_ ; }
    public Container inventory() { return inventory_ ; }
    public Level world() { return player_.level; }
    //------------------------------------------------------------------------------------------------------------------

    public LabeledCrateContainer(int cid, Inventory player_inventory)
    { this(cid, player_inventory, new SimpleContainer(LabeledCrateTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(LabeledCrateTileEntity.NUM_OF_FIELDS)); }

    private LabeledCrateContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
    {
      super(ModContent.getMenuType("labeled_crate"), cid); // @todo: class mapping
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      wpc_.execute((w,p)->inventory_.startOpen(player_));
      fields_ = fields;
      block_storage_range_ = new Inventories.InventoryRange(inventory_, 0, LabeledCrateTileEntity.ITEMFRAME_SLOTNO);
      player_inventory_range_ = new Inventories.InventoryRange(player_inventory, 0, 36);
      frame_slot_range_ = new Inventories.InventoryRange(inventory_, 54, 1);
      int i=-1;
      // storage slots (stacks 0 to 53)
      for(int y=0; y<6; ++y) {
        for(int x=0; x<9; ++x) {
          int xpos = 28+x*18, ypos = 10+y*18;
          addSlot(new StorageSlot(inventory_, ++i, xpos, ypos));
        }
      }
      // picture frame slot (54)
      addSlot(new Slot(frame_slot_range_, 0, 191, 100) { @Override public int getMaxStackSize(){return 1;} });
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
    public boolean stillValid(Player player)
    { return inventory_.stillValid(player); }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
    { return (slot.getMaxStackSize() > 1); }

    @Override
    public void removed(Player player)
    {
      super.removed(player);
      inventory_.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.hasItem())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getItem();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Crate slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!moveItemStackTo(slot_stack, 0, PLAYER_INV_START_SLOTNO-1, false)) return ItemStack.EMPTY;
      } else {
        // Invalid slot
        return ItemStack.EMPTY;
      }
      if(slot_stack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
      if(slot_stack.getCount() == transferred.getCount()) return ItemStack.EMPTY;
      slot.onTake(player, slot_stack);
      return transferred;
    }

    // Container client/server synchronisation --------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String message, CompoundTag nbt)
    {
      nbt.putString("action", message);
      Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt);
    }

    @Override
    public void onServerPacketReceived(int windowId, CompoundTag nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, Player player, CompoundTag nbt)
    {
      if(!nbt.contains("action")) return;
      boolean changed = false;
      final int slotId = nbt.contains("slot") ? nbt.getInt("slot") : -1;
      switch (nbt.getString("action")) {
        case QUICK_MOVE_ALL -> {
          if((slotId >= STORAGE_SLOT_BEGIN) && (slotId < STORAGE_SLOT_END) && (getSlot(slotId).hasItem())) {
            changed = block_storage_range_.move(getSlot(slotId).getSlotIndex(), player_inventory_range_, true, false, true, true);
          } else if ((slotId >= PLAYER_SLOT_BEGIN) && (slotId < PLAYER_SLOT_END) && (getSlot(slotId).hasItem())) {
            changed = player_inventory_range_.move(getSlot(slotId).getSlotIndex(), block_storage_range_, true, false, false, true);
          }
        }
        case INCREASE_STACK -> {}
        case DECREASE_STACK -> {}
      }
      if(changed) {
        inventory_.setChanged();
        player.getInventory().setChanged();
        broadcastChanges();
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class LabeledCrateGui extends Guis.ContainerGui<LabeledCrateContainer>
  {
    public LabeledCrateGui(LabeledCrateContainer container, Inventory player_inventory, Component title)
    {
      super(container, player_inventory, title,"textures/gui/labeled_crate_gui.png", 213, 206);
      titleLabelX = 23;
      titleLabelY = -10;
    }

    @Override
    protected void renderLabels(PoseStack mx, int x, int y)
    {
      font.draw(mx, title, (float)titleLabelX+1, (float)titleLabelY+1, 0x303030);
      font.draw(mx, title, (float)titleLabelX, (float)titleLabelY, 0x707070);
    }

    //------------------------------------------------------------------------------------------------------------------

    protected void action(String message)
    { action(message, new CompoundTag()); }

    protected void action(String message, CompoundTag nbt)
    { getMenu().onGuiAction(message, nbt); }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type)
    {
      if(!with_gui_mouse_handling) {
        super.slotClicked(slot, slotId, button, type);
      } else if((type == ClickType.QUICK_MOVE) && (slot!=null) && slot.hasItem() && Auxiliaries.isShiftDown() && Auxiliaries.isCtrlDown()) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("slot", slotId);
        action(LabeledCrateContainer.QUICK_MOVE_ALL, nbt);
      } else {
        super.slotClicked(slot, slotId, button, type);
      }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double wheel_inc)
    {
      if(!with_gui_mouse_handling) return super.mouseScrolled(mouseX, mouseY, wheel_inc);
      final Slot slot = getSlotUnderMouse();
      if((slot==null) || (!slot.hasItem())) return true;
      final int count = slot.getItem().getCount();
      int limit = (Auxiliaries.isShiftDown() ? 2 : 1) * (Auxiliaries.isCtrlDown() ? 4 : 1);
      if(wheel_inc > 0.1) {
        if(count > 0) {
          if((count < slot.getItem().getMaxStackSize()) && (count < slot.getMaxStackSize())) {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("slot", slot.index);
            if(limit > 1) nbt.putInt("limit", limit);
            action(LabeledCrateContainer.INCREASE_STACK, nbt);
          }
        }
      } else if(wheel_inc < -0.1) {
        if(count > 0) {
          CompoundTag nbt = new CompoundTag();
          nbt.putInt("slot", slot.index);
          if(limit > 1) nbt.putInt("limit", limit);
          action(LabeledCrateContainer.DECREASE_STACK, nbt);
        }
      }
      return true;
    }

  }
}
