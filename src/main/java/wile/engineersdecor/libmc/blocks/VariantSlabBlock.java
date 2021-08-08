/*
 * @file VariantSlabBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Standard half block horizontal slab characteristics class.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
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


public class VariantSlabBlock extends StandardBlocks.WaterLoggable implements StandardBlocks.IStandardBlock
{
  public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
  public static final IntegerProperty TEXTURE_VARIANT = IntegerProperty.create("tvariant", 0, 3);

  protected static final VoxelShape[] AABBs = {
    Shapes.create(new AABB(0,  8./16, 0, 1, 16./16, 1)), // top slab
    Shapes.create(new AABB(0,  0./16, 0, 1,  8./16, 1)), // bottom slab
    Shapes.create(new AABB(0,  0./16, 0, 1, 16./16, 1)), // both slabs
    Shapes.create(new AABB(0,  0./16, 0, 1, 16./16, 1))  // << 2bit fill
  };
  protected static final int[] num_slabs_contained_in_parts_ = {1,1,2,2};
  private static boolean with_pickup = false;

  public static void on_config(boolean direct_slab_pickup)
  { with_pickup = direct_slab_pickup; }

  protected boolean is_cube(BlockState state)
  { return state.getValue(TYPE) == SlabType.DOUBLE; }

  public VariantSlabBlock(long config, BlockBehaviour.Properties builder)
  { super(config, builder); registerDefaultState(defaultBlockState().setValue(TYPE, SlabType.BOTTOM)); }

  @Override
  public RenderTypeHint getRenderTypeHint()
  { return (((config & StandardBlocks.CFG_TRANSLUCENT)!=0) ? (RenderTypeHint.TRANSLUCENT) : (RenderTypeHint.CUTOUT)); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  {
    if(!Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true)) return;
    if(with_pickup) Auxiliaries.Tooltip.addInformation("engineersdecor.tooltip.slabpickup", "engineersdecor.tooltip.slabpickup", tooltip, flag, true);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  @SuppressWarnings("deprecation")
  public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side)
  { return (adjacentBlockState==state) || (super.skipRendering(state, adjacentBlockState, side)); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean canCreatureSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext selectionContext)
  { return AABBs[state.getValue(TYPE).ordinal() & 0x3]; }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext selectionContext)
  { return getShape(state, world, pos, selectionContext); }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
  { super.createBlockStateDefinition(builder); builder.add(TYPE, TEXTURE_VARIANT); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    BlockPos pos = context.getClickedPos();
    if(context.getLevel().getBlockState(pos).getBlock() == this) return context.getLevel().getBlockState(pos).setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
    final int rnd = Mth.clamp((int)(Mth.getSeed(context.getClickedPos()) & 0x3), 0, 3);
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
  public boolean canBeReplaced(BlockState state, BlockPlaceContext context)
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
  public List<ItemStack> dropList(BlockState state, Level world, BlockEntity te, boolean explosion)
  { return new ArrayList<>(Collections.singletonList(new ItemStack(this.asItem(), num_slabs_contained_in_parts_[state.getValue(TYPE).ordinal() & 0x3]))); }

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
      if(player.getInventory() != null) player.getInventory().setChanged();
    }
    SoundType st = this.getSoundType(state, world, pos, null);
    world.playSound(player, pos, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume()+1f)/2.5f, 0.9f*st.getPitch());
  }

  @Override
  public boolean placeLiquid(LevelAccessor world, BlockPos pos, BlockState state, FluidState fluidState)
  { return (state.getValue(TYPE) != SlabType.DOUBLE) && super.placeLiquid(world, pos, state, fluidState); }

  @Override
  public boolean canPlaceLiquid(BlockGetter world, BlockPos pos, BlockState state, Fluid fluid)
  { return (state.getValue(TYPE) != SlabType.DOUBLE) && super.canPlaceLiquid(world, pos, state, fluid); }

}
