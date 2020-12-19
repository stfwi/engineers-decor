/*
 * @file EdChimneyTrunkBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Roof block with chimney trunk, only straight.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.*;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.StairsShape;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

import javax.annotation.Nullable;


public class EdChimneyTrunkBlock extends EdRoofBlock implements IDecorBlock
{
  public EdChimneyTrunkBlock(long config, Block.Properties properties)
  { super(config, properties.variableOpacity(), VoxelShapes.empty(), VoxelShapes.empty()); }

  public EdChimneyTrunkBlock(long config, Block.Properties properties, VoxelShape add, VoxelShape cut)
  { super(config, properties, add, cut); }

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    BlockState state = super.getStateForPlacement(context);
    return (state==null) ? (state) : (state.with(EdRoofBlock.SHAPE, StairsShape.STRAIGHT).with(EdRoofBlock.HALF, Half.BOTTOM));
  }
}
