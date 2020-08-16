/*
 * @file EdFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED Lab furnace.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ExternalObjects;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Networking;
import net.minecraft.tileentity.*;
import net.minecraft.inventory.container.*;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.Items;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import java.util.*;


public class EdFurnace
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceBlock extends DecorBlock.Horizontal implements IDecorBlock
  {
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    public FurnaceBlock(long config, Block.Properties properties, final AxisAlignedBB[] unrotatedAABB)
    { super(config, properties, unrotatedAABB); setDefaultState(super.getDefaultState().with(LIT, false)); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(LIT); }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos)
    { return state.get(LIT) ? super.getLightValue(state, world, pos) : 0; }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).with(LIT, false); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos)
    {
      TileEntity te = world.getTileEntity(pos);
      return (te instanceof FurnaceTileEntity) ? ((FurnaceTileEntity)te).getComparatorOutput() : 0;
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new FurnaceTileEntity(); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      world.setBlockState(pos, state.with(LIT, false));
      if(world.isRemote) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("inventory"))) return;
      CompoundNBT inventory_nbt = stack.getTag().getCompound("inventory");
      if(inventory_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof FurnaceTileEntity)) return;
      final FurnaceTileEntity bte = ((FurnaceTileEntity)te);
      bte.readnbt(inventory_nbt);
      bte.markDirty();
      world.setBlockState(pos, state.with(LIT, bte.burning()));
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, final TileEntity te, boolean explosion) {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isRemote) return stacks;
      if(!(te instanceof FurnaceTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT inventory_nbt = ((FurnaceTileEntity)te).reset_getnbt();
        if(!inventory_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("inventory", inventory_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((FurnaceTileEntity)te).stacks_) stacks.add(stack);
        ((FurnaceTileEntity)te).reset();
      }
      return stacks;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return ActionResultType.SUCCESS;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof FurnaceTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      player.addStat(Stats.INTERACT_WITH_FURNACE);
      return ActionResultType.SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random rnd)
    {
      if((state.getBlock()!=this) || (!state.get(LIT))) return;
      final double rv = rnd.nextDouble();
      if(rv > 0.5) return;
      final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
      final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
      if(rv < 0.1d) world.playSound(x, y, z, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 0.4f, 0.5f, false);
      switch(state.get(HORIZONTAL_FACING)) {
        case WEST:  world.addParticle(ParticleTypes.SMOKE, x-xc, yr, z+xr, 0.0, 0.0, 0.0); break;
        case EAST:  world.addParticle(ParticleTypes.SMOKE, x+xc, yr, z+xr, 0.0, 0.0, 0.0); break;
        case NORTH: world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z-xc, 0.0, 0.0, 0.0); break;
        default:    world.addParticle(ParticleTypes.SMOKE, x+xr, yr, z+xc, 0.0, 0.0, 0.0); break;
      }
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider, ISidedInventory, IEnergyStorage
  {
    public static final IRecipeType<FurnaceRecipe> RECIPE_TYPE = IRecipeType.SMELTING;
    public static final int NUM_OF_FIELDS = 5;
    public static final int TICK_INTERVAL = 4;
    public static final int FIFO_INTERVAL = 20;
    public static final int STD_SMELTING_TIME = 200;
    public static final int MAX_BURNTIME = 0x7fff;
    public static final int DEFAULT_BOOST_ENERGY = 32;
    public static final int NUM_OF_SLOTS = 11;
    public static final int SMELTING_INPUT_SLOT_NO  = 0;
    public static final int SMELTING_FUEL_SLOT_NO   = 1;
    public static final int SMELTING_OUTPUT_SLOT_NO = 2;
    public static final int FIFO_INPUT_0_SLOT_NO    = 3;
    public static final int FIFO_INPUT_1_SLOT_NO    = 4;
    public static final int FIFO_FUEL_0_SLOT_NO     = 5;
    public static final int FIFO_FUEL_1_SLOT_NO     = 6;
    public static final int FIFO_OUTPUT_0_SLOT_NO   = 7;
    public static final int FIFO_OUTPUT_1_SLOT_NO   = 8;
    public static final int AUX_0_SLOT_NO           = 9;
    public static final int AUX_1_SLOT_NO           =10;

    // Config ----------------------------------------------------------------------------------

    private static double proc_fuel_efficiency_ = 1.0;
    private static double proc_speed_ = 1.2;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY * TICK_INTERVAL;

    public static void on_config(int speed_percent, int fuel_efficiency_percent, int boost_energy_per_tick)
    {
      proc_speed_ = ((double)MathHelper.clamp(speed_percent, 10, 500)) / 100;
      proc_fuel_efficiency_ = ((double) MathHelper.clamp(fuel_efficiency_percent, 10, 500)) / 100;
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 4, 4096);
      ModEngineersDecor.logger().info("Config lab furnace speed:" + (proc_speed_*100) + "%, efficiency:" + (proc_fuel_efficiency_*100) + "%");
    }

    // DecorFurnaceTileEntity -----------------------------------------------------------------------------

    private int tick_timer_;
    private int fifo_timer_;
    private int burntime_left_;
    private int fuel_burntime_;
    private double proc_time_elapsed_;
    private int proc_time_needed_;
    private int field_is_burning_;
    private int field_proc_time_elapsed_;

    private int boost_energy_; // small, not saved in nbt.
    private boolean heater_inserted_ = false;
    protected NonNullList<ItemStack> stacks_;
    protected @Nullable IRecipe current_recipe_ = null;
    private final List<String> recent_recipes_ = new ArrayList<>();

    public FurnaceTileEntity()
    { this(ModContent.TET_SMALL_LAB_FURNACE); }

    public FurnaceTileEntity(TileEntityType<?> te_type)
    { super(te_type); reset(); }

    public CompoundNBT reset_getnbt()
    {
      CompoundNBT nbt = new CompoundNBT();
      writenbt(nbt);
      reset();
      return nbt;
    }

    public void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      burntime_left_ = 0;
      fuel_burntime_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
      current_recipe_ = null;
    }

    public void readnbt(CompoundNBT nbt)
    {
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      burntime_left_ = nbt.getInt("BurnTime");
      proc_time_elapsed_ = nbt.getInt("CookTime");
      proc_time_needed_ = nbt.getInt("CookTimeTotal");
      fuel_burntime_ = nbt.getInt("FuelBurnTime");
      CompoundNBT rr = nbt.getCompound("Recipes");
      for(int i=0; i<rr.size(); ++i) {
        String recipe_id = rr.getString(Integer.toString(i));
        if(recipe_id.isEmpty()) break; // no further processing, nbt data set broken.
        recent_recipes_.add(recipe_id);
      }
    }

    private void writenbt(CompoundNBT nbt)
    {
      nbt.putInt("BurnTime", MathHelper.clamp(burntime_left_,0 , MAX_BURNTIME));
      nbt.putInt("CookTime", MathHelper.clamp((int)proc_time_elapsed_, 0, MAX_BURNTIME));
      nbt.putInt("CookTimeTotal", MathHelper.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      nbt.putInt("FuelBurnTime", MathHelper.clamp(fuel_burntime_, 0, MAX_BURNTIME));
      ItemStackHelper.saveAllItems(nbt, stacks_);
      CompoundNBT rr = new CompoundNBT();
      for(int i=0; i<recent_recipes_.size(); ++i) rr.putString(Integer.toString(i), recent_recipes_.get(i));
      nbt.put("Recipes", rr);
    }

    public int getComparatorOutput()
    {
      if(stacks_.get(FIFO_FUEL_0_SLOT_NO).isEmpty() && stacks_.get(FIFO_FUEL_1_SLOT_NO).isEmpty() && stacks_.get(SMELTING_FUEL_SLOT_NO).isEmpty()) {
        return 0; // fuel completely empty
      } else {
        return (
          (stacks_.get(FIFO_INPUT_1_SLOT_NO).isEmpty() ? 0 : 5) +
          (stacks_.get(FIFO_INPUT_0_SLOT_NO).isEmpty() ? 0 : 5) +
          (stacks_.get(SMELTING_INPUT_SLOT_NO).isEmpty() ? 0 : 5)
        );
      }
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void func_230337_a_(BlockState state, CompoundNBT nbt)
    { super.func_230337_a_(state, nbt); readnbt(nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      Arrays.stream(item_handlers).forEach(LazyOptional::invalidate);
      energy_handler_.invalidate();
    }

    // INamedContainerProvider / INameable ------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Lab furnace"); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public ITextComponent getCustomName()
    { return getName(); }

    // IContainerProvider ----------------------------------------------------------------------

    @Override
    public ITextComponent getDisplayName()
    { return INameable.super.getDisplayName(); }

    @Override
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new FurnaceContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

    // IInventory ------------------------------------------------------------------------------

    @Override
    public int getSizeInventory()
    { return stacks_.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return (index < getSizeInventory()) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks_, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks_, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
      ItemStack slot_stack = stacks_.get(index);
      boolean already_in_slot = (!stack.isEmpty()) && (Inventories.areItemStacksIdentical(stack, slot_stack));
      stacks_.set(index, stack);
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      if((index == SMELTING_INPUT_SLOT_NO) && (!already_in_slot)) {
        proc_time_needed_ = getSmeltingTimeNeeded(world, stack);
        proc_time_elapsed_ = 0;
        markDirty();
      }
    }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player)
    { return getPos().distanceSq(player.func_233580_cy_()) < 36; }

    @Override
    public void openInventory(PlayerEntity player)
    {}

    @Override
    public void closeInventory(PlayerEntity player)
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
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
          ItemStack slot_stack = stacks_.get(FIFO_FUEL_1_SLOT_NO);
          return isFuel(world, stack) || FurnaceFuelSlot.isBucket(stack) && (slot_stack.getItem() != Items.BUCKET);
        }
      }
    }

    @Override
    public void clear()
    { stacks_.clear(); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(FurnaceTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          case 0: return FurnaceTileEntity.this.burntime_left_;
          case 1: return FurnaceTileEntity.this.fuel_burntime_;
          case 2: return (int)FurnaceTileEntity.this.field_proc_time_elapsed_;
          case 3: return FurnaceTileEntity.this.proc_time_needed_;
          case 4: return FurnaceTileEntity.this.field_is_burning_;
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0: FurnaceTileEntity.this.burntime_left_ = value; break;
          case 1: FurnaceTileEntity.this.fuel_burntime_ = value; break;
          case 2: FurnaceTileEntity.this.field_proc_time_elapsed_ = value; break;
          case 3: FurnaceTileEntity.this.proc_time_needed_ = value; break;
          case 4: FurnaceTileEntity.this.field_is_burning_ = value;
        }
      }
    };

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SLOTS_TOP    = new int[] {FIFO_INPUT_1_SLOT_NO};
    private static final int[] SLOTS_BOTTOM = new int[] {FIFO_OUTPUT_1_SLOT_NO};
    private static final int[] SLOTS_SIDES  = new int[] {FIFO_FUEL_1_SLOT_NO};

    @Override
    public int[] getSlotsForFace(Direction side)
    {
      if(side == Direction.DOWN) return SLOTS_BOTTOM;
      if(side == Direction.UP) return SLOTS_TOP;
      return SLOTS_SIDES;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, Direction direction)
    { return ((index==FIFO_INPUT_1_SLOT_NO) || (index==FIFO_INPUT_0_SLOT_NO) || (index==FIFO_FUEL_1_SLOT_NO) || (index==FIFO_FUEL_0_SLOT_NO)) && isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction)
    {
      if((direction!=Direction.DOWN) || ((index!=SMELTING_FUEL_SLOT_NO) && (index!=FIFO_FUEL_0_SLOT_NO) && (index!=FIFO_FUEL_1_SLOT_NO) )) return true;
      return (stack.getItem()==Items.BUCKET);
    }

    // IEnergyStorage ----------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return false; }

    @Override
    public boolean canReceive()
    { return true; }

    @Override
    public int getMaxEnergyStored()
    { return boost_energy_consumption; }

    @Override
    public int getEnergyStored()
    { return boost_energy_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    { // only speedup support, no buffering, not in nbt -> no markdirty
      if((boost_energy_ >= boost_energy_consumption) || (maxReceive < boost_energy_consumption)) return 0;
      if(!simulate) boost_energy_ = boost_energy_consumption;
      return boost_energy_consumption;
    }

    // Capability export ----------------------------------------------------------------------------

    LazyOptional<? extends IItemHandler>[] item_handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);
    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
        if(facing == Direction.UP) return item_handlers[0].cast();
        if(facing == Direction.DOWN) return item_handlers[1].cast();
        return item_handlers[2].cast();
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
      final BlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof FurnaceBlock)) return;
      final boolean was_burning = burning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(world.isRemote) return;
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
        heater_inserted_ = (ExternalObjects.IE_EXTERNAL_HEATER==null) // without IE always allow electrical boost
          || (stacks_.get(AUX_0_SLOT_NO).getItem()==ExternalObjects.IE_EXTERNAL_HEATER)
          || (stacks_.get(AUX_1_SLOT_NO).getItem()==ExternalObjects.IE_EXTERNAL_HEATER);
        if(!burning()) cleanupRecentRecipes();
      }
      ItemStack fuel = stacks_.get(SMELTING_FUEL_SLOT_NO);
      if(burning() || (!fuel.isEmpty()) && (!(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
        IRecipe last_recipe = currentRecipe();
        updateCurrentRecipe();
        if(currentRecipe() != last_recipe) {
          proc_time_elapsed_ = 0;
          proc_time_needed_ = getSmeltingTimeNeeded(world, stacks_.get(SMELTING_INPUT_SLOT_NO));
        }
        if(!burning() && canSmeltCurrentItem()) {
          burntime_left_ = (int)MathHelper.clamp((proc_fuel_efficiency_ * getFuelBurntime(world, fuel)), 0, MAX_BURNTIME);
          fuel_burntime_ = (int)MathHelper.clamp(((double)burntime_left_)/((proc_speed_ > 0) ? proc_speed_ : 1), 1, MAX_BURNTIME);
          if(burning()) {
            dirty = true;
            if(!fuel.isEmpty()) {
              Item fuel_item = fuel.getItem();
              fuel.shrink(1);
              if(fuel.isEmpty()) stacks_.set(SMELTING_FUEL_SLOT_NO, fuel_item.getContainerItem(fuel));
            }
          }
        }
        if(burning() && canSmeltCurrentItem()) {
          proc_time_elapsed_ += TICK_INTERVAL * proc_speed_;
          if(heater_inserted_ && (boost_energy_ >= boost_energy_consumption)) { boost_energy_ = 0; proc_time_elapsed_ += TICK_INTERVAL; }
          if(proc_time_elapsed_ >= proc_time_needed_) {
            proc_time_elapsed_ = 0;
            proc_time_needed_ = getSmeltingTimeNeeded(world, stacks_.get(SMELTING_INPUT_SLOT_NO));
            smeltCurrentItem();
            dirty = true;
          }
        } else {
          proc_time_elapsed_ = 0;
        }
      } else if((!burning()) && (proc_time_elapsed_ > 0)) {
        proc_time_elapsed_ = MathHelper.clamp(proc_time_elapsed_-2, 0, proc_time_needed_);
      }
      if(was_burning != burning()) {
        dirty = true;
        world.setBlockState(pos, state.with(FurnaceBlock.LIT, burning()));
      }
      if(dirty) {
        markDirty();
      }
      field_is_burning_ = this.burning() ? 1 : 0;
      field_proc_time_elapsed_ = (int)proc_time_elapsed_;
    }

    // Furnace -------------------------------------------------------------------------------------

    @Nullable
    public static final <T extends AbstractCookingRecipe> T getSmeltingResult(IRecipeType<T> recipe_type, World world, ItemStack stack)
    {
      if(stack.isEmpty()) return null;
      Inventory inventory = new Inventory(3);
      inventory.setInventorySlotContents(0, stack);
      return world.getRecipeManager().getRecipe(recipe_type, inventory, world).orElse(null);
    }

    @Nullable
    protected IRecipe currentRecipe()
    { return current_recipe_; }

    public boolean burning()
    { return burntime_left_ > 0; }

    public int getSmeltingTimeNeeded(World world, ItemStack stack)
    {
      if(stack.isEmpty()) return 0;
      AbstractCookingRecipe recipe = getSmeltingResult(RECIPE_TYPE, world, stack);
      if(recipe == null) return 0;
      int t = recipe.getCookTime();
      return (t<=0) ? STD_SMELTING_TIME : t;
    }

    private boolean transferItems(final int index_from, final int index_to, int count)
    {
      ItemStack from = stacks_.get(index_from);
      if(from.isEmpty()) return false;
      ItemStack to = stacks_.get(index_to);
      if(from.getCount() < count) count = from.getCount();
      if(count <= 0) return false;
      boolean changed = true;
      if(to.isEmpty()) {
        stacks_.set(index_to, from.split(count));
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
        stacks_.set(index_from, ItemStack.EMPTY);
        changed = true;
      }
      return changed;
    }

    protected boolean canSmeltCurrentItem()
    {
      if((currentRecipe()==null) || (stacks_.get(SMELTING_INPUT_SLOT_NO).isEmpty())) return false;
      final ItemStack recipe_result_items = getSmeltingResult(stacks_.get(SMELTING_INPUT_SLOT_NO));
      if(recipe_result_items.isEmpty()) return false;
      final ItemStack result_stack = stacks_.get(SMELTING_OUTPUT_SLOT_NO);
      if(result_stack.isEmpty()) return true;
      if(!result_stack.isItemEqual(recipe_result_items)) return false;
      if(result_stack.getCount() + recipe_result_items.getCount() <= getInventoryStackLimit() && result_stack.getCount() + recipe_result_items.getCount() <= result_stack.getMaxStackSize()) return true;
      return result_stack.getCount() + recipe_result_items.getCount() <= recipe_result_items.getMaxStackSize();
    }

    protected void smeltCurrentItem()
    {
      if(!canSmeltCurrentItem()) return;
      final ItemStack smelting_input_stack = stacks_.get(SMELTING_INPUT_SLOT_NO);
      final ItemStack recipe_result_items = getSmeltingResult(smelting_input_stack);
      final ItemStack smelting_output_stack = stacks_.get(SMELTING_OUTPUT_SLOT_NO);
      final ItemStack fuel_stack = stacks_.get(SMELTING_FUEL_SLOT_NO);
      if(smelting_output_stack.isEmpty()) {
        stacks_.set(SMELTING_OUTPUT_SLOT_NO, recipe_result_items.copy());
      } else if(smelting_output_stack.getItem() == recipe_result_items.getItem()) {
        smelting_output_stack.grow(recipe_result_items.getCount());
      }
      smelting_input_stack.shrink(1);
    }

    public static int getFuelBurntime(World world, ItemStack stack)
    {
      if(stack.isEmpty()) return 0;
      int t = ForgeHooks.getBurnTime(stack);
      return (t<0) ? 0 : t;
    }

    public static boolean isFuel(World world, ItemStack stack)
    { return getFuelBurntime(world, stack) > 0; }

    public float getSmeltingExperience(ItemStack stack)
    {
      // This method is not often needed, so the time managing dealing with the recent
      // recipes is mainly invested here.
      float xp = stack.getItem().getSmeltingExperience(stack);
      if(xp >= 0) return xp;
      for(int i=0; i<recent_recipes_.size(); ++i) {
        IRecipe r = world.getRecipeManager().getRecipe(new ResourceLocation(recent_recipes_.get(i))).orElse(null);
        if((!(r instanceof AbstractCookingRecipe))) continue; // recipe not available (e.g. at the moment).
        if(!(stack.isItemEqual(r.getRecipeOutput()))) continue;
        xp = ((AbstractCookingRecipe)r).getExperience();
      }
      return (xp <= 0) ? 0 : xp;
    }

    public ItemStack getSmeltingResult(final ItemStack stack)
    { return (currentRecipe()==null) ? (ItemStack.EMPTY) : (currentRecipe().getRecipeOutput()); }

    public static boolean canSmelt(World world, final ItemStack stack)
    { return getSmeltingResult(RECIPE_TYPE, world, stack) != null; }

    protected void updateCurrentRecipe()
    { setCurrentRecipe(getSmeltingResult(RECIPE_TYPE, world, stacks_.get(SMELTING_INPUT_SLOT_NO))); }

    protected void setCurrentRecipe(IRecipe<?> recipe)
    {
      if(recipe == null) { current_recipe_ = null; return; }
      current_recipe_ = recipe;
      String recipe_id = recipe.getId().toString();
      if(!recent_recipes_.contains(recipe_id)) recent_recipes_.add(recipe_id);
    }

    private void cleanupRecentRecipes()
    {
      if(recent_recipes_.isEmpty()) return;
      if(!stacks_.get(SMELTING_INPUT_SLOT_NO).isEmpty()) return;
      if(!stacks_.get(SMELTING_OUTPUT_SLOT_NO).isEmpty()) return;
      if(!stacks_.get(FIFO_OUTPUT_0_SLOT_NO).isEmpty()) return;
      if(!stacks_.get(FIFO_OUTPUT_1_SLOT_NO).isEmpty()) return;
      recent_recipes_.clear();
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container slots
  //--------------------------------------------------------------------------------------------------------------------

  public static class FurnaceContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    // Slots --------------------------------------------------------------------------------------------

    public static class BSlotInpFifo extends Slot
    {
      public BSlotInpFifo(IInventory inv, int index, int xpos, int ypos)
      { super(inv, index, xpos, ypos); }
    }

    public static class BSlotFuelFifo extends Slot
    {
      public BSlotFuelFifo(IInventory inv, int index, int xpos, int ypos)
      { super(inv, index, xpos, ypos); }
    }

    public static class BSlotOutFifo extends BSlotResult
    {
      public BSlotOutFifo(PlayerEntity player, IInventory inventory, int index, int xpos, int ypos)
      { super(player, inventory, index, xpos, ypos); }
    }

    public static class BSlotResult extends Slot
    {
      private final IInventory inventory_;
      private final PlayerEntity player_;
      private int removeCount = 0;

      public BSlotResult(PlayerEntity player, IInventory inventory, int index, int xpos, int ypos)
      { super(inventory, index, xpos, ypos); inventory_ = inventory; player_ = player; }

      @Override
      public boolean isItemValid(ItemStack stack)
      { return false; }

      @Override
      public ItemStack decrStackSize(int amount)
      { removeCount += getHasStack() ? Math.min(amount, getStack().getCount()) : 0; return super.decrStackSize(amount); }

      @Override
      public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack)
      { onCrafting(stack); super.onTake(thePlayer, stack); return stack; }

      @Override
      protected void onCrafting(ItemStack stack, int amount)
      { removeCount += amount; onCrafting(stack); }

      @Override
      protected void onCrafting(ItemStack stack)
      {
        stack.onCrafting(player_.world, player_, removeCount);
        if((!player_.world.isRemote) && (inventory_ instanceof FurnaceTileEntity)) {
          FurnaceTileEntity te = (FurnaceTileEntity)inventory_;
          int xp = removeCount;
          float sxp = te.getSmeltingExperience(stack);
          if(sxp == 0) {
            xp = 0;
          } else if(sxp < 1.0) {
            xp = (int)((sxp*xp) + Math.round(Math.random()+0.75));
          }
          while(xp > 0) {
            int k = ExperienceOrbEntity.getXPSplit(xp);
            xp -= k;
            player_.world.addEntity((new ExperienceOrbEntity(player_.world, player_.func_233580_cy_().getX(), player_.func_233580_cy_().getY()+0.5, player_.func_233580_cy_().getZ()+0.5, k)));
          }
        }
        removeCount = 0;
        BasicEventHooks.firePlayerSmeltedEvent(player_, stack);
      }
    }

    public static class BFuelSlot extends Slot
    {
      private final FurnaceContainer container_;

      public BFuelSlot(IInventory inventory, int index, int xpos, int ypos, FurnaceContainer container)
      { super(inventory, index, xpos, ypos); container_=container; }

      @Override
      public boolean isItemValid(ItemStack stack)
      { return isBucket(stack) || (FurnaceTileEntity.isFuel(container_.world(), stack)); }

      @Override
      public int getItemStackLimit(ItemStack stack)
      { return isBucket(stack) ? 1 : super.getItemStackLimit(stack); }

      protected static boolean isBucket(ItemStack stack)
      { return (stack.getItem()==Items.BUCKET); }
    }

    // Container ----------------------------------------------------------------------------------------

    private static final int PLAYER_INV_START_SLOTNO = 11;
    protected final PlayerEntity player_;
    protected final IInventory inventory_;
    protected final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final IRecipeType<? extends AbstractCookingRecipe> recipe_type_;

    public int field(int index) { return fields_.get(index); }
    public PlayerEntity player() { return player_ ; }
    public IInventory inventory() { return inventory_ ; }
    public World world() { return player_.world; }

    public FurnaceContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(FurnaceTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new IntArray(FurnaceTileEntity.NUM_OF_FIELDS)); }

    private FurnaceContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_SMALL_LAB_FURNACE, cid);
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      wpc_ = wpc;
      fields_ = fields;
      recipe_type_ = FurnaceTileEntity.RECIPE_TYPE;
      addSlot(new Slot(inventory_, 0, 59, 17)); // smelting input
      addSlot(new BFuelSlot(inventory_, 1, 59, 53, this)); // fuel
      addSlot(new BSlotResult(player_, inventory_, 2, 101, 35)); // smelting result
      addSlot(new BSlotInpFifo(inventory_, 3, 34, 17)); // input fifo 0
      addSlot(new BSlotInpFifo(inventory_, 4, 16, 17)); // input fifo 1
      addSlot(new BSlotFuelFifo(inventory_, 5, 34, 53)); // fuel fifo 0
      addSlot(new BSlotFuelFifo(inventory_, 6, 16, 53)); // fuel fifo 1
      addSlot(new BSlotOutFifo(player_inventory.player, inventory_, 7, 126, 35)); // out fifo 0
      addSlot(new BSlotOutFifo(player_inventory.player, inventory_, 8, 144, 35)); // out fifo 1
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
      this.trackIntArray(fields_); // === Add reference holders
    }

    @Override
    public boolean canInteractWith(PlayerEntity player)
    { return inventory_.isUsableByPlayer(player); }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index==2) || (index==7) || (index==8)) {
        // Output slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==1) || (index==5) || (index==6)) {
        // Fuel slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==9) || (index==10)) {
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(FurnaceTileEntity.canSmelt(world(), slot_stack)) {
          if(
            (!mergeItemStack(slot_stack, 0, 1, false)) && // smelting input
            (!mergeItemStack(slot_stack, 3, 4, false)) && // fifo0
            (!mergeItemStack(slot_stack, 4, 5, false))    // fifo1
          ) return ItemStack.EMPTY;
        } else if(FurnaceTileEntity.isFuel(player_.world, slot_stack)) {
          if(
            (!mergeItemStack(slot_stack, 1, 2, false)) && // fuel input
            (!mergeItemStack(slot_stack, 5, 6, false)) && // fuel fifo0
            (!mergeItemStack(slot_stack, 6, 7, false))    // fuel fifo1
          ) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO) && (index < PLAYER_INV_START_SLOTNO+27)) {
          // player inventory --> player hotbar
          if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO+27, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
        } else if((index >= PLAYER_INV_START_SLOTNO+27) && (index < PLAYER_INV_START_SLOTNO+36) && (!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+27, false))) {
          // player hotbar --> player inventory
          return ItemStack.EMPTY;
        }
      } else {
        // invalid slot
        return ItemStack.EMPTY;
      }
      if(slot_stack.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }
      if(slot_stack.getCount() == transferred.getCount()) return ItemStack.EMPTY;
      slot.onTake(player, slot_stack);
      //if(!player.world.isRemote) detectAndSendChanges();
      return transferred;
    }

    // INetworkSynchronisableContainer ---------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(CompoundNBT nbt)
    { Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt); }

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String key, int value)
    {
      CompoundNBT nbt = new CompoundNBT();
      nbt.putInt(key, value);
      Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt);
    }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {}

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class FurnaceGui extends ContainerScreen<FurnaceContainer>
  {
    protected final PlayerEntity player_;

    public FurnaceGui(FurnaceContainer container, PlayerInventory player_inventory, ITextComponent title)
    { super(container, player_inventory, title); this.player_ = player_inventory.player; }

    @Override
    public void func_231160_c_/*init*/()
    { super.func_231160_c_(); }

    @Override
    public void func_230430_a_/*render*/(MatrixStack mx, int mouseX, int mouseY, float partialTicks)
    {
      func_230446_a_/*renderBackground*/(mx);
      super.func_230430_a_(mx, mouseX, mouseY, partialTicks);
      func_230459_a_/*renderHoveredToolTip*/(mx, mouseX, mouseY);
    }

    @Override
    protected void func_230451_b_(MatrixStack mx, int x, int y)
    {}

    @Override
    @SuppressWarnings("deprecation")
    protected void func_230450_a_/*drawGuiContainerBackgroundLayer*/(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.enableBlend();
      RenderSystem.color3f(1.0F, 1.0F, 1.0F);
      getMinecraft().getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_lab_furnace_gui.png"));
      final int x0=guiLeft, y0=guiTop, w=xSize, h=ySize;
      func_238474_b_(mx, x0, y0, 0, 0, w, h);
      if(getContainer().field(4) != 0) {
        final int k = flame_px(13);
        func_238474_b_(mx, x0+59, y0+36+12-k, 176, 12-k, 14, k+1);
      }
      func_238474_b_(mx, x0+79, y0+36, 176, 15, 1+progress_px(17), 15);
      RenderSystem.disableBlend();
    }

    private int progress_px(int pixels)
    { final int tc=getContainer().field(2), T=getContainer().field(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int flame_px(int pixels)
    { int ibt = getContainer().field(1); return ((getContainer().field(0) * pixels) / ((ibt>0) ? (ibt) : (FurnaceTileEntity.STD_SMELTING_TIME))); }
  }

}
