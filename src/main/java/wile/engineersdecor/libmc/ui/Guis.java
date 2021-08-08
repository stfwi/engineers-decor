package wile.engineersdecor.libmc.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import wile.engineersdecor.libmc.detail.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.Function;


public class Guis
{
  // -------------------------------------------------------------------------------------------------------------------
  // Gui base
  // -------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static abstract class ContainerGui<T extends AbstractContainerMenu> extends AbstractContainerScreen<T>
  {
    protected final ResourceLocation background_image_;
    protected final Player player_;
    protected final Guis.BackgroundImage gui_background_;
    protected final TooltipDisplay tooltip_ = new TooltipDisplay();

    public ContainerGui(T menu, Inventory player_inv, Component title, String background_image, int width, int height)
    {
      super(menu, player_inv, title);
      this.background_image_ = new ResourceLocation(Auxiliaries.modid(), background_image);
      this.player_ = player_inv.player;
      this.imageWidth = width;
      this.imageHeight = height;
      gui_background_ = new Guis.BackgroundImage(background_image_, width, height, new Coord2d(0,0));
    }

    public ContainerGui(T menu, Inventory player_inv, Component title, String background_image)
    {
      super(menu, player_inv, title);
      this.background_image_ = new ResourceLocation(Auxiliaries.modid(), background_image);
      this.player_ = player_inv.player;
      gui_background_ = new Guis.BackgroundImage(background_image_, imageWidth, imageHeight, new Coord2d(0,0));
    }

    @Override
    public void init()
    {
      super.init();
      gui_background_.init(this, new Coord2d(0,0)).show();
    }

    @Override
    public void render(PoseStack mx, int mouseX, int mouseY, float partialTicks)
    {
      renderBackground(mx);
      super.render(mx, mouseX, mouseY, partialTicks);
      if(!tooltip_.render(mx, this, mouseX, mouseY)) renderTooltip(mx, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack mx, int x, int y)
    {}

    @Override
    @SuppressWarnings("deprecation")
    protected final void renderBg(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      gui_background_.draw(mx, this);
      renderBgWidgets(mx, partialTicks, mouseX, mouseY);
      RenderSystem.disableBlend();
    }

    public final ResourceLocation getBackgroundImage()
    { return background_image_; }

    protected void renderBgWidgets(PoseStack mx, float partialTicks, int mouseX, int mouseY)
    {}

    protected void renderItemTemplate(PoseStack mx, ItemStack stack, int x, int y)
    {
      final ItemRenderer ir = itemRenderer;
      final int main_zl = getBlitOffset();
      final float zl = ir.blitOffset;
      final int x0 = getGuiLeft();
      final int y0 = getGuiTop();
      ir.blitOffset = -80;
      ir.renderGuiItem(stack, x0+x, y0+y);
      RenderSystem.disableColorLogicOp(); //RenderSystem.disableColorMaterial();
      RenderSystem.enableDepthTest(); //RenderSystem.enableAlphaTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableBlend();
      ir.blitOffset = zl;
      setBlitOffset(100);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.setShaderColor(0.7f, 0.7f, 0.7f, 0.8f);
      RenderSystem.setShaderTexture(0, background_image_);
      blit(mx, x0+x, y0+y, x, y, 16, 16);
      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
      setBlitOffset(main_zl);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Gui elements
  // -------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class Coord2d
  {
    public final int x, y;
    public Coord2d(int x, int y) { this.x=x; this.y=y; }
  }

  @OnlyIn(Dist.CLIENT)
  public static class UiWidget extends net.minecraft.client.gui.components.AbstractWidget
  {
    protected static final Component EMPTY_TEXT = new TextComponent("");
    protected static final Function<UiWidget,Component> NO_TOOLTIP = (uiw)->EMPTY_TEXT;

    @SuppressWarnings("all") private Function<UiWidget,Component> tooltip_ = NO_TOOLTIP;

    public UiWidget(int x, int y, int width, int height, Component title)
    { super(x, y, width, height, title); }

    public UiWidget init(Screen parent)
    {
      this.x += ((parent instanceof AbstractContainerScreen<?>) ? ((AbstractContainerScreen<?>)parent).getGuiLeft() : 0);
      this.y += ((parent instanceof AbstractContainerScreen<?>) ? ((AbstractContainerScreen<?>)parent).getGuiTop() : 0);
      return this;
    }

    public UiWidget init(Screen parent, Coord2d position)
    {
      this.x = position.x + ((parent instanceof AbstractContainerScreen<?>) ? ((AbstractContainerScreen<?>)parent).getGuiLeft() : 0);
      this.y = position.y + ((parent instanceof AbstractContainerScreen<?>) ? ((AbstractContainerScreen<?>)parent).getGuiTop() : 0);
      return this;
    }

    public int getWidth()
    { return this.width; }

    public int getHeight()
    { return this.height; }

    public UiWidget show()
    { visible = true; return this; }

    public UiWidget hide()
    { visible = false; return this; }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
      super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
      if(isHovered()) renderToolTip(matrixStack, mouseX, mouseY);
    }

    @Override
    @SuppressWarnings("all")
    public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
    {
      if(tooltip_ == NO_TOOLTIP) return;
      /// todo: need a Screen for that, not sure if adding a reference initialized in init() may cause GC problems.
    }

    @Override
    public void updateNarration(NarrationElementOutput element_output)
    {}
  }

  @OnlyIn(Dist.CLIENT)
  public static class HorizontalProgressBar extends UiWidget
  {
    private final Coord2d texture_position_base_;
    private final Coord2d texture_position_filled_;
    private final ResourceLocation atlas_;
    private double progress_max_ = 100;
    private double progress_ = 0;

    public HorizontalProgressBar(ResourceLocation atlas, int width, int height, Coord2d base_texture_xy, Coord2d filled_texture_xy)
    {
      super(0, 0, width, height, EMPTY_TEXT);
      atlas_ = atlas;
      texture_position_base_ = base_texture_xy;
      texture_position_filled_ = filled_texture_xy;
    }

    public HorizontalProgressBar setProgress(double progress)
    { progress_ = Mth.clamp(progress, 0, progress_max_); return this; }

    public double getProgress()
    { return progress_; }

    public HorizontalProgressBar setMaxProgress(double progress)
    { progress_max_ = Math.max(progress, 0); return this; }

    public double getMaxProgress()
    { return progress_max_; }

    public HorizontalProgressBar show()
    { visible = true; return this; }

    public HorizontalProgressBar hide()
    { visible = false; return this; }

    @Override
    public void playDownSound(SoundManager handler)
    {}

    @Override
    protected void renderBg(PoseStack mx, Minecraft mc, int x, int y)
    {}

    @Override
    public void renderButton(PoseStack mx, int mouseX, int mouseY, float partialTicks)
    {
      RenderSystem.setShaderTexture(0, atlas_);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      blit(mx, x, y, texture_position_base_.x, texture_position_base_.y, width, height);
      if((progress_max_ > 0) && (progress_ > 0)) {
        int w = Mth.clamp((int)Math.round((progress_ * width) / progress_max_), 0, width);
        blit(mx, x, y, texture_position_filled_.x, texture_position_filled_.y, w, height);
      }
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static class BackgroundImage extends UiWidget
  {
    private final ResourceLocation atlas_;
    private final Coord2d atlas_position_;
    public boolean visible;

    public BackgroundImage(ResourceLocation atlas, int width, int height, Coord2d atlas_position)
    {
      super(0, 0, width, height, EMPTY_TEXT);
      atlas_ = atlas;
      atlas_position_ = atlas_position;
      this.width = width;
      this.height = height;
      visible = true;
    }

    public void draw(PoseStack mx, Screen parent)
    {
      if(!visible) return;
      RenderSystem.setShaderTexture(0, atlas_);
      parent.blit(mx, x, y, atlas_position_.x, atlas_position_.y, width, height);
    }

  }

  @OnlyIn(Dist.CLIENT)
  public static class CheckBox extends UiWidget
  {
    private final Coord2d texture_position_off_;
    private final Coord2d texture_position_on_;
    private final ResourceLocation atlas_;
    private boolean checked_ = false;
    private Consumer<CheckBox> on_click_ = (checkbox)->{};

    public CheckBox(ResourceLocation atlas, int width, int height, Coord2d atlas_texture_position_off, Coord2d atlas_texture_position_on)
    {
      super(0, 0, width, height, EMPTY_TEXT);
      texture_position_off_ = atlas_texture_position_off;
      texture_position_on_ = atlas_texture_position_on;
      atlas_ = atlas;
    }

    public boolean checked()
    { return checked_; }

    public CheckBox checked(boolean on)
    { checked_ = on; return this; }

    public CheckBox onclick(Consumer<CheckBox> action)
    { on_click_ = action; return this; }

    @Override
    public void onClick(double mouseX, double mouseY)
    { checked_ = !checked_; on_click_.accept(this); }

    @Override
    public void renderButton(PoseStack mx, int mouseX, int mouseY, float partialTicks)
    {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, atlas_);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      Coord2d pos = checked_ ? texture_position_on_ : texture_position_off_;
      blit(mx, x, y, pos.x, pos.y, width, height);
    }

  }

}
