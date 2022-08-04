package space.maxus.macrocosm.forge

import com.google.gson.JsonObject
import space.maxus.macrocosm.async.Threading
import space.maxus.macrocosm.pack.PackProvider
import space.maxus.macrocosm.registry.Identifier
import space.maxus.macrocosm.registry.Registry
import space.maxus.macrocosm.util.GSON
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystems
import kotlin.io.path.readText

class ForgeRecipe(val input: HashMap<Identifier, Int>, val result: Pair<Identifier, Int>, val type: ForgeType, val requiredLvl: Int, val length: Long) {
    companion object {
        fun parse(jo: JsonObject): ForgeRecipe {
            val type = ForgeType.valueOf(jo["type"].asString.uppercase())
            val resultAmount = if(jo.has("amount")) jo["amount"].asInt else 1
            val result = Identifier.parse(jo["result"].asString)
            val level = jo["level"].asInt
            val ingredients = jo["ingredients"].asJsonArray.map { ele ->
                if (ele.isJsonObject) {
                    val eJo = ele.asJsonObject
                    Pair(Identifier.parse(eJo["item"].asString), eJo["amount"].asInt)
                } else Pair(Identifier.parse(ele.asString), 1)
            }
            val length = jo["length"].asLong
            return ForgeRecipe(hashMapOf(*ingredients.take(9).toTypedArray()), Pair(result, resultAmount), type, level, length)
        }

        fun initRecipes() {
            Threading.runAsync(name = "ForgeRecipeParser") {
                info("Starting forge recipe parser")
                val pool = Threading.newFixedPool(5)
                val input = this::class.java.classLoader.getResource("data")!!.toURI()
                val fs = try {
                    FileSystems.newFileSystem(input, hashMapOf<String, String>())
                } catch (e: FileSystemAlreadyExistsException) {
                    FileSystems.getFileSystem(input)
                }
                for (file in PackProvider.enumerateEntries(fs.getPath("data", "forge_recipes"))) {
                    info("Parsing forge recipes from ${file.fileName}...")
                    val data = GSON.fromJson(file.readText(), JsonObject::class.java)
                    for ((key, obj) in data.entrySet()) {
                        pool.execute {
                            val id = Identifier.parse(key)
                            Registry.FORGE_RECIPE.register(id, parse(obj.asJsonObject))
                        }
                    }
                }
            }
        }
    }
}
