/*
 * @file BlockDecorDropper.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Dropper, factory automation suitable.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.Networking;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
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
import net.minecraft.init.SoundEvents;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class BlockDecorDropper extends BlockDecorDirected
{
  public static final PropertyBool OPEN = BlockDoor.OPEN;

  public BlockDecorDropper(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.SOLID; }

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

  @Override
  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorDropper.BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("tedata"))) return;
    NBTTagCompound te_nbt = stack.getTagCompound().getCompoundTag("tedata");
    if(te_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BlockDecorDropper.BTileEntity)) return;
    ((BlockDecorDropper.BTileEntity)te).readnbt(te_nbt, false);
    ((BlockDecorDropper.BTileEntity)te).reset_rtstate();
    ((BlockDecorDropper.BTileEntity)te).markDirty();
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
      if(!isPointInRegion(114, 1, 61, 79, mouseX, mouseY)) {
        return;
      } else if(isPointInRegion(130, 10, 12, 25, mouseX, mouseY)) {
        int force_percent = 100 - MathHelper.clamp(((my-10)*100)/25, 0, 100);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_speed", force_percent);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(145, 10, 25, 25, mouseX, mouseY)) {
        int xdev = MathHelper.clamp( (int)Math.round(((double)((mx-157) * 100)) / 12), -100, 100);
        int ydev = MathHelper.clamp(-(int)Math.round(((double)((my- 22) * 100)) / 12), -100, 100);
        if(Math.abs(xdev) < 9) xdev = 0;
        if(Math.abs(ydev) < 9) ydev = 0;
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_xdev", xdev);
        nbt.setInteger("drop_ydev", ydev);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(129, 40, 44, 10, mouseX, mouseY)) {
        int ndrop = (mx-135);
        if(ndrop < -1) {
          ndrop = container.fields_[4] - 1; // -
        } else if(ndrop >= 34) {
          ndrop = container.fields_[4] + 1; // +
        } else {
          ndrop = MathHelper.clamp(1+ndrop, 1, BTileEntity.MAX_DROP_COUNT); // slider
        }
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_count", ndrop);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(129, 50, 44, 10, mouseX, mouseY)) {
        int period = (mx-135);
        if(period < -1) {
          period = container.fields_[6] - 3; // -
        } else if(period >= 34) {
          period = container.fields_[6] + 3; // +
        } else {
          period = (int)(0.5 + ((100.0 * period)/34));
        }
        period = MathHelper.clamp(period, 0, 100);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_period", period);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(114, 51, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("manual_rstrigger", 1);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(162, 66, 7, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_logic", container.fields_[5] ^ BTileEntity.DROPLOGIC_CONTINUOUS);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(132, 66, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_logic", container.fields_[5] ^ BTileEntity.DROPLOGIC_FILTER_ANDGATE);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(148, 66, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("drop_logic", container.fields_[5] ^ BTileEntity.DROPLOGIC_EXTERN_ANDGATE);
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
        int y = y0 + 45;
        drawTexturedModalRect(x, y, 190, 31, 5, 5);
      }
      // drop period
      {
        int px = (int)Math.round(((33.0 * container.fields_[6]) / 100) + 1);
        int x = x0 + 134 - 2 + MathHelper.clamp(px, 0, 33);
        int y = y0 + 56;
        drawTexturedModalRect(x, y, 190, 31, 5, 5);
      }
      // redstone input
      {
        if(container.fields_[11] != 0) {
          drawTexturedModalRect(x0+114, y0+51, 189, 18, 9, 9);
        }
      }
      // trigger logic
      {
        int filter_gate_offset = ((container.fields_[5] & BTileEntity.DROPLOGIC_FILTER_ANDGATE) != 0) ? 11 : 0;
        int extern_gate_offset = ((container.fields_[5] & BTileEntity.DROPLOGIC_EXTERN_ANDGATE) != 0) ? 11 : 0;
        int pulse_mode_offset  = ((container.fields_[5] & BTileEntity.DROPLOGIC_CONTINUOUS    ) != 0) ? 10 : 0;
        drawTexturedModalRect(x0+132, y0+66, 179+filter_gate_offset, 66, 9, 9);
        drawTexturedModalRect(x0+148, y0+66, 179+extern_gate_offset, 66, 9, 9);
        drawTexturedModalRect(x0+162, y0+66, 200+pulse_mode_offset, 66, 9, 9);
      }
      // drop timer running indicator
      {
        if((container.fields_[9] > BTileEntity.DROP_PERIOD_OFFSET) && ((System.currentTimeMillis() % 1000) < 500)) {
          drawTexturedModalRect(x0+149, y0+51, 201, 39, 3, 3);
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

    public BTileEntity()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      reset_rtstate();
    }

    public void reset_rtstate()
    {
      block_power_signal_ = false;
      block_power_updated_ = false;
      for(int i=0; i<filter_matches_.length; ++i) filter_matches_[i] = 0;
    }

    public void readnbt(NBTTagCompound nbt, boolean update_packet)
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbt, stacks_);
      while(stacks_.size() < NUM_OF_SLOTS) stacks_.add(ItemStack.EMPTY);
      block_power_signal_ = nbt.getBoolean("powered");
      open_timer_ = nbt.getInteger("open_timer");
      drop_speed_ = nbt.getInteger("drop_speed");
      drop_noise_ = nbt.getInteger("drop_noise");
      drop_xdev_ = nbt.getInteger("drop_xdev");
      drop_ydev_ = nbt.getInteger("drop_ydev");
      drop_slot_index_ = nbt.getInteger("drop_slot_index");
      drop_count_ = MathHelper.clamp(nbt.getInteger("drop_count"), 1, MAX_DROP_COUNT);
      drop_logic_ = nbt.getInteger("drop_logic");
      drop_period_ = nbt.getInteger("drop_period");
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.setBoolean("powered", block_power_signal_);
      nbt.setInteger("open_timer", open_timer_);
      nbt.setInteger("drop_speed", drop_speed_);
      nbt.setInteger("drop_noise", drop_noise_);
      nbt.setInteger("drop_xdev", drop_xdev_);
      nbt.setInteger("drop_ydev", drop_ydev_);
      nbt.setInteger("drop_slot_index", drop_slot_index_);
      nbt.setInteger("drop_count", drop_count_);
      nbt.setInteger("drop_logic", drop_logic_);
      nbt.setInteger("drop_period", drop_period_);
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
    public void setField(int id, int value)
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

    @Override
    public int getFieldCount()
    {  return 16; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private final IItemHandler item_handler_ = new SidedInvWrapper(this, EnumFacing.UP);
    private static final int[] SIDED_INV_SLOTS;
    static {
      SIDED_INV_SLOTS = new int[INPUT_SLOTS_SIZE];
      for(int i=0; i<INPUT_SLOTS_SIZE; ++i) SIDED_INV_SLOTS[i] = i+INPUT_SLOTS_FIRST;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction)
    { return is_input_slot(index) && isItemValidForSlot(index, stack); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return false; }

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
      if(nbt.hasKey("drop_count")) drop_count_  = MathHelper.clamp(nbt.getInteger("drop_count"), 1, MAX_DROP_COUNT);
      if(nbt.hasKey("drop_period")) drop_period_ = MathHelper.clamp(nbt.getInteger("drop_period"),   0,  100);
      if(nbt.hasKey("drop_logic")) drop_logic_  = nbt.getInteger("drop_logic");
      if(nbt.hasKey("manual_rstrigger") && (nbt.getInteger("manual_rstrigger")!=0)) { block_power_signal_=true; block_power_updated_=true; tick_timer_=1; }
      if(nbt.hasKey("manual_trigger") && (nbt.getInteger("manual_trigger")!=0)) { tick_timer_ = 1; triggered_ = true; }
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
      boolean open = (open_timer_ > 0);
      if(state.getValue(OPEN) != open) {
        state = state.withProperty(OPEN, open);
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
    public void update()
    {
      if(world.isRemote) return;
      if(--open_timer_ < 0) open_timer_ = 0;
      if((drop_timer_ > 0) && ((--drop_timer_) == 0)) markDirty();
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      boolean dirty = block_power_updated_;
      final boolean continuous_mode = (drop_logic_ & DROPLOGIC_CONTINUOUS)!=0;
      boolean redstone_trigger = (block_power_signal_ && ((block_power_updated_) || (continuous_mode)));
      boolean filter_trigger;
      boolean filter_defined = false;
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
              if(!inp_stack.isItemEqual(cmp_stack)) { slot = next_slot(slot); continue; }
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
      final IBlockState state = update_blockstate();
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
              drop_stacks[0] = ds.splitStack(drop_count_);
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
                if(!stack.isItemEqual(drop_stacks[fi])) continue;
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
          drop(world, pos, state.getValue(FACING), drop_stacks[i], drop_speed_, drop_xdev_, drop_ydev_, drop_noise_);
          dropped = true;
        }
        // cooldown
        if(dropped) drop_timer_ = DROP_PERIOD_OFFSET + drop_period_ * 2; // 0.1s time base -> 100%===10s
        // drop sound
        if(dropped && ((drop_logic_ & DROPLOGIC_SILENT_DROP) == 0)) {
          world.playSound(null, pos, SoundEvents.BLOCK_CLOTH_STEP, SoundCategory.BLOCKS, 0.1f, 4f);
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
}
