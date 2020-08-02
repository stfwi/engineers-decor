/*
 * @file BlockDecorHopper.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Hopper, factory automation suitable.
 */
package wile.engineersdecor.blocks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.Networking;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockHopper;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class BlockDecorHopper extends BlockDecorDirected
{
  public BlockDecorHopper(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.SOLID; }

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

  @Override
  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("tedata"))) return;
    NBTTagCompound te_nbt = stack.getTagCompound().getCompoundTag("tedata");
    if(te_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).readnbt(te_nbt, false);
    ((BTileEntity)te).reset_rtstate();
    ((BTileEntity)te).markDirty();
  }

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return super.removedByPlayer(state, world, pos, player, willHarvest);
    ItemStack stack = new ItemStack(this, 1);
    NBTTagCompound te_nbt = new NBTTagCompound();
    ((BTileEntity) te).writenbt(te_nbt, false);
    if(!te_nbt.isEmpty()) {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setTag("tedata", te_nbt);
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
    ((BTileEntity)te).reset_rtstate();
    super.onBlockExploded(world, pos, explosion);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_FACTORY_HOPPER, world, pos.getX(), pos.getY(), pos.getZ());
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

  @Override
  public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance)
  {
    super.onFallenUpon(world, pos, entity, fallDistance);
    if(!(entity instanceof EntityItem)) return;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).collection_timer_ = 0;
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
      if(container.fields_.length != 7) return;
      int mx = mouseX - getGuiLeft(), my = mouseY - getGuiTop();
      if(!isPointInRegion(126, 1, 49, 60, mouseX, mouseY)) {
        return;
      } else if(isPointInRegion(128, 9, 44, 10, mouseX, mouseY)) {
        int range = (mx-133);
        if(range < -1) {
          range = container.fields_[0] - 1; // -
        } else if(range >= 34) {
          range = container.fields_[0] + 1; // +
        } else {
          range = (int)(0.5 + ((((double)BTileEntity.MAX_COLLECTION_RANGE) * range)/34)); // slider
          range = MathHelper.clamp(range, 0, BTileEntity.MAX_COLLECTION_RANGE);
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("range", range);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(128, 21, 44, 10, mouseX, mouseY)) {
        int period = (mx-133);
        if(period < -1) {
          period = container.fields_[3] - 3; // -
        } else if(period >= 35) {
          period = container.fields_[3] + 3; // +
        } else {
          period = (int)(0.5 + ((100.0 * period)/34));
        }
        period = MathHelper.clamp(period, 0, 100);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("period", period);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(128, 34, 44, 10, mouseX, mouseY)) {
        int ndrop = (mx-134);
        if(ndrop < -1) {
          ndrop = container.fields_[1] - 1; // -
        } else if(ndrop >= 34) {
          ndrop = container.fields_[1] + 1; // +
        } else {
          ndrop = MathHelper.clamp(1+ndrop, 1, BTileEntity.MAX_TRANSFER_COUNT); // slider
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("xsize", ndrop);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(133, 49, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("manual_trigger", 1);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(145, 49, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("logic", container.fields_[2] ^ BTileEntity.LOGIC_INVERTED);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(159, 49, 7, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("logic", container.fields_[2] ^ BTileEntity.LOGIC_CONTINUOUS);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/factory_hopper_gui.png"));
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
      BContainer container = (BContainer)inventorySlots;
      if(container.fields_.length != 7) return; // no init, no cake.
      // active slot
      {
        int slot_index = container.fields_[6];
        if((slot_index < 0) || (slot_index >= BTileEntity.NUM_OF_SLOTS)) slot_index = 0;
        int x = (x0+10+((slot_index % 6) * 18));
        int y = (y0+8+((slot_index / 6) * 17));
        drawTexturedModalRect(x, y, 200, 8, 18, 18);
      }
      // collection range
      {
        int lut[] = { 133, 141, 149, 157, 166 };
        int px = lut[MathHelper.clamp(container.fields_[0], 0, BTileEntity.MAX_COLLECTION_RANGE)];
        int x = x0 + px - 2;
        int y = y0 + 14;
        drawTexturedModalRect(x, y, 179, 40, 5, 5);
      }
      // transfer period
      {
        int px = (int)Math.round(((33.5 * container.fields_[3]) / 100) + 1);
        int x = x0 + 132 - 2 + MathHelper.clamp(px, 0, 34);
        int y = y0 + 27;
        drawTexturedModalRect(x, y, 179, 40, 5, 5);
      }
      // transfer count
      {
        int x = x0 + 133 - 2 + (container.fields_[1]);
        int y = y0 + 40;
        drawTexturedModalRect(x, y, 179, 40, 5, 5);
      }
      // redstone input
      {
        if(container.fields_[5] != 0) {
          drawTexturedModalRect(x0+133, y0+49, 217, 49, 9, 9);
        }
      }
      // trigger logic
      {
        int inverter_offset = ((container.fields_[2] & BTileEntity.LOGIC_INVERTED) != 0) ? 11 : 0;
        drawTexturedModalRect(x0+145, y0+49, 177+inverter_offset, 49, 9, 9);
        int pulse_mode_offset  = ((container.fields_[2] & BTileEntity.LOGIC_CONTINUOUS    ) != 0) ? 9 : 0;
        drawTexturedModalRect(x0+159, y0+49, 199+pulse_mode_offset, 49, 9, 9);
      }
      // delay timer running indicator
      {
        if((container.fields_[4] > BTileEntity.PERIOD_OFFSET) && ((System.currentTimeMillis() % 1000) < 500)) {
          drawTexturedModalRect(x0+148, y0+22, 187, 22, 3, 3);
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
    private int fields_[] = new int[7];

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      int i=-1;
      // device slots (stacks 0 to 17)
      for(int y=0; y<3; ++y) {
        for(int x=0; x<6; ++x) {
          int xpos = 11+x*18, ypos = 9+y*17;
          addSlotToContainer(new Slot(te, ++i, xpos, ypos));
        }
      }
      // player slots
      for(int x=0; x<9; ++x) {
        addSlotToContainer(new Slot(playerInventory, x, 8+x*18, 129)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 8+x*18, 71+y*18)); // player slots: 9..35
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
        for(int k=0; k<fields_.length; ++k) {
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
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorHopper) && (player.getDistanceSq(pos) <= 64); }

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
        if(!mergeItemStack(slot_stack, 0, BTileEntity.NUM_OF_SLOTS, false)) return ItemStack.EMPTY;
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
    public static final int TICK_INTERVAL = 10;
    public static final int COLLECTION_INTERVAL = 25;
    public static final int NUM_OF_SLOTS = 18;
    public static final int MAX_TRANSFER_COUNT = 32;
    public static final int MAX_COLLECTION_RANGE = 4;
    public static final int PERIOD_OFFSET = 10;
    ///
    public static final int LOGIC_INVERTED   = 0x01;
    public static final int LOGIC_CONTINUOUS = 0x02;
    ///
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int collection_timer_ = 0;
    private int delay_timer_ = 0;
    private int transfer_count_ = 1;
    private int logic_ = LOGIC_INVERTED|LOGIC_CONTINUOUS;
    private int transfer_period_ = 0;
    private int collection_range_ = 0;
    private int current_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected NonNullList<ItemStack> stacks_;

    public static void on_config(int cooldown_ticks)
    {
      // ModEngineersDecor.logger.info("Config factory hopper:");
    }

    public BTileEntity()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      reset_rtstate();
    }

    public void reset_rtstate()
    {
      block_power_signal_ = false;
      block_power_updated_ = false;
    }

    public void readnbt(NBTTagCompound nbt, boolean update_packet)
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      block_power_signal_ = nbt.getBoolean("powered");
      current_slot_index_ = nbt.getInteger("act_slot_index");
      transfer_count_ = MathHelper.clamp(nbt.getInteger("xsize"), 1, MAX_TRANSFER_COUNT);
      logic_ = nbt.getInteger("logic");
      transfer_period_ = nbt.getInteger("period");
      collection_range_ = nbt.getInteger("range");
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.setBoolean("powered", block_power_signal_);
      nbt.setInteger("act_slot_index", current_slot_index_);
      nbt.setInteger("xsize", transfer_count_);
      nbt.setInteger("logic", logic_);
      nbt.setInteger("period", transfer_period_);
      nbt.setInteger("range", collection_range_);
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
    { return (index >= 0) && (index < NUM_OF_SLOTS); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorHopper)); }

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
        case 0: return collection_range_;
        case 1: return transfer_count_;
        case 2: return logic_;
        case 3: return transfer_period_;
        case 4: return delay_timer_;
        case 5: return block_power_signal_ ? 1 : 0;
        case 6: return current_slot_index_;
        default: return 0;
      }
    }

    @Override
    public void setField(int id, int value)
    {
      switch(id) {
        case 0: collection_range_ = MathHelper.clamp(value,0,  MAX_COLLECTION_RANGE); return;
        case 1: transfer_count_ = MathHelper.clamp(value,1,  MAX_TRANSFER_COUNT); return;
        case 2: logic_ = value; return;
        case 3: transfer_period_ = MathHelper.clamp(value,0,  100); return;
        case 4: delay_timer_ = MathHelper.clamp(value,0,  400); return;
        case 5: block_power_signal_ = (value != 0); return;
        case 6: current_slot_index_ = MathHelper.clamp(value, 0, NUM_OF_SLOTS-1); return;
        default: return;
      }
    }

    @Override
    public int getFieldCount()
    {  return 7; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private final IItemHandler item_handler_ = new SidedInvWrapper(this, EnumFacing.UP);
    private final IItemHandler down_item_handler_ = new SidedInvWrapper(this, EnumFacing.DOWN);
    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[NUM_OF_SLOTS];
      for(int i=0; i<NUM_OF_SLOTS; ++i) SIDED_INV_SLOTS[i] = i;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction)
    { return is_input_slot(index) && isItemValidForSlot(index, stack); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return direction==EnumFacing.DOWN; }

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return (cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) || super.hasCapability(cap, facing); }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
        return (facing == EnumFacing.DOWN) ? ((T)down_item_handler_) : ((T)item_handler_);
      }
      return super.getCapability(capability, facing);
    }

    // IPacketReceiver -------------------------------------------------------------------------------

    @Override
    public void onServerPacketReceived(NBTTagCompound nbt)
    {}

    @Override
    public void onClientPacketReceived(EntityPlayer player, NBTTagCompound nbt)
    {
      if(nbt.hasKey("xsize")) transfer_count_  = MathHelper.clamp(nbt.getInteger("xsize"), 1, MAX_TRANSFER_COUNT);
      if(nbt.hasKey("period")) transfer_period_ = MathHelper.clamp(nbt.getInteger("period"),   0,  100);
      if(nbt.hasKey("range")) collection_range_ = MathHelper.clamp(nbt.getInteger("range"),   0,  MAX_COLLECTION_RANGE);
      if(nbt.hasKey("logic")) logic_  = nbt.getInteger("logic");
      if(nbt.hasKey("manual_trigger") && (nbt.getInteger("manual_trigger")!=0)) { block_power_signal_=true; block_power_updated_=true; tick_timer_=1; }
      markDirty();
    }

    // ITickable and aux methods ---------------------------------------------------------------------

    private static int next_slot(int i)
    { return (i<NUM_OF_SLOTS-1) ? (i+1) : 0; }

    private int try_insert_into_hopper(final ItemStack stack)
    {
      final int max_to_insert = stack.getCount();
      int n_to_insert = max_to_insert;
      int first_empty_slot = -1;
      for(int i=0; i<stacks_.size(); ++i) {
        final ItemStack slotstack = stacks_.get(i);
        if((first_empty_slot < 0) && slotstack.isEmpty()) { first_empty_slot=i; continue; }
        if(!stack.isItemEqual(slotstack)) continue;
        int nspace = slotstack.getMaxStackSize() - slotstack.getCount();
        if(nspace <= 0) {
          continue;
        } else if(nspace >= n_to_insert) {
          slotstack.grow(n_to_insert);
          n_to_insert = 0;
          break;
        } else {
          slotstack.grow(nspace);
          n_to_insert -= nspace;
        }
      }
      if((n_to_insert > 0) && (first_empty_slot >= 0)) {
        ItemStack new_stack = stack.copy();
        new_stack.setCount(n_to_insert);
        stacks_.set(first_empty_slot, new_stack);
        n_to_insert = 0;
      }
      return max_to_insert - n_to_insert;
    }

    private boolean try_insert(EnumFacing facing)
    {
      ItemStack current_stack = ItemStack.EMPTY;
      for(int i=0; i<NUM_OF_SLOTS; ++i) {
        if(current_slot_index_ >= NUM_OF_SLOTS) current_slot_index_ = 0;
        current_stack = stacks_.get(current_slot_index_);
        if(!current_stack.isEmpty()) break;
        current_slot_index_ = next_slot(current_slot_index_);
      }
      if(current_stack.isEmpty()) {
        current_slot_index_ = 0;
        return false;
      }
      final TileEntity te = world.getTileEntity(pos.offset(facing));
      if((te == null) || (!te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()))) {
        delay_timer_ = TICK_INTERVAL+2; // no reason to recalculate this all the time if there is nothere to insert.
        return false;
      } else if(te instanceof TileEntityHopper) {
        EnumFacing f = world.getBlockState(pos.offset(facing)).getValue(BlockHopper.FACING);
        if(f==facing.getOpposite()) return false; // no back transfer
      } else if(te instanceof BTileEntity) {
        EnumFacing f = world.getBlockState(pos.offset(facing)).getValue(FACING);
        if(f==facing.getOpposite()) return false;
      }
      ItemStack insert_stack = current_stack.copy();
      if(insert_stack.getCount() > transfer_count_) insert_stack.setCount(transfer_count_);
      final int initial_insert_stack_size = insert_stack.getCount();
      final IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
      int first_empty_slot_index = -1;
      if((ih == null) || ih.getSlots() <= 0) return false;
      for(int i=0; i<ih.getSlots(); ++i) {
        if(!ih.isItemValid(i, insert_stack)) continue;
        final ItemStack target_stack = ih.getStackInSlot(i);
        if((first_empty_slot_index < 0) && target_stack.isEmpty()) first_empty_slot_index = i;
        if(!target_stack.isItemEqual(insert_stack)) continue;
        insert_stack = ih.insertItem(i, insert_stack.copy(), false);
        if(insert_stack.isEmpty()) break;
      }
      if((first_empty_slot_index >= 0) && (!insert_stack.isEmpty())) {
        insert_stack = ih.insertItem(first_empty_slot_index, insert_stack.copy(), false);
      }
      final int num_inserted = initial_insert_stack_size-insert_stack.getCount();
      if(num_inserted > 0) {
        current_stack.shrink(num_inserted);
        stacks_.set(current_slot_index_, current_stack);
      }
      if(!insert_stack.isEmpty()) current_slot_index_ = next_slot(current_slot_index_);
      return (num_inserted > 0);
    }

    private boolean try_item_handler_extract(final IItemHandler ih)
    {
      final int end = ih.getSlots();
      int n_to_extract = transfer_count_;
      for(int i=0; i<end; ++i) {
        if(ih.getStackInSlot(i).isEmpty()) continue;
        ItemStack stack = ih.extractItem(i, n_to_extract, true);
        if(stack.isEmpty()) continue;
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted > 0) {
          ItemStack test = ih.extractItem(i, n_accepted, false);
          n_to_extract -= n_accepted;
          if(n_to_extract <= 0) break;
        }
      }
      return (n_to_extract < transfer_count_);
    }

    private boolean try_inventory_extract(final IInventory inv)
    {
      final int end = inv.getSizeInventory();
      int n_to_extract = transfer_count_;
      for(int i=0; i<end; ++i) {
        ItemStack stack = inv.getStackInSlot(i).copy();
        if(stack.isEmpty()) continue;
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted > 0) {
          stack.shrink(n_accepted);
          n_to_extract -= n_accepted;
          if(stack.isEmpty()) stack = ItemStack.EMPTY;
          inv.setInventorySlotContents(i, stack);
          if(n_to_extract <= 0) break;
        }
      }
      if(n_to_extract < transfer_count_) {
        inv.markDirty();
        return true;
      } else {
        return false;
      }
    }

    private boolean try_collect(EnumFacing facing)
    {
      AxisAlignedBB collection_volume;
      BlockPos rpos;
      if(facing==EnumFacing.UP)  {
        rpos = pos.add(0.5, 1.5,0.5);
        collection_volume = (new AxisAlignedBB(pos.up())).grow(0.1+collection_range_, 0.6, 0.1+collection_range_);
      } else {
        rpos = pos.add(0.5, -1.5,0.5);
        collection_volume = (new AxisAlignedBB(pos.down(2))).grow(0.1+collection_range_, 1, 0.1+collection_range_);
      }
      final List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, collection_volume);
      if(items.size() <= 0) return false;
      final int max_to_collect = 3;
      int n_collected = 0;
      for(EntityItem ie:items) {
        boolean is_direct_collection_tange = ie.getDistanceSq(rpos)<0.7;
        if(!is_direct_collection_tange && (ie.cannotPickup() || (!ie.onGround))) continue;
        ItemStack stack = ie.getItem();
        int n_accepted = try_insert_into_hopper(stack);
        if(n_accepted <= 0) continue;
        if(n_accepted == stack.getCount()) {
          ie.setDead();
        } else {
          stack.shrink(n_accepted);
        }
        if((!is_direct_collection_tange) && (++n_collected >= max_to_collect)) break;
      }
      return (n_collected > 0);
    }

    @Override
    public void update()
    {
      // Tick cycle pre-conditions
      if(world.isRemote) return;
      if((delay_timer_ > 0) && ((--delay_timer_) == 0)) markDirty();
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final IBlockState state = world.getBlockState(pos);
      if(!(state.getBlock() instanceof BlockDecorHopper)) { block_power_signal_= false; return; }
      // Cycle init
      boolean dirty = block_power_updated_;
      boolean rssignal = ((logic_ & LOGIC_INVERTED)!=0)==(!block_power_signal_);
      boolean trigger = (rssignal && ((block_power_updated_) || ((logic_ & LOGIC_CONTINUOUS)!=0)));
      final EnumFacing hopper_facing = state.getValue(FACING);
      // Trigger edge detection for next cycle
      {
        boolean tr = world.isBlockPowered(pos);
        block_power_updated_ = (block_power_signal_ != tr);
        block_power_signal_ = tr;
        if(block_power_updated_) dirty = true;
      }
      // Collection
      if(rssignal) {
        EnumFacing hopper_input_facing = (hopper_facing==EnumFacing.UP) ? EnumFacing.DOWN : EnumFacing.UP;
        TileEntity te = world.getTileEntity(pos.offset(hopper_input_facing));
        boolean has_item_handler = ((te!=null) && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, hopper_input_facing.getOpposite()));
        if(has_item_handler || (te instanceof ISidedInventory)) {
          // IItemHandler pulling
          if(has_item_handler) {
            final IItemHandler ih = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, hopper_facing.getOpposite());
            if((ih != null) && try_item_handler_extract(ih)) dirty = true;
          } else {
            try_inventory_extract((IInventory)te);
          }
        } else if((collection_timer_ -= TICK_INTERVAL) <= 0) {
          // Ranged collection
          collection_timer_ = COLLECTION_INTERVAL;
          if(try_collect(hopper_input_facing)) dirty = true;
        }
      }
      // Insertion
      if(trigger && (delay_timer_ <= 0)) {
        delay_timer_ = PERIOD_OFFSET + transfer_period_ * 2;
        if(try_insert(hopper_facing)) dirty = true;
      }
      if(dirty) markDirty();
      if(trigger && (tick_timer_ > TICK_INTERVAL)) tick_timer_ = TICK_INTERVAL;
    }
  }
}
