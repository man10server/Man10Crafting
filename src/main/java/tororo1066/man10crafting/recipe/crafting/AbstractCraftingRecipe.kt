package tororo1066.man10crafting.recipe.crafting

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.CraftingInventory
import tororo1066.man10crafting.recipe.AbstractBukkitRecipe
import tororo1066.man10crafting.recipe.options.CommandExecutable
import tororo1066.man10crafting.recipe.options.Permissible

abstract class AbstractCraftingRecipe: AbstractBukkitRecipe(), Permissible, CommandExecutable {

    override var permission: String? = null
    override var commands: List<String> = listOf()

    abstract fun craftable(inventory: CraftingInventory): Boolean

    open fun performCraft(event: CraftItemEvent): Int {

        val inventory = event.inventory

        var amount = inventory.maxStackSize
        for (matrix in inventory.matrix) {
            if (matrix == null || matrix.isEmpty) continue
            val matrixAmount = matrix.amount
            if (matrixAmount < amount) amount = matrixAmount
        }

        val actualCraftableAmount = actualCraftableAmount(event, amount)
        if (actualCraftableAmount > 0) {
            executeCommands(event.whoClicked)
        }
        return actualCraftableAmount
    }

    override fun craftable(event: Event): Boolean {
        val (inventory, player) = when (event) {
            is PrepareItemCraftEvent -> event.inventory to event.view.player
            is CraftItemEvent -> event.inventory to event.whoClicked
            else -> return false
        }

        if (!accessible(player)) return false
        return craftable(inventory)
    }

    override fun accessible(humanEntity: HumanEntity): Boolean {
        return hasPermission(humanEntity)
    }

    override fun performCraft(event: Event): Int {
        return performCraft(event as CraftItemEvent)
    }

    override fun getInventoryTypes(): List<InventoryType> {
        return listOf(InventoryType.CRAFTING, InventoryType.WORKBENCH)
    }

    override fun serializeOption(): ConfigurationSection {
        val section = super<Permissible>.serializeOption()
        section.putAll(super<CommandExecutable>.serializeOption())
        return section
    }

    override fun serialize(): ConfigurationSection {
        val map = super.serialize()
        map.putAll(serializeOption())
        return map
    }
}