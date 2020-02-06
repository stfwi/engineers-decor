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
package wile.engineersdecor.libmc.blocks;

import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.server.ServerWorld;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.world.IWorld;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;


public class StandardBlocks
{
  public static final long CFG_DEFAULT                    = 0x0000000000000000L; // no special config
  public static final long CFG_CUTOUT                     = 0x0000000000000001L; // cutout rendering
  public static final long CFG_MIPPED                     = 0x0000000000000002L; // cutout mipped rendering
  public static final long CFG_TRANSLUCENT                = 0x0000000000000004L; // indicates a block/pane is glass like (transparent, etc)
  public static final long CFG_WATERLOGGABLE              = 0x0000000000000008L; // The derived block extends IWaterLoggable
  public static final long CFG_HORIZIONTAL                = 0x0000000000000010L; // horizontal block, affects bounding box calculation at construction time and placement
  public static final long CFG_LOOK_PLACEMENT             = 0x0000000000000020L; // placed in direction the player is looking when placing.
  public static final long CFG_FACING_PLACEMENT           = 0x0000000000000040L; // placed on the facing the player has clicked.
  public static final long CFG_OPPOSITE_PLACEMENT         = 0x0000000000000080L; // placed placed in the opposite direction of the face the player clicked.
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = 0x0000000000000100L; // placement direction flipped if an instance of the same class was clicked
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = 0x0000000000000200L; // placement direction flipped if player is sneaking
  public static final long CFG_STRICT_CONNECTIONS         = 0x0000000000000400L; // blocks do not connect to similar blocks around (implementation details may vary a bit)

  public interface IStandardBlock
  {
    default boolean hasDynamicDropList()
    { return false; }

    default List<ItemStack> dropList(BlockState state, World world, BlockPos pos, boolean explosion)
    { return Collections.singletonList((!world.isRemote()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY)); }

    enum RenderTypeHint { SOLID,CUTOUT,CUTOUT_MIPPED,TRANSLUCENT }

    default RenderTypeHint getRenderTypeHint()
    { return RenderTypeHint.SOLID; }
  }

  public static class BaseBlock extends Block implements IStandardBlock
  {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public final long config;
    public final VoxelShape vshape;

    public BaseBlock(long conf, Block.Properties properties)
    { this(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

    public BaseBlock(long conf, Block.Properties properties, AxisAlignedBB aabb)
    { super(properties); config = conf; vshape = VoxelShapes.create(aabb); }

    public BaseBlock(long conf, Block.Properties properties, VoxelShape voxel_shape)
    { super(properties); config = conf; vshape = voxel_shape; }

    ///////////// --------------------------------------------------------------------------------------------------------
    // 1.15 transition

    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    { return false; }

    @Deprecated
    public ActionResultType func_225533_a_(BlockState p_225533_1_, World p_225533_2_, BlockPos p_225533_3_, PlayerEntity p_225533_4_, Hand p_225533_5_, BlockRayTraceResult p_225533_6_)
    {
      return onBlockActivated(p_225533_1_,p_225533_2_,p_225533_3_,p_225533_4_,p_225533_5_,p_225533_6_) ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }

    @Deprecated
    public void func_225534_a_(BlockState p_225534_1_, ServerWorld p_225534_2_, BlockPos p_225534_3_, Random p_225534_4_)
    { tick(p_225534_1_,p_225534_2_,p_225534_3_,p_225534_4_); }

    public void tick(BlockState state, World world, BlockPos pos, Random rnd)
    {}

    // 1.15 /transition
    ///////////// --------------------------------------------------------------------------------------------------------

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
    { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

    @Override
    public RenderTypeHint getRenderTypeHint()
    { return ((config & CFG_CUTOUT)!=0) ? RenderTypeHint.CUTOUT : RenderTypeHint.SOLID; }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
    { return vshape; }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos,  ISelectionContext selectionContext)
    { return vshape; }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if((config & CFG_WATERLOGGABLE)!=0) {
        IFluidState fs = context.getWorld().getFluidState(context.getPos());
        state = state.with(WATERLOGGED,fs.getFluid()==Fluids.WATER);
      }
      return state;
    }

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

    public static boolean dropBlock(BlockState state, World world, BlockPos pos, @Nullable PlayerEntity player)
    {
      if(!(state.getBlock() instanceof IStandardBlock)) { world.removeBlock(pos, false); return true; }
      if(!world.isRemote()) {
        if((player==null) || (!player.isCreative())) {
          ((IStandardBlock)state.getBlock()).dropList(state, world, pos, player==null).forEach(stack->world.addEntity(new ItemEntity(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, stack)));
        }
      }
      if(state.getBlock().hasTileEntity(state)) world.removeTileEntity(pos);
      world.removeBlock(pos, false);
      return true;
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid)
    { return hasDynamicDropList() ? dropBlock(state, world, pos, player) : super.removedByPlayer(state, world,pos , player, willHarvest, fluid); }

    @Override
    public void onExplosionDestroy(World world, BlockPos pos, Explosion explosion)
    { if(hasDynamicDropList()) dropBlock(world.getBlockState(pos), world, pos, null); }

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
    { return hasDynamicDropList() ? Collections.singletonList(ItemStack.EMPTY) : super.getDrops(state, builder); }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.get(WATERLOGGED)) return false;
      }
      return super.propagatesSkylightDown(state, reader, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IFluidState getFluidState(BlockState state)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
      }
      return super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos)
    {
      if((config & CFG_WATERLOGGABLE)!=0) {
        if(state.get(WATERLOGGED)) world.getPendingFluidTicks().scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }
      return state;
    }
  }

  public static class WaterLoggable extends BaseBlock implements IWaterLoggable, IStandardBlock
  {
    public WaterLoggable(long config, Block.Properties properties)
    { super(config|CFG_WATERLOGGABLE, properties); }

    public WaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    public WaterLoggable(long config, Block.Properties properties, VoxelShape voxel_shape)
    { super(config|CFG_WATERLOGGABLE, properties, voxel_shape);  }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

  public static class Directed extends BaseBlock implements IStandardBlock
  {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    protected final ArrayList<VoxelShape> AABBs;

    public Directed(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    {
      super(config, builder);
      setDefaultState(stateContainer.getBaseState().with(FACING, Direction.UP));
      final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
      AABBs = new ArrayList<VoxelShape>(Arrays.asList(
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.DOWN, is_horizontal)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.UP, is_horizontal)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.NORTH, is_horizontal)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.SOUTH, is_horizontal)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.WEST, is_horizontal)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.EAST, is_horizontal)),
        VoxelShapes.create(unrotatedAABB),
        VoxelShapes.create(unrotatedAABB)
      ));
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
    { return AABBs.get((state.get(FACING)).getIndex() & 0x7); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return getShape(state, world, pos, selectionContext); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(FACING); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
      Direction facing = context.getFace();
      if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) {
        // horizontal placement in direction the player is looking
        facing = context.getPlacementHorizontalFacing();
      } else if((config & (CFG_HORIZIONTAL|CFG_LOOK_PLACEMENT)) == (CFG_HORIZIONTAL)) {
        // horizontal placement on a face
        if(((facing==Direction.UP)||(facing==Direction.DOWN))) return null;
      } else if((config & CFG_LOOK_PLACEMENT)!=0) {
        // placement in direction the player is looking, with up and down
        facing = context.getNearestLookingDirection();
      } else {
        // default: placement on the face the player clicking
      }
      if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().func_225608_bj_()/*isSneaking()*/)) facing = facing.getOpposite();
      return super.getStateForPlacement(context).with(FACING, facing);
    }
  }

  public static class Horizontal extends BaseBlock implements IStandardBlock
  {
    public static final DirectionProperty HORIZONTAL_FACING = HorizontalBlock.HORIZONTAL_FACING;
    protected final ArrayList<VoxelShape> AABBs;

    public Horizontal(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    {
      super(config|CFG_HORIZIONTAL, builder, unrotatedAABB);
      setDefaultState(stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
      AABBs = new ArrayList<VoxelShape>(Arrays.asList(
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.DOWN, true)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.UP, true)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.NORTH, true)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.SOUTH, true)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.WEST, true)),
        VoxelShapes.create(Auxiliaries.getRotatedAABB(unrotatedAABB, Direction.EAST, true)),
        VoxelShapes.create(unrotatedAABB),
        VoxelShapes.create(unrotatedAABB)
      ));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
    { return AABBs.get((state.get(HORIZONTAL_FACING)).getIndex() & 0x7); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return getShape(state, world, pos, selectionContext); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(HORIZONTAL_FACING); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
      Direction facing = context.getFace();
      if((config & CFG_LOOK_PLACEMENT) != 0) {
        // horizontal placement in direction the player is looking
        facing = context.getPlacementHorizontalFacing();
      } else {
        // horizontal placement on a face
        facing = ((facing==Direction.UP)||(facing==Direction.DOWN)) ? (context.getPlacementHorizontalFacing()) : facing;
      }
      if((config & CFG_OPPOSITE_PLACEMENT)!=0) facing = facing.getOpposite();
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().func_225608_bj_()/*isSneaking()*/)) facing = facing.getOpposite();
      return super.getStateForPlacement(context).with(HORIZONTAL_FACING, facing);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot)
    { return state.with(HORIZONTAL_FACING, rot.rotate(state.get(HORIZONTAL_FACING))); }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn)
    { return state.rotate(mirrorIn.toRotation(state.get(HORIZONTAL_FACING))); }
  }

  public static class DirectedWaterLoggable extends Directed implements IWaterLoggable, IStandardBlock
  {
    public DirectedWaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config|CFG_WATERLOGGABLE, properties, aabb); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

  public static class HorizontalWaterLoggable extends Horizontal implements IWaterLoggable, IStandardBlock
  {
    public HorizontalWaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, aabb); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

}
