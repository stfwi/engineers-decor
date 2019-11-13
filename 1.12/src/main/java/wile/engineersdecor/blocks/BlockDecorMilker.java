/*
 * @file BlockDecorMilker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Frequently attracts and milks nearby cows
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.BlockChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityChest;
import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.SoundEvents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.*;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import wile.engineersdecor.detail.ExtItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;


public class BlockDecorMilker extends BlockDecorDirectedHorizontal
{
  public static final PropertyBool FILLED = PropertyBool.create("filled");
  public static final PropertyBool ACTIVE = PropertyBool.create("active");


  public BlockDecorMilker(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FACING, FILLED, ACTIVE); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return super.getStateFromMeta(meta).withProperty(FILLED, ((meta & 0x4)!=0)).withProperty(ACTIVE, ((meta & 0x8)!=0)) ; }

  @Override
  public int getMetaFromState(IBlockState state)
  { return super.getMetaFromState(state) | (state.getValue(FILLED)?0x4:0x00) | (state.getValue(ACTIVE)?0x8:0x00); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FILLED, false).withProperty(ACTIVE, false); }

  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face)
  { return BlockFaceShape.UNDEFINED; }

  @Override
  public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(IBlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
  {
    BTileEntity te = getTe(world, pos);
    return (te==null) ? 0 : MathHelper.clamp((16 * te.fluid_level())/BTileEntity.TANK_CAPACITY, 0, 15);
  }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorMilker.BTileEntity(); }

  @Override
  public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
  {
    if(world.isRemote) return true;
    BTileEntity te = getTe(world, pos);
    if(te==null) return super.removedByPlayer(state, world, pos, player, willHarvest);
    final NBTTagCompound te_nbt = te.destroy_getnbt();
    ItemStack stack = new ItemStack(this, 1);
    if((te_nbt!=null) && !te_nbt.isEmpty()) {
      NBTTagCompound nbt = stack.getTagCompound();
      if(nbt == null) nbt = new NBTTagCompound();
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
  { super.onBlockExploded(world, pos, explosion); } // currently nothing to do here

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    BTileEntity te = getTe(world, pos);
    if(te==null) return true;
    final ItemStack in_stack = player.getHeldItem(hand);
    final ItemStack out_stack = te.milk_filled_container_item(in_stack);
    if(out_stack.isEmpty()) return FluidUtil.interactWithFluidHandler(player, hand, te.fluid_handler());
    boolean drained = false;
    IItemHandler player_inventory = new PlayerMainInvWrapper(player.inventory);
    if(te.fluid_level() >= 1000) {
      final ItemStack insert_stack = out_stack.copy();
      ItemStack remainder = ItemHandlerHelper.insertItemStacked(player_inventory, insert_stack, false);
      if(remainder.getCount() < insert_stack.getCount()) {
        te.drain(1000);
        in_stack.shrink(1);
        drained = true;
        if(remainder.getCount() > 0) {
          final EntityItem ei = new EntityItem(world, player.posX, player.posY + 0.5, player.posZ, remainder);
          ei.setPickupDelay(40);
          ei.motionX = 0;
          ei.motionZ = 0;
          world.spawnEntity(ei);
        }
      }
    }
    if(drained) {
      world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.8f, 1f);
    }
    return true;
  }

  @Nullable
  private BTileEntity getTe(World world, BlockPos pos)
  { final TileEntity te=world.getTileEntity(pos); return (!(te instanceof BTileEntity)) ? (null) : ((BTileEntity)te); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements ITickable, ICapabilityProvider, IEnergyStorage
  {
    public static final int TICK_INTERVAL = 80;
    public static final int PROCESSING_TICK_INTERVAL = 10;
    public static final int TANK_CAPACITY = 12000;
    public static final int MAX_MILKING_TANK_LEVEL = TANK_CAPACITY-500;
    public static final int MAX_ENERGY_BUFFER = 16000;
    public static final int MAX_ENERGY_TRANSFER = 512;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 16;
    private static final EnumFacing FLUID_TRANSFER_DIRECTRIONS[] = {EnumFacing.DOWN,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST,EnumFacing.NORTH};
    private static final ItemStack BUCKET_STACK = new ItemStack(Items.BUCKET);
    private enum MilkingState { IDLE, PICKED, COMING, POSITIONING, MILKING, LEAVING, WAITING }

    private static FluidStack milk_fluid_ = new FluidStack(FluidRegistry.WATER, 0);
    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;
    private int tick_timer_;
    private int energy_stored_;
    private int tank_level_ = 0;
    private UUID tracked_cow_ = null;
    private MilkingState state_ = MilkingState.IDLE;
    private int state_timeout_ = 0;
    private int state_timer_ = 0;
    private BlockPos tracked_cow_original_position_ = null;

    public static void on_config(int energy_consumption_per_tick, int heatup_per_second)
    {
      energy_consumption = MathHelper.clamp(energy_consumption_per_tick, 0, 4096);
      ModEngineersDecor.logger.info("Config milker energy consumption:" + energy_consumption + "rf/t");
    }

    public BTileEntity()
    { reset(); }

    public void reset()
    {
      tank_level_ = 0;
      energy_stored_ = 0;
      tick_timer_ = 0;
      tracked_cow_ = null;
      state_ = MilkingState.IDLE;
      state_timeout_ = 0;
    }

    public NBTTagCompound destroy_getnbt()
    {
      final UUID cowuid = tracked_cow_;
      NBTTagCompound nbt = new NBTTagCompound();
      writenbt(nbt, false); reset();
      if(cowuid == null) return nbt;
      world.getEntitiesWithinAABB(EntityCow.class, new AxisAlignedBB(pos).grow(16, 16, 16), e->e.getPersistentID().equals(cowuid)).forEach(e->e.setNoAI(false));
      return nbt;
    }

    public void readnbt(NBTTagCompound nbt, boolean update_packet)
    {
      tank_level_ = nbt.getInteger("tank");
      energy_stored_ = nbt.getInteger("energy");
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      if(tank_level_ > 0) nbt.setInteger("tank", tank_level_);
      if(energy_stored_ > 0) nbt.setInteger("energy", energy_stored_ );
    }

    private IFluidHandler fluid_handler()
    { return fluid_handler_; }

    private int fluid_level()
    { return MathHelper.clamp(tank_level_, 0, TANK_CAPACITY); }

    private void drain(int amount)
    { tank_level_ = MathHelper.clamp(tank_level_-1000, 0, TANK_CAPACITY); markDirty(); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    { return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorMilker)); }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    { super.readFromNBT(nbt); readnbt(nbt, false); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    { super.writeToNBT(nbt); writenbt(nbt, false); return nbt; }

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

    // IFluidHandler / IFluidTankProperties ---------------------------------------------------------------------

    private static class BFluidHandler implements IFluidHandler, IFluidTankProperties
    {
      private final BTileEntity te;
      private final IFluidTankProperties[] props_ = {this};
      BFluidHandler(BTileEntity te) { this.te=te; }
      @Override @Nullable public FluidStack getContents() { return new FluidStack(milk_fluid_, te.fluid_level()); }
      @Override public IFluidTankProperties[] getTankProperties() { return props_; }
      @Override public int fill(FluidStack resource, boolean doFill) { return 0; }
      @Override public int getCapacity() { return TANK_CAPACITY; }
      @Override public boolean canFill() { return false; }
      @Override public boolean canDrain() { return (milk_fluid_.amount > 0); }
      @Override public boolean canFillFluidType(FluidStack fs) { return false; }
      @Override public boolean canDrainFluidType(FluidStack fs) { return fs.isFluidEqual(milk_fluid_); }

      @Override @Nullable public FluidStack drain(FluidStack resource, boolean doDrain)
      { return (!resource.isFluidEqual(milk_fluid_)) ? (null) : drain(resource.amount, doDrain); }

      @Override @Nullable public FluidStack drain(int maxDrain, boolean doDrain)
      {
        if(te.fluid_level() <= 0) return null;
        FluidStack fs = milk_fluid_.copy();
        fs.amount = Math.min(fs.amount, te.fluid_level());
        if(doDrain) te.tank_level_ -= fs.amount;
        return fs;
      }
    }

    private final BFluidHandler fluid_handler_ = new BFluidHandler(this);

    // ICapabilityProvider ---------------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing facing)
    {
      if((cap==CapabilityEnergy.ENERGY) && (energy_consumption>0)) return true;
      if((cap==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)  && (milk_fluid_.amount>0)) return true;
      return super.hasCapability(cap, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
        return (T)fluid_handler_;
      } else if(capability == CapabilityEnergy.ENERGY) {
        return (T)this;
      } else {
        return super.getCapability(capability, facing);
      }
    }

    // ITickable ------------------------------------------------------------------------------------

    private void log(String s)
    {} // may be enabled with config

    private ItemStack milk_filled_container_item(ItemStack stack)
    {
      // Returns out stack for input stack size == 1 (convert one item of the stack).
      if(stack.isItemEqualIgnoreDurability(BTileEntity.BUCKET_STACK)) return new ItemStack(Items.MILK_BUCKET);
      if((ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE!=null) && stack.getItem().equals(Items.GLASS_BOTTLE)) return new ItemStack(ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE);
      return ItemStack.EMPTY;
    }

    private void fill_adjacent_inventory_item_containers(EnumFacing block_facing)
    {
      // Check inventory existence, back to down if possible, otherwise sort back into same inventory.
      IInventory src, dst;
      {
        TileEntity te_src = world.getTileEntity(pos.offset(block_facing));
        TileEntity te_dst = world.getTileEntity(pos.down());
        if(!(te_src instanceof IInventory)) te_src = null;
        if(!(te_dst instanceof IInventory)) te_dst = null;
        if((te_src==null)) te_src = te_dst;
        if((te_dst==null)) te_dst = te_src;
        if((te_src==null) || (te_dst==null)) return;
        src = (IInventory)te_src;
        dst = (IInventory)te_dst;
      }

      /// @todo --> hier weitermachen
    }

    private boolean milking_process()
    {
      if((tracked_cow_ == null) && (fluid_level() >= MAX_MILKING_TANK_LEVEL)) return false; // nothing to do
      EntityCow cow = null;
      {
        final List<EntityCow> cows = world.getEntitiesWithinAABB(EntityCow.class, new AxisAlignedBB(pos).grow(16, 3, 16),
          e->(((tracked_cow_==null) && (!e.isChild() && !e.isInLove()))||(e.getPersistentID().equals(tracked_cow_)))
        );
        if(cows.size() == 1) {
          cow = cows.get(0); // tracked or only one
        } else if(cows.size() > 1) {
          cow = cows.get(world.rand.nextInt(cows.size()-1)); // pick one
        }
      }
      if((state_ != MilkingState.IDLE) && ((state_timeout_ -= PROCESSING_TICK_INTERVAL) <= 0)) { log("Cow motion timeout"); cow = null; }
      if((cow == null) || (cow.isDead) || ((tracked_cow_ != null) && (!tracked_cow_.equals(cow.getPersistentID())))) { tracked_cow_ = null; cow = null; }
      if(tracked_cow_ == null) state_ = MilkingState.IDLE;
      if(cow == null) return false; // retry next cycle
      final EnumFacing facing = world.getBlockState(getPos()).getValue(FACING).getOpposite();
      tick_timer_ = PROCESSING_TICK_INTERVAL;
      state_timer_ -= PROCESSING_TICK_INTERVAL;
      if(state_timer_ > 0) return false;
      switch(state_) {
        case IDLE: {
          final List<EntityLivingBase> blocking_entities = world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos).grow(1, 2, 1));
          if(blocking_entities.size() > 0) return false; // an entity is blocking the way
          if(cow.getLeashed() || cow.isChild() || cow.isInLove() || (!cow.onGround) || cow.isBeingRidden() || cow.isSprinting()) return false;
          tracked_cow_ = cow.getPersistentID();
          state_ = MilkingState.PICKED;
          state_timeout_ = 200;
          tracked_cow_original_position_ = cow.getPosition();
          log("Idle: Picked cow" + tracked_cow_);
          return true;
        }
        case PICKED: {
          if(cow.hasPath()) return true;
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
            log("Picked: No path");
            tracked_cow_ = null;
            return false;
          }
          state_ = MilkingState.COMING;
          state_timeout_ = 300; // 15s should be enough
          log("Picked: coming");
          return true;
        }
        case COMING: {
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          if(cow.getPosition().distanceSq(p) > 1) {
            if(cow.hasPath()) return true;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
              log("Coming: lost path");
              tracked_cow_ = null;
              return false;
            } else {
              state_timeout_ -= 100;
            }
          } else {
            BlockPos next_p = getPos().offset(facing);
            if(!cow.getNavigator().tryMoveToXYZ(next_p.getX(), next_p.getY(), next_p.getZ(), 1.0)) {
              log("Coming: No path");
              tracked_cow_ = null;
              return false;
            }
            log("Coming: position reached");
            state_ = MilkingState.POSITIONING;
            state_timeout_ = 100; // 5s
          }
          return true;
        }
        case POSITIONING: {
          BlockPos p = getPos().offset(facing);
          if(cow.getPosition().distanceSq(p) > 0) {
            if(cow.hasPath()) return true;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0)) {
              log("Positioning: lost path");
            } else {
              state_timeout_ -= 200;
            }
            tracked_cow_ = null;
            return false;
          }
          cow.setNoAI(true);
          cow.move(MoverType.SELF, p.getX()+0.5-cow.posX, 0,p.getZ()+0.5-cow.posZ);
          world.playSound(null, pos, SoundEvents.ENTITY_COW_MILK, SoundCategory.BLOCKS, 0.5f, 1f);
          state_timeout_ = 600;
          state_ = MilkingState.MILKING;
          state_timer_ = 30;
          log("Positioning: start milking");
          return true;
        }
        case MILKING: {
          tank_level_ = MathHelper.clamp(tank_level_+1000, 0, TANK_CAPACITY);
          state_timeout_ = 600;
          state_ = MilkingState.LEAVING;
          state_timer_ = 20;
          BlockPos p = (tracked_cow_original_position_ != null) ? (tracked_cow_original_position_) : getPos().offset(facing.rotateYCCW(),2);
          cow.setNoAI(false);
          cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0);
          log("Milking: done, leave");
          return true;
        }
        case LEAVING: {
          BlockPos p = (tracked_cow_original_position_ != null) ? (tracked_cow_original_position_) : getPos().offset(facing.rotateYCCW(),2);
          cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0);
          state_timeout_ = 600;
          state_timer_ = 500;
          state_ = MilkingState.WAITING;
          log("Leaving: process done");
          return true;
        }
        case WAITING: {
          return true; // wait for the timeout to kick in until starting with the next.
        }
        default:
          tracked_cow_ = null;
      }
      return (tracked_cow_ != null);
    }

    @Override
    public void update()
    {
      if((world.isRemote) || ((--tick_timer_ > 0))) return;
      tick_timer_ = TICK_INTERVAL;
      final IBlockState block_state = world.getBlockState(pos);
      boolean dirty = false;
      // Track and milk cows
      if(milking_process()) dirty = true;
      // Fluid transfer
      if((milk_fluid_.amount > 0) && (fluid_level() >= 1000)) {
        for(EnumFacing facing: FLUID_TRANSFER_DIRECTRIONS) {
          IFluidHandler fh = FluidUtil.getFluidHandler(world, pos.offset(facing), facing.getOpposite());
          if(fh == null) continue;
          FluidStack fs = milk_fluid_.copy();
          fs.amount = 1000;
          int nfilled = MathHelper.clamp(fh.fill(fs, true), 0, 1000);
          if(nfilled <= 0) continue;
          tank_level_ -= nfilled;
          if(tank_level_ < 0) tank_level_ = 0;
          dirty = true;
          break;
        }
      }
      // Adjacent inventory update, only done just after milking to prevent waste of server cpu.
      if(dirty && (fluid_level() >= 1000)) {
        fill_adjacent_inventory_item_containers(block_state.getValue(FACING));
      }
      // State update
      IBlockState new_state = block_state.withProperty(FILLED, fluid_level()>0).withProperty(ACTIVE, state_==MilkingState.MILKING);
      if(block_state != new_state) world.setBlockState(pos, new_state,1|2|16);
      if(dirty) markDirty();
    }
  }

}
