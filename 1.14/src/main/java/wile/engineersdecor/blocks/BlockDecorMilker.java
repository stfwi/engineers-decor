/*
 * @file BlockDecorMilker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Frequently attracts and milks nearby cows
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.detail.ExtItems;
import wile.engineersdecor.detail.ItemHandling;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.item.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;


public class BlockDecorMilker extends BlockDecorDirectedHorizontal
{
  public static final BooleanProperty FILLED = BooleanProperty.create("filled");
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

  public BlockDecorMilker(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
  { super(config, builder, unrotatedAABB); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(ACTIVE); builder.add(FILLED); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  { return super.getStateForPlacement(context).with(FILLED, false).with(ACTIVE, false); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(BlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(BlockState state, World world, BlockPos pos)
  {
    BTileEntity te = getTe(world, pos);
    return (te==null) ? 0 : MathHelper.clamp((16 * te.fluid_level())/BTileEntity.TANK_CAPACITY, 0, 15);
  }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  @Nullable
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new BTileEntity(); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    if(world.isRemote) return true;
    BTileEntity te = getTe(world, pos);
    if(te==null) return true;
    final ItemStack in_stack = player.getHeldItem(hand);
    final ItemStack out_stack = BTileEntity.milk_filled_container_item(in_stack);
    if(out_stack.isEmpty() && (te.fluid_handler()!=null)) return FluidUtil.interactWithFluidHandler(player, hand, te.fluid_handler());
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
          final ItemEntity ei = new ItemEntity(world, player.posX, player.posY + 0.5, player.posZ, remainder);
          ei.setPickupDelay(40);
          ei.setMotion(0,0,0);
          world.addEntity(ei);
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

  public static class BTileEntity extends TileEntity implements ITickableTileEntity, IEnergyStorage, ICapabilityProvider
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
    private static final Direction FLUID_TRANSFER_DIRECTRIONS[] = {Direction.DOWN,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH};
    private enum MilkingState { IDLE, PICKED, COMING, POSITIONING, MILKING, LEAVING, WAITING }

    private static FluidStack milk_fluid_ = new FluidStack(Fluids.WATER, 0);
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
        Fluid milk = null; // FluidRe.getFluid("milk");
        if(milk != null) milk_fluid_ = new FluidStack(milk, BUCKET_SIZE);
      }
      {
        milk_containers_.put(new ItemStack(Items.BUCKET), new ItemStack(Items.MILK_BUCKET));
        if(ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE!=null) milk_containers_.put(new ItemStack(Items.GLASS_BOTTLE), new ItemStack(ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE));
      }
      ModEngineersDecor.logger().info(
        "Config milker energy consumption:" + energy_consumption + "rf/t"
          + ((milk_fluid_==null)?"":" [milk fluid available]")
          + ((ExtItems.BOTTLED_MILK_BOTTLE_DRINKLABLE==null)?"":" [bottledmilk mod available]")
      );
    }

    public BTileEntity()
    { this(ModContent.TET_SMALL_MILKING_MACHINE); }

    public BTileEntity(TileEntityType<?> te_type)
    { super(te_type); reset(); }

    public void reset()
    {
      tank_level_ = 0;
      energy_stored_ = 0;
      tick_timer_ = 0;
      tracked_cow_ = null;
      state_ = MilkingState.IDLE;
      state_timeout_ = 0;
    }

    public CompoundNBT destroy_getnbt()
    {
      final UUID cowuid = tracked_cow_;
      CompoundNBT nbt = new CompoundNBT();
      writenbt(nbt, false); reset();
      if(cowuid == null) return nbt;
      world.getEntitiesWithinAABB(CowEntity.class, new AxisAlignedBB(pos).grow(16, 16, 16), e->e.getUniqueID().equals(cowuid)).forEach(e->e.setNoAI(false));
      return nbt;
    }

    public void readnbt(CompoundNBT nbt, boolean update_packet)
    {
      tank_level_ = nbt.getInt("tank");
      energy_stored_ = nbt.getInt("energy");
    }

    protected void writenbt(CompoundNBT nbt, boolean update_packet)
    {
      if(tank_level_ > 0) nbt.putInt("tank", tank_level_);
      if(energy_stored_ > 0) nbt.putInt("energy", energy_stored_ );
    }

    private IFluidHandler fluid_handler()
    { return fluid_handler_.orElse(null); }

    private int fluid_level()
    { return MathHelper.clamp(tank_level_, 0, TANK_CAPACITY); }

    private void drain(int amount)
    { tank_level_ = MathHelper.clamp(tank_level_-BUCKET_SIZE, 0, TANK_CAPACITY); markDirty(); }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt, false); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt, false); return nbt; }

    // IEnergyStorage ----------------------------------------------------------------------------

    protected LazyOptional<IEnergyStorage> energy_handler_ = LazyOptional.of(() -> (IEnergyStorage)this);

    @Override public boolean canExtract()     { return false; }
    @Override public boolean canReceive()     { return true; }
    @Override public int getMaxEnergyStored() { return MAX_ENERGY_BUFFER; }
    @Override public int getEnergyStored()    { return energy_stored_; }
    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }

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

    private LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> (IFluidHandler)new BFluidHandler(this));

    private static class BFluidHandler implements IFluidHandler
    {
      private final BTileEntity te;
      BFluidHandler(BTileEntity te) { this.te = te; }
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return new FluidStack(milk_fluid_, te.fluid_level()); }
      @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return false; }
      @Override public int fill(FluidStack resource, FluidAction action)  { return 0; }

      @Override
      public FluidStack drain(FluidStack resource, FluidAction action)
      { return (!resource.isFluidEqual(milk_fluid_)) ? (FluidStack.EMPTY.copy()) : drain(resource.getAmount(), action); }

      @Override
      public FluidStack drain(int maxDrain, FluidAction action)
      {
        if(te.fluid_level() <= 0) return FluidStack.EMPTY.copy();
        FluidStack fs = milk_fluid_.copy();
        fs.setAmount(Math.min(fs.getAmount(), te.fluid_level()));
        if(action==FluidAction.EXECUTE) te.tank_level_ -= fs.getAmount();
        return fs;
      }
    }

    // ICapabilityProvider ---------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(!this.removed && (facing != null)) {
        if((capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) && (!milk_fluid_.isEmpty())) {
          return fluid_handler_.cast();
        } else if((capability == CapabilityEnergy.ENERGY) && (energy_consumption>0)) {
          return energy_handler_.cast();
        }
      }
      return super.getCapability(capability, facing);
    }

    // ITickable ------------------------------------------------------------------------------------

    private void log(String s)
    {
      // System.out.println("Milker|" + s);
    } // may be enabled with config, for dev was println

    private static ItemStack milk_filled_container_item(ItemStack stack)
    { return milk_containers_.entrySet().stream().filter(e->e.getKey().isItemEqual(stack)).map(Map.Entry::getValue).findFirst().orElse(ItemStack.EMPTY); }

    private void fill_adjacent_inventory_item_containers(Direction block_facing)
    {
      // Check inventory existence, back to down is preferred, otherwise sort back into same inventory.
      IItemHandler src = ItemHandling.itemhandler(world, pos.offset(block_facing), block_facing.getOpposite());
      IItemHandler dst = ItemHandling.itemhandler(world, pos.down(), Direction.UP);
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

    private void release_cow(CowEntity cow)
    {
      if(cow!=null) {
        cow.setNoAI(false);
        cow.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
      }
      tracked_cow_ = null;
      state_ = MilkingState.IDLE;
      tick_timer_ = TICK_INTERVAL;
    }

    private boolean milking_process()
    {
      if((tracked_cow_ == null) && (fluid_level() >= MAX_MILKING_TANK_LEVEL)) return false; // nothing to do
      final Direction facing = world.getBlockState(getPos()).get(HORIZONTAL_FACING).getOpposite();
      CowEntity cow = null;
      {
        AxisAlignedBB aabb = new AxisAlignedBB(pos.offset(facing, 3)).grow(4, 2, 4);
        final List<CowEntity> cows = world.getEntitiesWithinAABB(CowEntity.class, aabb,
          e->( ((tracked_cow_==null) && ((!e.isChild()) && (!e.isInLove()) && (!e.isBeingRidden()))) || (e.getUniqueID().equals(tracked_cow_)) )
        );
        if(cows.size() == 1) {
          cow = cows.get(0); // tracked or only one
        } else if(cows.size() > 1) {
          cow = cows.get(world.rand.nextInt(cows.size()-1)); // pick one
        }
      }
      if((state_ != MilkingState.IDLE) && ((state_timeout_ -= PROCESSING_TICK_INTERVAL) <= 0)) { log("Cow motion timeout"); cow = null; }
      if((cow == null) || (!cow.isAlive())) { release_cow(cow); cow = null; }
      if(tracked_cow_ == null) state_ = MilkingState.IDLE;
      if(cow == null) { log("Init: No cow"); return false; } // retry next cycle
      tick_timer_ = PROCESSING_TICK_INTERVAL;
      state_timer_ -= PROCESSING_TICK_INTERVAL;

      if(cow.getNavigator().noPath()) {
        BlockPos p = getPos().offset(facing,2);
        cow.getNavigator().tryMoveToXYZ(p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 1);
      }
      if(state_timer_ > 0) return false;
      switch(state_) { // Let's do this the old school FSA sequencing way ...
        case IDLE: {
          final List<LivingEntity> blocking_entities = world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(pos.offset(facing)).grow(0.5, 0.5, 0.5));
          if(blocking_entities.size() > 0) {
            tick_timer_ = TICK_INTERVAL;
            log("Idle: Position blocked");
            if(blocking_entities.get(0) instanceof CowEntity) {
              CowEntity blocker = (CowEntity)blocking_entities.get(0);
              BlockPos p = getPos().offset(facing,2);
              log("Idle: Shove off");
              blocker.setNoAI(false);
              blocker.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
              blocker.getNavigator().tryMoveToXYZ(p.getX()+0.5, p.getY()+0.5, p.getZ()+0.5, 1);
            }
            return false;
          }
          if(cow.getLeashed() || cow.isChild() || cow.isInLove() || (!cow.onGround) || cow.isBeingRidden() || cow.isSprinting()) return false;
          tracked_cow_ = cow.getUniqueID();
          state_ = MilkingState.PICKED;
          state_timeout_ = 200;
          tracked_cow_original_position_ = cow.getPosition();
          log("Idle: Picked cow " + tracked_cow_);
          return false;
        }
        case PICKED: {
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          cow.getNavigator().clearPath();
          if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
            log("Picked: No path");
            cow.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
            tracked_cow_ = null;
            tick_timer_ = TICK_INTERVAL;
            return false;
          }
          state_ = MilkingState.COMING;
          state_timeout_ = 300; // 15s should be enough
          log("Picked: coming to " +p);
          return false;
        }
        case COMING: {
          BlockPos p = getPos().offset(facing).offset(facing.rotateY());
          if(cow.getPosition().distanceSq(p) > 1) {
            if(cow.getNavigator().getTargetPos().equals(p) && (!cow.getNavigator().noPath())) return false;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(),1.0)) {
              log("Coming: lost path");
              cow.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
              tracked_cow_ = null;
              tick_timer_ = TICK_INTERVAL;
              return false;
            } else {
              log("Coming: Re-init path");
              state_timeout_ -= 100;
            }
          } else {
            BlockPos next_p = getPos().offset(facing);
            if(!cow.getNavigator().tryMoveToXYZ(next_p.getX(), next_p.getY(), next_p.getZ(), 1.0)) {
              log("Coming: No path");
              tracked_cow_ = null;
              cow.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
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
          if(p.distanceSq(cow.posX, cow.posY, cow.posZ, true) > 0.45) {
            if(cow.getNavigator().getTargetPos().equals(p) && (!cow.getNavigator().noPath())) return false;
            if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0)) {
              log("Positioning: lost path");
              tick_timer_ = TICK_INTERVAL;
              cow.goalSelector.getRunningGoals().forEach(PrioritizedGoal::resetTask);
            } else {
              log("Positioning: Re-init path");
              state_timeout_ -= 25;
            }
            tracked_cow_ = null;
            return false;
          }
          cow.setNoAI(true);
          cow.move(MoverType.SELF, new Vec3d(p.getX()+0.5-cow.posX, 0,p.getZ()+0.5-cow.posZ));
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
          cow.setNoAI(false);
          cow.getNavigator().clearPath();
          log("Milking: done, leave");
          return true;
        }
        case LEAVING: {
          BlockPos p = (tracked_cow_original_position_ != null) ? (tracked_cow_original_position_) : getPos().offset(facing,2).offset(facing.rotateYCCW());
          if(!cow.getNavigator().tryMoveToXYZ(p.getX(), p.getY(), p.getZ(), 1.0)) cow.getNavigator().clearPath();
          state_timeout_ = 600;
          state_timer_ = 500;
          tick_timer_ = TICK_INTERVAL;
          state_ = MilkingState.WAITING;
          log("Leaving: process done");
          return true;
        }
        case WAITING: {
          tick_timer_ = TICK_INTERVAL;
          log("Waiting: ...");
          return true; // wait for the timeout to kick in until starting with the next.
        }
        default: {
          tracked_cow_ = null;
        }
      }
      return (tracked_cow_ != null);
    }

    @Override
    public void tick()
    {
      if((world.isRemote) || ((--tick_timer_ > 0))) return;
      tick_timer_ = TICK_INTERVAL;
      boolean dirty = false;
      final BlockState block_state = world.getBlockState(pos);
      if(!world.isBlockPowered(pos) && (state_==MilkingState.IDLE)) {
        log("Cycle");
        if(energy_consumption > 0) {
          if(energy_stored_ <= 0) return;
          energy_stored_ = MathHelper.clamp(energy_stored_-energy_consumption, 0, MAX_ENERGY_BUFFER);
        }
        // Track and milk cows
        if(milking_process()) dirty = true;
        // Fluid transfer
        if((milk_fluid_.getAmount() > 0) && (fluid_level() >= BUCKET_SIZE)) {
          log("Fluid transfer");
          for(Direction facing: FLUID_TRANSFER_DIRECTRIONS) {
            IFluidHandler fh = FluidUtil.getFluidHandler(world, pos.offset(facing), facing.getOpposite()).orElse(null);
            if(fh == null) continue;
            FluidStack fs = milk_fluid_.copy();
            fs.setAmount(BUCKET_SIZE);
            int nfilled = MathHelper.clamp(fh.fill(fs, FluidAction.EXECUTE), 0, BUCKET_SIZE);
            if(nfilled <= 0) continue;
            tank_level_ -= nfilled;
            if(tank_level_ < 0) tank_level_ = 0;
            dirty = true;
            break;
          }
        }
        // Adjacent inventory update, only done just after milking to prevent waste of server cpu.
        if(dirty && (fluid_level() >= BUCKET_SIZE)) {
          log("Try item transfer");
          fill_adjacent_inventory_item_containers(block_state.get(HORIZONTAL_FACING));
        }
      }
      // State update
      BlockState new_state = block_state.with(FILLED, fluid_level()>=FILLED_INDICATION_THRESHOLD).with(ACTIVE, state_==MilkingState.MILKING);
      if(block_state != new_state) world.setBlockState(pos, new_state,1|2|16);
      if(dirty) markDirty();
    }
  }

}
