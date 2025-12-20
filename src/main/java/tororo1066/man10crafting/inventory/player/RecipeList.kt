package tororo1066.man10crafting.inventory.player

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import tororo1066.man10crafting.Man10Crafting
import tororo1066.tororopluginapi.defaultMenus.CategorySInventory
import tororo1066.tororopluginapi.lang.SLang.Companion.translate
import tororo1066.tororopluginapi.sInventory.SInventoryItem

class RecipeList: CategorySInventory("${Man10Crafting.prefix}§aレシピ一覧") {

    var search: String = ""

    init {
        setOnClick {
            it.isCancelled = true
        }
    }

    fun matchSearch(itemStack: ItemStack, player: Player): Boolean {
        if (search.isEmpty()) return true
        val hasDisplayName = itemStack.hasItemMeta() && itemStack.itemMeta!!.hasDisplayName()
        if (hasDisplayName) {
            val displayName = itemStack.itemMeta!!.displayName()
            displayName?.let {
                if (PlainTextComponentSerializer.plainText().serialize(it).contains(search, ignoreCase = true)) {
                    return true
                }
            }
        } else {
            val itemName = GlobalTranslator.translator().translate(
                itemStack.type.translationKey(),
                player.locale()
            )?.format(null, StringBuffer(), null)
                ?.toString() ?: itemStack.type.name
            if (itemName.contains(search, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    override fun renderMenu(p: Player): Boolean {
        val recipesByResult = Man10Crafting.recipes.values
            .filter {
                it.enabled && !it.hidden && it.accessible(p)
                        && (search.isEmpty() || matchSearch(it.result, p))
            }.groupBy {
                it.result.clone().apply { amount = 1 }
            }.entries.sortedBy {
                it.value.minOf { r -> r.index }
            }
        val items = LinkedHashMap<String, ArrayList<SInventoryItem>>()
        recipesByResult.forEach { (result, recipes) ->
            val item = SInventoryItem(result)
                .setCanClick(false)
                .setClickEvent { _ ->
                    val view = RecipeView(p) { r -> r.result.isSimilar(result) }
                    if (view.isEmpty) return@setClickEvent
                    moveChildInventory(view, p)
                }
            val categories = recipes.map { it.category }.distinct()
            categories.forEach { category ->
                val transKey = translate("categories.$category")
                if (nowCategory.isEmpty()) nowCategory = transKey
                items.computeIfAbsent(transKey) { ArrayList() }.add(item)
            }
        }

        if (items.isEmpty()) {
            p.sendMessage("${Man10Crafting.prefix}§cレシピが見つかりませんでした")
            setResourceItems(LinkedHashMap())
            search = ""
            return false
        }

        if (search.isNotEmpty()) {
            setCategoryName(items.keys.first())
        }

        setResourceItems(items)
        return true
    }

    override fun renderInventory(category: String, page: Int) {
        super.renderInventory(category, page)
        setItem(
            52,
            SInventoryItem(Material.OAK_SIGN)
                .setDisplayName("§a検索")
                .setLore(
                    "§7現在の検索ワード: §f${search.ifEmpty { "なし" }}",
                    "",
                    "§eクリックで変更"
                )
                .setCanClick(false)
                .setClickEvent { e ->
                    @Suppress("UnstableApiUsage")
                    val dialog = Dialog.create { builder ->
                        builder.empty()
                            .base(
                                DialogBase.builder(Component.text("検索"))
                                    .canCloseWithEscape(true)
                                    .body(listOf(DialogBody.plainMessage(Component.text("検索ワードを入力してください"))))
                                    .inputs(
                                        listOf(
                                            DialogInput.text("search", Component.text("検索ワード"))
                                                .initial(search)
                                                .build()
                                        )
                                    )
                                    .build()
                            )
                            .type(
                                DialogType.confirmation(
                                    ActionButton.builder(Component.text("§a確定"))
                                        .tooltip(Component.text("クリックで確定"))
                                        .width(100)
                                        .action(
                                            DialogAction.customClick(
                                                { view, _ ->
                                                    val input = view.getText("search") ?: ""
                                                    search = input
                                                    allRenderMenu(e.whoClicked as Player)
                                                }, ClickCallback.Options.builder()
                                                    .uses(1)
                                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                                    .build()
                                            )
                                        ).build(),
                                    ActionButton.create(
                                        Component.text("§cキャンセル"),
                                        Component.text("クリックでキャンセル"),
                                        100,
                                        null
                                    )
                                )
                            )
                    }
                    e.whoClicked.showDialog(dialog)
                }
        )
    }
}