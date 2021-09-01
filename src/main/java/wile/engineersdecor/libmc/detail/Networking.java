/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.network.NetworkDirection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;


public class Networking
{
  private static final String PROTOCOL = "1";
  private static SimpleChannel DEFAULT_CHANNEL;

  public static void init(String modid)
  {
    DEFAULT_CHANNEL = NetworkRegistry.ChannelBuilder
      .named(new ResourceLocation(modid, "default_ch"))
      .clientAcceptedVersions(PROTOCOL::equals).serverAcceptedVersions(PROTOCOL::equals).networkProtocolVersion(() -> PROTOCOL)
      .simpleChannel();
    int discr = -1;
    DEFAULT_CHANNEL.registerMessage(++discr, PacketTileNotifyClientToServer.class, PacketTileNotifyClientToServer::compose, PacketTileNotifyClientToServer::parse, PacketTileNotifyClientToServer.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketTileNotifyServerToClient.class, PacketTileNotifyServerToClient::compose, PacketTileNotifyServerToClient::parse, PacketTileNotifyServerToClient.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketContainerSyncClientToServer.class, PacketContainerSyncClientToServer::compose, PacketContainerSyncClientToServer::parse, PacketContainerSyncClientToServer.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketContainerSyncServerToClient.class, PacketContainerSyncServerToClient::compose, PacketContainerSyncServerToClient::parse, PacketContainerSyncServerToClient.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, OverlayTextMessage.class, OverlayTextMessage::compose, OverlayTextMessage::parse, OverlayTextMessage.Handler::handle);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity notifications
  //--------------------------------------------------------------------------------------------------------------------

  public interface IPacketTileNotifyReceiver
  {
    default void onServerPacketReceived(CompoundNBT nbt) {}
    default void onClientPacketReceived(PlayerEntity player, CompoundNBT nbt) {}
  }

  public static class PacketTileNotifyClientToServer
  {
    CompoundNBT nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToServer(BlockPos pos, CompoundNBT nbt)
    { if((pos!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(pos, nbt)); }

    public static void sendToServer(TileEntity te, CompoundNBT nbt)
    { if((te!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(te, nbt)); }

    public PacketTileNotifyClientToServer()
    {}

    public PacketTileNotifyClientToServer(BlockPos pos, CompoundNBT nbt)
    { this.nbt = nbt; this.pos = pos; }

    public PacketTileNotifyClientToServer(TileEntity te, CompoundNBT nbt)
    { this.nbt = nbt; pos = te.getBlockPos(); }

    public static PacketTileNotifyClientToServer parse(final PacketBuffer buf)
    { return new PacketTileNotifyClientToServer(buf.readBlockPos(), buf.readNbt()); }

    public static void compose(final PacketTileNotifyClientToServer pkt, final PacketBuffer buf)
    { buf.writeBlockPos(pkt.pos); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = ctx.get().getSender();
          World world = player.level;
          if(world == null) return;
          final TileEntity te = world.getBlockEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onClientPacketReceived(ctx.get().getSender(), pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketTileNotifyServerToClient
  {
    CompoundNBT nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToPlayer(PlayerEntity player, TileEntity te, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (te==null) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketTileNotifyServerToClient(te, nbt), ((ServerPlayerEntity)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayers(TileEntity te, CompoundNBT nbt)
    {
      if(te==null || te.getLevel().isClientSide()) return;
      for(PlayerEntity player: te.getLevel().players()) sendToPlayer(player, te, nbt);
    }

    public PacketTileNotifyServerToClient()
    {}

    public PacketTileNotifyServerToClient(BlockPos pos, CompoundNBT nbt)
    { this.nbt=nbt; this.pos=pos; }

    public PacketTileNotifyServerToClient(TileEntity te, CompoundNBT nbt)
    { this.nbt=nbt; pos=te.getBlockPos(); }

    public static PacketTileNotifyServerToClient parse(final PacketBuffer buf)
    { return new PacketTileNotifyServerToClient(buf.readBlockPos(), buf.readNbt()); }

    public static void compose(final PacketTileNotifyServerToClient pkt, final PacketBuffer buf)
    { buf.writeBlockPos(pkt.pos); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          World world = SidedProxy.getWorldClientSide();
          if(world == null) return;
          final TileEntity te = world.getBlockEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onServerPacketReceived(pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // (GUI) Container synchrsonisation
  //--------------------------------------------------------------------------------------------------------------------

  public interface INetworkSynchronisableContainer
  {
    void onServerPacketReceived(int windowId, CompoundNBT nbt);
    void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt);
  }

  public static class PacketContainerSyncClientToServer
  {
    int id = -1;
    CompoundNBT nbt = null;

    public static void sendToServer(int windowId, CompoundNBT nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(windowId, nbt)); }

    public static void sendToServer(Container container, CompoundNBT nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(container.containerId, nbt)); }

    public PacketContainerSyncClientToServer()
    {}

    public PacketContainerSyncClientToServer(int id, CompoundNBT nbt)
    { this.nbt = nbt; this.id = id; }

    public static PacketContainerSyncClientToServer parse(final PacketBuffer buf)
    { return new PacketContainerSyncClientToServer(buf.readInt(), buf.readNbt()); }

    public static void compose(final PacketContainerSyncClientToServer pkt, final PacketBuffer buf)
    { buf.writeInt(pkt.id); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = ctx.get().getSender();
          if(!(player.containerMenu instanceof INetworkSynchronisableContainer)) return;
          if(player.containerMenu.containerId != pkt.id) return;
          ((INetworkSynchronisableContainer)player.containerMenu).onClientPacketReceived(pkt.id, player,pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketContainerSyncServerToClient
  {
    int id = -1;
    CompoundNBT nbt = null;

    public static void sendToPlayer(PlayerEntity player, int windowId, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(windowId, nbt), ((ServerPlayerEntity)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayer(PlayerEntity player, Container container, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(container.containerId, nbt), ((ServerPlayerEntity)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <C extends Container & INetworkSynchronisableContainer>
    void sendToListeners(World world, C container, CompoundNBT nbt)
    {
      for(PlayerEntity player: world.players()) {
        if(player.containerMenu.containerId != container.containerId) continue;
        sendToPlayer(player, container.containerId, nbt);
      }
    }

    public PacketContainerSyncServerToClient()
    {}

    public PacketContainerSyncServerToClient(int id, CompoundNBT nbt)
    { this.nbt=nbt; this.id=id; }

    public static PacketContainerSyncServerToClient parse(final PacketBuffer buf)
    { return new PacketContainerSyncServerToClient(buf.readInt(), buf.readNbt()); }

    public static void compose(final PacketContainerSyncServerToClient pkt, final PacketBuffer buf)
    { buf.writeInt(pkt.id); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = SidedProxy.getPlayerClientSide();
          if(!(player.containerMenu instanceof INetworkSynchronisableContainer)) return;
          if(player.containerMenu.containerId != pkt.id) return;
          ((INetworkSynchronisableContainer)player.containerMenu).onServerPacketReceived(pkt.id,pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Main window GUI text message
  //--------------------------------------------------------------------------------------------------------------------

  public static class OverlayTextMessage
  {
    public static final int DISPLAY_TIME_MS = 3000;
    private static BiConsumer<ITextComponent, Integer> handler_ = null;
    private ITextComponent data_;
    private int delay_ = DISPLAY_TIME_MS;
    private ITextComponent data() { return data_; }
    private int delay() { return delay_; }


    public static void setHandler(BiConsumer<ITextComponent, Integer> handler)
    { if(handler_==null) handler_ = handler; }

    public static void sendToPlayer(PlayerEntity player, ITextComponent message, int delay)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer)) return;
      DEFAULT_CHANNEL.sendTo(new OverlayTextMessage(message, delay), ((ServerPlayerEntity)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public OverlayTextMessage()
    { data_ = new TranslationTextComponent("[unset]"); }

    public OverlayTextMessage(final ITextComponent tct, int delay)
    { data_ = (ITextComponent)tct.copy(); delay_ = delay; }

    public static OverlayTextMessage parse(final PacketBuffer buf)
    {
      try {
        return new OverlayTextMessage((ITextComponent)buf.readComponent(), DISPLAY_TIME_MS);
      } catch(Throwable e) {
        return new OverlayTextMessage(new TranslationTextComponent("[incorrect translation]"), DISPLAY_TIME_MS);
      }
    }

    public static void compose(final OverlayTextMessage pkt, final PacketBuffer buf)
    {
      try {
        buf.writeComponent(pkt.data());
      } catch(Throwable e) {
        Auxiliaries.logger().error("OverlayTextMessage.toBytes() failed: " + e.toString());
      }
    }

    public static class Handler
    {
      public static void handle(final OverlayTextMessage pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        if(handler_ != null) ctx.get().enqueueWork(() -> handler_.accept(pkt.data(), pkt.delay()));
        ctx.get().setPacketHandled(true);
      }
    }
  }

}
