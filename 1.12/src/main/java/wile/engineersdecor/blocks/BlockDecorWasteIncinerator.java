/*
 * @file BlockDecorWasteIncinerator.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Trash/void/nullifier device with internal fifos.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class BlockDecorWasteIncinerator extends BlockDecor
{
  public static final PropertyBool LIT = BlockDecorFurnace.LIT;

  public BlockDecorWasteIncinerator(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, LIT); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(LIT, (meta & 0x4)!=0); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return (state.getValue(LIT) ? 4 : 0); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(LIT, false); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(IBlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos)
  { return Container.calcRedstone(world.getTileEntity(pos)); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorWasteIncinerator.BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("inventory"))) return;
    NBTTagCompound inventory_nbt = stack.getTagCompound().getCompoundTag("inventory");
    if(inventory_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BlockDecorWasteIncinerator.BTileEntity)) return;
    ((BlockDecorWasteIncinerator.BTileEntity)te).readnbt(inventory_nbt);
    ((BlockDecorWasteIncinerator.BTileEntity)te).markDirty();
  }

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return super.removedByPlayer(state, world, pos, player, willHarvest);
    ItemStack stack = new ItemStack(this, 1);
    NBTTagCompound inventory_nbt = new NBTTagCompound();
    ItemStackHelper.saveAllItems(inventory_nbt, ((BTileEntity)te).stacks_, false);
    if(!inventory_nbt.isEmpty()) {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setTag("inventory", inventory_nbt);
      stack.setTagCompound(nbt);
    }
    world.spawnEntity(new EntityItem(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack));
    world.setBlockToAir(pos);
    world.removeTileEntity(pos);
    return false;
  }

  @Override
  public void onBlockExploded(World world, BlockPos pos, Explosion explosion)
  {
    if(world.isRemote) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    for(ItemStack stack: ((BTileEntity)te).stacks_) {
      if(!stack.isEmpty()) world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
    }
    ((BTileEntity)te).reset();
    super.onBlockExploded(world, pos, explosion);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_SMALL_WASTE_INCINERATOR, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rnd)
  {
    if((state.getBlock()!=this) || (!state.getValue(LIT))) return;
    final double rv = rnd.nextDouble();
    if(rv > 0.5) return;
    final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
    final double xr=rnd.nextDouble()*0.4-0.2, yr=rnd.nextDouble()*0.5, zr=rnd.nextDouble()*0.4-0.2;
    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xr, y+yr, z+zr, 0.0, 0.0, 0.0);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // ModEngineersDecor.GuiHandler connectors
  //--------------------------------------------------------------------------------------------------------------------

  public static Object getServerGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof BTileEntity) ? (new BContainer(player.inventory, world, pos, (BTileEntity)te)) : null; }

  public static Object getClientGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof BTileEntity) ? (new BGui(player.inventory, world, pos, (BTileEntity)te)) : null; }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  private static class BGui extends GuiContainer
  {
    private final BTileEntity te;

    public BGui(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    { super(new BContainer(playerInventory, world, pos, te)); this.te = te; }

    @Override
    public void initGui()
    { super.initGui(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
      drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_waste_incinerator_gui.png"));
      final int x0=(width-xSize)/2, y0=(height-ySize)/2, w=xSize, h=ySize;
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends Container
  {
    private static final int PLAYER_INV_START_SLOTNO = BTileEntity.NUM_OF_SLOTS;
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final BTileEntity te;
    private int proc_time_needed_;

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      int i=-1;
      addSlotToContainer(new Slot(te, ++i, 13, 9));
      addSlotToContainer(new Slot(te, ++i, 37, 12));
      addSlotToContainer(new Slot(te, ++i, 54, 13));
      addSlotToContainer(new Slot(te, ++i, 71, 14));
      addSlotToContainer(new Slot(te, ++i, 88, 15));
      addSlotToContainer(new Slot(te, ++i, 105, 16));
      addSlotToContainer(new Slot(te, ++i, 122, 17));
      addSlotToContainer(new Slot(te, ++i, 139, 18));
      addSlotToContainer(new Slot(te, ++i, 144, 38));
      addSlotToContainer(new Slot(te, ++i, 127, 39));
      addSlotToContainer(new Slot(te, ++i, 110, 40));
      addSlotToContainer(new Slot(te, ++i, 93, 41));
      addSlotToContainer(new Slot(te, ++i, 76, 42));
      addSlotToContainer(new Slot(te, ++i, 59, 43));
      addSlotToContainer(new Slot(te, ++i, 42, 44));
      addSlotToContainer(new Slot(te, ++i, 17, 58));
      for(int x=0; x<9; ++x) {
        addSlotToContainer(new Slot(playerInventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
    }

    @Override
    public void addListener(IContainerListener listener)
    { super.addListener(listener); listener.sendAllWindowProperties(this, te); }

    @Override
    public void detectAndSendChanges()
    {
      super.detectAndSendChanges();
      for(int i=0; i<listeners.size(); ++i) {
        IContainerListener lis = listeners.get(i);
        if(proc_time_needed_  != te.getField(3)) lis.sendWindowProperty(this, 3, te.getField(3));
      }
      proc_time_needed_ = te.getField(3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    { te.setField(id, data); }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorWasteIncinerator) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Device slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!mergeItemStack(slot_stack, 0, PLAYER_INV_START_SLOTNO-1, true)) return ItemStack.EMPTY;
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
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable, ISidedInventory, IEnergyStorage, IItemHandler
  {
    public static final int TICK_INTERVAL = 20;
    public static final int ENERGIZED_TICK_INTERVAL = 5;
    public static final int MAX_ENERGY_BUFFER = 16000;
    public static final int MAX_ENERGY_TRANSFER = 256;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 16;
    public static final int NUM_OF_SLOTS = 16;
    public static final int INPUT_SLOT_NO = 0;
    public static final int BURN_SLOT_NO = NUM_OF_SLOTS-1;

    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;

    private int tick_timer_;
    private int check_timer_;
    private int energy_stored_;
    protected NonNullList<ItemStack> stacks_;

    public static void on_config(int speed_percent, int fuel_efficiency_percent, int boost_energy_per_tick)
    {
      energy_consumption = MathHelper.clamp(boost_energy_per_tick, 16, 512);
      ModEngineersDecor.logger.info("Config waste incinerator boost energy consumption:" + energy_consumption);
    }

    public BTileEntity()
    { reset(); }

    protected void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      check_timer_ = 0;
      tick_timer_ = 0;
    }

    public void readnbt(NBTTagCompound compound)
    {
      reset();
      ItemStackHelper.loadAllItems(compound, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      energy_stored_ = compound.getInteger("Energy");
    }

    protected void writenbt(NBTTagCompound compound)
    {
      compound.setInteger("Energy", MathHelper.clamp(energy_stored_,0 , MAX_ENERGY_BUFFER));
      ItemStackHelper.saveAllItems(compound, stacks_);
    }

    private ItemStack shiftStacks(final int index_from, final int index_to)
    {
      if(index_from >= index_to) return ItemStack.EMPTY;
      ItemStack out_stack = ItemStack.EMPTY;
      ItemStack stack = stacks_.get(index_from);
      for(int i=index_from+1; i<=index_to; ++i) {
        out_stack = stacks_.get(i);
        stacks_.set(i, stack);
        stack = out_stack;
      }
      stacks_.set(index_from, ItemStack.EMPTY);
      return out_stack;
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
        stacks_.set(index_to, from.splitStack(count));
      } else if(to.getCount() >= to.getMaxStackSize()) {
        changed = false;
      } else if((!from.isItemEqual(to)) || (!ItemStack.areItemStackTagsEqual(from, to))) {
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

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorWasteIncinerator)); }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    { super.readFromNBT(compound); readnbt(compound); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    { super.writeToNBT(compound); writenbt(compound); return compound; }

    // IWorldNamable ---------------------------------------------------------------------------

    @Override
    public String getName()
    { final Block block=getBlockType(); return (block!=null) ? (block.getTranslationKey() + ".name") : (""); }

    @Override
    public boolean hasCustomName()
    { return false; }

    @Override
    public ITextComponent getDisplayName()
    { return new TextComponentTranslation(getName(), new Object[0]); }

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
      stacks_.set(index, stack);
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      markDirty();
    }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    { return ((world.getTileEntity(pos) == this) && (player.getDistanceSq(pos.getX()+0.5d, pos.getY()+0.5d, pos.getZ()+0.5d) <= 64.0d)); }

    @Override
    public void openInventory(EntityPlayer player)
    {}

    @Override
    public void closeInventory(EntityPlayer player)
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return index==0; }

    @Override
    public int getField(int id)
    { return 0; }

    @Override
    public void setField(int id, int value)
    {}

    @Override
    public int getFieldCount()
    {  return 4; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS = new int[] { INPUT_SLOT_NO };

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
    { return isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return false; }

    // IEnergyStorage ----------------------------------------------------------------------------

    public boolean canExtract()
    { return false; }

    public boolean canReceive()
    { return true; }

    public int getMaxEnergyStored()
    { return MAX_ENERGY_BUFFER; }

    public int getEnergyStored()
    { return energy_stored_; }

    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    public int receiveEnergy(int maxReceive, boolean simulate)
    {
      if(energy_stored_ >= MAX_ENERGY_BUFFER) return 0;
      int n = Math.min(maxReceive, (MAX_ENERGY_BUFFER - energy_stored_));
      if(n > MAX_ENERGY_TRANSFER) n = MAX_ENERGY_TRANSFER;
      if(!simulate) {energy_stored_ += n; markDirty(); }
      return n;
    }

    // IItemHandler  --------------------------------------------------------------------------------

    @Override
    public int getSlots()
    { return 1; }

    @Override
    public int getSlotLimit(int index)
    { return getInventoryStackLimit(); }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack)
    { return true; }

    @Override
    @Nonnull
    public ItemStack insertItem(int index, @Nonnull ItemStack stack, boolean simulate)
    {
      if(stack.isEmpty()) return ItemStack.EMPTY;
      if(index != 0) return ItemStack.EMPTY;
      int slotno = 0;
      ItemStack slotstack = getStackInSlot(slotno);
      if(!slotstack.isEmpty())
      {
        if(slotstack.getCount() >= Math.min(slotstack.getMaxStackSize(), getSlotLimit(index))) return stack;
        if(!ItemHandlerHelper.canItemStacksStack(stack, slotstack)) return stack;
        if(!canInsertItem(slotno, stack, EnumFacing.UP) || (!isItemValidForSlot(slotno, stack))) return stack;
        int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index)) - slotstack.getCount();
        if(stack.getCount() <= n) {
          if(!simulate) {
            ItemStack copy = stack.copy();
            copy.grow(slotstack.getCount());
            setInventorySlotContents(slotno, copy);
          }
          return ItemStack.EMPTY;
        } else {
          stack = stack.copy();
          if(!simulate) {
            ItemStack copy = stack.splitStack(n);
            copy.grow(slotstack.getCount());
            setInventorySlotContents(slotno, copy);
            return stack;
          } else {
            stack.shrink(n);
            return stack;
          }
        }
      } else {
        if(!canInsertItem(slotno, stack, EnumFacing.UP) || (!isItemValidForSlot(slotno, stack))) return stack;
        int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index));
        if(n < stack.getCount()) {
          stack = stack.copy();
          if(!simulate) {
            setInventorySlotContents(slotno, stack.splitStack(n));
            return stack;
          } else {
            stack.shrink(n);
            return stack;
          }
        } else {
          if(!simulate) setInventorySlotContents(slotno, stack);
          return ItemStack.EMPTY;
        }
      }
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int index, int amount, boolean simulate)
    { return ItemStack.EMPTY; }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return ((cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || (cap==CapabilityEnergy.ENERGY)) || super.hasCapability(cap, facing); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if((facing != null) && (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
        return (T)this;
      } else if(capability == CapabilityEnergy.ENERGY) {
        return (T)this;
      } else {
        return super.getCapability(capability, facing);
      }
    }

    // ITickable ------------------------------------------------------------------------------------

    @Override
    public void update()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      if(world.isRemote) return;
      boolean dirty = false;
      ItemStack processing_stack = stacks_.get(BURN_SLOT_NO);
      final boolean was_processing = !processing_stack.isEmpty();
      boolean is_processing = was_processing;
      boolean new_stack_processing = false;
      if((!stacks_.get(0).isEmpty()) && transferItems(0, 1, getInventoryStackLimit())) dirty = true;
      ItemStack first_stack = stacks_.get(0);
      boolean shift = !first_stack.isEmpty();
      if(is_processing) {
        processing_stack.shrink(1);
        if(processing_stack.getCount() <= 0) {
          processing_stack = ItemStack.EMPTY;
          is_processing = false;
        }
        stacks_.set(BURN_SLOT_NO, processing_stack);
        if(energy_stored_ >= (energy_consumption * TICK_INTERVAL)) {
          energy_stored_ -= (energy_consumption * TICK_INTERVAL);
          tick_timer_ = ENERGIZED_TICK_INTERVAL;
        }
        dirty = true;
      }
      if(shift) {
        int max_shift_slot_no = BURN_SLOT_NO-1;
        for(int i=1; i<BURN_SLOT_NO-1; ++i) { if(stacks_.get(i).isEmpty()) { max_shift_slot_no=i; break; } }
        if(max_shift_slot_no < (BURN_SLOT_NO-1)) {
          // re-stack
          boolean stacked = false;
          for(int i=1; i<=max_shift_slot_no; ++i) {
            if(transferItems(i-1, i, getInventoryStackLimit())) {
              dirty = true;
              stacked = true;
              break;
            }
          }
          if(!stacked) {
            shiftStacks(0, max_shift_slot_no);
          }
        } else if(!is_processing) {
          shiftStacks(0, BURN_SLOT_NO);
          dirty = true;
        }
      }
      if((was_processing != is_processing) || (new_stack_processing)) {
        if(new_stack_processing) world.playSound(null, pos, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.05f, 2.4f);
        final IBlockState state = world.getBlockState(pos);
        if(state.getBlock() instanceof BlockDecorWasteIncinerator) {
          world.setBlockState(pos, state.withProperty(LIT, is_processing), 2|16);
        }
      }
      if(dirty) markDirty();
    }
  }

}
