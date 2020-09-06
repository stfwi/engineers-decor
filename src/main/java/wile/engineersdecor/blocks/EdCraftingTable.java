/*
 * @file EdCraftingTable.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Crafting table
 */
package wile.engineersdecor.blocks;

import net.minecraft.inventory.container.*;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.world.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.inventory.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.systems.RenderSystem;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Inventories.SlotRange;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.TooltipDisplay;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;


public class EdCraftingTable
{
  public static boolean with_assist = true;
  public static boolean with_assist_direct_history_refab = false;
  public static boolean with_crafting_slot_mouse_scrolling = true;
  public static boolean with_outslot_defined_refab = true;

  public static final void on_config(boolean without_crafting_assist, boolean with_assist_immediate_history_refab,
                                     boolean without_crafting_slot_mouse_scrolling)
  {
    with_assist = !without_crafting_assist;
    with_assist_direct_history_refab = with_assist_immediate_history_refab;
    with_crafting_slot_mouse_scrolling = !without_crafting_slot_mouse_scrolling;
    with_outslot_defined_refab = with_assist;
    CraftingHistory.max_history_size(32);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static final class CraftingTableBlock extends DecorBlock.HorizontalWaterLoggable implements IDecorBlock
  {
    public CraftingTableBlock(long config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdCraftingTable.CraftingTableTileEntity(); }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return ActionResultType.SUCCESS;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof CraftingTableTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      return ActionResultType.SUCCESS;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isRemote) return;
      if(!stack.hasTag()) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundNBT inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof CraftingTableTileEntity)) return;
      ((CraftingTableTileEntity)te).readnbt(inventory_nbt);
      ((CraftingTableTileEntity)te).markDirty();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isRemote) return stacks;
      if(!(te instanceof CraftingTableTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT inventory_nbt = new CompoundNBT();
        ItemStackHelper.saveAllItems(inventory_nbt, ((CraftingTableTileEntity)te).stacks, false);
        if(!inventory_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("inventory", inventory_nbt);
          stack.setTag(nbt);
        }
        ((CraftingTableTileEntity) te).clear();
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((CraftingTableTileEntity)te).stacks) {
          if(!stack.isEmpty()) stacks.add(stack);
        }
        ((CraftingTableTileEntity)te).reset();
      }
      return stacks;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class CraftingTableTileEntity extends TileEntity implements IInventory, INameable, INamedContainerProvider
  {
    public static final int NUM_OF_SLOTS = 9+18;
    protected NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
    protected CompoundNBT history = new CompoundNBT();

    public CraftingTableTileEntity()
    { this(ModContent.TET_CRAFTING_TABLE); }

    public CraftingTableTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void reset()
    { stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void readnbt(CompoundNBT nbt)
    {
      reset();
      ItemStackHelper.loadAllItems(nbt, this.stacks);
      while(this.stacks.size() < NUM_OF_SLOTS) this.stacks.add(ItemStack.EMPTY);
      history = nbt.getCompound("history");
    }

    private void writenbt(CompoundNBT nbt)
    {
      ItemStackHelper.saveAllItems(nbt, this.stacks);
      if(!history.isEmpty()) nbt.put("history", history);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    { super.read(state, nbt); readnbt(nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt); return nbt; }

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
      //@todo: check if that is still needed: super.read(pkt.getNbtCompound());
      readnbt(pkt.getNbtCompound());
      super.onDataPacket(net, pkt);
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) // on client
    { read(state, tag); }

    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistanceSquared()
    { return 400; }

    // INameable ---------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Treated wood crafting table"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public ITextComponent getCustomName()
    { return getName(); }

    // INamedContainerProvider ------------------------------------------------------------------------------

    @Override
    public ITextComponent getDisplayName()
    { return INameable.super.getDisplayName(); }

    @Override
    public Container createMenu( int id, PlayerInventory inventory, PlayerEntity player )
    { return new CraftingTableContainer(id, inventory, this, IWorldPosCallable.of(world, pos)); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public int getSizeInventory()
    { return stacks.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return (index < getSizeInventory()) ? stacks.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    { stacks.set(index, stack); }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player)
    { return getPos().distanceSq(player.getPosition()) < 36; }

    @Override
    public void openInventory(PlayerEntity player)
    {}

    @Override
    public void closeInventory(PlayerEntity player)
    {
      markDirty();
      if(world instanceof World) {
        BlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 1|2);
      }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return true; }

    @Override
    public void clear()
    { stacks.clear(); }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting container
  //--------------------------------------------------------------------------------------------------------------------

  public static class CraftingTableContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    protected static final String BUTTON_NEXT = "next";
    protected static final String BUTTON_PREV = "prev";
    protected static final String BUTTON_CLEAR_GRID = "clear";
    protected static final String BUTTON_NEXT_COLLISION_RECIPE = "next-recipe";
    protected static final String ACTION_PLACE_CURRENT_HISTORY_SEL = "place-refab";
    protected static final String ACTION_PLACE_SHIFTCLICKED_STACK = "place-stack";
    protected static final String ACTION_MOVE_ALL_STACKS = "move-stacks";
    protected static final String ACTION_INCREASE_CRAFTING_STACKS = "inc-crafting-stacks";
    protected static final String ACTION_DECREASE_CRAFTING_STACKS = "dec-crafting-stacks";

    public static final int CRAFTING_SLOTS_BEGIN = 0;
    public static final int NUM_OF_CRAFTING_SLOTS = 9;
    public static final int STORAGE_SLOTS_BEGIN = NUM_OF_CRAFTING_SLOTS;
    public static final int NUM_OF_STORAGE_SLOTS = CraftingTableTileEntity.NUM_OF_SLOTS - NUM_OF_CRAFTING_SLOTS;

    public final ImmutableList<Tuple<Integer,Integer>> CRAFTING_SLOT_COORDINATES;
    private final PlayerEntity player_;
    private final IInventory inventory_;
    private final IWorldPosCallable wpc_;
    private final CraftingHistory history_;
    private final CraftingTableGrid matrix_;
    private final CraftResultInventory result_;
    private boolean has_recipe_collision_;
    private boolean crafting_matrix_changed_now_;

    public CraftingTableContainer(int cid, PlayerInventory pinv)
    { this(cid, pinv, new Inventory(CraftingTableTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY); }

    private CraftingTableContainer(int cid, PlayerInventory pinv, IInventory block_inventory, IWorldPosCallable wpc)
    {
      super(ModContent.CT_TREATED_WOOD_CRAFTING_TABLE, cid);
      wpc_ = wpc;
      player_ = pinv.player;
      inventory_ = block_inventory;
      World world = player_.world;
      if(world.isRemote && (inventory_ instanceof CraftingTableTileEntity)) world = ((CraftingTableTileEntity)inventory_).getWorld();
      history_ = new CraftingHistory(world);
      result_ = new CraftResultInventory();
      matrix_ = new CraftingTableGrid(this, block_inventory);
      matrix_.openInventory(player_);
      // container slotId 0 === crafting output
      addSlot(new CraftingOutputSlot(this, pinv.player, matrix_, result_, 0, 118, 27));
      ArrayList<Tuple<Integer,Integer>> slotpositions = new ArrayList<Tuple<Integer,Integer>>();
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
      // container slotId 46..53 === TE slots 9..17 (storage)
      for(int y=0; y<2; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(matrix_, 9+x+y*9, 8+x*18, 65+y*18));
        }
      }
      if((!player_.world.isRemote) && (inventory_ instanceof CraftingTableTileEntity)) {
        history_.read(((CraftingTableTileEntity)inventory_).history.copy());
        syncHistory();
      }
      CRAFTING_SLOT_COORDINATES = ImmutableList.copyOf(slotpositions);
      onCraftMatrixChanged(matrix_);
    }

    @Override
    public boolean canInteractWith(PlayerEntity player)
    { return inventory_.isUsableByPlayer(player); }

    @Override
    public void onCraftMatrixChanged(IInventory inv)
    {
      detectAndSendChanges();
      wpc_.consume((world,pos)->{
        if(world.isRemote) return;
        try {
          crafting_matrix_changed_now_ = true;
          ServerPlayerEntity player = (ServerPlayerEntity) player_;
          ItemStack stack = ItemStack.EMPTY;
          List<ICraftingRecipe> recipes = world.getServer().getRecipeManager().getRecipes(IRecipeType.CRAFTING, matrix_, world);
          has_recipe_collision_ = false;
          if(recipes.size() > 0) {
            ICraftingRecipe recipe = recipes.get(0);
            IRecipe<?> currently_used = result_.getRecipeUsed();
            has_recipe_collision_ = (recipes.size() > 1);
            if((recipes.size() > 1) && (currently_used instanceof ICraftingRecipe) && (recipes.contains(currently_used))) {
              recipe = (ICraftingRecipe)currently_used;
            }
            if(result_.canUseRecipe(world, player, recipe)) {
              stack = recipe.getCraftingResult(matrix_);
            }
          }
          result_.setInventorySlotContents(0, stack);
          player.connection.sendPacket(new SSetSlotPacket(windowId, 0, stack));
          syncProperties();
        } catch(Throwable exc) {
          ModEngineersDecor.logger().error("Recipe failed:", exc);
        }
      });
    }

    @Override
    public void onContainerClosed(PlayerEntity player)
    {
      matrix_.closeInventory(player);
      result_.clear();
      result_.closeInventory(player);
      if(player!=null) {
        for(Slot e:player.container.inventorySlots) {
          if(e instanceof CraftingResultSlot) {
            ((CraftingResultSlot)e).putStack(ItemStack.EMPTY);
          }
        }
      }
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot)
    { return (slot.inventory != result_) && (super.canMergeSlot(stack, slot)); }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot == null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slotstack = slot.getStack();
      ItemStack stack = slotstack.copy();
      if(index == 0) {
        wpc_.consume((world, pos)->slotstack.getItem().onCreated(slotstack, world, player));
        if(!this.mergeItemStack(slotstack, 10, 46, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slotstack, stack);
      } else if(index >= 10 && (index < 46)) {
        if(!this.mergeItemStack(slotstack, 46, 46+NUM_OF_STORAGE_SLOTS, false)) return ItemStack.EMPTY;
      } else if((index >= 46) && (index < 46+NUM_OF_STORAGE_SLOTS)) {
        if(!this.mergeItemStack(slotstack, 10, 46, false)) return ItemStack.EMPTY;
      } else if(!this.mergeItemStack(slotstack, 10, 46, false)) {
        return ItemStack.EMPTY;
      }
      if(slotstack.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }
      if(slotstack.getCount() == stack.getCount()) {
        return ItemStack.EMPTY;
      }
      ItemStack itemstack2 = slot.onTake(player, slotstack);
      if(index == 0) {
        player.dropItem(itemstack2, false);
      }
      return stack;
    }

    @Override
    public ItemStack slotClick(int slotId, int button, ClickType clickType, PlayerEntity player)
    {
      crafting_matrix_changed_now_ = false;
      final ItemStack stack = super.slotClick(slotId, button, clickType, player);
      if((with_outslot_defined_refab) && (slotId == 0) && (clickType == ClickType.PICKUP)) {
        if((!crafting_matrix_changed_now_) && (!player.world.isRemote()) && (crafting_grid_empty())) {
          final ItemStack dragged = player.inventory.getItemStack();
          if((dragged != null) && (!dragged.isEmpty())) {
            try_result_stack_refab(dragged, player.world);
          } else if(!history().current().isEmpty()) {
            try_result_stack_refab(history().current_recipe().getRecipeOutput(), player.world);
          }
        }
      }
      return stack;
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
    {
      if(nbt.contains("history"))  history_.read(nbt.getCompound("history"));
      if(nbt.contains("hascollision")) has_recipe_collision_ = nbt.getBoolean("hascollision");
    }

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      boolean changed = false;
      boolean player_inventory_changed = false;
      if(with_assist && nbt.contains("action")) {
        switch(nbt.getString("action")) {
          case BUTTON_NEXT: {
            history_.next();
            syncHistory();
            // implicitly clear the grid, so that the player can see the refab, and that no recipe is active.
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BUTTON_PREV: {
            history_.prev();
            syncHistory();
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BUTTON_CLEAR_GRID: {
            history_.reset_selection();
            syncHistory();
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case ACTION_PLACE_CURRENT_HISTORY_SEL: {
            if(place_stacks(new SlotRange[]{
              new SlotRange(player.inventory, 0, 9),
              new SlotRange(player.inventory, 9, 36),
              new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)
            }, refab_crafting_stacks()) != PlacementResult.UNCHANGED) {
              changed = true;
            }
          } break;
          case ACTION_PLACE_SHIFTCLICKED_STACK: {
            final int container_slot_id = nbt.getInt("containerslot");
            if((container_slot_id < 10) || (container_slot_id > 53)) {
              break; // out of range
            }
            if(container_slot_id >= 46) {
              // from storage
              final int storage_slot = container_slot_id - 46 + STORAGE_SLOTS_BEGIN;
              PlacementResult stat = distribute_stack(inventory_, storage_slot);
              if(stat != PlacementResult.UNCHANGED) changed = true;
            } else {
              // from player
              int player_slot = (container_slot_id >= 37) ? (container_slot_id-37) : (container_slot_id-10+9);
              final ItemStack reference_stack = player.inventory.getStackInSlot(player_slot).copy();
              if((!reference_stack.isEmpty()) && (distribute_stack(player.inventory, player_slot) != PlacementResult.UNCHANGED)) {
                player_inventory_changed = true;
                changed = true;
                if(nbt.contains("move-all")) {
                  for(int i=0; i < player.inventory.getSizeInventory(); ++i) {
                    final ItemStack stack = player.inventory.getStackInSlot(i);
                    if(!Inventories.areItemStacksIdentical(reference_stack, stack)) continue;
                    if(distribute_stack(player.inventory, i) == PlacementResult.UNCHANGED) break; // grid is full
                  }
                }
              }
            }
          } break;
          case ACTION_MOVE_ALL_STACKS: {
            final int container_slot_id = nbt.getInt("containerslot");
            if((container_slot_id < 1) || (container_slot_id >= (46+NUM_OF_STORAGE_SLOTS))) {
              break; // out of range
            } else if(container_slot_id < 10) {
              // from crafting grid to player inventory, we clear the grid here as this is most likely
              // what is wanted in the end. Saves clicking the other grid stacks.
              if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
              if(clear_grid_to_storage(player)) changed = true;
              break;
            }
            IInventory from_inventory;
            SlotRange[] to_ranges;
            int from_slot;
            if(container_slot_id >= 46) {
              // from storage to player inventory
              from_inventory = inventory_;
              from_slot = container_slot_id - 46 + STORAGE_SLOTS_BEGIN;
              to_ranges = new SlotRange[] {new SlotRange(player.inventory, 9, 36), new SlotRange(player.inventory, 0, 9)};
            } else {
              // from player to storage (otherwise ACTION_PLACE_SHIFTCLICKED_STACK would have been used)
              from_inventory = player.inventory;
              from_slot = (container_slot_id >= 37) ? (container_slot_id-37) : (container_slot_id-10+9);
              to_ranges = new SlotRange[] {new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)};
            }
            final ItemStack reference_stack = from_inventory.getStackInSlot(from_slot).copy();
            if(!reference_stack.isEmpty()) {
              boolean abort = false;
              for(int i=0; (i < from_inventory.getSizeInventory()) && (!abort); ++i) {
                final ItemStack stack = from_inventory.getStackInSlot(i);
                if(Inventories.areItemStacksDifferent(reference_stack, stack)) continue;
                ItemStack remaining = from_inventory.getStackInSlot(i);
                for(SlotRange range:to_ranges) {
                  remaining = range.insert(remaining, false, 0, false, true);
                  if(!remaining.isEmpty()) {
                    abort = true; // no space left
                    break;
                  } else {
                    changed = player_inventory_changed = true;
                  }
                }
                from_inventory.setInventorySlotContents(i, remaining);
              }
            }
          } break;
          case BUTTON_NEXT_COLLISION_RECIPE: {
            select_next_collision_recipe(inventory_);
          } break;
          case ACTION_DECREASE_CRAFTING_STACKS: {
            changed = player_inventory_changed = decrease_grid_stacks(new SlotRange[]{
              new SlotRange(player.inventory, 9, 36),
              new SlotRange(player.inventory, 0, 9),
              new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)
            }, MathHelper.clamp(nbt.getInt("limit"), 1, 8));
          } break;
          case ACTION_INCREASE_CRAFTING_STACKS: {
            changed = player_inventory_changed = increase_grid_stacks(new SlotRange[]{
              new SlotRange(player.inventory, 9, 36),
              new SlotRange(player.inventory, 0, 9),
              new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)
            }, MathHelper.clamp(nbt.getInt("limit"), 1, 8));
          } break;
        }
      }
      if(changed) inventory_.markDirty();
      if(player_inventory_changed) player.inventory.markDirty();
      if(changed || player_inventory_changed) {
        this.onCraftMatrixChanged(inventory_);
        this.detectAndSendChanges();
      }
    }

    public CraftingHistory history()
    { return history_; }

    private void syncHistory()
    {
      if(!with_assist) return;
      this.wpc_.consume((world,pos)->{
        if(world.isRemote()) return;
        CompoundNBT hist_nbt = history_.write();
        if((inventory_ instanceof CraftingTableTileEntity)) {
          ((CraftingTableTileEntity)inventory_).history = hist_nbt.copy();
          inventory_.markDirty();
        }
        final CompoundNBT nbt = new CompoundNBT();
        nbt.put("history", hist_nbt);
        nbt.putBoolean("hascollision", has_recipe_collision_);
        Networking.PacketContainerSyncServerToClient.sendToListeners(world, this, nbt);
      });
    }

    private void syncProperties()
    {
      this.wpc_.consume((world,pos)->{
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putBoolean("hascollision", has_recipe_collision_);
        Networking.PacketContainerSyncServerToClient.sendToListeners(world, this, nbt);
      });
    }

    // private aux methods ---------------------------------------------------------------------

    public boolean has_recipe_collision()
    { return has_recipe_collision_; }

    public void select_next_collision_recipe(IInventory inv)
    {
      wpc_.consume((world,pos)->{
        if(world.isRemote) return;
        try {
          ServerPlayerEntity player = (ServerPlayerEntity) player_;
          final List<ICraftingRecipe> matching_recipes = world.getServer().getRecipeManager().getRecipes(IRecipeType.CRAFTING, matrix_, world);
          if(matching_recipes.size() < 2) return; // nothing to change
          IRecipe<?> currently_used = result_.getRecipeUsed();
          List<ICraftingRecipe> usable_recipes = matching_recipes.stream()
            .filter((r)->result_.canUseRecipe(world,player,r))
            .sorted((a,b)->Integer.compare(a.getId().hashCode(), b.getId().hashCode()))
            .collect(Collectors.toList());
          for(int i=0; i<usable_recipes.size(); ++i) {
            if(usable_recipes.get(i) == currently_used) {
              if(++i >= usable_recipes.size()) i=0;
              currently_used = usable_recipes.get(i);
              ItemStack stack = ((ICraftingRecipe)currently_used).getCraftingResult(matrix_);
              result_.setInventorySlotContents(0, stack);
              result_.setRecipeUsed(currently_used);
              break;
            }
          }
          onCraftMatrixChanged(inv);
        } catch(Throwable exc) {
          ModEngineersDecor.logger().error("Recipe failed:", exc);
        }
      });
    }

    @Nullable
    private ICraftingRecipe find_first_recipe_for(World world, ItemStack stack)
    {
      return (ICraftingRecipe)world.getServer().getRecipeManager().getRecipes().stream()
        .filter(r->(r.getType()==IRecipeType.CRAFTING) && (r.getRecipeOutput().isItemEqual(stack)))
        .findFirst().orElse(null);
    }

    private ItemStack search_inventory(ItemStack match_stack, ItemStack not_found_value) {
      SlotRange search_ranges[] = new SlotRange[]{
        new SlotRange(player_.inventory, 0, 36),
        new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)
      };
      for(SlotRange range: search_ranges) {
        for(int i=0; i<range.inventory.getSizeInventory(); ++i) {
          ItemStack stack = range.inventory.getStackInSlot(i);
          if(Inventories.areItemStacksIdentical(stack, match_stack)) return match_stack;
        }
      }
      return not_found_value;
    }

    private ItemStack search_inventory(ItemStack[] match_stacks, ItemStack not_found_value) {
      for(ItemStack match_stack: match_stacks) {
        ItemStack stack = search_inventory(match_stack, ItemStack.EMPTY);
        if(!stack.isEmpty()) return stack;
      }
      return not_found_value;
    }

    private ArrayList<ItemStack> placement_stacks(ICraftingRecipe recipe)
    {
      final World world = player_.world;
      final ArrayList<ItemStack> grid = new ArrayList<ItemStack>();
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
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index++).getMatchingStacks();
          if(match_stacks.length == 0) { grid.add(ItemStack.EMPTY); continue; }
          ItemStack preferred = search_inventory(match_stacks, match_stacks[0]);
          if(preferred.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
          grid.add(preferred);
        }
      } else if(recipe instanceof ShapelessRecipe) {
        // todo: check if a collision resolver with shaped recipes makes sense here.
        for(int ingredient_index=0; ingredient_index<recipe.getIngredients().size(); ++ingredient_index) {
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index).getMatchingStacks();
          if(match_stacks.length == 0) { grid.add(ItemStack.EMPTY); continue; }
          ItemStack preferred = search_inventory(match_stacks, match_stacks[0]);
          if(preferred.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
          grid.add(preferred);
        }
        while(grid.size()<9) grid.add(ItemStack.EMPTY);
      }
      return grid;
    }

    private boolean adapt_recipe_placement(ICraftingRecipe recipe, List<ItemStack> grid_stacks)
    {
      boolean changed = false;
      final List<Ingredient> ingredients = recipe.getIngredients();
      for(int stack_index=0; stack_index < grid_stacks.size(); ++stack_index) {
        ItemStack to_replace = grid_stacks.get(stack_index);
        ItemStack replacement = to_replace;
        if(to_replace.isEmpty() || (!search_inventory(to_replace, ItemStack.EMPTY).isEmpty())) continue; // no replacement needed
        for(int ingredient_index=0; ingredient_index<recipe.getIngredients().size(); ++ingredient_index) {
          ItemStack[] match_stacks = recipe.getIngredients().get(ingredient_index).getMatchingStacks();
          if(Arrays.stream(match_stacks).anyMatch(s->Inventories.areItemStacksIdentical(s, to_replace))) {
            replacement = search_inventory(match_stacks, to_replace);
            changed = true;
            break;
          }
        }
        grid_stacks.set(stack_index, replacement);
      }
      return changed;
    }

    private void try_result_stack_refab(ItemStack output_stack, World world)
    {
      ICraftingRecipe recipe;
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
        onCraftMatrixChanged(inventory_);
        syncHistory();
      }
    }

    private boolean crafting_grid_empty()
    { for(int i=0; i<10; ++i) { if(getSlot(i).getHasStack()) return false; } return true; }

    private boolean itemstack_recipe_match(ItemStack grid_stack, ItemStack history_stack)
    {
      if(history_.current_recipe()!=null) {
        final NonNullList<Ingredient> ingredients = history_.current_recipe().getIngredients();
        boolean grid_match, dist_match;
        for(int i=0; i<ingredients.size(); ++i) {
          Ingredient ingredient = ingredients.get(i);
          grid_match = false; dist_match = false;
          for(final ItemStack match:ingredient.getMatchingStacks()) {
            if(match.isItemEqualIgnoreDurability(grid_stack)) dist_match = true;
            if(match.isItemEqualIgnoreDurability(history_stack)) grid_match = true;
            if(dist_match && grid_match) return true;
          }
        }
      }
      return false;
    }

    private List<ItemStack> refab_crafting_stacks()
    {
      final ArrayList<ItemStack> slots = new ArrayList<ItemStack>();
      final List<ItemStack> tocraft = history_.current();
      final int stack_sizes[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1};
      if(tocraft.isEmpty()) return slots;
      for(int i=0; i<9; ++i) {
        if((i+CraftingHistory.INPUT_STACKS_BEGIN) >= tocraft.size()) break;
        final ItemStack needed = tocraft.get(i+CraftingHistory.INPUT_STACKS_BEGIN);
        final ItemStack palced = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN);
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
        if(fillup_size > inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN).getMaxStackSize()) return slots; // can't fillup all
      }
      for(int i=0; i<9; ++i) {
        if(stack_sizes[i] < 0) {
          slots.add(ItemStack.EMPTY);
        } else {
          ItemStack st = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN).copy();
          if(st.isEmpty()) {
            st = tocraft.get(i+CraftingHistory.INPUT_STACKS_BEGIN).copy();
            st.setCount(Math.min(st.getMaxStackSize(), fillup_size));
          } else {
            st.setCount(MathHelper.clamp(fillup_size-st.getCount(), 0, st.getMaxStackSize()));
          }
          slots.add(st);
        }
      }
      return slots;
    }

    private List<ItemStack> incr_crafting_grid_stacks(int count)
    {
      final ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
      for(int i=0; i<9; ++i) {
        final ItemStack palced = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN).copy();
        if(!palced.isEmpty()) palced.setCount(count);
        stacks.add(palced);
      }
      return stacks;
    }

    private boolean clear_grid_to_storage(PlayerEntity player)
    {
      boolean changed = false;
      for(int grid_i = CRAFTING_SLOTS_BEGIN; grid_i < (CRAFTING_SLOTS_BEGIN+NUM_OF_CRAFTING_SLOTS); ++grid_i) {
        ItemStack stack = inventory_.getStackInSlot(grid_i);
        if(stack.isEmpty()) continue;
        ItemStack remaining = (new SlotRange(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS)).insert(stack, false, 0);
        inventory_.setInventorySlotContents(grid_i, remaining);
        changed = true;
      }
      return changed;
    }

    private boolean clear_grid_to_player(PlayerEntity player)
    {
      boolean changed = false;
      for(int grid_i = CRAFTING_SLOTS_BEGIN; grid_i < (CRAFTING_SLOTS_BEGIN+NUM_OF_CRAFTING_SLOTS); ++grid_i) {
        ItemStack remaining = inventory_.getStackInSlot(grid_i);
        if(remaining.isEmpty()) continue;
        remaining = (new SlotRange(player.inventory,9, 36)).insert(remaining,true, 0); // prefer filling up inventory stacks
        remaining = (new SlotRange(player.inventory,0, 9)).insert(remaining, true, 0);  // then fill up the hotbar stacks
        remaining = (new SlotRange(player.inventory,9, 36)).insert(remaining, false, 0); // then allow empty stacks in inventory
        remaining = (new SlotRange(player.inventory,0, 9)).insert(remaining, false, 0);  // then new stacks in the hotbar
        inventory_.setInventorySlotContents(grid_i, remaining);
        changed = true;
      }
      return changed;
    }

    private PlacementResult place_stacks(final SlotRange[] ranges, final List<ItemStack> to_fill)
    {
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      boolean slots_changed = false;
      if(!to_fill.isEmpty()) {
        for(SlotRange slot_range: ranges) {
          for(int it_guard=63; it_guard>=0; --it_guard) {
            boolean slots_updated = false;
            for(int i = 0; i < 9; ++i) {
              if(to_fill.get(i).isEmpty()) continue;
              ItemStack grid_stack = inventory_.getStackInSlot(i + CRAFTING_SLOTS_BEGIN).copy();
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
              inventory_.setInventorySlotContents(i + CRAFTING_SLOTS_BEGIN, grid_stack);
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

    private PlacementResult distribute_stack(IInventory inventory, final int slotno)
    {
      List<ItemStack> to_refab = refab_crafting_stacks();
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      ItemStack to_distribute = inventory.getStackInSlot(slotno).copy();
      if(to_distribute.isEmpty()) return PlacementResult.UNCHANGED;
      int matching_grid_stack_sizes[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1};
      int max_matching_stack_size = -1;
      int min_matching_stack_size = 65;
      int total_num_missing_stacks = 0;
      for(int i=0; i<9; ++i) {
        final ItemStack grid_stack = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN);
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
      final int stack_limit_size = Math.min(to_distribute.getMaxStackSize(), inventory_.getInventoryStackLimit());
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
        inventory.setInventorySlotContents(slotno, ItemStack.EMPTY);
      } else {
        to_distribute.setCount(n_to_distribute);
        inventory.setInventorySlotContents(slotno, to_distribute);
      }
      for(int i=0; i<9; ++i) {
        if(matching_grid_stack_sizes[i] < 0) continue;
        ItemStack grid_stack = inventory_.getStackInSlot(i + CRAFTING_SLOTS_BEGIN).copy();
        if(grid_stack.isEmpty()) grid_stack = to_distribute.copy();
        grid_stack.setCount(matching_grid_stack_sizes[i]);
        inventory_.setInventorySlotContents(i + CRAFTING_SLOTS_BEGIN, grid_stack);
      }
      return PlacementResult.PLACED;
    }

    private boolean decrease_grid_stacks(SlotRange[] ranges, int limit)
    {
      boolean changed = false;
      for(int i=0; i<9; ++i) {
        ItemStack stack = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN).copy();
        if(stack.isEmpty()) continue;
        for(SlotRange range:ranges) {
          ItemStack remaining = range.insert(stack, false, limit);
          if(remaining.getCount() < stack.getCount()) changed = true;
          boolean stop = (remaining.getCount() <= Math.max(0, (stack.getCount()-limit)));
          stack = remaining;
          if(stop) break;
        }
        inventory_.setInventorySlotContents(i+CRAFTING_SLOTS_BEGIN, stack.isEmpty() ? ItemStack.EMPTY : stack);
      }
      return changed;
    }

    private boolean increase_grid_stacks(SlotRange[] ranges, int limit)
    { return place_stacks(ranges, incr_crafting_grid_stacks(limit)) != PlacementResult.UNCHANGED; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class CraftingTableGui extends ContainerScreen<CraftingTableContainer>
  {
    protected static final ResourceLocation BACKGROUND = new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/metal_crafting_table_gui.png");
    protected final PlayerEntity player;
    protected final ArrayList<Button> buttons = new ArrayList<Button>();
    protected final boolean history_slot_tooltip[] = {false,false,false,false,false,false,false,false,false,false};
    protected final TooltipDisplay tooltip = new TooltipDisplay();

    public CraftingTableGui(CraftingTableContainer container, PlayerInventory playerInventory, ITextComponent title)
    {
      super(container, playerInventory, title);
      this.player = playerInventory.player;
      this.xSize = 176;
      this.ySize = 188;
    }

    @Override
    public void init()
    {
      super.init();
      final int x0=guiLeft, y0=guiTop;
      buttons.clear();
      if(with_assist) {
        buttons.add(addButton(new ImageButton(x0+158,y0+30, 12,12, 194,44, 12, BACKGROUND, (bt)->action(CraftingTableContainer.BUTTON_NEXT))));
        buttons.add(addButton(new ImageButton(x0+158,y0+16, 12,12, 180,30, 12, BACKGROUND, (bt)->action(CraftingTableContainer.BUTTON_PREV))));
        buttons.add(addButton(new ImageButton(x0+158,y0+44, 12,12, 194,8,  12, BACKGROUND, (bt)->action(CraftingTableContainer.BUTTON_CLEAR_GRID))));
        buttons.add(addButton(new ImageButton(x0+116,y0+10, 20,10, 183,95, 12, BACKGROUND, (bt)->action(CraftingTableContainer.BUTTON_NEXT_COLLISION_RECIPE))));
      }
      {
        List<TipRange> tooltips = new ArrayList<>();
        final String prefix = ModContent.CRAFTING_TABLE.getTranslationKey() + ".tooltips.";
        String[] translation_keys = { "next", "prev", "clear", "nextcollisionrecipe", "fromstorage", "tostorage", "fromplayer", "toplayer" };
        for(int i=0; (i<buttons.size()) && (i<translation_keys.length); ++i) {
          Button bt = buttons.get(i);
          tooltips.add(new TipRange(bt.x,bt.y, bt.getWidth(), bt.getWidth_CLASH/*getHeight*/(), Auxiliaries.localizable(prefix+translation_keys[i])));
        }
        tooltip.init(tooltips);
      }
    }

    @Override
    public void render(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
    {
      if(with_assist) {
        boolean is_collision = getContainer().has_recipe_collision();
        buttons.get(3).visible = is_collision;
        buttons.get(3).active = is_collision;
      }
      renderBackground(mx);
      super.render(mx, mouseX, mouseY, partialTicks);
      if(!tooltip.render(mx,this, mouseX, mouseY)) func_230459_a_/*renderHoveredToolTip*/(mx, mouseX, mouseY);
    }

    @Override
    protected void func_230459_a_/*renderHoveredToolTip*/(MatrixStack mx, int mouseX, int mouseY)
    {
      if((!player.inventory.getItemStack().isEmpty()) || (getSlotUnderMouse() == null)) return;
      final Slot slot = getSlotUnderMouse();
      if(!slot.getStack().isEmpty()) { renderTooltip(mx, slot.getStack(), mouseX, mouseY); return; }
      if(with_assist) {
        int hist_index = -1;
        if(slot instanceof CraftingResultSlot) {
          hist_index = 0;
        } else if(slot.inventory instanceof CraftingInventory) {
          hist_index = slot.getSlotIndex() + 1;
        }
        if((hist_index < 0) || (hist_index >= history_slot_tooltip.length)) return;
        if(!history_slot_tooltip[hist_index]) return;
        ItemStack hist_stack = getContainer().history().current().get(hist_index);
        if(!hist_stack.isEmpty()) renderTooltip(mx, hist_stack, mouseX, mouseY);
      }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack mx, int x, int y)
    {}

    @Override
    @SuppressWarnings("deprecation")
    protected void drawGuiContainerBackgroundLayer(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.color3f(1.0F, 1.0F, 1.0F);
      getMinecraft().getTextureManager().bindTexture(BACKGROUND);
      final int x0=guiLeft, y0=guiTop;
      blit(mx, x0, y0, 0, 0, xSize, ySize);
      if(with_assist) {
        for(int i=0; i<history_slot_tooltip.length; ++i) history_slot_tooltip[i] = false;
        final List<ItemStack> crafting_template = getContainer().history().current();
        if((crafting_template == null) || (crafting_template.isEmpty())) return;
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : ((CraftingTableContainer)getContainer()).CRAFTING_SLOT_COORDINATES) {
            if(i==0) continue; // explicitly here, that is the result slot.
            if((getContainer().getSlot(i).getHasStack())) {
              if(!getContainer().getSlot(i).getStack().isItemEqual(crafting_template.get(i))) {
                return; // user has placed another recipe
              }
            }
            ++i;
          }
        }
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : ((CraftingTableContainer) getContainer()).CRAFTING_SLOT_COORDINATES) {
            final ItemStack stack = crafting_template.get(i);
            if(!stack.isEmpty()) {
              if(!getContainer().getSlot(i).getHasStack()) history_slot_tooltip[i] = true;
              if((i==0) && getContainer().getSlot(i).getStack().isItemEqual(crafting_template.get(i))) {
                continue; // don't shade the output slot if the result can be crafted.
              } else {
                draw_template_item_at(mx, stack, x0, y0, e.getA(), e.getB());
              }
            }
            ++i;
          }
        }
      }
    }

    @SuppressWarnings("deprecation")
    protected void draw_template_item_at(MatrixStack mx, ItemStack stack, int x0, int y0, int x, int y)
    {
      ItemRenderer ir = this.itemRenderer;
      final int main_zl = getBlitOffset();
      final float zl = ir.zLevel;
      ir.zLevel = -80;
      RenderSystem.enableRescaleNormal();
      ir.renderItemIntoGUI(stack, x0+x, y0+y);
      RenderSystem.disableRescaleNormal();
      RenderSystem.disableLighting();
      RenderSystem.disableColorMaterial();
      RenderSystem.enableAlphaTest();
      RenderSystem.defaultAlphaFunc();
      RenderSystem.enableBlend();
      ir.zLevel = zl;
      setBlitOffset(100);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.color4f(0.7f, 0.7f, 0.7f, 0.8f);
      getMinecraft().getTextureManager().bindTexture(BACKGROUND);
      blit(mx, x0+x, y0+y, x, y, 16, 16);
      RenderSystem.color4f(1f, 1f, 1f, 1f);
      setBlitOffset(main_zl);
    }

    protected void action(String message)
    { action(message, new CompoundNBT()); }

    protected void action(String message, CompoundNBT nbt)
    { getContainer().onGuiAction(message, nbt); tooltip.resetTimer(); }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type)
    {
      tooltip.resetTimer();
      if(type == ClickType.PICKUP) {
        boolean place_refab = (slot instanceof CraftingResultSlot) && (!slot.getHasStack());
        if(place_refab && with_assist_direct_history_refab) on_history_item_placement(); // place before crafting -> direct item pick
        super.handleMouseClick(slot, slotId, mouseButton, type);
        if(place_refab && (!with_assist_direct_history_refab)) on_history_item_placement(); // place after crafting -> confirmation first
        return;
      }
      if((type == ClickType.QUICK_MOVE) && (slotId > 0) && (slot.getHasStack())) { // container slots 0 is crafting output
        if(with_assist) {
          List<ItemStack> history = getContainer().history().current();
          boolean palce_in_crafting_grid = false;
          if(slotId > 9) { // container slots 1..9 are crafting grid
            palce_in_crafting_grid = (!history.isEmpty());
            if(!palce_in_crafting_grid) {
              for(int i = 0; i < 9; ++i) {
                if(!(getContainer().getSlot(i).getStack().isEmpty())) {
                  palce_in_crafting_grid = true;
                  break;
                }
              }
            }
          }
          if(palce_in_crafting_grid) {
            // Explicit grid placement.
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("containerslot", slotId);
            if(Auxiliaries.isCtrlDown()) nbt.putBoolean("move-all", true);
            action(CraftingTableContainer.ACTION_PLACE_SHIFTCLICKED_STACK, nbt);
            return;
          } else if(Auxiliaries.isCtrlDown()) {
            // Move all same items from the inventory of the clicked slot
            // (or the crafting grid) to the corresponding target inventory.
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("containerslot", slotId);
            action(CraftingTableContainer.ACTION_MOVE_ALL_STACKS, nbt);
            return;
          } else {
            // Let the normal slot click handle that.
          }
        }
      }
      super.handleMouseClick(slot, slotId, mouseButton, type);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double wheel_inc)
    {
      tooltip.resetTimer();
      final Slot resultSlot = this.getSlotUnderMouse();
      if((!with_crafting_slot_mouse_scrolling) || (!(resultSlot instanceof CraftingResultSlot))) {
        return this.getEventListenerForPos(mouseX, mouseY).filter((evl) -> {
          return evl.mouseScrolled(mouseX, mouseY, wheel_inc);
        }).isPresent();
      }
      int count = resultSlot.getStack().getCount();
      int limit = (Auxiliaries.isShiftDown() ? 2 : 1) * (Auxiliaries.isCtrlDown() ? 4 : 1);
      if(wheel_inc > 0.1) {
        if(count > 0) {
          if((count < resultSlot.getStack().getMaxStackSize()) && (count < resultSlot.getSlotStackLimit())) {
            CompoundNBT nbt = new CompoundNBT();
            if(limit > 1) nbt.putInt("limit", limit);
            action(CraftingTableContainer.ACTION_INCREASE_CRAFTING_STACKS, nbt);
          }
        } else if(!getContainer().history().current().isEmpty()) {
          action(CraftingTableContainer.ACTION_PLACE_CURRENT_HISTORY_SEL);
        }
      } else if(wheel_inc < -0.1) {
        if(count > 0) {
          CompoundNBT nbt = new CompoundNBT();
          if(limit > 1) nbt.putInt("limit", limit);
          action(CraftingTableContainer.ACTION_DECREASE_CRAFTING_STACKS, nbt);
        }
      }
      return true;
    }

    private void on_history_item_placement()
    {
      if((getContainer().history().current().isEmpty())) return;
      final Slot resultSlot = this.getSlotUnderMouse(); // double check
      if(!(resultSlot instanceof CraftingResultSlot)) return;
      action(CraftingTableContainer.ACTION_PLACE_CURRENT_HISTORY_SEL);
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
    public static final List<ItemStack> NOTHING = new ArrayList<ItemStack>();
    private static int max_history_size_ = 5;
    private final World world;
    private List<String> history_ = new ArrayList<String>();
    private String stash_ = new String();
    private int current_ = -1;
    private List<ItemStack> current_stacks_ = new ArrayList<ItemStack>();
    private ICraftingRecipe current_recipe_ = null;

    public CraftingHistory(World world)
    { this.world = world; }

    public static int max_history_size()
    { return max_history_size_; }

    public static int max_history_size(int newsize)
    { return max_history_size_ = MathHelper.clamp(newsize, 0, 32); }

    public void read(final CompoundNBT nbt)
    {
      try {
        clear();
        String s = nbt.getString("elements");
        if((s!=null) && (s.length() > 0)) {
          String[] ls = s.split("[|]");
          for(String e:ls) history_.add(e.toLowerCase().trim());
        }
        current_ = (!nbt.contains("current")) ? (-1) : MathHelper.clamp(nbt.getInt("current"), -1, history_.size()-1);
        stash_ = nbt.getString("stash");
        update_current();
      } catch(Throwable ex) {
        ModEngineersDecor.logger().error("Exception reading crafting table history NBT, resetting, exception is:" + ex.getMessage());
        clear();
      }
    }

    public CompoundNBT write()
    {
      final CompoundNBT nbt = new CompoundNBT();
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
        Tuple<ICraftingRecipe, List<ItemStack>> data = str2stacks(stash_);
        if(data != null) {
          current_recipe_ = data.getA();
          current_stacks_ = data.getB();
        }
      } else if((current_ < 0) || (current_ >= history_.size())) {
        reset_current();
      } else {
        Tuple<ICraftingRecipe, List<ItemStack>> data = str2stacks(history_.get(current_));
        if(data == null) { reset_current(); return; }
        current_recipe_ = data.getA();
        current_stacks_ = data.getB();
      }
    }

    public void stash(final List<ItemStack> grid_stacks, ICraftingRecipe recipe)
    {
      if(grid_stacks.size() == 9) {
        ArrayList<ItemStack> result_and_stacks = new ArrayList<>();
        result_and_stacks.add(recipe.getRecipeOutput());
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
        Tuple<ICraftingRecipe, List<ItemStack>> data = str2stacks(history_.get(i));
        if((data!=null) && (data.getA().getRecipeOutput().isItemEqual(result))) return i;
      }
      return -1;
    }

    public void add(final List<ItemStack> grid_stacks, ICraftingRecipe recipe)
    {
      if(!with_assist) { clear(); return; }
      stash_ = "";
      String s = stacks2str(grid_stacks, recipe);
      if(s.isEmpty()) return;
      String recipe_filter = recipe.getId().toString() + ";";
      history_.removeIf(e->e.equals(s));
      history_.removeIf(e->e.startsWith(recipe_filter));
      history_.add(s);
      while(history_.size() > max_history_size()) history_.remove(0);
      if(current_ >= history_.size()) reset_current();
    }

    public String stacks2str(final List<ItemStack> grid_stacks, ICraftingRecipe recipe)
    {
      if((grid_stacks==null) || (recipe==null)) return "";
      final int num_stacks = grid_stacks.size();
      if((num_stacks < 9) || (num_stacks > 10)) return "";
      final ArrayList<String> items = new ArrayList<String>();
      items.add(recipe.getId().toString());
      if(num_stacks < 10) items.add(recipe.getRecipeOutput().getItem().getRegistryName().toString());
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

    public @Nullable Tuple<ICraftingRecipe, List<ItemStack>> str2stacks(final String entry)
    {
      if((world==null) || (entry == null) || (entry.isEmpty())) return null;
      try {
        ArrayList<String> item_regnames = new ArrayList<String>(Arrays.asList(entry.split("[;]")));
        if((item_regnames == null) || (item_regnames.size() < 2) || (item_regnames.size() > 11)) return null;
        while(item_regnames.size() < 11) item_regnames.add("");
        final String recipe_name = item_regnames.remove(0);
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for(String regname:item_regnames) {
          ItemStack stack = ItemStack.EMPTY;
          if(!regname.isEmpty()) {
            final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(regname));
            stack = ((item == null) || (item == Items.AIR)) ? ItemStack.EMPTY : (new ItemStack(item, 1));
          }
          stacks.add(stack);
        }
        if((stacks.size() != 10) || (stacks.get(0).isEmpty())) return null; // invalid size or no result
        IRecipe recipe = world.getRecipeManager().getRecipe(new ResourceLocation(recipe_name)).orElse(null);
        if(!(recipe instanceof ICraftingRecipe)) return null;
        return new Tuple<ICraftingRecipe, List<ItemStack>>((ICraftingRecipe)recipe, stacks);
      } catch(Throwable ex) {
        ModEngineersDecor.logger().error("History stack building failed: " + ex.getMessage());
        return null;
      }
    }

    public List<ItemStack> current()
    { return current_stacks_; }

    public ICraftingRecipe current_recipe()
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

  // Crafting slot of the container ------------------------------------------------------------------------------------
  public static class CraftingOutputSlot extends CraftingResultSlot
  {
    private final CraftingTableContainer container;
    private final PlayerEntity player;

    public CraftingOutputSlot(CraftingTableContainer container, PlayerEntity player, CraftingInventory craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition)
    { super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition); this.container = container; this.player=player; }

    @Override
    protected void onCrafting(ItemStack stack)
    {
      if((with_assist) && ((player.world!=null) && (!(player.world.isRemote))) && (!stack.isEmpty())) {
        final IRecipe recipe = ((CraftResultInventory)this.inventory).getRecipeUsed();
        final ArrayList<ItemStack> grid = new ArrayList<ItemStack>();
        grid.add(stack);
        for(int i = 0; i < 9; ++i) grid.add(container.inventory_.getStackInSlot(i));
        if(recipe instanceof ICraftingRecipe) {
          container.history().add(grid, (ICraftingRecipe)recipe);
          container.history().reset_current();
          container.syncHistory();
        }
      }
      super.onCrafting(stack);
    }
  }

  // Crafting grid slot of the container -------------------------------------------------------------------------------
  public static class CraftingGridSlot extends Slot
  {
    public CraftingGridSlot(IInventory inv, int index, int x, int y)
    { super(inv, index, x, y); }
  }

  // Crafting inventory (needed to allow SlotCrafting to have a InventoryCrafting) -------------------------------------
  public static class CraftingTableGrid extends CraftingInventory
  {
    protected final Container container;
    protected final IInventory inventory;

    public CraftingTableGrid(Container container_, IInventory block_inventory) {
      super(container_, 3, 3);
      container = container_;
      inventory = block_inventory;
    }

    @Override
    public int getSizeInventory()
    { return 9; }

    @Override
    public void openInventory(PlayerEntity player)
    { inventory.openInventory(player); }

    @Override
    public void closeInventory(PlayerEntity player)
    { inventory.closeInventory(player); }

    @Override
    public void markDirty()
    { inventory.markDirty(); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
      inventory.setInventorySlotContents(index, stack);
      container.onCraftMatrixChanged(this);
    }

    @Override
    public ItemStack getStackInSlot(int index)
    { return inventory.getStackInSlot(index); }

    @Override
    public ItemStack decrStackSize(int index, int count)
    {
      final ItemStack stack = inventory.decrStackSize(index, count);
      if(!stack.isEmpty()) container.onCraftMatrixChanged(this);
      return stack;
    }
  }

}
