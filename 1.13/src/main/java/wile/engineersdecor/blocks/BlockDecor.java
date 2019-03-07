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
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockReader;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nullable;
import java.util.List;

public class BlockDecor extends Block
{

  // The config combines some aspects of blocks, allowing to define different behaviour at construction time, without excessive polymorphy.
  // It's an old school flag set as it is used internally only and shall not have as littlt impact on performance as possible.
  public final long config;
  public static final long CFG_DEFAULT                = 0x0000000000000000L; // no special config
  public static final long CFG_CUTOUT                 = 0x0000000000000001L; // cutout rendering
  public static final long CFG_HORIZIONTAL            = 0x0000000000000002L; // horizontal block, affects bounding box calculation at construction time
  public static final long CFG_HORIZIONTAL_PLACEMENT  = 0x0000000000000004L; // placed in the horizontzal direction the player is looking when placing.
  public static final long CFG_WALL_DOOR_CONNECTION   = 0x0000000000000008L; // wall block connects to fence gates and doors.

  public BlockDecor(long config, Block.Properties properties)
  { super(properties); this.config = config; }

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
  public boolean isFullCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(IBlockState state)
  { return ((config & CFG_CUTOUT)==0); }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(IBlockState state)
  { return EnumPushReaction.NORMAL; }

}
