/*
 * @file ModConfig.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main class for module settings. Handles reading and
 * saving the config file.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig
{
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
  public static final Miscellaneous MISC = new Miscellaneous(BUILDER);
  public static final ForgeConfigSpec conf_spec = BUILDER.build();

  public static final class Miscellaneous
  {
    public final ForgeConfigSpec.ConfigValue<Boolean> with_experimental;

    public Miscellaneous(ForgeConfigSpec.Builder builder)
    {
      builder.push("Miscellaneous");
      with_experimental = builder
        .translation(ModEngineersDecor.MODID + ".config.with_experimental")
        .comment("Enables experimental features. Use at own risk.")
        .define("with_experimental", false);
      builder.pop();
    }
  }
}
