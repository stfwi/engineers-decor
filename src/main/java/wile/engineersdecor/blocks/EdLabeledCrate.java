/*
 * @file EdLabeledCrate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Storage crate with a content hint.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.AbstractBlock;
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
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.client.ContainerGui;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Inventories.InventoryRange;
import wile.engineersdecor.libmc.detail.Inventories.StorageInventory;
import wile.engineersdecor.libmc.detail.Inventories.MappedItemHandler;
import wile.engineersdecor.libmc.detail.Networking;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
    ModConfig.log("Config crate: unstorable:" + unstorable_containers.stream().map(e->e.getRegistryName().toString()).collect(Collectors.joining(",")));
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateBlock extends StandardBlocks.Horizontal implements IDecorBlock
  {
    public LabeledCrateBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, World world, BlockPos pos)
    { return Container.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos)); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new LabeledCrateTileEntity(); }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if((world.isClientSide) || (!stack.hasTag())) return;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof LabeledCrateTileEntity)) return;
      final CompoundNBT nbt = stack.getTag();
      if(nbt.contains("tedata")) {
        CompoundNBT te_nbt = nbt.getCompound("tedata");
        if(!te_nbt.isEmpty()) ((LabeledCrateTileEntity)te).readnbt(te_nbt);
      }
      ((LabeledCrateTileEntity)te).setCustomName(Auxiliaries.getItemLabel(stack));
      ((LabeledCrateTileEntity)te).setChanged();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isClientSide()) return stacks;
      if(!(te instanceof LabeledCrateTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT te_nbt = ((LabeledCrateTileEntity)te).getnbt();
        CompoundNBT nbt = new CompoundNBT();
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
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof LabeledCrateTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      return ActionResultType.CONSUME;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    { return PushReaction.BLOCK; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
      if(!Auxiliaries.Tooltip.extendedTipCondition() || Auxiliaries.Tooltip.helpCondition()) {
        super.appendHoverText(stack, world, tooltip, flag);
        return;
      }
      ItemStack frameStack = ItemStack.EMPTY;
      int num_used_slots = 0, total_items = 0;
      String stats = "";
      if(stack.hasTag() && stack.getTag().contains("tedata")) {
        final CompoundNBT nbt = stack.getTag().getCompound("tedata");
        if(nbt.contains("Items")) {
          NonNullList<ItemStack> all_items = NonNullList.withSize(LabeledCrateTileEntity.NUM_OF_SLOTS, ItemStack.EMPTY);
          ItemStackHelper.loadAllItems(nbt, all_items);
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
      String[] lines = Auxiliaries.localize(getDescriptionId()+".tip", new Object[] {
        (frameStack.isEmpty() ? (new StringTextComponent("-/-")) : (new TranslationTextComponent(frameStack.getDescriptionId()))),
        num_used_slots,
        num_free_slots,
        total_items,
        stats
      }).split("\n");
      for(String line:lines) {
        tooltip.add(new StringTextComponent(line.trim()));
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateTileEntity extends TileEntity implements INameable, INamedContainerProvider, Networking.IPacketTileNotifyReceiver
  {
    public static final int NUM_OF_FIELDS = 1;
    public static final int NUM_OF_SLOTS = 55;
    public static final int NUM_OF_STORAGE_SLOTS = 54;
    public static final int NUM_OF_STORAGE_ROWS = 6;
    public static final int ITEMFRAME_SLOTNO = NUM_OF_STORAGE_SLOTS;

    protected final Inventories.StorageInventory main_inventory_;
    protected LazyOptional<IItemHandler> item_handler_;
    protected final InventoryRange storage_range_;
    private @Nullable ITextComponent custom_name_;

    public LabeledCrateTileEntity()
    { this(ModContent.TET_LABELED_CRATE); }

    public LabeledCrateTileEntity(TileEntityType<?> te_type)
    {
      super(te_type);
      main_inventory_ = new StorageInventory(this, NUM_OF_SLOTS, 1);
      storage_range_ = new InventoryRange(main_inventory_, 0, NUM_OF_STORAGE_SLOTS, NUM_OF_STORAGE_ROWS);
      item_handler_ = MappedItemHandler.createGenericHandler(storage_range_,
        (slot,stack)->(slot!=ITEMFRAME_SLOTNO),
        (slot,stack)->(slot!=ITEMFRAME_SLOTNO),
        IntStream.range(0, NUM_OF_STORAGE_SLOTS).boxed().collect(Collectors.toList())
      );
      main_inventory_.setCloseAction((player)->{ Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(new CompoundNBT())); });
      main_inventory_.setSlotChangeAction((index,stack)->{
        if(index==ITEMFRAME_SLOTNO) Networking.PacketTileNotifyServerToClient.sendToPlayers(this, writenbt(new CompoundNBT()));
      });
    }

    public CompoundNBT getnbt()
    { return writenbt(new CompoundNBT()); }

    public CompoundNBT readnbt(CompoundNBT nbt)
    {
      if(nbt.contains("name", NBT.TAG_STRING)) custom_name_ = Auxiliaries.unserializeTextComponent(nbt.getString("name"));
      main_inventory_.load(nbt);
      return nbt;
    }

    protected CompoundNBT writenbt(CompoundNBT nbt)
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
    public void onServerPacketReceived(CompoundNBT nbt)
    { readnbt(nbt); }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      return super.getCapability(capability, facing);
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
      item_handler_.invalidate();
    }

    @Override
    public CompoundNBT getUpdateTag()
    { CompoundNBT nbt = super.getUpdateTag(); writenbt(nbt); return nbt; }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    { return new SUpdateTileEntityPacket(worldPosition, 1, getUpdateTag()); }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) // on client
    { readnbt(pkt.getTag()); super.onDataPacket(net, pkt); }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) // on client
    { load(state, tag); }

    @OnlyIn(Dist.CLIENT)
    public double getViewDistance()
    { return 1600; }

    // INameable  ---------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    {
      if(custom_name_ != null) return custom_name_;
      final Block block = getBlockState().getBlock();
      if(block!=null) return new TranslationTextComponent(block.getDescriptionId());
      return new StringTextComponent("Labeled Crate");
    }

    @Override
    @Nullable
    public ITextComponent getCustomName()
    { return custom_name_; }

    @Override
    public boolean hasCustomName()
    { return (custom_name_ != null); }

    public void setCustomName(ITextComponent name)
    { custom_name_ = name; }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public ITextComponent getDisplayName()
    { return INameable.super.getDisplayName(); }

    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new LabeledCrateContainer(id, inventory, main_inventory_, IWorldPosCallable.create(level, worldPosition), fields); }

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
    protected final PlayerEntity player_;
    protected final IInventory inventory_;
    protected final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final InventoryRange player_inventory_range_;
    private final InventoryRange block_storage_range_;
    private final InventoryRange frame_slot_range_;
    //------------------------------------------------------------------------------------------------------------------
    public int field(int index) { return fields_.get(index); }
    public PlayerEntity player() { return player_ ; }
    public IInventory inventory() { return inventory_ ; }
    public World world() { return player_.level; }
    //------------------------------------------------------------------------------------------------------------------

    public LabeledCrateContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(LabeledCrateTileEntity.NUM_OF_SLOTS), IWorldPosCallable.NULL, new IntArray(LabeledCrateTileEntity.NUM_OF_FIELDS)); }

    private LabeledCrateContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_LABELED_CRATE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      wpc_.execute((w,p)->inventory_.startOpen(player_));
      fields_ = fields;
      block_storage_range_ = new InventoryRange(inventory_, 0, LabeledCrateTileEntity.ITEMFRAME_SLOTNO);
      player_inventory_range_ = new InventoryRange(player_inventory, 0, 36);
      frame_slot_range_ = new InventoryRange(inventory_, 54, 1);
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
    public boolean stillValid(PlayerEntity player)
    { return inventory_.stillValid(player); }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
    { return (slot.getMaxStackSize() > 1); }

    @Override
    public void removed(PlayerEntity player)
    {
      super.removed(player);
      inventory_.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index)
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
    public void onGuiAction(String message, CompoundNBT nbt)
    {
      nbt.putString("action", message);
      Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt);
    }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      if(!nbt.contains("action")) return;
      boolean changed = false;
      final int slotId = nbt.contains("slot") ? nbt.getInt("slot") : -1;
      switch(nbt.getString("action")) {
        case QUICK_MOVE_ALL: {
          if((slotId >= STORAGE_SLOT_BEGIN) && (slotId < STORAGE_SLOT_END) && (getSlot(slotId).hasItem())) {
            changed = block_storage_range_.move(getSlot(slotId).getSlotIndex(), player_inventory_range_, true, false, true, true);
          } else if((slotId >= PLAYER_SLOT_BEGIN) && (slotId < PLAYER_SLOT_END) && (getSlot(slotId).hasItem())) {
            changed = player_inventory_range_.move(getSlot(slotId).getSlotIndex(), block_storage_range_, true, false, false, true);
          }
        } break;
        case INCREASE_STACK: {
        } break;
        case DECREASE_STACK: {
        } break;
      }
      if(changed) {
        inventory_.setChanged();
        player.inventory.setChanged();
        broadcastChanges();
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class LabeledCrateGui extends ContainerGui<LabeledCrateContainer>
  {
    protected final PlayerEntity player_;

    public LabeledCrateGui(LabeledCrateContainer container, PlayerInventory player_inventory, ITextComponent title)
    {
      super(container, player_inventory, title);
      player_ = player_inventory.player;
      imageWidth = 213;
      imageHeight = 206;
      titleLabelX = 23;
      titleLabelY = -10;
    }

    @Override
    public void init()
    { super.init(); }

    @Override
    public void render(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
    {
      renderBackground/*renderBackground*/(mx);
      super.render(mx, mouseX, mouseY, partialTicks);
      renderTooltip(mx, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(MatrixStack mx, int x, int y)
    {
      font.draw(mx, title, (float)titleLabelX+1, (float)titleLabelY+1, 0x303030);
      font.draw(mx, title, (float)titleLabelX, (float)titleLabelY, 0x707070);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void renderBg(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager._color4f(1.0F, 1.0F, 1.0F, 1.0F);
      getMinecraft().getTextureManager().bind(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/labeled_crate_gui.png"));
      final int x0=leftPos, y0=this.topPos, w=imageWidth, h=imageHeight;
      blit(mx, x0, y0, 0, 0, w, h);
    }

    //------------------------------------------------------------------------------------------------------------------

    protected void action(String message)
    { action(message, new CompoundNBT()); }

    protected void action(String message, CompoundNBT nbt)
    { getMenu().onGuiAction(message, nbt); }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type)
    {
      if(!with_gui_mouse_handling) {
        super.slotClicked(slot, slotId, button, type);
      } else if((type == ClickType.QUICK_MOVE) && (slot!=null) && slot.hasItem() && Auxiliaries.isShiftDown() && Auxiliaries.isCtrlDown()) {
        CompoundNBT nbt = new CompoundNBT();
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
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("slot", slot.index);
            if(limit > 1) nbt.putInt("limit", limit);
            action(LabeledCrateContainer.INCREASE_STACK, nbt);
          }
        }
      } else if(wheel_inc < -0.1) {
        if(count > 0) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.putInt("slot", slot.index);
          if(limit > 1) nbt.putInt("limit", limit);
          action(LabeledCrateContainer.DECREASE_STACK, nbt);
        }
      }
      return true;
    }

  }
}
