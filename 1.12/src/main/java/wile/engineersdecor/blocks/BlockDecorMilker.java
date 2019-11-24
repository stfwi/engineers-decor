/*
 * @file BlockDecorMilker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Frequently attracts and milks nearby cows
 */
package wile.engineersdecor.blocks;

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
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import wile.engineersdecor.detail.ExtItems;
import wile.engineersdecor.detail.ItemHandling;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    final ItemStack out_stack = BTileEntity.milk_filled_container_item(in_stack);
    if(out_stack.isEmpty()) return FluidUtil.interactWithFluidHandler(player, hand, te.fluid_handler());
    boolean drained = false;
    IItemHandler player_inventory = new PlayerMainInvWrapper(player.inventory);
    if(te.fluid_level() >= BTileEntity.BUCKET_SIZE) {
      final ItemStack insert_stack = out_stack.copy();
      ItemStack remainder = ItemHandlerHelper.insertItemStacked(player_inventory, insert_stack, false);
      if(remainder.getCount() < insert_stack.getCount()) {
        te.drain(BTileEntity.BUCKET_SIZE);
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
    public static final int BUCKET_SIZE = 1000;
    public static final int TICK_INTERVAL = 80;
    public static final int PROCESSING_TICK_INTERVAL = 20;
    public static final int TANK_CAPACITY = BUCKET_SIZE * 12;
    public static final int MAX_MILKING_TANK_LEVEL = TANK_CAPACITY-500;
    public static final int FILLED_INDICATION_THRESHOLD = BUCKET_SIZE;
    public static final int MAX_ENERGY_BUFFER = 16000;
    public static final int MAX_ENERGY_TRANSFER = 512;
    public static final int DEFAULT_ENERGY_CONSUMPTION = 0;
    private static final EnumFacing FLUID_TRANSFER_DIRECTRIONS[] = {EnumFacing.DOWN,EnumFacing.EAST,EnumFacing.SOUTH,EnumFacing.WEST,EnumFacing.NORTH};
    private enum MilkingState { IDLE, PICKED, COMING, POSITIONING, MILKING, LEAVING, WAITING }

    private static FluidStack milk_fluid_ = new FluidStack(FluidRegistry.WATER, 0);
    private static HashMap<ItemStack, ItemStack> milk_containers_ = new HashMap<>();
    private static int energy_consumption = DEFAULT_ENERGY_CONSUMPTION;
    private int tick_timer_;
    private int energy_stored_;
    private int tank_level_ = 0;
    private UUID tracked_cow_ = null;
    private MilkingState state_ = MilkingState.IDLE;
    private int state_timeout_ = 0;
    private int state_timer_ = 0;
    private BlockPos tracked_cow_original_position_ = null;

    public static void on_config(int energy_consumption_per_tick)
    {
      energy_consumption = MathHelper.clamp(energy_consumption_per_tick, 0, 128);
      {
        Fluid milk = FluidRegistry.getFluid("milk");
        if(milk != null) milk_fluid_ = new FluidStack(milk, BUCKET_SIZE);
      }
      {
        milk_containers_.put(new ItemStack(Items.BUCKET), new ItemStack(Items.MILK_BUCKET));
        if(ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE!=null) milk_containers_.put(new ItemStack(Items.GLASS_BOTTLE), new ItemStack(ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE));
      }
      ModEngineersDecor.logger.info(
        "Config milker energy consumption:" + energy_consumption + "rf/t"
          + ((milk_fluid_==null)?"":" [milk fluid available]")
          + ((ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE==null)?"":" [bottledmilk mod available]")
      );
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
    { tank_level_ = MathHelper.clamp(tank_level_-BUCKET_SIZE, 0, TANK_CAPACITY); markDirty(); }

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
    {} // may be enabled with config, for dev was println

    private static ItemStack milk_filled_container_item(ItemStack stack)
    { return milk_containers_.entrySet().stream().filter(e->e.getKey().isItemEqual(stack)).map(Map.Entry::getValue).findFirst().orElse(ItemStack.EMPTY); }

    private void fill_adjacent_inventory_item_containers(EnumFacing block_facing)
    {
      // Check inventory existence, back to down is preferred, otherwise sort back into same inventory.
      IItemHandler src = ItemHandling.itemhandler(world, pos.offset(block_facing), block_facing.getOpposite());
      IItemHandler dst = ItemHandling.itemhandler(world, pos.down(), EnumFacing.UP);
      if(src==null) { src = dst; } else if(dst==null) { dst = src; }
      if((src==null) || (dst==null)) return;
      while((tank_level_ >= BUCKET_SIZE)) {
        boolean inserted = false;
        for(Entry<ItemStack,ItemStack> e:milk_containers_.entrySet()) {
          if(ItemHandling.extract(src, e.getKey(), 1, true).isEmpty()) continue;
          if(!ItemHandling.insert(dst, e.getValue().copy(), false).isEmpty()) continue;
          ItemHandling.extract(src, e.getKey(), 1, false);
          tank_level_ -= BUCKET_SIZE;
          inserted = true;
        }
        if(!inserted) break;
      }
    }

    private boolean milking_process()
    {
      if((tracked_cow_ == null) && (fluid_level() >= MAX_MILKING_TANK_LEVEL)) return false; // nothing to do
      final EnumFacing facing = world.getBlockState(getPos()).getValue(FACING).getOpposite();
      EntityCow cow = null;
      {
        AxisAlignedBB aabb = new AxisAlignedBB(pos.offset(facing, 3)).grow(4, 2, 4);
        final List<EntityCow> cows = world.getEntitiesWithinAABB(EntityCow.class, aabb,
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
      tick_timer_ = PROCESSING_TICK_INTERVAL;
      state_timer_ -= PROCESSING_TICK_INTERVAL;
      if(state_timer_ > 0) return false;
      switch(state_) { // Let's do this the old school FSA sequencing way ...
        case IDLE: {
          final List<EntityLivingBase> blocking_entities = world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos.offset(facing)).grow(0.5, 0.5, 0.5));
          if(blocking_entities.size() > 0) { tick_timer_ = TICK_INTERVAL; return false; } // an entity is blocking the way
          if(cow.getLeashed() || cow.isChild() || cow.isInLove() || (!cow.onGround) || cow.isBeingRidden() || cow.isSprinting()) return false;
          tracked_cow_ = cow.getPersistentID();
          state_ = MilkingState.PICKED;
          state_timeout_ = 200;
          tracked_cow_original_position_ = cow.getPosition();
          log("Idle: Picked cow" + tracked_cow_);
          return false;
        }
        case PICKED: {
          if(cow.hasPath()) return false;
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
            log("Picked: No path");
            tracked_cow_ = null;
            tick_timer_ = TICK_INTERVAL;
            return false;
          }
          state_ = MilkingState.COMING;
          state_timeout_ = 300; // 15s should be enough
          log("Picked: coming");
          return false;
        }
        case COMING: {
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          if(cow.getPosition().distanceSq(p) > 1) {
            if(cow.hasPath()) return false;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
              log("Coming: lost path");
              tracked_cow_ = null;
              tick_timer_ = TICK_INTERVAL;
              return false;
            } else {
              state_timeout_ -= 100;
            }
          } else {
            BlockPos next_p = getPos().offset(facing);
            if(!cow.getNavigator().tryMoveToXYZ(next_p.getX(), next_p.getY(), next_p.getZ(), 1.0)) {
              log("Coming: No path");
              tracked_cow_ = null;
              tick_timer_ = TICK_INTERVAL;
              return false;
            }
            log("Coming: position reached");
            state_ = MilkingState.POSITIONING;
            state_timeout_ = 100; // 5s
          }
          return false;
        }
        case POSITIONING: {
          BlockPos p = getPos().offset(facing);
          if(p.distanceSqToCenter(cow.posX, cow.posY, cow.posZ) > 0.45) {
            if(cow.hasPath()) return false;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0)) {
              log("Positioning: lost path");
              tick_timer_ = TICK_INTERVAL;
            } else {
              state_timeout_ -= 25;
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
          return false;
        }
        case MILKING: {
          tank_level_ = MathHelper.clamp(tank_level_+BUCKET_SIZE, 0, TANK_CAPACITY);
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
          tick_timer_ = TICK_INTERVAL;
          state_ = MilkingState.WAITING;
          log("Leaving: process done");
          return true;
        }
        case WAITING: {
          tick_timer_ = TICK_INTERVAL;
          return true; // wait for the timeout to kick in until starting with the next.
        }
        default: {
          tracked_cow_ = null;
        }
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
      if(energy_consumption > 0) {
        if(energy_stored_ <= 0) return;
        energy_stored_ = MathHelper.clamp(energy_stored_-energy_consumption, 0, MAX_ENERGY_BUFFER);
      }
      // Track and milk cows
      if(milking_process()) dirty = true;
      // Fluid transfer
      if((milk_fluid_.amount > 0) && (fluid_level() >= BUCKET_SIZE)) {
        for(EnumFacing facing: FLUID_TRANSFER_DIRECTRIONS) {
          IFluidHandler fh = FluidUtil.getFluidHandler(world, pos.offset(facing), facing.getOpposite());
          if(fh == null) continue;
          FluidStack fs = milk_fluid_.copy();
          fs.amount = BUCKET_SIZE;
          int nfilled = MathHelper.clamp(fh.fill(fs, true), 0, BUCKET_SIZE);
          if(nfilled <= 0) continue;
          tank_level_ -= nfilled;
          if(tank_level_ < 0) tank_level_ = 0;
          dirty = true;
          break;
        }
      }
      // Adjacent inventory update, only done just after milking to prevent waste of server cpu.
      if(dirty && (fluid_level() >= BUCKET_SIZE)) {
        fill_adjacent_inventory_item_containers(block_state.getValue(FACING));
      }
      // State update
      IBlockState new_state = block_state.withProperty(FILLED, fluid_level()>=FILLED_INDICATION_THRESHOLD).withProperty(ACTIVE, state_==MilkingState.MILKING);
      if(block_state != new_state) world.setBlockState(pos, new_state,1|2|16);
      if(dirty) markDirty();
    }
  }

}
