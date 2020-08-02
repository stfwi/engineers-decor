/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;


public class Networking
{
  private static SimpleNetworkWrapper snw = null;

  public static void init()
  {
    if(snw != null) return;
    int discr = -1;
    snw = NetworkRegistry.INSTANCE.newSimpleChannel(ModEngineersDecor.MODID);
    snw.registerMessage(PacketTileNotify.ServerHandler.class, PacketTileNotify.class, ++discr, Side.SERVER);
    snw.registerMessage(PacketTileNotify.ClientHandler.class, PacketTileNotify.class, ++discr, Side.CLIENT);
  }

  public interface IPacketReceiver
  {
    default void onServerPacketReceived(NBTTagCompound nbt) {}
    default void onClientPacketReceived(EntityPlayer player, NBTTagCompound nbt) {}
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity notifications
  //--------------------------------------------------------------------------------------------------------------------

  public static class PacketTileNotify implements IMessage
  {
    NBTTagCompound nbt = null;
    BlockPos pos = BlockPos.ORIGIN;

    public static void sendToServer(TileEntity te, NBTTagCompound nbt)
    { if((te != null) && (nbt!=null)) snw.sendToServer(new PacketTileNotify(te, nbt)); }

    public static void sendToPlayer(EntityPlayer player, TileEntity te, NBTTagCompound nbt)
    { if((player instanceof EntityPlayerMP) && (te != null) && (nbt!=null)) snw.sendTo(new PacketTileNotify(te, nbt), (EntityPlayerMP)player); }

    public PacketTileNotify(TileEntity te, NBTTagCompound nbt)
    { this.nbt=nbt; pos=te.getPos(); }

    public PacketTileNotify()
    {}

    @Override
    public void fromBytes(ByteBuf buf)
    { pos=BlockPos.fromLong(buf.readLong()); nbt= ByteBufUtils.readTag(buf); }

    @Override
    public void toBytes(ByteBuf buf)
    { buf.writeLong(pos.toLong()); ByteBufUtils.writeTag(buf, nbt); }

    public static class ServerHandler implements IMessageHandler<PacketTileNotify, IMessage>
    {
      @Override
      public IMessage onMessage(PacketTileNotify pkt, MessageContext ctx)
      {
        EntityPlayer player = ctx.getServerHandler().player;
        WorldServer world = ctx.getServerHandler().player.getServerWorld();
        world.addScheduledTask(() -> {
          try {
            if(!world.isBlockLoaded(pkt.pos)) return;
            TileEntity te = world.getTileEntity(pkt.pos);
            if(!(te instanceof IPacketReceiver)) return;
            ((IPacketReceiver)te).onClientPacketReceived(player, pkt.nbt);
          } catch(Throwable ex) {
            ModEngineersDecor.logger.error("Failed to process TE notify packet: " + ex.getMessage());
          }
        });
        return null;
      }
    }

    public static class ClientHandler implements IMessageHandler<PacketTileNotify, IMessage>
    {
      @Override
      public IMessage onMessage(PacketTileNotify pkt, MessageContext ctx)
      {
        Minecraft.getMinecraft().addScheduledTask(() -> {
          try {
            final World world = ModEngineersDecor.proxy.getWorlClientSide();
            if(!(world instanceof World)) return;
            TileEntity te = world.getTileEntity(pkt.pos);
            if(!(te instanceof IPacketReceiver)) return;
            ((IPacketReceiver) te).onServerPacketReceived(pkt.nbt);
          } catch(Throwable ex) {
            ModEngineersDecor.logger.error("Failed to process TE notify packet: " + ex.getMessage());
          }
        });
        return null;
      }
    }
  }
}
