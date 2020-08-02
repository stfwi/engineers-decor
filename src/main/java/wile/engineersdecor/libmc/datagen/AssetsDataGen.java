/*
 * @file BlockStateDataGen.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Blockstate data generator.
 */
package wile.engineersdecor.libmc.datagen;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraftforge.client.model.generators.*;
import net.minecraft.data.*;

public class AssetsDataGen
{
  public static class BlockStates extends BlockStateProvider
  {
    public BlockStates(DataGenerator gen, ExistingFileHelper efh)
    { super(gen, Auxiliaries.modid(), efh); }

    @Override
    public String getName()
    { return Auxiliaries.modid() + " Block states"; }

    @Override
    protected void registerStatesAndModels()
    {
    }
  }

  public static class ItemModels extends ItemModelProvider
  {
    public ItemModels(DataGenerator generator, ExistingFileHelper efh)
    { super(generator, Auxiliaries.modid(), efh); }

    @Override
    public String getName()
    { return Auxiliaries.modid() +  "Item models"; }

    @Override
    protected void registerModels()
    {
    }
  }
}
