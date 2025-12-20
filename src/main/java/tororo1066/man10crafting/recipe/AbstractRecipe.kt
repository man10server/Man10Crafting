package tororo1066.man10crafting.recipe

import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.inventory.register.AbstractRegister
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.sInventory.SInventory
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.reflect.full.companionObjectInstance

abstract class AbstractRecipe {

    lateinit var result: ItemStack
    var category: String = ""
    var key: String = ""
    var enabled: Boolean = true
    var index: Int = Int.MAX_VALUE
    var registerIndex: Int? = null
    var hidden: Boolean = false

    val namespacedKey
        get() = NamespacedKey("man10crafting", "$category-$key")

    abstract fun craftable(event: Event): Boolean

    abstract fun accessible(humanEntity: HumanEntity): Boolean

    abstract fun performCraft(event: Event): Int

    abstract fun register()

    abstract fun unregister()

    abstract fun getIngredients(): List<AbstractIngredient>

    protected abstract fun SInventory.inlineRenderRecipeView(
        setIngredientItem: (slot: Int, ingredient: AbstractIngredient) -> Unit,
        setResultItem: (slot: Int, itemStack: ItemStack) -> Unit
    )

    abstract fun getRegisterInventory(): AbstractRegister<*>

    abstract fun getInventoryTypes(): List<InventoryType>

    fun renderRecipeView(
        inventory: SInventory,
        setIngredientItem: (slot: Int, ingredient: AbstractIngredient) -> Unit,
        setResultItem: (slot: Int, itemStack: ItemStack) -> Unit
    ) {
        inventory.inlineRenderRecipeView(setIngredientItem, setResultItem)
    }

    fun getResultOrNull(): ItemStack? {
        if (!::result.isInitialized) return null
        return result.clone()
    }

    fun save(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val file = File("${SJavaPlugin.plugin.dataFolder}/recipes/$category/$key.yml")
                file.parentFile?.mkdirs()
                val yaml = YamlConfiguration()
                yaml.putAll(serialize())
                yaml.save(file)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun delete(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val file = File("${SJavaPlugin.plugin.dataFolder}/recipes/$category/$key.yml")
                if (file.exists()) {
                    return@supplyAsync file.delete()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    open fun serialize(): ConfigurationSection {
        val section = YamlConfiguration()
        section.set("clazz", this::class.qualifiedName ?: "")
        section.set("category", category)
        section.set("key", key)
        section.set("enabled", enabled)
        section.set("index", index)
        registerIndex?.let { section.set("registerIndex", it) }
        section.set("hidden", hidden)
        section.set("result", result)
        return section
    }

    protected fun ConfigurationSection.putAll(other: ConfigurationSection) {
        for ((key, value) in other.getValues(true)) {
            this.set(key, value)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun deserialize(section: ConfigurationSection): AbstractRecipe? {
            val clazz = section.getString("clazz") ?: return null
            val kClass = try {
                Class.forName(clazz).kotlin
            } catch (_: ClassNotFoundException) {
                return null
            }
            val companion = kClass.companionObjectInstance ?: return null

            if (companion is RecipeDeserializer<*>) {
                val data = companion.deserialize(section) ?: return null
                data.result = section.getItemStack("result") ?: return null
                data.category = section.getString("category") ?: return null
                data.key = section.getString("key") ?: return null
                data.enabled = section.getBoolean("enabled", true)
                data.index = section.getInt("index", Int.MAX_VALUE)
                data.registerIndex = if (section.isInt("registerIndex")) section.getInt("registerIndex") else null
                data.hidden = section.getBoolean("hidden", false)
                return data
            }

            return null
        }
    }
}