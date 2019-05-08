/*
 * @file BlockDecorFurnaceElectrical.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED small electrical pass-through furnace.
 */
package wile.engineersdecor.blocks;


import wile.engineersdecor.ModEngineersDecor;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraft.stats.StatList;
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

public class BlockDecorFurnaceElectrical extends BlockDecorFurnace
{
  public BlockDecorFurnaceElectrical(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
  }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorFurnaceElectrical.BTileEntity(); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_ELECTRICAL_LAB_FURNACE, world, pos.getX(), pos.getY(), pos.getZ());
    player.addStat(StatList.FURNACE_INTERACTION);
    return true;
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    world.setBlockState(pos, state.withProperty(LIT, false));
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("inventory"))) return;
    NBTTagCompound inventory_nbt = stack.getTagCompound().getCompoundTag("inventory");
    if(inventory_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BlockDecorFurnaceElectrical.BTileEntity)) return;
    ((BlockDecorFurnaceElectrical.BTileEntity)te).readnbt(inventory_nbt);
    ((BlockDecorFurnaceElectrical.BTileEntity)te).markDirty();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rnd)
  {}

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
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_electrical_furnace_gui.png"));
      final int x0=(width-xSize)/2, y0=(height-ySize)/2, w=xSize, h=ySize;
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
      if(BTileEntity.isBurning(te))  {
        if(BlockDecorFurnace.BTileEntity.isBurning(te))  {
          final int hi = 13;
          final int k = heat_px(hi);
          drawTexturedModalRect(x0+61, y0+53+hi-k, 177, hi-k, 13, k);
        }
      }
      drawTexturedModalRect(x0+79, y0+30, 176, 15, 1+progress_px(17), 15);
      int we = energy_px(32, 8);
      if(we>0) drawTexturedModalRect(x0+88, y0+53, 185, 30, we, 13);
    }

    private int progress_px(int pixels)
    { final int tc=te.getField(2), T=te.getField(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int heat_px(int pixels)
    {
      int k = ((te.getField(0) * (pixels+1)) / (BlockDecorFurnaceElectrical.BTileEntity.HEAT_CAPACITY));
      return (k < pixels) ? k : pixels;
    }

    private int energy_px(int maxwidth, int quantization)
    {
      int k = ((maxwidth * te.getField(1) * 9) / 8) / (te.getMaxEnergyStored()+1);
      k = (k >= maxwidth-2) ? maxwidth : k;
      if(quantization > 0) k = ((k+(quantization/2))/quantization) * quantization;
      return k;
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends Container
  {
    private static final int PLAYER_INV_START_SLOTNO = 7;
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final BTileEntity te;
    private int burntime_left_, energy_stored_, proc_time_elapsed_, proc_time_needed_, speed_;

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      addSlotToContainer(new Slot(te, 0, 59, 28)); // smelting input
      addSlotToContainer(new Slot(te, 1, 16, 52)); // aux
      addSlotToContainer(new BSlotResult(playerInventory.player, te, 2, 101, 28)); // smelting result
      addSlotToContainer(new BSlotInpFifo(te, 3, 34, 28)); // input fifo 0
      addSlotToContainer(new BSlotInpFifo(te, 4, 16, 28)); // input fifo 1
      addSlotToContainer(new BSlotOutFifo(playerInventory.player, te, 5, 126, 28)); // out fifo 0
      addSlotToContainer(new BSlotOutFifo(playerInventory.player, te, 6, 144, 28)); // out fifo 1
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
        if(burntime_left_     != te.getField(0)) lis.sendWindowProperty(this, 0, te.getField(0));
        if(energy_stored_     != te.getField(1)) lis.sendWindowProperty(this, 1, te.getField(1));
        if(proc_time_elapsed_ != te.getField(2)) lis.sendWindowProperty(this, 2, te.getField(2));
        if(proc_time_needed_  != te.getField(3)) lis.sendWindowProperty(this, 3, te.getField(3));
        if(speed_             != te.getField(4)) lis.sendWindowProperty(this, 4, te.getField(4));
      }
      burntime_left_     = te.getField(0);
      energy_stored_     = te.getField(1);
      proc_time_elapsed_ = te.getField(2);
      proc_time_needed_  = te.getField(3);
      speed_             = te.getField(4);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    { te.setField(id, data); }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorFurnaceElectrical) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index==2) || (index==5) || (index==6)) {
        // Output slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if(index==1) {
        // Bypass slot
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(!BRecipes.instance().getSmeltingResult(slot_stack).isEmpty()) {
          if(
            (!mergeItemStack(slot_stack, 0, 1, false)) && // smelting input
            (!mergeItemStack(slot_stack, 3, 4, false)) && // fifo0
            (!mergeItemStack(slot_stack, 4, 5, false))    // fifo1
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
      return transferred;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends BlockDecorFurnace.BTileEntity implements ITickable, ISidedInventory, IEnergyStorage, IItemHandler
  {
    public static final int TICK_INTERVAL = 4;
    public static final int FIFO_INTERVAL = 20;
    public static final int HEAT_CAPACITY  = 200;
    public static final int HEAT_INCREMENT = 20;
    public static final int MAX_ENERGY_TRANSFER = 256;
    public static final int MAX_ENERGY_BUFFER = 32000;
    public static final int MAX_SPEED_SETTING = 2;
    public static final int NUM_OF_SLOTS = 7;
    public static final int SMELTING_INPUT_SLOT_NO  = 0;
    public static final int SMELTING_AUX_SLOT_NO    = 1;
    public static final int SMELTING_OUTPUT_SLOT_NO = 2;
    public static final int FIFO_INPUT_0_SLOT_NO    = 3;
    public static final int FIFO_INPUT_1_SLOT_NO    = 4;
    public static final int FIFO_OUTPUT_0_SLOT_NO   = 5;
    public static final int FIFO_OUTPUT_1_SLOT_NO   = 6;
    public static final int DEFAULT_SPEED_PERCENT   = 200;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 16 ;
    public static final int DEFAULT_SCALED_ENERGY_CONSUMPTION = DEFAULT_ENERGY_CONSUMPTION * HEAT_INCREMENT * DEFAULT_SPEED_PERCENT/100;

    private static int energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION;
    private static int transfer_energy_consumption_ = DEFAULT_SCALED_ENERGY_CONSUMPTION/8;
    private static int proc_speed_percent_ = DEFAULT_SPEED_PERCENT;
    private int burntime_left_;
    private int proc_time_elapsed_;
    private int proc_time_needed_;
    private int energy_stored_;
    private int speed_;
    private int tick_timer_;
    private int fifo_timer_;

    public static void on_config(int speed_percent, int standard_energy_per_tick)
    {
      proc_speed_percent_ = MathHelper.clamp(speed_percent, 10, 500);
      energy_consumption_ = MathHelper.clamp(standard_energy_per_tick, 10, 256) * HEAT_INCREMENT * proc_speed_percent_ / 100;
      transfer_energy_consumption_ = MathHelper.clamp(energy_consumption_/8, 8, HEAT_INCREMENT);
      ModEngineersDecor.logger.info("Config electrical furnace speed:" + proc_speed_percent_ + ", power consumption:" + energy_consumption_);
    }

    public BTileEntity()
    { super(); reset(); }

    public void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      burntime_left_ = 0;
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
      energy_stored_= 0;
      speed_ = 0;
    }

    public void readnbt(NBTTagCompound compound)
    {
      reset();
      ItemStackHelper.loadAllItems(compound, this.stacks_);
      while(this.stacks_.size() < NUM_OF_SLOTS) this.stacks_.add(ItemStack.EMPTY);
      burntime_left_ = compound.getInteger("BurnTime");
      proc_time_elapsed_ = compound.getInteger("CookTime");
      proc_time_needed_ = compound.getInteger("CookTimeTotal");
      energy_stored_ = compound.getInteger("Energy");
      speed_ = compound.getInteger("SpeedSetting");
    }

    protected void writenbt(NBTTagCompound compound)
    {
      compound.setInteger("BurnTime", MathHelper.clamp(burntime_left_,0 , HEAT_CAPACITY));
      compound.setInteger("CookTime", MathHelper.clamp(proc_time_elapsed_, 0, MAX_BURNTIME));
      compound.setInteger("CookTimeTotal", MathHelper.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      compound.setInteger("Energy", MathHelper.clamp(energy_stored_,0 , MAX_ENERGY_BUFFER));
      compound.setInteger("SpeedSetting", MathHelper.clamp(speed_, -1, MAX_SPEED_SETTING));
      ItemStackHelper.saveAllItems(compound, stacks_);
    }

    // TileEntity ------------------------------------------------------------------------------
    // IWorldNamable ---------------------------------------------------------------------------
    // IInventory ------------------------------------------------------------------------------

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
      switch(index) {
        case SMELTING_INPUT_SLOT_NO:
        case FIFO_INPUT_0_SLOT_NO:
        case FIFO_INPUT_1_SLOT_NO:
          return true;
        default:
          return false;
      }
    }

    @Override
    public int getField(int id)
    {
      switch (id) {
        case 0: return burntime_left_;
        case 1: return energy_stored_;
        case 2: return proc_time_elapsed_;
        case 3: return proc_time_needed_;
        case 4: return speed_;
        default: return 0;
      }
    }

    @Override
    public void setField(int id, int value)
    {
      switch(id) {
        case 0: burntime_left_ = value; break;
        case 1: energy_stored_ = value; break;
        case 2: proc_time_elapsed_ = value; break;
        case 3: proc_time_needed_ = value; break;
        case 4: speed_ = value; break;
      }
    }

    @Override
    public int getFieldCount()
    {  return 7; }

    public boolean isBurning()
    { return (burntime_left_ > 0); }

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

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS = new int[] {
      SMELTING_INPUT_SLOT_NO, SMELTING_AUX_SLOT_NO, SMELTING_OUTPUT_SLOT_NO,
      FIFO_INPUT_0_SLOT_NO, FIFO_INPUT_1_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO
    };

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
    { return isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return ((index!=SMELTING_INPUT_SLOT_NO) && (index!=FIFO_INPUT_0_SLOT_NO) && (index!=FIFO_INPUT_1_SLOT_NO)) || (stack.getItem()==Items.BUCKET); }

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
    { return SIDED_INV_SLOTS.length; }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(int index)
    { return ((index < 0) || (index >= SIDED_INV_SLOTS.length)) ? ItemStack.EMPTY : stacks_.get(SIDED_INV_SLOTS[index]); }

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
      if((index < 0) || (index >= SIDED_INV_SLOTS.length)) return ItemStack.EMPTY;
      int slotno = SIDED_INV_SLOTS[index];
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
    {
      if(amount == 0) return ItemStack.EMPTY;
      if((index < 0) || (index >= SIDED_INV_SLOTS.length)) return ItemStack.EMPTY;
      int slotno = SIDED_INV_SLOTS[index];
      ItemStack stackInSlot = getStackInSlot(slotno);
      if(stackInSlot.isEmpty()) return ItemStack.EMPTY;
      if(!canExtractItem(slotno, stackInSlot, EnumFacing.DOWN)) return ItemStack.EMPTY;
      if(simulate) {
        if(stackInSlot.getCount() < amount) return stackInSlot.copy();
        ItemStack ostack = stackInSlot.copy();
        ostack.setCount(amount);
        return ostack;
      } else {
        ItemStack ostack = decrStackSize(slotno, Math.min(stackInSlot.getCount(), amount));
        markDirty();
        return ostack;
      }
    }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return ((cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || (cap==CapabilityEnergy.ENERGY)) || super.hasCapability(cap, facing); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if((capability == CapabilityEnergy.ENERGY) || (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
        return ((T)this);
      } else {
        return super.getCapability(capability, facing);
      }
    }

    // ITickable ------------------------------------------------------------------------------------

    private boolean adjacent_inventory_shift(boolean inp, boolean out)
    {
      boolean dirty = false;
      if(energy_stored_  < transfer_energy_consumption_) return false;
      final IBlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof BlockDecorFurnaceElectrical)) return false;
      final EnumFacing out_facing = state.getValue(FACING);
      final EnumFacing inp_facing = state.getValue(FACING).getOpposite();
      if(out && (!stacks_.get(FIFO_OUTPUT_1_SLOT_NO).isEmpty())) {
        TileEntity te = world.getTileEntity(pos.offset(out_facing));
        if((te!=null) && (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inp_facing))) {
          IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, inp_facing);
          ItemStack remaining = ItemHandlerHelper.insertItemStacked(hnd, stacks_.get(FIFO_OUTPUT_1_SLOT_NO).copy(), false);
          stacks_.set(FIFO_OUTPUT_1_SLOT_NO, remaining);
          energy_stored_ -= transfer_energy_consumption_;
          dirty = true;
        }
      }
      if(inp && (stacks_.get(FIFO_INPUT_1_SLOT_NO).isEmpty())) {
        TileEntity te = world.getTileEntity(pos.offset(inp_facing));
        if((te!=null) && (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out_facing))) {
          IItemHandler hnd = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, out_facing);
          for(int i=0; i< hnd.getSlots(); ++i) {
            ItemStack adj_stack = hnd.getStackInSlot(i);
            if(!adj_stack.isEmpty()) {
              ItemStack my_stack = adj_stack.copy();
              if(my_stack.getCount() > getInventoryStackLimit()) my_stack.setCount(getInventoryStackLimit());
              adj_stack.shrink(my_stack.getCount());
              stacks_.set(FIFO_INPUT_1_SLOT_NO, my_stack);
              energy_stored_ -= transfer_energy_consumption_;
              dirty = true;
              break;
            }
          }
        }
      }
      return dirty;
    }

    // returns TE dirty
    private boolean heat_up()
    {
      if(energy_stored_ < (energy_consumption_)) return false;
      if(burntime_left_ >= (HEAT_CAPACITY-HEAT_INCREMENT)) return false;
      energy_stored_ -= energy_consumption_;
      burntime_left_ += HEAT_INCREMENT;
      this.markDirty();
      return true;
    }

    private void sync_blockstate()
    {
      final IBlockState state = world.getBlockState(pos);
      if((state.getBlock() instanceof BlockDecorFurnaceElectrical) && (state.getValue(LIT) != isBurning())) {
        world.setBlockState(pos, state.withProperty(LIT, isBurning()), 2);
      }
    }

    @Override
    public void update()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final boolean was_burning = isBurning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(world.isRemote) return;
      boolean update_blockstate = (was_burning != isBurning());
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
      if((!(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty()) && (energy_stored_ >= energy_consumption_)) {
        final boolean can_smelt = canSmelt();
        if((!can_smelt) && (BRecipes.instance().getSmeltingResult(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
          // bypass
          if(transferItems(SMELTING_INPUT_SLOT_NO, SMELTING_OUTPUT_SLOT_NO, 1)) dirty = true;
        } else {
          // smelt
          if(!isBurning() && can_smelt) {
            if(heat_up()) { dirty = true; update_blockstate = true; }
          }
          if(isBurning() && can_smelt) {
            if(heat_up()) dirty = true;
            proc_time_elapsed_ += (TICK_INTERVAL * proc_speed_percent_/100);
            if(proc_time_elapsed_ >= proc_time_needed_) {
              proc_time_elapsed_ = 0;
              proc_time_needed_ = getCookTime(stacks_.get(SMELTING_INPUT_SLOT_NO));
              smeltItem();
              dirty = true;
              shift_out = true;
            }
          } else {
            proc_time_elapsed_ = 0;
          }
        }
      } else if(proc_time_elapsed_ > 0) {
        proc_time_elapsed_ -= ((stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty() ? 20 : 1);
        if(proc_time_elapsed_ < 0) { proc_time_elapsed_ = 0; shift_out = true; update_blockstate = true; }
      }
      if(update_blockstate) {
        dirty = true;
        sync_blockstate();
      }
      if(adjacent_inventory_shift(shift_in, shift_out)) dirty = true;
      if(dirty) markDirty();
    }
  }
}
