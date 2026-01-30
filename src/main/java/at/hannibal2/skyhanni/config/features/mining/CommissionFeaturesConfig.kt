package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CommissionFeaturesConfig {
    @Expose
    @ConfigOption(name = "Redirect Clicks", desc = "Redirect clicks in commissions GUI.")
    @ConfigEditorBoolean
    @FeatureToggle
    var redirectClicks: Boolean = false

    @Expose
    @ConfigOption(name = "New Commission Message", desc = "Send message when new commission is received.")
    @ConfigEditorBoolean
    @FeatureToggle
    var newCommissionMessage: Boolean = false
}
