/*
 * @file ModTesrs.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Yet unstructured initial experiments with TESRs.
 * May be structured after I know what I am doing there.
 */
package wile.engineersdecor.detail;

import net.minecraft.util.text.ITextComponent;
import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.EdCraftingTable;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.matrix.MatrixStack;
import wile.engineersdecor.blocks.EdCraftingTable.CraftingTableBlock;
import wile.engineersdecor.blocks.EdLabeledCrate;


public class ModRenderers
{
  //--------------------------------------------------------------------------------------------------------------------
  // InvisibleEntityRenderer
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class InvisibleEntityRenderer<T extends Entity> extends EntityRenderer<T>
  {
    private final Minecraft mc = Minecraft.getInstance();

    public InvisibleEntityRenderer(EntityRendererManager renderManagerIn)
    { super(renderManagerIn); }

    public void render(T entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight)
    {}

    public Vector3d getRenderOffset(T entity, float partialTicks)
    { return Vector3d.ZERO; }

    @SuppressWarnings("deprecation")
    public ResourceLocation getEntityTexture(T entity)
    { return AtlasTexture.LOCATION_BLOCKS_TEXTURE; }

    protected boolean canRenderName(T entity)
    { return false; }

    protected void renderName(T entity, ITextComponent displayName, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight)
    {}
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting table
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class CraftingTableTer extends TileEntityRenderer<EdCraftingTable.CraftingTableTileEntity>
  {
    private static int tesr_error_counter = 4;
    private static float scaler = 0.1f;
    private static float gap = 0.19f;
    private static float yrotations[] = {0, 90, 180, 270}; // [hdirection] S-W-N-E
    private static float offsets[][][] = { // [hdirection][slotindex][xz]
      { {-1,-1},{+0,-1},{+1,-1}, {-1,+0},{+0,+0},{+1,+0}, {-1,+1},{+0,+1},{+1,+1} }, // S
      { {+1,-1},{+1,+0},{+1,+1}, {+0,-1},{+0,+0},{+0,+1}, {-1,-1},{-1,+0},{-1,+1} }, // W
      { {+1,+1},{+0,+1},{-1,+1}, {+1,+0},{+0,+0},{-1,+0}, {+1,-1},{+0,-1},{-1,-1} }, // N
      { {-1,+1},{-1,+0},{-1,-1}, {+0,+1},{+0,+0},{+0,-1}, {+1,+1},{+1,+0},{+1,-1} }, // E
    };

    public CraftingTableTer(TileEntityRendererDispatcher dispatcher)
    { super(dispatcher); }

    @Override
    @SuppressWarnings("deprecation")
    public void render(final EdCraftingTable.CraftingTableTileEntity te, float unused1, MatrixStack mxs, IRenderTypeBuffer buf, int i5, int i6)
    {
      if(tesr_error_counter <= 0) return;
      try {
        final int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).get(CraftingTableBlock.HORIZONTAL_FACING).getHorizontalIndex(), 0, 3);
        long posrnd = te.getPos().toLong();
        posrnd = (posrnd>>16)^(posrnd<<1);
        for(int i=0; i<9; ++i) {
          final ItemStack stack = te.mainInventory().getStackInSlot(i);
          if(stack.isEmpty()) continue;
          float prnd = ((float)(((Integer.rotateRight(stack.getItem().hashCode()^(int)posrnd,(stack.getCount()+i)&31)))&1023))/1024f;
          float rndo = gap * ((prnd*0.1f)-0.05f);
          float ox = gap * offsets[di][i][0], oz = gap * offsets[di][i][1];
          float oy = 0.5f;
          float ry = ((yrotations[di]+180) + ((prnd*60)-30)) % 360;
          if(stack.isEmpty()) return;
          mxs.push();
          mxs.translate(0.5+ox, 0.5+oy, 0.5+oz);
          mxs.rotate(Vector3f.XP.rotationDegrees(90.0f));
          mxs.rotate(Vector3f.ZP.rotationDegrees(ry));
          mxs.translate(rndo, rndo, 0);
          mxs.scale(scaler, scaler, scaler);
          Minecraft.getInstance().getItemRenderer().renderItem(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.FIXED, i5, i6, mxs, buf);
          mxs.pop();
        }
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger().error("TER was disabled because broken, exception was: " + e.getMessage());
          ModEngineersDecor.logger().error(e.getStackTrace());
        }
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Labeled Crate
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class DecorLabeledCrateTer extends TileEntityRenderer<EdLabeledCrate.LabeledCrateTileEntity>
  {
    private static int tesr_error_counter = 4;
    private static float scaler = 0.35f;
    double tr[][]= { // [hdirection=S-W-N-E][param]
      {  +8.0/32, -8.0/32, +15.5/32, 180.0 }, // N
      { -15.5/32, -8.0/32,  +8.0/32,  90.0 }, // E
      {  -8.0/32, -8.0/32, -15.5/32,   0.0 }, // S param=tx,ty,tz,ry
      { +15.5/32, -8.0/32,  -8.0/32, 270.0 }, // W
    };

    public DecorLabeledCrateTer(TileEntityRendererDispatcher dispatcher)
    { super(dispatcher); }

    @Override
    @SuppressWarnings("deprecation")
    public void render(final EdLabeledCrate.LabeledCrateTileEntity te, float unused1, MatrixStack mxs, IRenderTypeBuffer buf, int i5, int i6)
    {
      if(tesr_error_counter<=0) return;
      try {
        final ItemStack stack = te.getItemFrameStack();
        if(stack.isEmpty()) return;
        final int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).get(EdLabeledCrate.LabeledCrateBlock.HORIZONTAL_FACING).getHorizontalIndex(), 0, 3);
        double ox = tr[di][0], oy = tr[di][1], oz = tr[di][2];
        float ry = (float)tr[di][3];
        mxs.push();
        //GlStateManager.disableLighting();
        //RenderHelper.enableStandardItemLighting();
        mxs.translate(0.5+ox, 0.5+oy, 0.5+oz);
        mxs.rotate(Vector3f.YP.rotationDegrees(ry));
        mxs.scale(scaler, scaler, scaler);
        Minecraft.getInstance().getItemRenderer().renderItem(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.FIXED, i5, i6, mxs, buf);
        //RenderHelper.disableStandardItemLighting();
        //GlStateManager.enableLighting();
        mxs.pop();
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger().error("TER was disabled (because broken), exception was: " + e.getMessage());
        }
      }
    }
  }
}
