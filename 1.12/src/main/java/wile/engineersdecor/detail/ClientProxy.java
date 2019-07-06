/*
 * @file ClientProxy.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Client side only initialisation.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy implements ModEngineersDecor.IProxy
{
  @Override
  public void preInit(FMLPreInitializationEvent e)
  { OBJLoader.INSTANCE.addDomain(ModEngineersDecor.MODID); }

  @Override
  public World getWorlClientSide()
  { return Minecraft.getMinecraft().world; }

  @Override
  public EntityPlayer getPlayerClientSide()
  { return Minecraft.getMinecraft().player; }

}
