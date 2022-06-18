/*
 * @file Overlay.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Renders status messages in one line.
 */
package wile.engineersdecor.libmc.detail;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Optional;


public class Overlay
{
  public static void register()
  {
    if(SidedProxy.mc() != null) {
      MinecraftForge.EVENT_BUS.register(new TextOverlayGui());
      Networking.OverlayTextMessage.setHandler(TextOverlayGui::show);
    }
  }

  public static void show(Player player, final Component message)
  { Networking.OverlayTextMessage.sendToPlayer(player, message, 3000); }

  public static void show(Player player, final Component message, int delay)
  { Networking.OverlayTextMessage.sendToPlayer(player, message, delay); }

  public static void show(BlockState state, BlockPos pos)
  { show(state, pos, 100); }

  public static void show(BlockState state, BlockPos pos, int displayTimeoutMs)
  { DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ()->(()->TextOverlayGui.show(state, pos, displayTimeoutMs))); } // Only called when client side

  // -----------------------------------------------------------------------------
  // Client side handler
  // -----------------------------------------------------------------------------

  @Mod.EventBusSubscriber(Dist.CLIENT)
  @OnlyIn(Dist.CLIENT)
  public static class TextOverlayGui extends Screen
  {
    private static final Component EMPTY_TEXT = Component.literal("");
    private static final BlockState EMPTY_STATE = null;
    private static double overlay_y_ = 0.75;
    private static int text_color_ = 0x00ffaa00;
    private static int border_color_ = 0xaa333333;
    private static int background_color1_ = 0xaa333333;
    private static int background_color2_ = 0xaa444444;
    private final Minecraft mc;
    private static long text_deadline_ = 0;
    private static Component text_ = EMPTY_TEXT;
    private static long state_deadline_ = 0;
    private static @Nullable BlockState state_ = EMPTY_STATE;
    private static BlockPos pos_ = BlockPos.ZERO;

    public static void on_config(double overlay_y)
    { on_config(overlay_y, 0x00ffaa00, 0xaa333333, 0xaa333333, 0xaa444444); }

    public static void on_config(double overlay_y, int text_color, int border_color, int background_color1, int background_color2)
    {
      overlay_y_ = overlay_y;
      text_color_ = text_color;
      border_color_ = border_color;
      background_color1_ = background_color1;
      background_color2_ = background_color2;
    }

    public static synchronized Component text()
    { return text_; }

    public static synchronized long deadline()
    { return text_deadline_; }

    public static synchronized void hide()
    { text_deadline_ = 0; text_ = EMPTY_TEXT; }

    public static synchronized void show(Component s, int displayTimeoutMs)
    { text_ = (s==null)?(EMPTY_TEXT):(s.copy()); text_deadline_ = System.currentTimeMillis() + displayTimeoutMs; }

    public static synchronized void show(String s, int displayTimeoutMs)
    { text_ = ((s==null)||(s.isEmpty()))?(EMPTY_TEXT):(Component.literal(s)); text_deadline_ = System.currentTimeMillis() + displayTimeoutMs; }

    public static synchronized void show(BlockState state, BlockPos pos, int displayTimeoutMs)
    { pos_ = new BlockPos(pos); state_ = state; state_deadline_ = System.currentTimeMillis() + displayTimeoutMs; }

    private static synchronized Optional<Tuple<BlockState,BlockPos>> state_pos()
    { return ((state_deadline_ < System.currentTimeMillis()) || (state_==EMPTY_STATE)) ? Optional.empty() : Optional.of(new Tuple<>(state_, pos_)); }

    TextOverlayGui()
    { super(Component.literal("")); mc = SidedProxy.mc(); }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event)
    {
      if(event.getType() != RenderGameOverlayEvent.ElementType.CHAT) return;
      if(deadline() < System.currentTimeMillis()) return;
      if(text()==EMPTY_TEXT) return;
      String txt = text().getString();
      if(txt.isEmpty()) return;
      PoseStack mxs = event.getPoseStack();
      final Window win = mc.getWindow();
      final Font fr = mc.font;
      final boolean was_unicode = fr.isBidirectional();
      final int cx = win.getGuiScaledWidth() / 2;
      final int cy = (int)(win.getGuiScaledHeight() * overlay_y_);
      final int w = fr.width(txt);
      final int h = fr.lineHeight;
      fillGradient(mxs, cx-(w/2)-3, cy-2, cx+(w/2)+2, cy+h+2, 0xaa333333, 0xaa444444);
      hLine(mxs, cx-(w/2)-3, cx+(w/2)+2, cy-2, 0xaa333333);
      hLine(mxs, cx-(w/2)-3, cx+(w/2)+2, cy+h+2, 0xaa333333);
      vLine(mxs, cx-(w/2)-3, cy-2, cy+h+2, 0xaa333333);
      vLine(mxs, cx+(w/2)+2, cy-2, cy+h+2, 0xaa333333);
      drawCenteredString(mxs, fr, text(), cx , cy+1, 0x00ffaa00);
    }

    @SubscribeEvent
    public void onRenderWorldOverlay(RenderLevelLastEvent event)
    {
      final Optional<Tuple<BlockState,BlockPos>> sp = state_pos();
      if(sp.isEmpty()) return;
      final ClientLevel world = Minecraft.getInstance().level;
      final LocalPlayer player = Minecraft.getInstance().player;
      if((player==null) || (world==null)) return;
      final BlockState state = sp.get().getA();
      final BlockPos pos = sp.get().getB();
      @SuppressWarnings("deprecation")
      final int light = (world.hasChunkAt(pos)) ? LightTexture.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos)) : LightTexture.pack(15, 15);
      final MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
      final PoseStack mxs = event.getPoseStack();
      final double px = Mth.lerp(event.getPartialTick(), player.xo, player.getX());
      final double py = Mth.lerp(event.getPartialTick(), player.yo, player.getY());
      final double pz = Mth.lerp(event.getPartialTick(), player.zo, player.getZ());
      mxs.pushPose();
      mxs.translate((pos.getX()-px), (pos.getY()-py-player.getEyeHeight()), (pos.getZ()-pz));
      Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, mxs, buffer, light, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
      mxs.popPose();
    }

  }

}
