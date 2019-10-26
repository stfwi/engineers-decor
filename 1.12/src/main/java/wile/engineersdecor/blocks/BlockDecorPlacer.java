/*
 * @file BlockDecorPlacer.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block placer and planter, factory automation suitable.
 */
package wile.engineersdecor.blocks;




import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.Networking;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;


import net.minecraft.world.IBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockFaceShape;
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
import net.minecraft.util.math.*;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;


public class BlockDecorPlacer extends BlockDecorDirected
{
  public BlockDecorPlacer(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
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
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_FACTORY_PLACER, world, pos.getX(), pos.getY(), pos.getZ());
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
      if(container.fields_.length != 3) return;
      int mx = mouseX - getGuiLeft(), my = mouseY - getGuiTop();
      if(!isPointInRegion(126, 1, 49, 60, mouseX, mouseY)) {
        return;
      } else if(isPointInRegion(133, 49, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("manual_trigger", 1);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(145, 49, 9, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("logic", container.fields_[0] ^ BTileEntity.LOGIC_INVERTED);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      } else if(isPointInRegion(159, 49, 7, 9, mouseX, mouseY)) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("logic", container.fields_[0] ^ BTileEntity.LOGIC_CONTINUOUS);
        Networking.PacketTileNotify.sendToServer(te, nbt);
      }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/factory_placer_gui.png"));
      final int x0=getGuiLeft(), y0=getGuiTop(), w=getXSize(), h=getYSize();
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
      BContainer container = (BContainer)inventorySlots;
      if(container.fields_.length != 3) return; // no init, no cake.
      // active slot
      {
        int slot_index = container.fields_[2];
        if((slot_index < 0) || (slot_index >= BTileEntity.NUM_OF_SLOTS)) slot_index = 0;
        int x = (x0+10+((slot_index % 6) * 18));
        int y = (y0+8+((slot_index / 6) * 17));
        drawTexturedModalRect(x, y, 200, 8, 18, 18);
      }
      // redstone input
      {
        if(container.fields_[1] != 0) {
          drawTexturedModalRect(x0+133, y0+49, 217, 49, 9, 9);
        }
      }
      // trigger logic
      {
        int inverter_offset = ((container.fields_[0] & BTileEntity.LOGIC_INVERTED) != 0) ? 11 : 0;
        drawTexturedModalRect(x0+145, y0+49, 177+inverter_offset, 49, 9, 9);
        int pulse_mode_offset  = ((container.fields_[0] & BTileEntity.LOGIC_CONTINUOUS    ) != 0) ? 9 : 0;
        drawTexturedModalRect(x0+159, y0+49, 199+pulse_mode_offset, 49, 9, 9);
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
    private int fields_[] = new int[3];

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
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorPlacer) && (player.getDistanceSq(pos) <= 64); }

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
    public static final int TICK_INTERVAL = 40;
    public static final int NUM_OF_SLOTS = 18;
    ///
    public static final int LOGIC_INVERTED   = 0x01;
    public static final int LOGIC_CONTINUOUS = 0x02;
    ///
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int logic_ = LOGIC_INVERTED|LOGIC_CONTINUOUS;
    private int current_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected NonNullList<ItemStack> stacks_;

    public static void on_config(int cooldown_ticks)
    {
      // ModEngineersDecor.logger.info("Config factory placer:");
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
      logic_ = nbt.getInteger("logic");
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      ItemStackHelper.saveAllItems(nbt, stacks_);
      nbt.setBoolean("powered", block_power_signal_);
      nbt.setInteger("act_slot_index", current_slot_index_);
      nbt.setInteger("logic", logic_);
    }

    public void block_updated()
    {
      boolean powered = world.isBlockPowered(pos);
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
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorPlacer)); }

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
        case 0: return logic_;
        case 1: return block_power_signal_ ? 1 : 0;
        case 2: return current_slot_index_;
        default: return 0;
      }
    }

    @Override
    public void setField(int id, int value)
    {
      switch(id) {
        case 0: logic_ = value; return;
        case 1: block_power_signal_ = (value != 0); return;
        case 2: current_slot_index_ = MathHelper.clamp(value, 0, NUM_OF_SLOTS-1); return;
        default: return;
      }
    }

    @Override
    public int getFieldCount()
    {  return 3; }

    @Override
    public void clear()
    { stacks_.clear(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private final IItemHandler item_handler_ = new SidedInvWrapper(this, EnumFacing.UP);
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
      if(nbt.hasKey("logic")) logic_  = nbt.getInteger("logic");
      if(nbt.hasKey("manual_trigger") && (nbt.getInteger("manual_trigger")!=0)) { block_power_signal_=true; block_power_updated_=true; tick_timer_=1; }
      markDirty();
    }

    // ITickable and aux methods ---------------------------------------------------------------------

    private static int next_slot(int i)
    { return (i<NUM_OF_SLOTS-1) ? (i+1) : 0; }

    private boolean spit_out(EnumFacing facing)
    {
      ItemStack stack = stacks_.get(current_slot_index_);
      ItemStack drop = stack.copy();
      stack.shrink(1);
      stacks_.set(current_slot_index_, stack);
      drop.setCount(1);
      for(int i=0; i<8; ++i) {
        BlockPos p = pos.offset(facing, i);
        if(!world.isAirBlock(p)) continue;
        world.spawnEntity(new EntityItem(world, (p.getX()+0.5), (p.getY()+0.5), (p.getZ()+0.5), drop));
        world.playSound(null, p, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.7f, 0.8f);
        break;
      }
      return true;
    }

    private boolean try_place(EnumFacing facing)
    {
      if(world.isRemote) return false;
      BlockPos placement_pos = pos.offset(facing);
      if(world.getTileEntity(placement_pos) != null) return false;
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
      Block block = (item instanceof IPlantable) ? (((IPlantable)item).getPlant(world, pos).getBlock()) : Block.getBlockFromItem(item);
      if(block == Blocks.AIR) {
        if(item != null) {
          return spit_out(facing); // Item not accepted
        } else {
          // try next slot
        }
      } else if(block instanceof IPlantable) {
        if(world.isAirBlock(placement_pos)) {
          // plant here, block below has to be valid soil.
          IBlockState soilstate = world.getBlockState(placement_pos.down());
          if(!soilstate.getBlock().canSustainPlant(soilstate, world, pos, EnumFacing.UP, (IPlantable)block)) {
            block = Blocks.AIR;
          }
        } else {
          // adjacent block is the soil, plant above if the soil is valid.
          IBlockState soilstate = world.getBlockState(placement_pos);
          if(soilstate.getBlock() == block) {
            // The plant is already planted from the case above.
            block = Blocks.AIR;
            no_space = true;
          } else if(!world.isAirBlock(placement_pos.up())) {
            // If this is the soil an air block is needed above, if that is blocked we can't plant.
            block = Blocks.AIR;
            no_space = true;
          } else if(!soilstate.getBlock().canSustainPlant(soilstate, world, pos, EnumFacing.UP, (IPlantable)block)) {
            // Would be space above, but it's not the right soil for the plant.
            block = Blocks.AIR;
          } else {
            // Ok, plant above.
            placement_pos = placement_pos.up();
          }
        }
      } else if(!world.getBlockState(placement_pos).getBlock().isReplaceable(world, placement_pos)) {
        block = Blocks.AIR;
        no_space = true;
      }
      // System.out.println("PLACE " + current_stack + "  --> " + block + " at " + placement_pos.subtract(pos) + "( item=" + item + ")");
      if(block != Blocks.AIR) {
        try {
          final FakePlayer placer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer)world);
          final IBlockState placement_state = (placer==null) ? (block.getDefaultState()) : (block.getStateForPlacement(world, placement_pos, EnumFacing.DOWN,0.5f,0.5f,0f, 0, placer, EnumHand.MAIN_HAND));
          if(placement_state == null) {
            return spit_out(facing);
          } else if(item instanceof ItemBlock) {
            ItemStack placement_stack = current_stack.copy();
            placement_stack.setCount(1);
            ((ItemBlock)item).placeBlockAt(placement_stack, placer, world, placement_pos, EnumFacing.DOWN, 0.5f,0.5f,0f, placement_state);
            SoundType stype = block.getSoundType(placement_state, world, pos, null);
            if(stype != null) world.playSound(null, placement_pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
          } else {
            if(world.setBlockState(placement_pos, placement_state, 1|2|8)) {
              SoundType stype = block.getSoundType(placement_state, world, pos, null);
              if(stype != null) world.playSound(null, placement_pos, stype.getPlaceSound(), SoundCategory.BLOCKS, stype.getVolume()*0.6f, stype.getPitch());
            }
          }
          current_stack.shrink(1);
          stacks_.set(current_slot_index_, current_stack);
          return true;
        } catch(Throwable e) {
          // The block really needs a player or other issues happened during placement.
          // A hard crash should not be fired here, instead spit out the item to indicated that this
          // block is not compatible.
          System.out.println("Exception while trying to place " + e);
          world.setBlockToAir(placement_pos);
          return spit_out(facing);
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
    public void update()
    {
      // Tick cycle pre-conditions
      if(world.isRemote) return;
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      // Cycle init
      boolean dirty = block_power_updated_;
      boolean rssignal = ((logic_ & LOGIC_INVERTED)!=0)==(!block_power_signal_);
      boolean trigger = (rssignal && ((block_power_updated_) || ((logic_ & LOGIC_CONTINUOUS)!=0)));
      final IBlockState state = world.getBlockState(pos);
      if(state == null) { block_power_signal_= false; return; }
      final EnumFacing placer_facing = state.getValue(FACING);
      // Trigger edge detection for next cycle
      {
        boolean tr = world.isBlockPowered(pos);
        block_power_updated_ = (block_power_signal_ != tr);
        block_power_signal_ = tr;
        if(block_power_updated_) dirty = true;
      }
      // Placing
      if(trigger && try_place(placer_facing)) dirty = true;
      if(dirty) markDirty();
      if(trigger && (tick_timer_ > TICK_INTERVAL)) tick_timer_ = TICK_INTERVAL;
    }
  }
}
