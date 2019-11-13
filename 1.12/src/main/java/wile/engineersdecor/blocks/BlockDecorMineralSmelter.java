/*
 * @file BlockDecorMineralSmelter.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Small highly insulated stone liquification furnace
 * (magmatic phase).
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.Explosion;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.inventory.*;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BlockDecorMineralSmelter extends BlockDecorDirectedHorizontal
{
  public static final int PHASE_MAX = 3;
  public static final PropertyInteger PHASE = PropertyInteger.create("phase", 0, PHASE_MAX);

  public BlockDecorMineralSmelter(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, PHASE); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return super.getStateFromMeta(meta).withProperty(PHASE, (meta>>2) & 0x3); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return super.getMetaFromState(state) | (state.getValue(PHASE)<<2); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(PHASE, 0); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.SOLID; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(IBlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
  { return MathHelper.clamp((state.getValue(PHASE)*5), 0, 15); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorMineralSmelter.BTileEntity(); }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
  {}

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  {
    if(world.isRemote) return true;
    BTileEntity te = getTe(world, pos);
    if(te==null) return super.removedByPlayer(state, world, pos, player, willHarvest);
    ItemStack st = ItemStack.EMPTY;
    if(!te.getStackInSlot(1).isEmpty()) {
      st = te.getStackInSlot(1).copy();
    } else if(!te.getStackInSlot(0).isEmpty()) {
      st = te.getStackInSlot(0).copy();
    }
    te.reset_process();
    ItemStack stack = new ItemStack(this, 1);
    world.spawnEntity(new EntityItem(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack));
    world.setBlockToAir(pos);
    world.removeTileEntity(pos);
    if(!st.isEmpty()) {
      st.setCount(1);
      world.spawnEntity(new EntityItem(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, st));
    }
    return false;
  }

  @Override
  public void onBlockExploded(World world, BlockPos pos, Explosion explosion)
  {
    if(world.isRemote) return;
    BTileEntity te = getTe(world, pos);
    if(te==null) return;
    te.reset_process();
    super.onBlockExploded(world, pos, explosion);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    if(player.isSneaking()) return false;
    BTileEntity te = getTe(world, pos);
    if(te==null) return true;
    final ItemStack stack = player.getHeldItem(hand);
    boolean dirty = false;
    if(te.accepts_lava_container(stack)) {
      if(stack.isItemEqualIgnoreDurability(BTileEntity.BUCKET_STACK)) { // check how this works with item capabilities or so
        if(te.fluid_level() >= BTileEntity.MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
          if(stack.getCount() > 1) {
            int target_stack_index = -1;
            for(int i=0; i<player.inventory.getSizeInventory(); ++i) {
              if(player.inventory.getStackInSlot(i).isEmpty()) {
                target_stack_index = i;
                break;
              }
            }
            if(target_stack_index >= 0) {
              te.reset_process();
              stack.shrink(1);
              player.setHeldItem(hand, stack);
              player.inventory.setInventorySlotContents(target_stack_index, BTileEntity.LAVA_BUCKET_STACK.copy());
              world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1f, 1f);
              dirty = true;
            }
          } else {
            te.reset_process();
            player.setHeldItem(hand, BTileEntity.LAVA_BUCKET_STACK.copy());
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1f, 1f);
            dirty = true;
          }
        }
      }
    } else if(stack.getItem() == Items.AIR) {
      final ItemStack istack = te.getStackInSlot(1).copy();
      if(te.phase() > BTileEntity.PHASE_WARMUP) player.setFire(1);
      if(!istack.isEmpty()) {
        istack.setCount(1);
        player.setHeldItem(hand, istack);
        te.reset_process();
        dirty = true;
      }
    } else if(te.insert(stack.copy(),false)) {
      stack.shrink(1);
      dirty = true;
    }
    if(dirty) player.inventory.markDirty();
    return true;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rnd)
  {
    if(state.getBlock()!=this) return;
    EnumParticleTypes particle = EnumParticleTypes.SMOKE_NORMAL;
    switch(state.getValue(PHASE)) {
      case BTileEntity.PHASE_WARMUP:
        return;
      case BTileEntity.PHASE_HOT:
        if(rnd.nextInt(10) > 4) return;
        break;
      case BTileEntity.PHASE_MAGMABLOCK:
        if(rnd.nextInt(10) > 7) return;
        particle = EnumParticleTypes.SMOKE_LARGE;
        break;
      case BTileEntity.PHASE_LAVA:
        if(rnd.nextInt(10) > 2) return;
        particle = EnumParticleTypes.LAVA;
        break;
      default:
        return;
    }
    final double x=0.5+pos.getX(), y=0.5+pos.getY(), z=0.5+pos.getZ();
    final double xr=rnd.nextDouble()*0.4-0.2, yr=rnd.nextDouble()*0.5, zr=rnd.nextDouble()*0.4-0.2;
    world.spawnParticle(particle, x+xr, y+yr, z+zr, 0.0, 0.0, 0.0);
  }

  @Nullable
  private BTileEntity getTe(World world, BlockPos pos)
  { final TileEntity te=world.getTileEntity(pos); return (!(te instanceof BTileEntity)) ? (null) : ((BTileEntity)te); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable, ISidedInventory, IEnergyStorage
  {
    public static final int TICK_INTERVAL = 20;
    public static final int MAX_FLUID_LEVEL = 1000;
    public static final int MAX_BUCKET_EXTRACT_FLUID_LEVEL = 900;
    public static final int MAX_ENERGY_BUFFER = 32000;
    public static final int MAX_ENERGY_TRANSFER = 8192;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 92;
    public static final int DEFAULT_HEATUP_RATE = 2; // -> 50s for one smelting process
    public static final int PHASE_WARMUP = 0;
    public static final int PHASE_HOT = 1;
    public static final int PHASE_MAGMABLOCK = 2;
    public static final int PHASE_LAVA = 3;
    private static final ItemStack MAGMA_STACK = new ItemStack(Blocks.MAGMA);
    private static final ItemStack BUCKET_STACK = new ItemStack(Items.BUCKET);
    private static final ItemStack LAVA_BUCKET_STACK = new ItemStack(Items.LAVA_BUCKET);
    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;
    private static int heatup_rate = DEFAULT_HEATUP_RATE;
    private static int cooldown_rate = 1;
    private static Set<Item> accepted_minerals = new HashSet<Item>();
    private static Set<Item> accepted_lava_contrainers = new HashSet<Item>();

    private int tick_timer_;
    private int energy_stored_;
    private int progress_;
    private int fluid_level_;
    private boolean force_block_update_;
    private NonNullList<ItemStack> stacks_ = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);

    static {
      // Mineals: No Nether brick (made of Netherrack),
      // no glazed terracotta, no obsidian, no stairs, slabs etc.
      accepted_minerals.add(Item.getItemFromBlock(Blocks.COBBLESTONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.STONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.SANDSTONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.PRISMARINE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.END_STONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.MOSSY_COBBLESTONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.RED_SANDSTONE));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.QUARTZ_BLOCK));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.BRICK_BLOCK));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.END_BRICKS));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.HARDENED_CLAY));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.STAINED_HARDENED_CLAY));
      accepted_minerals.add(Item.getItemFromBlock(Blocks.STONEBRICK));
      // Lava containers
      accepted_lava_contrainers.add(Items.BUCKET);
    }

    public static void on_config(int energy_consumption, int heatup_per_second)
    {
      energy_consumption = MathHelper.clamp(energy_consumption, 32, 4096);
      heatup_rate = MathHelper.clamp(heatup_per_second, 1, 5);
      cooldown_rate = MathHelper.clamp(heatup_per_second/2, 1, 5);
      ModEngineersDecor.logger.info("Config mineal smelter energy consumption:" + energy_consumption + "rf/t, heat-up rate: " + heatup_rate + "%/s.");
    }

    public BTileEntity()
    { reset_process(); }

    public int progress()
    { return progress_; }

    public int phase()
    {
      if(progress_ >= 100) return PHASE_LAVA;
      if(progress_ >=  90) return PHASE_MAGMABLOCK;
      if(progress_ >=   5) return PHASE_HOT;
      return PHASE_WARMUP;
    }

    public int fluid_level()
    { return fluid_level_; }

    public int fluid_level_drain(int amount)
    { amount = MathHelper.clamp(amount, 0, fluid_level_); fluid_level_ -= amount; return amount; }

    public int comparator_signal()
    { return phase() * 5; } // -> 0..15

    private boolean accepts_lava_container(ItemStack stack)
    { return accepted_lava_contrainers.contains(stack.getItem()); }

    private boolean accepts_input(ItemStack stack)
    {
      if(!stacks_.get(0).isEmpty()) return false;
      if(fluid_level() > MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
        return accepts_lava_container(stack);
      } else {
        return accepted_minerals.contains(stack.getItem());
      }
    }

    public boolean insert(final ItemStack stack, boolean simulate)
    {
      if(stack.isEmpty() || (!accepts_input(stack))) return false;
      if(!simulate) {
        ItemStack st = stack.copy();
        st.setCount(st.getMaxStackSize());
        stacks_.set(0, st);
        if(!accepts_lava_container(stack)) progress_ = 0;
        force_block_update_ = true;
      }
      return true;
    }

    public ItemStack extract(boolean simulate)
    {
      ItemStack stack = stacks_.get(1).copy();
      if(stack.isEmpty()) return ItemStack.EMPTY;
      if(!simulate) reset_process();
      return stack;
    }

    protected void reset_process()
    {
      stacks_ = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);
      force_block_update_ = true;
      fluid_level_ = 0;
      tick_timer_ = 0;
      progress_ = 0;
    }

    public void readnbt(NBTTagCompound nbt)
    {
      energy_stored_ = nbt.getInteger("energy");
      progress_ = nbt.getInteger("progress");
      fluid_level_ = nbt.getInteger("fluidlevel");
      ItemStackHelper.loadAllItems(nbt, stacks_);
      if(stacks_.size() != 1) reset_process();
    }

    protected void writenbt(NBTTagCompound nbt)
    {
      nbt.setInteger("energy", MathHelper.clamp(energy_stored_,0 , MAX_ENERGY_BUFFER));
      nbt.setInteger("progress", MathHelper.clamp(progress_,0 , 100));
      nbt.setInteger("fluidlevel", MathHelper.clamp(fluid_level_,0 , MAX_FLUID_LEVEL));
      ItemStackHelper.saveAllItems(nbt, stacks_);
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorMineralSmelter)); }

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
    { return ((index >= 0) && (index < getSizeInventory())) ? stacks_.get(index) : ItemStack.EMPTY; }

    @Override
    public ItemStack decrStackSize(int index, int count)
    { return ItemStackHelper.getAndSplit(stacks_, index, count); }

    @Override
    public ItemStack removeStackFromSlot(int index)
    { return ItemStackHelper.getAndRemove(stacks_, index); }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    { if(stack.getCount()>getInventoryStackLimit()){stack.setCount(getInventoryStackLimit());} stacks_.set(index, stack); markDirty(); }

    @Override
    public int getInventoryStackLimit()
    { return 1; }

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
    { return ((index==0) && accepts_input(stack)) || (index==1); }

    @Override
    public int getField(int id)
    { return 0; }

    @Override
    public void setField(int id, int value)
    {}

    @Override
    public int getFieldCount()
    {  return 0; }

    @Override
    public void clear()
    { reset_process(); }

    // ISidedInventory ----------------------------------------------------------------------------

    private static final int[] SIDED_INV_SLOTS = new int[] {0,1};

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    { return SIDED_INV_SLOTS; }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction)
    { return (index==0) && isItemValidForSlot(index, stack); }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    { return (index==1) && (!stacks_.get(1).isEmpty()); }

    // IEnergyStorage ----------------------------------------------------------------------------

    @Override
    public boolean canExtract()
    { return false; }

    @Override
    public boolean canReceive()
    { return true; }

    @Override
    public int getMaxEnergyStored()
    { return MAX_ENERGY_BUFFER; }

    @Override
    public int getEnergyStored()
    { return energy_stored_; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    { return 0; }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
      if(energy_stored_ >= MAX_ENERGY_BUFFER) return 0;
      int n = Math.min(maxReceive, (MAX_ENERGY_BUFFER - energy_stored_));
      if(n > MAX_ENERGY_TRANSFER) n = MAX_ENERGY_TRANSFER;
      if(!simulate) {energy_stored_ += n; markDirty(); }
      return n;
    }

    // IItemHandler  --------------------------------------------------------------------------------

    protected static class BItemHandler implements IItemHandler
    {
      private BTileEntity te;

      BItemHandler(BTileEntity te)
      { this.te = te; }

      @Override
      public int getSlots()
      { return 2; }

      @Override
      public int getSlotLimit(int index)
      { return te.getInventoryStackLimit(); }

      @Override
      public boolean isItemValid(int slot, @Nonnull ItemStack stack)
      { return te.isItemValidForSlot(slot, stack); }

      @Override
      @Nonnull
      public ItemStack insertItem(int index, @Nonnull ItemStack stack, boolean simulate)
      {
        ItemStack rstack = stack.copy();
        if((index!=0) || (!te.insert(stack.copy(), simulate))) return rstack;
        rstack.shrink(1);
        return rstack;
      }

      @Override
      @Nonnull
      public ItemStack extractItem(int index, int amount, boolean simulate)
      { return (index!=1) ? ItemStack.EMPTY : te.extract(simulate); }

      @Override
      @Nonnull
      public ItemStack getStackInSlot(int index)
      { return te.getStackInSlot(index); }
    }

    private BItemHandler item_handler_ = new BItemHandler(this);

    // IFluidHandler  --------------------------------------------------------------------------------

    private static class BFluidHandler implements IFluidHandler, IFluidTankProperties
    {
      private final FluidStack lava;
      private final BTileEntity te;
      private final IFluidTankProperties[] props_ = {this};
      BFluidHandler(BTileEntity te) { this.te=te; lava = new FluidStack(FluidRegistry.LAVA, 1); }
      @Override @Nullable public FluidStack getContents() { return new FluidStack(lava, te.fluid_level()); }
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
      @Override public int getCapacity() { return 1000; }
      @Override public boolean canFill() { return false; }
      @Override public boolean canDrain() { return true; }
      @Override public boolean canFillFluidType(FluidStack fluidStack) { return false; }
      @Override public boolean canDrainFluidType(FluidStack fluidStack) { return fluidStack.isFluidEqual(lava); }

      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain)
      {
        if((te.fluid_level() <= 0) || (!resource.isFluidEqual(lava))) return null;
        FluidStack fs = getContents();
        if(doDrain) te.fluid_level_drain(fs.amount);
        return fs;
      }

      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain)
      {
        if(te.fluid_level() <= 0) return null;
        maxDrain = (doDrain) ? (te.fluid_level_drain(maxDrain)) : (Math.min(maxDrain, te.fluid_level()));
        return new FluidStack(FluidRegistry.LAVA, maxDrain);
      }
    }

    private final BFluidHandler fluid_handler_ = new BFluidHandler(this);

    // Capability export ----------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    { return ((cap==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
          || (cap==CapabilityEnergy.ENERGY)
          || (cap==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
          || (super.hasCapability(cap, facing))
      );
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
        return (T)item_handler_;
      } else if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
        return (T)fluid_handler_;
      } else if(capability == CapabilityEnergy.ENERGY) {
        return (T)this;
      } else {
        return super.getCapability(capability, facing);
      }
    }

    // ITickable ------------------------------------------------------------------------------------

    @Override
    public void update()
    {
      if(world.isRemote) return;
      if(--tick_timer_ > 0) return;
      tick_timer_ = TICK_INTERVAL;
      boolean dirty = false;
      final int last_phase = phase();
      final ItemStack istack = stacks_.get(0);
      if(istack.isEmpty() && (fluid_level()==0)) {
        progress_ = 0;
      } else if((energy_stored_ <= 0) || (world.isBlockPowered(pos))) {
        progress_ = MathHelper.clamp(progress_-cooldown_rate, 0,100);
      } else if(progress_ >= 100) {
        progress_ = 100;
        energy_stored_ = MathHelper.clamp(energy_stored_-((energy_consumption*TICK_INTERVAL)/20), 0, MAX_ENERGY_BUFFER);
      } else {
        energy_stored_ = MathHelper.clamp(energy_stored_-(energy_consumption*TICK_INTERVAL), 0, MAX_ENERGY_BUFFER);
        progress_ = MathHelper.clamp(progress_+heatup_rate, 0, 100);
      }
      int new_phase = phase();
      boolean is_lava_container = accepts_lava_container(istack);
      if(is_lava_container || (new_phase != last_phase)) {
        if(is_lava_container)  {
          // That stays in the slot until its extracted or somone takes it out.
          if(istack.isItemEqual(BUCKET_STACK)) {
            if(!stacks_.get(1).isItemEqual(LAVA_BUCKET_STACK)) {
              if(fluid_level() >= MAX_BUCKET_EXTRACT_FLUID_LEVEL) {
                stacks_.set(1, LAVA_BUCKET_STACK);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 0.2f, 1.3f);
              } else {
                stacks_.set(1, istack.copy());
              }
              dirty = true;
            }
          } else {
            stacks_.set(1, istack.copy());
            // Out stack -> Somehow the filled container or container with fluid+fluid_level().
          }
        } else if(new_phase > last_phase) {
          switch(new_phase) {
            case PHASE_LAVA:
              fluid_level_ = MAX_FLUID_LEVEL;
              stacks_.set(1, ItemStack.EMPTY);
              stacks_.set(0, ItemStack.EMPTY);
              world.playSound(null, pos, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2f, 1.0f);
              dirty = true;
              break;
            case PHASE_MAGMABLOCK:
              stacks_.set(1, MAGMA_STACK.copy());
              world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.2f, 0.8f);
              dirty = true;
              break;
            case PHASE_HOT:
              world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.2f, 0.8f);
              break;
          }
        } else {
          switch(new_phase) {
            case PHASE_MAGMABLOCK:
              stacks_.set(0, (fluid_level_ >= MAX_BUCKET_EXTRACT_FLUID_LEVEL) ? (MAGMA_STACK.copy()) : (ItemStack.EMPTY));
              stacks_.set(1, stacks_.get(0).copy());
              fluid_level_ = 0;
              world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.1f);
              dirty = true;
              break;
            case PHASE_HOT:
              if(istack.isItemEqual(MAGMA_STACK)) {
                stacks_.set(1, new ItemStack(Blocks.OBSIDIAN));
              } else {
                stacks_.set(1, new ItemStack(Blocks.COBBLESTONE));
              }
              world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.9f);
              dirty = true;
              break;
            case PHASE_WARMUP:
              world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 0.7f);
              break;
          }
        }
      }
      IBlockState state = world.getBlockState(pos);
      if((state.getBlock() instanceof BlockDecorMineralSmelter) && (force_block_update_ || (state.getValue(PHASE) != new_phase))) {
        state = state.withProperty(PHASE, new_phase);
        world.setBlockState(pos, state,3|16);
        world.notifyNeighborsOfStateChange(pos, state.getBlock(),false);
        force_block_update_ = false;
      }
      if(dirty) markDirty();
    }
  }

}
