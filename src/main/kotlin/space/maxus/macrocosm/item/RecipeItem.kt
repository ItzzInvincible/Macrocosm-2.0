package space.maxus.macrocosm.item

import com.destroystokyo.paper.profile.ProfileProperty
import net.axay.kspigot.extensions.bukkit.toComponent
import net.axay.kspigot.extensions.bukkit.toLegacyString
import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import space.maxus.macrocosm.ability.MacrocosmAbility
import space.maxus.macrocosm.chat.isBlankOrEmpty
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.chat.reduceToList
import space.maxus.macrocosm.cosmetic.Dye
import space.maxus.macrocosm.cosmetic.SkullSkin
import space.maxus.macrocosm.enchants.Enchantment
import space.maxus.macrocosm.item.buffs.MinorItemBuff
import space.maxus.macrocosm.item.runes.ApplicableRune
import space.maxus.macrocosm.item.runes.RuneState
import space.maxus.macrocosm.reforge.Reforge
import space.maxus.macrocosm.registry.Identifier
import space.maxus.macrocosm.stats.SpecialStatistics
import space.maxus.macrocosm.stats.Statistics
import space.maxus.macrocosm.text.comp
import space.maxus.macrocosm.util.putId
import space.maxus.macrocosm.util.stripTags
import java.util.*

class RecipeItem(
    override val base: Material,
    override var rarity: Rarity,
    private val baseName: String,
    private val headSkin: String? = null,
    private val description: String? = null,
    private val glow: Boolean = false
) : MacrocosmItem {
    override var stats: Statistics = Statistics.zero()
    override var specialStats: SpecialStatistics = SpecialStatistics()
    override var amount: Int = 1
    override var stars: Int = 0
    override val id: Identifier = Identifier.macro(baseName.stripTags().lowercase().replace(" ", "_"))
    override val type: ItemType = ItemType.OTHER
    override var name: Component = comp(baseName)
    override var rarityUpgraded: Boolean = false
    override var reforge: Reforge? = null
    override val abilities: MutableList<MacrocosmAbility> = mutableListOf()
    override val enchantments: HashMap<Enchantment, Int> = hashMapOf()
    override val runes: HashMap<ApplicableRune, RuneState> = HashMap()
    override val buffs: HashMap<MinorItemBuff, Int> = hashMapOf()
    override var breakingPower: Int = 0
    override var dye: Dye? = null
    override var skin: SkullSkin? = null

    override fun buildLore(lore: MutableList<Component>) {
        if(description != null) {
            val reduced = description.reduceToList(30).map { comp("<dark_gray>$it").noitalic() }.toMutableList()
            reduced.removeIf { it.toLegacyString().isBlankOrEmpty() }
            lore.addAll(reduced)
            lore.add("".toComponent())
        }
        lore.add(comp("<yellow>Right click to view recipes").noitalic())
    }

    override fun addExtraNbt(cmp: CompoundTag) {
        cmp.putByte("BlockClicks", 0)
        cmp.putId("ViewRecipes", this.id)
    }

    override fun addExtraMeta(meta: ItemMeta) {
        if(glow)
            meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 1, true)
        if(base == Material.PLAYER_HEAD && headSkin != null) {
            val skull = meta as SkullMeta
            val profile = Bukkit.createProfile(UUID.randomUUID())
            profile.setProperty(ProfileProperty("textures", headSkin))
            skull.playerProfile = profile
        }
    }

    override fun clone(): MacrocosmItem {
        return RecipeItem(base, rarity, baseName, headSkin)
    }
}
