/*
 * @file EdElectricalFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED small electrical pass-through furnace.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Crafting;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Inventories.MappedItemHandler;
import wile.engineersdecor.libmc.detail.Inventories.StorageInventory;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.RfEnergy;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EdElectricalFurnace
{
  public static void on_config(int speed_percent, int standard_energy_per_tick, boolean with_automatic_inventory_pulling)
  { ElectricalFurnaceTileEntity.on_config(speed_percent, standard_energy_per_tick, with_automatic_inventory_pulling); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<ElectricalFurnaceTileEntity>
  {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ElectricalFurnaceBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

    @Nullable
    @Override
    public BlockEntityType<EdElectricalFurnace.ElectricalFurnaceTileEntity> getBlockEntityType()
    { return ModContent.TET_SMALL_ELECTRICAL_FURNACE; }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return Shapes.block(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(LIT); }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos)
    { return state.getValue(LIT) ? super.getLightEmission(state, world, pos) : 0; }

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
    {
      BlockEntity te = world.getBlockEntity(pos);
      return (te instanceof EdElectricalFurnace.ElectricalFurnaceTileEntity) ? ((EdElectricalFurnace.ElectricalFurnaceTileEntity)te).getComparatorOutput() : 0;
    }

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
      world.setBlockAndUpdate(pos, state.setValue(LIT, false));
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundTag inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof ElectricalFurnaceTileEntity bte)) return;
      bte.readnbt(inventory_nbt);
      bte.setChanged();
      world.setBlockAndUpdate(pos, state.setValue(LIT, bte.burning()));
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, final BlockEntity te, boolean explosion) {
      final List<ItemStack> stacks = new ArrayList<>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof EdElectricalFurnace.ElectricalFurnaceTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag inventory_nbt = ((EdElectricalFurnace.ElectricalFurnaceTileEntity)te).reset_getnbt();
        if(!inventory_nbt.isEmpty()) {
          CompoundTag nbt = new CompoundTag();
          nbt.put("inventory", inventory_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((EdElectricalFurnace.ElectricalFurnaceTileEntity)te).inventory_) stacks.add(stack);
        ((EdElectricalFurnace.ElectricalFurnaceTileEntity)te).reset();
      }
      return stacks;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable
  {
    private static final RecipeType<SmeltingRecipe> RECIPE_TYPE = RecipeType.SMELTING;
    private static final int DEFAULT_SMELTING_TIME = 200;
    private static final int NUM_OF_FIELDS = 8;
    private static final int TICK_INTERVAL = 4;
    private static final int FIFO_INTERVAL = 20;
    private static final int HEAT_CAPACITY = 200;
    private static final int HEAT_INCREMENT = 20;
    private static final int MAX_BURNTIME = 0x7fff;
    private static final int MAX_XP_STORED = 65535;
    private static final int MAX_ENERGY_TRANSFER = 1024;
    private static final int MAX_ENERGY_BUFFER = 32000;
    private static final int MAX_SPEED_SETTING = 3;
    private static final int NUM_OF_SLOTS = 7;
    private static final int SMELTING_INPUT_SLOT_NO = 0;
    private static final int SMELTING_AUX_SLOT_NO = 1;
    private static final int SMELTING_OUTPUT_SLOT_NO = 2;
    private static final int FIFO_INPUT_0_SLOT_NO = 3;
    private static final int FIFO_INPUT_1_SLOT_NO = 4;
    private static final int FIFO_OUTPUT_0_SLOT_NO = 5;
    private static final int FIFO_OUTPUT_1_SLOT_NO = 6;

    public  static final int DEFAULT_SPEED_PERCENT = 290;
    public  static final int DEFAULT_ENERGY_CONSUMPTION = 16;
    public  static final int DEFAULT_SCALED_ENERGY_CONSUMPTION = DEFAULT_ENERGY_CONSUMPTION * TICK_INTERVAL;

    // Config ----------------------------------------------------------------------------------

    private static boolean with_automatic_inventory_pulling_ = false;
    private static int energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION;
    private static int transfer_energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION / 8;
    private static int proc_speed_percent_ = DEFAULT_SPEED_PERCENT;
    private static final double[] speed_setting_factor_ = {0.0, 1.0, 1.5, 2.0};

    public static void on_config(int speed_percent, int standard_energy_per_tick, boolean with_automatic_inventory_pulling)
    {
      proc_speed_percent_ = Mth.clamp(speed_percent, 10, 800);
      energy_consumption_ = Mth.clamp(standard_energy_per_tick, 4, 4096) * TICK_INTERVAL;
      transfer_energy_consumption_ = Mth.clamp(energy_consumption_ / 8, 8, HEAT_INCREMENT);
      with_automatic_inventory_pulling_ = with_automatic_inventory_pulling;
      ModConfig.log("Config electrical furnace speed:" + proc_speed_percent_ + "%, heat-loss: 1K/t, heating consumption:" + (energy_consumption_/TICK_INTERVAL)+"rf/t.");
    }

    // ElectricalFurnaceTileEntity -----------------------------------------------------------------------------
    private int tick_timer_;
    private int fifo_timer_;
    private double proc_time_elapsed_;
    private int proc_time_needed_;
    private int burntime_left_;
    private int field_is_burning_;
    private float xp_stored_;
    private @Nullable Recipe<?> current_recipe_ = null;
    private int fuel_burntime_;
    private int field_proc_time_elapsed_;
    private final StorageInventory inventory_;
    private final LazyOptional<IItemHandler> item_handler_;
    private final RfEnergy.Battery battery_;
    private final LazyOptional<IEnergyStorage> energy_handler_;
    private int speed_ = 1;
    private boolean enabled_ = false;
    private int field_power_consumption_;
    private int power_consumption_accumulator_;
    private int power_consumption_timer_;

    public ElectricalFurnaceTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.TET_SMALL_ELECTRICAL_FURNACE, pos, state);
      inventory_ = new StorageInventory(this, NUM_OF_SLOTS) {
        @Override
        public void setItem(int index, ItemStack stack)
        {
          ItemStack slot_stack = stacks_.get(index);
          boolean already_in_slot = (!stack.isEmpty()) && (Inventories.areItemStacksIdentical(stack, slot_stack));
          stacks_.set(index, stack);
          if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
          if((index == SMELTING_INPUT_SLOT_NO) && (!already_in_slot)) {
            proc_time_needed_ = Crafting.getSmeltingTimeNeeded(RECIPE_TYPE, level, stack);
            proc_time_elapsed_ = 0;
            setChanged();
          }
        }
      };
      inventory_.setValidator((index, stack)->switch (index) {
        case SMELTING_INPUT_SLOT_NO, FIFO_INPUT_0_SLOT_NO, FIFO_INPUT_1_SLOT_NO -> true;
        default -> false;
      });
      item_handler_ = MappedItemHandler.createGenericHandler(inventory_,
        (slot,stack)->((slot==FIFO_OUTPUT_0_SLOT_NO) || (slot==FIFO_OUTPUT_1_SLOT_NO)),
        (slot,stack)->((slot==FIFO_INPUT_0_SLOT_NO) || (slot==FIFO_INPUT_1_SLOT_NO)),
        Arrays.asList(FIFO_OUTPUT_0_SLOT_NO,FIFO_OUTPUT_1_SLOT_NO,FIFO_INPUT_0_SLOT_NO,FIFO_INPUT_1_SLOT_NO)
      );
      battery_ = new RfEnergy.Battery(MAX_ENERGY_BUFFER, MAX_ENERGY_TRANSFER, 0);
      energy_handler_ = battery_.createEnergyHandler();
    }

    public void reset()
    {
      inventory_.clearContent();
      burntime_left_ = 0;
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
      battery_.clear();
      speed_ = 1;
      field_is_burning_ = 0;
    }

    public CompoundTag reset_getnbt()
    {
      CompoundTag nbt = new CompoundTag();
      writenbt(nbt);
      reset();
      return nbt;
    }

    public void readnbt(CompoundTag nbt)
    {
      burntime_left_ = nbt.getInt("BurnTime");
      proc_time_elapsed_ = nbt.getInt("CookTime");
      proc_time_needed_ = nbt.getInt("CookTimeTotal");
      xp_stored_ = nbt.getFloat("XpStored");
      speed_ = nbt.getInt("SpeedSetting");
      speed_ = (speed_ < 0) ? (1) : (Math.min(speed_, MAX_SPEED_SETTING));
      battery_.load(nbt, "Energy");
      inventory_.load(nbt);
    }

    protected void writenbt(CompoundTag nbt)
    {
      nbt.putInt("BurnTime", Mth.clamp(burntime_left_, 0, HEAT_CAPACITY));
      nbt.putInt("CookTime", Mth.clamp((int)proc_time_elapsed_, 0, MAX_BURNTIME));
      nbt.putInt("CookTimeTotal", Mth.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      nbt.putInt("SpeedSetting", Mth.clamp(speed_, -1, MAX_SPEED_SETTING));
      nbt.putFloat("XpStored", Mth.clamp(xp_stored_, 0, MAX_XP_STORED));
      battery_.save(nbt, "Energy");
      inventory_.save(nbt);
    }

    public int getComparatorOutput()
    {
      return (battery_.isEmpty()) ? (0) : (
        (inventory_.getItem(FIFO_INPUT_1_SLOT_NO).isEmpty() ? 0 : 5) +
        (inventory_.getItem(FIFO_INPUT_0_SLOT_NO).isEmpty() ? 0 : 5) +
        (inventory_.getItem(SMELTING_INPUT_SLOT_NO).isEmpty() ? 0 : 5)
      );
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
      energy_handler_.invalidate();
    }

    @Override
    public Component getName()
    { final Block block=getBlockState().getBlock(); return new TextComponent((block!=null) ? block.getDescriptionId() : "Small electrical furnace"); }

    @Override
    public Component getDisplayName()
    { return Nameable.super.getDisplayName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player )
    { return new EdElectricalFurnace.ElectricalFurnaceContainer(id, inventory, inventory_, ContainerLevelAccess.create(level, worldPosition), fields); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final ContainerData fields = new ContainerData()
    {
      @Override
      public int getCount()
      { return ElectricalFurnaceTileEntity.NUM_OF_FIELDS; }

      @Override
      public int get(int id)
      {
        return switch(id) {
          case 0 -> ElectricalFurnaceTileEntity.this.burntime_left_;
          case 1 -> ElectricalFurnaceTileEntity.this.battery_.getEnergyStored();
          case 2 -> (int) ElectricalFurnaceTileEntity.this.proc_time_elapsed_;
          case 3 -> ElectricalFurnaceTileEntity.this.proc_time_needed_;
          case 4 -> ElectricalFurnaceTileEntity.this.speed_;
          case 5 -> ElectricalFurnaceTileEntity.this.battery_.getMaxEnergyStored();
          case 6 -> ElectricalFurnaceTileEntity.this.field_is_burning_;
          case 7 -> ElectricalFurnaceTileEntity.this.field_power_consumption_;
          default -> 0;
        };
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0 -> ElectricalFurnaceTileEntity.this.burntime_left_ = value;
          case 1 -> ElectricalFurnaceTileEntity.this.battery_.setEnergyStored(value);
          case 2 -> ElectricalFurnaceTileEntity.this.proc_time_elapsed_ = value;
          case 3 -> ElectricalFurnaceTileEntity.this.proc_time_needed_ = value;
          case 4 -> ElectricalFurnaceTileEntity.this.speed_ = value;
          case 5 -> ElectricalFurnaceTileEntity.this.battery_.setMaxEnergyStored(value);
          case 6 -> ElectricalFurnaceTileEntity.this.field_is_burning_ = value;
          case 7 -> ElectricalFurnaceTileEntity.this.field_power_consumption_ = value;
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
      if(!(level.getBlockState(worldPosition).getBlock() instanceof ElectricalFurnaceBlock)) return;
      final boolean was_burning = burning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(level.isClientSide()) return;
      int battery_energy = battery_.getEnergyStored();
      boolean update_blockstate = (was_burning != burning());
      boolean dirty = update_blockstate;
      boolean shift_in = false;
      boolean shift_out = false;
      if(--fifo_timer_ <= 0) {
        fifo_timer_ = FIFO_INTERVAL/TICK_INTERVAL;
        if(transferItems(FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO, 64)) { dirty = true; } else { shift_out = true; }
        if(transferItems(SMELTING_OUTPUT_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, 64)) dirty = true;
        if(transferItems(FIFO_INPUT_0_SLOT_NO, SMELTING_INPUT_SLOT_NO, 64)) dirty = true;
        if(transferItems(FIFO_INPUT_1_SLOT_NO, FIFO_INPUT_0_SLOT_NO, 64)) { dirty = true; } else { shift_in = true; }
      }
      if(battery_.getEnergyStored() < energy_consumption()) {
        enabled_ = false;
      } else if(battery_.getEnergyStored() >= (MAX_ENERGY_BUFFER/2)) {
        enabled_ = true;
      }
      if((!(inventory_.getItem(SMELTING_INPUT_SLOT_NO)).isEmpty()) && (enabled_) && (speed_>0)) {
        Recipe<?> last_recipe = currentRecipe();
        updateCurrentRecipe();
        if(currentRecipe() != last_recipe) {
          proc_time_elapsed_ = 0;
          proc_time_needed_ = getSmeltingTimeNeeded(level, inventory_.getItem(SMELTING_INPUT_SLOT_NO));
        }
        final boolean can_smelt = canSmeltCurrentItem();
        if((!can_smelt) && (getSmeltingResult(inventory_.getItem(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
          // bypass
          if(transferItems(SMELTING_INPUT_SLOT_NO, SMELTING_OUTPUT_SLOT_NO, 1)) dirty = true;
        } else {
          // smelt, automatically choke speed on low power storage
          final int speed = Mth.clamp((battery_.getSOC() >= 25) ? (speed_) : (1), 0, MAX_SPEED_SETTING);
          if(!burning() && can_smelt) {
            if(heat_up(speed)) { dirty = true; update_blockstate = true; }
          }
          if(burning() && can_smelt) {
            if(heat_up(speed)) dirty = true;
            proc_time_elapsed_ += (int)(TICK_INTERVAL * proc_speed_percent_ * speed_setting_factor_[speed] / 100);
            if(proc_time_elapsed_ >= proc_time_needed_) {
              proc_time_elapsed_ = 0;
              proc_time_needed_ = getSmeltingTimeNeeded(level, inventory_.getItem(SMELTING_INPUT_SLOT_NO));
              smeltCurrentItem();
              dirty = true;
              shift_out = true;
            }
          } else {
            proc_time_elapsed_ = 0;
          }
        }
      } else if(proc_time_elapsed_ > 0) {
        proc_time_elapsed_ -= ((inventory_.getItem(SMELTING_INPUT_SLOT_NO)).isEmpty() ? 20 : 1);
        if(proc_time_elapsed_ < 0) { proc_time_elapsed_ = 0; shift_out = true; update_blockstate = true; }
      }
      if(update_blockstate) {
        dirty = true;
        sync_blockstate();
      }
      if(adjacent_inventory_shift(shift_in, shift_out)) dirty = true;
      field_is_burning_ = burning() ? 1 : 0;
      // power consumption
      power_consumption_timer_ += TICK_INTERVAL;
      power_consumption_accumulator_ += (battery_.getEnergyStored() - battery_energy);
      if(power_consumption_timer_ >= 20) {
        field_power_consumption_ = power_consumption_accumulator_/power_consumption_timer_;
        power_consumption_accumulator_ = 0;
        power_consumption_timer_ = 0;
      }
      if(dirty) setChanged();
    }

    // Furnace --------------------------------------------------------------------------------------

    private boolean is_accepted_hopper(final ItemStack stack)
    { return (stack.getItem() == Blocks.HOPPER.asItem()) || (stack.getItem() == ModContent.FACTORY_HOPPER.asItem()); }

    private boolean transferItems(final int index_from, final int index_to, int count)
    {
      ItemStack from = inventory_.getItem(index_from);
      if(from.isEmpty()) return false;
      ItemStack to = inventory_.getItem(index_to);
      if(from.getCount() < count) count = from.getCount();
      if(count <= 0) return false;
      boolean changed = true;
      if(to.isEmpty()) {
        inventory_.setItem(index_to, from.split(count));
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
        inventory_.setItem(index_from, ItemStack.EMPTY);
        changed = true;
      }
      return changed;
    }

    private boolean adjacent_inventory_shift(boolean inp, boolean out)
    {
      boolean dirty = false;
      if(battery_.getEnergyStored() < transfer_energy_consumption_) return false;
      final BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof ElectricalFurnaceBlock)) return false;
      final Direction out_facing = state.getValue(ElectricalFurnaceBlock.HORIZONTAL_FACING);
      if(out && (!inventory_.getItem(FIFO_OUTPUT_1_SLOT_NO).isEmpty())) {
        BlockEntity te = level.getBlockEntity(worldPosition.relative(out_facing));
        if(te!=null) {
          IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out_facing).orElse(null);
          if(hnd != null) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(hnd, inventory_.getItem(FIFO_OUTPUT_1_SLOT_NO).copy(), false);
            inventory_.setItem(FIFO_OUTPUT_1_SLOT_NO, remaining);
            battery_.draw(transfer_energy_consumption_);
            dirty = true;
          }
        }
      }
      if(with_automatic_inventory_pulling_ || is_accepted_hopper(inventory_.getItem(SMELTING_AUX_SLOT_NO))) {
        final Direction inp_facing = state.getValue(ElectricalFurnaceBlock.HORIZONTAL_FACING).getOpposite();
        if(inp && (inventory_.getItem(FIFO_INPUT_1_SLOT_NO).isEmpty()) && (battery_.getEnergyStored() >= transfer_energy_consumption_)) {
          final int max_count = Mth.clamp((transfer_energy_consumption_ <= 0) ? (64) : (battery_.getEnergyStored()/transfer_energy_consumption_), 1, 64);
          final ItemStack retrieved = Inventories.extract(Inventories.itemhandler(level, worldPosition.relative(inp_facing), inp_facing), null, max_count, false);
          if(!retrieved.isEmpty()) {
            inventory_.setItem(FIFO_INPUT_1_SLOT_NO, retrieved);
            battery_.draw(max_count * transfer_energy_consumption_);
            dirty = true;
          }
        }
      }
      return dirty;
    }

    private int energy_consumption()
    { return energy_consumption(speed_); }

    private int energy_consumption(int speed)
    {
      return switch (speed) {
        case 1 -> energy_consumption_;
        case 2 -> energy_consumption_ * 2;
        case 3 -> energy_consumption_ * 4;
        default -> 0;
      };
    }

    private boolean heat_up(int speed)
    {
      if(!enabled_) return false;
      int p = energy_consumption(speed);
      if((p<=0) || (!battery_.draw(p))) return false;
      burntime_left_ = Math.min(burntime_left_+HEAT_INCREMENT, HEAT_CAPACITY);
      return true; // returns TE dirty
    }

    private void sync_blockstate()
    {
      final BlockState state = level.getBlockState(worldPosition);
      if((state.getBlock() instanceof ElectricalFurnaceBlock) && (state.getValue(ElectricalFurnaceBlock.LIT) != burning())) {
        level.setBlock(worldPosition, state.setValue(ElectricalFurnaceBlock.LIT, burning()), 2);
      }
    }

    ////////////////////
    // Furnace -------------------------------------------------------------------------------------

    @Nullable
    public static <T extends AbstractCookingRecipe> T getSmeltingResult(RecipeType<T> recipe_type, Level world, ItemStack stack)
    {
      if(stack.isEmpty()) return null;
      Container inventory = new SimpleContainer(3);
      inventory.setItem(0, stack);
      return world.getRecipeManager().getRecipeFor(recipe_type, inventory, world).orElse(null);
    }

    public boolean burning()
    { return burntime_left_ > 0; }

    public int getSmeltingTimeNeeded(Level world, ItemStack stack)
    {
      if(stack.isEmpty()) return 0;
      AbstractCookingRecipe recipe = getSmeltingResult(RECIPE_TYPE, world, stack);
      if(recipe == null) return 0;
      int t = recipe.getCookingTime();
      return (t<=0) ? DEFAULT_SMELTING_TIME : t;
    }

    protected boolean canSmeltCurrentItem()
    {
      if((currentRecipe()==null) || (inventory_.getItem(SMELTING_INPUT_SLOT_NO).isEmpty())) return false;
      final ItemStack recipe_result_items = getSmeltingResult(inventory_.getItem(SMELTING_INPUT_SLOT_NO));
      if(recipe_result_items.isEmpty()) return false;
      final ItemStack result_stack = inventory_.getItem(SMELTING_OUTPUT_SLOT_NO);
      if(result_stack.isEmpty()) return true;
      if(!result_stack.sameItem(recipe_result_items)) return false;
      if(result_stack.getCount() + recipe_result_items.getCount() <= inventory_.getMaxStackSize() && result_stack.getCount() + recipe_result_items.getCount() <= result_stack.getMaxStackSize()) return true;
      return result_stack.getCount() + recipe_result_items.getCount() <= recipe_result_items.getMaxStackSize();
    }

    protected void smeltCurrentItem()
    {
      if(!canSmeltCurrentItem()) return;
      final ItemStack smelting_input_stack = inventory_.getItem(SMELTING_INPUT_SLOT_NO);
      final ItemStack recipe_result_items = getSmeltingResult(smelting_input_stack);
      final ItemStack smelting_output_stack = inventory_.getItem(SMELTING_OUTPUT_SLOT_NO);
      final float xp = getCurrentSmeltingXp(smelting_output_stack);
      if(smelting_output_stack.isEmpty()) {
        inventory_.setItem(SMELTING_OUTPUT_SLOT_NO, recipe_result_items.copy());
      } else if(smelting_output_stack.getItem() == recipe_result_items.getItem()) {
        smelting_output_stack.grow(recipe_result_items.getCount());
      }
      smelting_input_stack.shrink(1);
      xp_stored_ += xp;
    }

    public static int getFuelBurntime(Level world, ItemStack stack)
    {
      if(stack.isEmpty()) return 0;
      int t = ForgeHooks.getBurnTime(stack, null);
      return Math.max(t, 0);
    }

    public static boolean isFuel(Level world, ItemStack stack)
    { return (getFuelBurntime(world, stack) > 0) || (stack.getItem()==Items.LAVA_BUCKET); }

    public int consumeSmeltingExperience(ItemStack stack)
    {
      if(xp_stored_ < 1) return 0;
      float xp = xp_stored_;
      if(xp >= 15) xp /= 2;
      xp = Math.min((float)Math.floor(xp), 150);
      xp_stored_ -= xp;
      return (int)xp;
    }

    public ItemStack getSmeltingResult(final ItemStack stack)
    { return (currentRecipe()==null) ? (ItemStack.EMPTY) : (currentRecipe().getResultItem()); }

    public float getCurrentSmeltingXp(final ItemStack stack)
    {
      float xp = (currentRecipe() instanceof AbstractCookingRecipe) ? (((AbstractCookingRecipe)currentRecipe()).getExperience()) : 0;
      return (xp <= 0) ? 0.7f : xp; // default value for recipes without defined xp
    }

    public static boolean canSmelt(Level world, final ItemStack stack)
    { return getSmeltingResult(RECIPE_TYPE, world, stack) != null; }

    @Nullable
    protected Recipe<?> currentRecipe()
    { return current_recipe_; }

    protected void updateCurrentRecipe()
    { setCurrentRecipe(getSmeltingResult(RECIPE_TYPE, getLevel(), inventory_.getItem(SMELTING_INPUT_SLOT_NO))); }

    protected void setCurrentRecipe(Recipe<?> recipe)
    { current_recipe_ = recipe; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
  {

    // Slots --------------------------------------------------------------------------------------------

    public static class OutputSlot extends Slot
    {
      private final Container inventory_;
      private final Player player_;
      private int removeCount = 0;

      public OutputSlot(Player player, Container inventory, int index, int xpos, int ypos)
      { super(inventory, index, xpos, ypos); inventory_ = inventory; player_ = player; }

      @Override
      public boolean mayPlace(ItemStack stack)
      { return false; }

      @Override
      public ItemStack remove(int amount)
      { removeCount += hasItem() ? Math.min(amount, getItem().getCount()) : 0; return super.remove(amount); }

      @Override
      public void onTake(Player thePlayer, ItemStack stack)
      { checkTakeAchievements(stack); super.onTake(thePlayer, stack); }

      @Override
      protected void onQuickCraft(ItemStack stack, int amount)
      { removeCount += amount; checkTakeAchievements(stack); }

      @Override
      protected void checkTakeAchievements(ItemStack stack)
      {
        stack.onCraftedBy(player_.level, player_, removeCount);
        if((!player_.level.isClientSide()) && (inventory_ instanceof StorageInventory) &&
          (((StorageInventory)inventory_).getTileEntity()) instanceof final ElectricalFurnaceTileEntity te) {
          int xp = te.consumeSmeltingExperience(stack);
          while(xp > 0) {
            int k = ExperienceOrb.getExperienceValue(xp);
            xp -= k;
            player_.level.addFreshEntity((new ExperienceOrb(player_.level, player_.blockPosition().getX(), player_.blockPosition().getY()+0.5, player_.blockPosition().getZ()+0.5, k)));
          }
        }
        removeCount = 0;
        ForgeEventFactory.firePlayerSmeltedEvent(player_, stack);
      }
    }

    private static final int PLAYER_INV_START_SLOTNO = 7;
    protected final Player player_;
    protected final Container inventory_;
    protected final ContainerLevelAccess wpc_;
    private final ContainerData fields_;
    private final RecipeType<? extends AbstractCookingRecipe> recipe_type_;

    public int field(int index) { return fields_.get(index); }
    public Player player() { return player_ ; }
    public Container inventory() { return inventory_ ; }
    public Level world() { return player_.level; }

    public ElectricalFurnaceContainer(int cid, Inventory player_inventory)
    { this(cid, player_inventory, new SimpleContainer(ElectricalFurnaceTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(ElectricalFurnaceTileEntity.NUM_OF_FIELDS)); }

    private ElectricalFurnaceContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
    {
      super(ModContent.CT_SMALL_ELECTRICAL_FURNACE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      recipe_type_ = ElectricalFurnaceTileEntity.RECIPE_TYPE;
      addSlot(new Slot(inventory_, 0, 59, 28)); // smelting input
      addSlot(new Slot(inventory_, 1, 16, 52)); // aux
      addSlot(new OutputSlot(player_, inventory_, 2, 101, 28)); // smelting result
      addSlot(new Slot(inventory_, 3, 34, 28)); // input fifo 0
      addSlot(new Slot(inventory_, 4, 16, 28)); // input fifo 1
      addSlot(new OutputSlot(player_, inventory_, 5, 126, 28)); // out fifo 0
      addSlot(new OutputSlot(player_, inventory_, 6, 144, 28)); // out fifo 1
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
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
      if((index==2) || (index==5) || (index==6)) {
        // Output slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onQuickCraft(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if(index==1) {
        // Bypass slot
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(ElectricalFurnaceTileEntity.canSmelt(world(), slot_stack)) {
          if(
            (!moveItemStackTo(slot_stack, 0, 1, false)) && // smelting input
            (!moveItemStackTo(slot_stack, 3, 4, false)) && // fifo0
            (!moveItemStackTo(slot_stack, 4, 5, false))    // fifo1
          ) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO) && (index < PLAYER_INV_START_SLOTNO+27)) {
          // player inventory --> player hotbar
          if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO+27, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO+27) && (index < PLAYER_INV_START_SLOTNO+36) && (!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+27, false))) {
          // player hotbar --> player inventory
          return ItemStack.EMPTY;
        }
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
    { CompoundTag nbt=new CompoundTag(); nbt.putInt(key, value); Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt); }

    @Override
    public void onServerPacketReceived(int windowId, CompoundTag nbt)
    {}

    public void onClientPacketReceived(int windowId, Player player, CompoundTag nbt)
    {
      if(!(inventory_ instanceof StorageInventory)) return;
      if(!((((StorageInventory)inventory_).getTileEntity()) instanceof final ElectricalFurnaceTileEntity te)) return;
      if(nbt.contains("speed")) te.speed_ = Mth.clamp(nbt.getInt("speed"), 0, ElectricalFurnaceTileEntity.MAX_SPEED_SETTING);
      te.setChanged();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class ElectricalFurnaceGui extends Guis.ContainerGui<ElectricalFurnaceContainer>
  {
    public ElectricalFurnaceGui(ElectricalFurnaceContainer container, Inventory player_inventory, Component title)
    { super(container, player_inventory, title, "textures/gui/small_electrical_furnace_gui.png"); }

    @Override
    public void init()
    {
      super.init();
      final String prefix = ModContent.SMALL_ELECTRICAL_FURNACE.getDescriptionId() + ".tooltips.";
      final int x0 = getGuiLeft(), y0 = getGuiTop();
      final Slot aux = menu.getSlot(ElectricalFurnaceTileEntity.SMELTING_AUX_SLOT_NO);
      tooltip_.init(
        new TipRange(x0+135, y0+50, 25, 25, new TranslatableComponent(prefix + "speed")),
        new TipRange(x0+aux.x, y0+aux.y, 16, 16, new TranslatableComponent(prefix + "auxslot")),
        new TipRange(x0+80, y0+55, 50, 14, ()->{
          final int soc = getMenu().field(1) * 100 / Math.max(getMenu().field(5), 1);
          final int consumption = getMenu().field(7);
          return new TranslatableComponent(prefix + "capacitors", soc, consumption);
        })
      );
    }

    @Override
    protected void renderBgWidgets(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {
      final int x0=leftPos, y0=topPos, w=imageWidth, h=imageHeight;
      blit(mx, x0, y0, 0, 0, w, h);
      if(getMenu().field(6)!=0)  {
        final int hi = 13;
        final int k = heat_px(hi);
        blit(mx, x0+62, y0+55+hi-k, 177, hi-k, 13, k);
      }
      blit(mx, x0+79, y0+30, 176, 15, 1+progress_px(17), 15);
      int we = energy_px(32, 8);
      if(we>0) blit(mx, x0+90, y0+54, 185, 30, we, 13);
      switch(getMenu().field(4)) {
        case 0: blit(mx, x0+144, y0+57, 180, 57, 6, 9); break;
        case 1: blit(mx, x0+142, y0+58, 190, 58, 9, 6); break;
        case 2: blit(mx, x0+144, y0+56, 200, 57, 6, 9); break;
        case 3: blit(mx, x0+143, y0+58, 210, 58, 9, 6); break;
        default: break;
      }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      tooltip_.resetTimer();
      ElectricalFurnaceContainer container = getMenu();
      int mx = (int)(mouseX - getGuiLeft() + .5), my = (int)(mouseY - getGuiTop() + .5);
      if((!isHovering(136, 48, 28, 28, mouseX, mouseY))) {
        return super.mouseClicked(mouseX, mouseY, mouseButton);
      } else if(isHovering(144, 64, 6, 10, mouseX, mouseY)) {
        container.onGuiAction("speed", 0);
      } else if(isHovering(134, 58, 10, 6, mouseX, mouseY)) {
        container.onGuiAction("speed", 1);
      } else if(isHovering(144, 48, 6, 10, mouseX, mouseY)) {
        container.onGuiAction("speed", 2);
      } else if(isHovering(150, 58, 10, 6, mouseX, mouseY)) {
        container.onGuiAction("speed", 3);
      }
      return true;
    }

    private int progress_px(int pixels)
    { final int tc=getMenu().field(2), T=getMenu().field(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int heat_px(int pixels)
    {
      int k = ((getMenu().field(0) * (pixels+1)) / (EdElectricalFurnace.ElectricalFurnaceTileEntity.HEAT_CAPACITY));
      return Math.min(k, pixels);
    }

    private int energy_px(int maxwidth, int quantization)
    {
      int emax = getMenu().field(5);
      int k = ((maxwidth * getMenu().field(1) * 9) / 8) / ((emax>0?emax:1)+1);
      k = (k >= maxwidth-2) ? maxwidth : k;
      if(quantization > 0) k = ((k+(quantization/2))/quantization) * quantization;
      return k;
    }
  }

}
