package MCplugin2.newplugin;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.*;

public class CustomCraftingListener implements Listener {

    private final RecipeManager recipeManager;
    private final Map<UUID, CraftingPlacement> playerCraftingPlacement = new HashMap<>();

    public CustomCraftingListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Bukkit.getLogger().info("DEBUG: onPrepareCraft triggered");

        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();


        findBestMatch(matrix).ifPresentOrElse(
                bestPlacement -> {
                    playerCraftingPlacement.put(player.getUniqueId(), bestPlacement);
                    inventory.setResult(bestPlacement.getRecipe().getResult());
                },
                () -> playerCraftingPlacement.remove(player.getUniqueId())
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player player)) return;

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        Optional<CraftingPlacement> maybePlacement = findBestMatch(matrix);
        if (maybePlacement.isEmpty()) return;
        CraftingPlacement placement = maybePlacement.get();

        if (event.getRecipe() instanceof Keyed keyed && !keyed.getKey().equals(placement.getRecipe().getKey())) {
            return;
        }

        ShapedRecipe recipe = placement.getRecipe();
        Map<Character, ItemStack> ingredients = recipeManager.getCustomShapedIngredients(recipe.getKey());
        if (ingredients == null) return;

        event.setCancelled(true);

        String[] shape = recipe.getShape();
        int gridWidth = (int) Math.sqrt(matrix.length); // 3 for 3x3, 2 for 2x2

        Map<Integer, Integer> amountPerSlot = new HashMap<>();
        Map<Integer, ItemStack> templatePerSlot = new HashMap<>();

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length(); c++) {
                char ch = shape[r].charAt(c);
                if (ch == ' ') continue;
                ItemStack req = ingredients.get(ch);
                if (req == null) {
                    return;
                }
                int slotIndex = (r + placement.getOffsetY()) * gridWidth + (c + placement.getOffsetX());
                amountPerSlot.merge(slotIndex, req.getAmount(), Integer::sum);
                templatePerSlot.putIfAbsent(slotIndex, req.clone());
            }
        }

        int maxCraftsFromGrid = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> e : amountPerSlot.entrySet()) {
            int slot = e.getKey();
            int perCraft = e.getValue();
            ItemStack found = matrix[slot];
            ItemStack templ = templatePerSlot.get(slot);

            if (found == null || templ == null) {
                maxCraftsFromGrid = 0;
                break;
            }
            if (!found.isSimilar(templ)) {
                maxCraftsFromGrid = 0;
                break;
            }
            maxCraftsFromGrid = Math.min(maxCraftsFromGrid, found.getAmount() / perCraft);
        }
        if (maxCraftsFromGrid == Integer.MAX_VALUE) maxCraftsFromGrid = 0;

        // final craft amount: shift-click wants as many as possible but limited by placement.getMaxCrafts()
        int craftAmount = event.isShiftClick() ? Math.min(placement.getMaxCrafts(), maxCraftsFromGrid) : 1;
        if (craftAmount <= 0) {
            return;
        }


        // Remove items from each slot
        for (Map.Entry<Integer, Integer> e : amountPerSlot.entrySet()) {
            int slot = e.getKey();
            int removeTotal = e.getValue() * craftAmount;
            ItemStack found = matrix[slot];
            if (found == null) continue;

            int remaining = found.getAmount() - removeTotal;
            if (remaining > 0) {
                found.setAmount(remaining);
                matrix[slot] = found;
            } else {
                matrix[slot] = null;
            }
        }

        inventory.setMatrix(matrix);
        inventory.setResult(null);


        ItemStack out = recipe.getResult().clone();
        out.setAmount(out.getAmount() * craftAmount);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(out);
        leftovers.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));


        for (int i = 0; i < matrix.length; i++) {
            ItemStack it = matrix[i];
            Bukkit.getLogger().info(" slot " + i + " -> " + (it == null ? "EMPTY" : it.getType() + " x" + it.getAmount()));
        }

        player.updateInventory();
    }



    private Optional<CraftingPlacement> findBestMatch(ItemStack[] matrix) {
        List<CraftingPlacement> potentialPlacements = new ArrayList<>();
        int gridWidth = (int) Math.sqrt(matrix.length);

        Set<NamespacedKey> customKeys = recipeManager.getCustomShapedRecipeKeys();

        if (customKeys.isEmpty()) {
            return Optional.empty();
        }

        for (NamespacedKey key : customKeys) {

            Recipe recipe = Bukkit.getRecipe(key);
            if (!(recipe instanceof ShapedRecipe)) {
                continue;
            }
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

            Map<Character, ItemStack> customIngredients = recipeManager.getCustomShapedIngredients(key);
            if (customIngredients == null) {
                continue;
            }


            for (int y = 0; y <= gridWidth - shapedRecipe.getShape().length; y++) {
                for (int x = 0; x <= gridWidth - shapedRecipe.getShape()[0].length(); x++) {


                    if (matches(shapedRecipe, customIngredients, matrix, gridWidth, y, x)) {
                        int maxCrafts = calculateMaxCrafts(shapedRecipe, customIngredients, matrix, y, x);

                        if (maxCrafts > 0) {
                            int score = calculateRecipeScore(customIngredients);
                            potentialPlacements.add(new CraftingPlacement(shapedRecipe, y, x, maxCrafts, score));
                        }
                    }
                }
            }
        }

        Optional<CraftingPlacement> result = potentialPlacements.stream().max(Comparator.comparingInt(CraftingPlacement::getScore));


        return result;
    }

    private boolean matches(ShapedRecipe recipe, Map<Character, ItemStack> customIngredients, ItemStack[] matrix, int gridWidth, int offsetY, int offsetX) {
        String[] shape = recipe.getShape();

        for (int i = 0; i < matrix.length; i++) {
            int gridY = i / gridWidth;
            int gridX = i % gridWidth;
            int shapeY = gridY - offsetY;
            int shapeX = gridX - offsetX;

            char recipeChar = ' ';
            if (shapeY >= 0 && shapeY < shape.length && shapeX >= 0 && shapeX < shape[shapeY].length()) {
                recipeChar = shape[shapeY].charAt(shapeX);
            }

            ItemStack itemInGrid = matrix[i];

            if (recipeChar == ' ') {
                if (itemInGrid != null && !itemInGrid.getType().isAir()) {
                    return false;
                }
            } else {
                if (itemInGrid == null) {
                    return false;
                }

                ItemStack requiredItem = customIngredients.get(recipeChar);
                if (requiredItem == null) {
                    return false;
                }

                boolean isSimilar = itemInGrid.isSimilar(requiredItem);
                boolean hasEnoughAmount = itemInGrid.getAmount() >= requiredItem.getAmount();



                if (!isSimilar || !hasEnoughAmount) {
                    return false;
                }
            }
        }
        return true;
    }

    private int calculateMaxCrafts(ShapedRecipe recipe, Map<Character, ItemStack> customIngredients, ItemStack[] matrix, int offsetY, int offsetX) {
        int maxCrafts = Integer.MAX_VALUE;
        String[] shape = recipe.getShape();
        int gridWidth = (int) Math.sqrt(matrix.length);

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length(); c++) {
                char ingredientChar = shape[r].charAt(c);
                if (ingredientChar == ' ') continue;

                int matrixIndex = (r + offsetY) * gridWidth + (c + offsetX);
                ItemStack itemInGrid = matrix[matrixIndex];
                ItemStack requiredItem = customIngredients.get(ingredientChar);
                if (itemInGrid == null || requiredItem == null) return 0;

                int requiredAmount = requiredItem.getAmount();
                if (requiredAmount <= 0) continue;


                int possibleCrafts = itemInGrid.getAmount() / requiredAmount;
                if (possibleCrafts < maxCrafts) {
                    maxCrafts = possibleCrafts;
                }
            }
        }
        return maxCrafts == Integer.MAX_VALUE ? 0 : maxCrafts;
    }

    private int calculateRecipeScore(Map<Character, ItemStack> customIngredients) {
        int score = 0;
        for (ItemStack stack : customIngredients.values()) {
            score += stack.getAmount();
        }
        score += customIngredients.size();
        return score;
    }

    private static final class CraftingPlacement {
        private final ShapedRecipe recipe;
        private final int offsetY;
        private final int offsetX;
        private final int maxCrafts;
        private final int score;

        public CraftingPlacement(ShapedRecipe recipe, int offsetY, int offsetX, int maxCrafts, int score) {
            this.recipe = recipe;
            this.offsetY = offsetY;
            this.offsetX = offsetX;
            this.maxCrafts = maxCrafts;
            this.score = score;
        }

        public ShapedRecipe getRecipe() {
            return recipe;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getMaxCrafts() {
            return maxCrafts;
        }

        public int getScore() {
            return score;
        }
    }
}