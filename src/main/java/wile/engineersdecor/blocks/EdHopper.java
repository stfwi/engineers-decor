/*
 * @file EdHopper.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Hopper, factory automation suitable.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.*;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class EdHopper
{
  public static void on_config()
  {}

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class HopperBlock extends StandardBlocks.Directed implements StandardEntityBlocks.IStandardEntityBlock<EdHopper.HopperTileEntity>
  {
    public HopperBlock(long config, BlockBehaviour.Properties builder, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config, builder, shape_supplier); }

    @Override
    public ResourceLocation getBlockRegistryName()
    { return getRegistryName(); }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
    { return Shapes.block(); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos)
    { return (world.getBlockEntity(pos) instanceof EdHopper.HopperTileEntity te) ? RsSignals.fromContainer(te.storage_slot_range_) : 0; }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundTag te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof HopperTileEntity)) return;
      ((HopperTileEntity)te).readnbt(te_nbt, false);
      ((HopperTileEntity)te).reset_rtstate();
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
      if(!(te instanceof HopperTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag te_nbt = ((HopperTileEntity)te).clear_getnbt();
        if(!te_nbt.isEmpty()) {
          CompoundTag nbt = new CompoundTag();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((HopperTileEntity)te).main_inventory_) {
          if(!stack.isEmpty()) stacks.add(stack);
        }
        ((HopperTileEntity)te).reset_rtstate();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    { return useOpenGui(state, world, pos, player); }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
    {
      if(!(world instanceof Level) || (world.isClientSide)) return;
      BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof HopperTileEntity)) return;
      ((HopperTileEntity)te).block_updated();
    }

    @Override
    public void fallOn(Level world, BlockState state, BlockPos pos, Entity entity, float fallDistance)
    {
      super.fallOn(world, state, pos, entity, fallDistance);
      if(!(entity instanceof ItemEntity)) return;
      if(!(world.getBlockEntity(pos) instanceof HopperTileEntity te)) return;
      te.collection_timer_ = 0;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side)
    { return 0; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class HopperTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable
  {
    public static final int NUM_OF_FIELDS = 7;
    public static final int TICK_INTERVAL = 10;
    public static final int COLLECTION_INTERVAL = 50;
    public static final int NUM_OF_SLOTS = 18;
    public static final int NUM_OF_STORAGE_SLOTS = NUM_OF_SLOTS;
    public static final int MAX_TRANSFER_COUNT = 32;
    public static final int MAX_COLLECTION_RANGE = 4;
    public static final int PERIOD_OFFSET = 10;
    ///
    public static final int LOGIC_NOT_INVERTED = 0x00;
    public static final int LOGIC_INVERTED     = 0x01;
    public static final int LOGIC_CONTINUOUS   = 0x02;
    public static final int LOGIC_IGNORE_EXT   = 0x04;
    ///
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int collection_timer_ = 0;
    private int delay_timer_ = 0;
    private int transfer_count_ = 1;
    private int logic_ = LOGIC_INVERTED|LOGIC_CONTINUOUS;
    private int transfer_period_ = 0;
    private int collection_range_ = 0;
    private int current_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected final Inventories.StorageInventory main_inventory_ = new Inventories.StorageInventory(this, NUM_OF_SLOTS, 1);
    protected final Inventories.InventoryRange storage_slot_range_ = new Inventories.InventoryRange(main_inventory_, 0, NUM_OF_STORAGE_SLOTS);
    protected LazyOptional<? extends IItemHandler> item_handler_ = Inventories.MappedItemHandler.createGenericHandler(storage_slot_range_);

    public HopperTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.getBlockEntityTypeOfBlock(state.getBlock().getRegistryName().getPath()), pos, state);
      main_inventory_.setSlotChangeAction((slot,stack)->tick_timer_ = Math.min(tick_timer_, 8));
    }

    public void reset_rtstate()
    {
      block_power_signal_ = false;
      block_power_updated_ = false;
    }

    public CompoundTag clear_getnbt()
    {
      CompoundTag nbt = new CompoundTag();
      block_power_signal_ = false;
      writenbt(nbt, false);
      boolean is_empty = main_inventory_.isEmpty();
      main_inventory_.clearContent();
      reset_rtstate();
      block_power_updated_ = false;
      if(is_empty) nbt = new CompoundTag();
      return nbt;
    }

    public void readnbt(CompoundTag nbt, boolean update_packet)
    {
      main_inventory_.load(nbt);
      block_power_signal_ = nbt.getBoolean("powered");
      current_slot_index_ = nbt.getInt("act_slot_index");
      transfer_count_ = Mth.clamp(nbt.getInt("xsize"), 1, MAX_TRANSFER_COUNT);
      logic_ = nbt.getInt("logic");
      transfer_period_ = nbt.getInt("period");
      collection_range_ = nbt.getInt("range");
    }

    protected void writenbt(CompoundTag nbt, boolean update_packet)
    {
      main_inventory_.save(nbt);
      nbt.putBoolean("powered", block_power_signal_);
      nbt.putInt("act_slot_index", current_slot_index_);
      nbt.putInt("xsize", transfer_count_);
      nbt.putInt("logic", logic_);
      nbt.putInt("period", transfer_period_);
      nbt.putInt("range", collection_range_);
    }

    public void block_updated()
    {
      // RS power check, both edges
      boolean powered = level.hasNeighborSignal(worldPosition);
      if(block_power_signal_ != powered) block_power_updated_ = true;
      block_power_signal_ = powered;
      tick_timer_ = 1;
    }

    // BlockEntity --------------------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt, false); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.saveAdditional(nbt); writenbt(nbt, false); }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      item_handler_.invalidate();
    }

    // Namable ----------------------------------------------------------------------------------------------

    @Override
    public Component getName()
    { final Block block=getBlockState().getBlock(); return new TextComponent((block!=null) ? block.getDescriptionId() : "Factory Hopper"); }

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
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player )
    { return new HopperContainer(id, inventory, main_inventory_, ContainerLevelAccess.create(level, worldPosition), fields); }

/// CONTAINER SETITEM
//    @Override
//    public void setItem(int index, ItemStack stack)
//    {
//      stacks_.set(index, stack);
//      if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
//      if(tick_timer_ > 8) tick_timer_ = 8;
//      setChanged();
//    }

    // Fields -----------------------------------------------------------------------------------------------

    protected final ContainerData fields = new ContainerData()
    {
      @Override
      public int getCount()
      { return HopperTileEntity.NUM_OF_FIELDS; }

      @Override
      public int get(int id)
      {
        return switch(id) {
          case 0 -> collection_range_;
          case 1 -> transfer_count_;
          case 2 -> logic_;
          case 3 -> transfer_period_;
          case 4 -> delay_timer_;
          case 5 -> block_power_signal_ ? 1 : 0;
          case 6 -> current_slot_index_;
          default -> 0;
        };
      }
      @Override
      public void set(int id, int value)
      {
        switch (id) {
          case 0 -> collection_range_ = Mth.clamp(value, 0, MAX_COLLECTION_RANGE);
          case 1 -> transfer_count_ = Mth.clamp(value, 1, MAX_TRANSFER_COUNT);
          case 2 -> logic_ = value;
          case 3 -> transfer_period_ = Mth.clamp(value, 0, 100);
          case 4 -> delay_timer_ = Mth.clamp(value, 0, 400);
          case 5 -> block_power_signal_ = (value != 0);
          case 6 -> current_slot_index_ = Mth.clamp(value, 0, NUM_OF_STORAGE_SLOTS - 1);
        }
      }
    };

    // Capability export ------------------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickable and aux methods ---------------------------------------------------------------------

    private IItemHandler inventory_entity_handler(BlockPos where)
    {
      final List<Entity> entities = level.getEntities((Entity)null, (new AABB(where)), EntitySelector.CONTAINER_ENTITY_SELECTOR);
      return entities.isEmpty() ? null : Inventories.itemhandler(entities.get(0));
    }

    private static int next_slot(int i)
    { return (i<NUM_OF_STORAGE_SLOTS-1) ? (i+1) : 0; }

    private int try_insert_into_hopper(final ItemStack stack)
    {
      final int max_to_insert = stack.getCount();
      int n_to_insert = max_to_insert;
      int first_empty_slot = -1;
      for(int i=0; i<storage_slot_range_.size(); ++i) {
        final ItemStack slotstack = storage_slot_range_.get(i);
        if((first_empty_slot < 0) && slotstack.isEmpty()) { first_empty_slot=i; continue; }
        if(Inventories.areItemStacksDifferent(stack, slotstack)) continue;
        int nspace = slotstack.getMaxStackSize() - slotstack.getCount();
        if(nspace <= 0) {
          continue;
        } else if(nspace >= n_to_insert) {
          slotstack.grow(n_to_insert);
          n_to_insert = 0;
          break;
        } else {
          slotstack.grow(nspace);
          n_to_insert -= nspace;
        }
      }
      if((n_to_insert > 0) && (first_empty_slot >= 0)) {
        ItemStack new_stack = stack.copy();
        new_stack.setCount(n_to_insert);
        storage_slot_range_.set(first_empty_slot, new_stack);
        n_to_insert = 0;
      }
      return max_to_insert - n_to_insert;
    }

    private boolean try_insert(Direction facing)
    {
      ItemStack current_stack = ItemStack.EMPTY;
      for(int i=0; i<NUM_OF_STORAGE_SLOTS; ++i) {
        if(current_slot_index_ >= NUM_OF_STORAGE_SLOTS) current_slot_index_ = 0;
        current_stack = storage_slot_range_.get(current_slot_index_);
        if(!current_stack.isEmpty()) break;
        current_slot_index_ = next_slot(current_slot_index_);
      }
      if(current_stack.isEmpty()) {
        current_slot_index_ = 0;
        return false;
      }
      final BlockPos facing_pos = worldPosition.relative(facing);
      IItemHandler ih = null;
      // Tile entity insertion check
      {
        final BlockEntity te = level.getBlockEntity(facing_pos);
        if(te != null) {
          ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null);
          if(ih == null) { delay_timer_ = TICK_INTERVAL+2; return false; }
          if(te instanceof net.minecraft.world.level.block.entity.HopperBlockEntity) {
            Direction f = level.getBlockState(facing_pos).getValue(net.minecraft.world.level.block.HopperBlock.FACING);
            if(f==facing.getOpposite()) return false; // no back transfer
          } else if(te instanceof EdHopper.HopperTileEntity) {
            Direction f = level.getBlockState(facing_pos).getValue(EdHopper.HopperBlock.FACING);
            if(f==facing.getOpposite()) return false;
          }
        }
      }
      // Entity insertion check
      if(ih == null) ih = inventory_entity_handler(facing_pos);
      if(ih == null) { delay_timer_ = TICK_INTERVAL+2; return false; } // no reason to recalculate this all the time if there is nowhere to insert.
      // Handler insertion
      {
        ItemStack insert_stack = current_stack.copy();
        if(insert_stack.getCount() > transfer_count_) insert_stack.setCount(transfer_count_);
        final int initial_insert_stack_size = insert_stack.getCount();
        if((ih == null) || ih.getSlots() <= 0) return false;
        // First stack comletion insert run.
        for(int i=0; i<ih.getSlots(); ++i) {
          final ItemStack target_stack = ih.getStackInSlot(i);
          if(Inventories.areItemStacksDifferent(target_stack, insert_stack)) continue;
          insert_stack = ih.insertItem(i, insert_stack.copy(), false);
          if(insert_stack.isEmpty()) break;
        }
        // First-available insert run.
        if(!insert_stack.isEmpty()) {
          for(int i=0; i<ih.getSlots(); ++i) {
            insert_stack = ih.insertItem(i, insert_stack.copy(), false);
            if(insert_stack.isEmpty()) break;
          }
        }
        final int num_inserted = initial_insert_stack_size-insert_stack.getCount();
        if(num_inserted > 0) {
          current_stack.shrink(num_inserted);
          storage_slot_range_.set(current_slot_index_, current_stack);
        }
        if(!insert_stack.isEmpty()) current_slot_index_ = next_slot(current_slot_index_);
        return (num_inserted > 0);
      }
    }

    private boolean try_item_handler_extract(final IItemHandler ih)
    {
      final int end = ih.getSlots();
      int n_to_extract = transfer_count_;
      for(int i=0; i<end; ++i) {
        if(ih.getStackInSlot(i).isEmpty()) continue;
        ItemStack stack = ih.extractItem(i, n_to_extract, true);
        if(stack.isEmpty()) continue;
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted > 0) {
          ItemStack test = ih.extractItem(i, n_accepted, false);
          n_to_extract -= n_accepted;
          if(n_to_extract <= 0) break;
        }
      }
      return (n_to_extract < transfer_count_);
    }

    private boolean try_inventory_extract(final Container inv)
    {
      final int end = inv.getContainerSize();
      int n_to_extract = transfer_count_;
      for(int i=0; i<end; ++i) {
        ItemStack stack = inv.getItem(i).copy();
        if(stack.isEmpty()) continue;
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted > 0) {
          stack.shrink(n_accepted);
          n_to_extract -= n_accepted;
          if(stack.isEmpty()) stack = ItemStack.EMPTY;
          inv.setItem(i, stack);
          if(n_to_extract <= 0) break;
        }
      }
      if(n_to_extract < transfer_count_) {
        inv.setChanged();
        return true;
      } else {
        return false;
      }
    }

    private boolean try_collect(Direction facing)
    {
      AABB collection_volume;
      Vec3 rpos;
      if(facing==Direction.UP)  {
        rpos = new Vec3(0.5+worldPosition.getX(),1.5+worldPosition.getY(),0.5+worldPosition.getZ());
        collection_volume = (new AABB(worldPosition.above())).inflate(0.1+collection_range_, 0.6, 0.1+collection_range_);
      } else {
        rpos = new Vec3(0.5+worldPosition.getX(),-1.5+worldPosition.getY(),0.5+worldPosition.getZ());
        collection_volume = (new AABB(worldPosition.below(2))).inflate(0.1+collection_range_, 1, 0.1+collection_range_);
      }
      final List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, collection_volume, e->(e.isAlive() && e.isOnGround()));
      if(items.size() <= 0) return false;
      final int max_to_collect = 3;
      int n_collected = 0;
      for(ItemEntity ie:items) {
        boolean is_direct_collection_tange = ie.distanceToSqr(rpos)<0.7;
        if(!is_direct_collection_tange && (ie.hasPickUpDelay())) continue;
        ItemStack stack = ie.getItem();
        if(stack.isEmpty()) continue;
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted <= 0) continue;
        if(n_accepted >= stack.getCount()) {
          stack.setCount(0);
          ie.setItem(stack);
          ie.remove(Entity.RemovalReason.DISCARDED);
        } else {
          stack.shrink(n_accepted);
          ie.setItem(stack);
        }
        if((!is_direct_collection_tange) && (++n_collected >= max_to_collect)) break;
      }
      return (n_collected > 0);
    }

    @Override
    public void tick()
    {
      // Tick cycle pre-conditions
      if(level.isClientSide) return;
      if((delay_timer_ > 0) && ((--delay_timer_) == 0)) setChanged();
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      // Cycle init
      boolean dirty = block_power_updated_;
      final boolean rssignal = ((logic_ & LOGIC_IGNORE_EXT)!=0) || ((logic_ & LOGIC_INVERTED)!=0)==(!block_power_signal_);
      final boolean pulse_mode = ((logic_ & (LOGIC_CONTINUOUS|LOGIC_IGNORE_EXT))==0);
      boolean trigger = ((logic_ & LOGIC_IGNORE_EXT)!=0) || (rssignal && ((block_power_updated_) || (!pulse_mode)));
      final BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof HopperBlock)) { block_power_signal_= false; return; }
      final Direction hopper_facing = state.getValue(HopperBlock.FACING);
      // Trigger edge detection for next cycle
      {
        boolean tr = level.hasNeighborSignal(worldPosition);
        block_power_updated_ = (block_power_signal_ != tr);
        block_power_signal_ = tr;
        if(block_power_updated_) dirty = true;
      }
      // Collection
      if(rssignal || pulse_mode) {
        Direction hopper_input_facing = (hopper_facing==Direction.UP) ? Direction.DOWN : Direction.UP;
        BlockEntity te = level.getBlockEntity(worldPosition.relative(hopper_input_facing));
        IItemHandler ih = (te==null) ? (null) : (te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, hopper_input_facing.getOpposite()).orElse(null));
        if((ih != null) || (te instanceof WorldlyContainer)) {
          // Tile Entity pulling
          if((ih != null)) {
            if(try_item_handler_extract(ih)) dirty = true;
          } else {
            if(try_inventory_extract((WorldlyContainer)te)) dirty = true;
          }
        }
        if(ih==null) {
          ih = inventory_entity_handler(worldPosition.relative(hopper_input_facing));
          if((ih!=null) && (try_item_handler_extract(ih))) dirty = true;
        }
        if((ih==null) && (collection_timer_ -= TICK_INTERVAL) <= 0) {
          // Ranged collection
          collection_timer_ = COLLECTION_INTERVAL;
          if(try_collect(hopper_input_facing)) dirty = true;
        }
      }
      // Insertion
      if(trigger && (delay_timer_ <= 0)) {
        delay_timer_ = PERIOD_OFFSET + transfer_period_ * 2;
        if(try_insert(hopper_facing)) dirty = true;
      }
      if(dirty) setChanged();
      if(trigger && (tick_timer_ > TICK_INTERVAL)) tick_timer_ = TICK_INTERVAL;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class HopperContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
  {
    protected static final String QUICK_MOVE_ALL = "quick-move-all";
    private static final int PLAYER_INV_START_SLOTNO = HopperTileEntity.NUM_OF_SLOTS;
    private static final int NUM_OF_CONTAINER_SLOTS = HopperTileEntity.NUM_OF_SLOTS + 36;
    protected static final int STORAGE_SLOT_BEGIN = 0;
    protected static final int STORAGE_SLOT_END = HopperTileEntity.NUM_OF_SLOTS;
    protected static final int PLAYER_SLOT_BEGIN = HopperTileEntity.NUM_OF_SLOTS;
    protected static final int PLAYER_SLOT_END = HopperTileEntity.NUM_OF_SLOTS+36;
    private final Inventories.InventoryRange player_inventory_range_;
    private final Inventories.InventoryRange block_storage_range_;
    private final Player player_;
    private final Container inventory_;
    private final ContainerLevelAccess wpc_;
    private final ContainerData fields_;

    public final int field(int index) { return fields_.get(index); }

    public HopperContainer(int cid, Inventory player_inventory)
    { this(cid, player_inventory, new SimpleContainer(HopperTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(HopperTileEntity.NUM_OF_FIELDS)); }

    private HopperContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
    {
      super(ModContent.getMenuType("factory_hopper"), cid); // @todo: class mapping
      fields_ = fields;
      wpc_ = wpc;
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      block_storage_range_ = new Inventories.InventoryRange(inventory_, 0, HopperTileEntity.NUM_OF_SLOTS);
      player_inventory_range_ = Inventories.InventoryRange.fromPlayerInventory(player_);
      int i=-1;
      // input slots (stacks 0 to 17)
      for(int y=0; y<3; ++y) {
        for(int x=0; x<6; ++x) {
          int xpos = 11+x*18, ypos = 9+y*17;
          addSlot(new Slot(inventory_, ++i, xpos, ypos));
        }
      }
      // player slots
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 8+x*18, 129)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 8+x*18, 71+y*18)); // player slots: 9..35
        }
      }
      this.addDataSlots(fields_); // === Add reference holders
    }

    @Override
    public boolean stillValid(Player player)
    { return inventory_.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.hasItem())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getItem();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Device slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!moveItemStackTo(slot_stack, 0, HopperTileEntity.NUM_OF_SLOTS, false)) return ItemStack.EMPTY;
      } else {
        // invalid slot
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

    // INetworkSynchronisableContainer ---------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(CompoundTag nbt)
    { Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt); }

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String key, int value)
    {
      CompoundTag nbt = new CompoundTag();
      nbt.putInt(key, value);
      Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt);
    }

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
      if(!(inventory_ instanceof Inventories.StorageInventory)) return;
      if(!((((((Inventories.StorageInventory)inventory_).getTileEntity())) instanceof final EdHopper.HopperTileEntity te))) return;
      if(nbt.contains("xsize")) te.transfer_count_  = Mth.clamp(nbt.getInt("xsize"), 1, HopperTileEntity.MAX_TRANSFER_COUNT);
      if(nbt.contains("period")) te.transfer_period_ = Mth.clamp(nbt.getInt("period"),   0,  100);
      if(nbt.contains("range")) te.collection_range_ = Mth.clamp(nbt.getInt("range"),   0,  HopperTileEntity.MAX_COLLECTION_RANGE);
      if(nbt.contains("logic")) te.logic_  = nbt.getInt("logic");
      if(nbt.contains("manual_trigger") && (nbt.getInt("manual_trigger")!=0)) { te.block_power_signal_=true; te.block_power_updated_=true; te.tick_timer_=1; }
      if(nbt.contains("action")) {
        boolean changed = false;
        final int slotId = nbt.contains("slot") ? nbt.getInt("slot") : -1;
        switch (nbt.getString("action")) {
          case QUICK_MOVE_ALL -> {
            if ((slotId >= STORAGE_SLOT_BEGIN) && (slotId < STORAGE_SLOT_END) && (getSlot(slotId).hasItem())) {
              changed = block_storage_range_.move(getSlot(slotId).getSlotIndex(), player_inventory_range_, true, false, true, true);
            } else if ((slotId >= PLAYER_SLOT_BEGIN) && (slotId < PLAYER_SLOT_END) && (getSlot(slotId).hasItem())) {
              changed = player_inventory_range_.move(getSlot(slotId).getSlotIndex(), block_storage_range_, true, false, false, true);
            }
          }
        }
        if(changed) {
          inventory_.setChanged();
          player.getInventory().setChanged();
          broadcastChanges();
        }
      }
      te.setChanged();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class HopperGui extends Guis.ContainerGui<HopperContainer>
  {
    public HopperGui(HopperContainer container, Inventory player_inventory, Component title)
    { super(container, player_inventory, title, "textures/gui/factory_hopper_gui.png"); }

    @Override
    public void init()
    {
      super.init();
      {
        final Block block = ModContent.getBlock(getMenu().getType().getRegistryName().getPath().replaceAll("^ct_",""));
        final String prefix = block.getDescriptionId() + ".tooltips.";
        final int x0 = getGuiLeft(), y0 = getGuiTop();
        tooltip_.init(
          new TooltipDisplay.TipRange(x0+148, y0+22,  3,  3, new TranslatableComponent(prefix + "delayindicator")),
          new TooltipDisplay.TipRange(x0+130, y0+ 9, 40, 10, new TranslatableComponent(prefix + "range")),
          new TooltipDisplay.TipRange(x0+130, y0+22, 40, 10, new TranslatableComponent(prefix + "period")),
          new TooltipDisplay.TipRange(x0+130, y0+35, 40, 10, new TranslatableComponent(prefix + "count")),
          new TooltipDisplay.TipRange(x0+133, y0+49,  9,  9, new TranslatableComponent(prefix + "rssignal")),
          new TooltipDisplay.TipRange(x0+145, y0+49,  9,  9, new TranslatableComponent(prefix + "inversion")),
          new TooltipDisplay.TipRange(x0+159, y0+49,  9,  9, new TranslatableComponent(prefix + "triggermode"))
        );
      }
    }

    @Override
    protected void renderBgWidgets(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      HopperContainer container = getMenu();
      // active slot
      {
        int slot_index = container.field(6);
        if((slot_index < 0) || (slot_index >= HopperTileEntity.NUM_OF_SLOTS)) slot_index = 0;
        int x = (x0+10+((slot_index % 6) * 18));
        int y = (y0+8+((slot_index / 6) * 17));
        blit(mx, x, y, 200, 8, 18, 18);
      }
      // collection range
      {
        int[] lut = { 133, 141, 149, 157, 166 };
        int px = lut[Mth.clamp(container.field(0), 0, HopperTileEntity.MAX_COLLECTION_RANGE)];
        int x = x0 + px - 2;
        int y = y0 + 14;
        blit(mx, x, y, 179, 40, 5, 5);
      }
      // transfer period
      {
        int px = (int)Math.round(((33.5 * container.field(3)) / 100) + 1);
        int x = x0 + 132 - 2 + Mth.clamp(px, 0, 34);
        int y = y0 + 27;
        blit(mx, x, y, 179, 40, 5, 5);
      }
      // transfer count
      {
        int x = x0 + 133 - 2 + (container.field(1));
        int y = y0 + 40;
        blit(mx, x, y, 179, 40, 5, 5);
      }
      // redstone input
      {
        if(container.field(5) != 0) {
          blit(mx, x0+133, y0+49, 217, 49, 9, 9);
        }
      }
      // trigger logic
      {
        int inverter_offset_x = ((container.field(2) & HopperTileEntity.LOGIC_INVERTED) != 0) ? 11 : 0;
        int inverter_offset_y = ((container.field(2) & HopperTileEntity.LOGIC_IGNORE_EXT) != 0) ? 10 : 0;
        blit(mx, x0+145, y0+49, 177+inverter_offset_x, 49+inverter_offset_y, 9, 9);
        int pulse_mode_offset  = ((container.field(2) & HopperTileEntity.LOGIC_CONTINUOUS    ) != 0) ? 9 : 0;
        blit(mx, x0+159, y0+49, 199+pulse_mode_offset, 49, 9, 9);
      }
      // delay timer running indicator
      {
        if((container.field(4) > HopperTileEntity.PERIOD_OFFSET) && ((System.currentTimeMillis() % 1000) < 500)) {
          blit(mx, x0+148, y0+22, 187, 22, 3, 3);
        }
      }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type)
    {
      tooltip_.resetTimer();
      if((type == ClickType.QUICK_MOVE) && (slot!=null) && slot.hasItem() && Auxiliaries.isShiftDown() && Auxiliaries.isCtrlDown()) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("slot", slotId);
        menu.onGuiAction(HopperContainer.QUICK_MOVE_ALL, nbt);
      } else {
        super.slotClicked(slot, slotId, button, type);
      }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      tooltip_.resetTimer();
      HopperContainer container = getMenu();
      int mx = (int)(mouseX - getGuiLeft() + .5), my = (int)(mouseY - getGuiTop() + .5);
      if((!isHovering(126, 1, 49, 60, mouseX, mouseY))) {
        return super.mouseClicked(mouseX, mouseY, mouseButton);
      } else if(isHovering(128, 9, 44, 10, mouseX, mouseY)) {
        int range = (mx-133);
        if(range < -1) {
          range = container.field(0) - 1; // -
        } else if(range >= 34) {
          range = container.field(0) + 1; // +
        } else {
          range = (int)(0.5 + ((((double)HopperTileEntity.MAX_COLLECTION_RANGE) * range)/34)); // slider
          range = Mth.clamp(range, 0, HopperTileEntity.MAX_COLLECTION_RANGE);
        }
        container.onGuiAction("range", range);
      } else if(isHovering(128, 21, 44, 10, mouseX, mouseY)) {
        int period = (mx-133);
        if(period < -1) {
          period = container.field(3) - 3; // -
        } else if(period >= 35) {
          period = container.field(3) + 3; // +
        } else {
          period = (int)(0.5 + ((100.0 * period)/34));
        }
        period = Mth.clamp(period, 0, 100);
        container.onGuiAction("period", period);
      } else if(isHovering(128, 34, 44, 10, mouseX, mouseY)) {
        int ndrop = (mx-134);
        if(ndrop < -1) {
          ndrop = container.field(1) - 1; // -
        } else if(ndrop >= 34) {
          ndrop = container.field(1) + 1; // +
        } else {
          ndrop = Mth.clamp(1+ndrop, 1, HopperTileEntity.MAX_TRANSFER_COUNT); // slider
        }
        container.onGuiAction("xsize", ndrop);
      } else if(isHovering(133, 49, 9, 9, mouseX, mouseY)) {
        container.onGuiAction("manual_trigger", 1);
      } else if(isHovering(145, 49, 9, 9, mouseX, mouseY)) {
        final int mask = (HopperTileEntity.LOGIC_INVERTED|HopperTileEntity.LOGIC_IGNORE_EXT|HopperTileEntity.LOGIC_NOT_INVERTED);
        final int logic = switch (container.field(2) & mask) {
          case HopperTileEntity.LOGIC_NOT_INVERTED -> HopperTileEntity.LOGIC_INVERTED;
          case HopperTileEntity.LOGIC_INVERTED -> HopperTileEntity.LOGIC_IGNORE_EXT;
          case HopperTileEntity.LOGIC_IGNORE_EXT -> HopperTileEntity.LOGIC_NOT_INVERTED;
          default -> HopperTileEntity.LOGIC_IGNORE_EXT;
        };
        container.onGuiAction("logic", (container.field(2) & (~mask)) | logic);
      } else if(isHovering(159, 49, 7, 9, mouseX, mouseY)) {
        container.onGuiAction("logic", container.field(2) ^ HopperTileEntity.LOGIC_CONTINUOUS);
      }
      return true;
    }

  }

}
