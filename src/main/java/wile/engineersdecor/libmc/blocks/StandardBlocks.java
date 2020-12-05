/*
 * @file StandardBlocks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Common functionality class for decor blocks.
 * Mainly needed for:
 * - MC block defaults.
 * - Tooltip functionality
 * - Model initialisation
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.pathfinding.PathType;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.loot.LootParameters;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.block.material.PushReaction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.loot.LootContext;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;


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
  public static final long CFG_AI_PASSABLE                = 0x0000000000000800L; // does not block movement path for AI, needed for non-opaque blocks with collision shapes not thin at the bottom or one side.

  public interface IStandardBlock
  {
    default long config()
    { return 0; }

    default boolean hasDynamicDropList()
    { return false; }

    default List<ItemStack> dropList(BlockState state, World world, @Nullable TileEntity te, boolean explosion)
    { return Collections.singletonList((!world.isRemote()) ? (new ItemStack(state.getBlock().asItem())) : (ItemStack.EMPTY)); }

    enum RenderTypeHint { SOLID,CUTOUT,CUTOUT_MIPPED,TRANSLUCENT,TRANSLUCENT_NO_CRUMBLING }

    default RenderTypeHint getRenderTypeHint()
    { return getRenderTypeHint(config()); }

    default RenderTypeHint getRenderTypeHint(long config)
    {
      if((config & CFG_CUTOUT)!=0) return RenderTypeHint.CUTOUT;
      if((config & CFG_MIPPED)!=0) return RenderTypeHint.CUTOUT_MIPPED;
      if((config & CFG_TRANSLUCENT)!=0) return RenderTypeHint.TRANSLUCENT;
      return RenderTypeHint.SOLID;
    }
  }

  public interface IBlockItemFactory
  {
    // BlockItem factory for item registry. Only invoked once.
    BlockItem getBlockItem(Block block, Item.Properties builder);
  }

  public static class BaseBlock extends Block implements IStandardBlock
  {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public final long config;
    private final VoxelShape vshape;

    public BaseBlock(long conf, Block.Properties properties)
    { this(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

    public BaseBlock(long conf, Block.Properties properties, AxisAlignedBB aabb)
    { this(conf, properties, VoxelShapes.create(aabb)); }

    public BaseBlock(long conf, Block.Properties properties, AxisAlignedBB[] aabbs)
    { this(conf, properties, Arrays.stream(aabbs).map(aabb->VoxelShapes.create(aabb)).reduce(VoxelShapes.empty(), (shape, aabb)->VoxelShapes.combine(shape, aabb, IBooleanFunction.OR))); }

    public BaseBlock(long conf, Block.Properties properties, VoxelShape voxel_shape)
    {
      super(properties);
      config = conf;
      vshape = voxel_shape;
      BlockState state = getStateContainer().getBaseState();
      if((conf & CFG_WATERLOGGABLE)!=0) state = state.with(WATERLOGGED, false);
      setDefaultState(state);
    }

    @Override
    public long config()
    { return config; }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    { return ActionResultType.PASS; }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rnd)
    {}

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
    { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

    @Override
    public RenderTypeHint getRenderTypeHint()
    { return getRenderTypeHint(config); }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
    { return vshape; }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos,  ISelectionContext selectionContext)
    { return vshape; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean allowsMovement(BlockState state, IBlockReader world, BlockPos pos, PathType type)
    { return ((config & CFG_AI_PASSABLE)==0) ? false : super.allowsMovement(state, world, pos, type); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
      BlockState state = super.getStateForPlacement(context);
      if((config & CFG_WATERLOGGABLE)!=0) {
        FluidState fs = context.getWorld().getFluidState(context.getPos());
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

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
    {
      final ServerWorld world = builder.getWorld();
      final Float explosion_radius = builder.get(LootParameters.EXPLOSION_RADIUS);
      final TileEntity te = builder.get(LootParameters.BLOCK_ENTITY);
      if((!hasDynamicDropList()) || (world==null)) return super.getDrops(state, builder);
      boolean is_explosion = (explosion_radius!=null) && (explosion_radius > 0);
      return dropList(state, world, te, is_explosion);
    }

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
    public FluidState getFluidState(BlockState state)
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

    public WaterLoggable(long config, Block.Properties properties, AxisAlignedBB[] aabbs)
    { super(config|CFG_WATERLOGGABLE, properties, aabbs); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

  public static class Directed extends BaseBlock implements IStandardBlock
  {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    protected final ArrayList<VoxelShape> vshapes;

    public Directed(long config, Block.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    {
      super(config, properties);
      setDefaultState(stateContainer.getBaseState().with(FACING, Direction.UP));
      vshapes = shape_supplier.get();
    }

    public Directed(long config, Block.Properties properties, final AxisAlignedBB[] unrotatedAABBs)
    {
      this(config, properties, ()->{
        final boolean is_horizontal = ((config & CFG_HORIZIONTAL)!=0);
        return new ArrayList<VoxelShape>(Arrays.asList(
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.DOWN, is_horizontal)),
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.UP, is_horizontal)),
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.NORTH, is_horizontal)),
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.SOUTH, is_horizontal)),
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.WEST, is_horizontal)),
          Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.EAST, is_horizontal)),
          VoxelShapes.fullCube(),
          VoxelShapes.fullCube()
        ));
      });
    }

    public Directed(long config, Block.Properties properties, final AxisAlignedBB unrotatedAABB)
    { this(config, properties, new AxisAlignedBB[]{unrotatedAABB}); }

    @Override
    public boolean canSpawnInBlock()
    { return false; }

    @Override
    public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
    { return false; }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
    { return vshapes.get((state.get(FACING)).getIndex() & 0x7); }

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
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().isSneaking())) facing = facing.getOpposite();
      return super.getStateForPlacement(context).with(FACING, facing);
    }
  }

  public static class Horizontal extends BaseBlock implements IStandardBlock
  {
    public static final DirectionProperty HORIZONTAL_FACING = HorizontalBlock.HORIZONTAL_FACING;
    protected final ArrayList<VoxelShape> vshapes;

    public Horizontal(long config, Block.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    {
      super(config|CFG_HORIZIONTAL, properties);
      setDefaultState(stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
      vshapes = shape_supplier.get();
    }

    public Horizontal(long config, Block.Properties properties, final AxisAlignedBB[] unrotatedAABBs)
    { this(config, properties, ()->makeHorizontalShapeLookup(unrotatedAABBs)); }

    public Horizontal(long config, Block.Properties properties, final AxisAlignedBB unrotatedAABB)
    { this(config, properties, new AxisAlignedBB[]{unrotatedAABB}); }

    protected static ArrayList<VoxelShape> makeHorizontalShapeLookup(final AxisAlignedBB[] unrotatedAABBs)
    {
      return new ArrayList<VoxelShape>(Arrays.asList(
        VoxelShapes.fullCube(),
        VoxelShapes.fullCube(),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.NORTH, true)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.SOUTH, true)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.WEST, true)),
        Auxiliaries.getUnionShape(Auxiliaries.getRotatedAABB(unrotatedAABBs, Direction.EAST, true)),
        VoxelShapes.fullCube(),
        VoxelShapes.fullCube()
      ));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
    { return vshapes.get((state.get(HORIZONTAL_FACING)).getIndex() & 0x7); }

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
      if(((config & CFG_FLIP_PLACEMENT_SHIFTCLICK) != 0) && (context.getPlayer().isSneaking())) facing = facing.getOpposite();
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

    public DirectedWaterLoggable(long config, Block.Properties properties, AxisAlignedBB[] aabbs)
    { super(config|CFG_WATERLOGGABLE, properties, aabbs); }

    public DirectedWaterLoggable(long config, Block.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE, properties, shape_supplier); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

  public static class HorizontalWaterLoggable extends Horizontal implements IWaterLoggable, IStandardBlock
  {
    public HorizontalWaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, aabb); }

    public HorizontalWaterLoggable(long config, Block.Properties properties, AxisAlignedBB[] aabbs)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, aabbs); }

    public HorizontalWaterLoggable(long config, Block.Properties properties, final Supplier<ArrayList<VoxelShape>> shape_supplier)
    { super(config|CFG_WATERLOGGABLE|CFG_HORIZIONTAL, properties, shape_supplier); }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(WATERLOGGED); }
  }

  static public class HorizontalFourWayWaterLoggable extends WaterLoggable implements IStandardBlock
  {
    public static final BooleanProperty NORTH = SixWayBlock.NORTH;
    public static final BooleanProperty EAST  = SixWayBlock.EAST;
    public static final BooleanProperty SOUTH = SixWayBlock.SOUTH;
    public static final BooleanProperty WEST  = SixWayBlock.WEST;
    protected final Map<BlockState, VoxelShape> shapes;
    protected final Map<BlockState, VoxelShape> collision_shapes;

    public HorizontalFourWayWaterLoggable(long config, Block.Properties properties, AxisAlignedBB base_aabb, final AxisAlignedBB side_aabb, int railing_height_extension)
    {
      super(config, properties, base_aabb);
      Map<BlockState, VoxelShape> build_shapes = new HashMap<>();
      Map<BlockState, VoxelShape> build_collision_shapes = new HashMap<>();
      for(BlockState state:getStateContainer().getValidStates()) {
        {
          VoxelShape shape = ((base_aabb.getXSize()==0) || (base_aabb.getYSize()==0) || (base_aabb.getZSize()==0)) ? VoxelShapes.empty() : VoxelShapes.create(base_aabb);
          if(state.get(NORTH)) shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.NORTH, true)), IBooleanFunction.OR);
          if(state.get(EAST))  shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.EAST, true)), IBooleanFunction.OR);
          if(state.get(SOUTH)) shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.SOUTH, true)), IBooleanFunction.OR);
          if(state.get(WEST))  shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.WEST, true)), IBooleanFunction.OR);
          if(shape.isEmpty()) shape = VoxelShapes.fullCube();
          build_shapes.put(state.with(WATERLOGGED, false), shape);
          build_shapes.put(state.with(WATERLOGGED, true), shape);
        }
        {
          // how the hack to extend a shape, these are the above with y+4px.
          VoxelShape shape = ((base_aabb.getXSize()==0) || (base_aabb.getYSize()==0) || (base_aabb.getZSize()==0)) ? VoxelShapes.empty() : VoxelShapes.create(base_aabb);
          if(state.get(NORTH)) shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.NORTH, true).expand(0, railing_height_extension, 0)), IBooleanFunction.OR);
          if(state.get(EAST))  shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.EAST, true).expand(0, railing_height_extension, 0)), IBooleanFunction.OR);
          if(state.get(SOUTH)) shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.SOUTH, true).expand(0, railing_height_extension, 0)), IBooleanFunction.OR);
          if(state.get(WEST))  shape = VoxelShapes.combine(shape,VoxelShapes.create(Auxiliaries.getRotatedAABB(side_aabb, Direction.WEST, true).expand(0, railing_height_extension, 0)), IBooleanFunction.OR);
          if(shape.isEmpty()) shape = VoxelShapes.fullCube();
          build_collision_shapes.put(state.with(WATERLOGGED, false), shape);
          build_collision_shapes.put(state.with(WATERLOGGED, true), shape);
        }
      }
      shapes = build_shapes;
      collision_shapes = build_collision_shapes;
      setDefaultState(super.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    { super.fillStateContainer(builder); builder.add(NORTH,EAST,SOUTH,WEST); }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    { return super.getStateForPlacement(context).with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false); }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    { return shapes.getOrDefault(state, VoxelShapes.fullCube()); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    { return collision_shapes.getOrDefault(state, VoxelShapes.fullCube()); }

    public static BooleanProperty getDirectionProperty(Direction face)
    {
      switch(face) {
        case EAST : return HorizontalFourWayWaterLoggable.EAST;
        case SOUTH: return HorizontalFourWayWaterLoggable.SOUTH;
        case WEST : return HorizontalFourWayWaterLoggable.WEST;
        default   : return HorizontalFourWayWaterLoggable.NORTH;
      }
    }
  }

}
