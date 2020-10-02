/*
 * @file EdTestBlock.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Creative mod testing block
 */
package wile.engineersdecor.blocks;

import wile.engineersdecor.ModContent;
import wile.engineersdecor.libmc.detail.Auxiliaries;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class EdTestBlock
{
  //--------------------------------------------------------------------------------------------------------------------
  // Block
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestBlock extends DecorBlock.Directed implements Auxiliaries.IExperimentalFeature, IDecorBlock
  {
    public TestBlock(long config, Block.Properties builder, final AxisAlignedBB unrotatedAABB)
    { super(config, builder, unrotatedAABB); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext selectionContext)
    { return VoxelShapes.fullCube(); }

    @Override
    public boolean hasTileEntity(BlockState state)
    { return true; }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    { return new TestTileEntity(); }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
    { return true; }

    @Override
    public boolean hasDynamicDropList()
    { return true; }

    @Override
    public List<ItemStack> dropList(BlockState state, World world, TileEntity te, boolean explosion)
    {
      ArrayList<ItemStack> list = new ArrayList<ItemStack>();
      list.add(new ItemStack(this, 1));
      return list;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {
      TileEntity te = world.getTileEntity(pos);
      if(!(te instanceof TestTileEntity)) return ActionResultType.SUCCESS;
      ((TestTileEntity)te).activated(player, hand, hit);
      return ActionResultType.SUCCESS;
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity
  //--------------------------------------------------------------------------------------------------------------------

  public static class TestTileEntity extends TileEntity implements ITickableTileEntity
  {
    private int tick_interval_ = 10;
    private int tick_timer_ = 0;

    // ------------------------------------------------------------------------------------------

    public TestTileEntity()
    { this(ModContent.TET_TEST_BLOCK); }

    public TestTileEntity(TileEntityType<?> te_type)
    { super(te_type); }

    // ------------------------------------------------------------------------------------------

    public void activated(PlayerEntity player, Hand hand, BlockRayTraceResult hit)
    {}

    // TileEntity ------------------------------------------------------------------------------

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    { super.read(state, nbt); }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    { super.write(nbt); return nbt; }

    @Override
    public void remove()
    { super.remove(); }

    @Override
    public void tick()
    {
      if((world.isRemote) || (--tick_timer_ > 0)) return;
      tick_timer_ = tick_interval_;
    }

  }
}
