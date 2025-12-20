package tororo1066.man10crafting

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.event.inventory.InventoryType
import tororo1066.man10crafting.commands.MCCommand
import tororo1066.man10crafting.customFurnace.CustomFurnaceManager
import tororo1066.man10crafting.data.CategoryDisplayItem
import tororo1066.man10crafting.data.LegacyRecipeData
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.ingredient.ItemStackIngredient
import tororo1066.man10crafting.ingredient.MaterialIngredient
import tororo1066.man10crafting.ingredient.NBTIngredient
import tororo1066.man10crafting.recipe.AbstractRecipe
import tororo1066.tororopluginapi.SInput
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.config.SConfig
import tororo1066.tororopluginapi.database.SDBVariable
import tororo1066.tororopluginapi.database.SDatabase
import tororo1066.tororopluginapi.database.mysql.SMySQL
import tororo1066.tororopluginapi.lang.SLang
import java.io.File

class Man10Crafting: SJavaPlugin(UseOption.SInput) {

    companion object {
        lateinit var plugin: Man10Crafting
        val old_recipes = HashMap<String,LegacyRecipeData>()
        val recipes = HashMap<NamespacedKey, AbstractRecipe>()
        val categoryDisplayItems = HashMap<String, CategoryDisplayItem>()
        val ingredientCache = HashMap<InventoryType, MutableList<AbstractIngredient>>()
        lateinit var sConfig: SConfig
        lateinit var sInput: SInput
        lateinit var sLang: SLang
        lateinit var sDatabase: SDatabase
        lateinit var customFurnaceManager: CustomFurnaceManager
        var enabledAutoCraft = true
        var disabledVanillaCraftWithCustomModelDataItem = false
        var disabledCustomModelDataItemAsVanillaItem = false
        val prefix = SStr("&c[&bMan10Crafting&c]&r").toString()

        var enabledNBTAPI = false
            private set
    }

    override fun onStart() {

        ConfigurationSerialization.registerClass(ItemStackIngredient::class.java)
        ConfigurationSerialization.registerClass(MaterialIngredient::class.java)
        ConfigurationSerialization.registerClass(NBTIngredient::class.java)

        if (Bukkit.getPluginManager().isPluginEnabled("NBTAPI")){
            enabledNBTAPI = true
        } else {
            logger.warning("NBTAPIが導入されていません。NBTIngredientを使用したレシピは読み込まれません。")
        }

        saveDefaultConfig()
        plugin = this
        sLang = SLang(this)
        sConfig = SConfig(this)
        sInput = SInput(this)
        sDatabase = SMySQL(this)
//        customFurnaceManager = CustomFurnaceManager()

        sDatabase.backGroundCreateTable(
            "craft_log",
            mapOf(
                "id" to SDBVariable(SDBVariable.Int, index = SDBVariable.Index.PRIMARY, autoIncrement = true),
                "type" to SDBVariable(SDBVariable.VarChar, length = 36),
                "uuid" to SDBVariable(SDBVariable.VarChar, length = 36),
                "name" to SDBVariable(SDBVariable.VarChar, length = 16),
                "location" to SDBVariable(SDBVariable.Text),
                "recipe" to SDBVariable(SDBVariable.Text),
                "amount" to SDBVariable(SDBVariable.Int),
                "date" to SDBVariable(SDBVariable.DateTime)
            )
        )

        reloadPluginConfig()
    }

    fun reloadPluginConfig() {
        plugin.reloadConfig()
        val iterator = Bukkit.recipeIterator()
        while (iterator.hasNext()) {
            val recipe = iterator.next()
            if (recipe is Keyed && recipe.key.namespace == "man10crafting"){
                iterator.remove()
            }
        }
        val folder = File("${dataFolder.path}/recipes/")
        if (!folder.exists()) folder.mkdirs()
        recipes.clear()
        val dataList = mutableListOf<AbstractRecipe>()
        folder.listFiles()?.forEach {
            it.listFiles()?.forEach { file ->
                if (file.extension != "yml") return@forEach
                val yaml = YamlConfiguration.loadConfiguration(file)

                dataList.add(AbstractRecipe.deserialize(yaml) ?: return@forEach)
            }
        }
        dataList.sortedBy { it.registerIndex ?: it.index }.forEach {
            try {
                it.register()
                recipes[it.namespacedKey] = it
            } catch (e: Exception) {
                logger.warning("Failed to register recipe: ${it.key}")
                e.printStackTrace()
            }
        }

        rebuildIngredientCache()

        enabledAutoCraft = config.getBoolean("enabledAutoCraft",true)
        disabledVanillaCraftWithCustomModelDataItem = config.getBoolean("disabledVanillaCraftWithCustomModelDataItem",false)
        disabledCustomModelDataItemAsVanillaItem = config.getBoolean("disabledCustomModelDataItemAsVanillaItem",false)

        categoryDisplayItems.clear()
        val section = config.getConfigurationSection("categoryDisplayItems")
        section?.getKeys(false)?.forEach {
            val categorySection = section.getConfigurationSection(it)?:return@forEach
            val item = categorySection.getItemStack("itemStack")?:return@forEach
            val respectItemStackName = categorySection.getBoolean("respectItemStackName",false)
            categoryDisplayItems[it] = CategoryDisplayItem(item,respectItemStackName)
        }

        MCCommand()
    }

    fun rebuildIngredientCache() {
        ingredientCache.clear()
        recipes.values.forEach { recipe ->
            val types = recipe.getInventoryTypes()
            types.forEach { type ->
                ingredientCache.getOrPut(type) { mutableListOf() }
                    .addAll(recipe.getIngredients())
            }
        }
    }

    override fun onEnd() {

    }
}