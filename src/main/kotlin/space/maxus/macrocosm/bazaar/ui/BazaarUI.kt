package space.maxus.macrocosm.bazaar.ui

import net.axay.kspigot.gui.*
import net.axay.kspigot.items.meta
import org.bukkit.Material
import space.maxus.macrocosm.bazaar.Bazaar
import space.maxus.macrocosm.bazaar.BazaarCategory
import space.maxus.macrocosm.bazaar.BazaarCollection
import space.maxus.macrocosm.bazaar.BazaarElement
import space.maxus.macrocosm.chat.Formatting
import space.maxus.macrocosm.chat.isBlankOrEmpty
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.chat.reduceToList
import space.maxus.macrocosm.item.ItemValue
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.registry.Identifier
import space.maxus.macrocosm.slayer.ui.LinearInventorySlots
import space.maxus.macrocosm.text.text
import space.maxus.macrocosm.util.pad
import space.maxus.macrocosm.util.padForward
import space.maxus.macrocosm.util.stripTags
import java.util.*

fun globalBazaarMenu(player: MacrocosmPlayer): GUI<ForInventorySixByNine> = kSpigotGUI(GUIType.SIX_BY_NINE) {
    defaultPage = 0
    title = text("Bazaar")

    for (category in BazaarCategory.values()) {
        page(category.ordinal) {
            placeholder(Slots.All, ItemValue.placeholder(category.outline, ""))
            addNavButtons(player)

            val items =
                category.items.map { Optional.of(it) }.padForward(24, Optional.empty<BazaarCollection>()).toList()

            val cmp = createRectCompound<Optional<BazaarCollection>>(Slots.RowTwoSlotThree, Slots.RowFiveSlotEight,
                iconGenerator = { coll ->
                    if (coll.isEmpty)
                        ItemValue.placeholder(Material.GRAY_STAINED_GLASS_PANE, "")
                    else {
                        val collection = coll.get()
                        val stack = collection.displayItem.clone()
                        stack.meta {
                            displayName(text(collection.displayName).noitalic())

                            val lore = mutableListOf<String>()
                            lore.add("<gray>Elements: <green>${collection.items.size}")
                            lore.add("")
                            lore.add(
                                "<gray>Sell Orders: <green>${
                                    Formatting.withCommas(collection.items.sumOf { id -> Bazaar.table.itemData[id]?.sell?.size ?: 0 }
                                        .toBigDecimal())
                                }"
                            )
                            lore.add(
                                "<gray>Buy Orders: <green>${
                                    Formatting.withCommas(collection.items.sumOf { id -> Bazaar.table.itemData[id]?.buy?.size ?: 0 }
                                        .toBigDecimal())
                                }"
                            )

                            lore(lore.map { text("<gray>$it").noitalic() })
                        }
                        stack
                    }
                },
                onClick = { e, coll ->
                    e.bukkitEvent.isCancelled = true
                    if (coll.isPresent) {
                        val collection = coll.get()
                        e.player.openGUI(specificCollectionMenu(player, collection))
                    }
                }
            )
            cmp.addContent(items)
            cmp.sortContentBy { coll ->
                if (coll.isEmpty) 1 else 0
            }
        }
    }
}

private fun specificCollectionMenu(player: MacrocosmPlayer, collection: BazaarCollection) =
    kSpigotGUI(GUIType.FOUR_BY_NINE) {
        defaultPage = 0
        title = text(collection.displayName.stripTags())

        page(0) {
            placeholder(Slots.All, ItemValue.placeholder(Material.GRAY_STAINED_GLASS_PANE, ""))

            val cmp = createRectCompound<Identifier>(Slots.RowThreeSlotTwo, Slots.RowThreeSlotEight,
                iconGenerator = { id ->
                    if (id.isNull())
                        ItemValue.placeholder(Material.GRAY_STAINED_GLASS_PANE, "")
                    else {
                        val stack = BazaarElement.idToElement(id)!!.build(player)!!
                        val data = Bazaar.table.itemData[id]!!
                        stack.meta {
                            val lore = mutableListOf(
                                "<gray>Total Items: <green>${
                                    Formatting.withCommas(data.sell.sumOf { order -> order.qty }.toBigDecimal())
                                }",
                                "<gray>Buy Orders: <green>${Formatting.withCommas(data.buy.size.toBigDecimal())}",
                                "<gray>Sell Orders: <green>${Formatting.withCommas(data.sell.size.toBigDecimal())}",
                                "",
                                "<gray>Instant Buy Price: ${
                                    data.nextSellOrder()?.pricePer?.let {
                                        "<gold>${
                                            Formatting.withCommas(
                                                it.toBigDecimal()
                                            )
                                        }"
                                    } ?: "<red>No Sell Orders!"
                                }",
                                "<gray>Instant Sell Price: ${
                                    data.nextBuyOrder()?.pricePer?.let {
                                        "<gold>${
                                            Formatting.withCommas(
                                                it.toBigDecimal()
                                            )
                                        }"
                                    } ?: "<red>No Buy Orders!"
                                }"
                            )
                            lore(lore.map { text(it).noitalic() })
                        }
                        stack
                    }
                },
                onClick = { e, id ->
                    e.bukkitEvent.isCancelled = true
                    if (id.isNotNull()) {
                        e.player.openGUI(openSpecificItemManagementMenu(player, id))
                    }
                }
            )

            val inserted = collection.items.pad(7, Identifier.NULL).padForward(7, Identifier.NULL)

            cmp.addContent(inserted)

            button(
                Slots.RowOneSlotFive,
                ItemValue.placeholderDescripted(Material.ARROW, "<green>Go Back", "<dark_gray>To Bazaar")
            ) { e ->
                e.bukkitEvent.isCancelled = true
                e.player.openGUI(globalBazaarMenu(player))
            }
        }
    }

private fun GUIPageBuilder<ForInventorySixByNine>.addNavButtons(player: MacrocosmPlayer) {
    // category navigation buttons
    for (category in BazaarCategory.values()) {
        val row = 6 - category.ordinal
        val slot = InventorySlot(row, 1)
        pageChanger(
            LinearInventorySlots(listOf(slot)),
            ItemValue.placeholderDescripted(
                category.displayItem,
                category.displayName,
                *category.description.reduceToList(21).filter { !it.isBlankOrEmpty() }.toTypedArray()
            ),
            category.ordinal,
            null
        ) { e ->
            e.bukkitEvent.isCancelled = true
        }
    }

    // manage orders button
    button(
        Slots.RowOneSlotTwo,
        ItemValue.placeholderDescripted(Material.BOOK, "<green>Manage Orders", "Manage your current orders")
    ) { e ->
        e.bukkitEvent.isCancelled = true
        e.player.openGUI(manageOrders(player))
    }
}
