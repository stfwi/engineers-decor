package wile.engineersdecor.libmc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerGui<T extends Container> extends ContainerScreen<T> implements IHasContainer<T>
{
  public ContainerGui(T screenContainer, PlayerInventory inv, ITextComponent titleIn)
  { super(screenContainer, inv, titleIn); }

  protected boolean canHaveDisturbingButtonsFromOtherMods()
  { return false; }

  public void init(Minecraft minecraft, int width, int height)
  {
    this.minecraft = minecraft;
    this.itemRenderer = minecraft.getItemRenderer();
    this.font = minecraft.fontRenderer;
    this.width = width;
    this.height = height;
    java.util.function.Consumer<Widget> remove = (b) -> { buttons.remove(b); children.remove(b); };
    if((!canHaveDisturbingButtonsFromOtherMods()) || (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Pre(this, this.buttons, this::addButton, remove)))) {
      this.buttons.clear();
      this.children.clear();
      this.setListener((IGuiEventListener)null);
      this.init();
    }
    if(canHaveDisturbingButtonsFromOtherMods()) {
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post(this, this.buttons, this::addButton, remove));
    }
  }

}
