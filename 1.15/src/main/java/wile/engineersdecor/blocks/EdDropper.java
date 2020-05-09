/*
 * @file EdDropper.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Dropper factory automation suitable.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.libmc.detail.Inventories;
import wile.engineersdecor.libmc.detail.Networking;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EdDropper
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class DropperBlock extends DecorBlock.Directed implements IDecorBlock
  {
    public static final BooleanProperty OPEN = DoorBlock.OPEN;

    public DropperBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public RenderTypeHint getRenderTypeHint()
    { return RenderTypeHint.SOLID; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.fullCube(); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(OPEN); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).with(OPEN, false); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(BlockState blockState, World world, BlockPos pos)
    { return Container.calcRedstone(world.getTileEntity(pos)); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdDropper.DropperTileEntity(); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isRemote) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundNBT te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof EdDropper.DropperTileEntity)) return;
      ((EdDropper.DropperTileEntity)te).readnbt(te_nbt, false);
      ((EdDropper.DropperTileEntity)te).reset_rtstate();
      ((EdDropper.DropperTileEntity)te).markDirty();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, BlockPos pos, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isRemote) return stacks;
      if(!(te instanceof DropperTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT te_nbt = ((DropperTileEntity) te).clear_getnbt();
        if(!te_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((DropperTileEntity)te).stacks_) {
          if(!stack.isEmpty()) stacks.add(stack);
        }
        ((DropperTileEntity)te).reset_rtstate();
      }
      return stacks;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return ActionResultType.SUCCESS;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof EdDropper.DropperTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      return ActionResultType.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
    {
      if(!(world instanceof World) || (((World) world).isRemote)) return;
      TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof DropperTileEntity)) return;
      ((DropperTileEntity)te).block_updated();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canProvidePower(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getStrongPower(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class DropperTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider, ISidedInventory
  {
    public static final int NUM_OF_FIELDS = 16;
    public static final int TICK_INTERVAL = 32;
    public static final int NUM_OF_SLOTS = 15;
    public static final int INPUT_SLOTS_FIRST = 0;
    public static final int INPUT_SLOTS_SIZE = 12;
    public static final int CTRL_SLOTS_FIRST = INPUT_SLOTS_SIZE;
    public static final int CTRL_SLOTS_SIZE = 3;
    public static final int SHUTTER_CLOSE_DELAY = 40;
    public static final int MAX_DROP_COUNT = 32;
    public static final int DROP_PERIOD_OFFSET = 10;
    ///
    public static final int DROPLOGIC_FILTER_ANDGATE = 0x01;
    public static final int DROPLOGIC_EXTERN_ANDGATE = 0x02;
    public static final int DROPLOGIC_SILENT_DROP    = 0x04;
    public static final int DROPLOGIC_SILENT_OPEN    = 0x08;
    public static final int DROPLOGIC_CONTINUOUS     = 0x10;
    ///
    private int filter_matches_[] = new int[CTRL_SLOTS_SIZE];
    private int open_timer_ = 0;
    private int drop_timer_ = 0;
    private boolean triggered_ = false;
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int drop_speed_ = 10;
    private int drop_noise_ = 0;
    private int drop_xdev_ = 0;
    private int drop_ydev_ = 0;
    private int drop_count_ = 1;
    private int drop_logic_ = DROPLOGIC_EXTERN_ANDGATE;
    private int drop_period_ = 0;
    private int drop_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected NonNullList<ItemStack> stacks_;

    public static void on_config(int cooldown_ticks)
    {
      // ModEngineersDecor.logger.info("Config factory dropper:");
    }

    public DropperTileEntity()
    { this(ModContent.TET_FACTORY_DROPPER); }

    public DropperTileEntity(TileEntityType<?> te_type)
    {
      super(te_type);
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      reset_rtstate();
    }

    public CompoundNBT clear_getnbt()
    {
      CompoundNBT nbt = new CompoundNBT();
      writenbt(nbt, false);
      for(int i=0; i<stacks_.size(); ++i) stacks_.set(i, ItemStack.EMPTY);
      reset_rtstate();
      triggered_ = false;
      block_power_updated_ = false;
      return nbt;
    }

    public void reset_rtstate()
    {
      block_power_signal_ = false;
      block_power_updated_ = false;
      for(int i=0; i<filter_matches_.length; ++i) filter_matches_[i] = 0;
    }

    public void readnbt(CompoundNBT nbt, boolean update_packet)
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      block_power_signal_ = nbt.getBoolean("powered");
      open_timer_ = nbt.getInt("open_timer");
      drop_speed_ = nbt.getInt("drop_speed");
      drop_noise_ = nbt.getInt("drop_noise");
      drop_xdev_ = nbt.getInt("drop_xdev");
      drop_ydev_ = nbt.getInt("drop_ydev");
      drop_slot_index_ = nbt.getInt("drop_slot_index");
      drop_count_ = MathHelper.clamp(nbt.getInt("drop_count"), 1, MAX_DROP_COUNT);
      drop_logic_ = nbt.getInt("drop_logic");
      drop_period_ = nbt.getInt("drop_period");
    }

    protected void writenbt(CompoundNBT nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.putBoolean("powered", block_power_signal_);
      nbt.putInt("open_timer", open_timer_);
      nbt.putInt("drop_speed", drop_speed_);
      nbt.putInt("drop_noise", drop_noise_);
      nbt.putInt("drop_xdev", drop_xdev_);
      nbt.putInt("drop_ydev", drop_ydev_);
      nbt.putInt("drop_slot_index", drop_slot_index_);
      nbt.putInt("drop_count", drop_count_);
      nbt.putInt("drop_logic", drop_logic_);
      nbt.putInt("drop_period", drop_period_);
    }

    public void block_updated()
    {
      // RS power check, both edges
      boolean powered = world.isBlockPowered(pos);
      if(block_power_signal_ != powered) block_power_updated_ = true;
      block_power_signal_ = powered;
      tick_timer_ = 1;
    }

    public boolean is_input_slot(int index)
    { return (index >= INPUT_SLOTS_FIRST) && (index < (INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE)); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt, false); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt, false); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      Arrays.stream(item_handlers).forEach(LazyOptional::invalidate);
    }

    // INamable ----------------------------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Factory dropper"); }

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
    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
    { return new DropperContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

    // IInventory -------------------------------------------------------------------------------------------

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
      stacks_.set(index, stack);
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      if(tick_timer_ > 8) tick_timer_ = 8;
      markDirty();
    }

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
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return true; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(DropperTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          case  0: return drop_speed_;
          case  1: return drop_xdev_;
          case  2: return drop_ydev_;
          case  3: return drop_noise_;
          case  4: return drop_count_;
          case  5: return drop_logic_;
          case  6: return drop_period_;
          case  9: return drop_timer_;
          case 10: return open_timer_;
          case 11: return block_power_signal_ ? 1 : 0;
          case 12: return filter_matches_[0];
          case 13: return filter_matches_[1];
          case 14: return filter_matches_[2];
          case 15: return drop_slot_index_;
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case  0: drop_speed_ = MathHelper.clamp(value,    0, 100); return;
          case  1: drop_xdev_  = MathHelper.clamp(value, -100, 100); return;
          case  2: drop_ydev_  = MathHelper.clamp(value, -100, 100); return;
          case  3: drop_noise_ = MathHelper.clamp(value,    0, 100); return;
          case  4: drop_count_ = MathHelper.clamp(value,    1,  MAX_DROP_COUNT); return;
          case  5: drop_logic_ = value; return;
          case  6: drop_period_ = MathHelper.clamp(value,   0,  100); return;
          case  9: drop_timer_ = MathHelper.clamp(value,    0,  400); return;
          case 10: open_timer_ = MathHelper.clamp(value,    0,  400); return;
          case 11: block_power_signal_ = (value != 0); return;
          case 12: filter_matches_[0] = (value & 0x3); return;
          case 13: filter_matches_[1] = (value & 0x3); return;
          case 14: filter_matches_[2] = (value & 0x3); return;
          case 15: drop_slot_index_ = MathHelper.clamp(value, INPUT_SLOTS_FIRST, INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE-1); return;
          default: return;
        }
      }
    };

    // ISidedInventory --------------------------------------------------------------------------------------

    LazyOptional<? extends IItemHandler>[] item_handlers = SidedInvWrapper.create(this, Direction.UP);
    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[INPUT_SLOTS_SIZE];
      for(int i=0; i<INPUT_SLOTS_SIZE; ++i) SIDED_INV_SLOTS[i] = i+INPUT_SLOTS_FIRST;
    }

    @Override
    public int[] getSlotsForFace(Direction side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, Direction direction)
    { return is_input_slot(index) && isItemValidForSlot(index, stack); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction)
    { return false; }

    // Capability export ------------------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handlers[0].cast();
      return super.getCapability(capability, facing);
    }

    // ITickable and aux methods ----------------------------------------------------------------------------

    private static void drop(World world, BlockPos pos, Direction facing, ItemStack stack, int speed_percent, int xdeviation, int ydeviation, int noise_percent)
    {
      final double ofs = facing==Direction.DOWN ? 0.8 : 0.7;
      Vec3d v0 = new Vec3d(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
      final ItemEntity ei = new ItemEntity(world, (pos.getX()+0.5)+(ofs*v0.x), (pos.getY()+0.5)+(ofs*v0.y), (pos.getZ()+0.5)+(ofs*v0.z), stack);
      if((xdeviation != 0) || (ydeviation != 0)) {
        double vdx = 1e-2 * MathHelper.clamp(xdeviation, -100, 100);
        double vdy = 1e-2 * MathHelper.clamp(ydeviation, -100, 100);
        switch(facing) { // switch-case faster than coorsys fwd transform
          case DOWN:  v0 = v0.add( vdx, 0,-vdy); break;
          case NORTH: v0 = v0.add( vdx, vdy, 0); break;
          case SOUTH: v0 = v0.add(-vdx, vdy, 0); break;
          case EAST:  v0 = v0.add(0, vdy,  vdx); break;
          case WEST:  v0 = v0.add(0, vdy, -vdx); break;
          case UP:    v0 = v0.add( vdx, 0, vdy); break;
        }
      }
      if(noise_percent > 0) {
        v0 = v0.add(
          ((world.rand.nextDouble()-0.5) * 1e-3 * noise_percent),
          ((world.rand.nextDouble()-0.5) * 1e-3 * noise_percent),
          ((world.rand.nextDouble()-0.5) * 1e-3 * noise_percent)
        );
      }
      if(speed_percent < 5) speed_percent = 5;
      double speed = 1e-2 * speed_percent;
      if(noise_percent > 0) speed += (world.rand.nextDouble()-0.5) * 1e-4 * noise_percent;
      v0 = v0.normalize().scale(speed);
      ei.setMotion(v0.x, v0.y, v0.z);
      ei.velocityChanged = true;
      world.addEntity(ei);
    }

    @Nullable
    BlockState update_blockstate()
    {
      BlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof DropperBlock)) return null;
      boolean open = (open_timer_ > 0);
      if(state.get(DropperBlock.OPEN) != open) {
        state = state.with(DropperBlock.OPEN, open);
        world.setBlockState(pos, state, 2|16);
        if((drop_logic_ & DROPLOGIC_SILENT_OPEN) == 0) {
          if(open) {
            world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.08f, 3f);
          } else {
            world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.08f, 3f);
          }
        }
      }
      return state;
    }

    private static int next_slot(int i)
    { return (i<INPUT_SLOTS_SIZE-1) ? (i+1) : INPUT_SLOTS_FIRST; }

    @Override
    public void tick()
    {
      if(world.isRemote) return;
      if(--open_timer_ < 0) open_timer_ = 0;
      if((drop_timer_ > 0) && ((--drop_timer_) == 0)) markDirty();
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final boolean continuous_mode = (drop_logic_ & DROPLOGIC_CONTINUOUS)!=0;
      boolean dirty = block_power_updated_;
      boolean redstone_trigger = (block_power_signal_ && ((block_power_updated_) || (continuous_mode)));
      boolean filter_trigger;
      boolean filter_defined;
      boolean trigger;
      // Trigger logic
      {
        boolean droppable_slot_found = false;
        for(int i=INPUT_SLOTS_FIRST; i<(INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE); ++i) {
          if(stacks_.get(i).getCount() >= drop_count_) { droppable_slot_found = true; break; }
        }
        // From filters / inventory checks
        {
          int filter_nset = 0;
          int last_filter_matches_[] = filter_matches_.clone();
          boolean slot_assigned = false;
          for(int ci=0; ci<CTRL_SLOTS_SIZE; ++ci) {
            filter_matches_[ci] = 0;
            final ItemStack cmp_stack = stacks_.get(CTRL_SLOTS_FIRST+ci);
            if(cmp_stack.isEmpty()) continue;
            filter_matches_[ci] = 1;
            final int cmp_stack_count = cmp_stack.getCount();
            int inventory_item_count = 0;
            int slot = drop_slot_index_;
            for(int i=INPUT_SLOTS_FIRST; i<(INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE); ++i) {
              final ItemStack inp_stack = stacks_.get(slot);
              if(Inventories.areItemStacksDifferent(inp_stack, cmp_stack)) { slot = next_slot(slot); continue; }
              inventory_item_count += inp_stack.getCount();
              if(inventory_item_count < cmp_stack_count) { slot = next_slot(slot); continue; }
              filter_matches_[ci] = 2;
              break;
            }
          }
          int nmatched = 0;
          for(int i=0; i<filter_matches_.length; ++i) {
            if(filter_matches_[i] > 0) ++filter_nset;
            if(filter_matches_[i] > 1) ++nmatched;
            if(filter_matches_[i] != last_filter_matches_[i]) dirty = true;
          }
          filter_defined = (filter_nset > 0);
          filter_trigger = ((filter_nset > 0) && (nmatched > 0));
          if(((drop_logic_ & DROPLOGIC_FILTER_ANDGATE) != 0) && (nmatched != filter_nset)) filter_trigger = false;
        }
        // gates
        {
          if(filter_defined) {
            trigger = ((drop_logic_ & DROPLOGIC_EXTERN_ANDGATE) != 0) ? (filter_trigger && redstone_trigger) : (filter_trigger || redstone_trigger);
          } else {
            trigger = redstone_trigger;
          }
          if(triggered_) { triggered_ = false; trigger = true; }
          if(!droppable_slot_found) {
            if(open_timer_> 10) open_timer_ = 10; // override if dropping is not possible at all.
          } else if(trigger || filter_trigger || redstone_trigger) {
            open_timer_ = SHUTTER_CLOSE_DELAY;
          }
        }
        // edge detection for next cycle
        {
          boolean tr = world.isBlockPowered(pos);
          block_power_updated_ = (block_power_signal_ != tr);
          block_power_signal_ = tr;
          if(block_power_updated_) dirty = true;
        }
      }
      // block state update
      final BlockState state = update_blockstate();
      if(state == null) { block_power_signal_= false; return; }
      // dispense action
      if(trigger && (drop_timer_ <= 0)) {
        // drop stack for non-filter triggers
        ItemStack drop_stacks[] = {ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
        if(!filter_trigger) {
          for(int i=0; i<INPUT_SLOTS_SIZE; ++i) {
            if(drop_slot_index_ >= INPUT_SLOTS_SIZE) drop_slot_index_ = 0;
            int ic = drop_slot_index_;
            drop_slot_index_ = next_slot(drop_slot_index_);
            ItemStack ds = stacks_.get(ic);
            if((!ds.isEmpty()) && (ds.getCount() >= drop_count_)) {
              drop_stacks[0] = ds.split(drop_count_);
              stacks_.set(ic, ds);
              break;
            }
          }
        } else {
          for(int fi=0; fi<filter_matches_.length; ++fi) {
            if(filter_matches_[fi] > 1) {
              drop_stacks[fi] = stacks_.get(CTRL_SLOTS_FIRST+fi).copy();
              int ntoremove = drop_stacks[fi].getCount();
              for(int i=INPUT_SLOTS_SIZE-1; (i>=0) && (ntoremove>0); --i) {
                ItemStack stack = stacks_.get(i);
                if(Inventories.areItemStacksDifferent(stack, drop_stacks[fi])) continue;
                if(stack.getCount() <= ntoremove) {
                  ntoremove -= stack.getCount();
                  stacks_.set(i, ItemStack.EMPTY);
                } else {
                  stack.shrink(ntoremove);
                  ntoremove = 0;
                  stacks_.set(i, stack);
                }
              }
              if(ntoremove > 0) drop_stacks[fi].shrink(ntoremove);
            }
          }
        }
        // drop action
        boolean dropped = false;
        for(int i = 0; i < drop_stacks.length; ++i) {
          if(drop_stacks[i].isEmpty()) continue;
          dirty = true;
          drop(world, pos, state.get(DropperBlock.FACING), drop_stacks[i], drop_speed_, drop_xdev_, drop_ydev_, drop_noise_);
          dropped = true;
        }
        // cooldown
        if(dropped) drop_timer_ = DROP_PERIOD_OFFSET + drop_period_ * 2; // 0.1s time base -> 100%===10s
        // drop sound
        if(dropped && ((drop_logic_ & DROPLOGIC_SILENT_DROP) == 0)) {
          world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.1f, 4f);
        }
        // advance to next nonempty slot.
        for(int i = 0; i < INPUT_SLOTS_SIZE; ++i) {
          if(!stacks_.get(drop_slot_index_).isEmpty()) break;
          drop_slot_index_ = next_slot(drop_slot_index_);
        }
      }
      if(dirty) markDirty();
      if(trigger && (tick_timer_ > 10)) tick_timer_ = 10;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class DropperContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    private static final int PLAYER_INV_START_SLOTNO = DropperTileEntity.NUM_OF_SLOTS;
    private final PlayerEntity player_;
    private final IInventory inventory_;
    private final IWorldPosCallable wpc_;
    private final IIntArray fields_;

    public final int field(int index) { return fields_.get(index); }

    public DropperContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(DropperTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new IntArray(DropperTileEntity.NUM_OF_FIELDS)); }

    private DropperContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_FACTORY_DROPPER, cid);
      fields_ = fields;
      wpc_ = wpc;
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      int i=-1;
      // input slots (stacks 0 to 11)
      for(int y=0; y<2; ++y) {
        for(int x=0; x<6; ++x) {
          int xpos = 10+x*18, ypos = 6+y*17;
          addSlot(new Slot(inventory_, ++i, xpos, ypos));
        }
      }
      // filter slots (stacks 12 to 14)
      addSlot(new Slot(inventory_, ++i, 19, 48));
      addSlot(new Slot(inventory_, ++i, 55, 48));
      addSlot(new Slot(inventory_, ++i, 91, 48));
      // player slots
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
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Device slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!mergeItemStack(slot_stack, 0, DropperTileEntity.INPUT_SLOTS_SIZE, false)) return ItemStack.EMPTY;
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
    {
      if(!(inventory_ instanceof DropperTileEntity)) return;
      DropperTileEntity te = (DropperTileEntity)inventory_;
      if(nbt.contains("drop_speed")) te.drop_speed_ = MathHelper.clamp(nbt.getInt("drop_speed"), 0, 100);
      if(nbt.contains("drop_xdev"))  te.drop_xdev_  = MathHelper.clamp(nbt.getInt("drop_xdev"), -100, 100);
      if(nbt.contains("drop_ydev"))  te.drop_ydev_  = MathHelper.clamp(nbt.getInt("drop_ydev"), -100, 100);
      if(nbt.contains("drop_count")) te.drop_count_  = MathHelper.clamp(nbt.getInt("drop_count"), 1, DropperTileEntity.MAX_DROP_COUNT);
      if(nbt.contains("drop_period")) te.drop_period_ = MathHelper.clamp(nbt.getInt("drop_period"),   0,  100);
      if(nbt.contains("drop_logic")) te.drop_logic_  = nbt.getInt("drop_logic");
      if(nbt.contains("manual_rstrigger") && (nbt.getInt("manual_rstrigger")!=0)) { te.block_power_signal_=true; te.block_power_updated_=true; te.tick_timer_=1; }
      if(nbt.contains("manual_trigger") && (nbt.getInt("manual_trigger")!=0)) { te.tick_timer_ = 1; te.triggered_ = true; }
      te.markDirty();
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class DropperGui extends ContainerScreen<DropperContainer>
  {
    protected final PlayerEntity player_;

    public DropperGui(DropperContainer container, PlayerInventory player_inventory, ITextComponent title)
    { super(container, player_inventory, title); this.player_ = player_inventory.player; }

    @Override
    public void init()
    { super.init(); }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
      renderBackground();
      super.render(mouseX, mouseY, partialTicks);
      renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      DropperContainer container = (DropperContainer)getContainer();
      int mx = (int)(mouseX - getGuiLeft() + .5), my = (int)(mouseY - getGuiTop() + .5);
      if((!isPointInRegion(114, 1, 61, 79, mouseX, mouseY))) {
        return super.mouseClicked(mouseX, mouseY, mouseButton);
      } else if(isPointInRegion(130, 10, 12, 25, mouseX, mouseY)) {
        int force_percent = 100 - MathHelper.clamp(((my-10)*100)/25, 0, 100);
        container.onGuiAction("drop_speed", force_percent);
      } else if(isPointInRegion(145, 10, 25, 25, mouseX, mouseY)) {
        int xdev = MathHelper.clamp( (int)Math.round(((double)((mx-157) * 100)) / 12), -100, 100);
        int ydev = MathHelper.clamp(-(int)Math.round(((double)((my- 22) * 100)) / 12), -100, 100);
        if(Math.abs(xdev) < 9) xdev = 0;
        if(Math.abs(ydev) < 9) ydev = 0;
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("drop_xdev", xdev);
        nbt.putInt("drop_ydev", ydev);
        container.onGuiAction(nbt);
      } else if(isPointInRegion(129, 40, 44, 10, mouseX, mouseY)) {
        int ndrop = (mx-135);
        if(ndrop < -1) {
          ndrop = container.field(4) - 1; // -
        } else if(ndrop >= 34) {
          ndrop = container.field(4) + 1; // +
        } else {
          ndrop = MathHelper.clamp(1+ndrop, 1, DropperTileEntity.MAX_DROP_COUNT); // slider
        }
        container.onGuiAction("drop_count", ndrop);
      } else if(isPointInRegion(129, 50, 44, 10, mouseX, mouseY)) {
        int period = (mx-135);
        if(period < -1) {
          period = container.field(6) - 3; // -
        } else if(period >= 34) {
          period = container.field(6) + 3; // +
        } else {
          period = (int)(0.5 + ((100.0 * period)/34));
        }
        period = MathHelper.clamp(period, 0, 100);
        container.onGuiAction("drop_period", period);
      } else if(isPointInRegion(114, 51, 9, 9, mouseX, mouseY)) {
        container.onGuiAction("manual_rstrigger", 1);
      } else if(isPointInRegion(162, 66, 7, 9, mouseX, mouseY)) {
        container.onGuiAction("drop_logic", container.field(5) ^ DropperTileEntity.DROPLOGIC_CONTINUOUS);
      } else if(isPointInRegion(132, 66, 9, 9, mouseX, mouseY)) {
        container.onGuiAction("drop_logic", container.field(5) ^ DropperTileEntity.DROPLOGIC_FILTER_ANDGATE);
      } else if(isPointInRegion(148, 66, 9, 9, mouseX, mouseY)) {
        container.onGuiAction("drop_logic", container.field(5) ^ DropperTileEntity.DROPLOGIC_EXTERN_ANDGATE);
      }
      return true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.enableBlend();
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.minecraft.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/factory_dropper_gui.png"));
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      blit(x0, y0, 0, 0, w, h);
      DropperContainer container = (DropperContainer)getContainer();
      // active drop slot
      {
        int drop_slot_index = container.field(15);
        if((drop_slot_index < 0) || (drop_slot_index >= 16)) drop_slot_index = 0;
        int x = (x0+9+((drop_slot_index % 6) * 18));
        int y = (y0+5+((drop_slot_index / 6) * 17));
        blit(x, y, 180, 45, 18, 18);
      }
      // filter LEDs
      {
        for(int i=0; i<3; ++i) {
          int xt = 180 + (6 * container.field(12+i)), yt = 38;
          int x = x0 + 31 + (i * 36), y = y0 + 65;
          blit(x, y, xt, yt, 6, 6);
        }
      }
      // force adjustment
      {
        int hy = 2 + (((100-container.field(0)) * 21) / 100);
        int x = x0+135, y = y0+12, xt = 181;
        int yt = 4 + (23-hy);
        blit(x, y, xt, yt, 3, hy);
      }
      // angle adjustment
      {
        int x = x0 + 157 - 3 + ((container.field(1) * 12) / 100);
        int y = y0 +  22 - 3 - ((container.field(2) * 12) / 100);
        blit(x, y, 180, 30, 7, 7);
      }
      // drop count
      {
        int x = x0 + 134 - 2 + (container.field(4));
        int y = y0 + 45;
        blit(x, y, 190, 31, 5, 5);
      }
      // drop period
      {
        int px = (int)Math.round(((33.0 * container.field(6)) / 100) + 1);
        int x = x0 + 134 - 2 + MathHelper.clamp(px, 0, 33);
        int y = y0 + 56;
        blit(x, y, 190, 31, 5, 5);
      }
      // redstone input
      {
        if(container.field(11) != 0) {
          blit(x0+114, y0+51, 189, 18, 9, 9);
        }
      }
      // trigger logic
      {
        int filter_gate_offset = ((container.field(5) & DropperTileEntity.DROPLOGIC_FILTER_ANDGATE) != 0) ? 11 : 0;
        int extern_gate_offset = ((container.field(5) & DropperTileEntity.DROPLOGIC_EXTERN_ANDGATE) != 0) ? 11 : 0;
        int pulse_mode_offset  = ((container.field(5) & DropperTileEntity.DROPLOGIC_CONTINUOUS    ) != 0) ? 10 : 0;
        blit(x0+132, y0+66, 179+filter_gate_offset, 66, 9, 9);
        blit(x0+148, y0+66, 179+extern_gate_offset, 66, 9, 9);
        blit(x0+162, y0+66, 200+pulse_mode_offset, 66, 9, 9);
      }
      // drop timer running indicator
      {
        if((container.field(9) > DropperTileEntity.DROP_PERIOD_OFFSET) && ((System.currentTimeMillis() % 1000) < 500)) {
          blit(x0+149, y0+51, 201, 39, 3, 3);
        }
      }
      RenderSystem.disableBlend();
    }
  }

}
