/*
 * @file EdCraftingTable.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Crafting table
 */
package wile.engineersdecor.blocks;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.hooks.BasicEventHooks;
import net.minecraftforge.registries.ForgeRegistries;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Inventories.InventoryRange;
import wile.engineersdecor.libmc.detail.Inventories.StorageInventory;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class EdCraftingTable
{
  public static boolean with_assist = true;
  public static boolean with_assist_direct_history_refab = false;
  public static boolean with_crafting_slot_mouse_scrolling = true;
  public static boolean with_outslot_defined_refab = true;

  public static void on_config(boolean without_crafting_assist, boolean with_assist_immediate_history_refab, boolean without_crafting_slot_mouse_scrolling)
  {
    with_assist = !without_crafting_assist;
    with_assist_direct_history_refab = with_assist_immediate_history_refab;
    with_crafting_slot_mouse_scrolling = !without_crafting_slot_mouse_scrolling;
    with_outslot_defined_refab = with_assist;
    CraftingHistory.max_history_size(32);
    ModConfig.log("Config crafting table: assist:" + with_assist + ", direct-refab:" + with_assist_direct_history_refab +
                               ", scrolling:"+with_crafting_slot_mouse_scrolling);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static final class CraftingTableBlock extends StandardBlocks.HorizontalWaterLoggable implements StandardEntityBlocks.IStandardEntityBlock<CraftingTableTileEntity>
  {
    public CraftingTableBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

    @Nullable
    @Override
    public BlockEntityType<EdCraftingTable.CraftingTableTileEntity> getBlockEntityType()
    { return ModContent.TET_CRAFTING_TABLE; }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    { return useOpenGui(state, world, pos, player); }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      final CompoundTag inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      if(!(world.getBlockEntity(pos) instanceof final CraftingTableTileEntity te)) return;
      te.readnbt(inventory_nbt);
      te.setChanged();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, final BlockEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof CraftingTableTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag inventory_nbt = new CompoundTag();
        ((CraftingTableTileEntity)te).mainInventory().save(inventory_nbt, false);
        if(!inventory_nbt.isEmpty()) {
          CompoundTag nbt = new CompoundTag();
          nbt.put("inventory", inventory_nbt);
          stack.setTag(nbt);
        }
        ((CraftingTableTileEntity) te).mainInventory().clearContent();
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((CraftingTableTileEntity)te).mainInventory()) {
          if(!stack.isEmpty()) stacks.add(stack);
        }
        ((CraftingTableTileEntity)te).reset();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random rand)
    {
      BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof CraftingTableTileEntity)) return;
      ((CraftingTableTileEntity)te).sync();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class CraftingTableTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable, Networking.IPacketTileNotifyReceiver
  {
    public static final int NUM_OF_STORAGE_SLOTS = 18;
    public static final int NUM_OF_STORAGE_ROWS = 2;
    public static final int NUM_OF_SLOTS = 9+NUM_OF_STORAGE_SLOTS+1;
    public static final int CRAFTING_RESULT_SLOT = NUM_OF_SLOTS-1;

    protected Inventories.StorageInventory inventory_;
    protected CompoundTag history = new CompoundTag();

    public CraftingTableTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.TET_CRAFTING_TABLE, pos, state);
      inventory_ = new StorageInventory(this, NUM_OF_SLOTS, 1);
      inventory_.setCloseAction((player)->{
        if(getLevel() instanceof Level) {
          scheduleSync();
          getLevel().sendBlockUpdated(getBlockPos(), state, state, 1|2|16);
        }
      });
      inventory_.setSlotChangeAction((slot_index,stack)-> {
        if(slot_index < 9) scheduleSync();
      });
    }

    public void reset()
    { inventory_.clearContent(); }

    public void readnbt(CompoundTag nbt)
    { reset(); inventory_.load(nbt); history = nbt.getCompound("history"); }

    private void writenbt(CompoundTag nbt)
    {
      inventory_.save(nbt);
      if(!history.isEmpty()) nbt.put("history", history);
    }

    public Inventories.StorageInventory mainInventory()
    { return inventory_; }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt); }

    @Override
    public CompoundTag save(CompoundTag nbt)
    { super.save(nbt); writenbt(nbt); return nbt; }

    @Override
    public CompoundTag getUpdateTag()
    { CompoundTag nbt = super.getUpdateTag(); writenbt(nbt); return nbt; }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    { return new ClientboundBlockEntityDataPacket(worldPosition, 1, getUpdateTag()); }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) // on client
    { readnbt(pkt.getTag()); super.onDataPacket(net, pkt); }

    @Override
    public void handleUpdateTag(CompoundTag tag) // on client
    { load(tag); }

    @OnlyIn(Dist.CLIENT)
    public double getViewDistance()
    { return 400; }

    // Nameable ----------------------------------------------------------------------------

    @Override
    public Component getName()
    { final Block block=getBlockState().getBlock(); return new TextComponent((block!=null) ? block.getDescriptionId() : "Treated wood crafting table"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public Component getCustomName()
    { return getName(); }

    // INamedContainerProvider ------------------------------------------------------------------------------

    @Override
    public Component getDisplayName()
    { return Nameable.super.getDisplayName(); }

    @Override
    public AbstractContainerMenu createMenu( int id, Inventory inventory, Player player )
    { return new CraftingTableUiContainer(id, inventory, inventory_, ContainerLevelAccess.create(level, worldPosition)); }

    @Override
    public void onServerPacketReceived(CompoundTag nbt)
    { readnbt(nbt); }

    public void sync()
    {
      if(getLevel().isClientSide()) return;
      CompoundTag nbt = new CompoundTag();
      writenbt(nbt);
      Networking.PacketTileNotifyServerToClient.sendToPlayers(this, nbt);
    }

    public void scheduleSync()
    {
      if(level.isClientSide()) return;
      final Block crafting_table_block = getBlockState().getBlock();
      if(!(crafting_table_block instanceof CraftingTableBlock)) return;
      if(level.getBlockTicks().hasScheduledTick(getBlockPos(), crafting_table_block)) return;
      level.getBlockTicks().scheduleTick(getBlockPos(), crafting_table_block, 10, TickPriority.LOW);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting container
  //--------------------------------------------------------------------------------------------------------------------

  public static class CraftingTableUiContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
  {
    protected static final String BUTTON_NEXT = "next";
    protected static final String BUTTON_PREV = "prev";
    protected static final String BUTTON_CLEAR_GRID = "clear";
    protected static final String BUTTON_NEXT_COLLISION_RECIPE = "next-recipe";
    protected static final String ACTION_PLACE_CURRENT_HISTORY_SEL = "place-refab";
    protected static final String ACTION_PLACE_SHIFTCLICKED_STACK = "place-stack";
    protected static final String ACTION_MOVE_ALL_STACKS = "move-stacks";
    protected static final String ACTION_MOVE_STACK = "move-stack";
    protected static final String ACTION_INCREASE_CRAFTING_STACKS = "inc-crafting-stacks";
    protected static final String ACTION_DECREASE_CRAFTING_STACKS = "dec-crafting-stacks";

    public static final int CRAFTING_SLOTS_BEGIN = 0;
    public static final int CRAFTING_SLOTS_SIZE = 9;
    public static final int NUM_OF_STORAGE_SLOTS = CraftingTableTileEntity.NUM_OF_STORAGE_SLOTS;
    public static final int NUM_OF_STORAGE_ROWS = CraftingTableTileEntity.NUM_OF_STORAGE_ROWS;

    public final ImmutableList<Tuple<Integer,Integer>> CRAFTING_SLOT_COORDINATES;
    private final Player player_;
    private final Container inventory_;
    private final ContainerLevelAccess wpc_;
    private final CraftingHistory history_;
    private final CraftingTableGrid matrix_;
    private final CraftOutputInventory result_;
    private final CraftingOutputSlot crafting_output_slot_;
    private boolean has_recipe_collision_;
    private boolean crafting_matrix_changed_now_;
    private final InventoryRange crafting_grid_range_;
    private final InventoryRange crafting_result_range_;
    private final InventoryRange block_storage_range_;
    private final InventoryRange player_storage_range_;
    private final InventoryRange player_hotbar_range_;
    private final InventoryRange player_inventory_range_;
    private final @Nullable CraftingTableTileEntity te_;

    public CraftingTableUiContainer(int cid, Inventory pinv)
    { this(cid, pinv, new SimpleContainer(CraftingTableTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL); }

    private CraftingTableUiContainer(int cid, Inventory pinv, Container block_inventory, ContainerLevelAccess wpc)
    {
      super(ModContent.CT_TREATED_WOOD_CRAFTING_TABLE, cid);
      wpc_ = wpc;
      player_ = pinv.player;
      inventory_ = block_inventory;
      inventory_.startOpen(player_);
      Level world = player_.level;
      if((inventory_ instanceof StorageInventory) && ((((StorageInventory)inventory_).getTileEntity()) instanceof CraftingTableTileEntity)) {
        te_ = (CraftingTableTileEntity)(((StorageInventory)inventory_).getTileEntity());
      } else {
        te_ = null;
      }
      crafting_grid_range_  = new InventoryRange(inventory_, 0, 9, 3);
      block_storage_range_  = new InventoryRange(inventory_, CRAFTING_SLOTS_SIZE, NUM_OF_STORAGE_SLOTS, NUM_OF_STORAGE_ROWS);
      crafting_result_range_= new InventoryRange(inventory_, CraftingTableTileEntity.CRAFTING_RESULT_SLOT, 1, 1);
      player_storage_range_ = InventoryRange.fromPlayerStorage(player_);
      player_hotbar_range_  = InventoryRange.fromPlayerHotbar(player_);
      player_inventory_range_= InventoryRange.fromPlayerInventory(player_);
      matrix_ = new CraftingTableGrid(this, inventory_);
      result_ = new CraftOutputInventory(crafting_result_range_);
      history_ = new CraftingHistory(world);
      // container slotId 0 === crafting output
      addSlot(crafting_output_slot_=(new CraftingOutputSlot(this, pinv.player, matrix_, result_, 0, 118, 27)));
      ArrayList<Tuple<Integer,Integer>> slotpositions = new ArrayList<>();
      slotpositions.add(new Tuple<>(118, 27));
      // container slotId 1..9 === TE slots 0..8
      for(int y=0; y<3; ++y) {
        for(int x=0; x<3; ++x) {
          int xpos = 44+x*18;
          int ypos =  9+y*18;
          addSlot(new CraftingGridSlot(matrix_, x+y*3, xpos, ypos));
          slotpositions.add(new Tuple<>(xpos, ypos));
        }
      }
      // container slotId 10..36 === player slots: 9..35
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(pinv, x+y*9+9, 8+x*18, 110+y*18));
        }
      }
      // container slotId 37..45 === player slots: 0..8
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(pinv, x, 8+x*18, 168));
      }
      // container slotId 46..63 === TE slots 9..27 (storage)
      for(int y=0; y<2; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(inventory_, 9+x+y*9, 8+x*18, 65+y*18));
        }
      }
      if((!player_.level.isClientSide()) && (te_ != null)) {
        history_.read(te_.history.copy());
      }
      CRAFTING_SLOT_COORDINATES = ImmutableList.copyOf(slotpositions);
      onCraftMatrixChanged();
    }

    @Override
    public boolean stillValid(Player player)
    { return inventory_.stillValid(player); }

    public void onCraftMatrixChanged()
    { slotsChanged(matrix_); }

    @Override
    public void slotsChanged(Container inv)
    {
      wpc_.execute((world,pos)->{
        if(world.isClientSide()) return;
        try {
          crafting_matrix_changed_now_ = true;
          ServerPlayer player = (ServerPlayer) player_;
          ItemStack stack = ItemStack.EMPTY;
          List<CraftingRecipe> recipes = world.getServer().getRecipeManager().getRecipesFor(RecipeType.CRAFTING, matrix_, world);
          has_recipe_collision_ = false;
          if(recipes.size() > 0) {
            CraftingRecipe recipe = recipes.get(0);
            Recipe<?> currently_used = result_.getRecipeUsed();
            has_recipe_collision_ = (recipes.size() > 1);
            if((recipes.size() > 1) && (currently_used instanceof CraftingRecipe) && (recipes.contains(currently_used))) {
              recipe = (CraftingRecipe)currently_used;
            }
            if(result_.setRecipeUsed(world, player, recipe)) {
              broadcastChanges();
              stack = recipe.assemble(matrix_);
            }
          }
          result_.setItem(0, stack);
          broadcastChanges();
        } catch(Throwable exc) {
          ModEngineersDecor.logger().error("Recipe failed:", exc);
        }
      });
    }

    @Override
    public void removed(Player player)
    { inventory_.stopOpen(player); }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
    { return (slot.container != result_) && (super.canTakeItemForPickAll(stack, slot)); }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
      Slot slot = slots.get(index);
      if((slot == null) || (!slot.hasItem())) return ItemStack.EMPTY;
      ItemStack slotstack = slot.getItem();
      ItemStack stack = slotstack.copy();
      if(index == 0) {
        if(!this.moveItemStackTo(slotstack, 10, 46+NUM_OF_STORAGE_SLOTS, false)) return ItemStack.EMPTY;
        wpc_.execute((world, pos)->slotstack.getItem().onCraftedBy(slotstack, world, player));
        slot.onQuickCraft(slotstack, stack);
      } else if(index >= 10 && (index < 46)) {
        if(!this.moveItemStackTo(slotstack, 46, 46+NUM_OF_STORAGE_SLOTS, false)) return ItemStack.EMPTY;
      } else if((index >= 46) && (index < 46+NUM_OF_STORAGE_SLOTS)) {
        if(!this.moveItemStackTo(slotstack, 10, 46, false)) return ItemStack.EMPTY;
      } else if(!this.moveItemStackTo(slotstack, 10, 46, false)) {
        return ItemStack.EMPTY;
      }
      if(slotstack.isEmpty()) slot.set(ItemStack.EMPTY);
      slot.setChanged();
      if((index != 0) && (slotstack.getCount() == stack.getCount())) return ItemStack.EMPTY;
      slot.onTake(player, slotstack);
      return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
      crafting_matrix_changed_now_ = false;
      super.clicked(slotId, button, clickType, player);
      if((with_outslot_defined_refab) && (slotId == 0) && (clickType == ClickType.PICKUP)) {
        if((!crafting_matrix_changed_now_) && (!player.level.isClientSide()) && (crafting_grid_empty())) {
          final ItemStack dragged = player.inventoryMenu.getCarried();
          if((dragged != null) && (!dragged.isEmpty())) {
            try_result_stack_refab(dragged, player.level);
          } else if(!history().current().isEmpty()) {
            try_result_stack_refab(history().current_recipe().getResultItem(), player.level);
          }
        }
      }
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
    {
      if(nbt.contains("history")) {
        history_.read(nbt.getCompound("history"));
      }
      if(nbt.contains("hascollision")) {
        has_recipe_collision_ = nbt.getBoolean("hascollision");
      }
      if(nbt.contains("inventory")) {
        Inventories.readNbtStacks(nbt, "inventory", inventory_);
        this.slotsChanged(matrix_);
      }
    }

    @Override
    public void onClientPacketReceived(int windowId, Player player, CompoundTag nbt)
    {
      boolean changed = false;
      boolean player_inventory_changed = false;
      if(with_assist && nbt.contains("action")) {
        switch(nbt.getString("action")) {
          case BUTTON_NEXT -> {
            history_.next();
            // implicitly clear the grid, so that the player can see the refab, and that no recipe is active.
            if (crafting_grid_range_.move(block_storage_range_)) changed = true;
            if (crafting_grid_range_.move(player_inventory_range_)) {
              changed = true;
              player_inventory_changed = true;
            }
            sync();
          }
          case BUTTON_PREV -> {
            history_.prev();
            if (crafting_grid_range_.move(block_storage_range_)) changed = true;
            if (crafting_grid_range_.move(player_inventory_range_)) {
              changed = true;
              player_inventory_changed = true;
            }
            sync();
          }
          case BUTTON_CLEAR_GRID -> {
            history_.reset_selection();
            sync();
            if(crafting_grid_range_.move(block_storage_range_)) changed = true;
            if(crafting_grid_range_.move(player_inventory_range_)) {
              changed = true;
              player_inventory_changed = true;
            }
          }
          case ACTION_PLACE_CURRENT_HISTORY_SEL -> {
            if(place_stacks(
              new InventoryRange[]{block_storage_range_, player_storage_range_, player_hotbar_range_},
              refab_crafting_stacks()) != PlacementResult.UNCHANGED) {
              changed = true;
            }
          }
          case ACTION_PLACE_SHIFTCLICKED_STACK -> {
            final int container_slot_id = nbt.getInt("containerslot");
            if((container_slot_id < 10) || (container_slot_id > (46 + NUM_OF_STORAGE_SLOTS))) {
              break; // out of range
            }
            if(container_slot_id >= 46) {
              // from storage
              PlacementResult stat = distribute_stack(block_storage_range_, container_slot_id - 46);
              if (stat != PlacementResult.UNCHANGED) changed = true;
            } else {
              // from player
              int player_slot = (container_slot_id >= 37) ? (container_slot_id - 37) : (container_slot_id - 10 + 9);
              final ItemStack reference_stack = player.getInventory().getItem(player_slot).copy();
              if((!reference_stack.isEmpty()) && (distribute_stack(player.getInventory(), player_slot) != PlacementResult.UNCHANGED)) {
                player_inventory_changed = true;
                changed = true;
                if(nbt.contains("move-all")) {
                  for(int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    final ItemStack stack = player.getInventory().getItem(i);
                    if(!Inventories.areItemStacksIdentical(reference_stack, stack)) continue;
                    if(distribute_stack(player.getInventory(), i) == PlacementResult.UNCHANGED) break; // grid is full
                  }
                }
              }
            }
          }
          case ACTION_MOVE_STACK -> {
            final int container_slot_id = nbt.getInt("containerslot");
            if((container_slot_id < 1) || (container_slot_id >= (46 + NUM_OF_STORAGE_SLOTS))) {
              break; // out of range
            } else if (container_slot_id < 10) {
              ItemStack remaining = Inventories.insert(
                new InventoryRange[]{block_storage_range_, player_storage_range_, player_hotbar_range_},
                inventory_.getItem(container_slot_id - 1)
              );
              changed = player_inventory_changed = (remaining.getCount() != inventory_.getItem(container_slot_id - 1).getCount());
              inventory_.setItem(container_slot_id - 1, remaining);
            }
          }
          case ACTION_MOVE_ALL_STACKS -> {
            final int container_slot_id = nbt.getInt("containerslot");
            if ((container_slot_id < 1) || (container_slot_id >= (46 + NUM_OF_STORAGE_SLOTS))) {
              break; // out of range
            } else if (container_slot_id < 10) {
              // from crafting grid to player inventory, we clear the grid here as this is most likely
              // what is wanted in the end. Saves clicking the other grid stacks.
              if (crafting_grid_range_.move(player_inventory_range_, true)) {
                crafting_grid_range_.move(player_inventory_range_, false, false, true);
                changed = true;
                player_inventory_changed = true;
              }
              if (crafting_grid_range_.move(block_storage_range_)) changed = true;
              if (crafting_grid_range_.move(player_inventory_range_, true)) {
                changed = true;
                player_inventory_changed = true;
              }
              break;
            }
            Container from_inventory;
            InventoryRange[] to_ranges;
            int from_slot;
            if (container_slot_id >= 46) {
              // from storage to player inventory
              from_inventory = inventory_;
              from_slot = container_slot_id - 46 + CRAFTING_SLOTS_SIZE;
              to_ranges = new InventoryRange[]{player_storage_range_, player_hotbar_range_};
            } else {
              // from player to storage (otherwise ACTION_PLACE_SHIFTCLICKED_STACK would have been used)
              from_inventory = player.getInventory();
              from_slot = (container_slot_id >= 37) ? (container_slot_id - 37) : (container_slot_id - 10 + 9);
              to_ranges = new InventoryRange[]{block_storage_range_};
            }
            final ItemStack reference_stack = from_inventory.getItem(from_slot).copy();
            if (!reference_stack.isEmpty()) {
              boolean abort = false;
              for (int i = 0; (i < from_inventory.getContainerSize()) && (!abort); ++i) {
                final ItemStack stack = from_inventory.getItem(i);
                if (Inventories.areItemStacksDifferent(reference_stack, stack)) continue;
                ItemStack remaining = Inventories.insert(to_ranges, from_inventory.getItem(i));
                changed = player_inventory_changed = (remaining.getCount() != from_inventory.getItem(i).getCount());
                from_inventory.setItem(i, remaining);
              }
            }
          }
          case BUTTON_NEXT_COLLISION_RECIPE -> select_next_collision_recipe(inventory_);
          case ACTION_DECREASE_CRAFTING_STACKS -> changed = player_inventory_changed = decrease_grid_stacks(
            new InventoryRange[]{block_storage_range_, player_storage_range_, player_hotbar_range_},
            Mth.clamp(nbt.getInt("limit"), 1, 8));
          case ACTION_INCREASE_CRAFTING_STACKS -> changed = player_inventory_changed = increase_grid_stacks(
            new InventoryRange[]{block_storage_range_, player_storage_range_, player_hotbar_range_},
            Mth.clamp(nbt.getInt("limit"), 1, 8));
        }
      }
      if(changed) inventory_.setChanged();
      if(player_inventory_changed) player.getInventory().setChanged();
      if(changed || player_inventory_changed) {
        this.onCraftMatrixChanged();
        this.broadcastChanges();
      }
    }

    public CraftingHistory history()
    { return history_; }

    private void sync()
    {
      this.wpc_.execute((world,pos)->{
        if(world.isClientSide()) return;
        inventory_.setChanged();
        final CompoundTag nbt = new CompoundTag();
        if(te_ != null) nbt.put("inventory", te_.mainInventory().save(false));
        if(with_assist) {
          CompoundTag hist_nbt = history_.write();
          if(te_ != null) {
            te_.history = hist_nbt.copy();
          }
          nbt.put("history", hist_nbt);
          nbt.putBoolean("hascollision", has_recipe_collision_);
        }
        Networking.PacketContainerSyncServerToClient.sendToListeners(world, this, nbt);
      });
    }

    // private aux methods ---------------------------------------------------------------------

    public boolean has_recipe_collision()
    { return has_recipe_collision_; }

    public void select_next_collision_recipe(Container inv)
    {
      wpc_.execute((world,pos)->{
        if(world.isClientSide) return;
        try {
          ServerPlayer player = (ServerPlayer) player_;
          final List<CraftingRecipe> matching_recipes = world.getServer().getRecipeManager().getRecipesFor(RecipeType.CRAFTING, matrix_, world);
          if(matching_recipes.size() < 2) return; // nothing to change
          Recipe<?> currently_used = result_.getRecipeUsed();
          List<CraftingRecipe> usable_recipes = matching_recipes.stream()
            .filter((r)->result_.setRecipeUsed(world,player,r))
            .sorted(Comparator.comparingInt(a -> a.getId().hashCode()))
            .collect(Collectors.toList());
          for(int i=0; i<usable_recipes.size(); ++i) {
            if(usable_recipes.get(i) == currently_used) {
              if(++i >= usable_recipes.size()) i=0;
              currently_used = usable_recipes.get(i);
              ItemStack stack = ((CraftingRecipe)currently_used).assemble(matrix_);
              result_.setItem(0, stack);
              result_.setRecipeUsed(currently_used);
              break;
            }
          }
          onCraftMatrixChanged();
        } catch(Throwable exc) {
          ModEngineersDecor.logger().error("Recipe failed:", exc);
        }
      });
    }

    @Nullable
    private CraftingRecipe find_first_recipe_for(Level world, ItemStack stack)
    {
      return (CraftingRecipe)world.getServer().getRecipeManager().getRecipes().stream()
        .filter(r->(r.getType()==RecipeType.CRAFTING) && (r.getResultItem().sameItem(stack)))
        .findFirst().orElse(null);
    }

    private Optional<ItemStack> search_inventory(ItemStack match_stack) {
      InventoryRange[] search_ranges = new InventoryRange[]{block_storage_range_, player_storage_range_, player_hotbar_range_};
      for(InventoryRange range: search_ranges) {
        for(int i=0; i<range.getContainerSize(); ++i) {
          if(Inventories.areItemStacksIdentical(range.getItem(i), match_stack)) return Optional.of(match_stack);
        }
      }
      return Optional.empty();
    }

    private Optional<ItemStack> search_inventory(ItemStack[] match_stacks) {
      for(ItemStack match_stack: match_stacks) {
        Optional<ItemStack> stack = search_inventory(match_stack);
        if(stack.isPresent()) return stack;
      }
      return Optional.empty();
    }

    private ArrayList<ItemStack> placement_stacks(CraftingRecipe recipe)
    {
      final Level world = player_.level;
      final ArrayList<ItemStack> grid = new ArrayList<>();
      if(recipe.getIngredients().size() > 9) {
        return grid;
      } else if(recipe instanceof ShapedRecipe) {
        final int endw = ((ShapedRecipe)recipe).getWidth();
        final int endh = ((ShapedRecipe)recipe).getHeight();
        int ingredient_index = 0;
        for(int i=3-endh; i>0; --i) for(int w=0; w<3; ++w) {
          grid.add(ItemStack.EMPTY);
        }
        for(int h=3-endh; h<3; ++h) for(int w=0; w<3; ++w) {
          if((w >= endw) || (ingredient_index >= recipe.getIngredients().size())) { grid.add(ItemStack.EMPTY); continue; }
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index++).getItems();
          if(match_stacks.length == 0) { grid.add(ItemStack.EMPTY); continue; }
          ItemStack preferred = search_inventory(match_stacks).orElse(match_stacks[0]);
          if(preferred.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
          grid.add(preferred);
        }
      } else if(recipe instanceof ShapelessRecipe) {
        // todo: check if a collision resolver with shaped recipes makes sense here.
        for(int ingredient_index=0; ingredient_index<recipe.getIngredients().size(); ++ingredient_index) {
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index).getItems();
          if(match_stacks.length == 0) { grid.add(ItemStack.EMPTY); continue; }
          ItemStack preferred = search_inventory(match_stacks).orElse(match_stacks[0]);
          if(preferred.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
          grid.add(preferred);
        }
        while(grid.size()<9) grid.add(ItemStack.EMPTY);
      }
      return grid;
    }

    private boolean adapt_recipe_placement(CraftingRecipe recipe, List<ItemStack> grid_stacks)
    {
      boolean changed = false;
      final List<Ingredient> ingredients = recipe.getIngredients();
      for(int stack_index=0; stack_index < grid_stacks.size(); ++stack_index) {
        ItemStack to_replace = grid_stacks.get(stack_index);
        ItemStack replacement = to_replace;
        if(to_replace.isEmpty() || (search_inventory(to_replace).isPresent())) continue; // no replacement needed
        for(int ingredient_index=0; ingredient_index<recipe.getIngredients().size(); ++ingredient_index) {
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index).getItems();
          if(Arrays.stream(match_stacks).anyMatch(s->Inventories.areItemStacksIdentical(s, to_replace))) {
            replacement = search_inventory(match_stacks).orElse(to_replace);
            changed = true;
            break;
          }
        }
        grid_stacks.set(stack_index, replacement);
      }
      return changed;
    }

    private void try_result_stack_refab(ItemStack output_stack, Level world)
    {
      CraftingRecipe recipe;
      int history_index = history().find(output_stack);
      if(history_index >= 0) {
        history().selection(history_index);
        recipe = history().current_recipe();
        List<ItemStack> grid_stacks = history().current().subList(1, history().current().size());
        if(adapt_recipe_placement(recipe, grid_stacks)) {
          history().stash(grid_stacks, recipe);
          recipe = history().current_recipe();
        }
      } else if((recipe=find_first_recipe_for(world, output_stack)) != null) {
        ArrayList<ItemStack> stacks = placement_stacks(recipe);
        if(stacks.isEmpty()) {
          recipe = null;
        } else {
          history().stash(stacks, recipe);
          recipe = history().current_recipe();
        }
      }
      if(recipe != null) {
        sync();
        onCraftMatrixChanged();
      }
    }

    private boolean crafting_grid_empty()
    { for(int i=0; i<10; ++i) { if(getSlot(i).hasItem()) return false; } return true; }

    private boolean itemstack_recipe_match(ItemStack grid_stack, ItemStack history_stack)
    {
      if(history_.current_recipe()!=null) {
        final NonNullList<Ingredient> ingredients = history_.current_recipe().getIngredients();
        boolean grid_match, dist_match;
        for(Ingredient ingredient: ingredients) {
          grid_match = false;
          dist_match = false;
          for(final ItemStack match: ingredient.getItems()) {
            if(match.sameItemStackIgnoreDurability(grid_stack)) dist_match = true;
            if(match.sameItemStackIgnoreDurability(history_stack)) grid_match = true;
            if(dist_match && grid_match) return true;
          }
        }
      }
      return false;
    }

    private List<ItemStack> refab_crafting_stacks()
    {
      final ArrayList<ItemStack> slots = new ArrayList<>();
      final List<ItemStack> tocraft = history_.current();
      final int[] stack_sizes = {-1,-1,-1,-1,-1,-1,-1,-1,-1};
      if(tocraft.isEmpty()) return slots;
      for(int i=0; i<9; ++i) {
        if((i+CraftingHistory.INPUT_STACKS_BEGIN) >= tocraft.size()) break;
        final ItemStack needed = tocraft.get(i+CraftingHistory.INPUT_STACKS_BEGIN);
        final ItemStack palced = inventory_.getItem(i+CRAFTING_SLOTS_BEGIN);
        if(needed.isEmpty() && (!palced.isEmpty())) return slots; // history vs grid mismatch.
        if((!palced.isEmpty()) && (!itemstack_recipe_match(needed, palced))) return slots; // also mismatch
        if(!needed.isEmpty()) stack_sizes[i] = palced.getCount();
      }
      int min_placed = 64, max_placed=0;
      for(int i=0; i<9; ++i) {
        if(stack_sizes[i] < 0) continue;
        min_placed = Math.min(min_placed, stack_sizes[i]);
        max_placed = Math.max(max_placed, stack_sizes[i]);
      }
      int fillup_size = (max_placed <= min_placed) ?  (min_placed + 1) : max_placed;
      for(int i=0; i<9; ++i) {
        if(stack_sizes[i] < 0) continue;
        if(fillup_size > inventory_.getItem(i+CRAFTING_SLOTS_BEGIN).getMaxStackSize()) return slots; // can't fillup all
      }
      for(int i=0; i<9; ++i) {
        if(stack_sizes[i] < 0) {
          slots.add(ItemStack.EMPTY);
        } else {
          ItemStack st = inventory_.getItem(i+CRAFTING_SLOTS_BEGIN).copy();
          if(st.isEmpty()) {
            st = tocraft.get(i+CraftingHistory.INPUT_STACKS_BEGIN).copy();
            st.setCount(Math.min(st.getMaxStackSize(), fillup_size));
          } else {
            st.setCount(Mth.clamp(fillup_size-st.getCount(), 0, st.getMaxStackSize()));
          }
          slots.add(st);
        }
      }
      return slots;
    }

    private List<ItemStack> incr_crafting_grid_stacks(int count)
    {
      final ArrayList<ItemStack> stacks = new ArrayList<>();
      for(int i=0; i<9; ++i) {
        final ItemStack palced = crafting_grid_range_.getItem(i).copy();
        if(!palced.isEmpty()) palced.setCount(count);
        stacks.add(palced);
      }
      return stacks;
    }

    private PlacementResult place_stacks(final InventoryRange[] ranges, final List<ItemStack> to_fill)
    {
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      boolean slots_changed = false;
      if(!to_fill.isEmpty()) {
        for(InventoryRange slot_range: ranges) {
          for(int it_guard=63; it_guard>=0; --it_guard) {
            boolean slots_updated = false;
            for(int i = 0; i < 9; ++i) {
              if(to_fill.get(i).isEmpty()) continue;
              ItemStack grid_stack = crafting_grid_range_.getItem(i).copy();
              if(grid_stack.getCount() >= grid_stack.getMaxStackSize()) continue;
              final ItemStack req_stack = to_fill.get(i).copy();
              req_stack.setCount(1);
              final ItemStack mv_stack = slot_range.extract(req_stack);
              if(mv_stack.isEmpty()) continue;
              to_fill.get(i).shrink(1);
              if(grid_stack.isEmpty()) {
                grid_stack = mv_stack.copy();
              } else {
                grid_stack.grow(mv_stack.getCount());
              }
              crafting_grid_range_.setItem(i, grid_stack);
              slots_changed = true;
              slots_updated = true;
            }
            if(!slots_updated) break;
          }
        }
      }
      boolean missing_item = false;
      for(ItemStack st:to_fill) {
        if(!st.isEmpty()) {
          missing_item = true;
          break;
        }
      }
      if(!slots_changed) {
        return PlacementResult.UNCHANGED;
      } else if(missing_item) {
        return PlacementResult.INCOMPLETE;
      } else {
        return PlacementResult.PLACED;
      }
    }

    private PlacementResult distribute_stack(Container inventory, final int slot_index)
    {
      List<ItemStack> to_refab = refab_crafting_stacks();
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      ItemStack to_distribute = inventory.getItem(slot_index).copy();
      if(to_distribute.isEmpty()) return PlacementResult.UNCHANGED;
      int[] matching_grid_stack_sizes = {-1,-1,-1,-1,-1,-1,-1,-1,-1};
      int max_matching_stack_size = -1;
      int min_matching_stack_size = 65;
      int total_num_missing_stacks = 0;
      for(int i=0; i<9; ++i) {
        final ItemStack grid_stack = crafting_grid_range_.getItem(i);
        final ItemStack refab_stack = to_refab.isEmpty() ? ItemStack.EMPTY : to_refab.get(i).copy();
        if((!grid_stack.isEmpty()) && Inventories.areItemStacksIdentical(grid_stack, to_distribute)) {
          matching_grid_stack_sizes[i] = grid_stack.getCount();
          total_num_missing_stacks += grid_stack.getMaxStackSize()-grid_stack.getCount();
          if(max_matching_stack_size < matching_grid_stack_sizes[i]) max_matching_stack_size = matching_grid_stack_sizes[i];
          if(min_matching_stack_size > matching_grid_stack_sizes[i]) min_matching_stack_size = matching_grid_stack_sizes[i];
        } else if((!refab_stack.isEmpty()) && (Inventories.areItemStacksIdentical(refab_stack, to_distribute))) {
          matching_grid_stack_sizes[i] = 0;
          total_num_missing_stacks += grid_stack.getMaxStackSize();
          if(max_matching_stack_size < matching_grid_stack_sizes[i]) max_matching_stack_size = matching_grid_stack_sizes[i];
          if(min_matching_stack_size > matching_grid_stack_sizes[i]) min_matching_stack_size = matching_grid_stack_sizes[i];
        } else if(grid_stack.isEmpty() && (!refab_stack.isEmpty())) {
          if(itemstack_recipe_match(to_distribute, refab_stack)) {
            matching_grid_stack_sizes[i] = 0;
            total_num_missing_stacks += grid_stack.getMaxStackSize();
            if(max_matching_stack_size < matching_grid_stack_sizes[i]) max_matching_stack_size = matching_grid_stack_sizes[i];
            if(min_matching_stack_size > matching_grid_stack_sizes[i]) min_matching_stack_size = matching_grid_stack_sizes[i];
          }
        }
      }
      if(min_matching_stack_size < 0) return PlacementResult.UNCHANGED;
      final int stack_limit_size = Math.min(to_distribute.getMaxStackSize(), crafting_grid_range_.getMaxStackSize());
      if(min_matching_stack_size >= stack_limit_size) return PlacementResult.UNCHANGED;
      int n_to_distribute = to_distribute.getCount();
      for(int it_guard=63; it_guard>=0; --it_guard) {
        if(n_to_distribute <= 0) break;
        for(int i=0; i<9; ++i) {
          if(n_to_distribute <= 0) break;
          if(matching_grid_stack_sizes[i] == min_matching_stack_size) {
            ++matching_grid_stack_sizes[i];
            --n_to_distribute;
          }
        }
        if(min_matching_stack_size < max_matching_stack_size) {
          ++min_matching_stack_size; // distribute short stacks
        } else {
          ++min_matching_stack_size; // stacks even, increase all
          max_matching_stack_size = min_matching_stack_size;
        }
        if(min_matching_stack_size >= stack_limit_size) break; // all full
      }
      if(n_to_distribute == to_distribute.getCount()) return PlacementResult.UNCHANGED; // was already full
      if(n_to_distribute <= 0) {
        inventory.setItem(slot_index, ItemStack.EMPTY);
      } else {
        to_distribute.setCount(n_to_distribute);
        inventory.setItem(slot_index, to_distribute);
      }
      for(int i=0; i<9; ++i) {
        if(matching_grid_stack_sizes[i] < 0) continue;
        ItemStack grid_stack = crafting_grid_range_.getItem(i).copy();
        if(grid_stack.isEmpty()) grid_stack = to_distribute.copy();
        grid_stack.setCount(matching_grid_stack_sizes[i]);
        crafting_grid_range_.setItem(i, grid_stack);
      }
      return PlacementResult.PLACED;
    }

    private boolean decrease_grid_stacks(InventoryRange[] ranges, int limit)
    {
      boolean changed = false;
      for(int i=0; i<9; ++i) {
        ItemStack stack = crafting_grid_range_.getItem(i).copy();
        if(stack.isEmpty()) continue;
        for(InventoryRange range:ranges) {
          ItemStack remaining = range.insert(stack, false, limit, false, false);
          if(remaining.getCount() < stack.getCount()) changed = true;
          boolean stop = (remaining.getCount() <= Math.max(0, (stack.getCount()-limit)));
          stack = remaining;
          if(stop) break;
        }
        crafting_grid_range_.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
      }
      return changed;
    }

    private boolean increase_grid_stacks(InventoryRange[] ranges, int limit)
    { return place_stacks(ranges, incr_crafting_grid_stacks(limit)) != PlacementResult.UNCHANGED; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class CraftingTableGui extends Guis.ContainerGui<CraftingTableUiContainer>
  {
    protected final ArrayList<Button> buttons = new ArrayList<>();
    protected final boolean[] history_slot_tooltip = {false,false,false,false,false,false,false,false,false,false};


    public CraftingTableGui(CraftingTableUiContainer uicontainer, Inventory playerInventory, Component title)
    { super(uicontainer, playerInventory, title, "textures/gui/metal_crafting_table_gui.png", 176, 188); }

    @Override
    public void init()
    {
      super.init();
      final int x0=leftPos, y0=topPos;
      buttons.clear();
      if(with_assist) {
        // @todo: Mod Wrapped ImageButton.
        buttons.add(addRenderableWidget(new ImageButton(x0+158,y0+30, 12,12, 194,44, 12, getBackgroundImage(), (bt)->action(CraftingTableUiContainer.BUTTON_NEXT))));
        buttons.add(addRenderableWidget(new ImageButton(x0+158,y0+16, 12,12, 180,30, 12, getBackgroundImage(), (bt)->action(CraftingTableUiContainer.BUTTON_PREV))));
        buttons.add(addRenderableWidget(new ImageButton(x0+158,y0+44, 12,12, 194,8,  12, getBackgroundImage(), (bt)->action(CraftingTableUiContainer.BUTTON_CLEAR_GRID))));
        buttons.add(addRenderableWidget(new ImageButton(x0+116,y0+10, 20,10, 183,95, 12, getBackgroundImage(), (bt)->action(CraftingTableUiContainer.BUTTON_NEXT_COLLISION_RECIPE))));
      }
      {
        List<TipRange> tooltips = new ArrayList<>();
        final String prefix = ModContent.CRAFTING_TABLE.getDescriptionId() + ".tooltips.";
        String[] translation_keys = { "next", "prev", "clear", "nextcollisionrecipe", "fromstorage", "tostorage", "fromplayer", "toplayer" };
        for(int i=0; (i<buttons.size()) && (i<translation_keys.length); ++i) {
          Button bt = buttons.get(i);
          tooltips.add(new TipRange(bt.x,bt.y, bt.getWidth(), bt.getHeight(), Auxiliaries.localizable(prefix+translation_keys[i])));
        }
        tooltip_.init(tooltips);
      }
    }

    @Override
    public void render(PoseStack mx, int mouseX, int mouseY, float partialTicks)
    {
      if(with_assist) {
        boolean is_collision = getMenu().has_recipe_collision();
        buttons.get(3).visible = is_collision;
        buttons.get(3).active = is_collision;
      }
      super.render(mx, mouseX, mouseY, partialTicks);
    }

    protected void renderHoveredToolTip(PoseStack mx, int mouseX, int mouseY)
    {
      if(!player_.getInventory().items.isEmpty()) return;
      final Slot slot = getSlotUnderMouse();
      if(slot == null) return;
      if(!slot.getItem().isEmpty()) { renderTooltip(mx, slot.getItem(), mouseX, mouseY); return; }
      if(with_assist) {
        int hist_index = -1;
        if(slot instanceof CraftingOutputSlot) {
          hist_index = 0;
        } else if(slot.container instanceof CraftOutputInventory) {
          hist_index = slot.getSlotIndex() + 1;
        }
        if((hist_index < 0) || (hist_index >= history_slot_tooltip.length)) return;
        if(!history_slot_tooltip[hist_index]) return;
        ItemStack hist_stack = getMenu().history().current().get(hist_index);
        if(!hist_stack.isEmpty()) renderTooltip(mx, hist_stack, mouseX, mouseY);
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void renderBgWidgets(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {
      if(with_assist) {
        Arrays.fill(history_slot_tooltip, false);
        final List<ItemStack> crafting_template = getMenu().history().current();
        if((crafting_template == null) || (crafting_template.isEmpty())) return;
        {
          int i = 0;
          for(Tuple<Integer, Integer> e: getMenu().CRAFTING_SLOT_COORDINATES) {
            if(i==0) continue; // explicitly here, that is the result slot.
            if((getMenu().getSlot(i).hasItem())) {
              if(!getMenu().getSlot(i).getItem().sameItem(crafting_template.get(i))) {
                return; // user has placed another recipe
              }
            }
            ++i;
          }
        }
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : getMenu().CRAFTING_SLOT_COORDINATES) {
            final ItemStack stack = crafting_template.get(i);
            if(!stack.isEmpty()) {
              if(!getMenu().getSlot(i).hasItem()) history_slot_tooltip[i] = true;
              if((i==0) && getMenu().getSlot(i).getItem().sameItem(crafting_template.get(i))) {
                continue; // don't shade the output slot if the result can be crafted.
              } else {
                renderItemTemplate(mx, stack, e.getA(), e.getB());
              }
            }
            ++i;
          }
        }
      }
    }

    protected void action(String message)
    { action(message, new CompoundTag()); }

    protected void action(String message, CompoundTag nbt)
    { getMenu().onGuiAction(message, nbt); tooltip_.resetTimer(); }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
    {
      tooltip_.resetTimer();
      if(type == ClickType.PICKUP) {
        boolean place_refab = (slot instanceof CraftingOutputSlot) && (!slot.hasItem());
        if(place_refab && with_assist_direct_history_refab) on_history_item_placement(); // place before crafting -> direct item pick
        super.slotClicked(slot, slotId, mouseButton, type);
        if(place_refab && (!with_assist_direct_history_refab)) on_history_item_placement(); // place after crafting -> confirmation first
        return;
      }
      if((type == ClickType.QUICK_MOVE) && (slotId > 0) && (slot.hasItem())) { // container slots 0 is crafting output
        if(with_assist) {
          List<ItemStack> history = getMenu().history().current();
          boolean palce_in_crafting_grid = false;
          if(slotId > 9) { // container slots 1..9 are crafting grid
            palce_in_crafting_grid = (!history.isEmpty());
            if(!palce_in_crafting_grid) {
              for(int i = 0; i < 9; ++i) {
                if(!Inventories.areItemStacksDifferent(getMenu().getSlot(i).getItem(), slot.getItem())) {
                  palce_in_crafting_grid = true;
                  break;
                }
              }
            }
          }
          if(palce_in_crafting_grid) {
            // Explicit grid placement.
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("containerslot", slotId);
            if(Auxiliaries.isCtrlDown()) nbt.putBoolean("move-all", true);
            action(CraftingTableUiContainer.ACTION_PLACE_SHIFTCLICKED_STACK, nbt);
            return;
          } else if(Auxiliaries.isCtrlDown()) {
            // Move all same items from the inventory of the clicked slot
            // (or the crafting grid) to the corresponding target inventory.
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("containerslot", slotId);
            action(CraftingTableUiContainer.ACTION_MOVE_ALL_STACKS, nbt);
            return;
          } else if((slotId > 0) && (slotId <= 9)) {
            // Move from crafting grid to inventory
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("containerslot", slotId);
            action(CraftingTableUiContainer.ACTION_MOVE_STACK, nbt);
            return;
          } else {
            // Let the normal slot click handle that.
          }
        }
      }
      super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double wheel_inc)
    {
      tooltip_.resetTimer();
      final Slot resultSlot = this.getSlotUnderMouse();
      if((!with_crafting_slot_mouse_scrolling) || (!(resultSlot instanceof CraftingOutputSlot))) {
        return this.getChildAt(mouseX, mouseY).filter((evl)->evl.mouseScrolled(mouseX, mouseY, wheel_inc)).isPresent();
      }
      int count = resultSlot.getItem().getCount();
      int limit = (Auxiliaries.isShiftDown() ? 2 : 1) * (Auxiliaries.isCtrlDown() ? 4 : 1);
      if(wheel_inc > 0.1) {
        if(count > 0) {
          if((count < resultSlot.getItem().getMaxStackSize()) && (count < resultSlot.getMaxStackSize())) {
            CompoundTag nbt = new CompoundTag();
            if(limit > 1) nbt.putInt("limit", limit);
            action(CraftingTableUiContainer.ACTION_INCREASE_CRAFTING_STACKS, nbt);
          }
        } else if(!getMenu().history().current().isEmpty()) {
          action(CraftingTableUiContainer.ACTION_PLACE_CURRENT_HISTORY_SEL);
        }
      } else if(wheel_inc < -0.1) {
        if(count > 0) {
          CompoundTag nbt = new CompoundTag();
          if(limit > 1) nbt.putInt("limit", limit);
          action(CraftingTableUiContainer.ACTION_DECREASE_CRAFTING_STACKS, nbt);
        }
      }
      return true;
    }

    private void on_history_item_placement()
    {
      if((getMenu().history().current().isEmpty())) return;
      final Slot resultSlot = this.getSlotUnderMouse(); // double check
      if(!(resultSlot instanceof CraftingOutputSlot)) return;
      action(CraftingTableUiContainer.ACTION_PLACE_CURRENT_HISTORY_SEL);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Nested auxilliary classes
  //--------------------------------------------------------------------------------------------------------------------

  public enum PlacementResult { UNCHANGED, INCOMPLETE, PLACED }

  // Crafting history --------------------------------------------------------------------------------------------------
  private static class CraftingHistory
  {
    public static final int RESULT_STACK_INDEX = 0;
    public static final int INPUT_STACKS_BEGIN = 1;
    public static final List<ItemStack> NOTHING = new ArrayList<>();
    private static int max_history_size_ = 5;
    private final Level world;
    private final List<String> history_ = new ArrayList<>();
    private String stash_ = "";
    private int current_ = -1;
    private List<ItemStack> current_stacks_ = new ArrayList<>();
    private CraftingRecipe current_recipe_ = null;

    public CraftingHistory(Level world)
    { this.world = world; }

    public static int max_history_size()
    { return max_history_size_; }

    public static int max_history_size(int newsize)
    { return max_history_size_ = Mth.clamp(newsize, 0, 32); }

    public void read(final CompoundTag nbt)
    {
      try {
        clear();
        String s = nbt.getString("elements");
        if((s!=null) && (s.length() > 0)) {
          String[] ls = s.split("[|]");
          for(String e:ls) history_.add(e.toLowerCase().trim());
        }
        current_ = (!nbt.contains("current")) ? (-1) : Mth.clamp(nbt.getInt("current"), -1, history_.size()-1);
        stash_ = nbt.getString("stash");
        update_current();
      } catch(Throwable ex) {
        ModEngineersDecor.logger().error("Exception reading crafting table history NBT, resetting, exception is:" + ex.getMessage());
        clear();
      }
    }

    public CompoundTag write()
    {
      final CompoundTag nbt = new CompoundTag();
      nbt.putInt("current", current_);
      nbt.putString("elements", String.join("|", history_));
      if(!stash_.isEmpty()) nbt.putString("stash", stash_);
      return nbt;
    }

    public void clear()
    { reset_current(); history_.clear(); }

    public void reset_current()
    { current_ = -1; stash_ = ""; current_stacks_ = NOTHING; current_recipe_ = null; }

    void update_current()
    {
      if(!stash_.isEmpty()) {
        Tuple<CraftingRecipe, List<ItemStack>> data = str2stacks(stash_);
        if(data != null) {
          current_recipe_ = data.getA();
          current_stacks_ = data.getB();
        }
      } else if((current_ < 0) || (current_ >= history_.size())) {
        reset_current();
      } else {
        Tuple<CraftingRecipe, List<ItemStack>> data = str2stacks(history_.get(current_));
        if(data == null) { reset_current(); return; }
        current_recipe_ = data.getA();
        current_stacks_ = data.getB();
      }
    }

    public void stash(final List<ItemStack> grid_stacks, CraftingRecipe recipe)
    {
      if(grid_stacks.size() == 9) {
        ArrayList<ItemStack> result_and_stacks = new ArrayList<>();
        result_and_stacks.add(recipe.getResultItem());
        result_and_stacks.addAll(grid_stacks);
        stash_ = stacks2str(result_and_stacks, recipe);
        current_stacks_ = result_and_stacks;
      } else {
        stash_ = stacks2str(grid_stacks, recipe);
        current_stacks_ = grid_stacks;
      }
      current_ = -1;
      current_recipe_ = recipe;
    }

    public int find(ItemStack result)
    {
      for(int i=0; i<history_.size(); ++i) {
        Tuple<CraftingRecipe, List<ItemStack>> data = str2stacks(history_.get(i));
        if((data!=null) && (data.getA().getResultItem().sameItem(result))) return i;
      }
      return -1;
    }

    public void add(final List<ItemStack> grid_stacks, CraftingRecipe recipe)
    {
      if(!with_assist) { clear(); return; }
      stash_ = "";
      String s = stacks2str(grid_stacks, recipe);
      if(s.isEmpty()) return;
      String recipe_filter = "" + recipe.getId() + ";";
      history_.removeIf(e->e.equals(s));
      history_.removeIf(e->e.startsWith(recipe_filter));
      history_.add(s);
      while(history_.size() > max_history_size()) history_.remove(0);
      if(current_ >= history_.size()) reset_current();
    }

    public String stacks2str(final List<ItemStack> grid_stacks, CraftingRecipe recipe)
    {
      if((grid_stacks==null) || (recipe==null)) return "";
      final int num_stacks = grid_stacks.size();
      if((num_stacks < 9) || (num_stacks > 10)) return "";
      final ArrayList<String> items = new ArrayList<>();
      items.add(recipe.getId().toString());
      if(num_stacks < 10) items.add(recipe.getResultItem().getItem().getRegistryName().toString());
      for(ItemStack st:grid_stacks) {
        if(st.isEmpty()) {
          items.add("");
        } else {
          ResourceLocation rl = st.getItem().getRegistryName();
          items.add( rl.getNamespace().equals("minecraft") ? rl.getPath() : rl.toString());
        }
      }
      return String.join(";", items);
    }

    public @Nullable Tuple<CraftingRecipe, List<ItemStack>> str2stacks(final String entry)
    {
      if((world==null) || (entry == null) || (entry.isEmpty())) return null;
      try {
        ArrayList<String> item_regnames = new ArrayList<>(Arrays.asList(entry.split("[;]")));
        if((item_regnames == null) || (item_regnames.size() < 2) || (item_regnames.size() > 11)) return null;
        while(item_regnames.size() < 11) item_regnames.add("");
        final String recipe_name = item_regnames.remove(0);
        List<ItemStack> stacks = new ArrayList<>();
        for(String regname:item_regnames) {
          ItemStack stack = ItemStack.EMPTY;
          if(!regname.isEmpty()) {
            final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(regname));
            stack = ((item == null) || (item == Items.AIR)) ? ItemStack.EMPTY : (new ItemStack(item, 1));
          }
          stacks.add(stack);
        }
        if((stacks.size() != 10) || (stacks.get(0).isEmpty())) return null; // invalid size or no result
        final Recipe<?> recipe = world.getRecipeManager().byKey(new ResourceLocation(recipe_name)).orElse(null);
        if(!(recipe instanceof CraftingRecipe)) return null;
        return new Tuple<>((CraftingRecipe)recipe, stacks);
      } catch(Throwable ex) {
        ModEngineersDecor.logger().error("History stack building failed: " + ex.getMessage());
        return null;
      }
    }

    public List<ItemStack> current()
    { return current_stacks_; }

    public CraftingRecipe current_recipe()
    { return current_recipe_; }

    public void next()
    {
      stash_ = "";
      if(history_.isEmpty()) {
        current_ = -1;
      } else {
        current_ = ((++current_) >= history_.size()) ? (-1) : (current_);
      }
      update_current();
    }

    public void prev()
    {
      stash_ = "";
      if(history_.isEmpty()) {
        current_ = -1;
      } else {
        current_ = ((--current_) < -1) ? (history_.size()-1) : (current_);
      }
      update_current();
    }

    public void reset_selection()
    { current_ = -1; stash_ = ""; update_current(); }

    public void selection(int index)
    { current_ = ((index < 0) || (index >= history_.size())) ? (-1) : (index); update_current(); }

    public int selection()
    { return current_; }

    public int size()
    { return history_.size(); }

    public String toString()
    {
      String rec = (current_recipe_==null) ? "none" : (current_recipe_.getId().toString());
      StringBuilder s = new StringBuilder("{ current:" + current_ + ", recipe:'" + rec + "', elements:[ ");
      for(int i=0; i<history_.size(); ++i) s.append("{i:").append(i).append(", e:[").append(history_.get(i))
        .append("], stash: '").append(stash_).append("'}");
      return s.toString();
    }
  }

  // Crafting Result Inventory of the container ------------------------------------------------------------------------

  public static class CraftOutputInventory extends ResultContainer implements Container, RecipeHolder
  {
    private final Container result_inv_;
    private Recipe<?> recipe_used_;

    public CraftOutputInventory(Container inventory)
    { result_inv_ = inventory; }

    public int getContainerSize()
    { return 1; }

    public boolean isEmpty()
    { return result_inv_.getItem(0).isEmpty(); }

    public ItemStack getItem(int index)
    { return result_inv_.getItem(0); }

    public ItemStack removeItem(int index, int count)
    { return result_inv_.removeItemNoUpdate(0); }

    public ItemStack removeItemNoUpdate(int index)
    { return result_inv_.removeItemNoUpdate(0); }

    public void setItem(int index, ItemStack stack)
    { result_inv_.setItem(0, stack); }

    public void setChanged()
    { result_inv_.setChanged(); }

    public boolean stillValid(Player player)
    { return true; }

    public void clearContent()
    { result_inv_.setItem(0, ItemStack.EMPTY); }

    public void setRecipeUsed(@Nullable Recipe<?> recipe)
    { recipe_used_ = recipe; }

    @Nullable
    public Recipe<?> getRecipeUsed()
    { return recipe_used_; }
  }

  // Crafting output slot of the container -----------------------------------------------------------------------------
  // Has to be re-implemented because CraftingResultSlot is not synchronsized for detectAndSendChanges().
  public static class CraftingOutputSlot extends Slot
  {
    private final CraftingTableUiContainer uicontainer;
    private final Player player;
    private final CraftingContainer craftMatrix;
    private int amountCrafted;

    public CraftingOutputSlot(CraftingTableUiContainer uicontainer, Player player, CraftingContainer craftingInventory, Container resultInventory, int slotIndex, int xPosition, int yPosition)
    {
      super(resultInventory, slotIndex, xPosition, yPosition);
      this.craftMatrix = craftingInventory;
      this.uicontainer = uicontainer;
      this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    { return false; }

    @Override
    public ItemStack remove(int amount)
    {
      if(hasItem()) amountCrafted += Math.min(amount, getItem().getCount());
      return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount)
    { amountCrafted += amount; checkTakeAchievements(stack); }

    @Override
    protected void onSwapCraft(int numItemsCrafted)
    { amountCrafted += numItemsCrafted; }

    @Override
    protected void checkTakeAchievements(ItemStack stack)
    {
      if((with_assist) && ((player.level!=null) && (!(player.level.isClientSide()))) && (!stack.isEmpty())) {
        final Recipe<?> recipe = ((CraftOutputInventory)this.container).getRecipeUsed();
        final ArrayList<ItemStack> grid = new ArrayList<>();
        grid.add(stack);
        for(int i = 0; i<9; ++i) grid.add(uicontainer.inventory_.getItem(i));
        if(recipe instanceof CraftingRecipe) {
          uicontainer.history().add(grid, (CraftingRecipe)recipe);
          uicontainer.history().reset_current();
        }
      }
      // Normal crafting result slot behaviour
      if(amountCrafted > 0) {
        stack.onCraftedBy(this.player.level, this.player, this.amountCrafted);
        BasicEventHooks.firePlayerCraftingEvent(this.player, stack, this.craftMatrix);
      }
      if(uicontainer instanceof RecipeHolder) {
        ((RecipeHolder)uicontainer).awardUsedRecipes(player);
      }
      amountCrafted = 0;
    }

    @Override
    public void onTake(Player taking_player, ItemStack stack) {
      checkTakeAchievements(stack);
      net.minecraftforge.common.ForgeHooks.setCraftingPlayer(taking_player);
      NonNullList<ItemStack> stacks = taking_player.level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, craftMatrix, taking_player.level);
      net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
      for(int i=0; i<stacks.size(); ++i) {
        ItemStack itemstack = craftMatrix.getItem(i);
        ItemStack itemstack1 = stacks.get(i);
        if(!itemstack.isEmpty()) {
          craftMatrix.removeItem(i, 1);
          itemstack = craftMatrix.getItem(i);
        }
        if(!itemstack1.isEmpty()) {
          if(itemstack.isEmpty()) {
            craftMatrix.setItem(i, itemstack1);
          } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
            itemstack1.grow(itemstack.getCount());
            craftMatrix.setItem(i, itemstack1);
          } else if (!player.getInventory().add(itemstack1)) {
            player.drop(itemstack1, false);
          }
        }
      }
      uicontainer.onCraftMatrixChanged();
    }
  }

  // Crafting grid slot of the container -------------------------------------------------------------------------------
  public static class CraftingGridSlot extends Slot
  {
    public CraftingGridSlot(Container inv, int index, int x, int y)
    { super(inv, index, x, y); }
  }

  // Crafting inventory (needed to allow SlotCrafting to have a InventoryCrafting) -------------------------------------
  public static class CraftingTableGrid extends CraftingContainer
  {
    protected final AbstractContainerMenu uicontainer;
    protected final Container inventory;

    public CraftingTableGrid(AbstractContainerMenu container_, Container block_inventory)
    { super(container_, 3, 3); uicontainer = container_; inventory = block_inventory; }

    @Override
    public int getContainerSize()
    { return 9; }

    @Override
    public void startOpen(Player player)
    { inventory.startOpen(player); }

    @Override
    public void stopOpen(Player player)
    { inventory.stopOpen(player); }

    @Override
    public void setChanged()
    { inventory.setChanged(); }

    @Override
    public void setItem(int index, ItemStack stack)
    {
      inventory.setItem(index, stack);
      uicontainer.slotsChanged(this);
    }

    @Override
    public ItemStack getItem(int index)
    { return inventory.getItem(index); }

    @Override
    public ItemStack removeItem(int index, int count)
    {
      final ItemStack stack = inventory.removeItem(index, count);
      if(!stack.isEmpty()) uicontainer.slotsChanged(this);
      return stack;
    }
  }

}
