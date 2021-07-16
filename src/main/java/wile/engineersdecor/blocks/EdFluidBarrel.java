/*
 * @file EdFluidBarrel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Simple fluid tank with a built-in gauge.
 */
package wile.engineersdecor.blocks;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.*;
import net.minecraft.world.IWorldReader;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.Fluidics;
import wile.engineersdecor.libmc.detail.Overlay;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;



public class EdFluidBarrel
{
  //--------------------------------------------------------------------------------------------------------------------
  // Config
  //--------------------------------------------------------------------------------------------------------------------

  private static int capacity_ = 12000;
  private static int item_fluid_handler_transfer_rate_ = 1000;
  private static int tile_fluid_handler_transfer_rate_ = 1000;

  public static void on_config(int tank_capacity, int transfer_rate)
  {
    capacity_ = MathHelper.clamp(tank_capacity, 2000, 64000);
    tile_fluid_handler_transfer_rate_ = MathHelper.clamp(tank_capacity, 50, 4096);
    item_fluid_handler_transfer_rate_ = tile_fluid_handler_transfer_rate_;
    ModConfig.log("Config fluid barrel: capacity:" + capacity_ + "mb, transfer-rate:" + tile_fluid_handler_transfer_rate_ + "mb/t.");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidBarrelBlock extends DecorBlock.DirectedWaterLoggable implements IDecorBlock, StandardBlocks.IBlockItemFactory
  {
    public static final int FILL_LEVEL_MAX = 4;
    public static final IntegerProperty FILL_LEVEL = IntegerProperty.create("level", 0, FILL_LEVEL_MAX);

    public FluidBarrelBlock(long config, AbstractBlock.Properties builder, final AxisAlignedBB[] unrotatedAABB)
    {
      super(config, builder, unrotatedAABB);
      registerDefaultState(super.defaultBlockState().setValue(FACING, Direction.UP).setValue(FILL_LEVEL, 0));
    }

    // IBlockItemFactory ----------------------------------------------------------------------------

    @Override
    public BlockItem getBlockItem(Block block, Item.Properties builder)
    { return new FluidBarrelItem(block, builder); }

    // IStandardBlock --------------------------------------------------------------------------------

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, final TileEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<ItemStack>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof FluidBarrelTileEntity)) return stacks;
      ItemStack stack = new ItemStack(this, 1);
      CompoundNBT te_nbt = ((FluidBarrelTileEntity) te).clear_getnbt();
      if(!te_nbt.isEmpty()) {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("tedata", te_nbt);
        stack.setTag(nbt);
      }
      stacks.add(stack);
      return stacks;
    }

    // Block/IForgeBlock -----------------------------------------------------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
      if(
        (!(stack.getItem() instanceof FluidBarrelItem)) ||
        (Auxiliaries.Tooltip.helpCondition())
      ) {
        super.appendHoverText(stack, world, tooltip, flag); return;
      }
      FluidStack fs = FluidBarrelItem.getFluid(stack);
      if(!fs.isEmpty()) {
        tooltip.add(Auxiliaries.localizable(getDescriptionId()+".status.tip", new Object[] {
          Integer.toString(fs.getAmount()),
          Integer.toString(capacity_),
          new TranslationTextComponent(fs.getTranslationKey())
        }));
      } else {
        tooltip.add(Auxiliaries.localizable(getDescriptionId()+".status.tip.empty", new Object[] {
          "0",
          Integer.toString(capacity_),
        }));
      }
      if(!Auxiliaries.Tooltip.extendedTipCondition()) {
        super.appendHoverText(stack, world, tooltip, flag);
      }
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new EdFluidBarrel.FluidBarrelTileEntity(); }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(FILL_LEVEL); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(!context.getPlayer().isShiftKeyDown()) state = state.setValue(FACING, Direction.UP);
      return state;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundNBT te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof FluidBarrelTileEntity)) return;
      ((FluidBarrelTileEntity)te).readnbt(te_nbt);
      ((FluidBarrelTileEntity)te).setChanged();
      world.getBlockTicks().scheduleTick(pos, this, 4);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
      if(player.getItemInHand(hand).getItem() == asItem()) return ActionResultType.PASS; // Pass that to block placement.
      if(world.isClientSide()) return ActionResultType.SUCCESS;
      TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof FluidBarrelTileEntity)) return ActionResultType.FAIL;
      if(!((FluidBarrelTileEntity)te).handlePlayerInteraction(state, world, pos, player, hand)) return ActionResultType.PASS;
      world.getBlockTicks().scheduleTick(pos, this, 4);
      return ActionResultType.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos)
    {
      TileEntity te = world.getBlockEntity(pos);
      if(!(te instanceof FluidBarrelTileEntity)) return 0;
      return (int)MathHelper.clamp(((FluidBarrelTileEntity)te).getNormalizedFillLevel() * 15, 0, 15);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
    { return false; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidBarrelTileEntity extends TileEntity implements ICapabilityProvider, ITickableTileEntity
  {
    private final int TICK_INTERVAL = 10;
    private int tick_timer_ = 0;
    private final Fluidics.Tank tank_;
    private final LazyOptional<IFluidHandler> fluid_handler_;

    public FluidBarrelTileEntity()
    { this(ModContent.TET_FLUID_BARREL); }

    public FluidBarrelTileEntity(TileEntityType<?> te_type)
    {
      super(te_type);
      tank_ = new Fluidics.Tank(capacity_);
      tank_.setInteractionNotifier((t,d)->on_tank_changed());
      fluid_handler_ = tank_.createFluidHandler();
    }

    public void readnbt(CompoundNBT nbt)
    { tank_.load(nbt); }

    public CompoundNBT writenbt(CompoundNBT nbt)
    { tank_.save(nbt); return nbt; }

    public boolean handlePlayerInteraction(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand)
    {
      if(world.isClientSide()) return false;
      {
        Tuple<Fluid,Integer> transferred = Fluidics.manualTrackedFluidHandlerInteraction(world, pos, null, player, hand);
        if(transferred==null) {
          world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.2f, 0.02f);
        } else if(transferred.getB() > 0) {
          SoundEvent se = (transferred.getA()==Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA: SoundEvents.BUCKET_EMPTY;
          world.playSound(null, pos, se, SoundCategory.BLOCKS, 1f, 1f);
        } else {
          SoundEvent se = (transferred.getA()==Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
          world.playSound(null, pos, se, SoundCategory.BLOCKS, 1f, 1f);
        }
      }
      {
        int vol = tank_.getFluidAmount();
        int cap = tank_.getCapacity();
        String name = (new TranslationTextComponent(tank_.getFluid().getTranslationKey())).getString();
        if((vol>0) && (cap>0)) {
          Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.fluid_barrel.status", new Object[]{
            Integer.toString(vol), Integer.toString(cap), name
          }));
        } else {
          Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.fluid_barrel.status.empty", new Object[]{
            Integer.toString(vol), Integer.toString(cap)
          }));
        }
      }
      return true;
    }

    public double getNormalizedFillLevel()
    { return (tank_.isEmpty()) ? (0) : ((double)tank_.getFluidAmount()/(double)tank_.getCapacity()); }

    protected void on_tank_changed()
    { if(tick_timer_ > 2) tick_timer_ = 2; }

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void load(BlockState state, CompoundNBT nbt)
    { super.load(state, nbt); readnbt(nbt); }

    @Override
    public CompoundNBT save(CompoundNBT nbt)
    { super.save(nbt); return writenbt(nbt); }

    @Override
    public void setRemoved()
    { super.setRemoved(); fluid_handler_.invalidate(); }

    public CompoundNBT clear_getnbt()
    { return tank_.save(new CompoundNBT()); }

    // ICapabilityProvider --------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluid_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // ITickableTileEntity --------------------------------------------------------------------

    private boolean transfer_down()
    {
      if(tank_.isEmpty()) return false;
      final IFluidHandler fh = FluidUtil.getFluidHandler(level, worldPosition.below(), Direction.UP).orElse(null);
      if(fh==null) return false;
      final FluidStack fs = tank_.getFluid().copy();
      if(fs.getAmount() > tile_fluid_handler_transfer_rate_) fs.setAmount(tile_fluid_handler_transfer_rate_);
      final int nfilled = fh.fill(fs, FluidAction.EXECUTE);
      if(nfilled <= 0) return false;
      tank_.drain(nfilled, FluidAction.EXECUTE);
      return true;
    }

    public void tick()
    {
      if((level.isClientSide()) || (--tick_timer_>=0)) return;
      tick_timer_ = TICK_INTERVAL;
      final BlockState state = getBlockState();
      final Block block = state.getBlock();
      if(!(block instanceof FluidBarrelBlock)) return;
      if(state.getValue(FluidBarrelBlock.FACING).getAxis().isVertical()) transfer_down(); // tick_timer_ ==> 1 if something was transferred, otherwise no need to waste CPU
      double norm_level = getNormalizedFillLevel();
      int fill_level = (norm_level <= 0) ? 0 : ((int)MathHelper.clamp((norm_level * FluidBarrelBlock.FILL_LEVEL_MAX)+0.5, 1, FluidBarrelBlock.FILL_LEVEL_MAX));
      if(fill_level != state.getValue(FluidBarrelBlock.FILL_LEVEL)) {
        level.setBlock(worldPosition, state.setValue(FluidBarrelBlock.FILL_LEVEL, fill_level), 2);
        level.updateNeighborsAt(worldPosition, block);
      }
    }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block item
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidBarrelItem extends BlockItem
  {
    public FluidBarrelItem(Block block, Item.Properties builder)
    { super(block, builder); }

    private static CompoundNBT read_fluid_nbt(ItemStack stack)
    {
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return new CompoundNBT();
      final CompoundNBT nbt = stack.getTag().getCompound("tedata");
      if(!nbt.contains("tank", Constants.NBT.TAG_COMPOUND)) return new CompoundNBT();
      return nbt.getCompound("tank");
    }

    private static void write_fluid_nbt(ItemStack stack, CompoundNBT fluid_nbt)
    {
      if((fluid_nbt==null) || (fluid_nbt.isEmpty())) {
        if((!stack.hasTag()) || (!stack.getTag().contains("tedata", Constants.NBT.TAG_COMPOUND))) return;
        final CompoundNBT tag = stack.getTag();
        final CompoundNBT tedata = tag.getCompound("tedata");
        if(tedata.contains("tank")) tedata.remove("tank");
        if(tedata.isEmpty()) tag.remove("tedata");
        stack.setTag(tag.isEmpty() ? null : tag);
      } else {
        CompoundNBT tag = stack.getTag();
        if(tag==null) tag = new CompoundNBT();
        CompoundNBT tedata = tag.getCompound("tedata");
        if(tedata==null) tedata = new CompoundNBT();
        tedata.put("tank", fluid_nbt);
        tag.put("tedata", tedata);
        stack.setTag(tag);
      }
    }

    public static FluidStack getFluid(ItemStack stack)
    {
      final CompoundNBT nbt = read_fluid_nbt(stack);
      return (nbt.isEmpty()) ? (FluidStack.EMPTY) : (FluidStack.loadFluidStackFromNBT(nbt));
    }

    public static ItemStack setFluid(ItemStack stack, FluidStack fs)
    { write_fluid_nbt(stack, fs.writeToNBT(new CompoundNBT())); return stack; }

    @Override
    public int getItemStackLimit(ItemStack stack)
    { return (!getFluid(stack).isEmpty()) ? 1 : super.getItemStackLimit(stack); }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    { return (!getFluid(stack).isEmpty()); }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    { return 1.0 - MathHelper.clamp(((double)(getFluid(stack).getAmount()))/((double)capacity_), 0.0, 1.0); }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack)
    { return 0x336633; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt)
    { return new Fluidics.FluidContainerItemCapabilityWrapper(stack, capacity_, item_fluid_handler_transfer_rate_, (s)->read_fluid_nbt(s), (s,n)->write_fluid_nbt(s,n), e->true); }

    @Override
    public boolean hasContainerItem(ItemStack stack)
    { return (stack.getCount()==1) && (!getFluid(stack).isEmpty()); }

    @Override
    public ItemStack getContainerItem(ItemStack stack)
    {
      if(stack.getCount()!=1) return ItemStack.EMPTY;
      FluidStack fs = getFluid(stack);
      if(fs.getAmount() > 1000) fs.shrink(1000); else fs = FluidStack.EMPTY;
      return setFluid(stack, fs);
    }
  }

}
