package at.hannibal2.skyhanni.config.features.commands

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class BetterWikiCommandConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Improve the functionality of the /wiki command. " +
            "This is required for all of the below features."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    // TODO Make this method not suck
    @Expose
    @ConfigOption(name = "SkyBlock Guide", desc = "Use SkyHanni's method in the SkyBlock Guide.")
    @ConfigEditorBoolean
    @FeatureToggle
    var sbGuide: Boolean = false

    @Expose
    @ConfigOption(
        name = "Use Fandom Wiki",
        desc = "Use Fandom Wiki (§ehypixel-skyblock.fandom.com§7) instead of " +
            "the Hypixel Wiki (§ewiki.hypixel.net§7) in most wiki-related chat messages."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var useFandom: Boolean = false

    @Expose
    @ConfigOption(
        name = "Auto Open",
        desc = "Directly open the Wiki when running the command instead of having to click a message in chat."
    )
    @ConfigEditorBoolean
    var autoOpenWiki: Boolean = false

    @Expose
    @ConfigOption(
        name = "Open from Menus",
        desc = "Directly open the Wiki from menus instead of having to click a message in chat."
    )
    @ConfigEditorBoolean
    var menuOpenWiki: Boolean = false

    @Expose
    @ConfigOption(
        name = "Wiki Key",
        desc = "Search for an item's wiki page with this keybind. §cFor an optimal experience, do §lNOT §cbind this to a mouse button."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var wikiKeybind: Int = Keyboard.KEY_NONE
}
