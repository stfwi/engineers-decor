/*
 * @file EdFluidBarrel.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Simple fluid tank with a built-in gauge.
 */
package wile.engineersdecor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import wile.engineersdecor.ModConfig;
import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.StandardBlocks;
import wile.engineersdecor.libmc.StandardEntityBlocks;
import wile.engineersdecor.libmc.Auxiliaries;
import wile.engineersdecor.libmc.Fluidics;
import wile.engineersdecor.libmc.Overlay;

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
    capacity_ = Mth.clamp(tank_capacity, 2000, 64000);
    tile_fluid_handler_transfer_rate_ = Mth.clamp(tank_capacity, 50, 4096);
    item_fluid_handler_transfer_rate_ = tile_fluid_handler_transfer_rate_;
    ModConfig.log("Config fluid barrel: capacity:" + capacity_ + "mb, transfer-rate:" + tile_fluid_handler_transfer_rate_ + "mb/t.");
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidBarrelBlock extends StandardBlocks.DirectedWaterLoggable implements StandardEntityBlocks.IStandardEntityBlock<FluidBarrelTileEntity>
  {
    public static final int FILL_LEVEL_MAX = 4;
    public static final IntegerProperty FILL_LEVEL = IntegerProperty.create("level", 0, FILL_LEVEL_MAX);

    public FluidBarrelBlock(long config, BlockBehaviour.Properties builder, final AABB[] unrotatedAABB)
    {
      super(config, builder, unrotatedAABB);
      registerDefaultState(super.defaultBlockState().setValue(FACING, Direction.UP).setValue(FILL_LEVEL, 0));
    }

    @Override
    public boolean isBlockEntityTicking(Level world, BlockState state)
    { return true; }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, Level world, final BlockEntity te, boolean explosion)
    {
      final List<ItemStack> stacks = new ArrayList<>();
      if(world.isClientSide) return stacks;
      if(!(te instanceof FluidBarrelTileEntity)) return stacks;
      ItemStack stack = new ItemStack(this, 1);
      CompoundTag te_nbt = ((FluidBarrelTileEntity) te).clear_getnbt();
      if(!te_nbt.isEmpty()) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("tedata", te_nbt);
        stack.setTag(nbt);
      }
      stacks.add(stack);
      return stacks;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
    {
      if((!(stack.getItem() instanceof FluidBarrelItem)) || (Auxiliaries.Tooltip.helpCondition())) {
        super.appendHoverText(stack, world, tooltip, flag); return;
      }
      FluidStack fs = FluidBarrelItem.getFluid(stack);
      if(!fs.isEmpty()) {
        tooltip.add(Auxiliaries.localizable(getDescriptionId()+".status.tip", Integer.toString(fs.getAmount()), Integer.toString(capacity_), Component.translatable(fs.getTranslationKey())));
      } else {
        tooltip.add(Auxiliaries.localizable(getDescriptionId()+".status.tip.empty", "0", Integer.toString(capacity_)));
      }
      if(!Auxiliaries.Tooltip.extendedTipCondition()) {
        super.appendHoverText(stack, world, tooltip, flag);
      }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
    { return Shapes.block(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    { super.createBlockStateDefinition(builder); builder.add(FILL_LEVEL); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if(!context.getPlayer().isShiftKeyDown()) state = state.setValue(FACING, Direction.UP);
      return state;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
      if(world.isClientSide) return;
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return;
      CompoundTag te_nbt = stack.getTag().getCompound("tedata");
      if(te_nbt.isEmpty()) return;
      final BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof FluidBarrelTileEntity)) return;
      ((FluidBarrelTileEntity)te).readnbt(te_nbt);
      te.setChanged();
      world.scheduleTick(pos, this, 4);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult)
    {
      if(player.getItemInHand(hand).getItem() == asItem()) return InteractionResult.PASS; // Pass that to block placement.
      if(world.isClientSide()) return InteractionResult.SUCCESS;
      if(!(world.getBlockEntity(pos) instanceof final FluidBarrelTileEntity te)) return InteractionResult.FAIL;
      if(!te.handlePlayerInteraction(state, world, pos, player, hand)) return InteractionResult.PASS;
      world.scheduleTick(pos, this, 4);
      return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state)
    { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos)
    {
      BlockEntity te = world.getBlockEntity(pos);
      if(!(te instanceof FluidBarrelTileEntity)) return 0;
      return (int)Mth.clamp(((FluidBarrelTileEntity)te).getNormalizedFillLevel() * 15, 0, 15);
    }

    @Override
    public boolean shouldCheckWeakPower(BlockState state, LevelReader world, BlockPos pos, Direction side)
    { return false; }

  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class FluidBarrelTileEntity extends StandardEntityBlocks.StandardBlockEntity implements ICapabilityProvider
  {
    private final int TICK_INTERVAL = 10;
    private int tick_timer_ = 0;
    private final Fluidics.Tank tank_ = (new Fluidics.Tank(capacity_)).setInteractionNotifier((t,d)->on_tank_changed());
    private final LazyOptional<IFluidHandler> fluid_handler_ = tank_.createFluidHandler();

    public FluidBarrelTileEntity(BlockPos pos, BlockState state)
    { super(ModContent.getBlockEntityTypeOfBlock(state.getBlock()), pos, state); }

    public void readnbt(CompoundTag nbt)
    { tank_.load(nbt); }

    public CompoundTag writenbt(CompoundTag nbt)
    { tank_.save(nbt); return nbt; }

    public boolean handlePlayerInteraction(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand)
    {
      if(world.isClientSide()) return false;
      {
        Tuple<Fluid,Integer> transferred = Fluidics.manualTrackedFluidHandlerInteraction(world, pos, null, player, hand);
        if(transferred==null) {
          world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.2f, 0.02f);
        } else if(transferred.getB() > 0) {
          SoundEvent se = (transferred.getA()==Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA: SoundEvents.BUCKET_EMPTY;
          world.playSound(null, pos, se, SoundSource.BLOCKS, 1f, 1f);
        } else {
          SoundEvent se = (transferred.getA()==Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
          world.playSound(null, pos, se, SoundSource.BLOCKS, 1f, 1f);
        }
      }
      {
        int vol = tank_.getFluidAmount();
        int cap = tank_.getCapacity();
        String name = (Component.translatable(tank_.getFluid().getTranslationKey())).getString();
        if((vol>0) && (cap>0)) {
          Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.fluid_barrel.status", Integer.toString(vol), Integer.toString(cap), name));
        } else {
          Overlay.show(player, Auxiliaries.localizable("block.engineersdecor.fluid_barrel.status.empty", Integer.toString(vol), Integer.toString(cap)));
        }
      }
      return true;
    }

    public double getNormalizedFillLevel()
    { return (tank_.isEmpty()) ? (0) : ((double)tank_.getFluidAmount()/(double)tank_.getCapacity()); }

    protected void on_tank_changed()
    { if(tick_timer_ > 2) tick_timer_ = 2; }

    // BlockEntity ------------------------------------------------------------------------------

    @Override
    public void load(CompoundTag nbt)
    { super.load(nbt); readnbt(nbt); }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    { super.saveAdditional(nbt); writenbt(nbt); }

    @Override
    public void setRemoved()
    { super.setRemoved(); fluid_handler_.invalidate(); }

    public CompoundTag clear_getnbt()
    { return tank_.save(new CompoundTag()); }

    // ICapabilityProvider --------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing)
    {
      if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return fluid_handler_.cast();
      return super.getCapability(capability, facing);
    }

    // Tick --------------------------------------------------------------------

    private boolean transfer_down()
    {
      if(tank_.isEmpty()) return false;
      final IFluidHandler fh = Fluidics.handler(level, worldPosition.below(), Direction.UP);
      if(fh==null) return false;
      final FluidStack fs = tank_.getFluid().copy();
      if(fs.getAmount() > tile_fluid_handler_transfer_rate_) fs.setAmount(tile_fluid_handler_transfer_rate_);
      final int nfilled = fh.fill(fs, IFluidHandler.FluidAction.EXECUTE);
      if(nfilled <= 0) return false;
      tank_.drain(nfilled, IFluidHandler.FluidAction.EXECUTE);
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
      int fill_level = (norm_level <= 0) ? 0 : ((int)Mth.clamp((norm_level * FluidBarrelBlock.FILL_LEVEL_MAX)+0.5, 1, FluidBarrelBlock.FILL_LEVEL_MAX));
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

    private static CompoundTag read_fluid_nbt(ItemStack stack)
    {
      if((!stack.hasTag()) || (!stack.getTag().contains("tedata"))) return new CompoundTag();
      final CompoundTag nbt = stack.getTag().getCompound("tedata");
      if(!nbt.contains("tank", Tag.TAG_COMPOUND)) return new CompoundTag();
      return nbt.getCompound("tank");
    }

    private static void write_fluid_nbt(ItemStack stack, CompoundTag fluid_nbt)
    {
      if((fluid_nbt==null) || (fluid_nbt.isEmpty())) {
        if((!stack.hasTag()) || (!stack.getTag().contains("tedata", Tag.TAG_COMPOUND))) return;
        final CompoundTag tag = stack.getTag();
        final CompoundTag tedata = tag.getCompound("tedata");
        if(tedata.contains("tank")) tedata.remove("tank");
        if(tedata.isEmpty()) tag.remove("tedata");
        stack.setTag(tag.isEmpty() ? null : tag);
      } else {
        CompoundTag tag = stack.getTag();
        if(tag==null) tag = new CompoundTag();
        CompoundTag tedata = tag.getCompound("tedata");
        if(tedata==null) tedata = new CompoundTag();
        tedata.put("tank", fluid_nbt);
        tag.put("tedata", tedata);
        stack.setTag(tag);
      }
    }

    public static FluidStack getFluid(ItemStack stack)
    {
      final CompoundTag nbt = read_fluid_nbt(stack);
      return (nbt.isEmpty()) ? (FluidStack.EMPTY) : (FluidStack.loadFluidStackFromNBT(nbt));
    }

    public static ItemStack setFluid(ItemStack stack, FluidStack fs)
    { write_fluid_nbt(stack, fs.writeToNBT(new CompoundTag())); return stack; }

    @Override
    public int getMaxStackSize(ItemStack stack)
    { return (!getFluid(stack).isEmpty()) ? 1 : super.getMaxStackSize(stack); }

    @Override
    public boolean isBarVisible(ItemStack stack)
    { return (!getFluid(stack).isEmpty()); }

    @Override
    public int getBarWidth(ItemStack stack)
    { return (int)Math.round(13f * Mth.clamp(((double)(getFluid(stack).getAmount()))/((double)capacity_), 0.0, 1.0)); }

    @Override
    public int getBarColor(ItemStack stack)
    { return 0x336633; }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt)
    { return new Fluidics.FluidContainerItemCapabilityWrapper(stack, capacity_, item_fluid_handler_transfer_rate_, FluidBarrelItem::read_fluid_nbt, FluidBarrelItem::write_fluid_nbt, e->true); }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack)
    { return (stack.getCount()==1) && (!getFluid(stack).isEmpty()); }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack)
    {
      if(stack.getCount()!=1) return ItemStack.EMPTY;
      FluidStack fs = getFluid(stack);
      if(fs.getAmount() > 1000) fs.shrink(1000); else fs = FluidStack.EMPTY;
      return setFluid(stack, fs);
    }
  }

}
