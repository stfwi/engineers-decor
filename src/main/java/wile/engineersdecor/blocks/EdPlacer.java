/*
 * @file EdPlacer.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block placer and planter, factory automation suitable.
 */
package wile.engineersdecor.blocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.network.NetworkHooks;
import wile.engineersdecor.libmc.detail.TooltipDisplay;
import wile.engineersdecor.libmc.detail.TooltipDisplay.TipRange;
import wile.engineersdecor.libmc.client.ContainerGui;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Inventories.InventoryRange;
import wile.engineersdecor.libmc.detail.Networking;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EdPlacer
{
  public static void on_config()
  {}

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class PlacerBlock extends DecorBlock.Directed implements IDecorBlock
  {
    public PlacerBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.block(); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, World world, BlockPos pos)
    { return Container.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos)); }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new PlacerTileEntity(); }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundNBT te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof PlacerTileEntity)) return;
      ((PlacerTileEntity)te).readnbt(te_nbt, false);
      ((PlacerTileEntity)te).reset_rtstate();
      ((PlacerTileEntity)te).setChanged();
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof PlacerTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT te_nbt = ((PlacerTileEntity) te).clear_getnbt();
        if(!te_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        for(ItemStack stack: ((PlacerTileEntity)te).stacks_) {
          if(!stack.isEmpty()) stacks.add(stack);
        }
        ((PlacerTileEntity)te).reset_rtstate();
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof PlacerTileEntity)) return ActionResultType.FAIL;
      if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return ActionResultType.FAIL;
      NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
      return ActionResultType.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
    {
      if(!(world instanceof World) || (((World) world).isClientSide)) return;
      TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof PlacerTileEntity)) return;
      ((PlacerTileEntity)te).block_updated();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }

    @Override
    @SuppressWarnings("deprecation")
    public int getDirectSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
    { return 0; }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class PlacerTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider, ISidedInventory
  {
    public static final int TICK_INTERVAL = 40;
    public static final int NUM_OF_SLOTS = 18;
    public static final int NUM_OF_FIELDS = 3;
    ///
    public static final int LOGIC_NOT_INVERTED = 0x00;
    public static final int LOGIC_INVERTED     = 0x01;
    public static final int LOGIC_CONTINUOUS   = 0x02;
    public static final int LOGIC_IGNORE_EXT   = 0x04;
    ///
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int logic_ = LOGIC_IGNORE_EXT|LOGIC_CONTINUOUS;
    private int current_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected NonNullList<ItemStack> stacks_;

    public PlacerTileEntity()
    { this(ModContent.TET_FACTORY_PLACER); }

    public PlacerTileEntity(TileEntityType<?> te_type)
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
      block_power_updated_ = false;
      return nbt;
    }

    public void reset_rtstate()
    {
      block_power_signal_ = false;
      block_power_updated_ = false;
    }

    public void readnbt(CompoundNBT nbt, boolean update_packet)
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      block_power_signal_ = nbt.getBoolean("powered");
      current_slot_index_ = nbt.getInt("act_slot_index");
      logic_ = nbt.getInt("logic");
    }

    protected void writenbt(CompoundNBT nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.putBoolean("powered", block_power_signal_);
      nbt.putInt("act_slot_index", current_slot_index_);
      nbt.putInt("logic", logic_);
    }

    public void block_updated()
    {
      boolean powered = level.hasNeighborSignal(worldPosition);
      if(block_power_signal_ != powered) block_power_updated_ = true;
      block_power_signal_ = powered;
      if(block_power_updated_) {
        tick_timer_ = 1;
      } else if(tick_timer_ > 4) {
        tick_timer_ = 4;
      }
    }

    public boolean is_input_slot(int index)
    { return (index >= 0) && (index < NUM_OF_SLOTS); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void load(BlockState state, CompoundNBT nbt)
    { super.load(state, nbt); readnbt(nbt, false); }

    @Override
    public CompoundNBT save(CompoundNBT nbt)
    { super.save(nbt); writenbt(nbt, false); return nbt; }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      Arrays.stream(item_handlers).forEach(LazyOptional::invalidate);
    }

    // INamable ----------------------------------------------------------------------------------------------

    @Override
    public ITextComponent getName()
    { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getDescriptionId() : "Factory placer"); }

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
    { return new PlacerContainer(id, inventory, this, IWorldPosCallable.create(level, worldPosition), fields); }

    // IInventory -------------------------------------------------------------------------------------------

    @Override
    public int getContainerSize()
    { return stacks_.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getItem(int index)
    { return (index < getContainerSize()) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int index, int count)
    { return ItemStackHelper.removeItem(stacks_, index, count); }

    @Override
    public ItemStack removeItemNoUpdate(int index)
    { return ItemStackHelper.takeItem(stacks_, index); }

    @Override
    public void setItem(int index, ItemStack stack)
    {
      if((index<0) || (index >= NUM_OF_SLOTS)) return;
      stacks_.set(index, stack);
      if(stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
      if(tick_timer_ > 8) tick_timer_ = 8;
      setChanged();
    }

    @Override
    public int getMaxStackSize()
    { return 64; }

    @Override
    public void setChanged()
    { super.setChanged(); }

    @Override
    public boolean stillValid(PlayerEntity player)
    { return ((getLevel().getBlockEntity(getBlockPos()) == this)) && (getBlockPos().distSqr(player.blockPosition()) < 64); }

    @Override
    public void startOpen(PlayerEntity player)
    {}

    @Override
    public void stopOpen(PlayerEntity player)
    { setChanged(); }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    { return (index>=0) && (index<NUM_OF_SLOTS); }

    @Override
    public void clearContent()
    { for(int i=0; i<stacks_.size(); ++i) stacks_.set(i, ItemStack.EMPTY); } // should search a better vectorizing method here.

    // Fields -----------------------------------------------------------------------------------------------

    protected final IIntArray fields = new IntArray(PlacerTileEntity.NUM_OF_FIELDS)
    {
      @Override
      public int get(int id)
      {
        switch(id) {
          case 0: return logic_;
          case 1: return block_power_signal_ ? 1 : 0;
          case 2: return MathHelper.clamp(current_slot_index_, 0, NUM_OF_SLOTS-1);
          default: return 0;
        }
      }
      @Override
      public void set(int id, int value)
      {
        switch(id) {
          case 0: logic_ = value; return;
          case 1: block_power_signal_ = (value != 0); return;
          case 2: current_slot_index_ = MathHelper.clamp(value, 0, NUM_OF_SLOTS-1); return;
          default: return;
        }
      }
    };

    // ISidedInventory --------------------------------------------------------------------------------------

    LazyOptional<? extends IItemHandler>[] item_handlers = SidedInvWrapper.create(this, Direction.UP);
    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[NUM_OF_SLOTS];
      for(int i=0; i<NUM_OF_SLOTS; ++i) SIDED_INV_SLOTS[i] = i;
    }

    @Override
    public int[] getSlotsForFace(Direction side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction)
    { return is_input_slot(index) && canPlaceItem(index, stack); }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction)
    { return false; }

    // Capability export ------------------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handlers[0].cast();
      return super.getCapability(capability, facing);
    }

    // ITickable and aux methods ----------------------------------------------------------------------------

    private static int next_slot(int i)
    { return (i<NUM_OF_SLOTS-1) ? (i+1) : 0; }

    private boolean spit_out(Direction facing)
    { return spit_out(facing, false); }

    private boolean spit_out(Direction facing, boolean all)
    {
      ItemStack stack = stacks_.get(current_slot_index_);
      ItemStack drop = stack.copy();
      if(!all) {
        stack.shrink(1);
        stacks_.set(current_slot_index_, stack);
        drop.setCount(1);
      } else {
        stacks_.set(current_slot_index_, ItemStack.EMPTY);
      }
      for(int i=0; i<8; ++i) {
        BlockPos p = worldPosition.relative(facing, i);
        if(!level.isEmptyBlock(p)) continue;
        level.addFreshEntity(new ItemEntity(level, (p.getX()+0.5), (p.getY()+0.5), (p.getZ()+0.5), drop));
        level.playSound(null, p, SoundEvents.ITEM_PICKUP, SoundCategory.BLOCKS, 0.7f, 0.8f);
        break;
      }
      return true;
    }

    private boolean try_place(Direction facing, boolean triggered)
    {
      if(level.isClientSide()) return false;
      BlockPos placement_pos = worldPosition.relative(facing);
      if(level.getBlockEntity(placement_pos) != null) return false;
      ItemStack current_stack = ItemStack.EMPTY;
      for(int i=0; i<NUM_OF_SLOTS; ++i) {
        if(current_slot_index_ >= NUM_OF_SLOTS) current_slot_index_ = 0;
        current_stack = stacks_.get(current_slot_index_);
        if(!current_stack.isEmpty()) break;
        current_slot_index_ = next_slot(current_slot_index_);
      }
      if(current_stack.isEmpty()) { current_slot_index_ = 0; return false; }
      boolean no_space = false;
      final Item item = current_stack.getItem();
      Block block = Block.byItem(item);
      if(block == Blocks.AIR) {
        if(item != null) {
          Auxiliaries.logDebug("Placer spit: No block for item " + item.getRegistryName().toString());
          return spit_out(facing); // Item not accepted
        } else {
          // try next slot
        }
      } else if(block instanceof IPlantable) {
        if(level.isEmptyBlock(placement_pos)) {
          // plant here, block below has to be valid soil.
          BlockState soilstate = level.getBlockState(placement_pos.below());
          if(!soilstate.getBlock().canSustainPlant(soilstate, level, worldPosition, Direction.UP, (IPlantable)block)) {
            block = Blocks.AIR;
          }
        } else {
          // adjacent block is the soil, plant above if the soil is valid.
          BlockState soilstate = level.getBlockState(placement_pos);
          if(soilstate.getBlock() == block) {
            // The plant is already planted from the case above.
            block = Blocks.AIR;
            no_space = true;
          } else if(!level.isEmptyBlock(placement_pos.above())) {
            // If this is the soil an air block is needed above, if that is blocked we can't plant.
            block = Blocks.AIR;
            no_space = true;
          } else if(!soilstate.getBlock().canSustainPlant(soilstate, level, worldPosition, Direction.UP, (IPlantable)block)) {
            // Would be space above, but it's not the right soil for the plant.
            block = Blocks.AIR;
          } else {
            // Ok, plant above.
            placement_pos = placement_pos.above();
          }
        }
      } else {
        final BlockState current_placement_pos_state = level.getBlockState(placement_pos);
        @SuppressWarnings("deprecation")
        final boolean replacable = (current_placement_pos_state.getBlock().canBeReplaced(current_placement_pos_state, Fluids.EMPTY)) && (
          level.isEmptyBlock(placement_pos) ||
          (current_placement_pos_state.getBlock() instanceof FlowingFluidBlock) ||
          (current_placement_pos_state.getMaterial().isReplaceable() && (!current_placement_pos_state.getMaterial().isSolid()))
        );
        if((!replacable) || (
          (!level.getEntitiesOfClass(Entity.class, new AxisAlignedBB(placement_pos), (Entity e)->{
            if(e.isPickable()) return true;
            if(triggered) return false;
            if((e instanceof ItemEntity)) {
              if((e.getDeltaMovement().y() > 0) || (e.getDeltaMovement().y() < -0.5)) return true; // not falling or falling by
              if(Math.abs(e.getDeltaMovement().x())+Math.abs(e.getDeltaMovement().z()) > 0) return true; // not straight
            }
            return false;
          }).isEmpty())
        )) {
          block = Blocks.AIR;
          no_space = true;
        }
      }
      // println("PLACE " + current_stack + "  --> " + block + " at " + placement_pos.subtract(pos) + "( item=" + item + ")");
      if(block != Blocks.AIR) {
        try {
          BlockItemUseContext use_context = null;
          {
            final FakePlayer placer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((ServerWorld)level);
            if(placer != null) {
              ItemStack placement_stack = current_stack.copy();
              placement_stack.setCount(1);
              ItemStack held = placer.getItemInHand(Hand.MAIN_HAND);
              placer.setItemInHand(Hand.MAIN_HAND, placement_stack);
              List<Direction> directions = new ArrayList<>(Arrays.asList(Direction.UP, facing.getOpposite()));
              for(Direction d:Direction.values()) if(!directions.contains(d)) directions.add(d);
              for(Direction d:directions) {
                Vector3d v = Vector3d.atCenterOf(placement_pos).subtract(Vector3d.atLowerCornerOf(d.getNormal()));
                use_context = new BlockItemUseContext(new ItemUseContext(placer, Hand.MAIN_HAND, new BlockRayTraceResult(v, d, placement_pos, false)));
                if(block.getStateForPlacement(use_context) == null) use_context = null;
                if(use_context!=null) break;
              }
              placer.setItemInHand(Hand.MAIN_HAND, held);
            }
          }
          BlockState placement_state = (use_context==null) ? (block.defaultBlockState()) : (block.getStateForPlacement(use_context));
          if(placement_state == null) {
            Auxiliaries.logDebug("Placer spit: No valid placement state for item " + item.getRegistryName().toString());
            return spit_out(facing);
          } else if((use_context!=null) && (item instanceof BlockItem)) {
            if(((BlockItem)item).place(use_context) != ActionResultType.FAIL) {
              SoundType stype = block.getSoundType(placement_state, level, worldPosition, null);
              if(stype != null) level.playSound(null, placement_pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
            } else {
              if(level.setBlock(placement_pos, placement_state, 1|2|8)) {
                SoundType stype = block.getSoundType(placement_state, level, worldPosition, null);
                if(stype != null) level.playSound(null, placement_pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
              }
            }
          } else {
            if(level.setBlock(placement_pos, placement_state, 1|2|8)) {
              SoundType stype = block.getSoundType(placement_state, level, worldPosition, null);
              if(stype != null) level.playSound(null, placement_pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
            }
          }
          current_stack.shrink(1);
          stacks_.set(current_slot_index_, current_stack);
          return true;
        } catch(Throwable e) {
          // The block really needs a player or other issues happened during placement.
          // A hard crash should not be fired here, instead spit out the item to indicated that this
          // block is not compatible.
          ModEngineersDecor.logger().error("Exception while trying to place " + ((block==null)?(""):(""+block)) + ", spitting out. Exception is: " + e);
          try {
            level.removeBlock(placement_pos, false);
          } catch(Throwable e1) {
            ModEngineersDecor.logger().error("Exception while removing failed block placement " + ((block==null)?(""):(""+block)) + ", spitting out. Exception is: " + e1);
          }
          return spit_out(facing, true);
        }
      }
      if((!no_space) && (!current_stack.isEmpty())) {
        // There is space, but the current plant cannot be planted there, so try next.
        for(int i=0; i<NUM_OF_SLOTS; ++i) {
          current_slot_index_ = next_slot(current_slot_index_);
          if(!stacks_.get(current_slot_index_).isEmpty()) break;
        }
      }
      return false;
    }

    @Override
    public void tick()
    {
      // Tick cycle pre-conditions
      if(level.isClientSide) return;
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      // Cycle init
      final BlockState state = level.getBlockState(worldPosition);
      if(!(state.getBlock() instanceof PlacerBlock)) { block_power_signal_= false; return; }
      final boolean updated = block_power_updated_;
      final boolean rssignal = ((logic_ & LOGIC_IGNORE_EXT)!=0) || ((logic_ & LOGIC_INVERTED)!=0)==(!block_power_signal_);
      final boolean trigger = ((logic_ & LOGIC_IGNORE_EXT)!=0) ||  (rssignal && ((updated) || ((logic_ & LOGIC_CONTINUOUS)!=0)));
      final Direction placer_facing = state.getValue(PlacerBlock.FACING);
      boolean dirty = updated;
      // Trigger edge detection for next cycle
      {
        boolean tr = level.hasNeighborSignal(worldPosition);
        block_power_updated_ = (block_power_signal_ != tr);
        block_power_signal_ = tr;
        if(block_power_updated_) dirty = true;
      }
      // Placing
      if(trigger && try_place(placer_facing, rssignal && updated)) dirty = true;
      if(dirty) setChanged();
      if(trigger && (tick_timer_ > TICK_INTERVAL)) tick_timer_ = TICK_INTERVAL;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class PlacerContainer extends Container implements Networking.INetworkSynchronisableContainer
  {
    protected static final String QUICK_MOVE_ALL = "quick-move-all";
    private static final int PLAYER_INV_START_SLOTNO = PlacerTileEntity.NUM_OF_SLOTS;
    private final PlayerEntity player_;
    private final IInventory inventory_;
    private final IWorldPosCallable wpc_;
    private final IIntArray fields_;
    private final InventoryRange player_inventory_range_;
    private final InventoryRange block_storage_range_;

    public final int field(int index) { return fields_.get(index); }

    public PlacerContainer(int cid, PlayerInventory player_inventory)
    { this(cid, player_inventory, new Inventory(PlacerTileEntity.NUM_OF_SLOTS), IWorldPosCallable.NULL, new IntArray(PlacerTileEntity.NUM_OF_FIELDS)); }

    private PlacerContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
    {
      super(ModContent.CT_FACTORY_PLACER, cid);
      fields_ = fields;
      wpc_ = wpc;
      player_ = player_inventory.player;
      inventory_ = block_inventory;
      block_storage_range_ = new InventoryRange(inventory_, 0, PlacerTileEntity.NUM_OF_SLOTS);
      player_inventory_range_ = InventoryRange.fromPlayerInventory(player_);
      int i=-1;
      // device slots (stacks 0 to 17)
      for(int y=0; y<3; ++y) {
        for(int x=0; x<6; ++x) {
          int xpos = 11+x*18, ypos = 9+y*17;
          addSlot(new Slot(inventory_, ++i, xpos, ypos));
        }
      }
      // player slots
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x, 9+x*18, 129)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlot(new Slot(player_inventory, x+y*9+9, 9+x*18, 71+y*18)); // player slots: 9..35
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
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Device slots
        if(!moveItemStackTo(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!moveItemStackTo(slot_stack, 0, PlacerTileEntity.NUM_OF_SLOTS, false)) return ItemStack.EMPTY;
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
    {
      CompoundNBT nbt = new CompoundNBT();
      nbt.putInt(key, value);
      Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt);
    }

    @OnlyIn(Dist.CLIENT)
    public void onGuiAction(String message, CompoundNBT nbt)
    {
      nbt.putString("action", message);
      Networking.PacketContainerSyncClientToServer.sendToServer(containerId, nbt);
    }

    @Override
    public void onServerPacketReceived(int windowId, CompoundNBT nbt)
    {}

    @Override
    public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
    {
      if(!(inventory_ instanceof PlacerTileEntity)) return;
      if(nbt.contains("action")) {
        boolean changed = false;
        final int slotId = nbt.contains("slot") ? nbt.getInt("slot") : -1;
        switch(nbt.getString("action")) {
          case QUICK_MOVE_ALL: {
            if((slotId >= 0) && (slotId < PLAYER_INV_START_SLOTNO) && (getSlot(slotId).hasItem())) {
              changed = block_storage_range_.move(getSlot(slotId).getSlotIndex(), player_inventory_range_, true, false, true, true);
            } else if((slotId >= PLAYER_INV_START_SLOTNO) && (slotId < PLAYER_INV_START_SLOTNO+36) && (getSlot(slotId).hasItem())) {
              changed = player_inventory_range_.move(getSlot(slotId).getSlotIndex(), block_storage_range_, true, false, false, true);
            }
          } break;
        }
        if(changed) {
          inventory_.setChanged();
          player.inventory.setChanged();
          broadcastChanges();
        }
      } else {
        PlacerTileEntity te = (PlacerTileEntity)inventory_;
        if(nbt.contains("logic")) te.logic_  = nbt.getInt("logic");
        if(nbt.contains("manual_trigger") && (nbt.getInt("manual_trigger")!=0)) { te.block_power_signal_=true; te.block_power_updated_=true; te.tick_timer_=1; }
        te.setChanged();
      }

    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class PlacerGui extends ContainerGui<PlacerContainer>
  {
    protected final PlayerEntity player_;
    protected final TooltipDisplay tooltip_ = new TooltipDisplay();

    public PlacerGui(PlacerContainer container, PlayerInventory player_inventory, ITextComponent title)
    { super(container, player_inventory, title); this.player_ = player_inventory.player; }

    @Override
    public void init()
    {
      super.init();
      {
        final String prefix = ModContent.FACTORY_PLACER.getDescriptionId() + ".tooltips.";
        final int x0 = getGuiLeft(), y0 = getGuiTop();
        tooltip_.init(
          new TipRange(x0+133, y0+49,  9,  9, new TranslationTextComponent(prefix + "rssignal")),
          new TipRange(x0+145, y0+49,  9,  9, new TranslationTextComponent(prefix + "inversion")),
          new TipRange(x0+159, y0+49,  9,  9, new TranslationTextComponent(prefix + "triggermode"))
        );
      }
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
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
      tooltip_.resetTimer();
      PlacerContainer container = (PlacerContainer)getMenu();
      int mx = (int)(mouseX - getGuiLeft() + .5), my = (int)(mouseY - getGuiTop() + .5);
      if((!isHovering(126, 1, 49, 60, mouseX, mouseY))) {
        return super.mouseClicked(mouseX, mouseY, mouseButton);
      } else if(isHovering(133, 49, 9, 9, mouseX, mouseY)) {
        container.onGuiAction("manual_trigger", 1);
      } else if(isHovering(145, 49, 9, 9, mouseX, mouseY)) {
        final int mask = (PlacerTileEntity.LOGIC_INVERTED|PlacerTileEntity.LOGIC_IGNORE_EXT|PlacerTileEntity.LOGIC_NOT_INVERTED);
        int logic = (container.field(0) & mask);
        switch(logic) {
          case PlacerTileEntity.LOGIC_NOT_INVERTED: logic = PlacerTileEntity.LOGIC_INVERTED; break;
          case PlacerTileEntity.LOGIC_INVERTED:     logic = PlacerTileEntity.LOGIC_IGNORE_EXT; break;
          case PlacerTileEntity.LOGIC_IGNORE_EXT:   logic = PlacerTileEntity.LOGIC_NOT_INVERTED; break;
          default: logic = PlacerTileEntity.LOGIC_IGNORE_EXT;
        }
        container.onGuiAction("logic", (container.field(0) & (~mask)) | logic);
      } else if(isHovering(159, 49, 7, 9, mouseX, mouseY)) {
        container.onGuiAction("logic", container.field(0) ^ PlacerTileEntity.LOGIC_CONTINUOUS);
      }
      return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type)
    {
      tooltip_.resetTimer();
      if((type == ClickType.QUICK_MOVE) && (slot!=null) && slot.hasItem() && Auxiliaries.isShiftDown() && Auxiliaries.isCtrlDown()) {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("slot", slotId);
        menu.onGuiAction(PlacerContainer.QUICK_MOVE_ALL, nbt);
      } else {
        super.slotClicked(slot, slotId, button, type);
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void renderBg(MatrixStack mx, float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.enableBlend();
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.getMinecraft().getTextureManager().bind(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/factory_placer_gui.png"));
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      blit(mx, x0, y0, 0, 0, w, h);
      PlacerContainer container = (PlacerContainer)getMenu();
      // active slot
      {
        int slot_index = container.field(2);
        if((slot_index < 0) || (slot_index >= PlacerTileEntity.NUM_OF_SLOTS)) slot_index = 0;
        int x = (x0+10+((slot_index % 6) * 18));
        int y = (y0+8+((slot_index / 6) * 17));
        blit(mx, x, y, 200, 8, 18, 18);
      }
      // redstone input
      {
        if(container.field(1) != 0) {
          blit(mx, x0+133, y0+49, 217, 49, 9, 9);
        }
      }
      // trigger logic
      {
        int inverter_offset_x = ((container.field(0) & PlacerTileEntity.LOGIC_INVERTED) != 0) ? 11 : 0;
        int inverter_offset_y = ((container.field(0) & PlacerTileEntity.LOGIC_IGNORE_EXT) != 0) ? 10 : 0;
        blit(mx, x0+145, y0+49, 177+inverter_offset_x, 49+inverter_offset_y, 9, 9);
        int pulse_mode_offset  = ((container.field(0) & PlacerTileEntity.LOGIC_CONTINUOUS    ) != 0) ? 9 : 0;
        blit(mx, x0+159, y0+49, 199+pulse_mode_offset, 49, 9, 9);
      }
      RenderSystem.disableBlend();
    }
  }

}
