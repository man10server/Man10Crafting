package tororo1066.man10crafting.recipe.stonecutting

import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.StonecutterInventory
import org.bukkit.inventory.StonecuttingRecipe
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.inventory.register.AbstractRegister
import tororo1066.man10crafting.inventory.register.Items
import tororo1066.man10crafting.inventory.register.Items.hideTooltip
import tororo1066.man10crafting.inventory.register.StonecuttingRegister
import tororo1066.man10crafting.recipe.AbstractBukkitRecipe
import tororo1066.man10crafting.recipe.RecipeDeserializer
import tororo1066.man10crafting.recipe.options.CommandExecutable
import tororo1066.man10crafting.recipe.options.Permissible
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem

class StonecuttingCraftingRecipe: AbstractBukkitRecipe(), Permissible, CommandExecutable {

    lateinit var input: AbstractIngredient

    override var permission: String? = null
    override var commands: List<String> = listOf()

    fun getInputOrNull(): AbstractIngredient? {
        if (!::input.isInitialized) return null
        return input
    }

    override fun createBukkitRecipe(): Recipe {
        return StonecuttingRecipe(
            namespacedKey,
            result,
            input.createBukkitRecipeChoice()
        )
    }

    override fun craftable(event: Event): Boolean {
        val (player, inventory) = when (event) {
            is PlayerStonecutterRecipeSelectEvent -> event.player to event.stonecutterInventory
            is InventoryClickEvent -> event.whoClicked to (event.inventory as? StonecutterInventory ?: return false)
            else -> return false
        }

        if (!accessible(player)) return false

        val inputItem = inventory.inputItem ?: return false
        return input.validate(inputItem)
    }

    override fun accessible(humanEntity: HumanEntity): Boolean {
        return hasPermission(humanEntity)
    }

    override fun performCraft(event: Event): Int {
        val event = event as? InventoryClickEvent ?: return 0
        val inventory = event.inventory as? StonecutterInventory ?: return 0
        val amount = inventory.inputItem?.amount ?: return 0
        val actualCraftableAmount = actualCraftableAmount(event, amount)
        if (actualCraftableAmount > 0) {
            executeCommands(event.whoClicked)
        }
        return actualCraftableAmount
    }

    override fun getIngredients(): List<AbstractIngredient> {
        return listOf(input)
    }

    override fun SInventory.inlineRenderRecipeView(
        setIngredientItem: (slot: Int, ingredient: AbstractIngredient) -> Unit,
        setResultItem: (slot: Int, itemStack: ItemStack) -> Unit
    ) {
        setItem(
            22,
            SInventoryItem(Material.STONECUTTER)
                .hideTooltip()
                .setCanClick(false)
        )

        setIngredientItem(20, input)
        setItem(29, Items.backgroundWhite())

        setResultItem(24, result)
    }

    override fun getRegisterInventory(): AbstractRegister<*> {
        return StonecuttingRegister(this)
    }

    override fun getInventoryTypes(): List<InventoryType> {
        return listOf(InventoryType.STONECUTTER)
    }

    override fun serializeOption(): ConfigurationSection {
        val section = super<Permissible>.serializeOption()
        section.putAll(super<CommandExecutable>.serializeOption())
        return section
    }

    override fun serialize(): ConfigurationSection {
        val section = super.serialize()
        section.set("input", input)
        section.putAll(serializeOption())
        return section
    }

    companion object: RecipeDeserializer<StonecuttingCraftingRecipe> {
        @Suppress("UNCHECKED_CAST")
        override fun deserialize(section: ConfigurationSection): StonecuttingCraftingRecipe? {
            val recipe = StonecuttingCraftingRecipe()

            recipe.input = section.get("input") as? AbstractIngredient ?: return null
            recipe.permission = Permissible.deserialize(section)
            recipe.commands = CommandExecutable.deserialize(section)

            return recipe
        }
    }
}