/*
 * @file EdCornerOrnamentedBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Block for corner/quoin ornamentation.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.world.World;
import wile.engineersdecor.libmc.detail.Auxiliaries;

import javax.annotation.Nullable;
import java.util.*;



public class EdCornerOrnamentedBlock extends DecorBlock.Directed
{
  protected final HashSet<Block> compatible_blocks;

  public EdCornerOrnamentedBlock(long config, AbstractBlock.Properties properties, Block[] assigned_wall_blocks)
  {
    super(config, properties, Auxiliaries.getPixeledAABB(0,0,0,16,16,16));
    compatible_blocks = new HashSet<Block>(Arrays.asList(assigned_wall_blocks));
  }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    final World world = context.getLevel();
    final BlockPos pos = context.getClickedPos();
    // 1. Placement as below/above for corners, or placement adjacent horizontally if up/down facing.
    for(Direction adj: Direction.values()) {
      BlockState state = world.getBlockState(pos.relative(adj));
      if(state.getBlock() != this) continue;
      Direction facing = state.getValue(FACING);
      if(facing.getAxis().isHorizontal() == (adj.getAxis().isVertical())) {
        return super.getStateForPlacement(context).setValue(FACING, state.getValue(FACING));
      }
    }
    // 2. By Player look angles with minimum horizontal diagonal deviation.
    {
      Direction facing = Direction.WEST;
      final Vector2f look = context.getPlayer().getRotationVector();
      final Direction hit_face = context.getClickedFace();
      if((context.getClickedFace()==Direction.DOWN) && (look.x <= -60)) {
        facing = Direction.DOWN;
      } else if((context.getClickedFace()==Direction.UP) && (look.x >= 60)) {
        facing = Direction.UP;
      } else if(MathHelper.degreesDifferenceAbs(look.y, 45) <= 45) {
        facing = Direction.NORTH;
      } else if(MathHelper.degreesDifferenceAbs(look.y, 45+90) <= 45) {
        facing = Direction.EAST;
      } else if(MathHelper.degreesDifferenceAbs(look.y, 45+180) <= 45) {
        facing = Direction.SOUTH;
      }
      return super.getStateForPlacement(context).setValue(FACING, facing);
    }
  }
}
