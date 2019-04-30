/*
 * @file BlockFurnace.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * ED Lab furnace.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ExtItems;

import net.minecraft.stats.StatList;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.SoundEvents;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.crafting.FurnaceRecipes;
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
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.google.common.collect.Maps;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;

public class BlockDecorFurnace extends BlockDecorDirected
{
  public static final PropertyBool LIT = PropertyBool.create("lit");

  public BlockDecorFurnace(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, LIT); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 0x3)).withProperty(LIT, (meta & 0x4)!=0); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return (state.getValue(FACING).getHorizontalIndex() & 0x3) | (state.getValue(LIT) ? 4 : 0); }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(LIT, false); }

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
  { return new BlockDecorFurnace.BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {
    if(world.isRemote) return;
    if((!stack.hasTagCompound()) || (!stack.getTagCompound().hasKey("inventory"))) return;
    NBTTagCompound inventory_nbt = stack.getTagCompound().getCompoundTag("inventory");
    if(inventory_nbt.isEmpty()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).readnbt(inventory_nbt);
    ((BTileEntity)te).markDirty();
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
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_SMALL_LAB_FURNACE, world, pos.getX(), pos.getY(), pos.getZ());
    player.addStat(StatList.FURNACE_INTERACTION);
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
    final double xc=0.52, xr=rnd.nextDouble()*0.4-0.2, yr=(y-0.3+rnd.nextDouble()*0.2);
    if(rv < 0.1d) world.playSound(x, y, z, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 0.4f, 0.5f, false);
    switch(state.getValue(FACING)) {
      case WEST:  world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x-xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case EAST:  world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xc, yr, z+xr, 0.0, 0.0, 0.0); break;
      case NORTH: world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xr, yr, z-xc, 0.0, 0.0, 0.0); break;
      default:    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x+xr, yr, z+xc, 0.0, 0.0, 0.0); break;
    }
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
      mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/small_lab_furnace_gui.png"));
      final int x0=(width-xSize)/2, y0=(height-ySize)/2, w=xSize, h=ySize;
      drawTexturedModalRect(x0, y0, 0, 0, w, h);
      if(BTileEntity.isBurning(te))  {
        final int k = flame_px(13);
        drawTexturedModalRect(x0+59, y0+36+12-k, 176, 12-k, 14, k+1);
      }
      drawTexturedModalRect(x0+79, y0+36, 176, 15, 1+progress_px(17), 15);
    }

    private int progress_px(int pixels)
    { final int tc=te.getField(2), T=te.getField(3); return ((T>0) && (tc>0)) ? (tc * pixels / T) : (0); }

    private int flame_px(int pixels)
    { int ibt = te.getField(1); return ((te.getField(0) * pixels) / ((ibt>0) ? (ibt) : (BTileEntity.proc_speed_interval_))); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // container slots
  //--------------------------------------------------------------------------------------------------------------------

  public static class BSlotInpFifo extends Slot
  {
    public BSlotInpFifo(IInventory inv, int index, int xpos, int ypos)
    { super(inv, index, xpos, ypos); }
  }

  public static class BSlotFuelFifo extends Slot
  {
    public BSlotFuelFifo(IInventory inv, int index, int xpos, int ypos)
    { super(inv, index, xpos, ypos); }
  }

  public static class BSlotOutFifo extends BSlotResult
  {
    public BSlotOutFifo(EntityPlayer player, BTileEntity te, int index, int xpos, int ypos)
    { super(player, te, index, xpos, ypos); }
  }

  public static class BSlotResult extends Slot
  {
    // This class is basically SlotFurnaceOutput.onCrafting(), except that the recipe overrides
    // are used, unfortunately a copy is needed due to private instance variables.
    private final EntityPlayer player;
    private int removeCount = 0;

    public BSlotResult(EntityPlayer player, BTileEntity te, int index, int xpos, int ypos)
    { super(te, index, xpos, ypos); this.player = player; }

    @Override
    public boolean isItemValid(ItemStack stack)
    { return false; }

    @Override
    public ItemStack decrStackSize(int amount)
    { removeCount += getHasStack() ? Math.min(amount, getStack().getCount()) : 0; return super.decrStackSize(amount); }

    @Override
    public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack)
    { onCrafting(stack); super.onTake(thePlayer, stack); return stack; }

    @Override
    protected void onCrafting(ItemStack stack, int amount)
    { removeCount += amount; onCrafting(stack); }

    @Override
    protected void onCrafting(ItemStack stack)
    {
      stack.onCrafting(player.world, player, removeCount);
      if(!player.world.isRemote) {
        int xp = removeCount;
        float sxp = BRecipes.instance().getSmeltingExperience(stack);
        if(sxp == 0) {
          xp = 0;
        } else if(sxp < 1.0) {
          xp = (int)((sxp*xp) + Math.round(Math.random()+0.75));
        }
        while(xp > 0) {
          int k = EntityXPOrb.getXPSplit(xp);
          xp -= k;
          player.world.spawnEntity(new EntityXPOrb(player.world, player.posX, player.posY+0.5, player.posZ+0.5, k));
        }
      }
      removeCount = 0;
      net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerSmeltedEvent(player, stack);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends Container
  {
    private static final int PLAYER_INV_START_SLOTNO = 11;
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final BTileEntity te;
    private int proc_time_elapsed_;
    private int burntime_left_;
    private int fuel_burntime_;
    private int proc_time_needed_;

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      addSlotToContainer(new Slot(te, 0, 59, 17)); // smelting input
      addSlotToContainer(new SlotFurnaceFuel(te, 1, 59, 53)); // fuel
      addSlotToContainer(new BSlotResult(playerInventory.player, te, 2, 101, 35)); // smelting result
      addSlotToContainer(new BSlotInpFifo(te, 3, 34, 17)); // input fifo 0
      addSlotToContainer(new BSlotInpFifo(te, 4, 16, 17)); // input fifo 1
      addSlotToContainer(new BSlotFuelFifo(te, 5, 34, 53)); // fuel fifo 0
      addSlotToContainer(new BSlotFuelFifo(te, 6, 16, 53)); // fuel fifo 1
      addSlotToContainer(new BSlotOutFifo(playerInventory.player, te, 7, 126, 35)); // out fifo 0
      addSlotToContainer(new BSlotOutFifo(playerInventory.player, te, 8, 144, 35)); // out fifo 1
      addSlotToContainer(new Slot(te,  9, 126, 61)); // aux slot 1
      addSlotToContainer(new Slot(te, 10, 144, 61)); // aux slot 2
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
        if(proc_time_elapsed_ != te.getField(2)) lis.sendWindowProperty(this, 2, te.getField(2));
        if(burntime_left_     != te.getField(0)) lis.sendWindowProperty(this, 0, te.getField(0));
        if(fuel_burntime_     != te.getField(1)) lis.sendWindowProperty(this, 1, te.getField(1));
        if(proc_time_needed_  != te.getField(3)) lis.sendWindowProperty(this, 3, te.getField(3));
      }
      proc_time_elapsed_ = te.getField(2);
      burntime_left_     = te.getField(0);
      fuel_burntime_     = te.getField(1);
      proc_time_needed_  = te.getField(3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    { te.setField(id, data); }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorFurnace) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slot_stack = slot.getStack();
      ItemStack transferred = slot_stack.copy();
      if((index==2) || (index==7) || (index==8)) {
        // Output slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slot_stack, transferred);
      } else if((index==0) || (index==3) || (index==4)) {
        // Input slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==1) || (index==5) || (index==6)) {
        // Fuel slots
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index==9) || (index==10)) {
        if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
      } else if((index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36)) {
        // Player inventory
        if(!BRecipes.instance().getSmeltingResult(slot_stack).isEmpty()) {
          if(
            (!mergeItemStack(slot_stack, 0, 1, false)) && // smelting input
            (!mergeItemStack(slot_stack, 3, 4, false)) && // fifo0
            (!mergeItemStack(slot_stack, 4, 5, false))    // fifo1
          ) return ItemStack.EMPTY;
        } else if(TileEntityFurnace.isItemFuel(slot_stack)) {
          if(
            (!mergeItemStack(slot_stack, 1, 2, false)) && // fuel input
            (!mergeItemStack(slot_stack, 5, 6, false)) && // fuel fifo0
            (!mergeItemStack(slot_stack, 6, 7, false))    // fuel fifo1
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

  public static class BTileEntity extends TileEntity implements ITickable, ISidedInventory, IEnergyStorage
  {
    public static final int TICK_INTERVAL = 4;
    public static final int FIFO_INTERVAL = 20;
    public static final int MAX_BURNTIME = 0x7fff;
    public static final int DEFAULT_BOOST_ENERGY = 32;
    public static final int VANILLA_FURNACE_SPEED_INTERVAL = 200;
    public static final int DEFAULT_SPEED_INTERVAL  = 150;
    public static final int NUM_OF_SLOTS = 11;
    public static final int SMELTING_INPUT_SLOT_NO  = 0;
    public static final int SMELTING_FUEL_SLOT_NO   = 1;
    public static final int SMELTING_OUTPUT_SLOT_NO = 2;
    public static final int FIFO_INPUT_0_SLOT_NO    = 3;
    public static final int FIFO_INPUT_1_SLOT_NO    = 4;
    public static final int FIFO_FUEL_0_SLOT_NO     = 5;
    public static final int FIFO_FUEL_1_SLOT_NO     = 6;
    public static final int FIFO_OUTPUT_0_SLOT_NO   = 7;
    public static final int FIFO_OUTPUT_1_SLOT_NO   = 8;
    public static final int AUX_0_SLOT_NO           = 9;
    public static final int AUX_1_SLOT_NO           =10;

    private static final int[] SLOTS_TOP    = new int[] {FIFO_INPUT_1_SLOT_NO};
    private static final int[] SLOTS_BOTTOM = new int[] {FIFO_OUTPUT_1_SLOT_NO};
    private static final int[] SLOTS_SIDES  = new int[] {FIFO_FUEL_1_SLOT_NO};
    private final IItemHandler sided_itemhandler_top_   = new SidedInvWrapper(this, EnumFacing.UP);
    private final IItemHandler sided_itemhandler_down_  = new SidedInvWrapper(this, EnumFacing.DOWN);
    private final IItemHandler sided_itemhandler_sides_ = new SidedInvWrapper(this, EnumFacing.WEST);
    private static double proc_fuel_efficiency_ = 1.0;
    private static int proc_speed_interval_ = DEFAULT_SPEED_INTERVAL;
    private static int boost_energy_consumption = DEFAULT_BOOST_ENERGY * TICK_INTERVAL;

    private int tick_timer_;
    private int fifo_timer_;
    private int burntime_left_;
    private int fuel_burntime_;
    private int proc_time_elapsed_;
    private int proc_time_needed_;
    private int boost_energy_; // small, not saved in nbt.
    private boolean heater_inserted_ = false;
    private NonNullList<ItemStack> stacks_;

    public static void on_config(int speed_percent, int fuel_efficiency_percent, int boost_energy_per_tick)
    {
      double ratio = (100.0 / MathHelper.clamp(speed_percent, 10, 500)) ;
      proc_speed_interval_ = MathHelper.clamp((int)(ratio * VANILLA_FURNACE_SPEED_INTERVAL), 20, 400);
      proc_fuel_efficiency_ = ((double) MathHelper.clamp(fuel_efficiency_percent, 10, 500)) / 100;
      boost_energy_consumption = TICK_INTERVAL * MathHelper.clamp(boost_energy_per_tick, 16, 512);
      ModEngineersDecor.logger.info("Config lab furnace interval:" + proc_speed_interval_ + ", efficiency:" + proc_fuel_efficiency_);
    }

    public BTileEntity()
    { reset(); }

    public void reset()
    {
      stacks_ = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
      proc_time_elapsed_ = 0;
      proc_time_needed_ = 0;
      burntime_left_ = 0;
      fuel_burntime_ = 0;
      fifo_timer_ = 0;
      tick_timer_ = 0;
    }

    public void readnbt(NBTTagCompound compound)
    {
      reset();
      ItemStackHelper.loadAllItems(compound, this.stacks_);
      while(this.stacks_.size() < NUM_OF_SLOTS) this.stacks_.add(ItemStack.EMPTY);
      burntime_left_ = compound.getInteger("BurnTime");
      proc_time_elapsed_ = compound.getInteger("CookTime");
      proc_time_needed_ = compound.getInteger("CookTimeTotal");
      fuel_burntime_ = getItemBurnTime(stacks_.get(SMELTING_FUEL_SLOT_NO));
    }

    private void writenbt(NBTTagCompound compound)
    {
      compound.setInteger("BurnTime", MathHelper.clamp(burntime_left_,0 , MAX_BURNTIME));
      compound.setInteger("CookTime", MathHelper.clamp(proc_time_elapsed_, 0, MAX_BURNTIME));
      compound.setInteger("CookTimeTotal", MathHelper.clamp(proc_time_needed_, 0, MAX_BURNTIME));
      ItemStackHelper.saveAllItems(compound, stacks_);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorFurnace)); }

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
      ItemStack slot_stack = stacks_.get(index);
      boolean already_in_slot = (!stack.isEmpty()) && (stack.isItemEqual(slot_stack)) && (ItemStack.areItemStackTagsEqual(stack, slot_stack));
      stacks_.set(index, stack);
      if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
      if((index == SMELTING_INPUT_SLOT_NO) && (!already_in_slot)) {
        proc_time_needed_ = getCookTime(stack);
        proc_time_elapsed_ = 0;
        markDirty();
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
    { return ((world.getTileEntity(pos) == this) && (player.getDistanceSq(pos.getX()+0.5d, pos.getY()+0.5d, pos.getZ()+0.5d) <= 64.0d)); }

    @Override
    public void openInventory(EntityPlayer player)
    {}

    @Override
    public void closeInventory(EntityPlayer player)
    { markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
      switch(index) {
        case SMELTING_OUTPUT_SLOT_NO:
        case FIFO_OUTPUT_0_SLOT_NO:
        case FIFO_OUTPUT_1_SLOT_NO:
          return false;
        case SMELTING_INPUT_SLOT_NO:
        case FIFO_INPUT_0_SLOT_NO:
        case FIFO_INPUT_1_SLOT_NO:
          return true;
        case AUX_0_SLOT_NO:
        case AUX_1_SLOT_NO:
          return true;
        default: {
          ItemStack slot_stack = stacks_.get(FIFO_FUEL_1_SLOT_NO);
          return isItemFuel(stack) || SlotFurnaceFuel.isBucket(stack) && (slot_stack.getItem() != Items.BUCKET);
        }
      }
    }

    @Override
    public int getField(int id)
    {
      switch (id) {
        case 0: return burntime_left_;
        case 1: return fuel_burntime_;
        case 2: return proc_time_elapsed_;
        case 3: return proc_time_needed_;
        default: return 0;
      }
    }

    @Override
    public void setField(int id, int value)
    {
      switch(id) {
        case 0: burntime_left_ = value; break;
        case 1: fuel_burntime_ = value; break;
        case 2: proc_time_elapsed_ = value; break;
        case 3: proc_time_needed_ = value; break;
      }
    }

    @Override
    public int getFieldCount()
    {  return 4; }

    @Override
    public void clear()
    { stacks_.clear(); }

    @Override
    public void update()
    {
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      final boolean was_burning = isBurning();
      if(was_burning) burntime_left_ -= TICK_INTERVAL;
      if(burntime_left_ < 0) burntime_left_ = 0;
      if(world.isRemote) return;
      boolean dirty = false;
      if(--fifo_timer_ <= 0) {
        fifo_timer_ = FIFO_INTERVAL/TICK_INTERVAL;
        // note, intentionally not using bitwise OR piping.
        if(transferItems(FIFO_OUTPUT_0_SLOT_NO, FIFO_OUTPUT_1_SLOT_NO, 1)) dirty = true;
        if(transferItems(SMELTING_OUTPUT_SLOT_NO, FIFO_OUTPUT_0_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_FUEL_0_SLOT_NO, SMELTING_FUEL_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_FUEL_1_SLOT_NO, FIFO_FUEL_0_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_INPUT_0_SLOT_NO, SMELTING_INPUT_SLOT_NO, 1)) dirty = true;
        if(transferItems(FIFO_INPUT_1_SLOT_NO, FIFO_INPUT_0_SLOT_NO, 1)) dirty = true;
        heater_inserted_ = (ExtItems.IE_EXTERNAL_HEATER==null) // without IE always allow electrical boost
          || (stacks_.get(AUX_0_SLOT_NO).getItem()==ExtItems.IE_EXTERNAL_HEATER)
          || (stacks_.get(AUX_1_SLOT_NO).getItem()==ExtItems.IE_EXTERNAL_HEATER);
      }
      ItemStack fuel = stacks_.get(SMELTING_FUEL_SLOT_NO);
      if(isBurning() || (!fuel.isEmpty()) && (!(stacks_.get(SMELTING_INPUT_SLOT_NO)).isEmpty())) {
        if(!isBurning() && canSmelt()) {
          burntime_left_ = (int)MathHelper.clamp((proc_fuel_efficiency_ * getItemBurnTime(fuel)), 0, MAX_BURNTIME);
          fuel_burntime_ = (burntime_left_ * proc_speed_interval_) / VANILLA_FURNACE_SPEED_INTERVAL;
          if(isBurning()) {
            dirty = true;
            if(!fuel.isEmpty()) {
              Item fuel_item = fuel.getItem();
              fuel.shrink(1);
              if(fuel.isEmpty()) stacks_.set(SMELTING_FUEL_SLOT_NO, fuel_item.getContainerItem(fuel));
            }
          }
        }
        if(isBurning() && canSmelt()) {
          proc_time_elapsed_ += TICK_INTERVAL;
          if(heater_inserted_ && (boost_energy_ >= boost_energy_consumption)) { boost_energy_ = 0; proc_time_elapsed_ += TICK_INTERVAL; }
          if(proc_time_elapsed_ >= proc_time_needed_) {
            proc_time_elapsed_ = 0;
            proc_time_needed_ = getCookTime(stacks_.get(SMELTING_INPUT_SLOT_NO));
            smeltItem();
            dirty = true;
          }
        } else {
          proc_time_elapsed_ = 0;
        }
      } else if(!isBurning() && (proc_time_elapsed_ > 0)) {
        proc_time_elapsed_ = MathHelper.clamp(proc_time_elapsed_-2, 0, proc_time_needed_);
      }
      if(was_burning != isBurning()) {
        dirty = true;
        final IBlockState state = world.getBlockState(pos);
        if(state.getBlock() instanceof BlockDecorFurnace) {
          world.setBlockState(pos, state.withProperty(LIT, isBurning()));
        }
      }
      if(dirty) markDirty();
    }

    public boolean isBurning()
    { return burntime_left_ > 0; }

    @SideOnly(Side.CLIENT)
    public static boolean isBurning(IInventory inventory)
    { return inventory.getField(0) > 0; }

    public int getCookTime(ItemStack stack)
    { return proc_speed_interval_ < 10 ? 10 : proc_speed_interval_; }

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

    private boolean canSmelt()
    {
      if(stacks_.get(SMELTING_INPUT_SLOT_NO).isEmpty()) return false;
      final ItemStack recipe_result_items = BRecipes.instance().getSmeltingResult(stacks_.get(SMELTING_INPUT_SLOT_NO));
      if(recipe_result_items.isEmpty()) return false;
      final ItemStack result_stack = stacks_.get(SMELTING_OUTPUT_SLOT_NO);
      if(result_stack.isEmpty()) return true;
      if(!result_stack.isItemEqual(recipe_result_items)) return false;
      if(result_stack.getCount() + recipe_result_items.getCount() <= getInventoryStackLimit() && result_stack.getCount() + recipe_result_items.getCount() <= result_stack.getMaxStackSize()) return true;
      return result_stack.getCount() + recipe_result_items.getCount() <= recipe_result_items.getMaxStackSize();
    }

    public void smeltItem()
    {
      if(!canSmelt()) return;
      final ItemStack smelting_input_stack = stacks_.get(SMELTING_INPUT_SLOT_NO);
      final ItemStack recipe_result_items = BRecipes.instance().getSmeltingResult(smelting_input_stack);
      final ItemStack smelting_output_stack = stacks_.get(SMELTING_OUTPUT_SLOT_NO);
      final ItemStack fuel_stack = stacks_.get(SMELTING_FUEL_SLOT_NO);
      if(smelting_output_stack.isEmpty()) {
        stacks_.set(SMELTING_OUTPUT_SLOT_NO, recipe_result_items.copy());
      } else if(smelting_output_stack.getItem() == recipe_result_items.getItem()) {
        smelting_output_stack.grow(recipe_result_items.getCount());
      }
      smelting_input_stack.shrink(1);
    }

    public static int getItemBurnTime(ItemStack stack)
    { return TileEntityFurnace.getItemBurnTime(stack); }

    public static boolean isItemFuel(ItemStack stack)
    { return TileEntityFurnace.isItemFuel(stack); }

    // ISidedInventory ----------------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
      if(side == EnumFacing.DOWN) return SLOTS_BOTTOM;
      if(side == EnumFacing.UP) return SLOTS_TOP;
      return SLOTS_SIDES;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
    { return isItemValidForSlot(index, itemStackIn); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    {
      if((direction!=EnumFacing.DOWN) || ((index!=SMELTING_FUEL_SLOT_NO) && (index!=FIFO_FUEL_0_SLOT_NO) && (index!=FIFO_FUEL_1_SLOT_NO) )) return true;
      return (stack.getItem()==Items.BUCKET);
    }

    // IEnergyStorage ----------------------------------------------------------------------------

    public boolean canExtract()
    { return false; }

    public boolean canReceive()
    { return true; }

    public int getMaxEnergyStored()
    { return boost_energy_consumption; }

    public int getEnergyStored()
    { return boost_energy_; }

    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    public int receiveEnergy(int maxReceive, boolean simulate)
    { // only speedup support, no buffering, not in nbt -> no markdirty
      if((boost_energy_ >= boost_energy_consumption) || (maxReceive < boost_energy_consumption)) return 0;
      if(!simulate) boost_energy_ = boost_energy_consumption;
      return boost_energy_consumption;
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
      if((facing != null) && (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
        if(facing == EnumFacing.DOWN) return (T) sided_itemhandler_down_;
        if(facing == EnumFacing.UP) return (T) sided_itemhandler_top_;
        return (T) sided_itemhandler_sides_;
      } else if(capability == CapabilityEnergy.ENERGY) {
        return (T)this;
      } else {
        return super.getCapability(capability, facing);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Furnace recipe overrides
  //--------------------------------------------------------------------------------------------------------------------
  // Based on net.minecraft.item.crafting.FurnaceRecipes, copy as
  // needed methods are private.
  public static class BRecipes
  {
    private static final BRecipes RECIPPE_OVERRIDES = new BRecipes();
    private final Map<ItemStack, ItemStack> recipes_ = Maps.<ItemStack, ItemStack>newHashMap();
    private final Map<ItemStack, Float> experiences_ = Maps.<ItemStack, Float>newHashMap();

    public static BRecipes instance()
    { return RECIPPE_OVERRIDES; }

    private BRecipes()
    {}

    public Map<ItemStack, ItemStack> getRecipes()
    { return recipes_; }

    public void reset()
    { recipes_.clear(); experiences_.clear(); }

    public ItemStack getSmeltingResult(final ItemStack stack)
    {
      ItemStack res = override_result(stack);
      if(res.isEmpty()) res = FurnaceRecipes.instance().getSmeltingResult(stack);
      return res;
    }

    public float getSmeltingExperience(ItemStack stack)
    {
      float ret = stack.getItem().getSmeltingExperience(stack);
      if(ret != -1) return ret;
      for(Map.Entry<ItemStack, Float> e : experiences_.entrySet()) {
        if(compare(stack, e.getKey())) return e.getValue();
      }
      return FurnaceRecipes.instance().getSmeltingExperience(stack);
    }

    public void add(Block input, ItemStack stack, float experience)
    { add(Item.getItemFromBlock(input), stack, experience); }

    public void add(Item input, ItemStack stack, float experience)
    { add(new ItemStack(input, 1, 32767), stack, experience); }

    public void add(ItemStack input, ItemStack stack, float xp)
    {
      // Forced override setting
      if(input==ItemStack.EMPTY) return;
      if(recipes_.containsKey(input)) recipes_.remove(input);
      if(experiences_.containsKey(input)) experiences_.remove(input);
      if((stack==null) || (stack==ItemStack.EMPTY)) return;
      recipes_.put(input, stack);
      experiences_.put(stack, xp);
    }

    public ItemStack override_result(ItemStack stack)
    {
      for(Map.Entry<ItemStack, ItemStack> e:recipes_.entrySet()) {
        if(compare(stack, e.getKey())) return e.getValue();
      }
      return ItemStack.EMPTY;
    }

    private boolean compare(final ItemStack stack1, final ItemStack stack2)
    { return (stack2.getItem() == stack1.getItem()) && ((stack2.getMetadata() == 32767) || (stack2.getMetadata() == stack1.getMetadata())); }

  }

}
