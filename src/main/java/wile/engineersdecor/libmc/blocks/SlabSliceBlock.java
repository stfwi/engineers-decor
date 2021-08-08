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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlabSliceBlock extends StandardBlocks.WaterLoggable implements StandardBlocks.IStandardBlock
{
  public static final IntegerProperty PARTS = IntegerProperty.create("parts", 0, 14);

  protected static final VoxelShape[] AABBs = {
    Shapes.create(new AABB(0,  0./16, 0, 1,  2./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1,  4./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1,  6./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1,  8./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1, 10./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1, 12./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1, 14./16, 1)),
    Shapes.create(new AABB(0,  0./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0,  2./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0,  4./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0,  6./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0,  8./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0, 10./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0, 12./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0, 14./16, 0, 1, 16./16, 1)),
    Shapes.create(new AABB(0,0,0,1,1,1)) // <- with 4bit fill
  };

  protected static final int[] num_slabs_contained_in_parts_ = { 1,2,3,4,5,6,7,8,7,6,5,4,3,2,1 ,0x1 }; // <- with 4bit fill
  private static boolean with_pickup = false;

  public static void on_config(boolean direct_slab_pickup)
  { with_pickup = direct_slab_pickup; }

  public SlabSliceBlock(long config, BlockBehaviour.Properties builder)
  { super(config, builder); }

  protected boolean is_cube(BlockState state)
  { return state.getValue(PARTS) == 0x07; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
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
  public boolean canCreatureSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
  { return AABBs[state.getValue(PARTS) & 0xf]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(PARTS); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
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
  public boolean canBeReplaced(BlockState state, BlockPlaceContext context)
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
  public List<ItemStack> dropList(BlockState state, Level world, BlockEntity te, boolean explosion)
  { return new ArrayList<>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.getValue(PARTS) & 0xf]))); }

  @Override
  @SuppressWarnings("deprecation")
  public void attack(BlockState state, Level world, BlockPos pos, Player player)
  {
    if((world.isClientSide) || (!with_pickup)) return;
    final ItemStack stack = player.getMainHandItem();
    if(stack.isEmpty() || (Block.byItem(stack.getItem()) != this)) return;
    if(stack.getCount() >= stack.getMaxStackSize()) return;
    Vec3 lv = player.getLookAngle();
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
      if(player.getInventory() != null) player.getInventory().setChanged();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

  @Override
  public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState)
  { return (state.getValue(PARTS) != 14) && (super.placeLiquid(world, pos, state, fluidState)); }

  @Override
  public boolean canPlaceLiquid(BlockGetter world, BlockPos pos, BlockState state, Fluid fluid)
  { return (state.getValue(PARTS) != 14) && (super.canPlaceLiquid(world, pos, state, fluid)); }

}
