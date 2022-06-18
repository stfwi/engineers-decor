/*
 * @file EdMilker.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Frequently attracts and milks nearby cows
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardEntityBlocks;
import wile.engineersdecor.libmc.detail.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;


public class EdMilker
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
  public static final int DEFAULT_MILKING_DELAY_PER_COW = 4000;
  private static final FluidStack NO_MILK_FLUID = new FluidStack(Fluids.WATER, 1000);

  private static FluidStack milk_fluid_ = NO_MILK_FLUID;
  private static final HashMap<ItemStack, ItemStack> milk_containers_ = new HashMap<>();
  private static int energy_consumption_ = DEFAULT_ENERGY_CONSUMPTION;
  private static long min_milking_delay_per_cow_ticks_ = DEFAULT_MILKING_DELAY_PER_COW;

  public static void on_config(int energy_consumption_per_tick, int min_milking_delay_per_cow)
  {
    energy_consumption_ = Mth.clamp(energy_consumption_per_tick, 0, 1024);
    min_milking_delay_per_cow_ticks_ = Mth.clamp(min_milking_delay_per_cow, 1000, 24000);
    {
      ResourceLocation milk_rl = ForgeRegistries.FLUIDS.getKeys().stream().filter(rl->rl.getPath().equals("milk")).findFirst().orElse(null);
      if(milk_rl != null) {
        Fluid milk = ForgeRegistries.FLUIDS.getValue(milk_rl);
        if(milk != null) milk_fluid_ = new FluidStack(milk, BUCKET_SIZE);
      }
    }
    {
      milk_containers_.put(new ItemStack(Items.BUCKET), new ItemStack(Items.MILK_BUCKET));
    }
    ModConfig.log(
      "Config milker: energy consumption:" + energy_consumption_ + "rf/t"
        + ((milk_fluid_==NO_MILK_FLUID)?"[no milk fluid registered]":" [milk fluid available]")
    );
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class MilkerBlock extends StandardBlocks.Horizontal implements StandardEntityBlocks.IStandardEntityBlock<MilkerTileEntity>
  {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MilkerBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABBs)
    {
      super(config, builder, unrotatedAABBs);
      cshapes.replaceAll((state,shape)->Shapes.create(Auxiliaries.getPixeledAABB(0,0,0, 16,24,16)));
    }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(ACTIVE); builder.add(FILLED); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    { return super.getStateForPlacement(context).setValue(FILLED, false).setValue(ACTIVE, false); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos)
    {
      MilkerTileEntity te = getTe(world, pos);
      return (te==null) ? 0 : Mth.clamp((16 * te.fluid_level())/TANK_CAPACITY, 0, 15);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      MilkerTileEntity te = getTe(world, pos);
      if(te==null) return InteractionResult.FAIL;
      final ItemStack in_stack = player.getItemInHand(hand);
      final ItemStack out_stack = MilkerTileEntity.milk_filled_container_item(in_stack);
      if(in_stack.isEmpty()) {
        te.state_message(player);
        return InteractionResult.CONSUME;
      } else if(out_stack.isEmpty() && (te.fluid_handler()!=null)) {
        return Fluidics.manualFluidHandlerInteraction(player, hand, te.fluid_handler()) ? InteractionResult.CONSUME : InteractionResult.FAIL;
      } else {
        boolean drained = false;
        IItemHandler player_inventory = Inventories.itemhandler(player);
        if(te.fluid_level() >= BUCKET_SIZE) {
          final ItemStack insert_stack = out_stack.copy();
          ItemStack remainder = ItemHandlerHelper.insertItemStacked(player_inventory, insert_stack, false);
          if(remainder.getCount() < insert_stack.getCount()) {
            te.drain(BUCKET_SIZE);
            in_stack.shrink(1);
            drained = true;
            if(remainder.getCount() > 0) {
              final ItemEntity ei = new ItemEntity(world, player.position().x(), player.position().y()+0.5, player.position().z(), remainder);
              ei.setPickUpDelay(40);
              ei.setDeltaMovement(0,0,0);
              world.addFreshEntity(ei);
            }
          }
        }
        if(drained) {
          world.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1f);
        }
      }
      return InteractionResult.CONSUME;
    }

    @Nullable
    private MilkerTileEntity getTe(Level world, BlockPos pos)
    { final BlockEntity te=world.getBlockEntity(pos); return (!(te instanceof MilkerTileEntity)) ? (null) : ((MilkerTileEntity)te); }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class MilkerTileEntity extends StandardEntityBlocks.StandardBlockEntity // implements IFluidTank
  {
    private static final Direction[] FLUID_TRANSFER_DIRECTRIONS = {Direction.DOWN,Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH};
    private enum MilkingState { IDLE, PICKED, COMING, POSITIONING, MILKING, LEAVING, WAITING }

    private int tick_timer_;
    private UUID tracked_cow_ = null;
    private MilkingState state_ = MilkingState.IDLE;
    private int state_timeout_ = 0;
    private int state_timer_ = 0;
    private BlockPos tracked_cow_original_position_ = null;
    private final RfEnergy.Battery battery_;
    private final LazyOptional<IEnergyStorage> energy_handler_;
    private final Fluidics.Tank tank_;
    private final LazyOptional<IFluidHandler> fluid_handler_;

    public MilkerTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.getBlockEntityTypeOfBlock(state.getBlock()), pos, state);
      tank_ = new Fluidics.Tank(TANK_CAPACITY, 0, BUCKET_SIZE, fs->fs.isFluidEqual(milk_fluid_));
      fluid_handler_ = tank_.createOutputFluidHandler();
      battery_ = new RfEnergy.Battery(MAX_ENERGY_BUFFER, MAX_ENERGY_TRANSFER, 0);
      energy_handler_ = battery_.createEnergyHandler();
      reset();
    }

    public void reset()
    {
      tank_.clear();
      battery_.clear();
      tick_timer_ = 0;
      tracked_cow_ = null;
      state_ = MilkingState.IDLE;
      state_timeout_ = 0;
    }

    public CompoundTag destroy_getnbt()
    {
      final UUID cowuid = tracked_cow_;
      CompoundTag nbt = new CompoundTag();
      writenbt(nbt, false); reset();
      if(cowuid == null) return nbt;
      level.getEntitiesOfClass(Cow.class, new AABB(worldPosition).inflate(16, 16, 16), e->e.getUUID().equals(cowuid)).forEach(e->e.setNoAi(false));
      return nbt;
    }

    public void readnbt(CompoundTag nbt, boolean update_packet)
    {
      battery_.load(nbt);
      tank_.load(nbt);
    }

    protected void writenbt(CompoundTag nbt, boolean update_packet)
    {
      tank_.save(nbt);
      if(!battery_.isEmpty()) battery_.save(nbt);
    }

    private boolean has_milk_fluid()
    { return !(NO_MILK_FLUID.isFluidEqual(milk_fluid_)); }

    private IFluidHandler fluid_handler()
    { return fluid_handler_.orElse(null); }

    private int fluid_level()
    { return tank_.getFluidAmount(); }

    private FluidStack drain(int amount)
    { return tank_.drain(amount); }

    public void state_message(Player player)
    {
      Component rf = (energy_consumption_ <= 0) ? (Component.empty()) : (Auxiliaries.localizable("block.engineersdecor.small_milking_machine.status.rf", battery_.getEnergyStored()));
      Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.small_milking_machine.status", tank_.getFluidAmount(), rf));
    }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt, false); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.saveAdditional(nbt); writenbt(nbt, false); }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
      fluid_handler_.invalidate();
    }

    // ICapabilityProvider ---------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if((capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) && has_milk_fluid()) return fluid_handler_.cast();
      if((capability == CapabilityEnergy.ENERGY) && (energy_consumption_>0)) return energy_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickable ------------------------------------------------------------------------------------

    private static final HashMap<Integer, Long> tracked_cows_ = new HashMap<>();

    private void log(String s)
    {} // println("Milker|" + s); may be enabled with config, for dev was println

    private static ItemStack milk_filled_container_item(ItemStack stack)
    { return milk_containers_.entrySet().stream().filter(e-> Inventories.areItemStacksIdentical(e.getKey(), stack)).map(Map.Entry::getValue).findFirst().orElse(ItemStack.EMPTY); }

    private boolean fill_adjacent_inventory_item_containers(Direction block_facing)
    {
      // Check inventory existence, back to down is preferred, otherwise sort back into same inventory.
      IItemHandler src = Inventories.itemhandler(level, worldPosition.relative(block_facing), block_facing.getOpposite());
      IItemHandler dst = Inventories.itemhandler(level, worldPosition.below(), Direction.UP);
      if(src==null) { src = dst; } else if(dst==null) { dst = src; }
      if((src==null) || (dst==null)) return false;
      boolean dirty = false;
      while((tank_.getFluidAmount() >= BUCKET_SIZE)) {
        boolean inserted = false;
        for(Entry<ItemStack,ItemStack> e:milk_containers_.entrySet()) {
          if(Inventories.extract(src, e.getKey(), 1, true).isEmpty()) continue;
          if(!Inventories.insert(dst, e.getValue().copy(), false).isEmpty()) continue;
          Inventories.extract(src, e.getKey(), 1, false);
          tank_.drain(BUCKET_SIZE);
          inserted = true;
          dirty = true;
          break;
        }
        if(!inserted) break;
      }
      return dirty;
    }

    private boolean fill_adjacent_tank()
    {
      if((fluid_level()<=0) || (!has_milk_fluid())) return false;
      final FluidStack fs = new FluidStack(milk_fluid_, Math.max(fluid_level(), BUCKET_SIZE));
      for(Direction dir:Direction.values()) {
        int amount = Fluidics.fill(getLevel(), getBlockPos().relative(dir), dir.getOpposite(), fs);
        if(amount > 0) {
          tank_.drain(amount);
          return true;
        }
      }
      return false;
    }

    private void release_cow(Cow cow)
    {
      log("release cow");
      if(cow != null) {
        cow.setNoAi(false);
        SingleMoveGoal.abortFor(cow);
        tracked_cows_.remove(cow.getId());
        for(int id:tracked_cows_.keySet().stream().filter(i->cow.getCommandSenderWorld().getEntity(i)==null).collect(Collectors.toList())) {
          tracked_cows_.remove(id);
        }
      }
      tracked_cow_ = null;
      state_ = MilkingState.IDLE;
      tick_timer_ = TICK_INTERVAL;
    }

    private boolean milking_process()
    {
      if((tracked_cow_ == null) && (fluid_level() >= MAX_MILKING_TANK_LEVEL)) return false; // nothing to do
      final Direction facing = level.getBlockState(getBlockPos()).getValue(MilkerBlock.HORIZONTAL_FACING).getOpposite();
      final Vec3 target_pos = Vec3.atLowerCornerOf(getBlockPos().relative(facing)).add(0.5,0,0.5);
      Cow cow = null;
      {
        AABB aabb = new AABB(worldPosition.relative(facing, 3)).inflate(4, 2, 4);
        final long t = level.getGameTime();
        final List<Cow> cows = level.getEntitiesOfClass(Cow.class, aabb,
          e-> {
            if(e.getUUID().equals(tracked_cow_)) return true;
            if((tracked_cow_!=null) || e.isBaby() || e.isInLove() || e.isVehicle()) return false;
            if(!e.getNavigation().isDone()) return false;
            if(Math.abs(tracked_cows_.getOrDefault(e.getId(), 0L)-t) < min_milking_delay_per_cow_ticks_) return false;
            return true;
          }
        );
        if(cows.size() == 1) {
          cow = cows.get(0); // tracked or only one
        } else if(cows.size() > 1) {
          cow = cows.get(level.random.nextInt(cows.size()-1)); // pick one
        }
      }
      if((state_ != MilkingState.IDLE) && ((state_timeout_ -= PROCESSING_TICK_INTERVAL) <= 0)) { release_cow(cow); log("Cow motion timeout"); cow = null; }
      if((cow == null) || (!cow.isAlive())) { release_cow(cow); cow = null; }
      if(tracked_cow_ == null) state_ = MilkingState.IDLE;
      if(cow == null) { log("Init: No cow"); return false; } // retry next cycle
      tick_timer_ = PROCESSING_TICK_INTERVAL;
      state_timer_ -= PROCESSING_TICK_INTERVAL;
      if(state_timer_ > 0) return false;
      switch(state_) { // Let's do this the old school FSA sequencing way ...
        case IDLE -> {
          final List<LivingEntity> blocking_entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition.relative(facing)).inflate(0.5, 0.5, 0.5));
          if(blocking_entities.size() > 0) {
            tick_timer_ = TICK_INTERVAL;
            log("Idle: Position blocked");
            if(blocking_entities.get(0) instanceof final Cow blocker) {
              BlockPos p = getBlockPos().relative(facing, 2);
              log("Idle: Shove off");
              blocker.setNoAi(false);
              SingleMoveGoal.startFor(blocker, p, 2, 1.0, (goal, world, pos) -> (pos.distSqr(goal.getCreature().blockPosition()) > 100));
            }
            return false;
          }
          if(cow.isLeashed() || cow.isBaby() || cow.isInLove() || (!cow.isOnGround()) || cow.isVehicle() || cow.isSprinting())
            return false;
          tracked_cows_.put(cow.getId(), cow.getCommandSenderWorld().getGameTime());
          tracked_cow_ = cow.getUUID();
          state_ = MilkingState.PICKED;
          state_timeout_ = 200;
          tracked_cow_original_position_ = cow.blockPosition();
          log("Idle: Picked cow " + tracked_cow_);
          return false;
        }
        case PICKED -> {
          SingleMoveGoal.startFor(
            cow, target_pos, 2, 1.0,
            (goal, world, pos) -> (pos.distSqr(goal.getCreature().blockPosition()) > 100),
            (goal, world, pos) -> {
              log("move: position reached");
              goal.getCreature().moveTo(goal.getTargetPosition().x(), goal.getTargetPosition().y(), goal.getTargetPosition().z(), facing.toYRot(), 0);
            },
            (goal, world, pos) -> log("move: aborted")
          );
          state_ = MilkingState.COMING;
          state_timeout_ = 400; // 15s should be enough
          log("Picked: coming to " + target_pos);
          return false;
        }
        case COMING -> {
          if(target_pos.distanceToSqr(cow.position()) <= 1) {
            log("Coming: position reached");
            state_ = MilkingState.POSITIONING;
            state_timeout_ = 100; // 5s
          } else if((!SingleMoveGoal.isActiveFor(cow))) {
            release_cow(cow);
            log("Coming: aborted");
          } else {
            state_timeout_ -= 100;
          }
          return false;
        }
        case POSITIONING -> {
          log("Positioning: start milking");
          SingleMoveGoal.abortFor(cow);
          cow.setNoAi(true);
          cow.moveTo(target_pos.x(), target_pos.y(), target_pos.z(), facing.toYRot(), 0);
          level.playSound(null, worldPosition, SoundEvents.COW_MILK, SoundSource.BLOCKS, 0.5f, 1f);
          state_timeout_ = 600;
          state_ = MilkingState.MILKING;
          state_timer_ = 30;
          return false;
        }
        case MILKING -> {
          tank_.fill(milk_fluid_.copy(), IFluidHandler.FluidAction.EXECUTE);
          state_timeout_ = 600;
          state_ = MilkingState.LEAVING;
          state_timer_ = 20;
          cow.setNoAi(false);
          cow.getNavigation().stop();
          log("Milking: done, leave");
          return true;
        }
        case LEAVING -> {
          BlockPos p = (tracked_cow_original_position_ != null) ? (tracked_cow_original_position_) : getBlockPos().relative(facing, 2).relative(facing.getCounterClockWise());
          SingleMoveGoal.startFor(cow, p, 2, 1.0, (goal, world, pos) -> (pos.distSqr(goal.getCreature().blockPosition()) > 100));
          state_timeout_ = 600;
          state_timer_ = 500;
          tick_timer_ = TICK_INTERVAL;
          state_ = MilkingState.WAITING;
          tracked_cows_.put(cow.getId(), cow.getCommandSenderWorld().getGameTime());
          log("Leaving: process done");
          return true;
        }
        case WAITING -> {
          // wait for the timeout to kick in until starting with the next.
          tick_timer_ = TICK_INTERVAL;
          if(state_timer_ < 40) {
            tracked_cow_ = null;
            release_cow(null);
          }
          log("Waiting time elapsed");
          return true;
        }
        default -> release_cow(cow);
      }
      return (tracked_cow_ != null);
    }

    @Override
    public void tick()
    {
      if((level.isClientSide) || ((--tick_timer_ > 0))) return;
      tick_timer_ = TICK_INTERVAL;
      boolean dirty = false;
      final BlockState block_state = level.getBlockState(worldPosition);
      if(!(block_state.getBlock() instanceof MilkerBlock)) return;
      if(!level.hasNeighborSignal(worldPosition) || (state_ != MilkingState.IDLE)) {
        if((energy_consumption_ > 0) && (!battery_.draw(energy_consumption_))) return;
        // Track and milk cows
        if(milking_process()) dirty = true;
        // Fluid transfer
        if(has_milk_fluid() && (!tank_.isEmpty())) {
          log("Fluid transfer");
          for(Direction facing: FLUID_TRANSFER_DIRECTRIONS) {
            final IFluidHandler fh = Fluidics.handler(level, worldPosition.relative(facing), facing.getOpposite());
            if(fh == null) continue;
            final FluidStack fs = tank_.drain(BUCKET_SIZE, IFluidHandler.FluidAction.SIMULATE);
            int nfilled = fh.fill(fs, IFluidHandler.FluidAction.EXECUTE);
            if(nfilled <= 0) continue;
            tank_.drain(nfilled, IFluidHandler.FluidAction.EXECUTE);
            dirty = true;
            break;
          }
        }
        // Adjacent inventory update, only done just after milking to prevent waste of server cpu.
        if((!dirty) && (fluid_level() > 0)) {
          log("Try item transfer");
          if(fill_adjacent_tank() || ((fluid_level() >= BUCKET_SIZE) && fill_adjacent_inventory_item_containers(block_state.getValue(MilkerBlock.HORIZONTAL_FACING)))) dirty = true;
        }
      }
      // State update
      BlockState new_state = block_state.setValue(MilkerBlock.FILLED, fluid_level()>=FILLED_INDICATION_THRESHOLD).setValue(MilkerBlock.ACTIVE, state_==MilkingState.MILKING);
      if(block_state != new_state) level.setBlock(worldPosition, new_state,1|2|16);
      if(dirty) setChanged();
    }
  }

  public static class SingleMoveGoal extends MoveToBlockGoal
  {
    @FunctionalInterface public interface TargetPositionInValidCheck { boolean test(SingleMoveGoal goal, LevelReader world, BlockPos pos); }
    @FunctionalInterface public interface StrollEvent { void apply(SingleMoveGoal goal, LevelReader world, Vec3 pos); }
    private static void log(String s) {} // println("SingleMoveGoal: "+s);

    private static final HashMap<Integer, SingleMoveGoal> tracked_entities_ = new HashMap<>();
    private static final int motion_timeout = 20*20;
    private boolean aborted_;
    private boolean in_position_;
    private boolean was_aborted_;
    private Vec3 target_pos_;
    private TargetPositionInValidCheck abort_condition_;
    private StrollEvent on_target_position_reached_;
    private StrollEvent on_aborted_;

    public SingleMoveGoal(PathfinderMob creature, Vec3 pos, double speed, TargetPositionInValidCheck abort_condition, @Nullable StrollEvent on_position_reached, @Nullable StrollEvent on_aborted)
    {
      super(creature, speed, 32, 32);
      abort_condition_ = abort_condition;
      on_target_position_reached_ = on_position_reached;
      on_aborted_ = on_aborted;
      blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
      tryTicks = 0;
      nextStartTick = 0;
      aborted_ = false;
      was_aborted_ = false;
      target_pos_ = pos;
    }

    public static void startFor(PathfinderMob entity, BlockPos target_pos, int priority, double speed, TargetPositionInValidCheck abort_condition)
    { startFor(entity, new Vec3(target_pos.getX(),target_pos.getY(),target_pos.getZ()), priority, speed, abort_condition, null, null); }

    public static boolean startFor(PathfinderMob entity, Vec3 target_pos, int priority, double speed, TargetPositionInValidCheck abort_condition, @Nullable StrollEvent on_position_reached, @Nullable StrollEvent on_aborted)
    {
      synchronized(tracked_entities_) {
        SingleMoveGoal goal = tracked_entities_.getOrDefault(entity.getId(), null);
        if(goal != null) {
          if(!goal.aborted()) return false; // that is still running.
          entity.goalSelector.removeGoal(goal);
        }
        log("::start("+entity.getId()+")");
        goal = new SingleMoveGoal(entity, target_pos, speed, abort_condition, on_position_reached, on_aborted);
        tracked_entities_.put(entity.getId(), goal);
        entity.goalSelector.addGoal(priority, goal);
        return true;
      }
    }

    public static boolean isActiveFor(PathfinderMob entity)
    { return (entity != null) && (entity.goalSelector.getRunningGoals().anyMatch(
      g->((g.getGoal()) instanceof SingleMoveGoal) && (!((SingleMoveGoal)(g.getGoal())).aborted())
    )); }

    public static void abortFor(PathfinderMob entity)
    {
      log("::abort("+entity.getId()+")");
      if(entity.isAlive()) {
        entity.goalSelector.getRunningGoals().filter(g->(g.getGoal()) instanceof SingleMoveGoal).forEach(g->((SingleMoveGoal)g.getGoal()).abort());
      }
      final Level world = entity.getCommandSenderWorld();
      if(world != null) {
        // @todo: check nicer way to filter a map.
        List<Integer> to_remove = tracked_entities_.keySet().stream().filter(i->(world.getEntity(i) == null)).collect(Collectors.toList());
        for(int id:to_remove)tracked_entities_.remove(id);
      }
    }

    public Vec3 getTargetPosition()
    { return target_pos_; }

    public PathfinderMob getCreature()
    { return mob; }

    public synchronized void abort()
    { aborted_ = true; }

    public synchronized boolean aborted()
    { return aborted_; }

    public synchronized void initialize(Vec3 target_pos, double speed, TargetPositionInValidCheck abort_condition, @Nullable StrollEvent on_position_reached, @Nullable StrollEvent on_aborted)
    {
      abort_condition_ = abort_condition;
      on_target_position_reached_ = on_position_reached;
      on_aborted_ = on_aborted;
      blockPos = new BlockPos(target_pos.x(), target_pos.y(), target_pos.z());
      tryTicks = 0;
      nextStartTick = 0;
      aborted_ = false;
      was_aborted_ = false;
      target_pos_ = new Vec3(target_pos.x(), target_pos.y(), target_pos.z());
      // this.movementSpeed = speed; -> that is final, need to override tick and func_whatever
    }

    @Override
    public void stop()
    { nextStartTick = 0; tryTicks = 0; }

    @Override
    public double acceptedDistance()
    { return 0.7; }

    @Override
    public boolean shouldRecalculatePath()
    { return (!aborted()) && (tryTicks & 0x7) == 0; }

    @Override
    public boolean canUse()
    {
      if(aborted_) {
        if((!was_aborted_) && (on_aborted_!=null)) on_aborted_.apply(this, mob.level, target_pos_);
        was_aborted_ = true;
        return false;
      } else if(!isValidTarget(mob.level, blockPos)) {
        synchronized(this) { aborted_ = true; }
        return false;
      } else if(--nextStartTick > 0) {
        return false;
      } else {
        nextStartTick = 10;
        return true;
      }
    }

    @Override
    public void start()
    {
      tryTicks = 0;
      if(!mob.getNavigation().moveTo(target_pos_.x(), target_pos_.y(), target_pos_.z(), this.speedModifier)) {
        abort();
        log("startExecuting() -> abort, no path");
      } else {
        log("startExecuting() -> started");
      }
    }

    public boolean canContinueToUse()
    {
      if(aborted()) {
        log("shouldContinueExecuting() -> already aborted");
        return false;
      } else if(mob.getNavigation().isDone()) {
        if((!mob.getNavigation().moveTo(mob.getNavigation().createPath(target_pos_.x(), target_pos_.y(), target_pos_.z(), 0), speedModifier))) {
          log("shouldContinueExecuting() -> abort, no path");
          abort();
          return false;
        } else {
          return true;
        }
      } else if(tryTicks > motion_timeout) {
        log("shouldContinueExecuting() -> abort, timeout");
        abort();
        return false;
      } else if(!isValidTarget(mob.level, blockPos)) {
        log("shouldContinueExecuting() -> abort, !shouldMoveTo()");
        abort();
        return false;
      } else {
        log("shouldContinueExecuting() -> yes");
        return true;
      }
    }

    @Override
    protected boolean isValidTarget(LevelReader world, BlockPos pos)
    {
      if(abort_condition_.test(this, world, pos)) {
        log("shouldMoveTo() -> abort_condition");
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void tick()
    {
      final BlockPos testpos = new BlockPos(target_pos_.x(), mob.position().y(), target_pos_.z());
      if(!testpos.closerToCenterThan(mob.position(), acceptedDistance())) {
        if((++tryTicks > motion_timeout)) {
          log("tick() -> abort, timeoutCounter");
          abort();
          return;
        }
        if(shouldRecalculatePath() && (!mob.getNavigation().moveTo(target_pos_.x(), target_pos_.y(), target_pos_.z(), speedModifier))) {
          log("tick() -> abort, !tryMoveToXYZ()");
          abort();
        }
      } else {
        log("tick() -> abort, in position)");
        in_position_ = true;
        abort();
        if(on_target_position_reached_ != null) on_target_position_reached_.apply(this, mob.level, target_pos_);
      }
    }
  }
}
