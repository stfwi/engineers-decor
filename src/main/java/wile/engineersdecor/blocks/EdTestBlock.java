/*
 * @file EdTestBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Creative mod testing block
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
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
import wile.engineersdecor.libmc.detail.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EdTestBlock
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestBlock extends DecorBlock.Directed implements Auxiliaries.IExperimentalFeature, IDecorBlock
  {
    public TestBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.fullCube(); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new TestTileEntity(); }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
    { return true; }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, TileEntity te, boolean explosion)
    { return Collections.singletonList(new ItemStack(this)); }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      if(world.isRemote()) return ActionResultType.SUCCESS;
      TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof TestTileEntity)) return ActionResultType.FAIL;
      return ((TestTileEntity)te).activated(player, hand, hit) ? ActionResultType.CONSUME : ActionResultType.PASS;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestTileEntity extends TileEntity implements ITickableTileEntity
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

    public TestTileEntity()
    { this(ModContent.TET_TEST_BLOCK); }

    public TestTileEntity(TileEntityType<?> te_type)
    {
      super(te_type);
      battery_ = new RfEnergy.Battery((int)1e9, (int)1e9, 0, 0);
      energy_handler_ = battery_.createEnergyHandler();
      tank_ = new Fluidics.Tank((int)1e9);
      fluid_handler_ = tank_.createFluidHandler();
      inventory_ = new Inventories.StorageInventory(this, 1);
      item_handler_ = Inventories.MappedItemHandler.createInsertionHandler(inventory_);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
      super.read(state, nbt);
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
      if(nbt.contains("insertion_item")) insertion_item = ItemStack.read(nbt.getCompound("insertion_item"));
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
      super.write(nbt);
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
      if(!liq_fill_stack.isEmpty()) nbt.put("liq_fill_stack", liq_fill_stack.writeToNBT(new CompoundNBT()));
      if(!insertion_item.isEmpty()) nbt.put("insertion_item", insertion_item.write(new CompoundNBT()));
      return nbt;
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

    public boolean activated(PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      final ItemStack held = player.getHeldItem(hand);
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
        Overlay.show(player, new StringTextComponent(String.join(" | ", msgs)), 1000);
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
            Overlay.show(player, new StringTextComponent("Fluid fill: none"), 1000);
          } else {
            Overlay.show(player, new StringTextComponent("Fluid fill: " + liq_fill_stack.getAmount() + "mb/t of " + liq_fill_stack.getFluid().getRegistryName()), 1000);
          }
        } else if(held.getItem() == Items.REDSTONE) {
          rf_feed_setting = (rf_feed_setting<<1) & 0x00fffff0;
          if(rf_feed_setting == 0) rf_feed_setting = 0x10;
          Overlay.show(player, new StringTextComponent("RF feed rate: " + rf_feed_setting + "rf/t"), 1000);
        } else {
          BlockState adjacent_state = world.getBlockState(pos.offset(block_facing));
          if(adjacent_state.getBlock()==Blocks.HOPPER || adjacent_state.getBlock()==ModContent.FACTORY_HOPPER) {
            insertion_item = held.copy();
            Overlay.show(player, new StringTextComponent("Insertion item: " + (insertion_item.getItem()==Items.LEVER ? "random" : insertion_item.toString()) + "/s"), 1000);
          }
        }
        return true;
      } else {
        return false;
      }
    }

    @Override
    public void remove()
    {
      super.remove();
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
      if(world.isRemote()) return;
      block_facing = getBlockState().get(TestBlock.FACING);
      paused = world.isBlockPowered(getPos());
      if(!paused) {
        boolean dirty = false;
        {
          int p = RfEnergy.feed(getWorld(), getPos().offset(block_facing), block_facing.getOpposite(), rf_feed_setting);
          rf_fed_acc += p;
          dirty |= p>0;
        }
        if(!liq_fill_stack.isEmpty()) {
          int f = Fluidics.fill(getWorld(), getPos().offset(block_facing), block_facing.getOpposite(), liq_fill_stack);
          liq_filled_acc += f;
          dirty |= f>0;
        }
        if(!inventory_.isEmpty()) {
          int i = inventory_.getStackInSlot(0).getCount();
          items_received_total += i;
          inventory_.clear();
          dirty |= i>0;
        }
        if((tick_timer == 1) && (!insertion_item.isEmpty())) {
          BlockState adjacent_state = world.getBlockState(pos.offset(block_facing));
          ItemStack stack = (insertion_item.getItem()==Items.LEVER) ? getRandomItemstack() : insertion_item.copy();
          if(adjacent_state.getBlock()==Blocks.HOPPER || adjacent_state.getBlock()==ModContent.FACTORY_HOPPER) {
            ItemStack remaining = Inventories.insert(getWorld(), getPos().offset(block_facing), block_facing.getOpposite(), stack, false);
            int n = stack.getCount() - remaining.getCount();
            items_inserted_total += n;
            dirty |= n>0;
          }
        }
        if(dirty) {
          markDirty();
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
