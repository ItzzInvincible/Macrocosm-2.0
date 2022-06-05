package space.maxus.macrocosm.slayer

import net.axay.kspigot.extensions.pluginKey
import net.axay.kspigot.runnables.task
import net.axay.kspigot.sound.sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import space.maxus.macrocosm.chat.Formatting
import space.maxus.macrocosm.chat.capitalized
import space.maxus.macrocosm.entity.macrocosm
import space.maxus.macrocosm.events.PlayerDeathEvent
import space.maxus.macrocosm.events.PlayerKillEntityEvent
import space.maxus.macrocosm.players.macrocosm
import space.maxus.macrocosm.text.comp
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

object SlayerHandlers: Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onKill(e: PlayerKillEntityEvent) {
        val quest = e.player.slayerQuest ?: return
        val type = e.killed.type

        if(!quest.type.slayer.validEntities.contains(type))
            return
        val newExp = quest.collectedExp + e.experience
        if(newExp >= quest.type.slayer.requiredExp[quest.tier - 1] && quest.status == SlayerStatus.COLLECT_EXPERIENCE) {
            // spawning boss and changing status to kill the boss
            val newQuest = SlayerQuest(quest.type, quest.tier, newExp.toFloat(), SlayerStatus.SLAY_BOSS)
            e.player.updateSlayerQuest(newQuest)
            newQuest.summonBoss(e.player.paper!!)
            return
        } else if(quest.status == SlayerStatus.COLLECT_EXPERIENCE) {
            // otherwise giving combat experience and updating display
            val newQuest = SlayerQuest(quest.type, quest.tier, newExp.toFloat(), quest.status)
            e.player.updateSlayerQuest(newQuest)
        }
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        if(e.player.summonedBoss == null)
            return
        Bukkit.getEntity(e.player.summonedBoss!!)?.remove()

        e.player.sendMessage("<red><bold>SLAYER QUEST FAILED! YOU DIED!")
        e.player.sendMessage("<gray>Good luck next time!")

        val quest = e.player.slayerQuest!!
        e.player.updateSlayerQuest(SlayerQuest(quest.type, quest.tier, quest.collectedExp, SlayerStatus.FAIL))
    }

    @EventHandler
    fun onKillBoss(e: PlayerKillEntityEvent) {
        val nbt = e.killed.persistentDataContainer
        if(!nbt.has(pluginKey("SUMMONER")))
            return
        val spawnedPlayer = UUID.fromString(nbt.get(pluginKey("SUMMONER"), PersistentDataType.STRING))
        val player = Bukkit.getPlayer(spawnedPlayer)?.macrocosm ?: return
        val quest = player.slayerQuest ?: return
        if(quest.status != SlayerStatus.SLAY_BOSS)
            return

        val mc = e.killed.macrocosm!!
        val id = mc.getId(e.killed)
        if(id.path != "${quest.type.slayer.id}_${quest.tier}")
            return

        // player has killed the boss, update status and give experience
        val newQuest = SlayerQuest(quest.type, quest.tier, quest.collectedExp, SlayerStatus.SUCCESS)
        player.updateSlayerQuest(newQuest)
        player.summonedBoss = null

        player.sendMessage("<gold><bold>NICE! SLAYER BOSS SLAIN!")
        val currentLevel = player.slayerExperience[quest.type]!!
        val newExp = currentLevel.overflow + rewardExperienceForTier(quest.tier)
        val rngForTier = rngMeterForTier(quest.tier)
        var rng = currentLevel.rngMeter + rngForTier
        if(rng > .0) {
            if(rng > 1.0) {
                val pool = e.killed.macrocosm!!.lootPool(e.player)
                val rngesus = pool.drops.filter { it.rarity.rarity >= 4 }.random().clone()
                val item = rngesus.dropRngesusReward(e.player)
                rng = .0
                e.player.paper!!.world.dropItemNaturally(e.player.paper!!.location, item)
            }
            val tiles = min(ceil(rng * 20).roundToInt(), 15)
            player.sendMessage("<light_purple>RNG Meter: <gradient:dark_purple:light_purple><bold>" + "-".repeat(tiles) + "</gradient>" + "-".repeat(15 - tiles))
        }
        if(SlayerTable.shouldLevelUp(currentLevel.level, newExp, .0)) {
            player.slayerExperience[quest.type] = SlayerLevel(currentLevel.level + 1, .0, rng)
            player.sendMessage("<green><bold>LVL UP! <dark_purple>▶ </bold><yellow>${quest.type.name.replace("_", " ").capitalized()} LVL ${currentLevel.level + 1}!")
            player.sendMessage("<green><bold>REWARDS AVAILABLE")
            val rewards = quest.type.slayer.rewards[currentLevel.level]
            for(reward in rewards) {
                player.paper!!.sendMessage(Component.text("+").color(NamedTextColor.DARK_GRAY).append(reward.display(currentLevel.level)))
            }
            // todo: open slayer menu here
            player.paper!!.sendMessage(comp("<gold><bold>CLICK TO COLLECT")
                .hoverEvent(
                    HoverEvent
                        .showText(comp("<gold>Rewards for ${quest.type.slayer.name}<gold> LVL ${currentLevel.level + 1}")
                        )
                )
                .clickEvent(ClickEvent.runCommand("/w @s boop!")
                )
            )
        } else {
            player.slayerExperience[quest.type] = SlayerLevel(currentLevel.level, newExp, rng)
            if(currentLevel.level == 9)
                player.sendMessage("<yellow>${quest.type.name.replace("_", " ").capitalized()} LVL 9<dark_purple> - <green><bold>LVL MAXED OUT!")
            else {
                val requiredExp = SlayerTable.expForLevel(currentLevel.level + 1)

                val needed = requiredExp - currentLevel.overflow
                player.sendMessage(
                    "<yellow>${
                        quest.type.name.replace("_", " ").capitalized()
                    } LVL ${currentLevel.level}<dark_purple> - <gray>Next LVL in <light_purple>${
                        Formatting.stats(
                            needed.toBigDecimal(),
                            true
                        )
                    } XP<gray>!"
                )
            }
        }
        for(i in 0..5) {
            task(delay = i * 2L) {
                sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP) {
                    pitch = .5f + (i / 10f)
                    playFor(player.paper!!)
                }
            }
        }
    }
}