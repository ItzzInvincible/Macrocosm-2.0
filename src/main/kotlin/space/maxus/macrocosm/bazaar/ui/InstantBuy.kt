package space.maxus.macrocosm.bazaar.ui

import net.axay.kspigot.items.meta
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.ValidatingPrompt
import org.bukkit.inventory.ItemStack
import space.maxus.macrocosm.Macrocosm
import space.maxus.macrocosm.bazaar.Bazaar
import space.maxus.macrocosm.bazaar.BazaarElement
import space.maxus.macrocosm.chat.Formatting
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.item.ItemValue
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.registry.Identifier
import space.maxus.macrocosm.text.str
import space.maxus.macrocosm.text.text
import space.maxus.macrocosm.ui.MacrocosmUI
import space.maxus.macrocosm.ui.UIDimensions
import space.maxus.macrocosm.ui.components.Slot
import space.maxus.macrocosm.ui.dsl.macrocosmUi
import space.maxus.macrocosm.util.emptySlots
import space.maxus.macrocosm.util.stripTags

internal fun buyInstantlyScreen(player: MacrocosmPlayer, item: Identifier): MacrocosmUI = macrocosmUi("bazaar_buy_instant", UIDimensions.FOUR_X_NINE) {
        val element = BazaarElement.idToElement(item)!!
        val elementName = element.name.color(null).str()
        val builtItem = element.build(player)!!
        val p = player.paper!!

        title = "${elementName.stripTags()} ▶ Instant Buy"

        pageLazy {
            background()

            button(
                Slot.RowTwoSlotTwo,
                modifyStackGenerateAmountButtonBuy(
                    player,
                    elementName,
                    "<green>Buy only <yellow>one<green>!",
                    1,
                    item,
                    builtItem.clone()
                )
            ) { e ->
                Bazaar.instantBuy(player, e.paper, item, 1)
            }

            button(
                Slot.RowTwoSlotFour,
                modifyStackGenerateAmountButtonBuy(
                    player,
                    elementName,
                    "<green>Buy a stack!",
                    64,
                    item,
                    builtItem.clone()
                ).apply { amount = 64 }) { e ->
                Bazaar.instantBuy(player, e.paper, item, 64)
            }

            val emptySlots = p.inventory.emptySlots * 64

            button(
                Slot.RowTwoSlotSix,
                modifyStackGenerateAmountButtonBuy(
                    player,
                    elementName,
                    "<green>Fill my inventory!",
                    emptySlots,
                    item,
                    ItemStack(Material.CHEST)
                )
            ) { e ->
                Bazaar.instantBuy(player, e.paper, item, emptySlots)
                e.instance.reload()
            }

            button(
                Slot.RowTwoSlotEight,
                ItemValue.placeholderDescripted(
                    Material.OAK_SIGN,
                    "<green>Custom Amount",
                    "<dark_gray>Buy Order Quantity",
                    "",
                    "Buy up to <green>${
                        Formatting.withCommas(
                            (Bazaar.table.itemData[item]!!.sell
                                .sumOf { it.qty }).toBigDecimal(), true
                        )
                    }x",
                    "",
                    "<yellow>Click to specify!"
                )
            ) { e ->
                val inputCoinsPrompt = object : ValidatingPrompt() {
                    override fun getPromptText(context: ConversationContext): String {
                        return ChatColor.YELLOW.toString() + "Input amount to order:"
                    }

                    override fun isInputValid(context: ConversationContext, input: String): Boolean {
                        return try {
                            Integer.parseInt(input)
                            true
                        } catch (e: NumberFormatException) {
                            false
                        }
                    }

                    override fun acceptValidatedInput(context: ConversationContext, input: String): Prompt? {
                        val amount = Integer.parseInt(input)
                        confirmInstantBuy(player, item, elementName, builtItem, amount).open(e.paper)
                        return Prompt.END_OF_CONVERSATION
                    }
                }
                e.paper.closeInventory()
                val conv = ConversationFactory(Macrocosm).withLocalEcho(false).withFirstPrompt(inputCoinsPrompt)
                    .buildConversation(e.paper)
                conv.begin()
            }

            goBack(
                Slot.RowFourSlotFive,
                { openSpecificItemManagementMenu(player, item) },
            )
        }
    }

private fun confirmInstantBuy(
    player: MacrocosmPlayer,
    item: Identifier,
    elementName: String,
    builtItem: ItemStack,
    amount: Int
): MacrocosmUI = macrocosmUi("bazaar_instant_buy_confirm", UIDimensions.FOUR_X_NINE) {
    title = "Confirm Instant Buy"

    page(0) {
        background()

        button(
            Slot.RowTwoSlotFive,
            modifyStackGenerateAmountButtonBuy(
                player,
                elementName,
                "<green>Custom Amount",
                amount,
                item,
                builtItem.clone()
            )
        ) { _ ->
            Bazaar.instantBuy(player, player.paper!!, item, amount)
        }

        goBack(
            Slot.RowOneSlotFive,
            { buyInstantlyScreen(player, item) }
        )
    }
}

internal fun modifyStackGenerateAmountButtonBuy(
    player: MacrocosmPlayer,
    elementName: String,
    name: String,
    amount: Int,
    item: Identifier,
    stack: ItemStack
): ItemStack {
    if (amount in 1..64) {
        stack.amount = amount
    }
    stack.meta {
        displayName(text(name).noitalic())

        val res = Bazaar.tryDoInstantBuy(player, item, amount, false).get()
        val perUnit = res.coinsSpent / (res.amountBought.let { if (it <= 0) 1 else it }).toBigDecimal()

        val loreCompound = mutableListOf(
            "<dark_gray>$elementName",
            "",
            "<gray>Amount: <green>${Formatting.withCommas(amount.toBigDecimal(), true)}<gray>x",
            ""
        )

        if (res.amountBought == 0) {
            loreCompound.add("<red>Could not find any orders!")
        } else {
            if (amount > 1) {
                loreCompound.add("<gray>Per unit: <gold>${Formatting.withCommas(perUnit)} coins")
            }
            loreCompound.add("<gray>Price: <gold>${Formatting.withCommas(res.coinsSpent)} coins")
            if (res.amountBought < amount) {
                loreCompound.add("<red>Only found <white>${res.amountBought}<red>x for sale!")
            }

            loreCompound.add("")
            if (res.coinsSpent > player.purse) {
                loreCompound.add("<red>You don't have enough Coins!")
            } else {
                loreCompound.add("<yellow>Click to order!")
            }
        }

        lore(loreCompound.map { text(it).noitalic() })
    }
    return stack
}
