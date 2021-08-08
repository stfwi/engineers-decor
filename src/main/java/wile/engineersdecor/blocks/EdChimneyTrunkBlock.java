/*
 * @file EdChimneyTrunkBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Roof block with chimney trunk, only straight.
 */
package wile.engineersdecor.blocks;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;


public class EdChimneyTrunkBlock extends EdRoofBlock
{
  public EdChimneyTrunkBlock(long config, BlockBehaviour.Properties properties)
  { super(config, properties.dynamicShape(), Shapes.empty(), Shapes.empty()); }

  public EdChimneyTrunkBlock(long config, BlockBehaviour.Properties properties, VoxelShape add, VoxelShape cut)
  { super(config, properties, add, cut); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockPlaceContext context)
  {
    BlockState state = super.getStateForPlacement(context);
    return (state==null) ? (state) : (state.setValue(EdRoofBlock.SHAPE, StairsShape.STRAIGHT).setValue(EdRoofBlock.HALF, Half.BOTTOM));
  }
}
