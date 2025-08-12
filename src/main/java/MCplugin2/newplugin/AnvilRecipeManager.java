package MCplugin2.newplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AnvilRecipeManager {

    private final Newplugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private final List<AnvilRecipe> anvilRecipes = new ArrayList<>();

    public AnvilRecipeManager(Newplugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "anvil-recipes.yml");
        if (!configFile.exists()) {
            plugin.saveResource("anvil-recipes.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadAnvilRecipes();
    }

    public void loadAnvilRecipes() {
        anvilRecipes.clear();
        ConfigurationSection recipesSection = config.getConfigurationSection("anvil-recipes");
        if (recipesSection != null) {
            for (String key : recipesSection.getKeys(false)) {
                ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
                if (recipeSection != null) {
                    ItemStack input1 = recipeSection.getItemStack("input1");
                    ItemStack input2 = recipeSection.getItemStack("input2");
                    ItemStack result = recipeSection.getItemStack("result");
                    int levelCost = recipeSection.getInt("levelCost", 1);
                    if (input1 != null && input2 != null && result != null) {
                        anvilRecipes.add(new AnvilRecipe(key, input1, input2, result, levelCost));
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + anvilRecipes.size() + " anvil recipes.");
    }

    public void saveAnvilRecipes() {
        config.set("anvil-recipes", null);
        ConfigurationSection recipesSection = config.createSection("anvil-recipes");
        for (AnvilRecipe recipe : anvilRecipes) {
            ConfigurationSection recipeSection = recipesSection.createSection(recipe.getName());
            recipeSection.set("input1", recipe.getInput1());
            recipeSection.set("input2", recipe.getInput2());
            recipeSection.set("result", recipe.getResult());
            recipeSection.set("levelCost", recipe.getLevelCost());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save anvil-recipes.yml", e);
        }
    }

    public void addAnvilRecipe(AnvilRecipe recipe) {
        anvilRecipes.add(recipe);
        saveAnvilRecipes();
    }

    public void deleteAnvilRecipe(String recipeName) {
        anvilRecipes.removeIf(recipe -> recipe.getName().equals(recipeName));
        saveAnvilRecipes();
    }

    public List<AnvilRecipe> getAnvilRecipes() {
        return new ArrayList<>(anvilRecipes);
    }

    public static class AnvilRecipe {
        private final String name;
        private final ItemStack input1;
        private final ItemStack input2;
        private final ItemStack result;
        private final int levelCost;

        public AnvilRecipe(String name, ItemStack input1, ItemStack input2, ItemStack result, int levelCost) {
            this.name = name;
            this.input1 = input1;
            this.input2 = input2;
            this.result = result;
            this.levelCost = levelCost;
        }

        public String getName() {
            return name;
        }

        public ItemStack getInput1() {
            return input1;
        }

        public ItemStack getInput2() {
            return input2;
        }

        public ItemStack getResult() {
            return result;
        }

        public int getLevelCost() {
            return levelCost;
        }
    }
}
