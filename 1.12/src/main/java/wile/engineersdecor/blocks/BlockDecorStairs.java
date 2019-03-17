/*
 * @file BlockDecorStairs.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Stairs and roof blocks, almost entirely based on vanilla stairs.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.state.IBlockState;
import wile.engineersdecor.ModEngineersDecor;
import javax.annotation.Nonnull;


public class BlockDecorStairs extends net.minecraft.block.BlockStairs
{
  public BlockDecorStairs(@Nonnull String registryName, IBlockState modelState)
  {
    super(modelState);
    setCreativeTab(ModEngineersDecor.CREATIVE_TAB_ENGINEERSDECOR);
    setRegistryName(ModEngineersDecor.MODID, registryName);
    setTranslationKey(ModEngineersDecor.MODID + "." + registryName);
  }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

}
