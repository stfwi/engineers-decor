/*
 * @file BlockDecorSlab.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.blocks;

import net.minecraft.util.math.*;
import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.block.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.*;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BlockDecorSlab extends BlockDecor
{
  public static final IntegerProperty PARTS = IntegerProperty.create("parts", 0, 2);
  public static final IntegerProperty TEXTURE_VARIANT = IntegerProperty.create("tvariant", 0, 3);

  protected static final VoxelShape AABBs[] = {
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1,  8./16, 1)), // bottom slab
    VoxelShapes.create(new AxisAlignedBB(0,  8./16, 0, 1, 16./16, 1)), // top slab
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1)), // both slabs
    VoxelShapes.create(new AxisAlignedBB(0,  0./16, 0, 1, 16./16, 1))  // << 2bit fill
  };
  protected static final int num_slabs_contained_in_parts_[] = { 1,1,2,2 };

  protected boolean is_cube(BlockState state)
  { return state.get(PARTS) >= 2; }

  public BlockDecorSlab(long config, Block.Properties builder)
  { super(config|CFG_WATERLOGGABLE, builder); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return (((config & CFG_TRANSLUCENT)!=0) ? (BlockRenderLayer.TRANSLUCENT) : (BlockRenderLayer.CUTOUT)); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  {
    if(!ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    //  if(!ModConfig.optout.without_direct_slab_pickup) {
    ModAuxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", "engineersdecor.tooltip.slabpickup", tooltip, flag, true);
    //  }
  }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader world, BlockPos pos, EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return AABBs[state.get(PARTS) & 0x3]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(PARTS, TEXTURE_VARIANT, WATERLOGGED); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    final Direction facing = context.getFace();
    double y = context.getHitVec().getY();
    int rnd = MathHelper.clamp((int)(MathHelper.getPositionRandom(context.getPos()) % 4), 0, 3);
    return super.getStateForPlacement(context).with(PARTS, ((facing==Direction.UP) || ((facing!=Direction.DOWN) && (y < 0.6))) ? 0 : 1).with(TEXTURE_VARIANT, rnd);
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
  { return new ArrayList<ItemStack>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.get(PARTS) & 0x3]))); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    Direction face = rayTraceResult.getFace();
    final ItemStack stack = player.getHeldItem(hand);
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return false;
    int parts = state.get(PARTS);
    if(((face == Direction.UP) && (parts == 0)) || ((face == Direction.DOWN) && (parts == 1))) {
      world.setBlockState(pos, state.with(PARTS, 2), 3);
    } else {
      return false; // "not handled" -> let parent decide if a new slab has to be placed on top/bottom.
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
    if((world.isRemote)) return; // || (ModConfig.optout.without_direct_slab_pickup)
    final ItemStack stack = player.getHeldItemMainhand();
    if(stack.isEmpty() || (Block.getBlockFromItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vec3d lv = player.getLookVec();
    Direction facing = Direction.getFacingFromVector((float)lv.x, (float)lv.y, (float)lv.z);
    if((facing != Direction.UP) && (facing != Direction.DOWN)) return;
    if(state.getBlock() != this) return;
    int parts = state.get(PARTS);
    if(facing == Direction.DOWN) {
      if(parts == 2) {
        world.setBlockState(pos, state.with(PARTS, 0), 3);
      } else {
        world.removeBlock(pos, false);
      }
    } else if(facing == Direction.UP) {
      if(parts == 2) {
        world.setBlockState(pos, state.with(PARTS, 1), 3);
      } else {
        world.removeBlock(pos, false);
      }
    }
    if(!player.isCreative()) {
      stack.grow(1);
      if(player.inventory != null) player.inventory.markDirty();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundCategory.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

}
