/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
    DEFAULT_CHANNEL.registerMessage(++discr, PacketNbtNotifyClientToServer.class, PacketNbtNotifyClientToServer::compose, PacketNbtNotifyClientToServer::parse, PacketNbtNotifyClientToServer.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketNbtNotifyServerToClient.class, PacketNbtNotifyServerToClient::compose, PacketNbtNotifyServerToClient::parse, PacketNbtNotifyServerToClient.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, OverlayTextMessage.class, OverlayTextMessage::compose, OverlayTextMessage::parse, OverlayTextMessage.Handler::handle);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity notifications
  //--------------------------------------------------------------------------------------------------------------------

  public interface IPacketTileNotifyReceiver
  {
    default void onServerPacketReceived(CompoundTag nbt) {}
    default void onClientPacketReceived(Player player, CompoundTag nbt) {}
  }

  public static class PacketTileNotifyClientToServer
  {
    CompoundTag nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToServer(BlockPos pos, CompoundTag nbt)
    { if((pos!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(pos, nbt)); }

    public static void sendToServer(BlockEntity te, CompoundTag nbt)
    { if((te!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(te, nbt)); }

    public PacketTileNotifyClientToServer()
    {}

    public PacketTileNotifyClientToServer(BlockPos pos, CompoundTag nbt)
    { this.nbt = nbt; this.pos = pos; }

    public PacketTileNotifyClientToServer(BlockEntity te, CompoundTag nbt)
    { this.nbt = nbt; pos = te.getBlockPos(); }

    public static PacketTileNotifyClientToServer parse(final FriendlyByteBuf buf)
    { return new PacketTileNotifyClientToServer(buf.readBlockPos(), buf.readNbt()); }

    public static void compose(final PacketTileNotifyClientToServer pkt, final FriendlyByteBuf buf)
    { buf.writeBlockPos(pkt.pos); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          Player player = ctx.get().getSender();
          if(player==null) return;
          Level world = player.level;
          final BlockEntity te = world.getBlockEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onClientPacketReceived(ctx.get().getSender(), pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketTileNotifyServerToClient
  {
    CompoundTag nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToPlayer(Player player, BlockEntity te, CompoundTag nbt)
    {
      if((!(player instanceof ServerPlayer)) || (player instanceof FakePlayer) || (te==null) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketTileNotifyServerToClient(te, nbt), ((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayers(BlockEntity te, CompoundTag nbt)
    {
      if(te==null || te.getLevel()==null) return;
      for(Player player: te.getLevel().players()) sendToPlayer(player, te, nbt);
    }

    public PacketTileNotifyServerToClient()
    {}

    public PacketTileNotifyServerToClient(BlockPos pos, CompoundTag nbt)
    { this.nbt=nbt; this.pos=pos; }

    public PacketTileNotifyServerToClient(BlockEntity te, CompoundTag nbt)
    { this.nbt=nbt; pos=te.getBlockPos(); }

    public static PacketTileNotifyServerToClient parse(final FriendlyByteBuf buf)
    { return new PacketTileNotifyServerToClient(buf.readBlockPos(), buf.readNbt()); }

    public static void compose(final PacketTileNotifyServerToClient pkt, final FriendlyByteBuf buf)
    { buf.writeBlockPos(pkt.pos); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          Level world = SidedProxy.getWorldClientSide();
          if(world == null) return;
          final BlockEntity te = world.getBlockEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onServerPacketReceived(pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // (GUI) Container synchronization
  //--------------------------------------------------------------------------------------------------------------------

  public interface INetworkSynchronisableContainer
  {
    void onServerPacketReceived(int windowId, CompoundTag nbt);
    void onClientPacketReceived(int windowId, Player player, CompoundTag nbt);
  }

  public static class PacketContainerSyncClientToServer
  {
    int id = -1;
    CompoundTag nbt = null;

    public static void sendToServer(int windowId, CompoundTag nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(windowId, nbt)); }

    public static void sendToServer(AbstractContainerMenu container, CompoundTag nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(container.containerId, nbt)); }

    public PacketContainerSyncClientToServer()
    {}

    public PacketContainerSyncClientToServer(int id, CompoundTag nbt)
    { this.nbt = nbt; this.id = id; }

    public static PacketContainerSyncClientToServer parse(final FriendlyByteBuf buf)
    { return new PacketContainerSyncClientToServer(buf.readInt(), buf.readNbt()); }

    public static void compose(final PacketContainerSyncClientToServer pkt, final FriendlyByteBuf buf)
    { buf.writeInt(pkt.id); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          Player player = ctx.get().getSender();
          if((player==null) || !(player.containerMenu instanceof INetworkSynchronisableContainer)) return;
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
    CompoundTag nbt = null;

    public static void sendToPlayer(Player player, int windowId, CompoundTag nbt)
    {
      if((!(player instanceof ServerPlayer)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(windowId, nbt), ((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayer(Player player, AbstractContainerMenu container, CompoundTag nbt)
    {
      if((!(player instanceof ServerPlayer)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(container.containerId, nbt), ((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <C extends AbstractContainerMenu & INetworkSynchronisableContainer>
    void sendToListeners(Level world, C container, CompoundTag nbt)
    {
      for(Player player: world.players()) {
        if(player.containerMenu.containerId != container.containerId) continue;
        sendToPlayer(player, container.containerId, nbt);
      }
    }

    public PacketContainerSyncServerToClient()
    {}

    public PacketContainerSyncServerToClient(int id, CompoundTag nbt)
    { this.nbt=nbt; this.id=id; }

    public static PacketContainerSyncServerToClient parse(final FriendlyByteBuf buf)
    { return new PacketContainerSyncServerToClient(buf.readInt(), buf.readNbt()); }

    public static void compose(final PacketContainerSyncServerToClient pkt, final FriendlyByteBuf buf)
    { buf.writeInt(pkt.id); buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          Player player = SidedProxy.getPlayerClientSide();
          if((player==null) || !(player.containerMenu instanceof INetworkSynchronisableContainer)) return;
          if(player.containerMenu.containerId != pkt.id) return;
          ((INetworkSynchronisableContainer)player.containerMenu).onServerPacketReceived(pkt.id,pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // World notifications
  //--------------------------------------------------------------------------------------------------------------------

  public static class PacketNbtNotifyClientToServer
  {
    public static final Map<String, BiConsumer<Player, CompoundTag>> handlers = new HashMap<>();
    final CompoundTag nbt;

    public static void sendToServer(CompoundTag nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketNbtNotifyClientToServer(nbt)); }

    public PacketNbtNotifyClientToServer(CompoundTag nbt)
    { this.nbt = nbt; }

    public static PacketNbtNotifyClientToServer parse(final FriendlyByteBuf buf)
    { return new PacketNbtNotifyClientToServer(buf.readNbt()); }

    public static void compose(final PacketNbtNotifyClientToServer pkt, final FriendlyByteBuf buf)
    { buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketNbtNotifyClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          final ServerPlayer player = ctx.get().getSender();
          if(player==null) return;
          final String hnd = pkt.nbt.getString("hnd");
          if(hnd.isEmpty()) return;
          if(handlers.containsKey(hnd)) handlers.get(hnd).accept(player, pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketNbtNotifyServerToClient
  {
    public static final Map<String, Consumer<CompoundTag>> handlers = new HashMap<>();
    final CompoundTag nbt;

    public static void sendToPlayer(Player player, CompoundTag nbt)
    {
      if((!(player instanceof ServerPlayer)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketNbtNotifyServerToClient(nbt), ((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayers(Level world, CompoundTag nbt)
    { for(Player player: world.players()) sendToPlayer(player, nbt); }

    public PacketNbtNotifyServerToClient(CompoundTag nbt)
    { this.nbt = nbt; }

    public static PacketNbtNotifyServerToClient parse(final FriendlyByteBuf buf)
    { return new PacketNbtNotifyServerToClient(buf.readNbt()); }

    public static void compose(final PacketNbtNotifyServerToClient pkt, final FriendlyByteBuf buf)
    { buf.writeNbt(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketNbtNotifyServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          final String hnd = pkt.nbt.getString("hnd");
          if(hnd.isEmpty()) return;
          if(handlers.containsKey(hnd)) handlers.get(hnd).accept(pkt.nbt);
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
    private static BiConsumer<Component, Integer> handler_ = null;
    private final Component data_;
    private int delay_ = DISPLAY_TIME_MS;
    private Component data() { return data_; }
    private int delay() { return delay_; }

    public static void setHandler(BiConsumer<Component, Integer> handler)
    { if(handler_==null) handler_ = handler; }

    public static void sendToPlayer(Player player, Component message, int delay)
    {
      if((!(player instanceof ServerPlayer)) || (player instanceof FakePlayer) || Auxiliaries.isEmpty(message)) return;
      DEFAULT_CHANNEL.sendTo(new OverlayTextMessage(message, delay), ((ServerPlayer)player).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public OverlayTextMessage()
    { data_ = Component.translatable("[unset]"); }

    public OverlayTextMessage(final Component tct, int delay)
    { data_ = tct.copy(); delay_ = delay; }

    public static OverlayTextMessage parse(final FriendlyByteBuf buf)
    {
      try {
        return new OverlayTextMessage(buf.readComponent(), DISPLAY_TIME_MS);
      } catch(Throwable e) {
        return new OverlayTextMessage(Component.translatable("[incorrect translation]"), DISPLAY_TIME_MS);
      }
    }

    public static void compose(final OverlayTextMessage pkt, final FriendlyByteBuf buf)
    {
      try {
        buf.writeComponent(pkt.data());
      } catch(Throwable e) {
        Auxiliaries.logger().error("OverlayTextMessage.toBytes() failed: " + e);
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
