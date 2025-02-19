package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose

abstract class ItemTrackerData : TrackerData() {

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinName(item: TrackedItem): String

    // TODO add amount in the string
    abstract fun getCoinDescription(item: TrackedItem): List<String>

    open fun getCustomPricePer(internalName: NeuInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        items.clear()
        resetItems()
    }

    fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
        val item = items.getOrPut(internalName) { TrackedItem() }

        if (!command) {
            item.timesGained++
        }
        item.totalAmount += amount
        item.lastTimeUpdated = SimpleTimeMark.now()
        if (command && item.totalAmount <= 0) {
            items.remove(internalName)
        }
    }

    @Expose
    var items: MutableMap<NeuInternalName, TrackedItem> = HashMap()

    class TrackedItem {

        @Expose
        var timesGained: Long = 0

        @Expose
        var totalAmount: Long = 0

        @Expose
        var hidden = false

        var lastTimeUpdated = SimpleTimeMark.farPast()

        fun copy(
            timesGained: Long = this.timesGained,
            totalAmount: Long = this.totalAmount,
            hidden: Boolean = this.hidden,
            lastTimeUpdated: SimpleTimeMark = this.lastTimeUpdated,
        ): TrackedItem {
            val copy = TrackedItem()
            copy.timesGained = timesGained
            copy.totalAmount = totalAmount
            copy.hidden = hidden
            copy.lastTimeUpdated = lastTimeUpdated
            return copy
        }
    }
}
