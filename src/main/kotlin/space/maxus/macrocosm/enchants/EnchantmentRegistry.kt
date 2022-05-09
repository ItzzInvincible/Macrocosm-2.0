package space.maxus.macrocosm.enchants

import org.bukkit.Bukkit
import space.maxus.macrocosm.Macrocosm
import space.maxus.macrocosm.util.Identifier

@Suppress("unused")
object EnchantmentRegistry {
    val enchants: HashMap<Identifier, Enchantment> = hashMapOf()

    fun register(name: Identifier, ench: Enchantment): Enchantment {
        if (enchants.containsKey(name)) {
            return ench
        }
        enchants[name] = ench
        Bukkit.getServer().pluginManager.registerEvents(ench, Macrocosm)
        return ench
    }

    fun find(name: Identifier) = enchants[name]

    fun nameOf(ability: Enchantment) = enchants.filter { (_, v) -> v == ability }.map { (k, _) -> k }.firstOrNull()
}

