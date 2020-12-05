/*
 * @file StandardDoorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Door blocks, almost entirely based on vanilla.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.properties.DoorHingeSide;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.*;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class StandardDoorBlock extends DoorBlock implements StandardBlocks.IStandardBlock
{
  private final long config_;
  protected final VoxelShape shapes_[][][][];
  protected final SoundEvent open_sound_;
  protected final SoundEvent close_sound_;

  public StandardDoorBlock(long config, Block.Properties properties, AxisAlignedBB[] open_aabbs_top, AxisAlignedBB[] open_aabbs_bottom, AxisAlignedBB[] closed_aabbs_top, AxisAlignedBB[] closed_aabbs_bottom, SoundEvent open_sound, SoundEvent close_sound)
  {
    super(properties);
    VoxelShape shapes[][][][] = new VoxelShape[Direction.values().length][2][2][2];
    for(Direction facing: Direction.values()) {
      for(boolean open: new boolean[]{false,true}) {
        for(DoubleBlockHalf half: new DoubleBlockHalf[]{DoubleBlockHalf.UPPER,DoubleBlockHalf.LOWER}) {
          for(boolean hinge_right: new boolean[]{false,true}) {
            VoxelShape shape = VoxelShapes.empty();
            if(facing.getAxis() == Axis.Y) {
              shape = VoxelShapes.fullCube();
            } else {
              final AxisAlignedBB[] aabbs = (open)?((half==DoubleBlockHalf.UPPER) ? open_aabbs_top : open_aabbs_bottom) : ((half==DoubleBlockHalf.UPPER) ? closed_aabbs_top : closed_aabbs_bottom);
              for(AxisAlignedBB e:aabbs) {
                AxisAlignedBB aabb = Auxiliaries.getRotatedAABB(e, facing, true);
                if(!hinge_right) aabb = Auxiliaries.getMirroredAABB(aabb, facing.rotateY().getAxis());
                shape = VoxelShapes.combineAndSimplify(shape, VoxelShapes.create(aabb), IBooleanFunction.OR);
              }
            }
            shapes[facing.ordinal()][open?1:0][hinge_right?1:0][half==DoubleBlockHalf.UPPER?0:1] = shape;
          }
        }
      }
    }
    config_ = config;
    shapes_ = shapes;
    open_sound_ = open_sound;
    close_sound_ = close_sound;
  }

  public StandardDoorBlock(long config, Block.Properties properties, AxisAlignedBB open_aabb, AxisAlignedBB closed_aabb, SoundEvent open_sound, SoundEvent close_sound)
  { this(config, properties, new AxisAlignedBB[]{open_aabb}, new AxisAlignedBB[]{open_aabb}, new AxisAlignedBB[]{closed_aabb}, new AxisAlignedBB[]{closed_aabb}, open_sound, close_sound); }

  public StandardDoorBlock(long config, Block.Properties properties, SoundEvent open_sound, SoundEvent close_sound)
  {
    this(
      config, properties,
      Auxiliaries.getPixeledAABB(13,0, 0, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
      open_sound,
      close_sound
    );
  }

  public StandardDoorBlock(long config, Block.Properties properties)
  {
    this(
      config, properties,
      Auxiliaries.getPixeledAABB(13,0, 0, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
      SoundEvents.BLOCK_WOODEN_DOOR_OPEN,
      SoundEvents.BLOCK_WOODEN_DOOR_CLOSE
    );
  }

  @Override
  public long config()
  { return config_; }

  protected void sound(IBlockReader world, BlockPos pos, boolean open)
  { if(world instanceof World) ((World)world).playSound(null, pos, open ? open_sound_ : close_sound_, SoundCategory.BLOCKS, 0.7f, 1f); }

  protected void actuate_adjacent_wing(BlockState state, IBlockReader world_ro, BlockPos pos, boolean open)
  {
    if(!(world_ro instanceof World)) return;
    final World world = (World)world_ro;
    final BlockPos adjecent_pos = pos.offset( (state.get(HINGE)==DoorHingeSide.LEFT) ? (state.get(FACING).rotateY()) : (state.get(FACING).rotateYCCW()));
    if(!world.isBlockPresent(adjecent_pos)) return;
    BlockState adjacent_state = world.getBlockState(adjecent_pos);
    if(adjacent_state.getBlock()!=this) return;
    if(adjacent_state.get(OPEN)==open) return;
    world.setBlockState(adjecent_pos, adjacent_state.with(OPEN, open), 2|10);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
  { return shapes_[state.get(FACING).ordinal()][state.get(OPEN)?1:0][state.get(HINGE)==DoorHingeSide.RIGHT?1:0][state.get(HALF)==DoubleBlockHalf.UPPER?0:1]; }

  @Override
  public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
  {
    boolean open = !state.get(OPEN);
    state = state.with(OPEN, open);
    world.setBlockState(pos, state, 2|8);
    sound(world, pos, open);
    actuate_adjacent_wing(state, world, pos, open);
    return ActionResultType.func_233537_a_(world.isRemote());
  }

  @Override
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    boolean powered = world.isBlockPowered(pos) || world.isBlockPowered(pos.offset(state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
    if((block == this) || (powered == state.get(POWERED))) return;
    world.setBlockState(pos, state.with(POWERED, powered).with(OPEN, powered), 2);
    actuate_adjacent_wing(state, world, pos, powered);
    if(powered != state.get(OPEN)) sound(world, pos, powered);
  }
}
