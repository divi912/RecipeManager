package MCplugin2.newplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RecipeMenu implements Listener, InventoryHolder {

    private final Newplugin plugin;
    private final RecipeManager recipeManager;
    private final AnvilRecipeManager anvilRecipeManager;
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    private static final String MAIN_MENU_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "✨ Recipe Management ✨";
    private static final String CREATE_TYPE_SELECTION_TITLE = ChatColor.DARK_AQUA + "⭐ Select Recipe Type ⭐";
    private static final String TABLE_SELECTION_TITLE = ChatColor.DARK_AQUA + "⛏ Select Table Type ⛏";
    private static final String CREATE_SHAPED_MENU_TITLE = ChatColor.DARK_AQUA + "✏ Create a Shaped Recipe";
    private static final String CREATE_FURNACE_MENU_TITLE = ChatColor.DARK_AQUA + "🔥 Create a Furnace Recipe";
    private static final String CREATE_SMITHING_MENU_TITLE = ChatColor.DARK_AQUA + "⚔ Create a Smithing Recipe";
    private static final String CREATE_ANVIL_MENU_TITLE = ChatColor.DARK_AQUA + "⚖ Create an Anvil Recipe";
    private static final String DELETE_MENU_TITLE = ChatColor.DARK_RED + "❌ Delete a Recipe ❌";
    private static final String ENABLE_MENU_TITLE = ChatColor.DARK_GREEN + "✅ Enable a Recipe ✅";
    private static final String DISABLE_MENU_TITLE = ChatColor.GOLD + "⛔ Disable a Recipe ⛔";
    private static final String CONFIRM_DELETE_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "❗ Confirm Deletion ❗";


    public RecipeMenu(Newplugin plugin, RecipeManager recipeManager, AnvilRecipeManager anvilRecipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.anvilRecipeManager = anvilRecipeManager;
    }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 9, "Recipe Menu");
    }

    private PlayerState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
    }

    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(this, 54, MAIN_MENU_TITLE);
        PlayerState state = getPlayerState(player);
        state.clear();
        state.setCurrentMenu(MenuType.MAIN);

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 10 || i > 43 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, border);
            }
        }

        menu.setItem(20, createGuiItem(Material.WRITABLE_BOOK, ChatColor.GREEN + "✍ Create Recipe", ChatColor.GRAY + "Create a new custom recipe."));
        menu.setItem(22, createGuiItem(Material.LAVA_BUCKET, ChatColor.RED + "❌ Delete Recipe", ChatColor.GRAY + "Permanently delete a custom recipe."));
        menu.setItem(24, createGuiItem(Material.EMERALD, ChatColor.AQUA + "✅ Enable Recipe", ChatColor.GRAY + "Enable a disabled vanilla recipe."));
        menu.setItem(31, createGuiItem(Material.BARRIER, ChatColor.YELLOW + "⛔ Disable Recipe", ChatColor.GRAY + "Disable an enabled vanilla recipe."));

        player.openInventory(menu);
    }

    private void openCreateMenuTypeSelection(Player player) {
        Inventory menu = Bukkit.createInventory(this, 27, CREATE_TYPE_SELECTION_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CREATE_TYPE_SELECTION);

        menu.setItem(10, createGuiItem(Material.CRAFTING_TABLE, ChatColor.GREEN + "Shaped Recipe", ChatColor.DARK_AQUA + "For crafting tables."));
        menu.setItem(12, createGuiItem(Material.FURNACE, ChatColor.YELLOW + "Furnace Recipe", ChatColor.DARK_AQUA + "For furnaces, smokers, and blast furnaces."));
        menu.setItem(14, createGuiItem(Material.SMITHING_TABLE, ChatColor.AQUA + "Smithing Recipe", ChatColor.DARK_AQUA + "For smithing tables."));
        menu.setItem(16, createGuiItem(Material.ANVIL, ChatColor.GRAY + "Anvil Recipe", ChatColor.DARK_AQUA + "For anvils."));
        menu.setItem(26, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Main Menu"));

        player.openInventory(menu);
    }

    private void openTableSelectionMenu(Player player, MenuType menuType) {
        Inventory menu = Bukkit.createInventory(this, 27, TABLE_SELECTION_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(menuType);

        menu.setItem(10, createGuiItem(Material.CRAFTING_TABLE, ChatColor.GREEN + "Crafting Recipes"));
        menu.setItem(11, createGuiItem(Material.FURNACE, ChatColor.YELLOW + "Furnace Recipes"));
        menu.setItem(12, createGuiItem(Material.BLAST_FURNACE, ChatColor.GRAY + "Blasting Recipes"));
        menu.setItem(13, createGuiItem(Material.SMOKER, ChatColor.DARK_GRAY + "Smoking Recipes"));
        menu.setItem(14, createGuiItem(Material.CAMPFIRE, ChatColor.RED + "Campfire Recipes"));
        menu.setItem(15, createGuiItem(Material.SMITHING_TABLE, ChatColor.AQUA + "Smithing Recipes"));
        menu.setItem(16, createGuiItem(Material.STONECUTTER, ChatColor.WHITE + "Stonecutting Recipes"));
        menu.setItem(26, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Main Menu"));

        player.openInventory(menu);
    }

    private void openCreateShapedMenu(Player player) {
        Inventory menu = Bukkit.createInventory(this, 54, CREATE_SHAPED_MENU_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CREATE_SHAPED);

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, border);
        }

        for (int i = 12; i <= 14; i++) {
            for (int j = 0; j < 3; j++) {
                menu.setItem(i + (j * 9), null);
            }
        }
        menu.setItem(24, null); // Result slot

        menu.setItem(40, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "✔ SAVE RECIPE", ChatColor.DARK_AQUA + "Save the current recipe."));
        menu.setItem(49, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Type Selection"));
        player.openInventory(menu);
    }

    private void openCreateFurnaceMenu(Player player) {
        Inventory menu = Bukkit.createInventory(this, 54, CREATE_FURNACE_MENU_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CREATE_FURNACE);

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, border);
        }

        menu.setItem(13, null); // Input slot
        menu.setItem(25, null); // Result slot

        menu.setItem(40, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "✔ SAVE RECIPE", ChatColor.DARK_AQUA + "Save the current recipe."));
        menu.setItem(49, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Type Selection"));
        player.openInventory(menu);
    }

    private void openCreateSmithingMenu(Player player) {
        Inventory menu = Bukkit.createInventory(this, 54, CREATE_SMITHING_MENU_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CREATE_SMITHING_TRANSFORM);

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, border);
        }

        menu.setItem(11, null); // Template slot
        menu.setItem(13, null); // Base slot
        menu.setItem(15, null); // Addition slot
        menu.setItem(24, null); // Result slot

        menu.setItem(40, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "✔ SAVE RECIPE", ChatColor.DARK_AQUA + "Save the current recipe."));
        menu.setItem(49, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Type Selection"));
        player.openInventory(menu);
    }

    private void openCreateAnvilMenu(Player player) {
        Inventory menu = Bukkit.createInventory(this, 54, CREATE_ANVIL_MENU_TITLE);
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CREATE_ANVIL);

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            menu.setItem(i, border);
        }

        menu.setItem(13, null); // Input1
        menu.setItem(22, null); // Input2
        menu.setItem(25, null); // Result

        menu.setItem(40, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "✔ SAVE RECIPE", ChatColor.DARK_AQUA + "Save the current recipe."));
        menu.setItem(49, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Type Selection"));
        player.openInventory(menu);
    }

    private void openRecipeListMenu(Player player, MenuType menuType, RecipeManager.RecipeType recipeType) {
        List<RecipeWrapper> recipes;
        PlayerState state = getPlayerState(player);

        switch (menuType) {
            case DELETE:
                recipes = recipeManager.getCustomRecipes();
                break;
            case ENABLE:
                recipes = recipeManager.getDisabledVanillaRecipes(recipeType);
                break;
            case DISABLE:
                recipes = recipeManager.getEnabledVanillaRecipes(recipeType);
                break;
            default:
                return;
        }

        state.setCurrentMenu(menuType);
        state.setSelectedRecipeType(recipeType);
        state.setCurrentPage(0);
        state.setOriginalRecipeList(recipes);
        state.setDisplayedRecipeList(new ArrayList<>(recipes));
        state.setSearchQuery(null);

        openPaginatedMenu(player);
    }

    private void openPaginatedMenu(Player player) {
        PlayerState state = getPlayerState(player);
        List<RecipeWrapper> recipes = state.getDisplayedRecipeList();
        int page = state.getCurrentPage();
        MenuType menuType = state.getCurrentMenu();
        String title;

        switch (menuType) {
            case DELETE: title = DELETE_MENU_TITLE; break;
            case ENABLE: title = ENABLE_MENU_TITLE; break;
            case DISABLE: title = DISABLE_MENU_TITLE; break;
            default: return;
        }

        Inventory menu = Bukkit.createInventory(this, 54, title + " (Page " + (page + 1) + ")");
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 10 || i > 43 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, border);
            }
        }

        int startIndex = page * 28;
        for (int i = 0; i < 28; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex < recipes.size()) {
                RecipeWrapper wrapper = recipes.get(recipeIndex);
                ItemStack item = new ItemStack(wrapper.getRecipe().getResult());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.RESET + "" + ChatColor.AQUA + "» " + wrapper.getKey().getKey() + " «");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Click to " + menuType.name().toLowerCase());
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                menu.setItem(10 + i + (i / 7 * 2), item);
            }
        }

        if (page > 0) {
            menu.setItem(45, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Previous Page"));
        }
        if ((page + 1) * 28 < recipes.size()) {
            menu.setItem(53, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Next Page ➡"));
        }

        if (state.getSearchQuery() != null && !state.getSearchQuery().isEmpty()) {
            menu.setItem(48, createGuiItem(Material.BARRIER, ChatColor.RED + "❌ Clear Search", ChatColor.GRAY + "Query: " + state.getSearchQuery()));
        } else {
            menu.setItem(48, createGuiItem(Material.OAK_SIGN, ChatColor.AQUA + "🔍 Search Recipes", ChatColor.DARK_AQUA + "Find a recipe by name."));
        }

        menu.setItem(49, createGuiItem(Material.ARROW, ChatColor.YELLOW + "⬅ Back to Main Menu"));

        player.openInventory(menu);
    }

    private void openConfirmDeleteMenu(Player player, RecipeWrapper recipeWrapper) {
        PlayerState state = getPlayerState(player);
        state.setCurrentMenu(MenuType.CONFIRM_DELETE);
        state.setRecipeToConfirm(recipeWrapper);

        Inventory menu = Bukkit.createInventory(this, 27, CONFIRM_DELETE_TITLE);

        menu.setItem(11, createGuiItem(Material.GREEN_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "✔ CONFIRM", ChatColor.DARK_AQUA + "Delete this recipe forever."));
        menu.setItem(13, recipeWrapper.getRecipe().getResult());
        menu.setItem(15, createGuiItem(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "❌ CANCEL", ChatColor.DARK_AQUA + "Go back to the delete list."));

        player.openInventory(menu);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RecipeMenu)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PlayerState state = getPlayerState(player);
        MenuType currentMenu = state.getCurrentMenu();

        if (currentMenu == MenuType.CREATE_SHAPED || currentMenu == MenuType.CREATE_FURNACE || currentMenu == MenuType.CREATE_SMITHING_TRANSFORM || currentMenu == MenuType.CREATE_ANVIL) {
            if (event.getClickedInventory() != event.getInventory()) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && (clickedItem.getType() == Material.GREEN_WOOL || clickedItem.getType() == Material.ARROW)) {
                event.setCancelled(true);
                if (clickedItem.getType() == Material.GREEN_WOOL) {
                    switch (currentMenu) {
                        case CREATE_SHAPED: saveShapedRecipe(player, event.getInventory()); break;
                        case CREATE_FURNACE: saveFurnaceRecipe(player, event.getInventory()); break;
                        case CREATE_SMITHING_TRANSFORM: saveSmithingTransformRecipe(player, event.getInventory()); break;
                        case CREATE_ANVIL: saveAnvilRecipe(player, event.getInventory()); break;
                    }
                } else if (clickedItem.getType() == Material.ARROW) {
                    openCreateMenuTypeSelection(player);
                }
            }
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        switch (currentMenu) {
            case MAIN:
                handleMainMenuClick(player, clickedItem.getType());
                break;
            case CREATE_TYPE_SELECTION:
                handleCreateTypeSelectionClick(player, clickedItem.getType());
                break;
            case ENABLE_TYPE_SELECTION:
            case DISABLE_TYPE_SELECTION:
                handleTableSelectionMenuClick(player, clickedItem.getType());
                break;
            case DELETE:
            case ENABLE:
            case DISABLE:
                handleRecipeListClick(player, event.getSlot(), clickedItem);
                break;
            case CONFIRM_DELETE:
                handleConfirmDeleteClick(player, clickedItem.getType());
                break;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerState state = playerStates.get(player.getUniqueId());

        if (state != null) {
            if (state.isSearching()) {
                event.setCancelled(true);
                String query = event.getMessage();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    state.setSearching(false);
                    if (query.equalsIgnoreCase("cancel")) {
                        player.sendMessage(ChatColor.YELLOW + "Search cancelled.");
                        openPaginatedMenu(player);
                        return;
                    }

                    player.sendMessage(ChatColor.GREEN + "Searching for: " + query);
                    state.setSearchQuery(query);
                    applyFilter(player);
                    openPaginatedMenu(player);
                });
            } else if (state.isEnteringLevelCost()) {
                event.setCancelled(true);
                String message = event.getMessage();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    state.setEnteringLevelCost(false);
                    if (message.equalsIgnoreCase("cancel")) {
                        player.sendMessage(ChatColor.YELLOW + "Anvil recipe creation cancelled.");
                        openCreateAnvilMenu(player);
                        return;
                    }

                    try {
                        int levelCost = Integer.parseInt(message);
                        if (levelCost <= 0) {
                            player.sendMessage(ChatColor.RED + "Level cost must be a positive number.");
                            openCreateAnvilMenu(player);
                            return;
                        }

                        ItemStack[] items = state.getTempAnvilRecipeItems();
                        String recipeName = "custom_anvil_" + items[2].getType().name().toLowerCase() + "_" + System.currentTimeMillis();

                        AnvilRecipeManager.AnvilRecipe recipe = new AnvilRecipeManager.AnvilRecipe(recipeName, items[0], items[1], items[2], levelCost);
                        anvilRecipeManager.addAnvilRecipe(recipe);

                        player.sendMessage(ChatColor.GREEN + "Anvil recipe saved successfully!");
                        openMainMenu(player);

                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number for level cost. Please try again.");
                        openCreateAnvilMenu(player);
                    }
                });
            }
        }
    }

    private void applyFilter(Player player) {
        PlayerState state = getPlayerState(player);
        String query = state.getSearchQuery();
        if (query == null || query.isEmpty()) {
            state.setDisplayedRecipeList(new ArrayList<>(state.getOriginalRecipeList()));
        } else {
            List<RecipeWrapper> filteredList = new ArrayList<>();
            for (RecipeWrapper wrapper : state.getOriginalRecipeList()) {
                if (wrapper.getKey().getKey().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(wrapper);
                }
            }
            state.setDisplayedRecipeList(filteredList);
        }
        state.setCurrentPage(0);
    }

    private void handleMainMenuClick(Player player, Material material) {
        switch (material) {
            case WRITABLE_BOOK:
                openCreateMenuTypeSelection(player);
                break;
            case LAVA_BUCKET:
                openRecipeListMenu(player, MenuType.DELETE, RecipeManager.RecipeType.ALL);
                break;
            case EMERALD:
                openTableSelectionMenu(player, MenuType.ENABLE_TYPE_SELECTION);
                break;
            case BARRIER:
                openTableSelectionMenu(player, MenuType.DISABLE_TYPE_SELECTION);
                break;
        }
    }

    private void handleCreateTypeSelectionClick(Player player, Material material) {
        switch (material) {
            case CRAFTING_TABLE:
                openCreateShapedMenu(player);
                break;
            case FURNACE:
                openCreateFurnaceMenu(player);
                break;
            case SMITHING_TABLE:
                openCreateSmithingMenu(player);
                break;
            case ANVIL:
                openCreateAnvilMenu(player);
                break;
            case ARROW:
                openMainMenu(player);
                break;
        }
    }

    private void handleTableSelectionMenuClick(Player player, Material material) {
        PlayerState state = getPlayerState(player);
        MenuType currentMenu = state.getCurrentMenu();
        MenuType targetMenuType = (currentMenu == MenuType.ENABLE_TYPE_SELECTION) ? MenuType.ENABLE : MenuType.DISABLE;

        RecipeManager.RecipeType recipeType = null;
        switch (material) {
            case CRAFTING_TABLE: recipeType = RecipeManager.RecipeType.CRAFTING; break;
            case FURNACE: recipeType = RecipeManager.RecipeType.FURNACE; break;
            case BLAST_FURNACE: recipeType = RecipeManager.RecipeType.BLASTING; break;
            case SMOKER: recipeType = RecipeManager.RecipeType.SMOKING; break;
            case CAMPFIRE: recipeType = RecipeManager.RecipeType.CAMPFIRE; break;
            case SMITHING_TABLE: recipeType = RecipeManager.RecipeType.SMITHING; break;
            case STONECUTTER: recipeType = RecipeManager.RecipeType.STONECUTTING; break;
            case ARROW: openMainMenu(player); return;
        }

        if (recipeType != null) {
            openRecipeListMenu(player, targetMenuType, recipeType);
        }
    }

    private void handleRecipeListClick(Player player, int slot, ItemStack clickedItem) {
        PlayerState state = getPlayerState(player);
        if (clickedItem.getType() == Material.ARROW) {
            if (slot == 45) {
                state.setCurrentPage(state.getCurrentPage() - 1);
                openPaginatedMenu(player);
            } else if (slot == 53) {
                state.setCurrentPage(state.getCurrentPage() + 1);
                openPaginatedMenu(player);
            } else if (slot == 49) {
                openMainMenu(player);
            }
            return;
        }

        if (slot == 48) {
            if (state.getSearchQuery() != null && !state.getSearchQuery().isEmpty()) {
                state.setSearchQuery(null);
                state.setDisplayedRecipeList(new ArrayList<>(state.getOriginalRecipeList()));
                state.setCurrentPage(0);
                openPaginatedMenu(player);
            } else {
                state.setSearching(true);
                player.closeInventory();
                player.sendMessage(ChatColor.GOLD + "Enter search query in chat. Type 'cancel' to exit search.");
            }
            return;
        }

        int page = state.getCurrentPage();
        int indexInPage = (slot - 10) - ((slot - 10) / 9 * 2);
        int recipeIndex = (page * 28) + indexInPage;

        if (recipeIndex >= 0 && recipeIndex < state.getDisplayedRecipeList().size()) {
            RecipeWrapper wrapper = state.getDisplayedRecipeList().get(recipeIndex);
            switch (state.getCurrentMenu()) {
                case DELETE:
                    openConfirmDeleteMenu(player, wrapper);
                    break;
                case ENABLE:
                    if (recipeManager.enableRecipe(wrapper)) {
                        player.sendMessage(ChatColor.GREEN + "Recipe enabled!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to enable recipe.");
                    }
                    openRecipeListMenu(player, MenuType.ENABLE, state.getSelectedRecipeType());
                    break;
                case DISABLE:
                    if (recipeManager.disableRecipe(wrapper)) {
                        player.sendMessage(ChatColor.GREEN + "Recipe disabled!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to disable recipe.");
                    }
                    openRecipeListMenu(player, MenuType.DISABLE, state.getSelectedRecipeType());
                    break;
            }
        }
    }

    private void handleConfirmDeleteClick(Player player, Material material) {
        PlayerState state = getPlayerState(player);
        if (material == Material.GREEN_WOOL) {
            RecipeWrapper toDelete = state.getRecipeToConfirm();
            if (toDelete != null && recipeManager.deleteRecipe(toDelete)) {
                player.sendMessage(ChatColor.GREEN + "Recipe " + toDelete.getKey().getKey() + " has been deleted.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to delete recipe.");
            }
            openRecipeListMenu(player, MenuType.DELETE, RecipeManager.RecipeType.ALL);
        } else if (material == Material.RED_WOOL) {
            openRecipeListMenu(player, MenuType.DELETE, RecipeManager.RecipeType.ALL);
        }
    }


    private void saveShapedRecipe(Player player, Inventory inventory) {
        ItemStack result = inventory.getItem(24);
        if (result == null || result.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must place an item in the result slot.");
            return;
        }

        ItemStack[] matrix = new ItemStack[9];
        boolean hasIngredients = false;
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int slot = 12 + col + (row * 9);
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                matrix[i] = item;
                hasIngredients = true;
            }
        }

        if (!hasIngredients) {
            player.sendMessage(ChatColor.RED + "You must place at least one ingredient in the crafting grid.");
            return;
        }

        TrimmedRecipe trimmedRecipe = trimRecipeShape(matrix);

        if (trimmedRecipe.shape().length == 0) {
            player.sendMessage(ChatColor.RED + "Invalid recipe shape.");
            return;
        }

        String recipeName = "custom_" + result.getType().name().toLowerCase() + "_" + System.currentTimeMillis();
        NamespacedKey key = new NamespacedKey(recipeManager.getPlugin(), recipeName);

        // First, construct the ShapedRecipe object
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(trimmedRecipe.shape());

        Map<Character, ItemStack> ingredientMap = trimmedRecipe.ingredientMap();
        for (Map.Entry<Character, ItemStack> entry : ingredientMap.entrySet()) {
            // The recipe itself needs choices with amount 1, but we'll pass the full map to the manager
            ItemStack choiceItem = entry.getValue().clone();
            choiceItem.setAmount(1);
            recipe.setIngredient(entry.getKey(), new RecipeChoice.ExactChoice(choiceItem));
        }

        // Now, call addShapedRecipe with the correct arguments
        if (recipeManager.addShapedRecipe(recipe, ingredientMap)) {
            player.sendMessage(ChatColor.GREEN + "Recipe saved successfully!");
            Bukkit.getScheduler().runTaskLater(recipeManager.getPlugin(), () -> openMainMenu(player), 1L);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to save recipe. Does a similar one already exist?");
        }
    }

    private void saveFurnaceRecipe(Player player, Inventory inventory) {
        ItemStack result = inventory.getItem(25);
        ItemStack input = inventory.getItem(13);

        if (result == null || result.getType().isAir() || input == null || input.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Input and result slots cannot be empty.");
            return;
        }

        String recipeName = "custom_furnace_" + result.getType().name().toLowerCase() + "_" + System.currentTimeMillis();
        NamespacedKey key = new NamespacedKey(recipeManager.getPlugin(), recipeName);

        FurnaceRecipe recipe = new FurnaceRecipe(key, result, new RecipeChoice.ExactChoice(input), 0, 200);

        if (recipeManager.addRecipe(recipe)) {
            player.sendMessage(ChatColor.GREEN + "Furnace recipe saved successfully!");
            Bukkit.getScheduler().runTaskLater(recipeManager.getPlugin(), () -> openMainMenu(player), 1L);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to save furnace recipe.");
        }
    }

    private void saveSmithingTransformRecipe(Player player, Inventory inventory) {
        ItemStack result = inventory.getItem(24);
        ItemStack template = inventory.getItem(11);
        ItemStack base = inventory.getItem(13);
        ItemStack addition = inventory.getItem(15);

        if (result == null || template == null || base == null || addition == null) {
            player.sendMessage(ChatColor.RED + "All slots (template, base, addition, result) must be filled.");
            return;
        }

        String recipeName = "custom_smithing_" + result.getType().name().toLowerCase() + "_" + System.currentTimeMillis();
        NamespacedKey key = new NamespacedKey(recipeManager.getPlugin(), recipeName);

        RecipeChoice templateChoice = new RecipeChoice.ExactChoice(template);
        RecipeChoice baseChoice = new RecipeChoice.ExactChoice(base);
        RecipeChoice additionChoice = new RecipeChoice.ExactChoice(addition);

        SmithingTransformRecipe recipe = new SmithingTransformRecipe(key, result, templateChoice, baseChoice, additionChoice);

        if (recipeManager.addRecipe(recipe)) {
            player.sendMessage(ChatColor.GREEN + "Smithing recipe saved successfully!");
            Bukkit.getScheduler().runTaskLater(recipeManager.getPlugin(), () -> openMainMenu(player), 1L);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to save smithing recipe.");
        }
    }

    private void saveAnvilRecipe(Player player, Inventory inventory) {
        ItemStack input1 = inventory.getItem(13);
        ItemStack input2 = inventory.getItem(22);
        ItemStack result = inventory.getItem(25);

        if (input1 == null || input2 == null || result == null) {
            player.sendMessage(ChatColor.RED + "Input and result slots cannot be empty.");
            return;
        }

        PlayerState state = getPlayerState(player);
        state.setEnteringLevelCost(true);
        state.setTempAnvilRecipeItems(new ItemStack[]{input1, input2, result});

        player.closeInventory();
        player.sendMessage(ChatColor.GOLD + "Enter the experience level cost for this recipe in chat. Type 'cancel' to abort.");
    }

    private TrimmedRecipe trimRecipeShape(ItemStack[] matrix) {
        int minRow = -1, maxRow = -1, minCol = -1, maxCol = -1;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (matrix[r * 3 + c] != null) {
                    if (minRow == -1) minRow = r;
                    maxRow = r;
                    if (minCol == -1 || c < minCol) minCol = c;
                    if (maxCol == -1 || c > maxCol) maxCol = c;
                }
            }
        }
        if (minRow == -1) {
            return new TrimmedRecipe(new String[0], Collections.emptyMap());
        }

        Map<Character, ItemStack> ingredientMap = new LinkedHashMap<>();
        char nextChar = 'a';
        List<String> shapeList = new ArrayList<>();

        for (int r = minRow; r <= maxRow; r++) {
            StringBuilder rowStr = new StringBuilder();
            for (int c = minCol; c <= maxCol; c++) {
                ItemStack item = matrix[r * 3 + c];
                if (item != null) {
                    char mappedChar = ' ';
                    boolean found = false;
                    for (Map.Entry<Character, ItemStack> entry : ingredientMap.entrySet()) {
                        if (entry.getValue().equals(item)) {
                            mappedChar = entry.getKey();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        mappedChar = nextChar++;
                        ItemStack recipeIngredient = item.clone();
                        ingredientMap.put(mappedChar, recipeIngredient);
                    }
                    rowStr.append(mappedChar);
                } else {
                    rowStr.append(" ");
                }
            }
            shapeList.add(rowStr.toString());
        }

        return new TrimmedRecipe(shapeList.toArray(new String[0]), ingredientMap);
    }

    private record TrimmedRecipe(String[] shape, Map<Character, ItemStack> ingredientMap) {}


    private ItemStack createGuiItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }

    private enum MenuType {
        MAIN, CREATE_TYPE_SELECTION, CREATE_SHAPED, CREATE_FURNACE, CREATE_SMITHING_TRANSFORM, CREATE_ANVIL, DELETE, ENABLE, DISABLE, CONFIRM_DELETE, ENABLE_TYPE_SELECTION, DISABLE_TYPE_SELECTION
    }

    private static class PlayerState {
        private MenuType currentMenu;
        private int currentPage;
        private List<RecipeWrapper> originalRecipeList;
        private List<RecipeWrapper> displayedRecipeList;
        private RecipeWrapper recipeToConfirm;
        private boolean searching;
        private String searchQuery;
        private boolean enteringLevelCost;
        private ItemStack[] tempAnvilRecipeItems;
        private RecipeManager.RecipeType selectedRecipeTipe;


        public PlayerState() {
            this.currentPage = 0;
            this.originalRecipeList = new ArrayList<>();
            this.displayedRecipeList = new ArrayList<>();
        }

        public void clear() {
            this.currentMenu = null;
            this.currentPage = 0;
            this.originalRecipeList.clear();
            this.displayedRecipeList.clear();
            this.recipeToConfirm = null;
            this.searching = false;
            this.searchQuery = null;
            this.enteringLevelCost = false;
            this.tempAnvilRecipeItems = null;
            this.selectedRecipeTipe = null;
        }

        public MenuType getCurrentMenu() { return currentMenu; }
        public void setCurrentMenu(MenuType currentMenu) { this.currentMenu = currentMenu; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public List<RecipeWrapper> getDisplayedRecipeList() { return displayedRecipeList; }
        public void setDisplayedRecipeList(List<RecipeWrapper> recipeList) { this.displayedRecipeList = recipeList; }
        public RecipeWrapper getRecipeToConfirm() { return recipeToConfirm; }
        public void setRecipeToConfirm(RecipeWrapper recipeToConfirm) { this.recipeToConfirm = recipeToConfirm; }
        public List<RecipeWrapper> getOriginalRecipeList() { return originalRecipeList; }
        public void setOriginalRecipeList(List<RecipeWrapper> originalRecipeList) { this.originalRecipeList = originalRecipeList; }
        public boolean isSearching() { return searching; }
        public void setSearching(boolean searching) { this.searching = searching; }
        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
        public boolean isEnteringLevelCost() { return enteringLevelCost; }
        public void setEnteringLevelCost(boolean enteringLevelCost) { this.enteringLevelCost = enteringLevelCost; }
        public ItemStack[] getTempAnvilRecipeItems() { return tempAnvilRecipeItems; }
        public void setTempAnvilRecipeItems(ItemStack[] tempAnvilRecipeItems) { this.tempAnvilRecipeItems = tempAnvilRecipeItems; }
        public RecipeManager.RecipeType getSelectedRecipeType() { return selectedRecipeTipe; }
        public void setSelectedRecipeType(RecipeManager.RecipeType selectedRecipeType) { this.selectedRecipeTipe = selectedRecipeType; }
    }
}
