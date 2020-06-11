/*
 * @file Tooltip.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Delayed tooltip for a selected area. Constructed with a
 * GUI, invoked in `render()`.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@OnlyIn(Dist.CLIENT)
public class TooltipDisplay
{
  private static long default_delay = 1500;
  private static int default_max_deviation = 1;

  public static void config(long delay, int max_deviation)
  {
    default_delay = MathHelper.clamp(delay, 500, 5000);
    default_max_deviation = MathHelper.clamp(max_deviation, 1, 5);
  }

  // ---------------------------------------------------------------------------------------------------

  public static class TipRange
  {
    public final int x0,y0,x1,y1;
    public final ITextComponent text;
    public TipRange(int x, int y, int w, int h, ITextComponent text)
    { this.text=text; this.x0=x; this.y0=y; this.x1=x0+w-1; this.y1=y0+h-1; }
  }

  // ---------------------------------------------------------------------------------------------------

  private List<TipRange> ranges = new ArrayList<>();
  private long delay = default_delay;
  private int max_deviation = default_max_deviation;
  private int x_last, y_last;
  private long t;

  public TooltipDisplay()
  { t = System.currentTimeMillis(); }

  public void init(List<TipRange> ranges, long delay_ms, int max_deviation_xy)
  {
    this.ranges = ranges;
    this.delay = delay_ms;
    this.max_deviation = max_deviation_xy;
    t = System.currentTimeMillis();
    x_last = y_last = 0;
  }

  public void init(List<TipRange> ranges)
  { init(ranges, default_delay, default_max_deviation); }

  public void init(TipRange... ranges)
  { init(Arrays.asList(ranges), default_delay, default_max_deviation); }

  public void resetTimer()
  { t = System.currentTimeMillis(); }

  public <T extends Container> boolean render(final ContainerScreen<T> gui, int x, int y)
  {
    if((Math.abs(x-x_last) > max_deviation) || (Math.abs(y-y_last) > max_deviation)) {
      x_last = x;
      y_last = y;
      resetTimer();
      return false;
    } else if(Math.abs(System.currentTimeMillis()-t) < delay) {
      return false;
    } else if(ranges.stream().noneMatch(
      (tip)->{
        if((x<tip.x0) || (x>tip.x1) || (y<tip.y0) || (y>tip.y1)) return false;
        String text = tip.text.getFormattedText();
        if(!text.isEmpty() && (!text.startsWith("block."))) {
          gui.renderTooltip(Collections.singletonList(tip.text.getFormattedText()), x, y, Minecraft.getInstance().fontRenderer);
        }
        return true;
      })
    ){
      resetTimer();
      return false;
    } else {
      return true;
    }
  }

}
