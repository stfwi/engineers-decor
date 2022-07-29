package wile.engineersdecor.libmc;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BiConsumer;

public class Containers
{
  // -------------------------------------------------------------------------------------------------------------------
  // Slots
  // -------------------------------------------------------------------------------------------------------------------

  public static class StorageSlot extends Slot
  {
    protected BiConsumer<ItemStack, ItemStack> slot_change_action_ = (oldStack, newStack)->{};
    protected int stack_limit_ = 64;
    public boolean enabled = true;

    public StorageSlot(Container inventory, int index, int x, int y)
    { super(inventory, index, x, y); }

    public StorageSlot setSlotStackLimit(int limit)
    { stack_limit_ = Mth.clamp(limit, 1, 64); return this; }

    public int getMaxStackSize()
    { return stack_limit_; }

    public StorageSlot setSlotChangeNotifier(BiConsumer<ItemStack, ItemStack> action)
    { slot_change_action_ = action; return this; }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack)
    { slot_change_action_.accept(oldStack, newStack);} // no crafting trigger

    @Override
    public void set(ItemStack stack)
    {
      if(stack.sameItem(getItem())) {
        super.set(stack);
      } else {
        final ItemStack before = getItem().copy();
        super.set(stack); // whatever this does else next to setting inventory.
        slot_change_action_.accept(before, getItem());
      }
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    { return enabled && this.container.canPlaceItem(this.getSlotIndex(), stack); }

    @Override
    public int getMaxStackSize(ItemStack stack)
    { return Math.min(getMaxStackSize(), stack_limit_); }

    @OnlyIn(Dist.CLIENT)
    public boolean isActive()
    { return enabled; }
  }

  public static class LockedSlot extends Slot
  {
    protected int stack_limit_ = 64;
    public boolean enabled = true;

    public LockedSlot(Container inventory, int index, int x, int y)
    { super(inventory, index, x, y); }

    public LockedSlot setSlotStackLimit(int limit)
    { stack_limit_ = Mth.clamp(limit, 1, 64); return this; }

    public int getMaxStackSize()
    { return stack_limit_; }

    @Override
    public int getMaxStackSize(ItemStack stack)
    { return Math.min(getMaxStackSize(), stack_limit_); }

    @Override
    public boolean mayPlace(ItemStack stack)
    { return false; }

    @Override
    public boolean mayPickup(Player player)
    { return false; }

    @OnlyIn(Dist.CLIENT)
    public boolean isActive()
    { return enabled; }
  }

  public static class HiddenSlot extends Slot
  {
    public HiddenSlot(Container inventory, int index)
    { super(inventory, index, 0, 0); }

    @Override
    public int getMaxStackSize(ItemStack stack)
    { return getMaxStackSize(); }

    @Override
    public boolean mayPlace(ItemStack stack)
    { return false; }

    @Override
    public boolean mayPickup(Player player)
    { return false; }

    @OnlyIn(Dist.CLIENT)
    public boolean isActive()
    { return false; }
  }

}
