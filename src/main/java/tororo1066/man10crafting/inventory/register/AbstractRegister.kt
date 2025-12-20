package tororo1066.man10crafting.inventory.register

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import tororo1066.man10crafting.Man10Crafting
import tororo1066.man10crafting.ingredient.AbstractIngredient
import tororo1066.man10crafting.ingredient.ItemStackIngredient
import tororo1066.man10crafting.recipe.AbstractRecipe
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem
import java.util.concurrent.CompletableFuture

abstract class AbstractRegister<T: AbstractRecipe>(
    name: String,
    var currentData: T,
    val isEdit: Boolean
): SInventory(SJavaPlugin.plugin, name, 5) {

    override var savePlaceItems = true
    open var advancedMode = false
    abstract val task: BukkitTask

    abstract fun prepareSave(): Boolean

    abstract fun readInput()

    init {
        setOnClose {
            task.cancel()
        }
    }

    protected fun onlySupportedAdvanced(ingredients: Collection<AbstractIngredient>): Boolean {
        return ingredients.any { it !is ItemStackIngredient || it.displayItems.size > 1 }
    }

    protected fun onlySupportedAdvanced(ingredient: AbstractIngredient): Boolean {
        return onlySupportedAdvanced(listOf(ingredient))
    }

    protected fun itemStackToIngredient(itemStack: ItemStack): Pair<AbstractIngredient, Int> {
        val ingredient = ItemStackIngredient()
        ingredient.itemStacks.add(itemStack.clone())
        return Pair(ingredient, 0)
    }

    fun save(name: String, category: String): CompletableFuture<Boolean> {
        if (!advancedMode) readInput()
        if (!prepareSave()) return CompletableFuture.completedFuture(false)
        currentData.key = name
        currentData.category = category
        return currentData.save().whenComplete { success, error ->
            if (success && error == null) {
                Bukkit.getScheduler().runTask(SJavaPlugin.plugin, Runnable {
                    Man10Crafting.recipes[currentData.namespacedKey] = currentData
                    currentData.register()
                    Man10Crafting.plugin.rebuildIngredientCache()
                })
            } else {
                error?.printStackTrace()
            }
        }
    }

    fun saveItem(): SInventoryItem {
        if (isEdit) {
            return SInventoryItem(
                SItem(Material.WRITABLE_BOOK)
                    .setDisplayName("§a上書き保存")
            ).setCanClick(false)
                .setClickEvent {
                    it.whoClicked.closeInventory()
                    save(currentData.key, currentData.category).thenAccept { success ->
                        if (success) {
                            it.whoClicked.sendMessage("§a保存に成功しました")
                        } else {
                            it.whoClicked.sendMessage("§c保存に失敗しました")
                        }
                    }
                }
        } else {
            return SInventoryItem(
                SItem(Material.WRITABLE_BOOK)
                    .setDisplayName("§a保存")
            ).setCanClick(false)
                .setClickEvent {
                    val p = it.whoClicked as Player
                    throughClose(p)
                    SJavaPlugin.sInput.sendInputCUI(
                        p,
                        String::class.java,
                        "§aカテゴリー名を入力してください",
                        action = { category ->

                            fun matchedNamespacedKeyRegex(str: String): Boolean {
                                val regex = Regex("^[a-z0-9_-]+$")
                                return regex.matches(str)
                            }

                            if (!matchedNamespacedKeyRegex(category)) {
                                p.sendMessage("§cカテゴリー名は半角英数字(小文字)、_、-のみ使用可能です")
                                open(p)
                                return@sendInputCUI
                            }

                            SJavaPlugin.sInput.sendInputCUI(
                                p,
                                String::class.java,
                                "§aレシピの内部名を入力してください",
                                action = { str ->

                                    if (!matchedNamespacedKeyRegex(str)) {
                                        p.sendMessage("§cレシピの内部名は半角英数字(小文字)、_、-のみ使用可能です")
                                        open(p)
                                        return@sendInputCUI
                                    }

                                    if (!isEdit && Man10Crafting.recipes.values.any { r -> r.key == str && r.category == category }) {
                                        p.sendMessage("§cその名前のレシピは既に存在します")
                                        open(p)
                                        return@sendInputCUI
                                    }
                                    task.cancel()
                                    save(str, category).thenAccept { success ->
                                        if (success) {
                                            p.sendMessage("§a保存に成功しました")
                                        } else {
                                            p.sendMessage("§c保存に失敗しました")
                                        }
                                    }
                                },
                                onCancel = {
                                    open(p)
                                })
                        },
                        onCancel = {
                            open(p)
                        })
                }
        }
    }
}