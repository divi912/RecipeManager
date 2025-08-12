package MCplugin2.newplugin;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

public class RecipeWrapper {

    private final Recipe recipe;
    private final boolean isCustom;
    private boolean isEnabled;

    public RecipeWrapper(Recipe recipe, boolean isCustom) {
        this.recipe = recipe;
        this.isCustom = isCustom;
        this.isEnabled = true;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public NamespacedKey getKey() {
        if (recipe instanceof Keyed) {
            return ((Keyed) recipe).getKey();
        }
        return null;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
