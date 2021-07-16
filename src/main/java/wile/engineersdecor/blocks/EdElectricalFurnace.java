/*
 * @file EdElectricalFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED small electrical pass-through furnace.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.EdFurnace.FurnaceTileEntity;
import wile.engineersdecor.libmc.detail.Inventories.StorageInventory;
import wile.engineersdecor.libmc.detail.Inventories.MappedItemHandler;
import wile.engineersdecor.libmc.detail.TooltipDisplay;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;
import wile.engineersdecor.libmc.client.ContainerGui;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Networking;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;


public class EdElectricalFurnace
{
  public static void on_config(int speed_percent, int standard_energy_per_tick, boolean with_automatic_inventory_pulling)
  { ElectricalFurnaceTileEntity.on_config(speed_percent, standard_energy_per_tick, with_automatic_inventory_pulling); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceBlock extends EdFurnace.FurnaceBlock implements IDecorBlock
  {
    public ElectricalFurnaceBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB[] unrotatedAABBs)
    { super(config, builder, unrotatedAABBs); }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdElectricalFurnace.ElectricalFurnaceTileEntity(); }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof EdElectricalFurnace.ElectricalFurnaceTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      player.awardStat(Stats.INTERACT_WITH_FURNACE);
      return ActionResultType.CONSUME;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      world.setBlockAndUpdate(pos, state.setValue(LIT, false));
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundNBT inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof EdElectricalFurnace.ElectricalFurnaceTileEntity)) return;
      ElectricalFurnaceTileEntity bte = (EdElectricalFurnace.ElectricalFurnaceTileEntity)te;
      bte.readnbt(inventory_nbt);
      bte.setChanged();
      world.setBlockAndUpdate(pos, state.setValue(LIT, bte.burning()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
    {}
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceTileEntity extends EdFurnace.FurnaceTileEntity implements ITickableTileEntity, INameable, INamedContainerProvider
  {
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
    private static final double speed_setting_factor_[] = {0.0, 1.0, 1.5, 2.0};

    public static void on_config(int speed_percent, int standard_energy_per_tick, boolean with_automatic_inventory_pulling)
    {
      proc_speed_percent_ = MathHelper.clamp(speed_percent, 10, 800);
      energy_consumption_ = MathHelper.clamp(standard_energy_per_tick, 4, 4096) * TICK_INTERVAL;
      transfer_energy_consumption_ = MathHelper.clamp(energy_consumption_ / 8, 8, HEAT_INCREMENT);
      with_automatic_inventory_pulling_ = with_automatic_inventory_pulling;
      ModConfig.log("Config electrical furnace speed:" + proc_speed_percent_ + "%, heat-loss: 1K/t, heating consumption:" + (energy_consumption_/TICK_INTERVAL)+"rf/t.");
    }

    // ElectricalFurnaceTileEntity -----------------------------------------------------------------------------

    private int speed_ = 1;
    private boolean enabled_ = false;
    protected int field_power_consumption_;
    protected int power_consumption_accumulator_;
    protected int power_consumption_timer_;
    private final LazyOptional<IItemHandler> item_handler_;

    public ElectricalFurnaceTileEntity()
    { this(ModContent.TET_SMALL_ELECTRICAL_FURNACE); }

    public ElectricalFurnaceTileEntity(TileEntityType<?> te_type)
    {
      super(te_type, NUM_OF_SLOTS);
      battery_.setMaxEnergyStored(MAX_ENERGY_BUFFER);
      battery_.setChargeRate(MAX_ENERGY_TRANSFER);
      battery_.setDischargeRate(0);
      inventory_.setValidator((index, stack)->{
        switch(index) {
          case SMELTING_INPUT_SLOT_NO:
          case FIFO_INPUT_0_SLOT_NO:
          case FIFO_INPUT_1_SLOT_NO:
            return true;
          default:
            return false;
        }
      });
      item_handler_ = MappedItemHandler.createGenericHandler(inventory_,
        (slot,stack)->((slot==FIFO_OUTPUT_0_SLOT_NO) || (slot==FIFO_OUTPUT_1_SLOT_NO)),
        (slot,stack)->((slot==FIFO_INPUT_0_SLOT_NO) || (slot==FIFO_INPUT_1_SLOT_NO)),
        Arrays.asList(FIFO_OUTPUT_0_SLOT_NO,FIFO_OUTPUT_1_SLOT_NO,FIFO_INPUT_0_SLOT_NO,FIFO_INPUT_1_SLOT_NO)
      );
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

    public void readnbt(CompoundNBT nbt)
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

    protected void writenbt(CompoundNBT nbt)
    {
      nbt.putInt("BurnTime", MathHelper.clamp(burntime_left_, 0, HEAT_CAPACITY));
      nbt.putInt("CookTime", MathHelper.clamp((int)proc_time_elapsed_, 0, MAX_BURNTIME));
      nbt.putInt("CookTimeTotal", MathHelper.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      nbt.putInt("SpeedSetting", MathHelper.clamp(speed_, -1, MAX_SPEED_SETTING));
      nbt.putFloat("XpStored", MathHelper.clamp(xp_stored_, 0, MAX_XP_STORED));
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
      energy_handler_.invalidate();
    }

    // INameable -------------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getDescriptionId() : "Small electrical furnace"); }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new EdElectricalFurnace.ElectricalFurnaceContainer(id, inventory, inventory_, IWorldPosCallable.create(level, worldPosition), fields); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(ElectricalFurnaceTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          case 0: return ElectricalFurnaceTileEntity.this.burntime_left_;
          case 1: return ElectricalFurnaceTileEntity.this.battery_.getEnergyStored();
          case 2: return (int)ElectricalFurnaceTileEntity.this.proc_time_elapsed_;
          case 3: return ElectricalFurnaceTileEntity.this.proc_time_needed_;
          case 4: return ElectricalFurnaceTileEntity.this.speed_;
          case 5: return ElectricalFurnaceTileEntity.this.battery_.getMaxEnergyStored();
          case 6: return ElectricalFurnaceTileEntity.this.field_is_burning_;
          case 7: return ElectricalFurnaceTileEntity.this.field_power_consumption_;
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0: ElectricalFurnaceTileEntity.this.burntime_left_ = value; break;
          case 1: ElectricalFurnaceTileEntity.this.battery_.setEnergyStored(value); break;
          case 2: ElectricalFurnaceTileEntity.this.proc_time_elapsed_ = value; break;
          case 3: ElectricalFurnaceTileEntity.this.proc_time_needed_ = value; break;
          case 4: ElectricalFurnaceTileEntity.this.speed_ = value; break;
          case 5: ElectricalFurnaceTileEntity.this.battery_.setMaxEnergyStored(value); break;
          case 6: ElectricalFurnaceTileEntity.this.field_is_burning_ = value; break;
          case 7: ElectricalFurnaceTileEntity.this.field_power_consumption_ = value; break;
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

    // ITickableTileEntity -------------------------------------------------------------------------

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
        IRecipe<?> last_recipe = currentRecipe();
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
          final int speed = MathHelper.clamp((battery_.getSOC() >= 25) ? (speed_) : (1), 0, MAX_SPEED_SETTING);
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
        TileEntity te = level.getBlockEntity(worldPosition.relative(out_facing));
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
          final int max_count = MathHelper.clamp((transfer_energy_consumption_ <= 0) ? (64) : (battery_.getEnergyStored()/transfer_energy_consumption_), 1, 64);
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
      switch(speed) {
        case 1: return energy_consumption_;
        case 2: return energy_consumption_ * 2;
        case 3: return energy_consumption_ * 4;
        default: return 0;
      }
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

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class ElectricalFurnaceContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    private static final int PLAYER_INV_START_SLOTNO = 7;
    protected final PlayerEntity player_;
    protected final IInventory inventory_;
    protected final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final IRecipeType<? extends AbstractCookingRecipe> recipe_type_;

    public int field(int index) { return fields_.get(index); }
    public PlayerEntity player() { return player_ ; }
    public IInventory inventory() { return inventory_ ; }
    public World world() { return player_.level; }

    public ElectricalFurnaceContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(ElectricalFurnaceTileEntity.NUM_OF_SLOTS), IWorldPosCallable.NULL, new IntArray(ElectricalFurnaceTileEntity.NUM_OF_FIELDS)); }

    private ElectricalFurnaceContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_SMALL_ELECTRICAL_FURNACE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      recipe_type_ = FurnaceTileEntity.RECIPE_TYPE;
      addSlot(new Slot(inventory_, 0, 59, 28)); // smelting input
      addSlot(new Slot(inventory_, 1, 16, 52)); // aux
      addSlot(new EdFurnace.FurnaceContainer.BSlotResult(player_, inventory_, 2, 101, 28)); // smelting result
      addSlot(new EdFurnace.FurnaceContainer.BSlotInpFifo(inventory_, 3, 34, 28)); // input fifo 0
      addSlot(new EdFurnace.FurnaceContainer.BSlotInpFifo(inventory_, 4, 16, 28)); // input fifo 1
      addSlot(new EdFurnace.FurnaceContainer.BSlotOutFifo(player_, inventory_, 5, 126, 28)); // out fifo 0
      addSlot(new EdFurnace.FurnaceContainer.BSlotOutFifo(player_, inventory_, 6, 144, 28)); // out fifo 1
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
    public boolean stillValid(PlayerEntity player)
    { return inventory_.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index)
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
    public void onGuiAction(CompoundNBT nbt)
    { Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt); }

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String key, int value)
    { CompoundNBT nbt=new CompoundNBT(); nbt.putInt(key, value); Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt); }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      if(!(inventory_ instanceof StorageInventory)) return;
      ElectricalFurnaceTileEntity te = (ElectricalFurnaceTileEntity)(((StorageInventory)inventory_).getTileEntity());
      if(nbt.contains("speed")) te.speed_ = MathHelper.clamp(nbt.getInt("speed"), 0, ElectricalFurnaceTileEntity.MAX_SPEED_SETTING);
      te.setChanged();
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class ElectricalFurnaceGui extends ContainerGui<ElectricalFurnaceContainer>
  {
    protected final PlayerEntity player_;
    protected final TooltipDisplay tooltip_ = new TooltipDisplay();

    public ElectricalFurnaceGui(ElectricalFurnaceContainer container, PlayerInventory player_inventory, ITextComponent title)
    { super(container, player_inventory, title); this.player_ = player_inventory.player; }

    @Override
    public void init()
    {
      super.init();
      final String prefix = ModContent.SMALL_ELECTRICAL_FURNACE.getDescriptionId() + ".tooltips.";
      final int x0 = getGuiLeft(), y0 = getGuiTop();
      final Slot aux = menu.getSlot(ElectricalFurnaceTileEntity.SMELTING_AUX_SLOT_NO);
      tooltip_.init(
        new TipRange(x0+135, y0+50, 25, 25, new TranslationTextComponent(prefix + "speed")),
        new TipRange(x0+aux.x, y0+aux.y, 16, 16, new TranslationTextComponent(prefix + "auxslot")),
        new TipRange(x0+80, y0+55, 50, 14, ()->{
          final int soc = getMenu().field(1) * 100 / Math.max(getMenu().field(5), 1);
          final int consumption = getMenu().field(7);
          return new TranslationTextComponent(prefix + "capacitors", soc, consumption);
        })
      );
    }

    @Override
    public void render(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
    {
      renderBackground(mx);
      super.render(mx, mouseX, mouseY, partialTicks);
      if(!tooltip_.render(mx, this, mouseX, mouseY)) renderTooltip(mx, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(MatrixStack mx, int x, int y)
    {}

    @Override
    @SuppressWarnings("deprecation")
    protected void renderBg(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.enableBlend();
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      getMinecraft().getTextureManager().bind(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_electrical_furnace_gui.png"));
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
      RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      tooltip_.resetTimer();
      ElectricalFurnaceContainer container = (ElectricalFurnaceContainer)getMenu();
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
