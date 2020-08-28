/*
 * @file Overlay.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Renders status messages in one line.
 */
package wile.engineersdecor.libmc.detail;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

public class Overlay
{
  public static void register()
  {
    if(SidedProxy.mc() != null) {
      MinecraftForge.EVENT_BUS.register(new TextOverlayGui());
      Networking.OverlayTextMessage.setHandler(TextOverlayGui::show);
    }
  }

  public static void show(PlayerEntity player, final ITextComponent message)
  { Networking.OverlayTextMessage.sendToPlayer(player, message, 3000); }

  public static void show(PlayerEntity player, final ITextComponent message, int delay)
  { Networking.OverlayTextMessage.sendToPlayer(player, message, delay); }

  // -----------------------------------------------------------------------------
  // Client side handler
  // -----------------------------------------------------------------------------

  @Mod.EventBusSubscriber(Dist.CLIENT)
  @OnlyIn(Dist.CLIENT)
  public static class TextOverlayGui extends AbstractGui
  {
    private static final ITextComponent EMPTY_TEXT = new StringTextComponent("");
    private static double overlay_y_ = 0.75;
    private static int text_color_ = 0x00ffaa00;
    private static int border_color_ = 0xaa333333;
    private static int background_color1_ = 0xaa333333;
    private static int background_color2_ = 0xaa444444;
    private final Minecraft mc;
    private static long deadline_;
    private static ITextComponent text_;

    public static void on_config(double overlay_y)
    {
      overlay_y_ = overlay_y;
      // currently const, just to circumvent "useless variable" warnings
      text_color_ = 0x00ffaa00;
      border_color_ = 0xaa333333;
      background_color1_ = 0xaa333333;
      background_color2_ = 0xaa444444;
    }

    public static synchronized ITextComponent text()
    { return text_; }

    public static synchronized long deadline()
    { return deadline_; }

    public static synchronized void hide()
    { deadline_ = 0; text_ = EMPTY_TEXT; }

    public static synchronized void show(ITextComponent s, int displayTimeoutMs)
    { text_ = (s==null)?(EMPTY_TEXT):(s.deepCopy()); deadline_ = System.currentTimeMillis() + displayTimeoutMs; }

    public static synchronized void show(String s, int displayTimeoutMs)
    { text_ = ((s==null)||(s.isEmpty()))?(EMPTY_TEXT):(new StringTextComponent(s)); deadline_ = System.currentTimeMillis() + displayTimeoutMs; }

    TextOverlayGui()
    { super(); mc = SidedProxy.mc(); }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event)
    {
      if(event.getType() != RenderGameOverlayEvent.ElementType.CHAT) return;
      if(deadline() < System.currentTimeMillis()) return;
      if(text()==EMPTY_TEXT) return;
      String txt = text().getString();
      if(txt.isEmpty()) return;
      MatrixStack mxs = event.getMatrixStack();
      final MainWindow win = mc.getMainWindow();
      final FontRenderer fr = mc.fontRenderer;
      final boolean was_unicode = fr.getBidiFlag();
      try {
        final int cx = win.getScaledWidth() / 2;
        final int cy = (int)(win.getScaledHeight() * overlay_y_);
        final int w = fr.getStringWidth(txt);
        final int h = fr.FONT_HEIGHT;
        fillGradient(mxs, cx-(w/2)-3, cy-2, cx+(w/2)+2, cy+h+2, 0xaa333333, 0xaa444444);
        hLine(mxs, cx-(w/2)-3, cx+(w/2)+2, cy-2, 0xaa333333);
        hLine(mxs, cx-(w/2)-3, cx+(w/2)+2, cy+h+2, 0xaa333333);
        vLine(mxs, cx-(w/2)-3, cy-2, cy+h+2, 0xaa333333);
        vLine(mxs, cx+(w/2)+2, cy-2, cy+h+2, 0xaa333333);
        drawCenteredString(mxs, fr, text(), cx , cy+1, 0x00ffaa00);
      } finally {
        ; // fr.setBidiFlag(was_unicode);
      }
    }
  }

}
