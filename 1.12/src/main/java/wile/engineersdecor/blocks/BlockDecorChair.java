/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorChair extends BlockDecorDirected
{
  public BlockDecorChair(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

}
