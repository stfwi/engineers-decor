/*
 * @file BlockDecorCraftingTable.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Mod crafting table, different style, UI and fetature set
 * than vanilla crafting table.
 */
package wile.engineersdecor.blocks;

import net.minecraft.client.gui.GuiButtonImage;
import wile.engineersdecor.ModEngineersDecor;
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
import net.minecraft.inventory.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.google.common.collect.ImmutableList;
import wile.engineersdecor.detail.Networking;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BlockDecorCraftingTable extends BlockDecorDirected
{
  public static boolean with_assist = true;

  public static final void on_config(boolean without_crafting_assist)
  {
    with_assist = !without_crafting_assist;
    CraftingHistory.max_history_size(32);
  }

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
  { return (te instanceof BTileEntity) ? (new BGui(player.inventory, world, pos, (BTileEntity)te)) : null; }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting history
  //--------------------------------------------------------------------------------------------------------------------

  private static class CraftingHistory
  {
    public static final List<ItemStack> NOTHING = new ArrayList<ItemStack>();

    private List<String> history_ = new ArrayList<String>();
    private int current_ = -1;
    private static int max_history_size_ = 5;

    public CraftingHistory()
    {}

    public static int max_history_size()
    { return max_history_size_; }

    public static int max_history_size(int newsize)
    { return max_history_size_ = MathHelper.clamp(newsize, 0, 32); }

    public void read(final NBTTagCompound nbt)
    {
      try {
        clear();
        final NBTTagCompound subsect = nbt.getCompoundTag("history");
        if(subsect.isEmpty()) return;
        {
          String s = subsect.getString("elements");
          if((s!=null) && (s.length() > 0)) {
            String[] ls = s.split("[|]");
            for(String e:ls) history_.add(e.toLowerCase().trim());
          }
        }
        current_ = (!subsect.hasKey("current")) ? (-1) : MathHelper.clamp(subsect.getInteger("current"), -1, history_.size()-1);
      } catch(Throwable ex) {
        ModEngineersDecor.logger.error("Exception reading crafting table history NBT, resetting, exception is:" + ex.getMessage());
        clear();
      }
    }

    public void write(final NBTTagCompound nbt)
    {
      final NBTTagCompound subsect = new NBTTagCompound();
      subsect.setInteger("current", current_);
      subsect.setString("elements", String.join("|", history_));
      nbt.setTag("history", subsect);
    }

    public void clear()
    { current_ = -1; history_.clear(); }

    public void reset_curent()
    { current_ = -1; }

    public void add(final List<ItemStack> grid_stacks)
    {
      if(!with_assist) { clear(); return; }
      String s = stacks2str(grid_stacks);
      if(s.isEmpty()) return;
      history_.removeIf(e->e.equals(s));
      history_.add(s);
      while(history_.size() > max_history_size()) history_.remove(0);
      if(current_ >= history_.size()) current_ = -1;
    }

    public String stacks2str(final List<ItemStack> grid_stacks)
    {
      if((grid_stacks==null) || (grid_stacks.size() != 10)) return "";
      if(grid_stacks.get(0).isEmpty()) return "";
      final ArrayList<String> items = new ArrayList<String>();
      for(ItemStack st:grid_stacks) {
        int meta = st.getMetadata();
        items.add( (st.isEmpty()) ? ("") : ((st.getItem().getRegistryName().toString().trim()) + ((meta==0)?(""):("/"+meta)) ));
      }
      return String.join(";", items);
    }

    public List<ItemStack> str2stacks(final String entry)
    {
      try {
        if((entry == null) || (entry.isEmpty())) return NOTHING;
        ArrayList<String> item_regnames = new ArrayList<String>(Arrays.asList(entry.split("[;]")));
        if((item_regnames == null) || (item_regnames.size() > 10)) return NOTHING;
        while(item_regnames.size() < 10) item_regnames.add("");
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for(String regname : item_regnames) {
          ItemStack stack = ItemStack.EMPTY;
          if(!regname.isEmpty()) {
            int meta = 0;
            if(regname.indexOf('/') >= 0) {
              String[] itemdetails = regname.split("[/]");
              if(itemdetails.length>0) regname = itemdetails[0];
              try { if(itemdetails.length>1) meta = Integer.parseInt(itemdetails[1]); } catch(Throwable e){meta=0;} // ignore exception here
            }
            final Item item = Item.REGISTRY.getObject(new ResourceLocation(regname));
            stack = ((item == null) || (item == Items.AIR)) ? ItemStack.EMPTY : (new ItemStack(item, 1, meta));
          }
          stacks.add(stack);
        }
        if((stacks.size() != 10) || (stacks.get(0).isEmpty())) return NOTHING; // invalid size or no result
        return stacks;
      } catch(Throwable ex) {
        ModEngineersDecor.logger.error("History stack building failed: " + ex.getMessage());
        return NOTHING;
      }
    }

    public List<ItemStack> current()
    {
      if((current_ < 0) || (current_ >= history_.size())) { current_ = -1; return NOTHING; }
      return str2stacks(history_.get(current_));
    }

    public void next()
    {
      if(history_.isEmpty()) { current_ = -1; return; }
      current_ = ((++current_) >= history_.size()) ? (-1) : (current_);
    }

    public void prev()
    {
      if(history_.isEmpty()) { current_ = -1; return; }
      current_ = ((--current_) < -1) ? (history_.size()-1) : (current_);
    }

    public String toString()
    {
      StringBuilder s = new StringBuilder("{ current:" + current_ + ", elements:[ ");
      for(int i=0; i<history_.size(); ++i) s.append("{i:").append(i).append(", e:[").append(history_.get(i)).append("]} ");
      s.append("]}");
      return s.toString();
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  private static class BGui extends GuiContainer
  {
    protected static final int BUTTON_NEXT = 0;
    protected static final int BUTTON_PREV = 1;
    protected static final int BUTTON_FROM_STORAGE = 2;
    protected static final int BUTTON_TO_STORAGE = 3;
    protected static final int BUTTON_FROM_PLAYER = 4;
    protected static final int BUTTON_TO_PLAYER = 5;
    protected static final ResourceLocation BACKGROUND = new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/treated_wood_crafting_table.png");
    protected final BTileEntity te;
    protected final ArrayList<GuiButton> buttons = new ArrayList<GuiButton>();

    public BGui(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    { super(new BContainer(playerInventory, world, pos, te)); this.te = te; }

    @Override
    @SuppressWarnings("unused")
    public void initGui()
    {
      super.initGui();
      final int x0=((width - xSize)/2), y0=((height - ySize)/2);
      buttons.clear();
      if(with_assist) {
        buttons.add(addButton(new GuiButtonImage(BUTTON_NEXT, x0+156,y0+44, 12,12, 194,44, 12, BACKGROUND)));
        buttons.add(addButton(new GuiButtonImage(BUTTON_PREV, x0+156,y0+30, 12,12, 180,30, 12, BACKGROUND)));
        //  buttons.add(addButton(new GuiButtonImage(BUTTON_FROM_STORAGE, x0+49,y0+34, 9,17, 219,34, 17, BACKGROUND)));
        //  buttons.add(addButton(new GuiButtonImage(BUTTON_TO_STORAGE, x0+49,y0+52, 9,17, 208,16, 17, BACKGROUND)));
        //  buttons.add(addButton(new GuiButtonImage(BUTTON_FROM_PLAYER, x0+77,y0+71, 17,9, 198,71, 9, BACKGROUND)));
        //  buttons.add(addButton(new GuiButtonImage(BUTTON_TO_PLAYER,   x0+59,y0+71, 17,9, 180,71, 9, BACKGROUND)));
      }
    }

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
      GlStateManager.color(1f, 1f, 1f, 1f);
      mc.getTextureManager().bindTexture(BACKGROUND);
      final int x0=((width - xSize)/2), y0=((height - ySize)/2);
      drawTexturedModalRect(x0, y0, 0, 0, xSize, ySize);
      if(with_assist) {
        List<ItemStack> crafting_template = te.history.current();
        if((crafting_template == null) || (crafting_template.isEmpty())) return;
        if(inventorySlots.getSlot(0).getHasStack()) return;
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : ((BContainer) inventorySlots).CRAFTING_SLOT_POSITIONS) {
            if((inventorySlots.getSlot(i).getHasStack())) {
              if(!inventorySlots.getSlot(i).getStack().getItem().equals(crafting_template.get(i).getItem())) {
                return; // user has placed another recipe
              }
            }
            ++i;
          }
        }
        {
          int i = 0;
          for(Tuple<Integer, Integer> e : ((BContainer) inventorySlots).CRAFTING_SLOT_POSITIONS) {
            final ItemStack stack = crafting_template.get(i);
            if(!stack.isEmpty()) drawTemplateItemAt(stack, x0, y0, e.getFirst(), e.getSecond());
            ++i;
          }
        }
      }
    }

    protected void drawTemplateItemAt(ItemStack stack, int x0, int y0, int x, int y)
    {
      RenderHelper.disableStandardItemLighting();
      RenderHelper.enableGUIStandardItemLighting();
      float zl = itemRender.zLevel;
      itemRender.zLevel = -50;
      itemRender.renderItemIntoGUI(stack, x0+x, y0+y);
      itemRender.zLevel = zl;
      zLevel = 100;
      GlStateManager.color(0.7f, 0.7f, 0.7f, 0.8f);
      mc.getTextureManager().bindTexture(BACKGROUND);
      drawTexturedModalRect(x0+x, y0+y, x, y, 16, 16);
      RenderHelper.enableStandardItemLighting();
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
      switch(button.id) {
        case BUTTON_NEXT:
        case BUTTON_PREV:
        case BUTTON_FROM_STORAGE:
        case BUTTON_TO_STORAGE:
        case BUTTON_FROM_PLAYER:
        case BUTTON_TO_PLAYER: {
          NBTTagCompound nbt = new NBTTagCompound();
          nbt.setInteger("button", button.id);
          Networking.PacketTileNotify.sendToServer(te, nbt);
          break;
        }
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting slot of container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BSlotCrafting extends SlotCrafting
  {
    private final BTileEntity te;
    private final EntityPlayer player;

    public BSlotCrafting(BTileEntity te, EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition)
    { super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition); this.te = te; this.player=player; }

    @Override
    protected void onCrafting(ItemStack stack)
    {
      if((with_assist) && ((player.world!=null) && (!(player.world.isRemote))) && (!stack.isEmpty())) {
        final ArrayList<ItemStack> grid = new ArrayList<ItemStack>();
        grid.add(stack);
        for(int i = 0; i < 9; ++i) grid.add(te.stacks.get(i));
        te.history.add(grid);
        te.history.reset_curent();
        te.syncHistory(player);
      }
      super.onCrafting(stack);
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting container
  //--------------------------------------------------------------------------------------------------------------------

  private static class BContainer extends Container
  {
    private final World world;
    private final BlockPos pos;
    private final EntityPlayer player;
    private final BTileEntity te;
    public BInventoryCrafting craftMatrix;
    public InventoryCraftResult craftResult = new InventoryCraftResult();
    public final ImmutableList<Tuple<Integer,Integer>> CRAFTING_SLOT_POSITIONS;

    public BContainer(InventoryPlayer playerInventory, World world, BlockPos pos, BTileEntity te)
    {
      ArrayList<Tuple<Integer,Integer>> slotpositions = new ArrayList<Tuple<Integer,Integer>>();
      this.player = playerInventory.player;
      this.world = world;
      this.pos = pos;
      this.te = te;
      craftMatrix = new BInventoryCrafting(this, te);
      craftMatrix.openInventory(player);
      addSlotToContainer(new BSlotCrafting(te, playerInventory.player, craftMatrix, craftResult, 0, 134, 35));
      slotpositions.add(new Tuple<>(134, 35));
      for(int y=0; y<3; ++y) {
        for(int x=0; x<3; ++x) {
          int xpos = 60+x*18;
          int ypos = 17+y*18;
          addSlotToContainer(new Slot(craftMatrix, x+y*3, xpos, ypos));   // block slots 0..8
          slotpositions.add(new Tuple<>(xpos, ypos));
        }
      }
      CRAFTING_SLOT_POSITIONS = ImmutableList.copyOf(slotpositions);
      for(int y=0; y<3; ++y) {
        for (int x=0; x<9; ++x) {
          addSlotToContainer(new Slot(playerInventory, x+y*9+9, 8+x*18, 86+y*18)); // player slots: 9..35
        }
      }
      for(int x=0; x<9; ++x) {
        addSlotToContainer(new Slot(playerInventory, x, 8+x*18, 144)); // player slots: 0..8
      }
      for(int y=0; y<4; ++y) {
        for (int x=0; x<2; ++x) {
          addSlotToContainer(new Slot(craftMatrix, x+y*2+9, 8+x*18, 9+y*18)); // block slots 9..17
        }
      }
      onCraftMatrixChanged(craftMatrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    { return (world.getBlockState(pos).getBlock() instanceof BlockDecorCraftingTable) && (player.getDistanceSq(pos) <= 64); }

    @Override
    public void onCraftMatrixChanged(IInventory inv)
    {
      try {
        slotChangedCraftingGrid(world, player, craftMatrix, craftResult);
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
    { return (slot.inventory != craftResult) && (super.canMergeSlot(stack, slot)); }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
    {
      Slot slot = inventorySlots.get(index);
      if((slot == null) || (!slot.getHasStack())) return ItemStack.EMPTY;
      ItemStack slotstack = slot.getStack();
      ItemStack stack = slotstack.copy();
      if(index == 0) {
        slotstack.getItem().onCreated(slotstack, this.world, playerIn);
        if(!this.mergeItemStack(slotstack, 10, 46, true)) return ItemStack.EMPTY;
        slot.onSlotChange(slotstack, stack);
      } else if(index >= 10 && (index < 46)) {
        if(!this.mergeItemStack(slotstack, 46, 54, false)) return ItemStack.EMPTY;
      } else if((index >= 46) && (index < 54)) {
        if(!this.mergeItemStack(slotstack, 10, 46, false)) return ItemStack.EMPTY;
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

  private static class BInventoryCrafting extends InventoryCrafting
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
  public static class BTileEntity extends TileEntity implements IInventory, Networking.IPacketReceiver
  {
    public static final int NUM_OF_CRAFTING_SLOTS = 9;
    public static final int NUM_OF_STORAGE_SLOTS = 8;
    public static final int NUM_OF_SLOTS = NUM_OF_CRAFTING_SLOTS+NUM_OF_STORAGE_SLOTS;
    protected NonNullList<ItemStack> stacks;
    protected final CraftingHistory history = new CraftingHistory();

    public BTileEntity()
    { stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void reset()
    { stacks = NonNullList.<ItemStack>withSize(NUM_OF_SLOTS, ItemStack.EMPTY); }

    public void readnbt(NBTTagCompound compound)
    {
      reset();
      ItemStackHelper.loadAllItems(compound, stacks);
      while(stacks.size() < NUM_OF_SLOTS) stacks.add(ItemStack.EMPTY);
      history.read(compound);
    }

    private void writenbt(NBTTagCompound compound)
    {
      ItemStackHelper.saveAllItems(compound, stacks);
      history.write(compound);
    }

    // Networking.IPacketReceiver --------------------------------------------------------------

    @Override
    public void onClientPacketReceived(EntityPlayer player, NBTTagCompound nbt)
    {
      if(with_assist && nbt.hasKey("button")) {
        switch(nbt.getInteger("button")) {
          case BGui.BUTTON_NEXT:
            history.next();
            syncHistory(player);
            break;
          case BGui.BUTTON_PREV:
            history.prev();
            syncHistory(player);
            break;
          case BGui.BUTTON_FROM_STORAGE:
            //System.out.println("BUTTON_FROM_STORAGE");
            break;
          case BGui.BUTTON_TO_STORAGE:
            //System.out.println("BUTTON_TO_STORAGE");
            break;
          case BGui.BUTTON_FROM_PLAYER:
            //System.out.println("BUTTON_FROM_PLAYER");
            break;
          case BGui.BUTTON_TO_PLAYER:
            //System.out.println("BUTTON_TO_PLAYER");
            break;
        }
      }
    }

    @Override
    public void onServerPacketReceived(NBTTagCompound nbt)
    {
      if(nbt.hasKey("historydata")) history.read(nbt.getCompoundTag("historydata"));
    }

    private void syncHistory(EntityPlayer player)
    {
      if(!with_assist) return;
      NBTTagCompound history_nbt = new NBTTagCompound();
      history.write(history_nbt);
      NBTTagCompound rnbt = new NBTTagCompound();
      rnbt.setTag("historydata", history_nbt);
      Networking.PacketTileNotify.sendToPlayer(player, this, rnbt);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorCraftingTable)); }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    { super.readFromNBT(nbt); readnbt(nbt); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    { super.writeToNBT(nbt); writenbt(nbt); return nbt; }

    @Override
    public NBTTagCompound getUpdateTag()
    { NBTTagCompound nbt = new NBTTagCompound(); super.writeToNBT(nbt); writenbt(nbt); return nbt; }

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
