package space.maxus.macrocosm.reward

import net.kyori.adventure.text.Component
import space.maxus.macrocosm.chat.noitalic
import space.maxus.macrocosm.players.MacrocosmPlayer
import space.maxus.macrocosm.registry.Registry
import space.maxus.macrocosm.text.comp
import space.maxus.macrocosm.text.str
import space.maxus.macrocosm.registry.Identifier

class RecipeReward(val recipe: Identifier, override val isHidden: Boolean = false) : Reward {
    override fun reward(player: MacrocosmPlayer, lvl: Int) {
        player.unlockedRecipes.add(recipe)
    }

    override fun display(lvl: Int): Component {
        val rec = Registry.RECIPE.find(recipe)
        return comp("${rec.resultItem().displayName().str()}<gray> Recipe").noitalic()
    }
}
