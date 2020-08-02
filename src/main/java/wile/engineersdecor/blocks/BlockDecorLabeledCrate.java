/*
 * @file BlockDecorLabeledCrate.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Storage crate with a content hint.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorLabeledCrate extends BlockDecorDirectedHorizontal
{
  public static void on_config(int stack_limit)
  {
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public BlockDecorLabeledCrate(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new LabeledCrateTileEntity(); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_LABELED_CRATE, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("inventory"))) return;
    NBTTagCompound inventory_nbt = stack.getTagCompound().getCompoundTag("inventory");
    if(inventory_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof LabeledCrateTileEntity)) return;
    ((LabeledCrateTileEntity)te).readnbt(inventory_nbt);
    ((LabeledCrateTileEntity)te).markDirty();
  }

  private ItemStack itemize_with_inventory(World world, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof LabeledCrateTileEntity)) return ItemStack.EMPTY;
    ItemStack stack = new ItemStack(this, 1);
    NBTTagCompound inventory_nbt = new NBTTagCompound();
    ItemStackHelper.saveAllItems(inventory_nbt, ((LabeledCrateTileEntity)te).stacks_, false);
    if(!inventory_nbt.isEmpty()) {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setTag("inventory", inventory_nbt);
      stack.setTagCompound(nbt);
    }
    return stack;
  }

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  {
    if(world.isRemote) return true;
    final ItemStack stack = itemize_with_inventory(world, pos);
    if(stack != ItemStack.EMPTY) {
      world.spawnEntity(new EntityItem(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack));
      world.setBlockToAir(pos);
      world.removeTileEntity(pos);
      return false;
    } else {
      return super.removedByPlayer(state, world, pos, player, willHarvest);
    }
  }

  @Override
  public void onBlockExploded(World world, BlockPos pos, Explosion explosion)
  {
    if(world.isRemote) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof LabeledCrateTileEntity)) return;
    for(ItemStack stack: ((LabeledCrateTileEntity)te).stacks_) {
      if(!stack.isEmpty()) world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
    }
    ((LabeledCrateTileEntity)te).reset();
    super.onBlockExploded(world, pos, explosion);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(IBlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos)
  { return Container.calcRedstone(world.getTileEntity(pos)); }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  private static class BGui extends GuiContainer
  {
    private final LabeledCrateTileEntity te;

    public BGui(InventoryPlayer playerInventory, World world, BlockPos pos, LabeledCrateTileEntity te)
    {
      super(new BContainer(playerInventory, world, pos, te));
      this.te = te;
      xSize = 213;
      ySize = 206;
    }

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
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/labeled_crate_gui.png"));
      final int x0=guiLeft, y0=guiTop, w=xSize, h=ySize;
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class LabeledCrateTileEntity extends TileEntity implements ISidedInventory
  {
    public static final int NUM_OF_FIELDS = 1;
    public static final int NUM_OF_SLOTS = 55;
    public static final int ITEMFRAME_SLOTNO = 54;

    // LabeledCrateTileEntity --------------------------------------------------------------------------

    protected NonNullList<ItemStack> stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);

    public LabeledCrateTileEntity()
    { reset(); }

    protected void reset()
    { stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void readnbt(NBTTagCompound compound)
    {
      NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(compound, stacks);
      while(stacks.size() < NUM_OF_SLOTS) stacks.add(ItemStack.EMPTY);
      stacks_ = stacks;
    }

    protected void writenbt(NBTTagCompound compound)
    { ItemStackHelper.saveAllItems(compound, stacks_); }

    public ItemStack getItemFrameStack()
    { return (stacks_.size() > ITEMFRAME_SLOTNO)?(stacks_.get(ITEMFRAME_SLOTNO)):(ItemStack.EMPTY); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock()!=ns.getBlock())||(!(ns.getBlock() instanceof BlockDecorLabeledCrate));}

    @Override
    public void readFromNBT(NBTTagCompound compound)
    { super.readFromNBT(compound); readnbt(compound); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    { super.writeToNBT(compound); writenbt(compound); return compound; }

    @Override
    public NBTTagCompound getUpdateTag()
    { NBTTagCompound nbt = super.getUpdateTag(); writenbt(nbt); return nbt; }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    { return new SPacketUpdateTileEntity(pos, 1, getUpdateTag()); }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) // on client
    { super.readFromNBT(pkt.getNbtCompound()); readnbt(pkt.getNbtCompound()); super.onDataPacket(net, pkt); }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) // on client
    { readFromNBT(tag); }

    // INameable  ---------------------------------------------------------------------------

    @Override
    public String getName()
    { final Block block = getBlockType(); return (block!=null)?(block.getTranslationKey()+".name"):(""); }

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
    { for(ItemStack stack : stacks_) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return ((index >= 0)&&(index < getSizeInventory()))?stacks_.get(index):ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks_, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks_, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      stacks_.set(index, stack);
      markDirty();
      if(!getWorld().isRemote) {
        // This should result in sending TE data (getUpdateTag etc) to the client for the TER.
        IBlockState state = world.getBlockState(getPos());
        getWorld().notifyBlockUpdate(getPos(), state, state, 2|16|32);
      }
    }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    { return getPos().distanceSq(player.getPosition()) < 36; }

    @Override
    public void openInventory(EntityPlayer player)
    {}

    @Override
    public void closeInventory(EntityPlayer player)
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return (index!=ITEMFRAME_SLOTNO); }

    @Override
    public void clear()
    { stacks_.clear(); }

    // Fields -----------------------------------------------------------------------------------------------

    @Override
    public int getField(int id)
    { return 0; }

    @Override
    public void setField(int id, int value)
    {}

    @Override
    public int getFieldCount()
    {  return 0; }

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[LabeledCrateTileEntity.NUM_OF_SLOTS-1];
      for(int i = 0; i < SIDED_INV_SLOTS.length; ++i) SIDED_INV_SLOTS[i] = i;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction)
    { return true; }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return true; }

    // IItemHandler  --------------------------------------------------------------------------------

    protected static class BItemHandler implements IItemHandler
    {
      private LabeledCrateTileEntity te;

      BItemHandler(LabeledCrateTileEntity te)
      {
        this.te = te;
      }

      @Override
      public int getSlots()
      {
        return ITEMFRAME_SLOTNO;
      } // iframe slot is the last

      @Override
      public int getSlotLimit(int index)
      {
        return te.getInventoryStackLimit();
      }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      {
        return true;
      }

      @Override
      @Nonnull
      public ItemStack insertItem(int slotno, @Nonnull ItemStack stack, boolean simulate)
      {
        if(stack.isEmpty()) return ItemStack.EMPTY;
        if((slotno < 0)||((slotno >= NUM_OF_SLOTS))||((slotno==ITEMFRAME_SLOTNO))) return ItemStack.EMPTY;
        ItemStack slotstack = getStackInSlot(slotno);
        if(!slotstack.isEmpty()) {
          if(slotstack.getCount() >= Math.min(slotstack.getMaxStackSize(), getSlotLimit(slotno))) return stack;
          if(!ItemHandlerHelper.canItemStacksStack(stack, slotstack)) return stack;
          if(!te.canInsertItem(slotno, stack, EnumFacing.UP)||(!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(slotno))-slotstack.getCount();
          if(stack.getCount() <= n) {
            if(!simulate) {
              ItemStack copy = stack.copy();
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
            }
            return ItemStack.EMPTY;
          }
          else {
            stack = stack.copy();
            if(!simulate) {
              ItemStack copy = stack.splitStack(n);
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
              return stack;
            }
            else {
              stack.shrink(n);
              return stack;
            }
          }
        }
        else {
          if(!te.canInsertItem(slotno, stack, EnumFacing.UP)||(!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(slotno));
          if(n < stack.getCount()) {
            stack = stack.copy();
            if(!simulate) {
              te.setInventorySlotContents(slotno, stack.splitStack(n));
              return stack;
            }
            else {
              stack.shrink(n);
              return stack;
            }
          }
          else {
            if(!simulate) te.setInventorySlotContents(slotno, stack);
            return ItemStack.EMPTY;
          }
        }
      }

      @Override
      @Nonnull
      public ItemStack extractItem(int index, int amount, boolean simulate)
      {
        if((index < 0)||((index >= NUM_OF_SLOTS))||((index==ITEMFRAME_SLOTNO))) return ItemStack.EMPTY;
        if(!simulate) return ItemStackHelper.getAndSplit(te.stacks_, index, amount);
        ItemStack stack = te.stacks_.get(index).copy();
        if(stack.getCount() > amount) stack.setCount(amount);
        return stack;
      }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      {
        return te.getStackInSlot(index);
      }
    }

    // Capability export ----------------------------------------------------------------------------

    private final IItemHandler item_handler_ = new BItemHandler(this);

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    {
      return (cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)||super.hasCapability(cap, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T)item_handler_;
      return super.getCapability(capability, facing);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends Container
  {
    //------------------------------------------------------------------------------------------------------------------
    private static final int PLAYER_INV_START_SLOTNO = LabeledCrateTileEntity.NUM_OF_SLOTS;
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final LabeledCrateTileEntity te;
    //------------------------------------------------------------------------------------------------------------------

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, LabeledCrateTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      int i=-1;
      // storage slots (stacks 0 to 53)
      for(int y=0; y<6; ++y) {
        for(int x=0; x<9; ++x) {
          int xpos = 28+x*18, ypos = 10+y*18;
          addSlotToContainer(new Slot(te, ++i, xpos, ypos));
        }
      }
      // picture frame slot (54)
      addSlotToContainer(new Slot(te, ++i, 191, 100) {
        @Override public int getSlotStackLimit(){return 1;}
      });
      // player slots
      for(int x=0; x<9; ++x) {
        addSlotToContainer(new Slot(playerInventory, x, 28+x*18, 183)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 28+x*18, 125+y*18)); // player slots: 9..35
        }
      }
    }

    @Override
    public void addListener(IContainerListener listener)
    { super.addListener(listener); listener.sendAllWindowProperties(this, te); }

    @Override
    public void detectAndSendChanges()
    { super.detectAndSendChanges(); }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    { te.setField(id, data); }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorLabeledCrate) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot)
    { return (slot.getSlotStackLimit() > 1); }

    @Override
    public void onContainerClosed(EntityPlayer player)
    { super.onContainerClosed(player); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
      Slot slot = getSlot(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Crate slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!mergeItemStack(slot_stack, 0, PLAYER_INV_START_SLOTNO-1, false)) return ItemStack.EMPTY;
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
  // ModEngineersDecor.GuiHandler connectors
  //--------------------------------------------------------------------------------------------------------------------

  public static Object getServerGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof LabeledCrateTileEntity) ? (new BContainer(player.inventory, world, pos, (LabeledCrateTileEntity)te)) : null; }

  public static Object getClientGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof LabeledCrateTileEntity) ? (new BGui(player.inventory, world, pos, (LabeledCrateTileEntity)te)) : null; }

}
