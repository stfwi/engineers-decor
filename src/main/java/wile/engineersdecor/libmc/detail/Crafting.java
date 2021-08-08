/*
 * @file Recipes.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Recipe utility functionality.
 */
package wile.engineersdecor.libmc.detail;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;


public class Crafting
{
  // -------------------------------------------------------------------------------------------------------------------

  public static final class CraftingGrid extends CraftingContainer
  {
    protected static final CraftingGrid instance3x3 = new CraftingGrid(3,3);

    protected CraftingGrid(int width, int height)
    { super(new AbstractContainerMenu(null,0) { public boolean stillValid(Player player) { return false; } }, width, height); }

    protected void fill(Container grid)
    { for(int i=0; i<getContainerSize(); ++i) setItem(i, i>=grid.getContainerSize() ? ItemStack.EMPTY : grid.getItem(i)); }

    public List<CraftingRecipe> getRecipes(Level world, Container grid)
    { fill(grid); return world.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, this, world); }

    public List<ItemStack> getRemainingItems(Level world, Container grid, CraftingRecipe recipe)
    { fill(grid); return recipe.getRemainingItems(this); }

    public ItemStack getCraftingResult(Level world, Container grid, CraftingRecipe recipe)
    { fill(grid); return recipe.assemble(this); }
  }

  /**
   * Returns a Crafting recipe by registry name.
   */
  public static Optional<CraftingRecipe> getCraftingRecipe(Level world, ResourceLocation recipe_id)
  {
    Recipe<?> recipe = world.getRecipeManager().byKey(recipe_id).orElse(null);
    return (recipe instanceof CraftingRecipe) ? Optional.of((CraftingRecipe)recipe) : Optional.empty();
  }

  /**
   * Returns a list of matching recipes by the first N slots (crafting grid slots) of the given inventory.
   */
  public static List<CraftingRecipe> get3x3CraftingRecipes(Level world, Container crafting_grid_slots)
  { return CraftingGrid.instance3x3.getRecipes(world, crafting_grid_slots); }

  /**
   * Returns a recipe by the first N slots (crafting grid slots).
   */
  public static Optional<CraftingRecipe> get3x3CraftingRecipe(Level world, Container crafting_grid_slots)
  { return get3x3CraftingRecipes(world, crafting_grid_slots).stream().findFirst(); }

  /**
   * Returns the result item of the recipe with the given grid layout.
   */
  public static ItemStack get3x3CraftingResult(Level world, Container grid, CraftingRecipe recipe)
  { return CraftingGrid.instance3x3.getCraftingResult(world, grid, recipe); }

  /**
   * Returns the items remaining in the grid after crafting 3x3.
   */
  public static List<ItemStack> get3x3RemainingItems(Level world, Container grid, CraftingRecipe recipe)
  { return CraftingGrid.instance3x3.getRemainingItems(world, grid, recipe); }

  public static List<ItemStack> get3x3Placement(Level world, CraftingRecipe recipe, Container item_inventory, @Nullable Container crafting_grid)
  {
    final int width = 3;
    final int height = 3;
    if(!recipe.canCraftInDimensions(width,height)) return Collections.emptyList();
    List<ItemStack> used = new ArrayList<>();   //NonNullList.withSize(width*height);
    for(int i=width*height; i>0; --i) used.add(ItemStack.EMPTY);
    Container check_inventory = Inventories.copyOf(item_inventory);
    Inventories.InventoryRange source = new Inventories.InventoryRange(check_inventory);
    final List<Ingredient> ingredients = recipe.getIngredients();
    final List<ItemStack> preferred = new ArrayList<>(width*height);
    if(crafting_grid != null) {
      for(int i=0; i<crafting_grid.getContainerSize(); ++i) {
        ItemStack stack = crafting_grid.getItem(i);
        if(stack.isEmpty()) continue;
        stack = stack.copy();
        stack.setCount(1);
        if(!source.extract(stack).isEmpty()) preferred.add(stack);
      }
    }
    for(int i=0; i<ingredients.size(); ++i) {
      final Ingredient ingredient = ingredients.get(i);
      if(ingredient == Ingredient.EMPTY) continue;
      ItemStack stack = preferred.stream().filter(ingredient).findFirst().orElse(ItemStack.EMPTY);
      if(!stack.isEmpty()) {
        preferred.remove(stack);
      } else {
        stack = source.stream().filter(ingredient).findFirst().orElse(ItemStack.EMPTY);
        if(stack.isEmpty()) return Collections.emptyList();
        stack = stack.copy();
        stack.setCount(1);
        if(source.extract(stack).isEmpty()) return Collections.emptyList();
      }
      used.set(i, stack);
    }
    if(recipe instanceof ShapedRecipe shaped) {
      List<ItemStack> placement = NonNullList.withSize(width*height, ItemStack.EMPTY);
      for(int row=0; row<shaped.getRecipeHeight(); ++row) {
        for(int col=0; col<shaped.getRecipeWidth(); ++col) {
          placement.set(width*row+col, used.get(row*shaped.getRecipeWidth()+col));
        }
      }
      return placement;
    } else {
      return used;
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Returns the recipe for a given input stack to smelt, null if there is no recipe
   * for the given type (SMELTING,BLASTING,SMOKING, etc).
   */
  public static <T extends Recipe<?>> Optional<AbstractCookingRecipe> getFurnaceRecipe(RecipeType<T> recipe_type, Level world, ItemStack input_stack)
  {
    if(input_stack.isEmpty()) {
      return Optional.empty();
    } else if(recipe_type == RecipeType.SMELTING) {
      SimpleContainer inventory = new SimpleContainer(3);
      inventory.setItem(0, input_stack);
      SmeltingRecipe recipe = world.getRecipeManager().getRecipeFor(RecipeType.SMELTING, inventory, world).orElse(null);
      return (recipe==null) ? Optional.empty() : Optional.of(recipe);
    } else if(recipe_type == RecipeType.BLASTING) {
      SimpleContainer inventory = new SimpleContainer(3);
      inventory.setItem(0, input_stack);
      BlastingRecipe recipe = world.getRecipeManager().getRecipeFor(RecipeType.BLASTING, inventory, world).orElse(null);
      return (recipe==null) ? Optional.empty() : Optional.of(recipe);
    } else if(recipe_type == RecipeType.SMOKING) {
      SimpleContainer inventory = new SimpleContainer(3);
      inventory.setItem(0, input_stack);
      SmokingRecipe recipe = world.getRecipeManager().getRecipeFor(RecipeType.SMOKING, inventory, world).orElse(null);
      return (recipe==null) ? Optional.empty() : Optional.of(recipe);
    } else {
      return Optional.empty();
    }
  }

  public static <T extends Recipe<?>> int getSmeltingTimeNeeded(RecipeType<T> recipe_type, Level world, ItemStack stack)
  {
    if(stack.isEmpty()) return 0;
    final int t = getFurnaceRecipe(recipe_type, world, stack).map((AbstractCookingRecipe::getCookingTime)).orElse(0);
    return (t<=0) ? 200 : t;
  }

  /**
   * Returns the burn time of an item when used as fuel, 0 if it is no fuel.
   */
  public static int getFuelBurntime(Level world, ItemStack stack)
  {
    if(stack.isEmpty()) return 0;
    int t = ForgeHooks.getBurnTime(stack, null);
    return Math.max(t, 0);
  }

  /**
   * Returns true if an item can be used as fuel.
   */
  public static boolean isFuel(Level world, ItemStack stack)
  { return (getFuelBurntime(world, stack) > 0) || (stack.getItem()==Items.LAVA_BUCKET); }

  /**
   * Returns burntime and remaining stack then the item shall be used as fuel.
   */
  public static Tuple<Integer,ItemStack> consumeFuel(Level world, ItemStack stack)
  {
    if(stack.isEmpty()) return new Tuple<>(0,stack);
    int burnime = getFuelBurntime(world, stack);
    if((stack.getItem()==Items.LAVA_BUCKET)) {
      if(burnime <= 0) burnime = 1000*20;
      return new Tuple<>(burnime,new ItemStack(Items.BUCKET));
    } else if(burnime <= 0) {
      return new Tuple<>(0,stack);
    } else {
      ItemStack left_over = stack.copy();
      left_over.shrink(1);
      return new Tuple<>(burnime,left_over);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Returns true if the item can be used as brewing fuel.
   */
  public static boolean isBrewingFuel(Level world, ItemStack stack)
  { return (stack.getItem() == Items.BLAZE_POWDER) || (stack.getItem() == Items.BLAZE_ROD); }

  /**
   * Returns true if the item can be used as brewing ingredient.
   */
  public static boolean isBrewingIngredient(Level world, ItemStack stack)
  { return BrewingRecipeRegistry.isValidIngredient(stack); }

  /**
   * Returns true if the item can be used as brewing bottle.
   */
  public static boolean isBrewingInput(Level world, ItemStack stack)
  { return BrewingRecipeRegistry.isValidInput(stack); }

  /**
   * Returns the burn time for brewing of the given stack.
   */
  public static int getBrewingFuelBurntime(Level world, ItemStack stack)
  {
    if(stack.isEmpty()) return 0;
    if(stack.getItem() == Items.BLAZE_POWDER) return (400*20);
    if(stack.getItem() == Items.BLAZE_ROD) return (400*40);
    return 0;
  }

  /**
   * Returns brewing burn time and remaining stack if the item shall be used as fuel.
   */
  public static Tuple<Integer,ItemStack> consumeBrewingFuel(Level world, ItemStack stack)
  {
    int burntime = getBrewingFuelBurntime(world, stack);
    if(burntime <= 0) return new Tuple<>(0, stack.copy());
    stack = stack.copy();
    stack.shrink(1);
    return new Tuple<>(burntime, stack.isEmpty() ? ItemStack.EMPTY : stack);
  }

  public static final class BrewingOutput
  {
    public static final int DEFAULT_BREWING_TIME = 400;
    public static final BrewingOutput EMPTY = new BrewingOutput(ItemStack.EMPTY, new SimpleContainer(1), new SimpleContainer(1), 0,0, DEFAULT_BREWING_TIME);
    public final ItemStack item;
    public final Container potionInventory;
    public final Container ingredientInventory;
    public final int potionSlot;
    public final int ingredientSlot;
    public final int brewTime;

    public BrewingOutput(ItemStack output_potion, Container potion_inventory, Container ingredient_inventory, int potion_slot, int  ingredient_slot, int time_needed)
    {
      item = output_potion;
      potionInventory = potion_inventory;
      ingredientInventory = ingredient_inventory;
      potionSlot = potion_slot;
      ingredientSlot = ingredient_slot;
      brewTime = time_needed;
    }

    public static BrewingOutput find(Level world, Container potion_inventory, Container ingredient_inventory)
    {
      for(int potion_slot = 0; potion_slot<potion_inventory.getContainerSize(); ++potion_slot) {
        final ItemStack pstack = potion_inventory.getItem(potion_slot);
        if(!isBrewingInput(world, pstack)) continue;
        for(int ingredient_slot = 0; ingredient_slot<ingredient_inventory.getContainerSize(); ++ingredient_slot) {
          final ItemStack istack = ingredient_inventory.getItem(ingredient_slot);
          if((!isBrewingIngredient(world, istack)) || (ingredient_slot == potion_slot) || (isBrewingFuel(world, istack))) continue;
          final ItemStack result = BrewingRecipeRegistry.getOutput(pstack, istack);
          if(result.isEmpty()) continue;
          return new BrewingOutput(result, potion_inventory, ingredient_inventory, potion_slot, ingredient_slot, DEFAULT_BREWING_TIME);
        }
      }
      return BrewingOutput.EMPTY;
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static double getCompostingChance(ItemStack stack)
  { return ComposterBlock.COMPOSTABLES.getOrDefault(stack.getItem(),0); }

  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Returns the enchtments bound to the given stack.
   */
  public static Map<Enchantment, Integer> getEnchantmentsOnItem(Level world, ItemStack stack)
  { return (stack.isEmpty() || (stack.getTag()==null)) ? Collections.emptyMap() : EnchantmentHelper.getEnchantments(stack); }

  /**
   * Returns an enchanted book with the given enchantment, emtpy stack if not applicable.
   */
  public static ItemStack getEnchantmentBook(Level world, Enchantment enchantment, int level)
  { return ((!enchantment.isAllowedOnBooks()) || (level <= 0)) ? ItemStack.EMPTY : EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)); }

  /**
   * Returns the accumulated repair cost for the given enchantments.
   */
  public static int getEnchantmentRepairCost(Level world, Map<Enchantment, Integer> enchantments)
  {
    int repair_cost = 0;
    for(Map.Entry<Enchantment, Integer> e:enchantments.entrySet()) repair_cost = repair_cost * 2 + 1; // @see: RepairContainer.getNewRepairCost()
    return repair_cost;
  }

  /**
   * Trys to add an enchtment to the given stack, returns boolean success.
   */
  public static boolean addEnchantmentOnItem(Level world, ItemStack stack, Enchantment enchantment, int level)
  {
    if(stack.isEmpty() || (level <= 0) || (!stack.isEnchantable()) || (level >= enchantment.getMaxLevel())) return false;
    final Map<Enchantment, Integer> on_item = getEnchantmentsOnItem(world, stack);
    if(on_item.keySet().stream().anyMatch(ench-> ench.isCompatibleWith(enchantment))) return false;
    final ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level));
    if((!(stack.isBookEnchantable(book) && enchantment.isAllowedOnBooks())) && (!stack.canApplyAtEnchantingTable(enchantment)) && (!enchantment.canEnchant(stack))) return false;
    final int existing_level = on_item.getOrDefault(enchantment, 0);
    if(existing_level > 0) level = Mth.clamp(level+existing_level, 1, enchantment.getMaxLevel());
    on_item.put(enchantment, level);
    EnchantmentHelper.setEnchantments(on_item, stack);
    stack.setRepairCost(getEnchantmentRepairCost(world, on_item));
    return true;
  }

  /**
   * Removes enchantments from a stack, returns the removed enchantments.
   */
  public static Map<Enchantment, Integer> removeEnchantmentsOnItem(Level world, ItemStack stack, BiPredicate<Enchantment,Integer> filter)
  {
    if(stack.isEmpty()) return Collections.emptyMap();
    final Map<Enchantment, Integer> on_item = getEnchantmentsOnItem(world, stack);
    final Map<Enchantment, Integer> removed = new HashMap<>();
    for(Map.Entry<Enchantment, Integer> e:on_item.entrySet()) {
      if(filter.test(e.getKey(), e.getValue())) {
        removed.put(e.getKey(), e.getValue());
      }
    }
    for(Enchantment e:removed.keySet()) {
      on_item.remove(e);
    }
    EnchantmentHelper.setEnchantments(on_item, stack);
    stack.setRepairCost(getEnchantmentRepairCost(world, on_item));
    return removed;
  }


}