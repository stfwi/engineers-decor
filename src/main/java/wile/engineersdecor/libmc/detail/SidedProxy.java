/*
 * @file SidedProxy.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General client/server sideness selection proxy.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.DistExecutor;
import javax.annotation.Nullable;
import java.util.Optional;

public class SidedProxy
{
  @Nullable
  public static Player getPlayerClientSide()
  { return proxy.getPlayerClientSide(); }

  @Nullable
  public static Level getWorldClientSide()
  { return proxy.getWorldClientSide(); }

  @Nullable
  public static Minecraft mc()
  { return proxy.mc(); }

  public static Optional<Boolean> isCtrlDown()
  { return proxy.isCtrlDown(); }

  public static Optional<Boolean> isShiftDown()
  { return proxy.isShiftDown(); }

  public static Optional<String> getClipboard()
  { return proxy.getClipboard(); }

  public static boolean setClipboard(String text)
  { return proxy.setClipboard(text); }

  // --------------------------------------------------------------------------------------------------------

  private static final ISidedProxy proxy = DistExecutor.unsafeRunForDist(()->ClientProxy::new, ()->ServerProxy::new);

  private interface ISidedProxy
  {
    default @Nullable Player getPlayerClientSide() { return null; }
    default @Nullable Level getWorldClientSide() { return null; }
    default @Nullable Minecraft mc() { return null; }
    default Optional<Boolean> isCtrlDown() { return Optional.empty(); }
    default Optional<Boolean> isShiftDown() { return Optional.empty(); }
    default Optional<String> getClipboard() { return Optional.empty(); }
    default boolean setClipboard(String text) { return false; }
  }

  private static final class ClientProxy implements ISidedProxy
  {
    public @Nullable Player getPlayerClientSide() { return Minecraft.getInstance().player; }
    public @Nullable Level getWorldClientSide() { return Minecraft.getInstance().level; }
    public @Nullable Minecraft mc() { return Minecraft.getInstance(); }
    public Optional<Boolean> isCtrlDown() { return Optional.of(Auxiliaries.isCtrlDown()); }
    public Optional<Boolean> isShiftDown() { return Optional.of(Auxiliaries.isShiftDown()); }
    public Optional<String> getClipboard() { return (mc()==null) ? Optional.empty() : Optional.of(net.minecraft.client.gui.font.TextFieldHelper.getClipboardContents(mc())); }
    public boolean setClipboard(String text) { if(mc()==null) {return false;} net.minecraft.client.gui.font.TextFieldHelper.setClipboardContents(Minecraft.getInstance(), text); return true; }
  }

  private static final class ServerProxy implements ISidedProxy
  {
  }

}
