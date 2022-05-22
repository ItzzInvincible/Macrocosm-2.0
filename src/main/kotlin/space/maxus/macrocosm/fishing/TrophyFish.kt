package space.maxus.macrocosm.fishing

import com.destroystokyo.paper.profile.ProfileProperty
import net.axay.kspigot.extensions.bukkit.toLegacyString
import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import space.maxus.macrocosm.ability.ItemAbility
import space.maxus.macrocosm.chat.isBlankOrEmpty
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.chat.reduceToList
import space.maxus.macrocosm.enchants.Enchantment
import space.maxus.macrocosm.item.ItemType
import space.maxus.macrocosm.item.MacrocosmItem
import space.maxus.macrocosm.item.Rarity
import space.maxus.macrocosm.item.buffs.MinorItemBuff
import space.maxus.macrocosm.item.runes.ApplicableRune
import space.maxus.macrocosm.item.runes.RuneState
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.reforge.Reforge
import space.maxus.macrocosm.registry.Identifier
import space.maxus.macrocosm.stats.SpecialStatistics
import space.maxus.macrocosm.stats.Statistics
import space.maxus.macrocosm.text.comp
import space.maxus.macrocosm.util.Chance
import space.maxus.macrocosm.util.id
import space.maxus.macrocosm.zone.Zone
import java.util.*
import java.util.function.Predicate

data class CatchConditions(
    val description: String,
    val predicate: Predicate<Pair<MacrocosmPlayer, Zone>>,
    val chance: Float
)

class TrophyFish(
    private val baseName: String,
    private val headSkin: String,
    val conditions: CatchConditions,
    override var rarity: Rarity,
    var tier: TrophyTier? = null
) : MacrocosmItem, Chance {
    override var stats: Statistics = Statistics.zero()
    override var specialStats: SpecialStatistics = SpecialStatistics()
    override var amount: Int = 1
    override var stars: Int = 0
    override val maxStars: Int = 0
    override val id: Identifier get() = id(baseName.lowercase())
    override val type: ItemType = ItemType.OTHER
    override var name: Component
        get() = comp("$baseName <bold>${tier?.name}")
        set(@Suppress("UNUSED_PARAMETER") value) {}
    override val base: Material = Material.PLAYER_HEAD
    override var rarityUpgraded: Boolean = false
    override var reforge: Reforge? = null
    override val abilities: MutableList<ItemAbility> = mutableListOf()
    override val enchantments: HashMap<Enchantment, Int> = hashMapOf()
    override val runes: HashMap<ApplicableRune, RuneState> = hashMapOf()
    override val buffs: HashMap<MinorItemBuff, Int> = hashMapOf()
    override var breakingPower: Int = 0

    override fun buildLore(lore: MutableList<Component>) {
        val reduced = conditions.description.reduceToList(25).map { comp("<gray>$it").noitalic() }.toMutableList()
        reduced.removeIf { it.toLegacyString().isBlankOrEmpty() }
        lore.addAll(reduced)
    }

    override fun addPotatoBooks(amount: Int) {

    }

    override fun addRune(gem: ApplicableRune, tier: Int): Boolean {
        return false
    }

    override fun reforge(ref: Reforge) {

    }

    override fun stats(): Statistics {
        return Statistics.zero()
    }

    override fun unlockRune(rune: ApplicableRune): Boolean {
        return false
    }

    override fun specialStats(): SpecialStatistics {
        return SpecialStatistics()
    }

    override fun enchant(enchantment: Enchantment, level: Int): Boolean {
        return false
    }

    override fun addExtraMeta(meta: ItemMeta) {
        val skull = meta as SkullMeta
        val profile = Bukkit.createProfile(UUID.randomUUID())
        profile.setProperty(ProfileProperty("textures", headSkin))
        skull.playerProfile = profile
    }

    override fun addExtraNbt(cmp: CompoundTag) {
        val tier = CompoundTag()
        tier.putString("Display", this.tier!!.name)
        tier.putString("Modifier", this.tier!!.modifier)
        tier.putDouble("Chance", this.tier!!.chance)
        cmp.put("Tier", tier)
    }

    override fun convert(from: ItemStack, nbt: CompoundTag): MacrocosmItem {
        val base = super.convert(from, nbt) as TrophyFish
        val tt = nbt.getCompound("Tier")
        base.tier = TrophyTier(tt.getString("Display"), tt.getString("Modifier"), tt.getDouble("Chance"))
        return base
    }

    override fun clone(): MacrocosmItem {
        return TrophyFish(baseName, headSkin, conditions, rarity)
    }

    override val chance: Double
        get() = conditions.chance.toDouble()
}