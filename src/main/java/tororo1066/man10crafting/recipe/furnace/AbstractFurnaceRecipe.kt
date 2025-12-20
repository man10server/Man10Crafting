package tororo1066.man10crafting.recipe.furnace

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.block.BlockCookEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.inventory.register.Items
import tororo1066.man10crafting.recipe.AbstractBukkitRecipe
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem

abstract class AbstractFurnaceRecipe: AbstractBukkitRecipe() {

    lateinit var input: AbstractIngredient
    var experience: Float = 0f
    var cookingTime: Int = 200

    protected abstract val furnaceMaterial: Material

    fun getInputOrNull(): AbstractIngredient? {
        if (!::input.isInitialized) return null
        return input
    }

    override fun craftable(event: Event): Boolean {
        val blockCookEvent = event as? BlockCookEvent ?: return false
        return input.validate(blockCookEvent.source)
    }

    override fun accessible(humanEntity: HumanEntity): Boolean {
        return true
    }

    override fun performCraft(event: Event): Int {
        return 1
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
            SInventoryItem(furnaceMaterial)
                .setCanClick(false)
                .setLore(
                    "§7製錬時間: §e${cookingTime} ticks",
                    "§7経験値量: §e${String.format("%.2f", experience).replace(Regex("\\.0+$"), "")}"
                )
        )

        setIngredientItem(11, input)
        setItem(20, Items.backgroundBlack())

        setResultItem(24, result)
    }

    override fun getInventoryTypes(): List<InventoryType> {
        return listOf(InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER)
    }

    override fun serialize(): ConfigurationSection {
        val section = super.serialize()
        section.set("input", input)
        section.set("experience", experience.toDouble())
        section.set("cookingTime", cookingTime)
        return section
    }

    @Suppress("UNCHECKED_CAST")
    protected fun deserializeCommon(section: ConfigurationSection): Boolean {
        input = section.get("input") as? AbstractIngredient ?: return false
        experience = section.getDouble("experience", 0.0).toFloat()
        cookingTime = section.getInt("cookingTime", 200)
        return true
    }
}