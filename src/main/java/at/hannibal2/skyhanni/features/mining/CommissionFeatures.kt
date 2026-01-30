package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld

@SkyHanniModule
object CommissionFeatures {

    private val config get() = SkyHanniMod.feature.mining.commissionFeatures
    private val inCommissionsGui get() = InventoryUtils.openInventoryName() == "Commissions"

    private var lastClaimedCommission: Int? = null

    @HandleEvent
    fun onMouseInput(event: GuiMouseInputEvent) {
        if (handlePress()) event.cancel()
    }

    private fun handlePress(): Boolean {
        if (!config.redirectClicks || !inCommissionsGui || !KeyboardManager.LEFT_MOUSE.isKeyHeld()) return false

        for (slot in listOf(11, 12, 14, 15)) {
            val isCommissionCompleted = InventoryUtils.getItemsInOpenChest()[slot]
                .item
                .getLore()
                .any { it.contains("COMPLETED") }
            if (isCommissionCompleted) {
                lastClaimedCommission = slot
                InventoryUtils.clickSlot(slot)
                return true
            }
        }

        return false
    }

    @HandleEvent
    fun onTick() {
        if (!config.newCommissionMessage || !inCommissionsGui || lastClaimedCommission == null) return

        val item = InventoryUtils.getItemsInOpenChestWithNull().getOrNull(lastClaimedCommission!!)?.item
            ?: return
        val lore = item.getLore()

        if (lore.any { it.contains("COMPLETED") } || !item.displayName.string.contains("NEW")) return

        ChatUtils.chat("New commission: ${lore[5]}")

        lastClaimedCommission = null
    }
}
