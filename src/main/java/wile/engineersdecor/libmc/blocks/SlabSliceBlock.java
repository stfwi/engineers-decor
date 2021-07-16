/*
 * @file BlockDecorHalfSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Half slab ("slab slices") characteristics class. Actually
 * it's now a quater slab, but who cares.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.IBlockReader;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.state.IntegerProperty;
import net.minecraft.entity.EntityType;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import wile.engineersdecor.libmc.blocks.StandardBlocks.IStandardBlock.RenderTypeHint;

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

  public SlabSliceBlock(long config, AbstractBlock.Properties builder)
  { super(config, builder); }

  protected boolean is_cube(BlockState state)
  { return state.getValue(PARTS) == 0x07; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  {
    if(!Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    if(with_pickup) Auxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", tooltip);
  }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return (((config & StandardBlocks.CFG_TRANSLUCENT)!=0) ? (RenderTypeHint.TRANSLUCENT) : (RenderTypeHint.CUTOUT)); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs[state.getValue(PARTS) & 0xf]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(PARTS); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    final BlockPos pos = context.getClickedPos();
    BlockState state = context.getLevel().getBlockState(pos);
    if(state.getBlock() == this) {
      int parts = state.getValue(PARTS);
      if(parts == 7) return null; // -> is already a full block.
      parts += (parts < 7) ? 1 : -1;
      if(parts==7) state = state.setValue(WATERLOGGED, false);
      return state.setValue(PARTS, parts);
    } else {
      final Direction face = context.getClickedFace();
      final BlockState placement_state = super.getStateForPlacement(context); // fluid state
      if(face == Direction.UP) return placement_state.setValue(PARTS, 0);
      if(face == Direction.DOWN) return placement_state.setValue(PARTS, 14);
      if(!face.getAxis().isHorizontal()) return placement_state;
      final boolean isupper = ((context.getClickLocation().y() - context.getClickedPos().getY()) > 0.5);
      return placement_state.setValue(PARTS, isupper ? 14 : 0);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canBeReplaced(BlockState state, BlockItemUseContext context)
  {
    if(context.getItemInHand().getItem() != this.asItem()) return false;
    if(!context.replacingClickedOnBlock()) return true;
    final Direction face = context.getClickedFace();
    final int parts = state.getValue(PARTS);
    if(parts == 7) return false;
    if((face == Direction.UP) && (parts < 7)) return true;
    if((face == Direction.DOWN) && (parts > 7)) return true;
    if(!face.getAxis().isHorizontal()) return false;
    final boolean isupper = ((context.getClickLocation().y() - context.getClickedPos().getY()) > 0.5);
    return isupper ? (parts==0) : (parts==1);
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
  { return new ArrayList<ItemStack>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.getValue(PARTS) & 0xf]))); }

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
    int parts = state.getValue(PARTS);
    if((facing == Direction.DOWN) && (parts <= 7)) {
      if(parts > 0) {
        world.setBlock(pos, state.setValue(PARTS, parts-1), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else if((facing == Direction.UP) && (parts >= 7)) {
      if(parts < 14) {
        world.setBlock(pos, state.setValue(PARTS, parts + 1), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else {
      return;
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.setChanged(); // @todo: check if inventory can actually be null
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

  @Override
  public boolean placeLiquid(IWorld world, BlockPos pos, BlockState state, FluidState fluidState)
  { return (state.getValue(PARTS)==14) ? false : super.placeLiquid(world, pos, state, fluidState); }

  @Override
  public boolean canPlaceLiquid(IBlockReader world, BlockPos pos, BlockState state, Fluid fluid)
  { return (state.getValue(PARTS)==14) ? false : super.canPlaceLiquid(world, pos, state, fluid); }

}
