package tororo1066.man10crafting.recipe.paper

import io.papermc.paper.potion.PotionMix
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BrewingStand
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.block.BrewingStartEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.inventory.InventoryType
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.inventory.register.AbstractRegister
import tororo1066.man10crafting.inventory.register.BrewingRegister
import tororo1066.man10crafting.recipe.AbstractRecipe
import tororo1066.man10crafting.recipe.RecipeDeserializer
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem

class BrewingCraftingRecipe: AbstractRecipe() {

    lateinit var input: AbstractIngredient
    lateinit var ingredient: AbstractIngredient
    var brewingTime: Int = 400

    @Suppress("UnstableApiUsage")
    override fun craftable(event: Event): Boolean {
        val inventory = when (event) {
            is BrewingStartEvent -> (event.block.state as? BrewingStand)?.inventory ?: return false
            is BrewEvent -> event.contents
            else -> return false
        }
        val ingredientItem = inventory.ingredient ?: return false
        if (!ingredient.validate(ingredientItem)) return false
        var found = false
        for (i in 0 until 3) {
            val potion = inventory.getItem(i) ?: continue
            if (input.validate(potion)) {
                found = true
                break
            }
        }

        return found
    }

    override fun accessible(humanEntity: HumanEntity): Boolean {
        return true
    }

    override fun performCraft(event: Event): Int {
        val inventory = (event as? BrewEvent)?.contents ?: return 0
        var count = 0
        for (i in 0 until 3) {
            val potion = inventory.getItem(i) ?: continue
            if (input.validate(potion)) count++
        }

        return count
    }

    fun getInputOrNull(): AbstractIngredient? {
        if (!::input.isInitialized) return null
        return input
    }

    fun getIngredientOrNull(): AbstractIngredient? {
        if (!::ingredient.isInitialized) return null
        return ingredient
    }

    override fun getIngredients(): List<AbstractIngredient> {
        return listOf(input, ingredient)
    }

    override fun SInventory.inlineRenderRecipeView(
        setIngredientItem: (slot: Int, ingredient: AbstractIngredient) -> Unit,
        setResultItem: (slot: Int, itemStack: ItemStack) -> Unit
    ) {
        setItem(
            10,
            SInventoryItem(Material.NETHER_WART)
                .setDisplayName("§a素材")
                .setCanClick(false)
        )
        setItem(
            12,
            SInventoryItem(Material.POTION)
                .setDisplayName("§dベース")
                .setCanClick(false)
        )
        setItem(
            23,
            SInventoryItem(Material.BREWING_STAND)
                .setLore("§7醸造時間: §e${brewingTime} ticks")
                .setCanClick(false)
        )

        setIngredientItem(19, ingredient)
        setIngredientItem(21, input)

        setResultItem(25, result)
    }

    override fun getRegisterInventory(): AbstractRegister<*> {
        return BrewingRegister(this)
    }

    override fun getInventoryTypes(): List<InventoryType> {
        return listOf(InventoryType.BREWING)
    }

    override fun register() {
        val potionMix = PotionMix(
            namespacedKey,
            result,
            input.createBukkitRecipeChoice(),
            ingredient.createBukkitRecipeChoice()
        )

        Bukkit.getPotionBrewer().addPotionMix(potionMix)
    }

    override fun unregister() {
        Bukkit.getPotionBrewer().removePotionMix(namespacedKey)
    }

    override fun serialize(): ConfigurationSection {
        val section = super.serialize()
        section.set("ingredient", ingredient)
        section.set("input", input)
        section.set("brewingTime", brewingTime)
        return section
    }

    companion object: RecipeDeserializer<BrewingCraftingRecipe> {
        @Suppress("UNCHECKED_CAST")
        override fun deserialize(section: ConfigurationSection): BrewingCraftingRecipe? {
            val recipe = BrewingCraftingRecipe()
            recipe.ingredient = section.get("ingredient") as? AbstractIngredient ?: return null
            recipe.input = section.get("input") as? AbstractIngredient ?: return null
            recipe.brewingTime = section.getInt("brewingTime", 400)
            return recipe
        }
    }
}