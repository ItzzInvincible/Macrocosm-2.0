package space.maxus.macrocosm.damage

import space.maxus.macrocosm.stats.Statistic
import space.maxus.macrocosm.stats.Statistics
import kotlin.random.Random

object DamageCalculator {
    private fun crits(stats: Statistics) = Random.nextFloat() < (stats.critChance / 100f)

    fun calculateMagicDamage(base: Int, scaling: Float, stats: Statistics): Float {
        return base * (1 + (stats.abilityDamage) / 100f) * (1 + (stats.intelligence / 100f) * scaling) * (1 + (stats.damageBoost / 100f))
    }

    fun calculateStandardDealt(amount: Float, stats: Statistics): Pair<Float, Boolean> {
        val strengthMul = (1 + (stats.strength / 100f))
        val extraMul = (1 + (stats.damageBoost / 100f))
        val crit = crits(stats)
        val critMultiplier = 1 + if (crit) (stats.critDamage / 100f) else 0f
        val trueMultiplier = 1 + (stats.trueDamage / (stats.trueDamage + 100f))
        return Pair(amount * strengthMul * critMultiplier * extraMul * trueMultiplier, crit)
    }

    fun calculateStandardReceived(amount: Float, stats: Statistics): Float {
        val reduction = clamp(
            stats.damageReduction + (stats.defense / (100 + stats.defense)) + (stats.trueDefense / (stats.trueDefense * 10 + 1000)),
            0f,
            0.80f
        )
        return amount * (1 - reduction)
    }

    @Suppress("unused")
    fun calculateTrueReceived(amount: Float, stats: Statistics): Float {
        val reduction = clamp(stats.damageReduction + (stats.trueDefense / (100 + stats.trueDefense)), 0.01f, 0.95f)
        return amount * (1 - reduction)
    }

    fun calculateLightEffectiveHealth(health: Float, stats: Statistics): Float {
        return health * (1 + (stats.defense / (100f + stats.defense)))
    }

    fun calculateEffectiveHealth(stats: Statistics): Float {
        return stats[Statistic.HEALTH] * (1 + (stats[Statistic.DEFENSE] / 100f))
    }

    fun calculateDamageReduction(stats: Statistics): Float {
        return stats[Statistic.DEFENSE] / (stats[Statistic.DEFENSE] + 100f)
    }
}
