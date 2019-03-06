/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.Explosion;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorCraftingTable extends BlockDecorDirected
{

  public BlockDecorCraftingTable(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorCraftingTable.BTileEntity(); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_CRAFTING_TABLE, world, pos.getX(), pos.getY(), pos.getZ());
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
    if(!(te instanceof BTileEntity)) return;
    ((BTileEntity)te).readnbt(inventory_nbt);
    ((BTileEntity)te).markDirty();
  }

  private ItemStack itemize_with_inventory(World world, BlockPos pos)
  {
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return ItemStack.EMPTY;
    ItemStack stack = new ItemStack(this, 1);
    NBTTagCompound inventory_nbt = new NBTTagCompound();
    ItemStackHelper.saveAllItems(inventory_nbt, ((BTileEntity)te).stacks, false);
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
    if(!(te instanceof BTileEntity)) return;
    for(ItemStack stack: ((BTileEntity)te).stacks) {
      if(!stack.isEmpty()) world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
    }
    ((BTileEntity)te).reset();
    super.onBlockExploded(world, pos, explosion);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // ModEngineersDecor.GuiHandler connectors
  //--------------------------------------------------------------------------------------------------------------------

  public static Object getServerGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof BTileEntity) ? (new BContainer(player.inventory, world, pos, (BTileEntity)te)) : null; }

  public static Object getClientGuiElement(final EntityPlayer player, final World world, final BlockPos pos, final TileEntity te)
  { return (te instanceof BTileEntity) ? (new BGuiCrafting(player.inventory, world, pos, (BTileEntity)te)) : null; }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  public static class BGuiCrafting extends GuiContainer
  {
     public BGuiCrafting(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    { super(new BContainer(playerInventory, world, pos, te)); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    { super.drawScreen(mouseX, mouseY, partialTicks); renderHoveredToolTip(mouseX, mouseY); }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {}

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/treated_wood_crafting_table.png"));
      drawTexturedModalRect(((this.width - this.xSize)/2), ((this.height - this.ySize)/2), 0, 0, this.xSize, this.ySize);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends Container
  {
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final BTileEntity te;
    public BInventoryCrafting craftMatrix;
    public InventoryCraftResult craftResult = new InventoryCraftResult();


    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      this.craftMatrix = new BInventoryCrafting(this, te);
      this.craftMatrix.openInventory(player);
      this.addSlotToContainer(new SlotCrafting(playerInventory.player, this.craftMatrix, this.craftResult, 0, 124+14, 35));
      for(int y=0; y<3; ++y) {
        for(int x=0; x<3; ++x) {
          addSlotToContainer(new Slot(this.craftMatrix, x+y*3, 28+30+x*18, 17+y*18));   // block slots 0..8
        }
      }
      for(int y=0; y<3; ++y) {
        for (int x=0; x<9; ++x) {
          this.addSlotToContainer(new Slot(playerInventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
      for (int x=0; x<9; ++x) {
        this.addSlotToContainer(new Slot(playerInventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<4; ++y) {
        for (int x=0; x<2; ++x) {
          this.addSlotToContainer(new Slot(this.craftMatrix, x+y*2+9, 8+x*18, 9+y*18)); // block slots 9..17
        }
      }
      this.onCraftMatrixChanged(this.craftMatrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorCraftingTable) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public void onCraftMatrixChanged(IInventory inv)
    {
      try {
        slotChangedCraftingGrid(this.world, this.player, this.craftMatrix, this.craftResult);
      } catch(Throwable exc) {
        ModEngineersDecor.logger.error("Recipe failed:", exc);
      }
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
      craftMatrix.closeInventory(player);
      craftResult.clear();
      craftResult.closeInventory(player);
      if(player!=null) {
        for(Slot e:player.inventoryContainer.inventorySlots) {
          if(e instanceof SlotCrafting) {
            ((SlotCrafting)e).putStack(ItemStack.EMPTY);
          }
        }
      }
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot)
    { return (slot.inventory != this.craftResult) && (super.canMergeSlot(stack, slot)); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
    {
      ItemStack stack = ItemStack.EMPTY;
      Slot slot = this.inventorySlots.get(index);
      if((slot == null) || (!slot.getHasStack())) return stack;
      ItemStack slotstack = slot.getStack();
      stack = slotstack.copy();
      if(index == 0) {
        slotstack.getItem().onCreated(slotstack, this.world, playerIn);
        if(!this.mergeItemStack(slotstack, 10, 46, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slotstack, stack);
      } else if (index >= 10 && (index < 37)) {
        if(!this.mergeItemStack(slotstack, 37, 46, false)) return ItemStack.EMPTY;
      } else if((index >= 37) && (index < 46)) {
        if(!this.mergeItemStack(slotstack, 10, 37, false)) return ItemStack.EMPTY;
      } else if(!this.mergeItemStack(slotstack, 10, 46, false)) {
        return ItemStack.EMPTY;
      }
      if(slotstack.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }
      if(slotstack.getCount() == stack.getCount()) {
        return ItemStack.EMPTY;
      }
      ItemStack itemstack2 = slot.onTake(playerIn, slotstack);
      if(index == 0) {
        playerIn.dropItem(itemstack2, false);
      }
      return stack;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting inventory (needed to allow SlotCrafting to have a InventoryCrafting)
  //--------------------------------------------------------------------------------------------------------------------

  public static class BInventoryCrafting extends InventoryCrafting
  {
    protected final Container container;
    protected final IInventory inventory;

    public BInventoryCrafting(Container container_, IInventory inventory_te) {
      super(container_, 3, 3);
      container = container_;
      inventory = inventory_te;
    }

    @Override
    public int getSizeInventory()
    { return 9; }

    @Override
    public void openInventory(EntityPlayer player)
    { inventory.openInventory(player); }

    @Override
    public void closeInventory(EntityPlayer player)
    { inventory.closeInventory(player); }

    @Override
    public void markDirty()
    { inventory.markDirty(); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
      inventory.setInventorySlotContents(index, stack);
      container.onCraftMatrixChanged(this);
    }

    @Override
    public ItemStack getStackInSlot(int index)
    { return inventory.getStackInSlot(index); }

    @Override
    public ItemStack decrStackSize(int index, int count)
    {
      final ItemStack stack = inventory.decrStackSize(index, count);
      if(!stack.isEmpty()) container.onCraftMatrixChanged(this);
      return stack;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------
  public static class BTileEntity extends TileEntity implements IInventory
  {
    public static final int NUM_OF_CRAFTING_SLOTS = 9;
    public static final int NUM_OF_STORAGE_SLOTS = 9;
    public static final int NUM_OF_SLOTS = NUM_OF_CRAFTING_SLOTS+NUM_OF_STORAGE_SLOTS;
    protected NonNullList<ItemStack> stacks;

    public BTileEntity()
    { stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void reset()
    { stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void readnbt(NBTTagCompound compound)
    {
      reset();
      ItemStackHelper.loadAllItems(compound, this.stacks);
      while(this.stacks.size() < NUM_OF_SLOTS) this.stacks.add(ItemStack.EMPTY);
    }

    private void writenbt(NBTTagCompound compound)
    { ItemStackHelper.saveAllItems(compound, this.stacks); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorCraftingTable)); }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    { super.readFromNBT(compound); readnbt(compound); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    { super.writeToNBT(compound); writenbt(compound); return compound; }

    //@Override
    //public NBTTagCompound getUpdateTag()
    //{ return writeToNBT(new NBTTagCompound()); }

    //@Override
    //public SPacketUpdateTileEntity getUpdatePacket()
    //{ return new SPacketUpdateTileEntity(getPos(), 0x1, getUpdateTag()); }
    //
    //@Override
    //public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    //{ readFromNBT(pkt.getNbtCompound()); super.onDataPacket(net, pkt); }

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
    // @see net.minecraft.inventory.InventoryCrafting

    @Override
    public int getSizeInventory()
    { return stacks.size(); }

    @Override
    public boolean isEmpty()
    { for(ItemStack stack: stacks) { if(!stack.isEmpty()) return false; } return true; }

    @Override
    public ItemStack getStackInSlot(int index)
    { return (index < getSizeInventory()) ? stacks.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    { stacks.set(index, stack); }

    @Override
    public int getInventoryStackLimit()
    { return 64; }

    @Override
    public void markDirty()
    { super.markDirty(); }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    { return true; }

    @Override
    public void openInventory(EntityPlayer player)
    {}

    @Override
    public void closeInventory(EntityPlayer player)
    { this.markDirty(); }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    { return true; }

    @Override
    public int getField(int id)
    { return 0; }

    @Override
    public void setField(int id, int value)
    {}

    @Override
    public int getFieldCount()
    { return 0; }

    @Override
    public void clear()
    { stacks.clear(); }

  }

}
