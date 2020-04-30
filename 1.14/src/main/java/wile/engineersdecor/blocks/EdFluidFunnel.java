/*
 * @file EdFluidFunnel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * A device that collects and stores fluid blocks above it.
 * Tracks flowing fluid to their source blocks. Compatible
 * with vanilla infinite water source.
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;


public class EdFluidFunnel
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidFunnelBlock extends StandardBlocks.BaseBlock implements IDecorBlock
  {
    public static final int FILL_LEVEL_MAX = 3;
    public static final IntegerProperty FILL_LEVEL = IntegerProperty.create("level", 0, FILL_LEVEL_MAX);

    public FluidFunnelBlock(long config, Block.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(FILL_LEVEL); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).with(FILL_LEVEL, 0); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new FluidFunnelTileEntity(); }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos)
    { return MathHelper.clamp((state.get(FILL_LEVEL)*5), 0, 15); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isRemote) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundNBT te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof FluidFunnelTileEntity)) return;
      ((FluidFunnelTileEntity)te).readnbt(te_nbt);
      ((FluidFunnelTileEntity)te).markDirty();
      world.setBlockState(pos, state.with(FILL_LEVEL, 0));
    }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, BlockPos pos, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isRemote) return stacks;
      final TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof FluidFunnelTileEntity)) return stacks;
      if(!explosion) {
        ItemStack stack = new ItemStack(this, 1);
        CompoundNBT te_nbt = new CompoundNBT();
        ((FluidFunnelTileEntity)te).writenbt(te_nbt);
        if(!te_nbt.isEmpty()) {
          CompoundNBT nbt = new CompoundNBT();
          nbt.put("tedata", te_nbt);
          stack.setTag(nbt);
        }
        stacks.add(stack);
      } else {
        stacks.add(new ItemStack(this, 1));
      }
      return stacks;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(world.isRemote) return true;
      TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof FluidFunnelTileEntity)) return false;
      return FluidUtil.interactWithFluidHandler(player, hand, world, pos, rayTraceResult.getFace());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean unused)
    { TileEntity te = world.getTileEntity(pos); if(te instanceof FluidFunnelTileEntity) ((FluidFunnelTileEntity)te).block_changed(); }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidFunnelTileEntity extends TileEntity implements ITickableTileEntity, ICapabilityProvider, IFluidTank
  {
    public static final int TANK_CAPACITY = 3000;
    public static final int TICK_INTERVAL = 10; // ca 500ms
    public static final int COLLECTION_INTERVAL = 40; // ca 2000ms, simulates suction delay and saves CPU when not drained.
    public static final int MAX_TRACK_RADIUS = 16;
    public static final int MAX_TRACKING_STEPS_PER_CYCLE = 72;
    public static final int MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE = 1024;
    public static final int MAX_TRACK_RADIUS_SQ = MAX_TRACK_RADIUS*MAX_TRACK_RADIUS;
    public static final int INTENSIVE_SEARCH_TRIGGER_THRESHOLD = 16;
    private FluidStack tank_ = FluidStack.EMPTY;
    private int tick_timer_ = 0;
    private int collection_timer_ = 0;
    private int no_fluid_found_counter_ = 0;
    private int intensive_search_counter_ = 0;
    private int total_pick_counter_ = 0;
    private BlockPos last_pick_pos_ = BlockPos.ZERO;
    private ArrayList<Vec3i> search_offsets_ = null;

    public void block_changed()
    { tick_timer_ = TICK_INTERVAL; } // collect after flowing fluid has a stable state, otherwise it looks odd.

    public FluidFunnelTileEntity()
    { this(ModContent.TET_SMALL_FLUID_FUNNEL); }

    public FluidFunnelTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    public void readnbt(CompoundNBT nbt)
    {
      tank_ = (!nbt.contains("tank")) ? (FluidStack.EMPTY) : (FluidStack.loadFluidStackFromNBT(nbt.getCompound("tank")));
    }

    public void writenbt(CompoundNBT nbt)
    {
      if(!tank_.isEmpty()) nbt.put("tank", tank_.writeToNBT(new CompoundNBT()));
    }

    // TileEntity -----------------------------------------------------------------------------------------

    @Override
    public void read(CompoundNBT nbt)
    { super.read(nbt); readnbt(nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); writenbt(nbt); return nbt; }

    @Override
    public void remove()
    {
      super.remove();
      fluid_handler_.invalidate();
    }

    // ICapabilityProvider / Output flow handler ----------------------------------------------------------

    private static class OutputFluidHandler implements IFluidHandler
    {
      private final FluidFunnelTileEntity te;
      OutputFluidHandler(FluidFunnelTileEntity parent) { te = parent; }
      @Override public int getTanks() { return 1; }
      @Override public FluidStack getFluidInTank(int tank) { return te.tank_.copy(); }
      @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
      @Override public boolean isFluidValid(int tank, @Nonnull FluidStack stack) { return true; }
      @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
      @Override public FluidStack drain(FluidStack resource, FluidAction action) { return te.drain(resource, action); }
      @Override public FluidStack drain(int maxDrain, FluidAction action) { return te.drain(maxDrain, action); }
    }

    private final LazyOptional<IFluidHandler> fluid_handler_ = LazyOptional.of(() -> new OutputFluidHandler(this));

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluid_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // IFluidTank ------------------------------------------------------------------------------------------

    @Override
    @Nonnull
    public FluidStack getFluid()
    { return tank_.copy(); }

    @Override
    public int getFluidAmount()
    { return tank_.getAmount(); }

    @Override
    public int getCapacity()
    { return TANK_CAPACITY; }

    @Override
    public boolean isFluidValid(FluidStack stack)
    { return true; }

    @Override
    public int fill(FluidStack resource, FluidAction action)
    { return 0; }

    @Override
    @Nonnull
    public FluidStack drain(FluidStack resource, FluidAction action)
    {
      if((resource==null) || (tank_.isEmpty())) return FluidStack.EMPTY;
      return (!(tank_.isFluidEqual(resource))) ? (FluidStack.EMPTY) : drain(resource.getAmount(), action);
    }

    @Override
    @Nonnull
    public FluidStack drain(int maxDrain, FluidAction action)
    {
      if(tank_.isEmpty()) return FluidStack.EMPTY;
      FluidStack res = tank_.copy();
      maxDrain = MathHelper.clamp(maxDrain ,0 , tank_.getAmount());
      res.setAmount(maxDrain);
      if(action != FluidAction.EXECUTE) return res;
      tank_.setAmount(tank_.getAmount()-maxDrain);
      if(tank_.getAmount() <= 0) tank_ = FluidStack.EMPTY;
      return res;
    }

    // ITickableTileEntity --------------------------------------------------------------------------------

    private IFluidState get_fluidstate(BlockPos pos)
    {
      // todo: check if getFluidState() is enough
      final Block collection_block = world.getBlockState(pos).getBlock();
      if((!(collection_block instanceof IFluidBlock)) && (!(collection_block instanceof FlowingFluidBlock)) && (!(collection_block instanceof IWaterLoggable))) {
        return Fluids.EMPTY.getDefaultState();
      }
      return world.getFluidState(pos);
    }

    private boolean try_pick(BlockPos pos, IFluidState fluidstate)
    {
      if(!fluidstate.isSource()) return false;
      IFluidHandler hnd = FluidUtil.getFluidHandler(world, pos, null).orElse(null);
      FluidStack fs;
      if(hnd != null) {
        fs = hnd.drain(TANK_CAPACITY, FluidAction.EXECUTE); // IFluidBlock
      } else {
        fs = new FluidStack(fluidstate.getFluid(), 1000);
        BlockState state = world.getBlockState(pos);
        if(state instanceof IBucketPickupHandler) {
          ((IBucketPickupHandler)state).pickupFluid(world, pos, state);
        } else {
          world.setBlockState(pos, Blocks.AIR.getDefaultState(), 1|2); // ok we can't leave the block, that would be an infinite source of an unknown fluid.
        }
      }
      if((fs==null) || (fs.isEmpty())) return false; // it's marked nonnull but I don't trust every modder - including meself ...
      if(tank_.isEmpty()) {
        tank_ = fs.copy();
      } else if(tank_.isFluidEqual(fs)) {
        tank_.setAmount(MathHelper.clamp(tank_.getAmount()+fs.getAmount(), 0, TANK_CAPACITY));
      } else {
        return false;
      }
      return true;
    }

    private boolean can_pick(BlockPos pos, IFluidState fluidstate)
    {
      if(fluidstate.isSource()) return true;
      IFluidHandler hnd = FluidUtil.getFluidHandler(world, pos, null).orElse(null);
      if(hnd == null) return false;
      FluidStack fs = hnd.drain(TANK_CAPACITY, FluidAction.SIMULATE); // don't trust that everyone returns nonnull
      return ((fs!=null) && (!fs.isEmpty())) && (fluidstate.getFluid().isEquivalentTo(fs.getFluid()));
    }

    private void rebuild_search_offsets(boolean intensive)
    {
      search_offsets_ = new ArrayList<>(9);
      search_offsets_.add(new Vec3i(0, 1, 0)); // up first
      {
        ArrayList<Vec3i> ofs = new ArrayList<Vec3i>(Arrays.asList(new Vec3i(-1, 0, 0), new Vec3i( 1, 0, 0), new Vec3i( 0, 0,-1), new Vec3i( 0, 0, 1)));
        if(intensive || (total_pick_counter_ > 50)) Collections.shuffle(ofs);
        search_offsets_.addAll(ofs);
      }
      if(intensive) {
        ArrayList<Vec3i> ofs = new ArrayList<Vec3i>(Arrays.asList(new Vec3i(-1, 1, 0), new Vec3i( 1, 1, 0), new Vec3i( 0, 1,-1), new Vec3i( 0, 1, 1)));
        Collections.shuffle(ofs);
        search_offsets_.addAll(ofs);
      }
    }

    private boolean try_collect(final BlockPos collection_pos)
    {
      IFluidState collection_fluidstate = get_fluidstate(collection_pos);
      if(collection_fluidstate.isEmpty()) return false;
      Fluid fluid_to_collect = collection_fluidstate.getFluid();
      if((!tank_.isEmpty()) && (!tank_.getFluid().isEquivalentTo(fluid_to_collect))) return false;
      if(try_pick(collection_pos, collection_fluidstate)) { last_pick_pos_ = collection_pos; return true; } // Blocks directly always first. Allows water source blocks to recover/reflow to source blocks.
      if((last_pick_pos_==null) || (last_pick_pos_.distanceSq(collection_pos) > MAX_TRACK_RADIUS_SQ)) { last_pick_pos_ = collection_pos; search_offsets_ = null; }
      BlockPos pos = last_pick_pos_;
      HashSet<BlockPos> checked = new HashSet<>();
      Stack<BlockPos> trail = new Stack<BlockPos>();
      trail.add(pos);
      checked.add(pos);
      int steps=0;
      boolean intensive = (no_fluid_found_counter_ >= INTENSIVE_SEARCH_TRIGGER_THRESHOLD);
      if(intensive) { no_fluid_found_counter_ = 0; ++intensive_search_counter_; }
      if(search_offsets_ == null) rebuild_search_offsets(intensive);
      int max = intensive ? MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE : MAX_TRACKING_STEPS_PER_CYCLE;
      while(++steps <= max) {
        int num_adjacent = 0;
        for(int i=0; i<search_offsets_.size(); ++i) {
          BlockPos p = pos.add(search_offsets_.get(i));
          if(checked.contains(p)) continue;
          checked.add(p);
          ++steps;
          IFluidState fluidstate = get_fluidstate(p);

          // @todo: nice thing in 1.14: the fluid level is easily readable,
          //        so lateral motion can be restricted to higher fill levels.

          if(fluidstate.getFluid().isEquivalentTo(fluid_to_collect)) {
            ++num_adjacent;
            pos = p;
            trail.push(pos);
            if(steps < MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE/2) {
              // check for same fluid above (only source blocks)
              final int max_surface_search = (MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE/2)-steps;
              for(int k=0; k<max_surface_search; ++k) {
                IFluidState fs = get_fluidstate(pos.up());
                if(!can_pick(pos.up(), fs)) break;
                fluidstate = fs;
                pos = pos.up();
                trail.push(pos);
              }
            }
            if(try_pick(pos, fluidstate)) {
              last_pick_pos_ = pos;
              no_fluid_found_counter_ = 0;
              search_offsets_ = null;
              // probability reset, so it's not turteling too far away, mainly for large nether lava seas, not desert lakes.
              if((++total_pick_counter_ > 50) && world.rand.nextInt(10)==0) last_pick_pos_ = collection_pos;
              //println("PASS " + steps + " - " + (pos.subtract(collection_pos)));
              return true;
            }
          }
        }
        if(trail.isEmpty()) break; // reset search
        if(num_adjacent==0) pos = trail.pop();
      }
      //println("FAIL=" + steps + " - " + (pos.subtract(collection_pos)));
      //String s = new String(); for(BlockPos p:checked) s += "\n" + p; println(s);
      if(intensive_search_counter_ > 2) world.removeBlock(pos, false);
      last_pick_pos_ = collection_pos;
      search_offsets_ = null; // try other search order
      ++no_fluid_found_counter_;
      return false;
    }

    public void tick()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      collection_timer_ += TICK_INTERVAL;
      boolean dirty = false;
      // Collection
      if((collection_timer_ >= COLLECTION_INTERVAL) && ((tank_==null) || (tank_.getAmount() <= (TANK_CAPACITY-1000)))) {
      collection_timer_ = 0;
        if(!world.isBlockPowered(pos)) { // redstone disable feature
          if(last_pick_pos_==null) last_pick_pos_ = pos.up();
          if(try_collect(pos.up())) dirty = true;
        }
      }
      // Gravity fluid transfer
      if((tank_.getAmount() >= 1000)) {
        IFluidHandler fh = FluidUtil.getFluidHandler(world, pos.down(), Direction.UP).orElse(null);
        if(fh != null) {
          FluidStack fs = new FluidStack(tank_.getFluid(), 1000);
          int nfilled = MathHelper.clamp(fh.fill(fs, FluidAction.EXECUTE), 0, 1000);
          tank_.shrink(nfilled);
          dirty = true;
        }
      }
      // Block state
      int fill_level = (tank_==null) ? 0 : (MathHelper.clamp(tank_.getAmount()/1000,0, FluidFunnelBlock.FILL_LEVEL_MAX));
      final BlockState funnel_state = world.getBlockState(pos);
      if(funnel_state.get(FluidFunnelBlock.FILL_LEVEL) != fill_level) world.setBlockState(pos, funnel_state.with(FluidFunnelBlock.FILL_LEVEL, fill_level), 2|16);
      if(dirty) markDirty();
    }
  }
}
