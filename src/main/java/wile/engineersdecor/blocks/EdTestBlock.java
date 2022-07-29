/*
 * @file EdTestBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Creative mod testing block
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.*;


import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EdTestBlock
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestBlock extends StandardBlocks.Directed implements StandardEntityBlocks.IStandardEntityBlock<TestTileEntity>, Auxiliaries.IExperimentalFeature
  {
    public TestBlock(long config, BlockBehaviour.Properties builder, final AABB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return Shapes.block(); }

    @Override
    @SuppressWarnings("deprecation") // public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) { return true; }
    public boolean isSignalSource(BlockState p_60571_)
    { return true; }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, BlockEntity te, boolean explosion)
    { return Collections.singletonList(new ItemStack(this)); }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof TestTileEntity)) return InteractionResult.FAIL;
      return ((TestTileEntity)te).activated(player, hand, hit) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestTileEntity extends StandardEntityBlocks.StandardBlockEntity
  {
    private final RfEnergy.Battery battery_;
    private final LazyOptional<IEnergyStorage> energy_handler_;
    private final Fluidics.Tank tank_;
    private final LazyOptional<IFluidHandler> fluid_handler_;
    private final Inventories.StorageInventory inventory_;
    private final LazyOptional<IItemHandler> item_handler_;
    private int tick_timer = 0;
    private int rf_fed_avg = 0;
    private int rf_fed_total = 0;
    private int rf_fed_acc = 0;
    private int rf_received_avg = 0;
    private int rf_received_total = 0;
    private int liq_filled_avg = 0;
    private int liq_filled_total = 0;
    private int liq_filled_acc = 0;
    private int liq_received_avg = 0;
    private int liq_received_total = 0;
    private int items_inserted_total = 0;
    private int items_received_total = 0;
    private int rf_feed_setting = 4096;
    private FluidStack liq_fill_stack = new FluidStack(Fluids.WATER, 128);
    private ItemStack insertion_item = ItemStack.EMPTY;
    private Direction block_facing = Direction.NORTH;
    private boolean paused = false;


    public TestTileEntity(BlockPos pos, BlockState state)
    {
      super(ModContent.getBlockEntityTypeOfBlock(state.getBlock()), pos, state);
      battery_ = new RfEnergy.Battery((int)1e9, (int)1e9, 0, 0);
      energy_handler_ = battery_.createEnergyHandler();
      tank_ = new Fluidics.Tank((int)1e9);
      fluid_handler_ = tank_.createFluidHandler();
      inventory_ = new Inventories.StorageInventory(this, 1);
      item_handler_ = Inventories.MappedItemHandler.createInsertionHandler(inventory_);
    }

    @Override
    public void load(CompoundTag nbt)
    {
      super.load(nbt);
      tank_.load(nbt);
      battery_.load(nbt);
      rf_fed_avg = nbt.getInt("rf_fed_avg");
      rf_fed_total = nbt.getInt("rf_fed_total");
      rf_fed_acc = nbt.getInt("rf_fed_acc");
      rf_received_avg = nbt.getInt("rf_received_avg");
      rf_received_total = nbt.getInt("rf_received_total");
      liq_filled_avg = nbt.getInt("liq_filled_avg");
      liq_filled_total = nbt.getInt("liq_filled_total");
      liq_filled_acc = nbt.getInt("liq_filled_acc");
      liq_received_avg = nbt.getInt("liq_received_avg");
      liq_received_total = nbt.getInt("liq_received_total");
      rf_feed_setting = nbt.getInt("rf_feed_setting");
      items_received_total = nbt.getInt("items_received_total");
      items_inserted_total = nbt.getInt("items_inserted_total");
      if(nbt.contains("liq_fill_stack")) liq_fill_stack = FluidStack.loadFluidStackFromNBT(nbt.getCompound("liq_fill_stack"));
      if(nbt.contains("insertion_item")) insertion_item = ItemStack.of(nbt.getCompound("insertion_item"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    {
      super.saveAdditional(nbt);
      tank_.save(nbt);
      battery_.save(nbt);
      nbt.putInt("rf_fed_avg", rf_fed_avg);
      nbt.putInt("rf_fed_total", rf_fed_total);
      nbt.putInt("rf_fed_acc", rf_fed_acc);
      nbt.putInt("rf_received_avg", rf_received_avg);
      nbt.putInt("rf_received_total", rf_received_total);
      nbt.putInt("liq_filled_avg", liq_filled_avg);
      nbt.putInt("liq_filled_total", liq_filled_total);
      nbt.putInt("liq_filled_acc", liq_filled_acc);
      nbt.putInt("liq_received_avg", liq_received_avg);
      nbt.putInt("liq_received_total", liq_received_total);
      nbt.putInt("rf_feed_setting", rf_feed_setting);
      nbt.putInt("items_received_total", items_received_total);
      nbt.putInt("items_inserted_total", items_inserted_total);
      if(!liq_fill_stack.isEmpty()) nbt.put("liq_fill_stack", liq_fill_stack.writeToNBT(new CompoundTag()));
      if(!insertion_item.isEmpty()) nbt.put("insertion_item", insertion_item.save(new CompoundTag()));
    }

    private FluidStack getFillFluid(ItemStack stack)
    {
      // intentionally not item fluid handler, only specific items.
      if(stack.getItem() == Items.WATER_BUCKET) return new FluidStack(Fluids.WATER, 1000);
      if(stack.getItem() == Items.LAVA_BUCKET) return new FluidStack(Fluids.LAVA, 1000);
      return FluidStack.EMPTY;
    }

    private ItemStack getRandomItemstack()
    {
      final int n = (int)Math.floor(Math.random() * ForgeRegistries.ITEMS.getValues().size());
      ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValues().stream().skip(n).findAny().orElse(Items.COBBLESTONE));
      stack.setCount((int)Math.floor(Math.random() * stack.getMaxStackSize()));
      return stack;
    }

    public boolean activated(Player player, InteractionHand hand, BlockHitResult hit)
    {
      final ItemStack held = player.getItemInHand(hand);
      if(held.isEmpty()) {
        ArrayList<String> msgs = new ArrayList<>();
        if(rf_fed_avg > 0) msgs.add("-" + rf_fed_avg + "rf/t");
        if(rf_fed_total > 0) msgs.add("-" + rf_fed_total + "rf");
        if(rf_received_avg > 0) msgs.add("+" + rf_received_avg + "rf/t");
        if(rf_received_total > 0) msgs.add("+" + rf_received_total + "rf");
        if(liq_filled_avg > 0) msgs.add("-" + liq_filled_avg + "mb/t");
        if(liq_filled_total > 0) msgs.add("-" + liq_filled_total + "mb");
        if(liq_received_avg > 0) msgs.add("+" + liq_received_avg + "mb/t");
        if(liq_received_total > 0) msgs.add("+" + liq_received_total + "mb");
        if(items_received_total > 0) msgs.add("+" + items_received_total + "items");
        if(items_inserted_total > 0) msgs.add("-" + items_inserted_total + "items");
        if(msgs.isEmpty()) msgs.add("Nothing transferred yet.");
        Overlay.show(player, Component.literal(String.join(" | ", msgs)), 1000);
        return true;
      } else if(paused) {
        if(!getFillFluid(held).isEmpty()) {
          FluidStack fs = getFillFluid(held);
          if(liq_fill_stack.isEmpty() || !liq_fill_stack.isFluidEqual(fs)) {
            fs.setAmount(128);
            liq_fill_stack = fs;
          } else {
            int amount = liq_fill_stack.getAmount() * 2;
            if(amount > 4096) amount = 16;
            liq_fill_stack.setAmount(amount);
          }
          if(liq_fill_stack.isEmpty()) {
            Overlay.show(player, Component.literal("Fluid fill: none"), 1000);
          } else {
            Overlay.show(player, Component.literal("Fluid fill: " + liq_fill_stack.getAmount() + "mb/t of " + Auxiliaries.getResourceLocation(liq_fill_stack.getFluid())), 1000);
          }
        } else if(held.getItem() == Items.REDSTONE) {
          rf_feed_setting = (rf_feed_setting<<1) & 0x00fffff0;
          if(rf_feed_setting == 0) rf_feed_setting = 0x10;
          Overlay.show(player, Component.literal("RF feed rate: " + rf_feed_setting + "rf/t"), 1000);
        } else {
          BlockState adjacent_state = level.getBlockState(worldPosition.relative(block_facing));
          if(adjacent_state.getBlock()==Blocks.HOPPER || adjacent_state.getBlock()==ModContent.getBlock("factory_hopper")) {
            insertion_item = held.copy();
            Overlay.show(player, Component.literal("Insertion item: " + (insertion_item.getItem()==Items.LEVER ? "random" : insertion_item.toString()) + "/s"), 1000);
          }
        }
        return true;
      } else {
        return false;
      }
    }

    @Override
    public void setRemoved()
    {
      super.setRemoved();
      energy_handler_.invalidate();
      fluid_handler_.invalidate();
      item_handler_.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if((!paused) && (facing != block_facing)) {
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluid_handler_.cast();
        if(capability == CapabilityEnergy.ENERGY) return energy_handler_.cast();
        if(capability ==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return item_handler_.cast();
      }
      return super.getCapability(capability, facing);
    }

    @Override
    public void tick()
    {
      if(level.isClientSide()) return;
      block_facing = getBlockState().getValue(TestBlock.FACING);
      paused = level.hasNeighborSignal(getBlockPos());
      if(!paused) {
        boolean dirty = false;
        {
          int p = RfEnergy.feed(getLevel(), getBlockPos().relative(block_facing), block_facing.getOpposite(), rf_feed_setting);
          rf_fed_acc += p;
          dirty |= p>0;
        }
        if(!liq_fill_stack.isEmpty()) {
          int f = Fluidics.fill(getLevel(), getBlockPos().relative(block_facing), block_facing.getOpposite(), liq_fill_stack);
          liq_filled_acc += f;
          dirty |= f>0;
        }
        if(!inventory_.isEmpty()) {
          int i = inventory_.getItem(0).getCount();
          items_received_total += i;
          inventory_.clearContent();
          dirty |= i>0;
        }
        if((tick_timer == 1) && (!insertion_item.isEmpty())) {
          BlockState adjacent_state = level.getBlockState(worldPosition.relative(block_facing));
          ItemStack stack = (insertion_item.getItem()==Items.LEVER) ? getRandomItemstack() : insertion_item.copy();
          if(adjacent_state.getBlock()==Blocks.HOPPER || adjacent_state.getBlock()==ModContent.getBlock("factory_hopper")) {
            ItemStack remaining = Inventories.insert(getLevel(), getBlockPos().relative(block_facing), block_facing.getOpposite(), stack, false);
            int n = stack.getCount() - remaining.getCount();
            items_inserted_total += n;
            dirty |= n>0;
          }
        }
        if(dirty) {
          setChanged();
        }
      }
      if(--tick_timer <= 0) {
        tick_timer = 20;
        rf_fed_avg = rf_fed_acc/20;
        rf_fed_total += rf_fed_acc;
        rf_fed_acc = 0;
        rf_received_avg = battery_.getEnergyStored()/20;
        rf_received_total += battery_.getEnergyStored();
        battery_.clear();
        liq_received_avg = tank_.getFluidAmount();
        liq_received_total += tank_.getFluidAmount();
        tank_.clear();
        liq_filled_avg   = (liq_fill_stack.isEmpty()) ? 0 : (liq_filled_acc/20);
        liq_filled_total = (liq_fill_stack.isEmpty()) ? 0 : (liq_filled_total+liq_filled_acc);
        liq_filled_acc = 0;
      }
    }

  }
}
