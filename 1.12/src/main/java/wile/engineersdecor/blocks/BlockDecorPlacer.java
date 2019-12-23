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
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.Entity;
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
import java.util.HashMap;
import java.util.Map.Entry;


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
        addSlotToContainer(new Slot(playerInventory, x, 9+x*18, 129)); // player slots: 0..8
      }
      for(int y=0; y<3; ++y) {
        for(int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 9+x*18, 71+y*18)); // player slots: 9..35
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
    @FunctionalInterface private interface SpecialPlacementFunction{ EnumActionResult apply(ItemStack stack, World world, BlockPos pos);}
    public static final int TICK_INTERVAL = 40;
    public static final int NUM_OF_SLOTS = 18;
    ///
    public static final int LOGIC_INVERTED   = 0x01;
    public static final int LOGIC_CONTINUOUS = 0x02;
    public static final int DEFAULT_LOGIC    = LOGIC_INVERTED|LOGIC_CONTINUOUS;
    public static HashMap<ItemStack, SpecialPlacementFunction> special_placement_conversions = new HashMap<>();
    ///
    private boolean block_power_signal_ = false;
    private boolean block_power_updated_ = false;
    private int logic_ = DEFAULT_LOGIC;
    private int current_slot_index_ = 0;
    private int tick_timer_ = 0;
    protected NonNullList<ItemStack> stacks_;

    public static void on_config()
    {
      special_placement_conversions.put(new ItemStack(Items.DYE, 1, 3), (stack,world,pos)->{ // cocoa
        if(world.getBlockState(pos).getBlock() instanceof BlockCocoa) return EnumActionResult.PASS;
        if(!Blocks.COCOA.canPlaceBlockAt(world, pos)) return EnumActionResult.FAIL;
        for(EnumFacing facing:EnumFacing.HORIZONTALS) {
          IBlockState st = world.getBlockState(pos.offset(facing));
          if(!(st.getBlock() instanceof BlockLog)) continue;
          if(st.getBlock().getMetaFromState(st) != 3) continue;
          IBlockState state = Blocks.COCOA.getDefaultState().withProperty(BlockCocoa.FACING, facing);
          return world.setBlockState(pos, state, 1|2) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
        }
        return EnumActionResult.FAIL;
      });
      ModEngineersDecor.logger.info("Config placer: " + special_placement_conversions.size() + " special placement handling entries.");
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
      logic_ = nbt.hasKey("logic") ? nbt.getInteger("logic") : DEFAULT_LOGIC;
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      boolean stacks_not_empty = stacks_.stream().anyMatch(s->!s.isEmpty());
      if(stacks_not_empty) ItemStackHelper.saveAllItems(nbt, stacks_);
      if(block_power_signal_) nbt.setBoolean("powered", block_power_signal_);
      if(stacks_not_empty) nbt.setInteger("act_slot_index", current_slot_index_);
      if(logic_ != DEFAULT_LOGIC) nbt.setInteger("logic", logic_);
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
      ItemStack drop = stacks_.get(current_slot_index_).copy();
      stacks_.set(current_slot_index_, ItemStack.EMPTY);
      for(int i=0; i<8; ++i) {
        BlockPos p = pos.offset(facing, i);
        if(!world.isAirBlock(p)) continue;
        world.spawnEntity(new EntityItem(world, (p.getX()+0.5), (p.getY()+0.5), (p.getZ()+0.5), drop));
        world.playSound(null, p, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.7f, 0.8f);
        break;
      }
      return true;
    }

    private static boolean place_item(ItemStack stack, EntityPlayer placer, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
      final Item place_item = stack.getItem();
      Block place_block = (place_item instanceof IPlantable) ? (((IPlantable)place_item).getPlant(world, pos)).getBlock() : Block.getBlockFromItem(place_item);
      if(((place_block==Blocks.AIR) || (place_block==null)) && ((place_item instanceof ItemBlockSpecial) && (((ItemBlockSpecial)place_item).getBlock()!=null))) place_block = ((ItemBlockSpecial)place_item).getBlock(); // Covers e.g. REEDS
      if((place_block==null) || (place_block==Blocks.AIR)) return false;
      Block block = world.getBlockState(pos).getBlock();
      if(!block.isReplaceable(world, pos)) pos = pos.offset(facing);
      if(!world.mayPlace(place_block, pos, true, facing, (Entity)null)) return false;
      if(place_item instanceof ItemBlock) {
        ItemBlock item = (ItemBlock)place_item;
        int meta = item.getMetadata(stack.getMetadata());
        final IBlockState state = item.getBlock().getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        if(!item.placeBlockAt(stack, placer, world, pos, facing, hitX, hitY, hitZ, state)) return false;
      } else if(place_item instanceof IPlantable) {
        IPlantable item = (IPlantable)place_item;
        final IBlockState state = item.getPlant(world, pos);
        if(!world.setBlockState(pos, state, 1|2)) return false;
      } else {
        final IBlockState state = place_block.getDefaultState();
        if(!world.setBlockState(pos, state, 1|2)) return false;
      }
      final IBlockState soundstate = world.getBlockState(pos);
      final SoundType stype = soundstate.getBlock().getSoundType(soundstate, world, pos, placer);
      world.playSound(placer, pos, stype.getPlaceSound(), SoundCategory.BLOCKS, (stype.getVolume()+1f)/8, stype.getPitch()*1.1f);
      return true;
    }

    private boolean try_plant(BlockPos pos, final EnumFacing facing, final ItemStack stack, final Block plant_block)
    {
      final Item item = stack.getItem();
      if((!(item instanceof ItemBlock)) && (!(item instanceof IPlantable)) && (!(plant_block instanceof IPlantable))) return spit_out(facing);
      Block block = (plant_block instanceof IPlantable) ? plant_block : ((item instanceof IPlantable) ? (((IPlantable)item).getPlant(world, pos)).getBlock() : Block.getBlockFromItem(item));
      if(item instanceof IPlantable) {
        IBlockState st = ((IPlantable)item).getPlant(world, pos); // prefer block from getPlant
        if(st!=null) block = st.getBlock();
      }
      if(world.isAirBlock(pos)) {
        // plant here, block below has to be valid soil.
        final IBlockState soilstate = world.getBlockState(pos.down());
        if((block instanceof IPlantable) && (!soilstate.getBlock().canSustainPlant(soilstate, world, pos.down(), EnumFacing.UP, (IPlantable)block))) {
          // Not the right soil for this plant.
          return false;
        }
      } else {
        // adjacent block is the soil, plant above if the soil is valid.
        final IBlockState soilstate = world.getBlockState(pos);
        if(soilstate.getBlock() == block) {
          // The plant is already planted from the case above, it's not the assumed soil but the planted plant.
          return false;
        } else if(!world.isAirBlock(pos.up())) {
          // If this is the soil an air block is needed above, if that is blocked we can't plant.
          return false;
        } else if((block instanceof IPlantable) && (!soilstate.getBlock().canSustainPlant(soilstate, world, pos, EnumFacing.UP, (IPlantable)block))) {
          // Would be space above, but it's not the right soil for the plant.
          return false;
        } else {
          // Ok, plant above.
          pos = pos.up();
        }
      }
      try {
        //println("PLANT " + stack + "  --> " + block + " at " + pos.subtract(getPos()) + "( item=" + item + ")");
        final FakePlayer placer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer)world);
        if((placer==null) || (!place_item(stack, placer, world, pos, EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.5f, 0f, 0.5f))) return spit_out(facing);
        stack.shrink(1);
        stacks_.set(current_slot_index_, stack);
        return true;
      } catch(Throwable e) {
        ModEngineersDecor.logger.error("Exception while trying to plant " + e);
        world.setBlockToAir(pos);
        return spit_out(facing);
      }
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
      final Item item = current_stack.getItem();
      Block block = Block.getBlockFromItem(item);
      if(((block==Blocks.AIR) || (block==null)) && ((item instanceof ItemBlockSpecial) && (((ItemBlockSpecial)item).getBlock()!=null))) block = ((ItemBlockSpecial)item).getBlock(); // e.g. reeds
      if(item == null) return false;
      if((item instanceof IPlantable) || (block instanceof IPlantable)) return try_plant(placement_pos, facing, current_stack, block);
      if(block == Blocks.AIR) {
        // Check special stuff that is not detected otherwise (like coco, which is technically dye)
        try {
          for(Entry<ItemStack,SpecialPlacementFunction> e:special_placement_conversions.entrySet()) {
            if(e.getKey().isItemEqual(current_stack)) {
              ItemStack placement_stack = current_stack.copy();
              placement_stack.setCount(1);
              switch(e.getValue().apply(current_stack, world, placement_pos)) {
                case PASS:
                  return false;
                case SUCCESS:
                  current_stack.shrink(1);
                  stacks_.set(current_slot_index_, current_stack);
                  return true;
                default:
                  return false;
              }
            }
          }
        } catch(Throwable e) {
          ModEngineersDecor.logger.error("Exception while trying to place " + e);
          world.setBlockToAir(placement_pos);
        }
        return spit_out(facing);
      }
      if(world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(placement_pos)).size() > 0) return false;
      if(!world.getBlockState(placement_pos).getBlock().isReplaceable(world, placement_pos)) return false;
      try {
        final FakePlayer placer = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer)world);
        //println("PLACE ITEMBLOCK" + current_stack + "  --> " + block + " at " + placement_pos.subtract(pos) + "( item=" + item + ")");
        ItemStack placement_stack = current_stack.copy();
        placement_stack.setCount(1);
        if((placer==null) || (!place_item(placement_stack, placer, world, placement_pos, EnumHand.MAIN_HAND, EnumFacing.DOWN, 0.6f, 0f, 0.5f))) return false;
        current_stack.shrink(1);
        stacks_.set(current_slot_index_, current_stack);
      } catch(Throwable e) {
        // The block really needs a player or other issues happened during placement.
        // A hard crash should not be fired here, instead spit out the item to indicated that this
        // block is not compatible.
        ModEngineersDecor.logger.error("Exception while trying to place " + e);
        world.setBlockToAir(placement_pos);
        return spit_out(facing);
      }
      return true;
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
      if(trigger) {
         if(try_place(placer_facing)) {
           dirty = true;
         } else {
           current_slot_index_ = next_slot(current_slot_index_);
         }
      }
      if(dirty) markDirty();
      if(trigger && (tick_timer_ > TICK_INTERVAL)) tick_timer_ = TICK_INTERVAL;
    }
  }
}
