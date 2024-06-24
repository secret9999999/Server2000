package io.papermc.testplugin;

import io.papermc.paper.event.player.ChatEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import static net.kyori.adventure.text.Component.text;

public final class TestPlugin extends JavaPlugin implements Listener {

    final NamespacedKey key = new NamespacedKey(this, "test");
    final NamespacedKey shapelessTest = new NamespacedKey(this, "shapeless_test");
    final ItemStack ingredient = new ItemStack(Material.STICK);
    {
        this.ingredient.editMeta(meta -> {
            meta.displayName(text("ingredient"));
            meta.setCustomModelData(2);
            meta.getPersistentDataContainer().set(this.key, PersistentDataType.BOOLEAN, true);
        });
    }
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        final ItemStack result = new ItemStack(Material.DIAMOND);
        result.editMeta(meta -> {
            meta.displayName(text("CUSTOM"));
        });
        final ShapelessRecipe recipe = new ShapelessRecipe(this.key, result);
        recipe.addIngredient(Material.BRICK);
        recipe.addIngredient(Material.STICK);

        recipe.addIngredient(new RecipeChoice.ExactChoice(this.ingredient));
        this.getServer().addRecipe(recipe);


        final ShapelessRecipe test = new ShapelessRecipe(this.shapelessTest, new ItemStack(Material.DIAMOND));
        test.addIngredient(Material.STICK);
        test.addIngredient(Material.STICK);
        this.getServer().addRecipe(test);
        // io.papermc.testplugin.brigtests.Registration.registerViaOnEnable(this);
    }

    @EventHandler
    public void event(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(this.key);
        event.getPlayer().discoverRecipe(this.shapelessTest);
    }

    @EventHandler
    public void event(ChatEvent event) {
        event.getPlayer().getInventory().addItem(this.ingredient.asQuantity(64));
    }

}
