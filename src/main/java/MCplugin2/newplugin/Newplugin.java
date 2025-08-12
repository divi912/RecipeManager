package MCplugin2.newplugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class Newplugin extends JavaPlugin {

    private static Newplugin instance;
    private RecipeManager recipeManager;
    private AnvilRecipeManager anvilRecipeManager;
    private RecipeMenu recipeMenu;

    @Override
    public void onEnable() {
        instance = this;
        recipeManager = new RecipeManager(this);
        anvilRecipeManager = new AnvilRecipeManager(this);
        recipeManager.loadCustomRecipes(); // Load custom recipes on startup
        recipeMenu = new RecipeMenu(this, recipeManager, anvilRecipeManager);

            getServer().getPluginManager().registerEvents(new CustomCraftingListener(recipeManager), this);


        // The MenuCommand class is not a listener, so it doesn't need to be registered
        // menuCommand = new MenuCommand(this);
        getServer().getPluginManager().registerEvents(recipeMenu, this);
        getServer().getPluginManager().registerEvents(new AnvilListener(anvilRecipeManager), this);
        getServer().getPluginManager().registerEvents(new CustomCraftingListener(recipeManager), this);
        this.getCommand("recipe").setExecutor(new RecipeCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Newplugin getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public AnvilRecipeManager getAnvilRecipeManager() {
        return anvilRecipeManager;
    }

    public RecipeMenu getRecipeMenu() {
        return recipeMenu;
    }


}
