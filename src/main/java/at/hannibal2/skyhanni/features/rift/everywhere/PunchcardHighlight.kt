package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.EntityLivingBase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PunchcardHighlight {
    private val config get() = SkyHanniMod.feature.rift.punchcard
    private var lastRiftServer: String = ""

    private var listening = false

    private val patternGroup = RepoPattern.group("rift.punchcard")

    /**
     * REGEX-TEST: §5§lPUNCHCARD! §r§eYou punched §r§b[MVP§r§c+§r§b] ThorQOM§r§f §r§eand both regained §r§a+25ф Rift Time§r§e!
     * REGEX-TEST: §5§lPUNCHCARD! §r§eYou punched §r§7Metafighter§r§7 §r§eand both regained §r§a+25ф Rift Time§r§e!
     * REGEX-TEST: §5§lPUNCHCARD! §r§eYou punched §r§a[VIP] RickyLafleur22§r§f §r§eand both regained §r§a+25ф Rift Time§r§e!
     */
    private val punchedPattern by patternGroup.pattern(
        "new",
        "§5§lPUNCHCARD! §r§eYou punched §r§.(?:.*?)?(?<name>\\w+)§r§. §r§eand both regained §r§a\\+25ф Rift Time§r§e!",
    )

    /**
     * REGEX-TEST: §c§lAWKWARD! §r§cThis player has already been punched by you... somehow!
     */
    private val repeatPattern by patternGroup.pattern(
        "repeat",
        "§c§lAWKWARD! §r§cThis player has already been punched by you\\.\\.\\. somehow!",
    )

    /**
     * REGEX-TEST: §c§lUH OH! §r§cYou reached the limit of 20 players you can punch in one session!
     */
    private val limitPattern by patternGroup.pattern(
        "limit",
        "§c§lUH OH! §r§cYou reached the limit of 20 players you can punch in one session!",
    )

    private val playerList: MutableSet<String> = mutableSetOf()
    private val playerQueue = mutableListOf<String>()

    private val PUNCHCARD_ARTIFACT = "PUNCHCARD_ARTIFACT".toInternalName()
    private val displayIcon by lazy { PUNCHCARD_ARTIFACT.getItemStack() }
    private var display: Renderable = Renderable.string("hello")

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPlayerSpawn(event: MobEvent.Spawn.Player) {
        if (!config.highlight.get()) return
        if (config.reverse.get()) return
        val size = playerList.size
        if (size >= 20) return
        val entity = event.mob
        if (!playerList.contains(entity.name)) {
            colorPlayer(entity.baseEntity)
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(
            config.highlight,
            config.color,
            config.reverse,
        ) {
            reloadColors()
        }
        ConditionalUtils.onToggle(
            config.gui,
            config.compact,
            config.reverseGUI,
        ) {
            display = drawDisplay()
        }
        ConditionalUtils.onToggle(
            config.highlight,
            config.color,
        ) {
            checkPunchcard()
        }
    }

    private var warningCooldown = SimpleTimeMark.farPast()

    private fun checkPunchcard() {
        if (!RiftApi.inRift()) return

        val hasPunchcard = InventoryUtils.isItemInInventory(PUNCHCARD_ARTIFACT)
        if (!hasPunchcard && warningCooldown.passedSince() > 30.seconds) {
            warningCooldown = SimpleTimeMark.now()
            ChatUtils.chat("You don't seem to own a Punchcard Artifact, this feature will not work without one.")
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        DelayedRun.runDelayed(1500.milliseconds) {
            if (playerList.isEmpty()) return@runDelayed
            if (event.newIsland != IslandType.THE_RIFT) return@runDelayed

            if (HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                reloadColors()
                lastRiftServer = HypixelData.server
                playerList.clear()
            }
            display = drawDisplay()
        }
    }

    private fun colorPlayer(entity: EntityLivingBase) {
        val color = config.color.get().toSpecialColor()
        val alpha = when (color.alpha) {
            0 -> 0
            255 -> 1
            else -> 255 - color.alpha
        }
        RenderLivingEntityHelper.setEntityColor(entity, color.addAlpha(alpha)) { IslandType.THE_RIFT.isInIsland() }
    }

    private fun removePlayerColor(entity: EntityLivingBase) {
        RenderLivingEntityHelper.removeEntityColor(entity)
    }

    fun onResetCommand() {
        playerList.clear()
        playerQueue.clear()
        if (config.reverse.get()) {
            MobData.players.forEach {
                colorPlayer(it.baseEntity)
            }
        } else {
            MobData.players.forEach {
                removePlayerColor(it.baseEntity)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPunch(event: EntityClickEvent) {
        val entity = event.clickedEntity
        if (entity !is AbstractClientPlayer) return
        if (entity.isNpc()) return
        val name = entity.name
        if (name in playerList || name in playerQueue) return
        playerQueue.add(name)
        listening = true
        DelayedRun.runDelayed(1.seconds) {
            if (name in playerQueue) playerQueue.remove(name)
            listening = false
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onChat(event: SkyHanniChatEvent) {
        if (!listening) return
        if (playerQueue.isEmpty()) return
        val message = event.message
        val queuedName = playerQueue[0]
        punchedPattern.matchMatcher(message) {
            val name = group("name")
            if (queuedName == name) {
                addPunch(name)
            } else ErrorManager.logErrorStateWithData(
                "Error finding punched player", "queuedName and capturedName were different",
                "queuedName" to queuedName,
                "capturedName" to name,
                noStackTrace = true,
                betaOnly = true,
            )
            return
        }
        if (limitPattern.matches(message) || repeatPattern.matches(message)) addPunch(queuedName)
    }

    private fun addPunch(playerName: String) {
        playerList.add(playerName)
        playerQueue.remove(playerName)
        val player = MobData.players.firstOrNull { it.name == playerName } ?: return
        if (!config.reverse.get()) removePlayerColor(player.baseEntity)
        else colorPlayer(player.baseEntity)
        display = drawDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.gui.get()) return

        config.position.renderRenderable(display, "Punchcard Overlay")
    }

    private fun drawDisplay(): Renderable {
        var string = ""
        if (!config.compact.get()) string += "Punchcard Artifact: "
        string += "§d" + if (!config.reverseGUI.get()) playerList.size
        else 20 - playerList.size

        return Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(displayIcon),
                Renderable.string(string),
            ),
            spacing = 1,
        )
    }

    private fun reloadColors() {
        MobData.players.forEach {
            removePlayerColor(it.baseEntity)
        }
        if (!config.highlight.get()) return
        val reverse = config.reverse.get()
        for (player in MobData.players.filter { (reverse && it.name in playerList) || (!reverse && it.name !in playerList) }) {
            colorPlayer(player.baseEntity)
        }
    }
}
