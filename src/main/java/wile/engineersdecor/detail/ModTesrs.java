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
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wile.engineersdecor.blocks.BlockDecorLabeledCrate;
import wile.engineersdecor.blocks.BlockDecorTest;

public class ModTesrs
{
  //--------------------------------------------------------------------------------------------------------------------
  // Crafting table
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  public static class TesrDecorCraftingTable extends TileEntitySpecialRenderer<BlockDecorCraftingTable.BTileEntity>
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
    public void render(final BlockDecorCraftingTable.BTileEntity te, final double x, final double y, final double z, final float partialTicks, final int destroyStage, final float alpha)
    {
      if(tesr_error_counter<=0) return;
      try {
        int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).getValue(BlockDecorCraftingTable.FACING).getHorizontalIndex(), 0, 3);
        long posrnd = te.getPos().toLong();
        posrnd = (posrnd>>16)^(posrnd<<1);
        for(int i=0; i<BlockDecorCraftingTable.BTileEntity.NUM_OF_CRAFTING_SLOTS; ++i) {
          final ItemStack stack = te.getStackInSlot(BlockDecorCraftingTable.BTileEntity.CRAFTING_SLOTS_BEGIN+i);
          if(stack.isEmpty()) continue;
          boolean isblock = (stack.getItem() instanceof ItemBlock);
          double prnd = ((double)(((Integer.rotateRight(stack.getItem().hashCode()^(int)posrnd,(stack.getCount()+i)&31)))&1023))/1024.0;
          double rndo = gap * ((prnd*0.1)-0.05);
          double ox = gap * offsets[di][i][0], oz = gap * offsets[di][i][1];
          double oy = 0.5;
          double ry = ((yrotations[di]+180) + ((prnd*60)-30)) % 360;
          if(stack.isEmpty()) return;
          GlStateManager.pushMatrix();
          GlStateManager.disableLighting();
          RenderHelper.enableStandardItemLighting();
          GlStateManager.translate(x+0.5+ox, y+0.5+oy, z+0.5+oz);
          GlStateManager.rotate((float)90, 1, 0, 0);
          GlStateManager.rotate((float)ry, 0, 0, 1);
          GlStateManager.translate(rndo, rndo, 0);
          GlStateManager.scale(scaler, scaler, scaler);
          Minecraft.getMinecraft().getRenderItem().renderItem(stack, TransformType.FIXED);
          RenderHelper.disableStandardItemLighting();
          GlStateManager.enableLighting();
          GlStateManager.popMatrix();
        }
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger.error("Crafting Table TESR was disabled because broken, exception was: " + e.getMessage());
          ModEngineersDecor.logger.error(e.getStackTrace());
        }
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Labeled Crate
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  public static class TesrDecorLabeledCrate extends TileEntitySpecialRenderer<BlockDecorLabeledCrate.LabeledCrateTileEntity>
  {
    private static int tesr_error_counter = 4;
    private static double scaler = 0.35;
    double tr[][]= { // [hdirection=S-W-N-E][param]
      {  +8.0/32, -8.0/32, +15.5/32, 180.0 }, // N
      { -15.5/32, -8.0/32,  +8.0/32,  90.0 }, // E
      {  -8.0/32, -8.0/32, -15.5/32,   0.0 }, // S param=tx,ty,tz,ry
      { +15.5/32, -8.0/32,  -8.0/32, 270.0 }, // W
    };

    @Override
    public void render(final BlockDecorLabeledCrate.LabeledCrateTileEntity te, final double x, final double y, final double z, final float partialTicks, final int destroyStage, final float alpha)
    {
      if(tesr_error_counter<=0) return;
      try {
        final ItemStack stack = te.getItemFrameStack();
        if(stack.isEmpty()) return;
        final int di = MathHelper.clamp(te.getWorld().getBlockState(te.getPos()).getValue(BlockDecorLabeledCrate.FACING).getHorizontalIndex(), 0, 3);
        double ox = tr[di][0], oy = tr[di][1], oz = tr[di][2], ry = tr[di][3];
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(x+0.5+ox, y+0.5+oy, z+0.5+oz);
        GlStateManager.rotate((float)ry, 0, 1, 0);
        GlStateManager.scale(scaler, scaler, scaler);
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          ModEngineersDecor.logger.error("Labeled Crate TESR was disabled (because broken), exception was: " + e.getMessage());
        }
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Test Block
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  public static class TesrDecorTest extends TileEntitySpecialRenderer<BlockDecorTest.BTileEntity>
  {
    @Override
    public void render(final BlockDecorTest.BTileEntity te, double x, double y, double z, final float partialTicks, final int destroyStage, final float alpha)
    {
      if(te.progress() < 0) return;
      renderBlockState(Blocks.SANDSTONE.getDefaultState(), te.getPos(), (new Vec3d(1,1,1)).scale(te.progress()), x,y,z);
    }

    public void renderBlockState(IBlockState state, BlockPos pos, Vec3d offset, double basex, double basey, double basez)
    {
      if(state.getMaterial() == Material.AIR) return;
      BlockRendererDispatcher brd = Minecraft.getMinecraft().getBlockRendererDispatcher();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bb = tessellator.getBuffer();
      bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.enableBlend();
      GlStateManager.disableCull();
      GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? 7425 : 7424);
      bb.begin(7, DefaultVertexFormats.BLOCK);
      bb.setTranslation(basex-(double)pos.getX()+offset.x, basey-(double)pos.getY()+offset.y,basez-(double)pos.getZ()+offset.z);
      final boolean checkSides=true;
      brd.getBlockModelRenderer().renderModel(getWorld(), brd.getModelForState(state), state, pos, bb, checkSides);
      bb.setTranslation(0.0, 0.0, 0.0);
      tessellator.draw();
      RenderHelper.enableStandardItemLighting();
    }
  }
}
