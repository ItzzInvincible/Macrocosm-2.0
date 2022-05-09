package space.maxus.macrocosm.entity

import net.axay.kspigot.extensions.bukkit.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.nbt.CompoundTag
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot
import space.maxus.macrocosm.Macrocosm
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.damage.healthColor
import space.maxus.macrocosm.damage.truncateEntityHealth
import space.maxus.macrocosm.entity.loot.LootPool
import space.maxus.macrocosm.item.MACROCOSM_TAG
import space.maxus.macrocosm.item.MacrocosmItem
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.stats.SpecialStatistics
import space.maxus.macrocosm.stats.Statistics
import space.maxus.macrocosm.text.comp
import space.maxus.macrocosm.util.Identifier
import space.maxus.macrocosm.util.getId
import space.maxus.macrocosm.util.putId
import kotlin.math.max
import kotlin.math.roundToInt

fun levelFromStats(stats: Statistics, extraWeight: Float = 0f): Int {
    val weight =
        (stats.damage + stats.strength + stats.critDamage + stats.defense + stats.health) + stats.critChance + extraWeight
    return max((weight / 100f).roundToInt(), 1)
}

interface MacrocosmEntity {
    var mainHand: MacrocosmItem?
    var offHand: MacrocosmItem?
    var helmet: MacrocosmItem?
    var chestplate: MacrocosmItem?
    var leggings: MacrocosmItem?
    var boots: MacrocosmItem?

    var currentHealth: Float

    val name: Component
    val type: EntityType
    var baseStats: Statistics
    var baseSpecials: SpecialStatistics

    val level get() = levelFromStats(calculateStats(), extraWeight())

    fun lootPool(player: MacrocosmPlayer?): LootPool

    fun extraWeight(): Float {
        return 0f
    }

    fun addExtraNbt(nbt: CompoundTag) {

    }

    fun preModify(entity: LivingEntity) {

    }

    fun getId(entity: LivingEntity): Identifier {
        val nbt = entity.readNbt()
        return if (nbt.contains(MACROCOSM_TAG))
            nbt.getCompound(MACROCOSM_TAG).getId("ID")
        else
            EntityRegistry.nameOf(this) ?: Identifier.NULL
    }

    fun calculateStats(): Statistics {
        val base = baseStats.clone()
        val specials = specialStats()
        base.multiply(1 + specials.statBoost)
        base.increase(mainHand?.stats())
        base.increase(offHand?.stats())
        base.increase(helmet?.stats())
        base.increase(chestplate?.stats())
        base.increase(leggings?.stats())
        base.increase(boots?.stats())
        return base
    }

    fun specialStats(): SpecialStatistics {
        val base = baseSpecials.clone()
        base.increase(mainHand?.specialStats() ?: SpecialStatistics())
        base.increase(offHand?.specialStats() ?: SpecialStatistics())
        base.increase(helmet?.specialStats() ?: SpecialStatistics())
        base.increase(chestplate?.specialStats() ?: SpecialStatistics())
        base.increase(leggings?.specialStats() ?: SpecialStatistics())
        base.increase(boots?.specialStats() ?: SpecialStatistics())
        return base
    }

    fun buildName(): Component {
        val stats = calculateStats()
        val curHealth = truncateEntityHealth(currentHealth)
        val fullHealth = truncateEntityHealth(stats.health)
        val level = levelFromStats(stats, extraWeight())
        return comp("<dark_gray>[<gray>Lv $level<dark_gray>] ").append(name.colorIfAbsent(NamedTextColor.RED))
            .append(" ".toComponent())
            .append(curHealth.toComponent().color(healthColor(currentHealth, stats.health)))
            .append(comp("<white>/<green>$fullHealth <red> ❤"))
            .noitalic()
    }

    fun damage(amount: Float, damager: Entity? = null)
    fun kill(damager: Entity? = null)

    fun loadChanges(entity: LivingEntity) {
        if (entity.type != type)
            return

        preModify(entity)

        // equipment
        entity.equipment?.setItemInMainHand(this.mainHand?.build(), true)
        entity.equipment?.setItemInOffHand(this.offHand?.build(), true)
        entity.equipment?.helmet = this.helmet?.build()
        entity.equipment?.chestplate = this.chestplate?.build()
        entity.equipment?.leggings = this.leggings?.build()
        entity.equipment?.boots = this.boots?.build()

        EquipmentSlot.values().forEach {
            try {
                entity.equipment?.setDropChance(it, 0.0f)
            } catch (e: java.lang.IllegalArgumentException) {
                // pass
            }
        }
        entity.isCustomNameVisible = true
        // name
        entity.customName(buildName())

        // actual data
        val nbt = entity.readNbt()

        val stats = calculateStats()

        val tag = CompoundTag()
        tag.put("Stats", stats.compound())
        tag.putFloat("CurrentHealth", currentHealth)
        tag.putId("ID", getId(entity))

        addExtraNbt(tag)

        nbt.put(MACROCOSM_TAG, tag)
        entity.loadNbt(nbt)
    }
}

fun Entity.loadNbt(nbt: CompoundTag) {
    this.persistentDataContainer.set(NamespacedKey(Macrocosm, "NBT_WRAPPER"), CompoundDataType, nbt)
}

fun Entity.readNbt(): CompoundTag {
    return this.persistentDataContainer.get(NamespacedKey(Macrocosm, "NBT_WRAPPER"), CompoundDataType) ?: CompoundTag()
}
