/*
 * @file BlockDecorDirected.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Crafting table
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.Networking;
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
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.inventory.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.platform.GlStateManager;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class BlockDecorCraftingTable extends BlockDecorDirected.WaterLoggable
{
  public static boolean with_assist = true;
  public static boolean with_assist_direct_history_refab = false;
  public static boolean with_assist_quickmove_buttons = false;

  public static final void on_config(boolean without_crafting_assist, boolean with_assist_immediate_history_refab, boolean with_quickmove_buttons)
  {
    with_assist = !without_crafting_assist;
    with_assist_direct_history_refab = with_assist_immediate_history_refab;
    with_assist_quickmove_buttons = with_quickmove_buttons;
    CraftingHistory.max_history_size(32);
  }

  public BlockDecorCraftingTable(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config|CFG_WATERLOGGABLE, builder, unrotatedAABB); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BlockDecorCraftingTable.BTileEntity(); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if(world.isRemote) return true;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return true;
    if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return true;
    NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
    return true;
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
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).readnbt(inventory_nbt);
    ((BTileEntity)te).markDirty();
  }

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, World world, BlockPos pos, boolean explosion)
  {
    final List<ItemStack> stacks = new ArrayList<ItemStack>();
    if(world.isRemote) return stacks;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return stacks;
    if(!explosion) {
      ItemStack stack = new ItemStack(this, 1);
      CompoundNBT inventory_nbt = new CompoundNBT();
      ItemStackHelper.saveAllItems(inventory_nbt, ((BTileEntity)te).stacks, false);
      if(!inventory_nbt.isEmpty()) {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("inventory", inventory_nbt);
        stack.setTag(nbt);
      }
      ((BTileEntity) te).clear();
      stacks.add(stack);
    } else {
      for(ItemStack stack: ((BTileEntity)te).stacks) {
        if(!stack.isEmpty()) stacks.add(stack);
      }
      ((BTileEntity)te).reset();
    }
    return stacks;
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IInventory, INameable, INamedContainerProvider
  {
    public static final int NUM_OF_SLOTS = 9+8;
    protected NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
    protected CompoundNBT history = new CompoundNBT();

    public BTileEntity()
    { this(ModContent.TET_TREATED_WOOD_CRAFTING_TABLE); }

    public BTileEntity(TileEntityType<?> te_type)
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
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt); }

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
    { super.read(pkt.getNbtCompound()); readnbt(pkt.getNbtCompound()); super.onDataPacket(net, pkt); }

    @Override
    public void handleUpdateTag(CompoundNBT tag) // on client
    { read(tag); }

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
    { return new BContainer(id, inventory, this, IWorldPosCallable.of(world, pos)); }

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

  public static class BContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    // Crafting slot of container --------------------------------------------------------------------------------------
    public static class BSlotCrafting extends CraftingResultSlot
    {
      private final BContainer container;
      private final PlayerEntity player;

      public BSlotCrafting(BContainer container, PlayerEntity player, CraftingInventory craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition)
      { super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition); this.container = container; this.player=player; }

      @Override
      protected void onCrafting(ItemStack stack)
      {
        if((with_assist) && ((player.world!=null) && (!(player.world.isRemote))) && (!stack.isEmpty())) {
          final IRecipe recipe = ((CraftResultInventory)this.inventory).getRecipeUsed();
          final ArrayList<ItemStack> grid = new ArrayList<ItemStack>();
          grid.add(stack);
          for(int i = 0; i < 9; ++i) grid.add(container.inventory_.getStackInSlot(i));
          container.history().add(grid, recipe);
          container.history().reset_current();
          container.syncHistory(player);
        }
        super.onCrafting(stack);
      }
    }

    // Crafting inventory (needed to allow SlotCrafting to have a InventoryCrafting) -----------------------------------
    public static class BInventoryCrafting extends CraftingInventory
    {
      protected final Container container;
      protected final IInventory inventory;

      public BInventoryCrafting(Container container_, IInventory block_inventory) {
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

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    public static final int CRAFTING_SLOTS_BEGIN = 0;
    public static final int NUM_OF_CRAFTING_SLOTS = 9;
    public static final int STORAGE_SLOTS_BEGIN = NUM_OF_CRAFTING_SLOTS;
    public static final int NUM_OF_STORAGE_SLOTS = 8;

    public final ImmutableList<Tuple<Integer,Integer>> CRAFTING_SLOT_COORDINATES;
    private final PlayerEntity player_;
    private final IInventory inventory_;
    private final IWorldPosCallable wpc_;
    private final CraftingHistory history_;
    private final BInventoryCrafting matrix_;
    private final CraftResultInventory result_;
    private boolean has_recipe_collision_;

    public BContainer(int cid, PlayerInventory pinv)
    { this(cid, pinv, new Inventory(BTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY); }

    private BContainer(int cid, PlayerInventory pinv, IInventory block_inventory, IWorldPosCallable wpc)
    {
      super(ModContent.CT_TREATED_WOOD_CRAFTING_TABLE, cid);
      wpc_ = wpc;
      player_ = pinv.player;
      inventory_ = block_inventory;
      World world = player_.world;
      if(world.isRemote && (inventory_ instanceof BTileEntity)) world = ((BTileEntity)inventory_).getWorld();
      history_ = new CraftingHistory(world);
      result_ = new CraftResultInventory();
      matrix_ = new BInventoryCrafting(this, block_inventory);
      matrix_.openInventory(player_);
      // container slotId 0 === crafting output
      addSlot(new BSlotCrafting(this, pinv.player, matrix_, result_, 0, 134, 35));
      ArrayList<Tuple<Integer,Integer>> slotpositions = new ArrayList<Tuple<Integer,Integer>>();
      slotpositions.add(new Tuple<>(134, 35));
      // container slotId 1..9 === TE slots 0..8
      for(int y=0; y<3; ++y) {
        for(int x=0; x<3; ++x) {
          int xpos = 60+x*18;
          int ypos = 17+y*18;
          addSlot(new Slot(matrix_, x+y*3, xpos, ypos));
          slotpositions.add(new Tuple<>(xpos, ypos));
        }
      }
      // container slotId 10..36 === player slots: 9..35
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(pinv, x+y*9+9, 8+x*18, 86+y*18));
        }
      }
      // container slotId 37..45 === player slots: 0..8
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(pinv, x, 8+x*18, 144));
      }
      // container slotId 46..53 === TE slots 9..17 (storage)
      for(int y=0; y<4; ++y) {
        for(int x=0; x<2; ++x) {
          addSlot(new Slot(matrix_, x+y*2+9, 8+x*18, 9+y*18));
        }
      }
      if((!player_.world.isRemote) && (inventory_ instanceof BTileEntity)) {
        history_.read(((BTileEntity)inventory_).history.copy());
        syncHistory(player_);
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
          //craft(windowId, world, player_, matrix_, result_);
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
          syncProperties(player);
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
        if(!this.mergeItemStack(slotstack, 46, 54, false)) return ItemStack.EMPTY;
      } else if((index >= 46) && (index < 54)) {
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

    private boolean itemstack_recipe_match(ItemStack grid_stack, ItemStack history_stack)
    {
      if(history_.current_recipe()!=null) {
        boolean grid_match, dist_match;
        for(int i=0; i<history_.current_recipe().getIngredients().size(); ++i) {
          Ingredient ingredient = (Ingredient)history_.current_recipe().getIngredients().get(i);
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

    private List<ItemStack> crafting_slot_stacks_to_add()
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

    /**
     * Moves as much items from the stack to the slots in range [first_slot, last_slot] of the inventory,
     * filling up existing stacks first, then (player inventory only) checks appropriate empty slots next
     * to stacks that have that item already, and last uses any empty slot that can be found.
     * Returns the stack that is still remaining in the referenced `stack`.
     */
    private ItemStack move_stack_to_inventory(final ItemStack stack_to_move, IInventory inventory, final int slot_begin, final int slot_end, boolean only_fillup)
    {
      final ItemStack mvstack = stack_to_move.copy();
      if((mvstack.isEmpty()) || (slot_begin < 0) || (slot_end > inventory.getSizeInventory())) return mvstack;
      // first iteration: fillup existing stacks
      for(int i = slot_begin; i < slot_end; ++i) {
        final ItemStack stack = inventory.getStackInSlot(i);
        if((stack.isEmpty()) || (!stack.isItemEqual(mvstack))) continue;
        int nmax = stack.getMaxStackSize() - stack.getCount();
        if(mvstack.getCount() <= nmax) {
          stack.setCount(stack.getCount()+mvstack.getCount());
          mvstack.setCount(0);
          inventory.setInventorySlotContents(i, stack);
          return mvstack;
        } else {
          stack.setCount(stack.getMaxStackSize());
          mvstack.shrink(nmax);
          inventory.setInventorySlotContents(i, stack);
        }
      }
      if(only_fillup) return mvstack;
      if(inventory instanceof PlayerInventory) {
        // second iteration: use appropriate empty slots
        for(int i = slot_begin+1; i < slot_end-1; ++i) {
          final ItemStack stack = inventory.getStackInSlot(i);
          if(!stack.isEmpty()) continue;
          if((!inventory.getStackInSlot(i+1).isItemEqual(mvstack)) && (!inventory.getStackInSlot(i-1).isItemEqual(mvstack))) continue;
          inventory.setInventorySlotContents(i, mvstack.copy());
          mvstack.setCount(0);
          return mvstack;
        }
      }
      // third iteration: use any empty slots
      for(int i = slot_begin; i < slot_end; ++i) {
        final ItemStack stack = inventory.getStackInSlot(i);
        if(!stack.isEmpty()) continue;
        inventory.setInventorySlotContents(i, mvstack.copy());
        mvstack.setCount(0);
        return mvstack;
      }
      return mvstack;
    }

    /**
     * Moves as much items from the slots in range [first_slot, last_slot] of the inventory into a new stack.
     * Implicitly shrinks the inventory stacks and the `request_stack`.
     */
    private ItemStack move_stack_from_inventory(IInventory inventory, final ItemStack request_stack, final int slot_begin, final int slot_end)
    {
      ItemStack fetched_stack = request_stack.copy();
      fetched_stack.setCount(0);
      int n_left = request_stack.getCount();
      while(n_left > 0) {
        int smallest_stack_size = 0;
        int smallest_stack_index = -1;
        for(int i = slot_begin; i < slot_end; ++i) {
          final ItemStack stack = inventory.getStackInSlot(i);
          if((!stack.isEmpty()) && (stack.isItemEqual(request_stack))) {
            // Never automatically place stuff with nbt (except a few allowed like "Damage"),
            // as this could be a full crate, a valuable tool item, etc. For these recipes
            // the user has to place this item manually.
            if(stack.hasTag()) {
              final CompoundNBT nbt = stack.getTag();
              int n = stack.getTag().size();
              if((n > 0) && (stack.getTag().contains("Damage"))) --n;
              if(n > 0) continue;
            }
            fetched_stack = stack.copy(); // copy exact stack with nbt and tool damage, otherwise we have an automagical repair of items.
            fetched_stack.setCount(0);
            int n = stack.getCount();
            if((n < smallest_stack_size) || (smallest_stack_size <= 0)) {
              smallest_stack_size = n;
              smallest_stack_index = i;
            }
          }
        }
        if(smallest_stack_index < 0) {
          break; // no more items available
        } else {
          int n = Math.min(n_left, smallest_stack_size);
          n_left -= n;
          fetched_stack.grow(n);
          ItemStack st = inventory.getStackInSlot(smallest_stack_index);
          st.shrink(n);
          inventory.setInventorySlotContents(smallest_stack_index, st);
        }
      }
      return fetched_stack;
    }

    private boolean clear_grid_to_storage(PlayerEntity player)
    {
      boolean changed = false;
      for(int grid_i = CRAFTING_SLOTS_BEGIN; grid_i < (CRAFTING_SLOTS_BEGIN+NUM_OF_CRAFTING_SLOTS); ++grid_i) {
        ItemStack stack = inventory_.getStackInSlot(grid_i);
        if(stack.isEmpty()) continue;
        ItemStack remaining = move_stack_to_inventory(stack, inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS, false);
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
        remaining = move_stack_to_inventory(remaining, player.inventory,9, 36, true); // prefer filling up inventory stacks
        remaining = move_stack_to_inventory(remaining, player.inventory,0, 9, true);  // then fill up the hotbar stacks
        remaining = move_stack_to_inventory(remaining, player.inventory,9, 36, false); // then allow empty stacks in inventory
        remaining = move_stack_to_inventory(remaining, player.inventory,0, 9, false);  // then new stacks in the hotbar
        inventory_.setInventorySlotContents(grid_i, remaining);
        changed = true;
      }
      return changed;
    }

    enum EnumRefabPlacement { UNCHANGED, INCOMPLETE, PLACED }
    private EnumRefabPlacement place_refab_stacks(IInventory inventory, final int slot_begin, final int slot_end)
    {
      List<ItemStack> to_fill = crafting_slot_stacks_to_add();
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      boolean slots_changed = false;
      boolean missing_item = false;
      int num_slots_placed = 0;
      if(!to_fill.isEmpty()) {
        for(int it_guard=63; it_guard>=0; --it_guard) {
          boolean slots_updated = false;
          for(int i = 0; i < 9; ++i) {
            final ItemStack req_stack = to_fill.get(i).copy();
            if(req_stack.isEmpty()) continue;
            req_stack.setCount(1);
            to_fill.get(i).shrink(1);
            final ItemStack mv_stack = move_stack_from_inventory(inventory, req_stack, slot_begin, slot_end);
            if(mv_stack.isEmpty()) { missing_item=true; continue; }
            // sizes already checked
            ItemStack grid_stack = inventory_.getStackInSlot(i + CRAFTING_SLOTS_BEGIN).copy();
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
      if(!slots_changed) {
        return EnumRefabPlacement.UNCHANGED;
      } else if(missing_item) {
        return EnumRefabPlacement.INCOMPLETE;
      } else {
        return EnumRefabPlacement.PLACED;
      }
    }

    private EnumRefabPlacement distribute_stack(IInventory inventory, final int slotno)
    {
      List<ItemStack> to_refab = crafting_slot_stacks_to_add();
      if(history_.current_recipe() != null) result_.setRecipeUsed(history_.current_recipe());
      ItemStack to_distribute = inventory.getStackInSlot(slotno).copy();
      if(to_distribute.isEmpty()) return EnumRefabPlacement.UNCHANGED;
      int matching_grid_stack_sizes[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1};
      int max_matching_stack_size = -1;
      int min_matching_stack_size = 65;
      int total_num_missing_stacks = 0;
      for(int i=0; i<9; ++i) {
        final ItemStack grid_stack = inventory_.getStackInSlot(i+CRAFTING_SLOTS_BEGIN);
        final ItemStack refab_stack = to_refab.isEmpty() ? ItemStack.EMPTY : to_refab.get(i).copy();
        if((!grid_stack.isEmpty()) && (grid_stack.isItemEqual(to_distribute))) {
          matching_grid_stack_sizes[i] = grid_stack.getCount();
          total_num_missing_stacks += grid_stack.getMaxStackSize()-grid_stack.getCount();
          if(max_matching_stack_size < matching_grid_stack_sizes[i]) max_matching_stack_size = matching_grid_stack_sizes[i];
          if(min_matching_stack_size > matching_grid_stack_sizes[i]) min_matching_stack_size = matching_grid_stack_sizes[i];
        } else if((!refab_stack.isEmpty()) && (refab_stack.isItemEqual(to_distribute))) {
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
      if(min_matching_stack_size < 0) return EnumRefabPlacement.UNCHANGED;
      final int stack_limit_size = Math.min(to_distribute.getMaxStackSize(), inventory_.getInventoryStackLimit());
      if(min_matching_stack_size >= stack_limit_size) return EnumRefabPlacement.UNCHANGED;
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
      if(n_to_distribute == to_distribute.getCount()) return EnumRefabPlacement.UNCHANGED; // was already full
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
      return EnumRefabPlacement.PLACED;
    }

    // Container client/server synchronisation --------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String message, CompoundNBT nbt)
    {
      nbt.putString("action", message);
      Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt);
    }

    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {
      if(nbt.contains("history"))  history_.read(nbt.getCompound("history"));
      if(nbt.contains("hascollision")) has_recipe_collision_ = nbt.getBoolean("hascollision");
    }

    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      boolean changed = false;
      boolean player_inventory_changed = false;
      if(with_assist && nbt.contains("action")) {
        switch(nbt.getString("action")) {
          case BGui.BUTTON_NEXT: {
            history_.next();
            syncHistory(player);
            // implicitly clear the grid, so that the player can see the refab, and that no recipe is active.
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BGui.BUTTON_PREV: {
            history_.prev();
            syncHistory(player);
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BGui.BUTTON_CLEAR_GRID: {
            history_.reset_selection();
            syncHistory(player);
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BGui.BUTTON_TO_STORAGE: {
            if(clear_grid_to_storage(player)) changed = true;
          } break;
          case BGui.BUTTON_TO_PLAYER: {
            if(clear_grid_to_player(player)) { changed = true; player_inventory_changed = true; }
          } break;
          case BGui.BUTTON_FROM_STORAGE: {
            EnumRefabPlacement from_storage = place_refab_stacks(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS);
            if(from_storage != EnumRefabPlacement.UNCHANGED) changed = true;
          } break;
          case BGui.BUTTON_FROM_PLAYER: {
            EnumRefabPlacement from_player_inv = place_refab_stacks(player.inventory, 9, 36);
            if(from_player_inv != EnumRefabPlacement.UNCHANGED) { changed = true; player_inventory_changed = true; }
            if(from_player_inv != EnumRefabPlacement.PLACED) {
              EnumRefabPlacement from_hotbar = place_refab_stacks(player.inventory, 0, 9);
              if(from_hotbar != EnumRefabPlacement.UNCHANGED) { changed = true; player_inventory_changed = true; }
            }
          } break;
          case BGui.ACTION_PLACE_CURRENT_HISTORY_SEL: {
            EnumRefabPlacement from_storage = place_refab_stacks(inventory_, STORAGE_SLOTS_BEGIN, STORAGE_SLOTS_BEGIN+NUM_OF_STORAGE_SLOTS);
            if(from_storage != EnumRefabPlacement.UNCHANGED) changed = true;
            if(from_storage != EnumRefabPlacement.PLACED) {
              EnumRefabPlacement from_player_inv = place_refab_stacks(player.inventory, 9, 36);
              if(from_player_inv != EnumRefabPlacement.UNCHANGED) { changed = true; player_inventory_changed = true; }
              if(from_player_inv != EnumRefabPlacement.PLACED) {
                EnumRefabPlacement from_hotbar = place_refab_stacks(player.inventory, 0, 9);
                if(from_hotbar != EnumRefabPlacement.UNCHANGED) { changed = true; player_inventory_changed = true; }
              }
            }
          } break;
          case BGui.ACTION_PLACE_SHIFTCLICKED_STACK: {
            final int container_slot_id = nbt.getInt("containerslot");
            if((container_slot_id < 10) || (container_slot_id > 53)) break; // out of range
            if(container_slot_id >= 46) {
              // from storage
              final int storage_slot = container_slot_id - 46 + STORAGE_SLOTS_BEGIN;
              EnumRefabPlacement stat = distribute_stack(inventory_, storage_slot);
              if(stat != EnumRefabPlacement.UNCHANGED) changed = true;
            } else {
              // from player
              int player_slot = (container_slot_id >= 37) ? (container_slot_id-37) : (container_slot_id-10+9);
              EnumRefabPlacement stat = distribute_stack(player.inventory, player_slot);
              if(stat != EnumRefabPlacement.UNCHANGED) { player_inventory_changed = true; changed = true; }
            }
          } break;
          case BGui.BUTTON_NEXT_COLLISION_RECIPE: {
            select_next_collision_recipe(inventory_);
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

    // todo: somehow hook into the container listeners for syncing all clients having that container open.
    private void syncHistory(PlayerEntity player)
    {
      if((!with_assist) || (player.world.isRemote)) return;
      CompoundNBT hist_nbt = history_.write();
      if((inventory_ instanceof BTileEntity)) {
        ((BTileEntity)inventory_).history = hist_nbt.copy();
        inventory_.markDirty();
      }
      final CompoundNBT nbt = new CompoundNBT();
      nbt.put("history", hist_nbt);
      nbt.putBoolean("hascollision", has_recipe_collision_);
      Networking.PacketContainerSyncServerToClient.sendToPlayer(player, windowId, nbt);
    }

    private void syncProperties(PlayerEntity player)
    {
      final CompoundNBT nbt = new CompoundNBT();
      nbt.putBoolean("hascollision", has_recipe_collision_);
      Networking.PacketContainerSyncServerToClient.sendToPlayer(player, windowId, nbt);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class BGui extends ContainerScreen<BContainer>
  {
    protected static final String BUTTON_NEXT = "next";
    protected static final String BUTTON_PREV = "prev";
    protected static final String BUTTON_CLEAR_GRID = "clear";
    protected static final String BUTTON_FROM_STORAGE = "from-storage";
    protected static final String BUTTON_TO_STORAGE = "to-storage";
    protected static final String BUTTON_FROM_PLAYER = "from-player";
    protected static final String BUTTON_TO_PLAYER = "to-player";
    protected static final String BUTTON_NEXT_COLLISION_RECIPE = "next-recipe";
    protected static final String ACTION_PLACE_CURRENT_HISTORY_SEL = "place-refab";
    protected static final String ACTION_PLACE_SHIFTCLICKED_STACK = "place-stack";
    protected static final ResourceLocation BACKGROUND = new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/treated_wood_crafting_table.png");
    protected final PlayerEntity player;
    protected final ArrayList<Button> buttons = new ArrayList<Button>();
    protected final boolean history_slot_tooltip[] = {false,false,false,false,false,false,false,false,false,false};

    public BGui(BContainer container, PlayerInventory playerInventory, ITextComponent title)
    {
      super(container, playerInventory, title);
      this.player = playerInventory.player;
    }

    @Override
    public void init()
    {
      super.init();
      final int x0=guiLeft, y0=guiTop;
      buttons.clear();
      if(with_assist) {
        buttons.add(addButton(new ImageButton(x0+158,y0+44, 12,12, 194,44, 12, BACKGROUND, (bt)->action(BUTTON_NEXT))));
        buttons.add(addButton(new ImageButton(x0+158,y0+30, 12,12, 180,30, 12, BACKGROUND, (bt)->action(BUTTON_PREV))));
        buttons.add(addButton(new ImageButton(x0+158,y0+58, 12,12, 194,8,  12, BACKGROUND, (bt)->action(BUTTON_CLEAR_GRID))));
        buttons.add(addButton(new ImageButton(x0+132,y0+18, 20,10, 183,95, 12, BACKGROUND, (bt)->action(BUTTON_NEXT_COLLISION_RECIPE))));
        if(with_assist_quickmove_buttons) {
          buttons.add(addButton(new ImageButton(x0+49, y0+34,  9,17, 219,34, 17, BACKGROUND, (bt)->action(BUTTON_FROM_STORAGE))));
          buttons.add(addButton(new ImageButton(x0+49, y0+52,  9,17, 208,16, 17, BACKGROUND, (bt)->action(BUTTON_TO_STORAGE))));
          buttons.add(addButton(new ImageButton(x0+77, y0+71, 17, 9, 198,71,  9, BACKGROUND, (bt)->action(BUTTON_FROM_PLAYER))));
          buttons.add(addButton(new ImageButton(x0+59, y0+71, 17, 9, 180,71,  9, BACKGROUND, (bt)->action(BUTTON_TO_PLAYER))));
        }
      }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
      if(with_assist) {
        boolean is_collision = getContainer().has_recipe_collision();
        buttons.get(3).visible = is_collision;
        buttons.get(3).active = is_collision;
      }
      renderBackground();
      super.render(mouseX, mouseY, partialTicks);
      renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY)
    {
      if((!player.inventory.getItemStack().isEmpty()) || (getSlotUnderMouse() == null)) return;
      final Slot slot = getSlotUnderMouse();
      if(!slot.getStack().isEmpty()) { renderTooltip(slot.getStack(), mouseX, mouseY); return; }
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
        if(!hist_stack.isEmpty()) renderTooltip(hist_stack, mouseX, mouseY);
      }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color4f(1f, 1f, 1f, 1f);
      this.minecraft.getTextureManager().bindTexture(BACKGROUND);
      final int x0=guiLeft, y0=guiTop;
      blit(x0, y0, 0, 0, xSize, ySize);
      if(with_assist) {
        for(int i=0; i<history_slot_tooltip.length; ++i) history_slot_tooltip[i] = false;
        final List<ItemStack> crafting_template = getContainer().history().current();
        if((crafting_template == null) || (crafting_template.isEmpty())) return;
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : ((BContainer)getContainer()).CRAFTING_SLOT_COORDINATES) {
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
          for(Tuple<Integer, Integer> e : ((BContainer) getContainer()).CRAFTING_SLOT_COORDINATES) {
            final ItemStack stack = crafting_template.get(i);
            if(!stack.isEmpty()) {
              if(!getContainer().getSlot(i).getHasStack()) history_slot_tooltip[i] = true;
              if((i==0) && getContainer().getSlot(i).getStack().isItemEqual(crafting_template.get(i))) {
                continue; // don't shade the output slot if the result can be crafted.
              } else {
                draw_template_item_at(stack, x0, y0, e.getA(), e.getB());
              }
            }
            ++i;
          }
        }
      }
    }

    protected void draw_template_item_at(ItemStack stack, int x0, int y0, int x, int y)
    {
      final int main_zl = this.blitOffset;
      RenderHelper.disableStandardItemLighting();
      RenderHelper.enableGUIStandardItemLighting();
      final float zl = itemRenderer.zLevel;
      itemRenderer.zLevel = -50;
      itemRenderer.renderItemIntoGUI(stack, x0+x, y0+y);
      itemRenderer.zLevel = zl;
      this.blitOffset = 100;
      GlStateManager.color4f(0.7f, 0.7f, 0.7f, 0.8f);
      minecraft.getTextureManager().bindTexture(BACKGROUND);
      blit(x0+x, y0+y, x, y, 16, 16);
      RenderHelper.enableGUIStandardItemLighting();
      RenderHelper.enableStandardItemLighting();
      this.blitOffset = main_zl;
    }

    protected void action(String message)
    { action(message, new CompoundNBT()); }

    protected void action(String message, CompoundNBT nbt)
    { getContainer().onGuiAction(message, nbt); }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type)
    {
      if(type == ClickType.PICKUP) {
        boolean place_refab = (slot instanceof CraftingResultSlot) && (!slot.getHasStack());
        if(place_refab && with_assist_direct_history_refab) on_history_item_placement(); // place before crafting -> direct item pick
        super.handleMouseClick(slot, slotId, mouseButton, type);
        if(place_refab && (!with_assist_direct_history_refab)) on_history_item_placement(); // place after crafting -> confirmation first
        return;
      }
      if((type == ClickType.QUICK_MOVE) && (slotId > 9) && (slot.getHasStack())) { // container slots 0..9 are crafting output and grid
        if(with_assist) {
          List<ItemStack> history = getContainer().history().current();
          boolean palce_in_crafting_grid = (!history.isEmpty());
          if(!palce_in_crafting_grid) {
            for(int i = 0; i < 9; ++i) {
              if(!(getContainer().getSlot(i).getStack().isEmpty())) {
                palce_in_crafting_grid = true;
                break;
              }
            }
          }
          if(palce_in_crafting_grid) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("containerslot", slotId);
            action(ACTION_PLACE_SHIFTCLICKED_STACK, nbt);
            return;
          }
        }
      }
      super.handleMouseClick(slot, slotId, mouseButton, type);
    }

    private void on_history_item_placement()
    {
      if((getContainer().history().current().isEmpty())) return;
      final Slot resultSlot = this.getSlotUnderMouse(); // double check
      if(!(resultSlot instanceof CraftingResultSlot)) return;
      action(ACTION_PLACE_CURRENT_HISTORY_SEL);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting history
  //--------------------------------------------------------------------------------------------------------------------

  private static class CraftingHistory
  {
    public static final int RESULT_STACK_INDEX = 0;
    public static final int INPUT_STACKS_BEGIN = 1;
    public static final List<ItemStack> NOTHING = new ArrayList<ItemStack>();
    private static int max_history_size_ = 5;
    private final World world;
    private List<String> history_ = new ArrayList<String>();
    private int current_ = -1;
    private List<ItemStack> current_stacks_ = new ArrayList<ItemStack>();
    private IRecipe current_recipe_ = null;

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
      return nbt;
    }

    public void clear()
    { reset_current(); history_.clear(); }

    public void reset_current()
    { current_ = -1; current_stacks_ = NOTHING; current_recipe_ = null; }

    void update_current()
    {
      if((current_ < 0) || (current_ >= history_.size())) { reset_current(); return; }
      Tuple<IRecipe, List<ItemStack>> data = str2stacks(history_.get(current_));
      if(data == null) { reset_current(); return; }
      current_recipe_ = data.getA();
      current_stacks_ = data.getB();
    }

    public void add(final List<ItemStack> grid_stacks, IRecipe recipe)
    {
      if(!with_assist) { clear(); return; }
      String s = stacks2str(grid_stacks, recipe);
      String recipe_filter = recipe.getId().toString() + ";";
      if(s.isEmpty()) return;
      history_.removeIf(e->e.equals(s));
      history_.removeIf(e->e.startsWith(recipe_filter));
      history_.add(s);
      while(history_.size() > max_history_size()) history_.remove(0);
      if(current_ >= history_.size()) reset_current();
    }

    public String stacks2str(final List<ItemStack> grid_stacks, IRecipe recipe)
    {
      if((grid_stacks==null) || (grid_stacks.size() != 10) || (recipe==null)) return "";
      if(grid_stacks.get(0).isEmpty()) return "";
      final ArrayList<String> items = new ArrayList<String>();
      items.add(recipe.getId().toString().trim());
      for(ItemStack st:grid_stacks) items.add( (st.isEmpty()) ? ("") : ((st.getItem().getRegistryName().toString().trim())));
      return String.join(";", items);
    }

    public @Nullable Tuple<IRecipe, List<ItemStack>> str2stacks(final String entry)
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
        if(recipe==null) return null;
        return new Tuple<IRecipe, List<ItemStack>>(recipe, stacks);
      } catch(Throwable ex) {
        ModEngineersDecor.logger().error("History stack building failed: " + ex.getMessage());
        return null;
      }
    }

    public List<ItemStack> current()
    { return current_stacks_; }

    public IRecipe current_recipe()
    { return current_recipe_; }

    public void next()
    {
      if(history_.isEmpty()) {
        current_ = -1;
      } else {
        current_ = ((++current_) >= history_.size()) ? (-1) : (current_);
      }
      update_current();
    }

    public void prev()
    {
      if(history_.isEmpty()) {
        current_ = -1;
      } else {
        current_ = ((--current_) < -1) ? (history_.size()-1) : (current_);
      }
      update_current();
    }

    public void reset_selection()
    { current_ = -1; update_current(); }

    public String toString()
    {
      String rec = (current_recipe_==null) ? "none" : (current_recipe_.getId().toString());
      StringBuilder s = new StringBuilder("{ current:" + current_ + ", recipe:'" + rec + "', elements:[ ");
      for(int i=0; i<history_.size(); ++i) s.append("{i:").append(i).append(", e:[").append(history_.get(i)).append("]} ");
      s.append("]}");
      return s.toString();
    }
  }

}
