/*
 * @file ExtItems.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Object holder based item references.
 */
package wile.engineersdecor.detail;

import net.minecraft.item.Item;
import net.minecraftforge.registries.ObjectHolder;

public class ExtItems
{
  @ObjectHolder("immersiveengineering:external_heater")
  public static final Item IE_EXTERNAL_HEATER = null;

  @ObjectHolder("bottledmilk:milk_bottle_drinkable")
  public static final Item BOTTLED_MILK_BOTTLE_DRINKLABLE = null;

  public static final void onPostInit()
  {}
}
