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
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy implements ModEngineersDecor.IProxy
{
  public void preInit(FMLPreInitializationEvent e)
  { OBJLoader.INSTANCE.addDomain(ModEngineersDecor.MODID); }

  public void init(FMLInitializationEvent e)
  {}

  public void postInit(FMLPostInitializationEvent e)
  {}
}
