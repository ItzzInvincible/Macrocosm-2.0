package space.maxus.macrocosm.commands

import com.mojang.brigadier.arguments.StringArgumentType
import net.axay.kspigot.commands.argument
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import space.maxus.macrocosm.entity.loot.DropRarity
import space.maxus.macrocosm.item.ItemValue
import space.maxus.macrocosm.item.macrocosm
import space.maxus.macrocosm.players.macrocosm

fun testStatsCommand() = command("stats") {
    runs {
        for (comp in player.macrocosm?.calculateStats()?.formatFancy()!!) {
            player.sendMessage(comp)
        }
    }
}

fun testDropCommand() = command("raredrop") {
    runs {
        val item = ItemStack(Material.NETHER_STAR).macrocosm!!
        DropRarity.UNBELIEVABLE.announceEntityDrop(player, item)
        player.location.world.dropItemNaturally(player.location, item.build()!!)
    }
}

fun testEnchantedItem() = command("enchanted_item") {
    argument("type", StringArgumentType.word()) {
        runs {
            val type = Material.valueOf(getArgument("type"))
            player.inventory.addItem(ItemValue.enchanted(type).build()!!)
        }
    }
}
