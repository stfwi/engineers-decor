/*
 * @file BlockDecorHalfSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Half slab ("slab slices") characteristics class. Actually
 * it's now a quater slab, but who cares.
 */
package wile.engineersdecor.libmc.blocks;

import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.world.IWorld;
import net.minecraft.block.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.block.BlockState;
import net.minecraft.world.IBlockReader;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SlabSliceBlock extends StandardBlocks.WaterLoggable implements StandardBlocks.IStandardBlock
{
  public static final IntegerProperty PARTS = IntegerProperty.create("parts", 0, 14);

  protected static final VoxelShape AABBs[] = {
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  2./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  4./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  6./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  8./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 10./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 12./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 14./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  2./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  4./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  6./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,  8./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0, 10./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0, 12./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0, 14./16, 0, 1, 16./16, 1)),
    VoxelShapes.create(new AxisAlignedBB(0,0,0,1,1,1)) // <- with 4bit fill
  };

  protected static final int num_slabs_contained_in_parts_[] = { 1,2,3,4,5,6,7,8,7,6,5,4,3,2,1 ,0x1 }; // <- with 4bit fill
  private static boolean with_pickup = false;

  public static void on_config(boolean direct_slab_pickup)
  { with_pickup = direct_slab_pickup; }

  public SlabSliceBlock(long config, Block.Properties builder)
  { super(config, builder); }

  protected boolean is_cube(BlockState state)
  { return state.get(PARTS) == 0x07; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  {
    if(!Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    if(with_pickup) Auxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", "engineersdecor.tooltip.slabpickup", tooltip, flag, true);
  }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return (((config & StandardBlocks.CFG_TRANSLUCENT)!=0) ? (RenderTypeHint.TRANSLUCENT) : (RenderTypeHint.CUTOUT)); }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs[state.get(PARTS) & 0xf]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(PARTS); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    final Direction facing = context.getFace();
    double y = context.getHitVec().getY();
    return super.getStateForPlacement(context).with(PARTS, ((facing==Direction.UP) || ((facing!=Direction.DOWN) && (y < 0.6))) ? 0 : 14);
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
  public List<ItemStack> dropList(BlockState state, World world, BlockPos pos, boolean explosion)
  { return new ArrayList<ItemStack>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.get(PARTS) & 0xf]))); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    Direction face = rayTraceResult.getFace();
    final ItemStack stack = player.getHeldItem(hand);
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return false;
    if((face != Direction.UP) && (face != Direction.DOWN)) return false;
    int parts = state.get(PARTS);
    if((face != Direction.UP) && (parts > 7)) {
      world.setBlockState(pos, state.with(PARTS, parts-1), 3);
    } else if((face != Direction.DOWN) && (parts < 7)) {
      world.setBlockState(pos, state.with(PARTS, parts+1), 3);
    } else {
      return (parts != 7);
    }
    if(world.isRemote) return true;
    if(!player.isCreative()) {
      stack.shrink(1);
      if(player.inventory != null) player.inventory.markDirty();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(null, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onBlockClicked(BlockState state, World world, BlockPos pos, PlayerEntity player)
  {
    if((world.isRemote) || (!with_pickup)) return;
    final ItemStack stack = player.getHeldItemMainhand();
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vec3d lv = player.getLookVec();
    Direction facing = Direction.getFacingFromVector((float)lv.x, (float)lv.y, (float)lv.z);
    if((facing != Direction.UP) && (facing != Direction.DOWN)) return;
    if(state.getBlock() != this) return;
    int parts = state.get(PARTS);
    if((facing == Direction.DOWN) && (parts <= 7)) {
      if(parts > 0) {
        world.setBlockState(pos, state.with(PARTS, parts-1), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else if((facing == Direction.UP) && (parts >= 7)) {
      if(parts < 14) {
        world.setBlockState(pos, state.with(PARTS, parts + 1), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else {
      return;
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.markDirty(); // @todo: check if inventory can actually be null
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

  @Override
  public boolean receiveFluid(IWorld world, BlockPos pos, BlockState state, IFluidState fluidState)
  { return (state.get(PARTS)==14) ? false : super.receiveFluid(world, pos, state, fluidState); }

  @Override
  public boolean canContainFluid(IBlockReader world, BlockPos pos, BlockState state, Fluid fluid)
  { return (state.get(PARTS)==14) ? false : super.canContainFluid(world, pos, state, fluid); }

}
