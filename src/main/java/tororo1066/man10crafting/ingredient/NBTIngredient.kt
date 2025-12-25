package tororo1066.man10crafting.ingredient

import de.tr7zw.nbtapi.NBT
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import tororo1066.man10crafting.Man10Crafting
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.defaultMenus.LargeSInventory
import tororo1066.tororopluginapi.defaultMenus.SingleItemInventory
import tororo1066.tororopluginapi.sInventory.SInventory
import tororo1066.tororopluginapi.sInventory.SInventoryItem
import tororo1066.tororopluginapi.sItem.SItem
import java.util.function.Consumer

class NBTIngredient: MaterialIngredient() {

    var requiredNBT: String? = null
    override val displayItems: ArrayList<ItemStack> = ArrayList()

    override fun getAmount(itemStack: ItemStack): Int? {
        val materialAmount = super.getAmount(itemStack) ?: return null
        if (!matchNBT(itemStack)) return null
        return materialAmount
    }

    override fun isSimilar(itemStack: ItemStack): Boolean {
        if (!super.isSimilar(itemStack)) return false
        if (!matchNBT(itemStack)) return false
        return true
    }

    private fun matchNBT(itemStack: ItemStack): Boolean {
        requiredNBT?.let { required ->
            try {
                val nbt = NBT.itemStackToNBT(itemStack)
                val requiredNBT = NBT.parseNBT(required)
                if (requiredNBT.extractDifference(nbt) != emptyNBT) return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    override fun editItem(
        inventory: SInventory,
        index: Int,
        onSelectNewIngredient: (player: Player, newIngredient: AbstractIngredient?) -> Unit
    ): SInventoryItem {
        val item = super.editItem(inventory, index, onSelectNewIngredient)
            .setDisplayName("§aNBTIngredient")
            .setLore(
                "§7アイテムの種類とNBTで素材かどうか判定します",
                "§7タグを用いることも可能です",
                "§7必要なNBT: ${if (requiredNBT == null) "§cなし" else "§aあり"}",
                "",
                "§e左クリックで編集",
                "§e右クリックで素材の種類を変更する",
                "§eシフト右クリックでNBTを編集"
            )
            .setClickEvent { e ->
                if (e.click != ClickType.SHIFT_RIGHT) return@setClickEvent
                val player = e.whoClicked as Player
                val inv = object : SInventory(SJavaPlugin.plugin, "${Man10Crafting.prefix}§b必要なNBTを編集", 3) {
                    init {
                        setOnClick {
                            it.isCancelled = true
                        }
                    }

                    override fun renderMenu(p: Player): Boolean {
                        setItem(
                            11,
                            SInventoryItem(Material.DIAMOND_BLOCK)
                                .setDisplayName("§a表示上のアイテムを設定する")
                                .setCanClick(false)
                                .setClickEvent {
                                    val inv =
                                        object : LargeSInventory("${Man10Crafting.prefix}§b表示上のアイテムを選択") {
                                            init {
                                                setOnClick {
                                                    it.isCancelled = true
                                                }
                                            }

                                            override fun renderMenu(p: Player): Boolean {
                                                val items = ArrayList<SInventoryItem>()
                                                displayItems.forEach { item ->
                                                    items.add(
                                                        SInventoryItem(item)
                                                            .addLore("", "§cシフト右クリックで削除")
                                                            .setCanClick(false)
                                                            .setClickEvent { e2 ->
                                                                if (e2.click == ClickType.SHIFT_RIGHT) {
                                                                    displayItems.remove(item)
                                                                    allRenderMenu(player)
                                                                }
                                                            }
                                                    )
                                                }

                                                items.add(
                                                    SInventoryItem(Material.EMERALD_BLOCK)
                                                        .setDisplayName("§a追加")
                                                        .setCanClick(false)
                                                        .setClickEvent {
                                                            val inv = SingleItemInventory(
                                                                SJavaPlugin.plugin,
                                                                "${Man10Crafting.prefix}§a追加するアイテム"
                                                            )
                                                            inv.onConfirm = Consumer { itemStack ->
                                                                displayItems.add(itemStack)
                                                                player.closeInventory()
                                                            }
                                                            moveChildInventory(inv, p)
                                                        }
                                                )

                                                setResourceItems(items)
                                                return true
                                            }
                                        }

                                    moveChildInventory(inv, p)
                                }
                        )

                        setItem(
                            15,
                            createNullableInputItem(
                                SItem(Material.WRITABLE_BOOK)
                                    .setDisplayName("§a必要なNBTを設定する")
                                    .addLore(
                                        "§b指定したNBTが含まれていた場合素材として扱います",
                                        "§eルートからのNBT形式で入力してください",
                                        "§e例: {components:{\"minecraft:custom_data\":{my_plugin:{my_key:1}}}}",
                                        "§d現在の値: ${requiredNBT ?: "§cなし"}"
                                    ), String::class.java, "§b必要なNBTを入力してください"
                            ) { str, _ ->
                                requiredNBT = str
                            }
                        )

                        return true
                    }
                }

                inventory.moveChildInventory(inv, player)
            }

        return item
    }

    override fun serialize(): Map<String?, Any?> {
        val map = super.serialize().toMutableMap()
        requiredNBT?.let {
            map["requiredNBT"] = it
        }
        map["displayItems"] = displayItems
        return map
    }

    companion object {
        private val emptyNBT = NBT.createNBTObject()

        @JvmStatic
        @Suppress("unused", "UNCHECKED_CAST")
        fun deserialize(map: Map<String?, Any?>): NBTIngredient? {
            if (!Man10Crafting.enabledNBTAPI) return null
            val materialIngredient = MaterialIngredient.deserialize(map) ?: return null
            val nbtIngredient = NBTIngredient()
            nbtIngredient.materials.putAll(materialIngredient.materials)
            nbtIngredient.tags.putAll(materialIngredient.tags)

            nbtIngredient.requiredNBT = map["requiredNBT"] as String?
            val displayItems = map["displayItems"] as? List<ItemStack> ?: return null
            nbtIngredient.displayItems.addAll(displayItems)

            return nbtIngredient
        }
    }
}