package tororo1066.man10crafting.listeners

import org.bukkit.Keyed
import org.bukkit.Location
import org.bukkit.block.BrewingStand
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.block.BrewingStartEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.*
import org.bukkit.inventory.BrewerInventory
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.StonecutterInventory
import tororo1066.man10crafting.Man10Crafting
import tororo1066.man10crafting.recipe.AbstractRecipe
import tororo1066.man10crafting.recipe.paper.BrewingCraftingRecipe
import tororo1066.man10crafting.recipe.stonecutting.StonecuttingCraftingRecipe
import tororo1066.tororopluginapi.annotation.SEventHandler
import tororo1066.tororopluginapi.utils.LocType
import tororo1066.tororopluginapi.utils.toLocString
import java.time.LocalDateTime

class CraftListener {

    private fun insertLog(recipe: AbstractRecipe, player: Player? = null, location: Location, amount: Int) {
        Man10Crafting.sDatabase.backGroundInsert(
            "craft_log",
            mapOf(
                "type" to recipe.javaClass.simpleName,
                "uuid" to (player?.uniqueId?.toString() ?: "none"),
                "name" to (player?.name ?: "none"),
                "location" to location.toLocString(LocType.WORLD_BLOCK_SPACE),
                "recipe" to "${recipe.category}/${recipe.key}",
                "amount" to amount,
                "date" to LocalDateTime.now()
            )
        )
    }

    private fun CraftingInventory.checkCanCraftVanilla(): Boolean {
        if (!Man10Crafting.disabledVanillaCraftWithCustomModelDataItem) return true

        (1..9).forEach {
            val item = this.getItem(it)?:return@forEach
            if (item.hasItemMeta() && item.itemMeta.hasCustomModelDataComponent()) {
                return false
            }
        }
        return true
    }

    @SEventHandler
    fun event(e: PrepareItemCraftEvent) {
        val eventRecipe = e.recipe ?: return
        if (eventRecipe !is Keyed) return
        val namespacedKey = eventRecipe.key

        if (namespacedKey.namespace == "minecraft" && !e.inventory.checkCanCraftVanilla()) {
            e.inventory.result = null
            return
        }

        if (namespacedKey.namespace != "man10crafting") return
        val recipe = Man10Crafting.recipes[namespacedKey] ?: return
        if (!recipe.enabled || !recipe.craftable(e)) {
            e.inventory.result = null
            return
        }
    }

    @SEventHandler
    fun event(e: CraftItemEvent) {
        if (e.isCancelled) return
        val eventRecipe = e.recipe
        if (eventRecipe !is Keyed) return
        val namespacedKey = eventRecipe.key

        if (namespacedKey.namespace == "minecraft" && !e.inventory.checkCanCraftVanilla()) {
            e.isCancelled = true
            e.inventory.result = null
            return
        }

        if (namespacedKey.namespace != "man10crafting") return
        val recipe = Man10Crafting.recipes[namespacedKey] ?: return
        if (!recipe.enabled || !recipe.craftable(e)) {
            e.isCancelled = true
            e.inventory.result = null
            return
        }

        val amount = recipe.performCraft(e)

        if (!recipe.craftable(e)) {
            e.inventory.result = null
        }

        if (amount > 0) {
            val player = e.whoClicked as? Player ?: return
            insertLog(recipe, player, player.location, amount)
        }
    }

    @SEventHandler
    fun event(e: CrafterCraftEvent) {
        if (Man10Crafting.recipes.containsKey(e.recipe.key)) {
            e.isCancelled = true
        }
    }

    @SEventHandler
    fun event(e: BlockCookEvent) {
        if (e.isCancelled) return
        val eventRecipe = e.recipe ?: return
        val namespacedKey = eventRecipe.key
        if (namespacedKey.namespace != "man10crafting") return
        val recipe = Man10Crafting.recipes[namespacedKey] ?: return
        if (!recipe.enabled || !recipe.craftable(e)) {
            e.isCancelled = true
            return
        }

        val amount = recipe.performCraft(e)

        if (amount > 0) {
            insertLog(recipe, null, e.block.location, amount)
        }
    }

    @SEventHandler
    fun event(e: FurnaceSmeltEvent) {
        event(e as BlockCookEvent)
    }

    @SEventHandler
    fun event(e: PrepareSmithingEvent) {
        val eventRecipe = e.inventory.recipe ?: return
        if (eventRecipe !is Keyed) return
        val namespacedKey = eventRecipe.key
        if (namespacedKey.namespace != "man10crafting") return
        val recipe = Man10Crafting.recipes[namespacedKey] ?: return
        if (!recipe.enabled || !recipe.craftable(e)) {
            e.inventory.result = null
            return
        }
    }

    @SEventHandler
    fun event(e: SmithItemEvent) {
        if (e.isCancelled) return
        val eventRecipe = e.inventory.recipe ?: return
        if (eventRecipe !is Keyed) return
        val namespacedKey = eventRecipe.key
        if (namespacedKey.namespace != "man10crafting") return
        val recipe = Man10Crafting.recipes[namespacedKey] ?: return
        if (!recipe.enabled || !recipe.craftable(e)) {
            e.isCancelled = true
            e.inventory.result = null
            return
        }

        val amount = recipe.performCraft(e)
        if (amount > 0) {
            val player = e.whoClicked as? Player ?: return
            insertLog(recipe, player, player.location, amount)
        }
    }

    @SEventHandler // Stonecutter
    fun event(e: InventoryClickEvent) {
        if (e.isCancelled) return
        val inventory = e.inventory
        if (inventory !is StonecutterInventory) return
        if (e.clickedInventory != inventory) return
        if (e.slot != 1) return
        val input = inventory.inputItem ?: return
        val result = inventory.result ?: return
        val recipe = Man10Crafting.recipes.values.firstOrNull {
            it is StonecuttingCraftingRecipe && result == it.result && it.input.validate(input)
        } ?: return

        if (!recipe.enabled || !recipe.craftable(e)) {
            e.isCancelled = true
            inventory.result = null
            return
        }

        val amount = recipe.performCraft(e)

        if (amount > 0) {
            val player = e.whoClicked as? Player ?: return
            insertLog(recipe, player, player.location, amount)
        }
    }

    @Suppress("UnstableApiUsage")
    @SEventHandler
    fun event(e: BrewingStartEvent) {
        val inventory = (e.block.state as? BrewingStand)?.inventory ?: return
        val recipes = getBrewingRecipes(inventory)
        if (recipes.isEmpty()) return
        var canBrew = true
        recipes.forEach { recipe ->
            if (!recipe.enabled || !recipe.craftable(e)) {
                e.brewingTime = Int.MAX_VALUE
                canBrew = false
            }
        }

        if (!canBrew) return
        e.brewingTime = recipes.maxOf { it.brewingTime }
    }

    @SEventHandler
    fun event(e: BrewEvent) {
        if (e.isCancelled) return
        val inventory = e.contents
        val recipes = getBrewingRecipes(inventory)
        if (recipes.isEmpty()) return
        recipes.forEach { recipe ->
            if (!recipe.enabled || !recipe.craftable(e)) {
                e.isCancelled = true
                return@forEach
            }

            val amount = recipe.performCraft(e)
            if (amount > 0) {
                insertLog(recipe, null, e.block.location, amount)
            }
        }
    }

    private fun getBrewingRecipes(inventory: BrewerInventory): List<BrewingCraftingRecipe> {
        val ingredientItem = inventory.ingredient ?: return listOf()
        val inputs = listOfNotNull(
            inventory.getItem(0),
            inventory.getItem(1),
            inventory.getItem(2)
        )
        return Man10Crafting.recipes.values.filterIsInstance<BrewingCraftingRecipe>().filter {
            it.ingredient.validate(ingredientItem) && inputs.any { input -> it.input.validate(input) }
        }
    }
}