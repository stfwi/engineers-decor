/*
 * @file EdWasteIncinerator.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Trash/void/nullifier device with internal fifos.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.blocks.EdFurnace.FurnaceBlock;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.RfEnergy;
import wile.engineersdecor.libmc.detail.RsSignals;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class EdWasteIncinerator
{
  public static final int MAX_ENERGY_BUFFER = 16000;
  public static final int MAX_ENERGY_TRANSFER = 256;
  public static final int DEFAULT_ENERGY_CONSUMPTION = 16;
  private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;

  public static void on_config(int boost_energy_per_tick)
  {
    energy_consumption = Mth.clamp(boost_energy_per_tick, 4, 4096);
    ModConfig.log("Config waste incinerator: boost energy consumption:" + energy_consumption + ".");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class WasteIncineratorBlock extends StandardBlocks.Cutout implements StandardEntityBlocks.IStandardEntityBlock<WasteIncineratorTileEntity>
  {
    public static final BooleanProperty LIT = FurnaceBlock.LIT;

    public WasteIncineratorBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    @Nullable
    public BlockEntityType<EdWasteIncinerator.WasteIncineratorTileEntity> getBlockEntityType()
    { return ModContent.TET_WASTE_INCINERATOR; }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(LIT); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    { return super.getStateForPlacement(context).setValue(LIT, false); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos)
    { return (!(world.getBlockEntity(pos) instanceof WasteIncineratorTileEntity te)) ? 0 : RsSignals.fromContainer(te.main_inventory_); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundTag te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof EdWasteIncinerator.WasteIncineratorTileEntity)) return;
      ((EdWasteIncinerator.WasteIncineratorTileEntity)te).readnbt(te_nbt);
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
      if(!(te instanceof WasteIncineratorTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag te_nbt = ((WasteIncineratorTileEntity) te).getnbt();
        if(!te_nbt.isEmpty()) {
          CompoundTag nbt = new CompoundTag();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((WasteIncineratorTileEntity)te).main_inventory_) stacks.add(stack);
        ((WasteIncineratorTileEntity)te).getnbt();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    { return useOpenGui(state, world, pos, player); }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level world, BlockPos pos, Random rnd)
    {
      if((state.getBlock()!=this) || (!state.getValue(LIT))) return;
      final double rv = rnd.nextDouble();
      if(rv > 0.5) return;
      final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
      final double xr=rnd.nextDouble()*0.4-0.2, yr=rnd.nextDouble()*0.5, zr=rnd.nextDouble()*0.4-0.2;
      world.addParticle(ParticleTypes.SMOKE, x+xr, y+yr, z+zr, 0.0, 0.0, 0.0);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class WasteIncineratorTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable
  {
    public static final int NUM_OF_FIELDS = 1;
    public static final int TICK_INTERVAL = 20;
    public static final int ENERGIZED_TICK_INTERVAL = 5;
    public static final int INCINERATION_STACK_DECREMENT = 4;
    public static final int NUM_OF_SLOTS = 16;
    public static final int INPUT_SLOT_NO = 0;
    public static final int BURN_SLOT_NO = NUM_OF_SLOTS-1;

    // WasteIncineratorTileEntity -----------------------------------------------------------------------------

    private int tick_timer_;
    private int check_timer_;
    private final Inventories.StorageInventory main_inventory_ = new Inventories.StorageInventory(this, NUM_OF_SLOTS, 1);
    private final LazyOptional<? extends IItemHandler> item_handler_ = Inventories.MappedItemHandler.createInsertionHandler(main_inventory_, INPUT_SLOT_NO);
    private final RfEnergy.Battery battery_ = new RfEnergy.Battery(MAX_ENERGY_BUFFER, MAX_ENERGY_TRANSFER, 0);
    private final LazyOptional<IEnergyStorage> energy_handler_ = battery_.createEnergyHandler();

    public WasteIncineratorTileEntity(BlockPos pos, BlockState state)
    { super(ModContent.TET_WASTE_INCINERATOR, pos, state); reset(); }

    public CompoundTag getnbt()
    { return writenbt(new CompoundTag()); }

    protected void reset()
    {
      main_inventory_.clearContent();
      check_timer_ = 0;
      tick_timer_ = 0;
    }

    public void readnbt(CompoundTag nbt)
    {
      main_inventory_.load(nbt);
      battery_.load(nbt);
    }

    protected CompoundTag writenbt(CompoundTag nbt)
    {
      main_inventory_.save(nbt);
      battery_.save(nbt);
      return nbt;
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
      energy_handler_.invalidate();
      item_handler_.invalidate();
    }

    // INameable  ---------------------------------------------------------------------------

    @Override
    public Component getName()
    { final Block block=getBlockState().getBlock(); return new TextComponent((block!=null) ? block.getDescriptionId() : "Small Waste Incinerator"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public Component getCustomName()
    { return getName(); }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public Component getDisplayName()
    { return Nameable.super.getDisplayName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player )
    { return new EdWasteIncinerator.WasteIncineratorContainer(id, inventory, main_inventory_, ContainerLevelAccess.create(level, worldPosition), fields); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final ContainerData fields = new ContainerData()
    {
      @Override
      public int getCount()
      { return WasteIncineratorTileEntity.NUM_OF_FIELDS; }

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

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      if(capability == CapabilityEnergy.ENERGY) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      if(level.isClientSide) return;
      boolean dirty = false;
      ItemStack processing_stack = main_inventory_.getItem(BURN_SLOT_NO);
      final boolean was_processing = !processing_stack.isEmpty();
      boolean is_processing = was_processing;
      boolean new_stack_processing = false;
      if((!main_inventory_.getItem(0).isEmpty()) && transferItems(0, 1, main_inventory_.getMaxStackSize())) dirty = true;
      ItemStack first_stack = main_inventory_.getItem(0);
      boolean shift = !first_stack.isEmpty();
      if(is_processing) {
        processing_stack.shrink(INCINERATION_STACK_DECREMENT);
        if(processing_stack.getCount() <= 0) {
          processing_stack = ItemStack.EMPTY;
          is_processing = false;
        }
        main_inventory_.setItem(BURN_SLOT_NO, processing_stack);
        if(battery_.draw(energy_consumption * TICK_INTERVAL)) {
          tick_timer_ = ENERGIZED_TICK_INTERVAL;
        }
        dirty = true;
      }
      if(shift) {
        boolean transferred = false;
        for(int i=BURN_SLOT_NO-1; i>0; --i) {
          transferred |= transferItems(i-1, i, main_inventory_.getMaxStackSize());
        }
        if((!is_processing) && (!transferred)) {
          shiftStacks(0, BURN_SLOT_NO);
          dirty = true;
        }
      }
      if((was_processing != is_processing) || (new_stack_processing)) {
        if(new_stack_processing) level.playSound(null, worldPosition, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.05f, 2.4f);
        final BlockState state = level.getBlockState(worldPosition);
        if(state.getBlock() instanceof WasteIncineratorBlock) {
          level.setBlock(worldPosition, state.setValue(WasteIncineratorBlock.LIT, is_processing), 2|16);
        }
      }
      if(dirty) setChanged();
    }

    // Aux methods ----------------------------------------------------------------------------------

    private ItemStack shiftStacks(final int index_from, final int index_to)
    {
      if(index_from >= index_to) return ItemStack.EMPTY;
      ItemStack out_stack = ItemStack.EMPTY;
      ItemStack stack = main_inventory_.getItem(index_from);
      for(int i=index_from+1; i<=index_to; ++i) {
        out_stack = main_inventory_.getItem(i);
        main_inventory_.setItem(i, stack);
        stack = out_stack;
      }
      main_inventory_.setItem(index_from, ItemStack.EMPTY);
      return out_stack;
    }

    private boolean transferItems(final int index_from, final int index_to, int count)
    {
      ItemStack from = main_inventory_.getItem(index_from);
      if(from.isEmpty()) return false;
      ItemStack to = main_inventory_.getItem(index_to);
      if(from.getCount() < count) count = from.getCount();
      if(count <= 0) return false;
      boolean changed = true;
      if(to.isEmpty()) {
        main_inventory_.setItem(index_to, from.split(count));
      } else if(to.getCount() >= to.getMaxStackSize()) {
        changed = false;
      } else if(Inventories.areItemStacksDifferent(from, to)) {
        changed = false;
      } else {
        if((to.getCount()+count) >= to.getMaxStackSize()) {
          from.shrink(to.getMaxStackSize()-to.getCount());
          to.setCount(to.getMaxStackSize());
        } else {
          from.shrink(count);
          to.grow(count);
        }
      }
      if(from.isEmpty() && from!=ItemStack.EMPTY) {
        main_inventory_.setItem(index_from, ItemStack.EMPTY);
        changed = true;
      }
      return changed;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class WasteIncineratorContainer extends AbstractContainerMenu
  {
    private static final int PLAYER_INV_START_SLOTNO = WasteIncineratorTileEntity.NUM_OF_SLOTS;
    protected final Player player_;
    protected final Container inventory_;
    protected final ContainerLevelAccess wpc_;
    private final ContainerData fields_;
    private int proc_time_needed_;

    public int field(int index) { return fields_.get(index); }
    public Player player() { return player_ ; }
    public Container inventory() { return inventory_ ; }
    public Level world() { return player_.level; }

    public WasteIncineratorContainer(int cid, Inventory player_inventory)
    { this(cid, player_inventory, new SimpleContainer(WasteIncineratorTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(WasteIncineratorTileEntity.NUM_OF_FIELDS)); }

    private WasteIncineratorContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
    {
      super(ModContent.CT_WASTE_INCINERATOR, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      int i=-1;
      addSlot(new Slot(inventory_, ++i, 13, 9));
      addSlot(new Slot(inventory_, ++i, 37, 12));
      addSlot(new Slot(inventory_, ++i, 54, 13));
      addSlot(new Slot(inventory_, ++i, 71, 14));
      addSlot(new Slot(inventory_, ++i, 88, 15));
      addSlot(new Slot(inventory_, ++i, 105, 16));
      addSlot(new Slot(inventory_, ++i, 122, 17));
      addSlot(new Slot(inventory_, ++i, 139, 18));
      addSlot(new Slot(inventory_, ++i, 144, 38));
      addSlot(new Slot(inventory_, ++i, 127, 39));
      addSlot(new Slot(inventory_, ++i, 110, 40));
      addSlot(new Slot(inventory_, ++i, 93, 41));
      addSlot(new Slot(inventory_, ++i, 76, 42));
      addSlot(new Slot(inventory_, ++i, 59, 43));
      addSlot(new Slot(inventory_, ++i, 42, 44));
      addSlot(new Slot(inventory_, ++i, 17, 58));
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
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
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!moveItemStackTo(slot_stack, 0, PLAYER_INV_START_SLOTNO-1, true)) return ItemStack.EMPTY;
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
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class WasteIncineratorGui extends Guis.ContainerGui<WasteIncineratorContainer>
  {
    public WasteIncineratorGui(WasteIncineratorContainer container, Inventory player_inventory, Component title)
    { super(container, player_inventory, title, "textures/gui/small_waste_incinerator_gui.png"); }
  }

}
