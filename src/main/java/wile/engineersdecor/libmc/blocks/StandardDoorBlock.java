/*
 * @file StandardDoorBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Door blocks, almost entirely based on vanilla.
 */
package wile.engineersdecor.libmc.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.List;


public class StandardDoorBlock extends DoorBlock implements StandardBlocks.IStandardBlock
{
  private final long config_;
  protected final VoxelShape[][][][] shapes_;
  protected final SoundEvent open_sound_;
  protected final SoundEvent close_sound_;

  public StandardDoorBlock(long config, BlockBehaviour.Properties properties, AABB[] open_aabbs_top, AABB[] open_aabbs_bottom, AABB[] closed_aabbs_top, AABB[] closed_aabbs_bottom, SoundEvent open_sound, SoundEvent close_sound)
  {
    super(properties);
    VoxelShape[][][][] shapes = new VoxelShape[Direction.values().length][2][2][2];
    for(Direction facing: Direction.values()) {
      for(boolean open: new boolean[]{false,true}) {
        for(DoubleBlockHalf half: new DoubleBlockHalf[]{DoubleBlockHalf.UPPER,DoubleBlockHalf.LOWER}) {
          for(boolean hinge_right: new boolean[]{false,true}) {
            VoxelShape shape = Shapes.empty();
            if(facing.getAxis() == Direction.Axis.Y) {
              shape = Shapes.block();
            } else {
              final AABB[] aabbs = (open)?((half==DoubleBlockHalf.UPPER) ? open_aabbs_top : open_aabbs_bottom) : ((half==DoubleBlockHalf.UPPER) ? closed_aabbs_top : closed_aabbs_bottom);
              for(AABB e:aabbs) {
                AABB aabb = Auxiliaries.getRotatedAABB(e, facing, true);
                if(!hinge_right) aabb = Auxiliaries.getMirroredAABB(aabb, facing.getClockWise().getAxis());
                shape = Shapes.join(shape, Shapes.create(aabb), BooleanOp.OR);
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

  public StandardDoorBlock(long config, BlockBehaviour.Properties properties, AABB open_aabb, AABB closed_aabb, SoundEvent open_sound, SoundEvent close_sound)
  { this(config, properties, new AABB[]{open_aabb}, new AABB[]{open_aabb}, new AABB[]{closed_aabb}, new AABB[]{closed_aabb}, open_sound, close_sound); }

  public StandardDoorBlock(long config, BlockBehaviour.Properties properties, SoundEvent open_sound, SoundEvent close_sound)
  {
    this(
      config, properties,
      Auxiliaries.getPixeledAABB(13,0, 0, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
      open_sound,
      close_sound
    );
  }

  public StandardDoorBlock(long config, BlockBehaviour.Properties properties)
  {
    this(
      config, properties,
      Auxiliaries.getPixeledAABB(13,0, 0, 16,16,16),
      Auxiliaries.getPixeledAABB( 0,0,13, 16,16,16),
      SoundEvents.WOODEN_DOOR_OPEN,
      SoundEvents.WOODEN_DOOR_CLOSE
    );
  }

  @Override
  public long config()
  { return config_; }

  protected void sound(BlockGetter world, BlockPos pos, boolean open)
  { if(world instanceof Level) ((Level)world).playSound(null, pos, open ? open_sound_ : close_sound_, SoundSource.BLOCKS, 0.7f, 1f); }

  protected void actuate_adjacent_wing(BlockState state, BlockGetter world_ro, BlockPos pos, boolean open)
  {
    if(!(world_ro instanceof final Level world)) return;
    final BlockPos adjecent_pos = pos.relative( (state.getValue(HINGE)==DoorHingeSide.LEFT) ? (state.getValue(FACING).getClockWise()) : (state.getValue(FACING).getCounterClockWise()));
    if(!world.isLoaded(adjecent_pos)) return;
    BlockState adjacent_state = world.getBlockState(adjecent_pos);
    if(adjacent_state.getBlock()!=this) return;
    if(adjacent_state.getValue(OPEN)==open) return;
    world.setBlock(adjecent_pos, adjacent_state.setValue(OPEN, open), 2|10);
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag)
  { Auxiliaries.Tooltip.addInformation(stack, world, tooltip, flag, true); }

  @Override
  public boolean isPossibleToRespawnInThis()
  { return false; }

  @Override
  public boolean canCreatureSpawn(BlockState state, BlockGetter world, BlockPos pos, SpawnPlacements.Type type, @Nullable EntityType<?> entityType)
  { return false; }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
  { return shapes_[state.getValue(FACING).ordinal()][state.getValue(OPEN)?1:0][state.getValue(HINGE)==DoorHingeSide.RIGHT?1:0][state.getValue(HALF)==DoubleBlockHalf.UPPER?0:1]; }

  @Override
  public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
  { setOpen(player, world, state, pos, !state.getValue(OPEN)); return InteractionResult.sidedSuccess(world.isClientSide()); }

  @Override
  public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    boolean powered = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN));
    if((block == this) || (powered == state.getValue(POWERED))) return;
    world.setBlock(pos, state.setValue(POWERED, powered).setValue(OPEN, powered), 2);
    actuate_adjacent_wing(state, world, pos, powered);
    if(powered != state.getValue(OPEN)) sound(world, pos, powered);
  }

  @Override
  public void setOpen(@Nullable Entity entity, Level world, BlockState state, BlockPos pos, boolean open)
  {
    if(!state.is(this) || (state.getValue(OPEN) == open)) return;
    state = state.setValue(OPEN, open);
    world.setBlock(pos, state, 2|8);
    sound(world, pos, open);
    actuate_adjacent_wing(state, world, pos, open);
  }

}
