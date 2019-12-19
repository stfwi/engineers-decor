/*
 * @file ModTesrs.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2018 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Yet unstructured initial experiments with TESRs.
 * May be structured after I know what I am doing there.
 */
package wile.engineersdecor.detail;

import wile.engineersdecor.ModEngineersDecor;
import wile.engineersdecor.blocks.BlockDecorChair;
import wile.engineersdecor.blocks.BlockDecorCraftingTable;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.storage.MapData;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.matrix.MatrixStack;

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

    public void func_225623_a_/*render*/(T p_225623_1_, float p_225623_2_, float p_225623_3_, MatrixStack p_225623_4_, IRenderTypeBuffer p_225623_5_, int p_225623_6_)
    {}

    public Vec3d func_225627_b_/*likely getOffset*/(T p_225627_1_, float p_225627_2_)
    { return Vec3d.ZERO; }

    @SuppressWarnings("deprecation")
    public ResourceLocation getEntityTexture(T entity)
    { return AtlasTexture.LOCATION_BLOCKS_TEXTURE; }

    protected boolean canRenderName(T entity)
    { return false; }

    protected void func_225629_a_/*renderName/renderLabel*/(T p_225629_1_, String p_225629_2_, MatrixStack p_225629_3_, IRenderTypeBuffer p_225629_4_, int p_225629_5_)
    {}
  }




  //--------------------------------------------------------------------------------------------------------------------
  // Crafting table
  //--------------------------------------------------------------------------------------------------------------------

/*
  @OnlyIn(Dist.CLIENT)
  public static class TesrDecorCraftingTable extends TileEntityRenderer<BlockDecorCraftingTable.BTileEntity>
  {
    private static int tesr_error_counter = 4;
    private static double scaler = 0.10;
    private static double gap = 0.19;
    private static double yrotations[] = {0, 90, 180, 270}; // [hdirection] S-W-N-E
    private static double offsets[][][] = { // [hdirection][slotindex][xz]
      { {-1,-1},{+0,-1},{+1,-1}, {-1,+0},{+0,+0},{+1,+0}, {-1,+1},{+0,+1},{+1,+1} }, // S
      { {+1,-1},{+1,+0},{+1,+1}, {+0,-1},{+0,+0},{+0,+1}, {-1,-1},{-1,+0},{-1,+1} }, // W
      { {+1,+1},{+0,+1},{-1,+1}, {+1,+0},{+0,+0},{-1,+0}, {+1,-1},{+0,-1},{-1,-1} }, // N
      { {-1,+1},{-1,+0},{-1,-1}, {+0,+1},{+0,+0},{+0,-1}, {+1,+1},{+1,+0},{+1,-1} }, // E
    };

    @Override
    public void func_225616_a_(final BlockDecorCraftingTable.BTileEntity te, float p_225616_2_, MatrixStack p_225616_3_, IRenderTypeBuffer p_225616_4_, int p_225616_5_, int p_225616_6_)
    {
      render(final BlockDecorCraftingTable.BTileEntity te, double x, double y, double z, float partialTicks, int destroyStage);
    }

    @SuppressWarnings("deprecation")
    public void render(final BlockDecorCraftingTable.BTileEntity te, double x, double y, double z, float partialTicks, int destroyStage)
    {
      if(tesr_error_counter<=0) return;
      try {
        int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).get(BlockDecorCraftingTable.FACING).getHorizontalIndex(), 0, 3);
        long posrnd = te.getPos().toLong();
        posrnd = (posrnd>>16)^(posrnd<<1);
        for(int i=0; i<9; ++i) {
          final ItemStack stack = te.getStackInSlot(i);
          if(stack.isEmpty()) continue;
          double prnd = ((double)(((Integer.rotateRight(stack.getItem().hashCode()^(int)posrnd,(stack.getCount()+i)&31)))&1023))/1024.0;
          double rndo = gap * ((prnd*0.1)-0.05);
          double ox = gap * offsets[di][i][0], oz = gap * offsets[di][i][1];
          double oy = 0.5;
          double ry = ((yrotations[di]+180) + ((prnd*60)-30)) % 360;
          if(stack.isEmpty()) return;
          GlStateManager.pushMatrix();
          GlStateManager.disableLighting();
          RenderHelper.enableStandardItemLighting();
          GlStateManager.translated(x+0.5+ox, y+0.5+oy, z+0.5+oz);
          GlStateManager.rotated(90, 1, 0, 0);
          GlStateManager.rotated(ry, 0, 0, 1);
          GlStateManager.translated(rndo, rndo, 0);
          GlStateManager.scaled(scaler, scaler, scaler);
          Minecraft.getInstance().getItemRenderer().renderItem(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.FIXED);
          RenderHelper.disableStandardItemLighting();
          GlStateManager.enableLighting();
          GlStateManager.popMatrix();
        }
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger().error("TESR was disabled because broken, exception was: " + e.getMessage());
          ModEngineersDecor.logger().error(e.getStackTrace());
        }
      }
    }
  }
*/
}
