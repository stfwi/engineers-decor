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

import wile.engineersdecor.libmc.blocks.StandardBlocks;
import wile.engineersdecor.libmc.blocks.StandardBlocks.IStandardBlock;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.block.Block;


public class BlockDecor extends StandardBlocks.BaseBlock implements IDecorBlock
{
  public static final long CFG_DEFAULT                    = StandardBlocks.CFG_DEFAULT;
  public static final long CFG_CUTOUT                     = StandardBlocks.CFG_CUTOUT;
  public static final long CFG_MIPPED                     = StandardBlocks.CFG_MIPPED;
  public static final long CFG_TRANSLUCENT                = StandardBlocks.CFG_TRANSLUCENT;
  public static final long CFG_WATERLOGGABLE              = StandardBlocks.CFG_WATERLOGGABLE;
  public static final long CFG_HORIZIONTAL                = StandardBlocks.CFG_HORIZIONTAL;
  public static final long CFG_LOOK_PLACEMENT             = StandardBlocks.CFG_LOOK_PLACEMENT;
  public static final long CFG_FACING_PLACEMENT           = StandardBlocks.CFG_FACING_PLACEMENT;
  public static final long CFG_OPPOSITE_PLACEMENT         = StandardBlocks.CFG_OPPOSITE_PLACEMENT;
  public static final long CFG_FLIP_PLACEMENT_IF_SAME     = StandardBlocks.CFG_FLIP_PLACEMENT_IF_SAME;
  public static final long CFG_FLIP_PLACEMENT_SHIFTCLICK  = StandardBlocks.CFG_FLIP_PLACEMENT_SHIFTCLICK;
  public static final long CFG_STRICT_CONNECTIONS         = StandardBlocks.CFG_STRICT_CONNECTIONS;
  public static final long CFG_HARD_IE_DEPENDENT          = 0x8000000000000000L;

  public BlockDecor(long conf, Block.Properties properties)
  { super(conf, properties, Auxiliaries.getPixeledAABB(0, 0, 0, 16, 16,16 )); }

  public BlockDecor(long conf, Block.Properties properties, AxisAlignedBB aabb)
  { super(conf, properties, aabb);}

  public BlockDecor(long conf, Block.Properties properties, VoxelShape voxel_shape)
  { super(conf, properties, voxel_shape); }

  public static class WaterLoggable extends StandardBlocks.WaterLoggable implements IStandardBlock, IWaterLoggable
  {
    public WaterLoggable(long config, Block.Properties properties)
    { super(config, properties); }

    public WaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config, properties, aabb); }

    public WaterLoggable(long config, Block.Properties properties, VoxelShape voxel_shape)
    { super(config, properties, voxel_shape);  }
  }

  public static class Directed extends StandardBlocks.Directed implements IDecorBlock
  {
    public Directed(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }
  }

  public static class DirectedWaterLoggable extends StandardBlocks.DirectedWaterLoggable implements IDecorBlock,IWaterLoggable
  {
    public DirectedWaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config, properties, aabb); }
  }

  public static class Horizontal extends StandardBlocks.Horizontal implements IDecorBlock
  {
    public Horizontal(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }
  }

  public static class HorizontalWaterLoggable extends StandardBlocks.HorizontalWaterLoggable implements IWaterLoggable
  {
    public HorizontalWaterLoggable(long config, Block.Properties properties, AxisAlignedBB aabb)
    { super(config, properties, aabb); }
  }
}
