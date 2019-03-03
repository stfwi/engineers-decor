/*
 * @file BlockDecorFull.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Full block characteristics class.
 */
package wile.engineersdecor.blocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wile.engineersdecor.ModEngineersDecor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class BlockDecorCraftingTable extends BlockDecorDirected
{
  public BlockDecorCraftingTable(@Nonnull String registryName, long config, @Nullable Material material, float hardness, float resistance, @Nullable SoundType sound, @Nonnull AxisAlignedBB unrotatedAABB)
  {
    super(registryName, config, material, hardness, resistance, sound, unrotatedAABB);
    setLightOpacity(0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
  { return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()); }

  @Override
  public boolean hasTileEntity(IBlockState state)
  { return true; }

  @Nullable
  public TileEntity createTileEntity(World world, IBlockState state)
  { return new BlockDecorCraftingTable.BEntity(); }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
  {
    if(world.isRemote) return true;
    player.openGui(ModEngineersDecor.instance, ModEngineersDecor.GuiHandler.GUIID_CRAFTING_TABLE, world, pos.getX(), pos.getY(), pos.getZ());
    return true;
  }

  //--------------------------------------------------------------------------------------------------------------------
  // TE
  //--------------------------------------------------------------------------------------------------------------------
  public static class BEntity extends TileEntity
  {
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Container
  //--------------------------------------------------------------------------------------------------------------------

  public static class BContainer extends ContainerWorkbench
  {
    private World world;
    private BlockPos pos;
    private EntityPlayer player;

    public BContainer(InventoryPlayer inv, World world, BlockPos pos)
    {
      super(inv, world, pos);
      this.world = world;
      this.pos = pos;
      this.player = inv.player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    { return (world.getBlockState(this.pos).getBlock() instanceof BlockDecorCraftingTable) && (playerIn.getDistanceSq(this.pos) <= 8*8); }

    @Override
    public void onCraftMatrixChanged(IInventory inv)
    {
      try {
        slotChangedCraftingGrid(this.world, player, this.craftMatrix, this.craftResult);
      } catch(Throwable exc) {
        ModEngineersDecor.logger.error("Recipe failed:", exc);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // GUI
  //--------------------------------------------------------------------------------------------------------------------

  @SideOnly(Side.CLIENT)
  public static class BGuiCrafting extends GuiContainer
  {
    public BGuiCrafting(InventoryPlayer playerInventory, World world, BlockPos pos)
    { super(new BContainer(playerInventory, world, pos)); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    { super.drawScreen(mouseX, mouseY, partialTicks); renderHoveredToolTip(mouseX, mouseY); }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {}

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(new ResourceLocation(ModEngineersDecor.MODID, "textures/gui/treated_wood_crafting_table.png"));
      drawTexturedModalRect(((this.width - this.xSize)/2), ((this.height - this.ySize)/2), 0, 0, this.xSize, this.ySize);
    }
  }

}
