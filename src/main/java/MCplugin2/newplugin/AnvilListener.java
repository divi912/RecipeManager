package MCplugin2.newplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public class AnvilListener implements Listener {

    private final AnvilRecipeManager anvilRecipeManager;

    public AnvilListener(AnvilRecipeManager anvilRecipeManager) {
        this.anvilRecipeManager = anvilRecipeManager;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack item1 = inventory.getItem(0);
        ItemStack item2 = inventory.getItem(1);

        if (item1 != null && item2 != null) {
            for (AnvilRecipeManager.AnvilRecipe recipe : anvilRecipeManager.getAnvilRecipes()) {
                if (recipe.getInput1().isSimilar(item1) && recipe.getInput2().isSimilar(item2)) {
                    inventory.setRepairCost(recipe.getLevelCost());
                    event.setResult(recipe.getResult());
                    // We need to refresh the player's inventory to show the result
                    if (event.getView().getPlayer() instanceof Player) {
                        ((Player) event.getView().getPlayer()).updateInventory();
                    }
                    return;
                }
            }
        }
    }
}
