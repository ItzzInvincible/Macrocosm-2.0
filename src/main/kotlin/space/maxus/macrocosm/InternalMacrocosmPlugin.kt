package space.maxus.macrocosm

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import net.axay.kspigot.main.KSpigot
import space.maxus.macrocosm.async.Threading
import space.maxus.macrocosm.commands.*
import space.maxus.macrocosm.cosmetic.Cosmetics
import space.maxus.macrocosm.data.DataGenerators
import space.maxus.macrocosm.db.Database
import space.maxus.macrocosm.enchants.Enchant
import space.maxus.macrocosm.entity.EntityValue
import space.maxus.macrocosm.fishing.FishingHandler
import space.maxus.macrocosm.fishing.SeaCreatures
import space.maxus.macrocosm.fishing.TrophyFishes
import space.maxus.macrocosm.generators.generate
import space.maxus.macrocosm.item.Armor
import space.maxus.macrocosm.item.ItemValue
import space.maxus.macrocosm.item.buffs.Buffs
import space.maxus.macrocosm.item.runes.DefaultRune
import space.maxus.macrocosm.listeners.*
import space.maxus.macrocosm.mining.MiningHandler
import space.maxus.macrocosm.pack.PackDescription
import space.maxus.macrocosm.pack.PackProvider
import space.maxus.macrocosm.pets.PetValue
import space.maxus.macrocosm.pets.types.PyroclasticToadPet
import space.maxus.macrocosm.pets.types.WaspPet
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.recipes.RecipeMenu
import space.maxus.macrocosm.recipes.RecipeValue
import space.maxus.macrocosm.reforge.ReforgeType
import space.maxus.macrocosm.registry.Registry
import space.maxus.macrocosm.skills.AlchemyReward
import space.maxus.macrocosm.util.id
import space.maxus.macrocosm.zone.ZoneType
import java.util.*

class InternalMacrocosmPlugin : KSpigot() {
    companion object {
        lateinit var INSTANCE: InternalMacrocosmPlugin; private set
        lateinit var PACKET_MANAGER: ProtocolManager; private set
    }

    val id: String = "macrocosm"
    val onlinePlayers: HashMap<UUID, MacrocosmPlayer> = hashMapOf()
    lateinit var playersLazy: MutableList<UUID>; private set

    override fun load() {
        INSTANCE = this
        Threading.runAsync {
            Database.connect()
            playersLazy = Database.readAllPlayers().toMutableList()
        }
    }

    override fun startup() {
        PACKET_MANAGER = ProtocolLibrary.getProtocolManager()

        DataListener.joinLeave()
        server.pluginManager.registerEvents(ChatHandler, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(AbilityTriggers, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(DamageHandlers, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(EntityHandlers, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(RecipeMenu, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(BlockClickListener, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(PickupListener, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(AlchemyReward, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(MiningHandler, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(DamageHandlers, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(FishingHandler, this@InternalMacrocosmPlugin)
        // server.pluginManager.registerEvents(EquipListener, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(FallingBlockListener, this@InternalMacrocosmPlugin)
        server.pluginManager.registerEvents(PackProvider, this@InternalMacrocosmPlugin)
        protocolManager.addPacketListener(MiningHandler)

        ReforgeType.init()
        ItemValue.init()
        Enchant.init()
        RecipeValue.init()
        Armor.init()
        DefaultRune.init()
        Buffs.init()
        EntityValue.init()
        PetValue.init()
        ZoneType.init()
        Cosmetics.init()

        SeaCreatures.init()
        TrophyFishes.init()

        PyroclasticToadPet.init()
        WaspPet.init()

        playtimeCommand()
        rankCommand()
        statCommand()
        recombobulateCommand()
        reforgeCommand()
        myDamageCommand()
        itemCommand()
        giveCoinsCommand()
        enchantCommand()
        viewRecipeCommand()
        recipesCommand()
        addScrollCommand()
        setStarsCommand()
        setGemsCommand()
        addPotatoBooks()
        unlockGemsCommand()
        givePetItemCommand()
        addPetCommand()
        spawnPetCommand()
        itemsCommand()
        summonCommand()
        giveAdminItemCommand()
        armorCommand()
        cosmeticCommand()

        testStatsCommand()
        testLevelUp()
        testCollUp()
        skillExp()
        collAmount()
        testCraftingTable()

        // registering resource generators
        Registry.RESOURCE_GENERATORS.register(id("pack_manifest"), generate("pack.mcmeta", PackDescription::descript))

        if (dumpTestData) {
            DataGenerators.registries()
        }

        val cfgFile = dataFolder.resolve("config.yml")
        if(!cfgFile.exists()) {
            saveDefaultConfig()
            reloadConfig()
        }

        config.load(cfgFile)

        PackProvider.init()
    }

    private val dumpTestData: Boolean = false

    override fun shutdown() {
        Threading.runAsync {
            for ((id, v) in onlinePlayers) {
                println("Saving data for player $id...")
                v.storeSelf(Database.statement)
            }
        }
    }
}

val protocolManager by lazy { InternalMacrocosmPlugin.PACKET_MANAGER }
val Macrocosm by lazy { InternalMacrocosmPlugin.INSTANCE }
