/*
 * @file BlockDecorDropper.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Dropper factory automation suitable.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockDoor;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wile.engineersdecor.detail.Networking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class BlockDecorDropper extends BlockDecorDirected
{
  public static final PropertyBool OPEN = BlockDoor.OPEN;

  public BlockDecorDropper(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, OPEN); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return super.getStateFromMeta(meta).withProperty(OPEN, (meta & 0x8)!=0); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return super.getMetaFromState(state) | (state.getValue(OPEN) ? 0x8 : 0x0); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(OPEN, false); }

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
  { return new BlockDecorDropper.BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("inventory"))) return;
    NBTTagCompound inventory_nbt = stack.getTagCompound().getCompoundTag("inventory");
    if(inventory_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BlockDecorDropper.BTileEntity)) return;
    ((BlockDecorDropper.BTileEntity)te).readnbt(inventory_nbt, false);
    ((BlockDecorDropper.BTileEntity)te).markDirty();
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
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_FACTORY_DROPPER, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos neighborPos)
  {
    if(!(world instanceof World) || (((World) world).isRemote)) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).block_updated();
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      BContainer container = (BContainer)inventorySlots;
      if(container.fields_.length != 16) return;
      int mx = mouseX - getGuiLeft(), my = mouseY - getGuiTop();
      if(isPointInRegion(130, 10, 12, 25, mouseX, mouseY)) {
        int force_percent = 100 - MathHelper.clamp(((my-10)*100)/25, 0, 100);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_speed", force_percent);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(145, 10, 25, 25, mouseX, mouseY)) {
        int xdev = MathHelper.clamp(((mx-157) * 100) / 12, -100, 100);
        int ydev = -MathHelper.clamp(((my-22) * 100) / 12, -100, 100);
        if(Math.abs(xdev) < 3) xdev = 0;
        if(Math.abs(ydev) < 3) ydev = 0;
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_xdev", xdev);
        nbt.setInteger("drop_ydev", ydev);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(129, 40, 44, 10, mouseX, mouseY)) {
        int ndrop = (mx-135);
        if(ndrop < -1) {
          ndrop = container.fields_[4] - 1;  // -
        } else if(ndrop >= 36) {
          ndrop = container.fields_[4] + 1; // +
        } else {
          ndrop = MathHelper.clamp(1+ndrop, 1, 32); // slider
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_count", ndrop);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(
        isPointInRegion(114, 51, 9, 9, mouseX, mouseY) ||
        isPointInRegion(162, 66, 7, 9, mouseX, mouseY)
      ) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("manual_trigger", 1);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/factory_dropper_gui.png"));
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
      BContainer container = (BContainer)inventorySlots;
      if(container.fields_.length != 16) return; // no init, no cake.
      // active drop slot
      {
        int drop_slot_index = container.fields_[15];
        if((drop_slot_index < 0) || (drop_slot_index >= 16)) drop_slot_index = 0;
        int x = (x0+9+((drop_slot_index % 6) * 18));
        int y = (y0+5+((drop_slot_index / 6) * 17));
        drawTexturedModalRect(x, y, 180, 45, 18, 18);
      }
      // filter LEDs
      {
        for(int i=0; i<3; ++i) {
          int xt = 180 + (6 * container.fields_[12+i]), yt = 38;
          int x = x0 + 31 + (i * 36), y = y0 + 65;
          drawTexturedModalRect(x, y, xt, yt, 6, 6);
        }
      }
      // force adjustment
      {
        int hy = 2 + (((100-container.fields_[0]) * 21) / 100);
        int x = x0+135, y = y0+12, xt = 181;
        int yt = 4 + (23-hy);
        drawTexturedModalRect(x, y, xt, yt, 3, hy);
      }
      // angle adjustment
      {
        int x = x0 + 157 - 3 + ((container.fields_[1] * 12) / 100);
        int y = y0 +  22 - 3 - ((container.fields_[2] * 12) / 100);
        drawTexturedModalRect(x, y, 180, 30, 7, 7);
      }
      // drop count
      {
        int x = x0 + 134 - 2 + (container.fields_[4]);
        int y = y0 + 44;
        drawTexturedModalRect(x, y, 190, 31, 5, 5);
      }
      // redstone input
      {
        if(container.fields_[11] != 0) {
          drawTexturedModalRect(x0+114, y0+51, 189, 18, 9, 9);
        }
      }
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
    private int fields_[] = new int[16];

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      int i=-1;
      // input slots (stacks 0 to 11)
      for(int y=0; y<2; ++y) {
        for(int x=0; x<6; ++x) {
          int xpos = 10+x*18, ypos = 6+y*17;
          addSlotToContainer(new Slot(te, ++i, xpos, ypos));
        }
      }
      // filter slots (stacks 12 to 14)
      addSlotToContainer(new Slot(te, ++i, 19, 48));
      addSlotToContainer(new Slot(te, ++i, 55, 48));
      addSlotToContainer(new Slot(te, ++i, 91, 48));
      // player slots
      for(int x=0; x<9; ++x) {
        addSlotToContainer(new Slot(playerInventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
    }

    public BlockPos getPos()
    { return pos; }

    @Override
    public void addListener(IContainerListener listener)
    { super.addListener(listener); listener.sendAllWindowProperties(this, te); }

    @Override
    public void detectAndSendChanges()
    {
      super.detectAndSendChanges();
      for(int il=0; il<listeners.size(); ++il) {
        IContainerListener lis = listeners.get(il);
        for(int k=0; k<16; ++k) {
          int f = te.getField(k);
          if(fields_[k] != f) {
            fields_[k] = f;
            lis.sendWindowProperty(this, k, f);
          }
        }
      }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value)
    {
      if((id < 0) || (id >= fields_.length)) return;
      fields_[id] = value;
      te.setField(id, value);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorDropper) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
        // Device slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player slot
        if(!mergeItemStack(slot_stack, 0, BTileEntity.INPUT_SLOTS_SIZE, false)) return ItemStack.EMPTY;
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

  public static class BTileEntity extends TileEntity implements ITickable, ISidedInventory, Networking.IPacketReceiver
  {
    public static final int TICK_INTERVAL = 32;
    public static final int NUM_OF_SLOTS = 15;
    public static final int INPUT_SLOTS_FIRST = 0;
    public static final int INPUT_SLOTS_SIZE = 12;
    public static final int CTRL_SLOTS_FIRST = INPUT_SLOTS_SIZE;
    public static final int CTRL_SLOTS_SIZE = 3;

    private int tick_timer_ = 0;
    private int filter_matches_[] = new int[CTRL_SLOTS_SIZE];
    private boolean active_ = false;
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int drop_speed_ = 10;
    private int drop_noise_ = 0;
    private int drop_xdev_ = 0;
    private int drop_ydev_ = 0;
    private int drop_slot_index_ = 0;
    private int drop_count_ = 0;

    protected NonNullList<ItemStack> stacks_;

    public static void on_config(int cooldown_ticks)
    {
      // ModEngineersDecor.logger.info("Config factory dropper:");
    }

    public BTileEntity()
    { reset(); }

    protected void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      block_power_signal_ = false;
      block_power_updated_ = false;
      drop_count_ = 0;
      for(int i=0; i<filter_matches_.length; ++i) filter_matches_[i] = 0;
    }

    public void readnbt(NBTTagCompound nbt, boolean update_packet)
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      active_ = nbt.getBoolean("active");
      block_power_signal_ = nbt.getBoolean("powered");
      drop_speed_ = nbt.getInteger("drop_speed");
      drop_noise_ = nbt.getInteger("drop_noise");
      drop_xdev_ = nbt.getInteger("drop_xdev");
      drop_ydev_ = nbt.getInteger("drop_ydev");
      drop_slot_index_ = nbt.getInteger("drop_slot_index");
      drop_count_ = nbt.getInteger("drop_count");
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.setBoolean("active", active_);
      nbt.setBoolean("powered", block_power_signal_);
      nbt.setInteger("drop_speed", drop_speed_);
      nbt.setInteger("drop_noise", drop_noise_);
      nbt.setInteger("drop_xdev", drop_xdev_);
      nbt.setInteger("drop_ydev", drop_ydev_);
      nbt.setInteger("drop_slot_index", drop_slot_index_);
      nbt.setInteger("drop_count", drop_count_);
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
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorDropper)); }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    { super.readFromNBT(nbt); readnbt(nbt, false); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    { super.writeToNBT(nbt); writenbt(nbt, false); return nbt; }

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
      tick_timer_ = 2;
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
    { return true; }

    @Override
    public int getField(int id)
    {
      switch(id) {
        case  0: return drop_speed_;
        case  1: return drop_xdev_;
        case  2: return drop_ydev_;
        case  3: return drop_noise_;
        case  4: return drop_count_;
        case 10: return active_ ? 1 : 0;
        case 11: return block_power_signal_ ? 1 : 0;
        case 12: return filter_matches_[0];
        case 13: return filter_matches_[1];
        case 14: return filter_matches_[2];
        case 15: return drop_slot_index_;
        default: return 0;
      }
    }

    @Override
    public void setField(int id, int value)
    {
      switch(id) {
        case 0: drop_speed_ = MathHelper.clamp(value,    0, 100); return;
        case 1: drop_xdev_  = MathHelper.clamp(value, -100, 100); return;
        case 2: drop_ydev_  = MathHelper.clamp(value, -100, 100); return;
        case 3: drop_noise_ = MathHelper.clamp(value,    0, 100); return;
        case 4: drop_count_ = MathHelper.clamp(value,    1,  64); return;
        case 10: active_ = (value != 0); return;
        case 11: block_power_signal_ = (value != 0); return;
        case 12: filter_matches_[0] = (value & 0x3); return;
        case 13: filter_matches_[1] = (value & 0x3); return;
        case 14: filter_matches_[2] = (value & 0x3); return;
        case 15: drop_slot_index_ = MathHelper.clamp(value, INPUT_SLOTS_FIRST, INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE-1); return;
        default: return;
      }
    }

    @Override
    public int getFieldCount()
    {  return 16; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[INPUT_SLOTS_SIZE];
      for(int i=INPUT_SLOTS_FIRST; i<INPUT_SLOTS_SIZE; ++i) SIDED_INV_SLOTS[i] = i;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
    { return is_input_slot(index) && isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return false; }

    // IItemHandler  --------------------------------------------------------------------------------

    protected static class BItemHandler implements IItemHandler
    {
      private BTileEntity te;

      BItemHandler(BTileEntity te)
      { this.te = te; }

      @Override
      public int getSlots()
      { return SIDED_INV_SLOTS.length; }

      @Override
      public int getSlotLimit(int index)
      { return te.getInventoryStackLimit(); }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      { return te.is_input_slot(slot); }

      @Override
      @Nonnull
      public ItemStack insertItem(int index, @Nonnull ItemStack stack, boolean simulate)
      {
        if((stack.isEmpty()) || (!te.is_input_slot(index))) return ItemStack.EMPTY;
        int slotno = 0;
        ItemStack slotstack = getStackInSlot(slotno);
        if(!slotstack.isEmpty())
        {
          if(slotstack.getCount() >= Math.min(slotstack.getMaxStackSize(), getSlotLimit(index))) return stack;
          if(!ItemHandlerHelper.canItemStacksStack(stack, slotstack)) return stack;
          if(!te.canInsertItem(slotno, stack, EnumFacing.UP) || (!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index)) - slotstack.getCount();
          if(stack.getCount() <= n) {
            if(!simulate) {
              ItemStack copy = stack.copy();
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
            }
            return ItemStack.EMPTY;
          } else {
            stack = stack.copy();
            if(!simulate) {
              ItemStack copy = stack.splitStack(n);
              copy.grow(slotstack.getCount());
              te.setInventorySlotContents(slotno, copy);
              return stack;
            } else {
              stack.shrink(n);
              return stack;
            }
          }
        } else {
          if(!te.canInsertItem(slotno, stack, EnumFacing.UP) || (!te.isItemValidForSlot(slotno, stack))) return stack;
          int n = Math.min(stack.getMaxStackSize(), getSlotLimit(index));
          if(n < stack.getCount()) {
            stack = stack.copy();
            if(!simulate) {
              te.setInventorySlotContents(slotno, stack.splitStack(n));
              return stack;
            } else {
              stack.shrink(n);
              return stack;
            }
          } else {
            if(!simulate) te.setInventorySlotContents(slotno, stack);
            return ItemStack.EMPTY;
          }
        }
      }

      @Override
      @Nonnull
      public ItemStack extractItem(int index, int amount, boolean simulate)
      {
        if((amount <= 0) || (!te.is_input_slot(index))) return ItemStack.EMPTY;
        ItemStack stack = te.stacks_.get(index).copy();
        if(stack.getCount() > amount) stack.setCount(amount);
        if(simulate) return stack;
        te.stacks_.get(index).shrink(stack.getCount());
        return stack;
      }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      { return te.getStackInSlot(index); }
    }

    BItemHandler item_handler_ = new BItemHandler(this);

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return (cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || super.hasCapability(cap, facing); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T)item_handler_;
      return super.getCapability(capability, facing);
    }

    // IPacketReceiver -------------------------------------------------------------------------------

    @Override
    public void onServerPacketReceived(NBTTagCompound nbt)
    {}

    @Override
    public void onClientPacketReceived(EntityPlayer player, NBTTagCompound nbt)
    {
      if(nbt.hasKey("drop_speed")) drop_speed_ = MathHelper.clamp(nbt.getInteger("drop_speed"), 0, 100);
      if(nbt.hasKey("drop_xdev"))  drop_xdev_  = MathHelper.clamp(nbt.getInteger("drop_xdev"), -100, 100);
      if(nbt.hasKey("drop_ydev"))  drop_ydev_  = MathHelper.clamp(nbt.getInteger("drop_ydev"), -100, 100);
      if(nbt.hasKey("drop_count")) drop_count_  = MathHelper.clamp(nbt.getInteger("drop_count"), 1, 64);
      if(nbt.hasKey("manual_trigger") && (nbt.getInteger("manual_trigger")!=0)) { block_power_signal_ = true; block_power_updated_ = true; tick_timer_ = 1; }
      markDirty();
    }

    // ITickable and aux methods ---------------------------------------------------------------------

    private static void drop(World world, BlockPos pos, EnumFacing facing, ItemStack stack, int speed_percent, int xdeviation, int ydeviation, int noise_percent)
    {
      final double ofs = facing==EnumFacing.DOWN ? 0.8 : 0.7;
      Vec3d v0 = new Vec3d(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
      final EntityItem ei = new EntityItem(world, (pos.getX()+0.5)+(ofs*v0.x), (pos.getY()+0.5)+(ofs*v0.y), (pos.getZ()+0.5)+(ofs*v0.z), stack);
      if((xdeviation != 0) || (ydeviation != 0)) {
        double vdx = 1e-2 * MathHelper.clamp(xdeviation, -100, 100);
        double vdy = 1e-2 * MathHelper.clamp(ydeviation, -100, 100);
        switch(facing) { // switch-case faster than coorsys fwd transform
          case DOWN:  v0 = v0.add( vdx, 0, vdy); break; // down/up: use xz
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
      ei.motionX = v0.x;
      ei.motionY = v0.y;
      ei.motionZ = v0.z;
      world.spawnEntity(ei);
    }

    @Nullable
    IBlockState update_blockstate()
    {
      IBlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof BlockDecorDropper)) return null;
      if(state.getValue(OPEN) != active_) {
        state = state.withProperty(OPEN, active_);
        world.setBlockState(pos, state);
      }
      return state;
    }

    private static int next_slot(int i)
    { return (i<INPUT_SLOTS_SIZE-1) ? (i+1) : INPUT_SLOTS_FIRST; }

    @Override
    public void update()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      final IBlockState state = update_blockstate();
      if(state == null) { block_power_signal_= false; return; }
      boolean dirty = block_power_updated_;
      boolean trigger = (block_power_signal_ && block_power_updated_);
      int drop_count = MathHelper.clamp(drop_count_, 1, 64);
      boolean slot_assigned = false;
      if(!trigger) {
        int last_filter_matches_[] = filter_matches_.clone();
        for(int ci=0; ci<CTRL_SLOTS_SIZE; ++ci) {
          filter_matches_[ci] = 0;
          final ItemStack cmp_stack = stacks_.get(CTRL_SLOTS_FIRST+ci);
          if(cmp_stack.isEmpty()) continue;
          filter_matches_[ci] = 1;
          final int cmp_stack_count = cmp_stack.getCount();
          int dropslot = drop_slot_index_;
          for(int i=INPUT_SLOTS_FIRST; i<(INPUT_SLOTS_FIRST+INPUT_SLOTS_SIZE); ++i) {
            final ItemStack inp_stack = stacks_.get(dropslot);
            if((inp_stack.getCount() < cmp_stack_count) || (!inp_stack.isItemEqual(cmp_stack))) {
              dropslot = next_slot(dropslot);
              continue;
            }
            trigger = true;
            dirty = true;
            filter_matches_[ci] = 2;
            if(!slot_assigned) {
              slot_assigned = true;
              drop_slot_index_ = dropslot;
            }
            break;
          }
        }
        for(int i=0; i<filter_matches_.length; ++i) {
          if(filter_matches_[i] != last_filter_matches_[i]) { dirty = true; break; }
        }
      }
      if(trigger) {
        boolean tr = world.isBlockPowered(pos); // effect active next cycle
        block_power_updated_ = (block_power_signal_ != tr);
        block_power_signal_ = tr;
        ItemStack drop_stack = ItemStack.EMPTY;
        for(int i=0; i<INPUT_SLOTS_SIZE; ++i) {
          if(drop_slot_index_ >= INPUT_SLOTS_SIZE) drop_slot_index_ = 0;
          int ic = drop_slot_index_;
          drop_slot_index_ = next_slot(drop_slot_index_);
          ItemStack ds = stacks_.get(ic);
          if((!ds.isEmpty()) && (ds.getCount() >= drop_count)) {
            drop_stack = ds.splitStack(drop_count);
            break;
          }
        }
        for(int i=0; i<INPUT_SLOTS_SIZE; ++i) {
          if(!stacks_.get(drop_slot_index_).isEmpty()) break;
          drop_slot_index_ = next_slot(drop_slot_index_);
        }
        if(!drop_stack.isEmpty()) {
          dirty = true;
          // todo: Check inventory insert before ...
          drop(world, pos, state.getValue(FACING), drop_stack, drop_speed_, drop_xdev_, drop_ydev_, drop_noise_);
        }
      }
      if(dirty) markDirty();
      if(trigger && (tick_timer_ > 10)) tick_timer_ = 10;
    }
  }

}
