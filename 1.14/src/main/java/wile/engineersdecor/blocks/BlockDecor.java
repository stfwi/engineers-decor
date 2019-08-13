/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 * Mainly needed for:
 * - MC block defaults.
 * - Tooltip functionality
 * - Model initialisation
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.detail.ModAuxiliaries;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.material.PushReaction;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;


public class BlockDecor extends Block implements IDecorBlock
{
  public static final long CFG_DEFAULT                    = 0x0000000000000000L; // no special config
  public static final long CFG_CUTOUT                     = 0x0000000000000001L; // cutout rendering
  public static final long CFG_HORIZIONTAL                = 0x0000000000000002L; // horizontal block, affects bounding box calculation at construction time and placement
  public static final long CFG_LOOK_PLACEMENT             = 0x0000000000000004L; // placed in direction the player is looking when placing.
  public static final long CFG_FACING_PLACEMENT           = 0x0000000000000008L; // placed on the facing the player has clicked.
  public static final long CFG_OPPOSITE_PLACEMENT         = 0x0000000000000010L; // placed placed in the opposite direction of the face the player clicked.
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = 0x0000000000000020L; // placement direction flipped if an instance of the same class was clicked
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = 0x0000000000000040L; // placement direction flipped if player is sneaking
  public static final long CFG_TRANSLUCENT                = 0x0000000000000080L; // indicates a block/pane is glass like (transparent, etc)
  public static final long CFG_ELECTRICAL                 = 0x0000000000010000L; // Denotes if a component is mainly flux driven.
  public static final long CFG_REDSTONE_CONTROLLED        = 0x0000000000020000L; // Denotes if a component has somehow a redstone control input
  public static final long CFG_ANALOG                     = 0x0000000000040000L; // Denotes if a component has analog behaviour
  public static final long CFG_HARD_IE_DEPENDENT          = 0x8000000000000000L; // The block is implicitly opt'ed out if IE is not installed

  public final long config;
  public final VoxelShape vshape;

  public BlockDecor(long config, Block.Properties properties)
  { this(config, properties, ModAuxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

  public BlockDecor(long config, Block.Properties properties, AxisAlignedBB aabb)
  { super(properties); this.config = config; this.vshape = VoxelShapes.create(aabb); }

  public BlockDecor(long config, Block.Properties properties, VoxelShape voxel_shape)
  { super(properties); this.config = config; this.vshape = voxel_shape; }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { ModAuxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return ((config & CFG_CUTOUT)!=0) ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID; }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return vshape; }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos,  ISelectionContext selectionContext)
  { return vshape; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.NORMAL; }

  @Override
  @SuppressWarnings("deprecation")
  public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
  {
    if(state.hasTileEntity() && (state.getBlock() != newState.getBlock())) {
      world.removeTileEntity(pos);
      world.updateComparatorOutputLevel(pos, this);
    }
  }

  public static boolean dropBlock(BlockState state, World world, BlockPos pos, boolean explosion)
  {
    if(!(state.getBlock() instanceof IDecorBlock)) { world.removeBlock(pos, false); return true; }
    if(!world.isRemote()) {
      ((IDecorBlock)state.getBlock()).dropList(state, world, pos, explosion).forEach(stack->world.addEntity(new ItemEntity(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack)));
    }
    if(state.getBlock().hasTileEntity(state)) world.removeTileEntity(pos);
    world.removeBlock(pos, false);
    return true;
  }

  @Override
  public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid)
  {
    if(player.isCreative()) return true;
    return dropBlock(state, world, pos, false);
  }

  @Override
  public void onExplosionDestroy(World world, BlockPos pos, Explosion explosion)
  { dropBlock(world.getBlockState(pos), world, pos, false); }

  @Override
  @SuppressWarnings("deprecation")
  public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
  { return Collections.singletonList(ItemStack.EMPTY); } // { return Collections.singletonList(new ItemStack(this.asItem())); } //

}
