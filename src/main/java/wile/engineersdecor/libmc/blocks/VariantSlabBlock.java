/*
 * @file VariantSlabBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.IBlockReader;
import net.minecraft.state.StateContainer;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import wile.engineersdecor.libmc.blocks.StandardBlocks.IStandardBlock.RenderTypeHint;

public class VariantSlabBlock extends StandardBlocks.WaterLoggable implements StandardBlocks.IStandardBlock
{
  public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
  public static final IntegerProperty TEXTURE_VARIANT = IntegerProperty.create("tvariant", 0, 3);

  protected static final VoxelShape AABBs[] = {
    VoxelShapes.create(new AxisAlignedBB(0,  8./16, 0, 1, 16./16, 1)), // top slab
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  8./16, 1)), // bottom slab
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1)), // both slabs
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1))  // << 2bit fill
  };
  protected static final int num_slabs_contained_in_parts_[] = {1,1,2,2};
  private static boolean with_pickup = false;

  public static void on_config(boolean direct_slab_pickup)
  { with_pickup = direct_slab_pickup; }

  protected boolean is_cube(BlockState state)
  { return state.getValue(TYPE) == SlabType.DOUBLE; }

  public VariantSlabBlock(long config, AbstractBlock.Properties builder)
  { super(config, builder); registerDefaultState(defaultBlockState().setValue(TYPE, SlabType.BOTTOM)); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return (((config & StandardBlocks.CFG_TRANSLUCENT)!=0) ? (RenderTypeHint.TRANSLUCENT) : (RenderTypeHint.CUTOUT)); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  {
    if(!Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    if(with_pickup) Auxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", "engineersdecor.tooltip.slabpickup", tooltip, flag, true);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side)
  { return (adjacentBlockState==state) ? true : super.skipRendering(state, adjacentBlockState, side); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs[state.getValue(TYPE).ordinal() & 0x3]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(TYPE, TEXTURE_VARIANT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    BlockPos pos = context.getClickedPos();
    if(context.getLevel().getBlockState(pos).getBlock() == this) return context.getLevel().getBlockState(pos).setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
    final int rnd = MathHelper.clamp((int)(MathHelper.getSeed(context.getClickedPos()) & 0x3), 0, 3);
    final Direction face = context.getClickedFace();
    final BlockState placement_state = super.getStateForPlacement(context).setValue(TEXTURE_VARIANT, rnd); // fluid state
    if(face == Direction.UP) return placement_state.setValue(TYPE, SlabType.BOTTOM);
    if(face == Direction.DOWN) return placement_state.setValue(TYPE, SlabType.TOP);
    if(!face.getAxis().isHorizontal()) return placement_state;
    final boolean isupper = ((context.getClickLocation().y() - context.getClickedPos().getY()) > 0.5);
    return placement_state.setValue(TYPE, isupper ? SlabType.TOP : SlabType.BOTTOM);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canBeReplaced(BlockState state, BlockItemUseContext context)
  {
    if(context.getItemInHand().getItem() != this.asItem()) return false;
    if(!context.replacingClickedOnBlock()) return true;
    final Direction face = context.getClickedFace();
    final SlabType type = state.getValue(TYPE);
    if((face == Direction.UP) && (type==SlabType.BOTTOM)) return true;
    if((face == Direction.DOWN) && (type==SlabType.TOP)) return true;
    if(!face.getAxis().isHorizontal()) return false;
    final boolean isupper = ((context.getClickLocation().y() - context.getClickedPos().getY()) > 0.5);
    return isupper ? (type==SlabType.BOTTOM) : (type==SlabType.TOP);
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState rotate(BlockState state, Rotation rot)
  { return state; }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  public boolean hasDynamicDropList()
  { return true; }

  @Override
  public List<ItemStack> dropList(BlockState state, World world, TileEntity te, boolean explosion)
  { return new ArrayList<ItemStack>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.getValue(TYPE).ordinal() & 0x3]))); }

  @Override
  @SuppressWarnings("deprecation")
  public void attack(BlockState state, World world, BlockPos pos, PlayerEntity player)
  {
    if((world.isClientSide) || (!with_pickup)) return;
    final ItemStack stack = player.getMainHandItem();
    if(stack.isEmpty() || (Block.byItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vector3d lv = player.getLookAngle();
    Direction facing = Direction.getNearest((float)lv.x, (float)lv.y, (float)lv.z);
    if((facing != Direction.UP) && (facing != Direction.DOWN)) return;
    if(state.getBlock() != this) return;
    SlabType type = state.getValue(TYPE);
    if(facing == Direction.DOWN) {
      if(type == SlabType.DOUBLE) {
        world.setBlock(pos, state.setValue(TYPE, SlabType.BOTTOM), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else if(facing == Direction.UP) {
      if(type == SlabType.DOUBLE) {
        world.setBlock(pos, state.setValue(TYPE, SlabType.TOP), 3);
      } else {
        world.removeBlock(pos, false);
      }
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.setChanged();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

  @Override
  public boolean placeLiquid(IWorld world, BlockPos pos, BlockState state, FluidState fluidState)
  { return (state.getValue(TYPE)==SlabType.DOUBLE) ? false : super.placeLiquid(world, pos, state, fluidState); }

  @Override
  public boolean canPlaceLiquid(IBlockReader world, BlockPos pos, BlockState state, Fluid fluid)
  { return (state.getValue(TYPE)==SlabType.DOUBLE) ? false : super.canPlaceLiquid(world, pos, state, fluid); }

}
