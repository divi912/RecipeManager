package MCplugin2.newplugin;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RecipeManager {

    public final Newplugin plugin;
    public final Map<NamespacedKey, RecipeWrapper> registeredRecipes = new HashMap<>();
    private final Map<NamespacedKey, Map<Character, ItemStack>> customShapedIngredients = new HashMap<>();
    private final File configFile;
    private final FileConfiguration config;

    public enum RecipeType {
        ALL,
        CRAFTING,
        FURNACE,
        BLASTING,
        SMOKING,
        CAMPFIRE,
        SMITHING,
        STONECUTTING
    }

    public RecipeManager(Newplugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "recipes.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadRecipes();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Map<Character, ItemStack> getCustomShapedIngredients(NamespacedKey key) {
        return customShapedIngredients.get(key);
    }

    public Set<NamespacedKey> getCustomShapedRecipeKeys() {
        return Collections.unmodifiableSet(customShapedIngredients.keySet());
    }

    public void loadRecipes() {
        Iterator<Recipe> serverRecipes = Bukkit.recipeIterator();
        while (serverRecipes.hasNext()) {
            Recipe recipe = serverRecipes.next();
            if (recipe.getResult().getType().isAir()) continue;
            NamespacedKey key = getRecipeKey(recipe);
            if (key != null) {
                registeredRecipes.put(key, new RecipeWrapper(recipe, false));
            }
        }
        plugin.getLogger().info("Loaded " + registeredRecipes.size() + " server recipes.");

        loadCustomRecipes();

        ConfigurationSection disabledRecipesSection = config.getConfigurationSection("recipes");
        if (disabledRecipesSection != null) {
            for (String keyString : disabledRecipesSection.getKeys(false)) {
                if (!disabledRecipesSection.getBoolean(keyString + ".enabled", true)) {
                    NamespacedKey key = NamespacedKey.fromString(keyString);
                    if (key != null && registeredRecipes.containsKey(key)) {
                        registeredRecipes.get(key).setEnabled(false);
                        Bukkit.removeRecipe(key);
                    }
                }
            }
        }
    }

    public void loadCustomRecipes() {
        ConfigurationSection customRecipesSection = config.getConfigurationSection("custom-recipes");
        if (customRecipesSection == null) {
            return;
        }

        int loadedCount = 0;
        for (String keyString : customRecipesSection.getKeys(false)) {
            try {
                NamespacedKey key = NamespacedKey.fromString(keyString);
                if (key == null) continue;

                ConfigurationSection recipeSection = customRecipesSection.getConfigurationSection(keyString);
                if (recipeSection == null) continue;

                String type = recipeSection.getString("type", "SHAPED");
                Recipe recipe = null;

                switch (type.toUpperCase()) {
                    case "SHAPED":
                        recipe = loadShapedRecipe(key, recipeSection);
                        break;
                    case "FURNACE":
                        recipe = loadFurnaceRecipe(key, recipeSection);
                        break;
                    case "SMITHING_TRANSFORM":
                        recipe = loadSmithingTransformRecipe(key, recipeSection);
                        break;
                }

                if (recipe != null) {
                    if (Bukkit.getRecipe(key) != null) {
                        Bukkit.removeRecipe(key);
                    }
                    if (Bukkit.addRecipe(recipe)) {
                        registeredRecipes.put(key, new RecipeWrapper(recipe, true));
                        loadedCount++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load custom recipe: " + keyString, e);
            }
        }
        if (loadedCount > 0) {
            plugin.getLogger().info("Loaded " + loadedCount + " custom recipes.");
        }
    }

    private void saveCustomRecipes() {
        config.set("custom-recipes", null);
        ConfigurationSection customRecipesSection = config.createSection("custom-recipes");

        for (RecipeWrapper wrapper : registeredRecipes.values()) {
            if (wrapper.isCustom()) {
                String keyString = wrapper.getKey().toString();
                ConfigurationSection recipeSection = customRecipesSection.createSection(keyString);
                Recipe recipe = wrapper.getRecipe();

                if (recipe instanceof ShapedRecipe shapedRecipe) {
                    saveShapedRecipe(recipeSection, shapedRecipe);
                } else if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                    saveFurnaceRecipe(recipeSection, furnaceRecipe);
                } else if (recipe instanceof SmithingTransformRecipe smithingTransformRecipe) {
                    saveSmithingTransformRecipe(recipeSection, smithingTransformRecipe);
                }
            }
        }
    }

    private void saveRecipeStates() {
        ConfigurationSection recipesSection = config.createSection("recipes");
        for (RecipeWrapper wrapper : registeredRecipes.values()) {
            recipesSection.set(wrapper.getKey().toString() + ".enabled", wrapper.isEnabled());
        }
    }

    public void saveConfig() {
        saveCustomRecipes();
        saveRecipeStates();
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save recipes.yml", e);
        }
    }

    private void updatePlayersWithRecipe(NamespacedKey key) {
        Bukkit.getOnlinePlayers().forEach(player -> player.discoverRecipe(key));
    }

    private void updatePlayersWithoutRecipe(NamespacedKey key) {
        Bukkit.getOnlinePlayers().forEach(player -> player.undiscoverRecipe(key));
    }

    public boolean addShapedRecipe(ShapedRecipe recipe, Map<Character, ItemStack> ingredients) {
        if (Bukkit.addRecipe(recipe)) {
            NamespacedKey key = recipe.getKey();
            RecipeWrapper wrapper = new RecipeWrapper(recipe, true);
            registeredRecipes.put(key, wrapper);
            customShapedIngredients.put(key, ingredients);
            saveConfig();
            updatePlayersWithRecipe(key);
            return true;
        }
        return false;
    }

    public boolean addRecipe(Recipe recipe) {
        if (recipe instanceof Keyed keyedRecipe) {
            if (Bukkit.addRecipe(recipe)) {
                RecipeWrapper wrapper = new RecipeWrapper(recipe, true);
                registeredRecipes.put(keyedRecipe.getKey(), wrapper);
                saveConfig();
                updatePlayersWithRecipe(keyedRecipe.getKey());
                return true;
            }
        }
        return false;
    }

    public boolean deleteRecipe(RecipeWrapper wrapper) {
        if (!wrapper.isCustom()) {
            return false;
        }
        NamespacedKey key = wrapper.getKey();
        if (Bukkit.removeRecipe(key)) {
            registeredRecipes.remove(key);
            if (wrapper.getRecipe() instanceof ShapedRecipe) {
                customShapedIngredients.remove(key);
            }
            saveConfig();
            updatePlayersWithoutRecipe(key);
            return true;
        }
        return false;
    }

    public boolean disableRecipe(RecipeWrapper wrapper) {
        if (wrapper.isEnabled()) {
            NamespacedKey key = wrapper.getKey();
            wrapper.setEnabled(false);
            Bukkit.removeRecipe(key);
            saveConfig();
            updatePlayersWithoutRecipe(key);
            return true;
        }
        return false;
    }

    public boolean enableRecipe(RecipeWrapper wrapper) {
        if (!wrapper.isEnabled()) {
            wrapper.setEnabled(true);
            if (Bukkit.addRecipe(wrapper.getRecipe())) {
                saveConfig();
                updatePlayersWithRecipe(wrapper.getKey());
                return true;
            } else {
                plugin.getLogger().warning("Failed to re-add recipe: " + wrapper.getKey());
                wrapper.setEnabled(false);
                return false;
            }
        }
        return false;
    }

    public RecipeType getRecipeType(Recipe recipe) {
        if (recipe instanceof CraftingRecipe) {
            return RecipeType.CRAFTING;
        } else if (recipe instanceof FurnaceRecipe) {
            return RecipeType.FURNACE;
        } else if (recipe instanceof BlastingRecipe) {
            return RecipeType.BLASTING;
        } else if (recipe instanceof SmokingRecipe) {
            return RecipeType.SMOKING;
        } else if (recipe instanceof CampfireRecipe) {
            return RecipeType.CAMPFIRE;
        } else if (recipe instanceof SmithingRecipe) {
            return RecipeType.SMITHING;
        } else if (recipe instanceof StonecuttingRecipe) {
            return RecipeType.STONECUTTING;
        }
        return null;
    }

    public List<RecipeWrapper> getEnabledVanillaRecipes(RecipeType type) {
        return registeredRecipes.values().stream()
                .filter(r -> !r.isCustom() && r.isEnabled() && (type == RecipeType.ALL || getRecipeType(r.getRecipe()) == type))
                .collect(Collectors.toList());
    }

    public List<RecipeWrapper> getDisabledVanillaRecipes(RecipeType type) {
        return registeredRecipes.values().stream()
                .filter(r -> !r.isCustom() && !r.isEnabled() && (type == RecipeType.ALL || getRecipeType(r.getRecipe()) == type))
                .collect(Collectors.toList());
    }

    public List<RecipeWrapper> getCustomRecipes() {
        return registeredRecipes.values().stream()
                .filter(RecipeWrapper::isCustom)
                .collect(Collectors.toList());
    }

    private NamespacedKey getRecipeKey(Recipe recipe) {
        if (recipe instanceof Keyed) {
            return ((Keyed) recipe).getKey();
        }
        return null;
    }

    private void saveShapedRecipe(ConfigurationSection section, ShapedRecipe recipe) {
        section.set("type", "SHAPED");
        section.set("result", recipe.getResult());
        section.set("shape", Arrays.asList(recipe.getShape()));

        ConfigurationSection ingredientsSection = section.createSection("ingredients");
        Map<Character, ItemStack> ingredients = customShapedIngredients.get(recipe.getKey());
        if (ingredients != null) {
            for (Map.Entry<Character, ItemStack> entry : ingredients.entrySet()) {
                ingredientsSection.set(entry.getKey().toString(), entry.getValue());
            }
        } else {
            plugin.getLogger().warning("Could not find custom ingredients for " + recipe.getKey() + " during save. Amounts may be incorrect.");
        }
    }

    private void saveFurnaceRecipe(ConfigurationSection section, FurnaceRecipe recipe) {
        section.set("type", "FURNACE");
        section.set("result", recipe.getResult());

        if (recipe.getInputChoice() instanceof RecipeChoice.ExactChoice choice) {
            if (!choice.getChoices().isEmpty()) {
                section.set("input", choice.getChoices().getFirst());
            }
        }
        section.set("experience", recipe.getExperience());
        section.set("cookingTime", recipe.getCookingTime());
    }

    private void saveSmithingTransformRecipe(ConfigurationSection section, SmithingTransformRecipe recipe) {
        section.set("type", "SMITHING_TRANSFORM");
        section.set("result", recipe.getResult());
        if (recipe.getTemplate() instanceof RecipeChoice.ExactChoice choice) {
            if (!choice.getChoices().isEmpty()) {
                section.set("template", choice.getChoices().getFirst());
            }
        }
        if (recipe.getBase() instanceof RecipeChoice.ExactChoice choice) {
            if (!choice.getChoices().isEmpty()) {
                section.set("base", choice.getChoices().getFirst());
            }
        }
        if (recipe.getAddition() instanceof RecipeChoice.ExactChoice choice) {
            if (!choice.getChoices().isEmpty()) {
                section.set("addition", choice.getChoices().getFirst());
            }
        }
    }

    private ShapedRecipe loadShapedRecipe(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = section.getItemStack("result");
        if (result == null) {
            plugin.getLogger().warning("Shaped recipe " + key + " has no result defined in recipes.yml.");
            return null;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, result);

        List<String> shapeList = section.getStringList("shape");
        if (shapeList.isEmpty()) {
            plugin.getLogger().warning("Shaped recipe " + key + " has no shape defined in recipes.yml.");
            return null;
        }
        String[] shape = shapeList.toArray(new String[0]);
        recipe.shape(shape);

        plugin.getLogger().info("DEBUG: Recipe shape after setting: " + Arrays.toString(recipe.getShape()));

        ConfigurationSection ingredientsSection = section.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            plugin.getLogger().warning("Shaped recipe " + key + " has a shape but no 'ingredients' section in recipes.yml.");
            return null;
        }

        Map<Character, ItemStack> ingredientsMap = new HashMap<>();

        Set<Character> uniqueChars = new HashSet<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ') uniqueChars.add(c);
            }
        }

        for (Character ingredientChar : uniqueChars) {
            ItemStack ingredientItem = ingredientsSection.getItemStack(ingredientChar.toString());

            if (ingredientItem == null || ingredientItem.getType().isAir()) {
                plugin.getLogger().warning("Shaped recipe " + key + " is missing or has an invalid ingredient definition for character '" + ingredientChar + "' in recipes.yml.");
                return null;
            }


            recipe.setIngredient(ingredientChar, new RecipeChoice.ExactChoice(ingredientItem.clone()));


            ingredientsMap.put(ingredientChar, ingredientItem.clone());
        }

        customShapedIngredients.put(key, ingredientsMap);
        return recipe;
    }

    private FurnaceRecipe loadFurnaceRecipe(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = section.getItemStack("result");
        if (result == null) {
            plugin.getLogger().warning("Furnace recipe " + key + " has no result.");
            return null;
        }

        ItemStack inputStack = section.getItemStack("input");
        if (inputStack == null) {
            plugin.getLogger().warning("Furnace recipe " + key + " has no input.");
            return null;
        }
        RecipeChoice input = new RecipeChoice.ExactChoice(inputStack);

        float experience = (float) section.getDouble("experience", 0f);
        int cookingTime = section.getInt("cookingTime", 200);

        return new FurnaceRecipe(key, result, input, experience, cookingTime);
    }

    private SmithingTransformRecipe loadSmithingTransformRecipe(NamespacedKey key, ConfigurationSection section) {
        ItemStack result = section.getItemStack("result");
        if (result == null) return null;

        ItemStack templateStack = section.getItemStack("template");
        if (templateStack == null) return null;
        RecipeChoice template = new RecipeChoice.ExactChoice(templateStack);

        ItemStack baseStack = section.getItemStack("base");
        if (baseStack == null) return null;
        RecipeChoice base = new RecipeChoice.ExactChoice(baseStack);

        ItemStack additionStack = section.getItemStack("addition");
        if (additionStack == null) return null;
        RecipeChoice addition = new RecipeChoice.ExactChoice(additionStack);

        return new SmithingTransformRecipe(key, result, template, base, addition);
    }
}
