package tororo1066.man10crafting.recipe.smithing

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.inventory.SmithItemEvent
import org.bukkit.inventory.RecipeChoice
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.recipe.AbstractBukkitRecipe
import tororo1066.man10crafting.recipe.options.CommandExecutable
import tororo1066.man10crafting.recipe.options.Permissible
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem

abstract class AbstractSmithingRecipe: AbstractBukkitRecipe(), Permissible, CommandExecutable {
    var template: AbstractIngredient? = null
    var base: AbstractIngredient? = null
    var addition: AbstractIngredient? = null
    var copyDataComponents: Boolean = false

    override var permission: String? = null
    override var commands: List<String> = listOf()

    protected fun recipeChoiceOrEmpty(ingredient: AbstractIngredient?): RecipeChoice {
        return ingredient?.createBukkitRecipeChoice() ?: RecipeChoice.empty()
    }

    override fun craftable(event: Event): Boolean {
        val (inventory, player) = when (event) {
            is PrepareSmithingEvent -> event.inventory to event.view.player
            is SmithItemEvent -> event.inventory to event.whoClicked
            else -> return false
        }

        if (!accessible(player)) return false

        val inputTemplate = inventory.inputTemplate
        val inputBase = inventory.inputEquipment
        val inputAddition = inventory.inputMineral

        template?.let { template ->
            if ((inputTemplate == null || inputTemplate.isEmpty) || !template.validate(inputTemplate)) return false
        }
        base?.let { base ->
            if ((inputBase == null || inputBase.isEmpty) || !base.validate(inputBase)) return false
        }
        addition?.let { addition ->
            if ((inputAddition == null || inputAddition.isEmpty) || !addition.validate(inputAddition)) return false
        }

        return true
    }

    override fun accessible(humanEntity: HumanEntity): Boolean {
        return hasPermission(humanEntity)
    }

    override fun performCraft(event: Event): Int {
        val smithItemEvent = event as? SmithItemEvent ?: return 0
        val amount = minOf(
            smithItemEvent.inventory.inputTemplate?.amount ?: Int.MAX_VALUE,
            smithItemEvent.inventory.inputEquipment?.amount ?: Int.MAX_VALUE,
            smithItemEvent.inventory.inputMineral?.amount ?: Int.MAX_VALUE,
            result.maxStackSize
        )

        val actualCraftableAmount = actualCraftableAmount(smithItemEvent, amount)
        if (actualCraftableAmount > 0) {
            executeCommands(smithItemEvent.whoClicked)
        }
        return actualCraftableAmount
    }

    override fun getIngredients(): List<AbstractIngredient> {
        return listOfNotNull(template, base, addition)
    }

    protected fun SInventory.inlineRenderCommonSmithingRecipeView(
        setIngredientItem: (slot: Int, ingredient: AbstractIngredient) -> Unit
    ) {
        setItem(
            10,
            SInventoryItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .setDisplayName("§aテンプレート")
                .setCanClick(false)
        )
        setItem(
            11,
            SInventoryItem(Material.DIAMOND_PICKAXE)
                .setDisplayName("§aベース")
                .setCanClick(false)
        )
        setItem(
            12,
            SInventoryItem(Material.NETHERITE_INGOT)
                .setDisplayName("§a追加素材")
                .setCanClick(false)
        )

        val noneItem = SInventoryItem(Material.BARRIER)
            .setDisplayName("§cなし")
            .setCanClick(false)

        template?.let {
            setIngredientItem(19, it)
        } ?: setItem(19, noneItem)
        base?.let {
            setIngredientItem(20, it)
        } ?: setItem(20, noneItem)
        addition?.let {
            setIngredientItem(21, it)
        } ?: setItem(21, noneItem)
    }

    override fun getInventoryTypes(): List<InventoryType> {
        return listOf(InventoryType.SMITHING)
    }

    override fun serializeOption(): ConfigurationSection {
        val section = super<Permissible>.serializeOption()
        section.putAll(super<CommandExecutable>.serializeOption())
        return section
    }

    override fun serialize(): ConfigurationSection {
        val section = super.serialize()
        template?.let { section.set("template", it) }
        base?.let { section.set("base", it) }
        addition?.let { section.set("addition", it) }
        section.set("copyDataComponents", copyDataComponents)
        section.putAll(serializeOption())
        return section
    }

    @Suppress("UNCHECKED_CAST")
    protected fun deserializeCommon(section: ConfigurationSection): Boolean {
        template = section.get("template") as? AbstractIngredient
        base = section.get("base") as? AbstractIngredient
        addition = section.get("addition") as? AbstractIngredient
        copyDataComponents = section.getBoolean("copyDataComponents", false)
        permission = Permissible.deserialize(section)
        commands = CommandExecutable.deserialize(section)
        return template != null || base != null || addition != null
    }
}