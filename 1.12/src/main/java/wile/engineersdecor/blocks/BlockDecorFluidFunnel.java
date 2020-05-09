/*
 * @file BlockDecorFluidFunnel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * A device that collects and stores fluid blocks above it.
 * Tracks flowing fluid to their source blocks. Compatible
 * with vanilla infinite water source.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;


public class BlockDecorFluidFunnel extends BlockDecor
{
  public static final int FILL_LEVEL_MAX = 3;
  public static final PropertyInteger FILL_LEVEL = PropertyInteger.create("level", 0, FILL_LEVEL_MAX);

  public BlockDecorFluidFunnel(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  { super(registryName, config, material, hardness, resistance, sound, unrotatedAABB); }

  @Override
  protected BlockStateContainer createBlockState()
  { return new BlockStateContainer(this, FILL_LEVEL); }

  @Override
  public IBlockState getStateFromMeta(int meta)
  { return super.getStateFromMeta(meta).withProperty(FILL_LEVEL, meta & 0x3); }

  @Override
  public int getMetaFromState(IBlockState state)
  { return super.getMetaFromState(state) | (state.getValue(FILL_LEVEL)); }

  @Override
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
  { return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FILL_LEVEL, 0); }

  @Override
  @SuppressWarnings("deprecation")
  public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos)
  { return MathHelper.clamp((state.getValue(FILL_LEVEL)*5), 0, 15); }

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
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof BTileEntity)) return false;
    return FluidUtil.interactWithFluidHandler(player, hand, world, pos, facing);
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
  { TileEntity te = world.getTileEntity(pos); if(te instanceof BTileEntity) ((BTileEntity)te).block_changed(); }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class BTileEntity extends TileEntity implements IFluidHandler, IFluidTankProperties, ICapabilityProvider, ITickable
  {
    public static final int TANK_CAPACITY = 3000;
    public static final int TICK_INTERVAL = 10; // ca 500ms
    public static final int COLLECTION_INTERVAL = 40; // ca 2000ms, simulates suction delay and saves CPU when not drained.
    public static final int MAX_TRACK_RADIUS = 16;
    public static final int MAX_TRACKING_STEPS_PER_CYCLE = 72;
    public static final int MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE = 1024;
    public static final int MAX_TRACK_RADIUS_SQ = MAX_TRACK_RADIUS*MAX_TRACK_RADIUS;
    public static final int INTENSIVE_SEARCH_TRIGGER_THRESHOLD = 16;
    private final IFluidTankProperties[] fluid_props_ = {this};
    private FluidStack tank_ = null;
    private int tick_timer_ = 0;
    private int collection_timer_ = 0;
    private BlockPos last_pick_pos_ = BlockPos.ORIGIN;
    private ArrayList<Vec3i> search_offsets_ = null;
    private int no_fluid_found_counter_ = 0;
    private int intensive_search_counter_ = 0;
    private int total_pick_counter_ = 0;

    public BTileEntity()
    {}

    public void block_changed()
    { tick_timer_ = TICK_INTERVAL; } // collect after flowing fluid has a stable state, otherwise it looks odd.

    public void readnbt(NBTTagCompound nbt, boolean update_packet)
    {
      tank_ = (!nbt.hasKey("tank")) ? (null) : (FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("tank")));
    }

    protected void writenbt(NBTTagCompound nbt, boolean update_packet)
    {
      if(tank_ != null) nbt.setTag("tank", tank_.writeToNBT(new NBTTagCompound()));
    }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState os, IBlockState ns)
    {
      block_changed();
      return (os.getBlock() != ns.getBlock()) || (!(ns.getBlock() instanceof BlockDecorFluidFunnel));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    { super.readFromNBT(nbt); readnbt(nbt, false); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    { super.writeToNBT(nbt); writenbt(nbt, false); return nbt; }

    // ICapabilityProvider --------------------------------------------------------------------

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing)
    { return ((capability==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)) || super.hasCapability(capability, facing); }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing)
    {
      if(capability != CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return super.getCapability(capability, facing);
      return ((T)this);
    }

    // IFluidHandler of the output port --------------------------------------------------------

    @Override
    public IFluidTankProperties[] getTankProperties()
    { return fluid_props_; }

    @Override
    public int fill(FluidStack resource, boolean doFill)
    { return 0; }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain)
    {
      if((resource==null) || (tank_==null)) return null;
      return (!(tank_.isFluidEqual(resource))) ? (null) : drain(resource.amount, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain)
    {
      if(tank_==null) return null;
      maxDrain = MathHelper.clamp(maxDrain ,0 , tank_.amount);
      FluidStack res = tank_.copy();
      res.amount = maxDrain;
      if(doDrain) tank_.amount -= maxDrain;
      if(tank_.amount <= 0) tank_= null;
      return res;
    }

    // IFluidTankProperties --------------------------------------------------------------------

    @Override @Nullable public FluidStack getContents() { return (tank_==null) ? (null) : (tank_.copy()); }
    @Override public int getCapacity() { return TANK_CAPACITY; }
    @Override public boolean canFill() { return false; }
    @Override public boolean canDrain()  { return true; }
    @Override public boolean canFillFluidType(FluidStack fluidStack)  { return false; }
    @Override public boolean canDrainFluidType(FluidStack fluidStack) { return true; }

    // ITickable--------------------------------------------------------------------------------

    private Fluid get_fluid(BlockPos pos)
    { return FluidRegistry.lookupFluidForBlock(world.getBlockState(pos).getBlock()); }

    private boolean try_pick(BlockPos pos)
    {
      IFluidHandler hnd = FluidUtil.getFluidHandler(world, pos, null);
      if(hnd == null) return false;
      FluidStack fs = hnd.drain((tank_==null) ? (TANK_CAPACITY) : (TANK_CAPACITY-tank_.amount), true);
      if(fs == null) return false;
      if(tank_ == null) {
        tank_ = fs.copy();
      } else if(tank_.isFluidEqual(fs)) {
        tank_.amount = MathHelper.clamp(tank_.amount+fs.amount, 0, TANK_CAPACITY);
      } else {
        return false;
      }
      world.setBlockToAir(pos);
      world.notifyNeighborsOfStateChange(pos, world.getBlockState(pos).getBlock(), true); // explicitly update neighbours to allow these start flowing
      return true;
    }

    private boolean can_pick(BlockPos pos, Fluid fluid)
    {
      IFluidHandler hnd = FluidUtil.getFluidHandler(world, pos, null);
      if(hnd == null) return false;
      FluidStack fs = hnd.drain((tank_==null) ? (TANK_CAPACITY) : (TANK_CAPACITY-tank_.amount), false);
      return (fs != null) && (fs.getFluid().equals(fluid));
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

    private boolean try_collect(BlockPos collection_pos)
    {
      final Block collection_block = world.getBlockState(collection_pos).getBlock();
      if((!(collection_block instanceof IFluidBlock)) && (!(collection_block instanceof BlockLiquid))) return false;
      final Fluid fluid_to_collect = FluidRegistry.lookupFluidForBlock(collection_block);
      if(fluid_to_collect == null) return false; // not sure if that can return null
      if((tank_!=null) && (!tank_.getFluid().equals(fluid_to_collect))) return false;
      if(try_pick(collection_pos)) { last_pick_pos_ = collection_pos; return true; } // Blocks directly always first. Allows water source blocks to recover/reflow to source blocks.
      // Not picked, not a source block -> search highest allowed source block to pick
      // ---------------------------------------------------------------------------------------------------------------
      // Plan is to pick preferably surface blocks whilst not wasting much mem and cpu. Blocks will dynamically change due to flowing.
      // Basic assumptions: fluid flows straight (not diagonal) and not up the hill. Only falling fluid streams are long, lateral streams are about max 16 blocks (water 8).
      // Problem resolving: Threre are fluids going all over the place, and do not sometimes continue flowing without a source block.
      //                    - Stack based trail tracking is used
      //                    - Turtle motion with reset on fail, preferrs up, remaining motion order is shuffled to pick not in the same direction.
      //                    - Calculation time capping, reset on fail, extended search for far blocks (e.g. stream from a high position) on many fails.
      //                    - Preferrs fluid surface blocks if possible and anough calculation time left.
      //                    - On fail, replace last flowing block with air and cause a block update, in case previous block updates or strange fluid block behaviour does not prevent advancing.
      //                    - Assumption is: The search can go much further than fluids can flow, except top-bottom falling streams. so
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
          if(fluid_to_collect.equals(get_fluid(p))) {
            ++num_adjacent;
            pos = p;
            trail.push(pos);
            if(steps < MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE/2) {
              // check for same fluid above (only source blocks)
              final int max_surface_search = (MAX_TRACKING_STEPS_PER_CYCLE_INTENSIVE/2)-steps;
              for(int k=0; k<max_surface_search; ++k) {
                if(!can_pick(pos.up(), fluid_to_collect)) break;
                pos = pos.up();
                trail.push(pos);
              }
            }
            if(try_pick(pos)) {
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
      if(intensive_search_counter_ > 2) world.setBlockToAir(pos);
      last_pick_pos_ = collection_pos;
      search_offsets_ = null; // try other search order
      ++no_fluid_found_counter_;
      return false;
    }

    public void update()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = TICK_INTERVAL;
      collection_timer_ += TICK_INTERVAL;
      boolean dirty = false;
      // Collection
      if((collection_timer_ >= COLLECTION_INTERVAL) && ((tank_==null) || (tank_.amount <= (TANK_CAPACITY-1000)))) {
      collection_timer_ = 0;
        if(!world.isBlockPowered(pos)) { // redstone disable feature
          if(last_pick_pos_==null) last_pick_pos_ = pos.up();
          if(try_collect(pos.up())) dirty = true;
        }
      }
      // Gravity fluid transfer
      if((tank_!=null) && (tank_.amount >= 1000)) {
        IFluidHandler fh = FluidUtil.getFluidHandler(world, pos.down(), EnumFacing.UP);
        if(fh != null) {
          FluidStack fs = new FluidStack(tank_.getFluid(), 1000);
          int nfilled = MathHelper.clamp(fh.fill(fs, true), 0, 1000);
          tank_.amount -= nfilled;
          if(tank_.amount <= 0) tank_ = null;
          dirty = true;
        }
      }
      // Block state
      int fill_level = (tank_==null) ? 0 : (MathHelper.clamp(tank_.amount/1000,0,FILL_LEVEL_MAX));
      final IBlockState funnel_state = world.getBlockState(pos);
      if(funnel_state.getValue(FILL_LEVEL) != fill_level) world.setBlockState(pos, funnel_state.withProperty(FILL_LEVEL, fill_level), 2|16);
      if(dirty) markDirty();
    }
  }
}
