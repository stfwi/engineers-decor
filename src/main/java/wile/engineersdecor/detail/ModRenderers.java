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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import wile.engineersdecor.blocks.EdCraftingTable;
import wile.engineersdecor.blocks.EdCraftingTable.CraftingTableBlock;
import wile.engineersdecor.blocks.EdLabeledCrate;
import wile.engineersdecor.libmc.detail.Auxiliaries;


public class ModRenderers
{
  //--------------------------------------------------------------------------------------------------------------------
  // InvisibleEntityRenderer
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class InvisibleEntityRenderer<T extends Entity> extends EntityRenderer<T>
  {
    private final Minecraft mc = Minecraft.getInstance();

    public InvisibleEntityRenderer(EntityRendererProvider.Context context)
    { super(context); }

    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight)
    {}

    public Vec3 getRenderOffset(T entity, float partialTicks)
    { return Vec3.ZERO; }

    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(T entity)
    { return TextureAtlas.LOCATION_BLOCKS; }

    protected boolean shouldShowName(T entity)
    { return false; }

    protected void renderNameTag(T entity, Component displayName, PoseStack matrixStack, MultiBufferSource buffer, int packedLight)
    {}
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Crafting table
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class CraftingTableTer implements BlockEntityRenderer<EdCraftingTable.CraftingTableTileEntity>
  {
    private static int tesr_error_counter = 16;
    private static final float scaler = 0.1f;
    private static final float gap = 0.19f;
    private static final float[] yrotations = {0, 90, 180, 270}; // [hdirection] S-W-N-E
    private static final float[][][] offsets = { // [hdirection][slotindex][xz]
      { {-1,-1},{+0,-1},{+1,-1}, {-1,+0},{+0,+0},{+1,+0}, {-1,+1},{+0,+1},{+1,+1} }, // S
      { {+1,-1},{+1,+0},{+1,+1}, {+0,-1},{+0,+0},{+0,+1}, {-1,-1},{-1,+0},{-1,+1} }, // W
      { {+1,+1},{+0,+1},{-1,+1}, {+1,+0},{+0,+0},{-1,+0}, {+1,-1},{+0,-1},{-1,-1} }, // N
      { {-1,+1},{-1,+0},{-1,-1}, {+0,+1},{+0,+0},{+0,-1}, {+1,+1},{+1,+0},{+1,-1} }, // E
    };
    private final BlockEntityRendererProvider.Context renderer_;

    public CraftingTableTer(BlockEntityRendererProvider.Context renderer)
    { this.renderer_ = renderer; }

    @Override
    @SuppressWarnings("deprecation")
    public void render(final EdCraftingTable.CraftingTableTileEntity te, float unused1, PoseStack mxs, MultiBufferSource buf, int i5, int overlayTexture)
    {
      if(tesr_error_counter <= 0) return;
      try {
        final BlockState state = te.getLevel().getBlockState(te.getBlockPos());
        if(!(state.getBlock() instanceof EdCraftingTable.CraftingTableBlock)) return;
        final int di = Mth.clamp(state.getValue(CraftingTableBlock.HORIZONTAL_FACING).get2DDataValue(), 0, 3);
        long posrnd = te.getBlockPos().asLong();
        posrnd = (posrnd>>16)^(posrnd<<1);
        for(int i=0; i<9; ++i) {
          final ItemStack stack = te.mainInventory().getItem(i);
          if(stack.isEmpty()) continue;
          float prnd = ((float)(((Integer.rotateRight(stack.getItem().hashCode()^(int)posrnd,(stack.getCount()+i)&31)))&1023))/1024f;
          float rndo = gap * ((prnd*0.1f)-0.05f);
          float ox = gap * offsets[di][i][0], oz = gap * offsets[di][i][1];
          float oy = 0.5f;
          float ry = ((yrotations[di]+180) + ((prnd*60)-30)) % 360;
          if(stack.isEmpty()) return;
          mxs.pushPose();
          mxs.translate(0.5+ox, 0.5+oy, 0.5+oz);
          mxs.mulPose(Vector3f.XP.rotationDegrees(90.0f));
          mxs.mulPose(Vector3f.ZP.rotationDegrees(ry));
          mxs.translate(rndo, rndo, 0);
          mxs.scale(scaler, scaler, scaler);
          Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.FIXED, i5, overlayTexture, mxs, buf, 0);
          mxs.popPose();
        }
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          Auxiliaries.logger().error("TER was disabled because broken, exception was: " + e.getMessage());
          Auxiliaries.logger().error(e.getStackTrace());
        }
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Labeled Crate
  //--------------------------------------------------------------------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public static class DecorLabeledCrateTer implements BlockEntityRenderer<EdLabeledCrate.LabeledCrateTileEntity>
  {
    private static int tesr_error_counter = 4;
    private static final float scaler = 0.35f;
    private static final double[][] tr = { // [hdirection=S-W-N-E][param]
      {  +8.0/32, -8.0/32, +15.5/32, 180.0 }, // N
      { -15.5/32, -8.0/32,  +8.0/32,  90.0 }, // E
      {  -8.0/32, -8.0/32, -15.5/32,   0.0 }, // S param=tx,ty,tz,ry
      { +15.5/32, -8.0/32,  -8.0/32, 270.0 }, // W
    };
    private final BlockEntityRendererProvider.Context renderer_;

    public DecorLabeledCrateTer(BlockEntityRendererProvider.Context renderer)
    { this.renderer_ = renderer; }

    @Override
    @SuppressWarnings("deprecation")
    public void render(final EdLabeledCrate.LabeledCrateTileEntity te, float unused1, PoseStack mxs, MultiBufferSource buf, int i5, int overlayTexture)
    {
      if(tesr_error_counter<=0) return;
      try {
        final ItemStack stack = te.getItemFrameStack();
        if(stack.isEmpty()) return;
        final BlockState state = te.getLevel().getBlockState(te.getBlockPos());
        if(!(state.getBlock() instanceof EdLabeledCrate.LabeledCrateBlock)) return;
        final int di = Mth.clamp(state.getValue(EdLabeledCrate.LabeledCrateBlock.HORIZONTAL_FACING).get2DDataValue(), 0, 3);
        double ox = tr[di][0], oy = tr[di][1], oz = tr[di][2];
        float ry = (float)tr[di][3];
        mxs.pushPose();
        mxs.translate(0.5+ox, 0.5+oy, 0.5+oz);
        mxs.mulPose(Vector3f.YP.rotationDegrees(ry));
        mxs.scale(scaler, scaler, scaler);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemTransforms.TransformType.FIXED, i5, overlayTexture, mxs, buf, 0);
        mxs.popPose();
      } catch(Throwable e) {
        if(--tesr_error_counter<=0) {
          Auxiliaries.logger().error("TER was disabled (because broken), exception was: " + e.getMessage());
        }
      }
    }
  }
}
