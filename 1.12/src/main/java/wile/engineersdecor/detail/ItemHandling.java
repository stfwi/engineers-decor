package wile.engineersdecor.detail;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;


public class ItemHandling
{

  public static IItemHandler itemhandler(World world, BlockPos pos, @Nullable EnumFacing side)
  {
    TileEntity te = world.getTileEntity(pos);
    if(te==null) return null;
    if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) return (IItemHandler)te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    if((side!=null) && (te instanceof ISidedInventory)) return new SidedInvWrapper((ISidedInventory)te, side);
    if(te instanceof IInventory) return new InvWrapper((IInventory)te);
    return null;
  }

  public static ItemStack insert(IItemHandler inventory, ItemStack stack , boolean simulate)
  { return ItemHandlerHelper.insertItemStacked(inventory, stack, simulate); }

  public static ItemStack insert(TileEntity te, @Nullable EnumFacing side, ItemStack stack, boolean simulate)
  {
    if(te==null) return stack;
    IItemHandler hnd = null;
    if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
      hnd = (IItemHandler)te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
    } else if((side!=null) &&  (te instanceof ISidedInventory)) {
      hnd = new SidedInvWrapper((ISidedInventory)te, side);
    } else if(te instanceof IInventory) {
      hnd = new InvWrapper((IInventory)te);
    }
    return (hnd==null) ? stack : ItemHandlerHelper.insertItemStacked(hnd, stack, simulate);
  }

  public static ItemStack extract(IItemHandler inventory, @Nullable ItemStack match, int amount, boolean simulate)
  {
    if((inventory==null) || (amount<=0)) return ItemStack.EMPTY;
    final int max = inventory.getSlots();
    ItemStack out_stack = ItemStack.EMPTY;
    for(int i=0; i<max; ++i) {
      final ItemStack stack = inventory.getStackInSlot(i);
      if(stack.isEmpty()) continue;
      if(out_stack.isEmpty()) {
        if((match!=null) && (!stack.isItemEqual(match))) continue;
        out_stack = inventory.extractItem(i, amount, simulate);
      } else if(stack.isItemEqual(out_stack)) {
        ItemStack es = inventory.extractItem(i, (amount-out_stack.getCount()), simulate);
        out_stack.grow(es.getCount());
      }
      if(out_stack.getCount() >= amount) break;
    }
    return out_stack;
  }


}
