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
import wile.engineersdecor.blocks.BlockDecorCraftingTable;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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

  @OnlyIn(Dist.CLIENT)
  public static class CraftingTableTer extends TileEntityRenderer<BlockDecorCraftingTable.CraftingTableTileEntity>
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
    public void func_225616_a_(final BlockDecorCraftingTable.CraftingTableTileEntity te, float f2, MatrixStack mxs, IRenderTypeBuffer buf, int i5, int i6)
    { render(te, f2, mxs, buf, i5, i6); }

    @SuppressWarnings("deprecation")
    public void render(final BlockDecorCraftingTable.CraftingTableTileEntity te, float unused1, MatrixStack mxs, IRenderTypeBuffer buf, int i5, int i6)
    {
      if(tesr_error_counter <= 0) return;
      try {
        final int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).get(BlockDecorCraftingTable.CraftingTableBlock.FACING).getHorizontalIndex(), 0, 3);
        long posrnd = te.getPos().toLong();
        posrnd = (posrnd>>16)^(posrnd<<1);
        for(int i=0; i<9; ++i) {
          final ItemStack stack = te.getStackInSlot(i);
          if(stack.isEmpty()) continue;
          float prnd = ((float)(((Integer.rotateRight(stack.getItem().hashCode()^(int)posrnd,(stack.getCount()+i)&31)))&1023))/1024f;
          float rndo = gap * ((prnd*0.1f)-0.05f);
          float ox = gap * offsets[di][i][0], oz = gap * offsets[di][i][1];
          float oy = 0.5f;
          float ry = ((yrotations[di]+180) + ((prnd*60)-30)) % 360;
          if(stack.isEmpty()) return;
          mxs.func_227860_a_(); // mxs.push()
          mxs.func_227861_a_(0.5+ox, 0.5+oy, 0.5+oz); // mxs.translate()

          mxs.func_227863_a_(Vector3f.field_229179_b_.func_229187_a_(90.0f));  // mxs.transform(Vector3f.x_plus.rotation(90))

          mxs.func_227863_a_(Vector3f.field_229183_f_.func_229187_a_(ry)); // mxs.transform(Vector3f.z_plus.rotation(ry))

          mxs.func_227861_a_(rndo, rndo, 0); // mxs.translate()
          mxs.func_227862_a_(scaler, scaler, scaler); // mxs.scale()
          Minecraft.getInstance().getItemRenderer().func_229110_a_(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.FIXED, i5, i6, mxs, buf);
          mxs.func_227865_b_(); // mxs.pop()
        }
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger().error("TESR was disabled because broken, exception was: " + e.getMessage());
          ModEngineersDecor.logger().error(e.getStackTrace());
        }
      }
    }
  }
}
