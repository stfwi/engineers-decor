/*
 * @file EdFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED Lab furnace.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Inventories.MappedItemHandler;
import wile.engineersdecor.libmc.detail.Inventories.StorageInventory;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.libmc.detail.RfEnergy;
import wile.engineersdecor.libmc.ui.Guis;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class EdFurnace
{
  public static void on_config(int speed_percent, int fuel_efficiency_percent, int boost_energy_per_tick, String accepted_heaters_csv)
  { FurnaceTileEntity.on_config(speed_percent, fuel_efficiency_percent, boost_energy_per_tick, accepted_heaters_csv); }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<FurnaceTileEntity>
  {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FurnaceBlock(long config, BlockBehaviour.Properties properties, final AABB[] unrotatedAABB)
    { super(config, properties, unrotatedAABB); registerDefaultState(super.defaultBlockState().setValue(LIT, false)); }

    @Override
    @Nullable
    public BlockEntityType<EdFurnace.FurnaceTileEntity> getBlockEntityType()
    { return ModContent.TET_SMALL_LAB_FURNACE; }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

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
      return (te instanceof FurnaceTileEntity) ? ((FurnaceTileEntity)te).getComparatorOutput() : 0;
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      world.setBlockAndUpdate(pos, state.setValue(LIT, false));
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundTag inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof final FurnaceTileEntity bte)) return;
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
      if(!(te instanceof FurnaceTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundTag inventory_nbt = ((FurnaceTileEntity)te).reset_getnbt();
        if(!inventory_nbt.isEmpty()) {
          CompoundTag nbt = new CompoundTag();
          nbt.put("inventory", inventory_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((FurnaceTileEntity)te).inventory_) stacks.add(stack);
        ((FurnaceTileEntity)te).reset();
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
      final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
      if(rv < 0.1d) world.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, 0.5f, false);
      switch(state.getValue(HORIZONTAL_FACING)) {
        case WEST -> world.addParticle(ParticleTypes.SMOKE, x - xc, yr, z + xr, 0.0, 0.0, 0.0);
        case EAST -> world.addParticle(ParticleTypes.SMOKE, x + xc, yr, z + xr, 0.0, 0.0, 0.0);
        case NORTH -> world.addParticle(ParticleTypes.SMOKE, x + xr, yr, z - xc, 0.0, 0.0, 0.0);
        default -> world.addParticle(ParticleTypes.SMOKE, x + xr, yr, z + xc, 0.0, 0.0, 0.0);
      }
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceTileEntity extends StandardEntityBlocks.StandardBlockEntity implements MenuProvider, Nameable
  {
    private static final RecipeType<SmeltingRecipe> RECIPE_TYPE = RecipeType.SMELTING;
    private static final int MAX_BURNTIME = 0x7fff;
    private static final int MAX_XP_STORED = 65535;
    private static final int NUM_OF_FIELDS = 5;
    private static final int TICK_INTERVAL = 4;
    private static final int FIFO_INTERVAL = 20;
    private static final int DEFAULT_SMELTING_TIME = 200;
    private static final int DEFAULT_BOOST_ENERGY = 32;
    private static final int NUM_OF_SLOTS = 11;
    private static final int SMELTING_INPUT_SLOT_NO  = 0;
    private static final int SMELTING_FUEL_SLOT_NO   = 1;
    private static final int SMELTING_OUTPUT_SLOT_NO = 2;
    private static final int FIFO_INPUT_0_SLOT_NO    = 3;
    private static final int FIFO_INPUT_1_SLOT_NO    = 4;
    private static final int FIFO_FUEL_0_SLOT_NO     = 5;
    private static final int FIFO_FUEL_1_SLOT_NO     = 6;
    private static final int FIFO_OUTPUT_0_SLOT_NO   = 7;
    private static final int FIFO_OUTPUT_1_SLOT_NO   = 8;
    private static final int AUX_0_SLOT_NO           = 9;
    private static final int AUX_1_SLOT_NO           =10;

    // Config ----------------------------------------------------------------------------------

    private static double proc_fuel_efficiency_ = 1.0;
    private static double proc_speed_ = 1.2;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY * TICK_INTERVAL;
    private static final Set<Item> accepted_heaters_ = new HashSet<>();

    public static void on_config(int speed_percent, int fuel_efficiency_percent, int boost_energy_per_tick, String accepted_heaters_csv)
    {
      proc_speed_ = ((double)Mth.clamp(speed_percent, 10, 500)) / 100;
      proc_fuel_efficiency_ = ((double) Mth.clamp(fuel_efficiency_percent, 10, 500)) / 100;
      boost_energy_consumption = TICK_INTERVAL * Mth.clamp(boost_energy_per_tick, 4, 4096);
      {
        List<String> heater_resource_locations = Arrays.stream(accepted_heaters_csv.toLowerCase().split("[\\s,;]+"))
          .map(String::trim)
          .collect(Collectors.toList());
        accepted_heaters_.clear();
        for(String rlstr: heater_resource_locations) {
          try {
            ResourceLocation rl = new ResourceLocation(rlstr);
            Item heater = ForgeRegistries.ITEMS.getValue(rl);
            if((heater==null) || (heater==Items.AIR)) {
              ModConfig.log("Furnace accepted heater config: Skipped '" + rl + "', item not found/mod not installed.");
            } else {
              accepted_heaters_.add(heater);
            }
          } catch(Throwable e) {
            Auxiliaries.logError("Furnace accepted heater config invalid: '" + rlstr + "', not added.");
          }
        }
      }
      ModConfig.log("Config lab furnace speed:" + (proc_speed_*100) + "%, efficiency:" + (proc_fuel_efficiency_*100) + "%, boost: " + (boost_energy_consumption/TICK_INTERVAL) + "rf/t.");
      ModConfig.log("Config lab furnace accepted heaters: " + accepted_heaters_.stream().map(item->item.getRegistryName().toString()).collect(Collectors.joining(","))   + ".");
    }

    // DecorFurnaceTileEntity -----------------------------------------------------------------------------

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
    private boolean heater_inserted_ = false;
    private final StorageInventory inventory_;
    private final LazyOptional<IItemHandler> item_extraction_handler_;
    private final LazyOptional<IItemHandler> item_insertion_handler_;
    private final LazyOptional<IItemHandler> item_fuel_insertion_handler_;
    private final RfEnergy.Battery battery_ = new RfEnergy.Battery(boost_energy_consumption * 16, boost_energy_consumption, 0);
    private final LazyOptional<IEnergyStorage> energy_handler_ = battery_.createEnergyHandler();

    public FurnaceTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.TET_SMALL_LAB_FURNACE, pos, state);
      inventory_ = new StorageInventory(this, NUM_OF_SLOTS) {
        @Override
        public void setItem(int index, ItemStack stack)
        {
          ItemStack slot_stack = stacks_.get(index);
          boolean already_in_slot = (!stack.isEmpty()) && (Inventories.areItemStacksIdentical(stack, slot_stack));
          stacks_.set(index, stack);
          if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
          if((index == SMELTING_INPUT_SLOT_NO) && (!already_in_slot)) {
            proc_time_needed_ = getSmeltingTimeNeeded(level, stack);
            proc_time_elapsed_ = 0;
            setChanged();
          }
        }
      };
      inventory_.setValidator((index, stack)->{
        // applies to gui and handlers
        switch(index) {
          case SMELTING_OUTPUT_SLOT_NO:
          case FIFO_OUTPUT_0_SLOT_NO:
          case FIFO_OUTPUT_1_SLOT_NO:
            return false;
          case SMELTING_INPUT_SLOT_NO:
          case FIFO_INPUT_0_SLOT_NO:
          case FIFO_INPUT_1_SLOT_NO:
            return true;
          case AUX_0_SLOT_NO:
          case AUX_1_SLOT_NO:
            return true;
          default: {
            ItemStack slot_stack = inventory_.getItem(FIFO_FUEL_1_SLOT_NO);
            return isFuel(level, stack) || FurnaceFuelSlot.isBucket(stack) && (slot_stack.getItem() != Items.BUCKET);
          }
        }
      });
      item_extraction_handler_ = MappedItemHandler.createExtractionHandler(inventory_,
        (slot,stack)->(slot!=SMELTING_FUEL_SLOT_NO) || (stack.getItem()==Items.BUCKET) || (!isFuel(getLevel(), stack)),
        Arrays.asList(FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO, SMELTING_FUEL_SLOT_NO)
      );
      item_insertion_handler_ = MappedItemHandler.createInsertionHandler(inventory_,
        FIFO_INPUT_1_SLOT_NO,FIFO_INPUT_0_SLOT_NO,SMELTING_INPUT_SLOT_NO
      );
      item_fuel_insertion_handler_ = MappedItemHandler.createInsertionHandler(inventory_,
        FIFO_FUEL_1_SLOT_NO,FIFO_FUEL_0_SLOT_NO,SMELTING_FUEL_SLOT_NO
      );
    }

    public CompoundTag reset_getnbt()
    {
      CompoundTag nbt = new CompoundTag();
      writenbt(nbt);
      reset();
      return nbt;
    }

    public void reset()
    {
      inventory_.clearContent();
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      burntime_left_ = 0;
      fuel_burntime_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
      xp_stored_ = 0;
      current_recipe_ = null;
    }

    public void readnbt(CompoundTag nbt)
    {
      burntime_left_ = nbt.getInt("BurnTime");
      proc_time_elapsed_ = nbt.getInt("CookTime");
      proc_time_needed_ = nbt.getInt("CookTimeTotal");
      fuel_burntime_ = nbt.getInt("FuelBurnTime");
      xp_stored_ = nbt.getFloat("XpStored");
      battery_.load(nbt, "Energy");
      inventory_.load(nbt);
    }

    protected void writenbt(CompoundTag nbt)
    {
      nbt.putInt("BurnTime", Mth.clamp(burntime_left_,0 , MAX_BURNTIME));
      nbt.putInt("CookTime", Mth.clamp((int)proc_time_elapsed_, 0, MAX_BURNTIME));
      nbt.putInt("CookTimeTotal", Mth.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      nbt.putInt("FuelBurnTime", Mth.clamp(fuel_burntime_, 0, MAX_BURNTIME));
      nbt.putFloat("XpStored", Mth.clamp(xp_stored_, 0, MAX_XP_STORED));
      battery_.save(nbt, "Energy");
      inventory_.save(nbt);
    }

    public int getComparatorOutput()
    {
      if(inventory_.getItem(FIFO_FUEL_0_SLOT_NO).isEmpty() && inventory_.getItem(FIFO_FUEL_1_SLOT_NO).isEmpty() && inventory_.getItem(SMELTING_FUEL_SLOT_NO).isEmpty()) {
        return 0; // fuel completely empty
      } else {
        return (
          (inventory_.getItem(FIFO_INPUT_1_SLOT_NO).isEmpty() ? 0 : 5) +
          (inventory_.getItem(FIFO_INPUT_0_SLOT_NO).isEmpty() ? 0 : 5) +
          (inventory_.getItem(SMELTING_INPUT_SLOT_NO).isEmpty() ? 0 : 5)
        );
      }
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
      item_extraction_handler_.invalidate();
      item_insertion_handler_.invalidate();
      item_fuel_insertion_handler_.invalidate();
      energy_handler_.invalidate();
    }

    // INamedContainerProvider / Nameable ------------------------------------------------------

    @Override
    public Component getName()
    { final Block block=getBlockState().getBlock(); return new TextComponent((block!=null) ? block.getDescriptionId() : "Lab furnace"); }

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
    { return new FurnaceContainer(id, inventory, inventory_, ContainerLevelAccess.create(level, worldPosition), fields); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final ContainerData fields = new ContainerData()
    {
      @Override
      public int getCount()
      { return FurnaceTileEntity.NUM_OF_FIELDS; }

      @Override
      public int get(int id)
      {
        return switch(id) {
          case 0 -> FurnaceTileEntity.this.burntime_left_;
          case 1 -> FurnaceTileEntity.this.fuel_burntime_;
          case 2 -> FurnaceTileEntity.this.field_proc_time_elapsed_;
          case 3 -> FurnaceTileEntity.this.proc_time_needed_;
          case 4 -> FurnaceTileEntity.this.field_is_burning_;
          default -> 0;
        };
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0 -> FurnaceTileEntity.this.burntime_left_ = value;
          case 1 -> FurnaceTileEntity.this.fuel_burntime_ = value;
          case 2 -> FurnaceTileEntity.this.field_proc_time_elapsed_ = value;
          case 3 -> FurnaceTileEntity.this.proc_time_needed_ = value;
          case 4 -> FurnaceTileEntity.this.field_is_burning_ = value;
        }
      }
    };

    // Capability export ----------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
        if(facing == Direction.UP) return item_insertion_handler_.cast();
        if(facing == Direction.DOWN) return item_extraction_handler_.cast();
        return item_fuel_insertion_handler_.cast();
      } else if(capability== CapabilityEnergy.ENERGY) {
        return energy_handler_.cast();
      }
      return super.getCapability(capability, facing);
    }

    // ITickableTileEntity -------------------------------------------------------------------------

    @Override
    public void tick()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof FurnaceBlock)) return;
      final boolean was_burning = burning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(level.isClientSide) return;
      boolean dirty = false;
      if(--fifo_timer_ <= 0) {
        fifo_timer_ = FIFO_INTERVAL/TICK_INTERVAL;
        // note, intentionally not using bitwise OR piping.
        if(transferItems(FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO, 1)) dirty = true;
        if(transferItems(SMELTING_OUTPUT_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_FUEL_0_SLOT_NO, SMELTING_FUEL_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_FUEL_1_SLOT_NO, FIFO_FUEL_0_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_INPUT_0_SLOT_NO, SMELTING_INPUT_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_INPUT_1_SLOT_NO, FIFO_INPUT_0_SLOT_NO, 1)) dirty = true;
        heater_inserted_ = accepted_heaters_.isEmpty() // without IE always allow electrical boost
          || accepted_heaters_.contains(inventory_.getItem(AUX_0_SLOT_NO).getItem())
          || accepted_heaters_.contains(inventory_.getItem(AUX_1_SLOT_NO).getItem());
      }
      ItemStack fuel = inventory_.getItem(SMELTING_FUEL_SLOT_NO);
      if(burning() || (!fuel.isEmpty()) && (!(inventory_.getItem(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
        Recipe<?> last_recipe = currentRecipe();
        updateCurrentRecipe();
        if(currentRecipe() != last_recipe) {
          proc_time_elapsed_ = 0;
          proc_time_needed_ = getSmeltingTimeNeeded(level, inventory_.getItem(SMELTING_INPUT_SLOT_NO));
        }
        if(!burning() && canSmeltCurrentItem()) {
          burntime_left_ = (int)Mth.clamp((proc_fuel_efficiency_ * getFuelBurntime(level, fuel)), 0, MAX_BURNTIME);
          fuel_burntime_ = (int)Mth.clamp(((double)burntime_left_)/((proc_speed_ > 0) ? proc_speed_ : 1), 1, MAX_BURNTIME);
          if(burning()) {
            dirty = true;
            if(!fuel.isEmpty()) {
              Item fuel_item = fuel.getItem();
              fuel.shrink(1);
              if(fuel.isEmpty()) inventory_.setItem(SMELTING_FUEL_SLOT_NO, fuel_item.getContainerItem(fuel));
            }
          }
        }
        if(burning() && canSmeltCurrentItem()) {
          proc_time_elapsed_ += TICK_INTERVAL * proc_speed_;
          if(heater_inserted_ && battery_.draw(boost_energy_consumption)) {
            proc_time_elapsed_ += (TICK_INTERVAL * proc_speed_) * 2;
          }
          if(proc_time_elapsed_ >= proc_time_needed_) {
            proc_time_elapsed_ = 0;
            proc_time_needed_ = getSmeltingTimeNeeded(level, inventory_.getItem(SMELTING_INPUT_SLOT_NO));
            smeltCurrentItem();
            dirty = true;
          }
        } else {
          proc_time_elapsed_ = 0;
        }
      } else if((!burning()) && (proc_time_elapsed_ > 0)) {
        proc_time_elapsed_ = Mth.clamp(proc_time_elapsed_-2, 0, proc_time_needed_);
      }
      if(was_burning != burning()) {
        dirty = true;
        level.setBlockAndUpdate(worldPosition, state.setValue(FurnaceBlock.LIT, burning()));
      }
      if(dirty) {
        setChanged();
      }
      field_is_burning_ = this.burning() ? 1 : 0;
      field_proc_time_elapsed_ = (int)proc_time_elapsed_;
    }

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
      float xp = (currentRecipe() instanceof AbstractCookingRecipe) ? (((AbstractCookingRecipe)currentRecipe()).getExperience()) : (0);
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
  // Container slots
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceContainer extends AbstractContainerMenu implements Networking.INetworkSynchronisableContainer
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
          ((((StorageInventory)inventory_).getTileEntity()) instanceof final FurnaceTileEntity te)
        ) {
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

    public static class FuelSlot extends Slot
    {
      private final FurnaceContainer container_;

      public FuelSlot(Container inventory, int index, int xpos, int ypos, FurnaceContainer container)
      { super(inventory, index, xpos, ypos); container_=container; }

      @Override
      public boolean mayPlace(ItemStack stack)
      { return isBucket(stack) || (FurnaceTileEntity.isFuel(container_.world(), stack)); }

      @Override
      public int getMaxStackSize(ItemStack stack)
      { return isBucket(stack) ? 1 : super.getMaxStackSize(stack); }

      protected static boolean isBucket(ItemStack stack)
      { return (stack.getItem()==Items.BUCKET); }
    }

    // Container ----------------------------------------------------------------------------------------

    private static final int PLAYER_INV_START_SLOTNO = 11;
    protected final Player player_;
    protected final Container inventory_;
    protected final ContainerLevelAccess wpc_;
    private final ContainerData fields_;
    private final RecipeType<? extends AbstractCookingRecipe> recipe_type_;

    public int field(int index) { return fields_.get(index); }
    public Player player() { return player_ ; }
    public Container inventory() { return inventory_ ; }
    public Level world() { return player_.level; }

    public FurnaceContainer(int cid, Inventory player_inventory)
    { this(cid, player_inventory, new SimpleContainer(FurnaceTileEntity.NUM_OF_SLOTS), ContainerLevelAccess.NULL, new SimpleContainerData(FurnaceTileEntity.NUM_OF_FIELDS)); }

    private FurnaceContainer(int cid, Inventory player_inventory, Container block_inventory, ContainerLevelAccess wpc, ContainerData fields)
    {
      super(ModContent.CT_SMALL_LAB_FURNACE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      recipe_type_ = FurnaceTileEntity.RECIPE_TYPE;
      addSlot(new Slot(inventory_, 0, 59, 17)); // smelting input
      addSlot(new FuelSlot(inventory_, 1, 59, 53, this)); // fuel
      addSlot(new OutputSlot(player_, inventory_, 2, 101, 35)); // smelting result
      addSlot(new Slot(inventory_, 3, 34, 17)); // input fifo 0
      addSlot(new Slot(inventory_, 4, 16, 17)); // input fifo 1
      addSlot(new Slot(inventory_, 5, 34, 53)); // fuel fifo 0
      addSlot(new Slot(inventory_, 6, 16, 53)); // fuel fifo 1
      addSlot(new OutputSlot(player_inventory.player, inventory_, 7, 126, 35)); // out fifo 0
      addSlot(new OutputSlot(player_inventory.player, inventory_, 8, 144, 35)); // out fifo 1
      addSlot(new Slot(inventory_,  9, 126, 61)); // aux slot 1
      addSlot(new Slot(inventory_, 10, 144, 61)); // aux slot 2
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
      if((index==2) || (index==7) || (index==8)) {
        // Output slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onQuickCraft(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==1) || (index==5) || (index==6)) {
        // Fuel slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==9) || (index==10)) {
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(FurnaceTileEntity.canSmelt(world(), slot_stack)) {
          if(
            (!moveItemStackTo(slot_stack, 0, 1, false)) && // smelting input
            (!moveItemStackTo(slot_stack, 3, 4, false)) && // fifo0
            (!moveItemStackTo(slot_stack, 4, 5, false))    // fifo1
          ) return ItemStack.EMPTY;
        } else if(FurnaceTileEntity.isFuel(player_.level, slot_stack)) {
          if(
            (!moveItemStackTo(slot_stack, 1, 2, false)) && // fuel input
            (!moveItemStackTo(slot_stack, 5, 6, false)) && // fuel fifo0
            (!moveItemStackTo(slot_stack, 6, 7, false))    // fuel fifo1
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
      //if(!player.world.isRemote) detectAndSendChanges();
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

    @Override
    public void onServerPacketReceived(int windowId, CompoundTag nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, Player player, CompoundTag nbt)
    {}

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class FurnaceGui extends Guis.ContainerGui<FurnaceContainer>
  {
    public FurnaceGui(FurnaceContainer container, Inventory player_inventory, Component title)
    { super(container, player_inventory, title, "textures/gui/small_lab_furnace_gui.png"); }

    @Override
    protected void renderBgWidgets(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {
      final int x0=leftPos, y0=topPos, w=imageWidth, h=imageHeight;
      if(getMenu().field(4) != 0) {
        final int k = flame_px(13);
        blit(mx, x0+59, y0+36+12-k, 176, 12-k, 14, k+1);
      }
      blit(mx, x0+79, y0+36, 176, 15, 1+progress_px(17), 15);
    }

    private int progress_px(int pixels)
    { final int tc=getMenu().field(2), T=getMenu().field(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int flame_px(int pixels)
    { int ibt = getMenu().field(1); return ((getMenu().field(0) * pixels) / ((ibt>0) ? (ibt) : (FurnaceTileEntity.DEFAULT_SMELTING_TIME))); }
  }

}
