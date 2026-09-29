package com.ztftrue.music.sqlData.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ztftrue.music.sqlData.IntArrayConverters
import com.ztftrue.music.utils.Utils

// because Aux make an error
@Entity(tableName = "aux")
data class Auxr(
    // TODO for every track
    @PrimaryKey
    @ColumnInfo val id: Long,
    @ColumnInfo var speed: Float,
    @ColumnInfo var pitch: Float,
    @ColumnInfo var echo: Boolean,
    @ColumnInfo var echoDelay: Float,
    @ColumnInfo var echoDecay: Float,
    @ColumnInfo var echoRevert: Boolean,
    @ColumnInfo var equalizer: Boolean,
    @ColumnInfo(defaultValue = Utils.Q.toString()) var equalizerQ: Float = Utils.Q,
    @field:TypeConverters(IntArrayConverters::class)
    @ColumnInfo var equalizerBand: IntArray,
    @ColumnInfo(defaultValue = "0") var virtualizerEnabled: Boolean = false, // 虚拟环绕开关
    @ColumnInfo(defaultValue = "0") var virtualizerStrength: Int = 0,        // 强度 (0 - 1000)
    // --- Bass Boost ---
    @ColumnInfo(defaultValue = "0") var bassBoostEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0") var bassBoostStrength: Int = 0,
    // --- Equalizer ---
    @ColumnInfo(defaultValue = "0") var equalizerType: Int = 0,
    @ColumnInfo(defaultValue = "'Custom'") var selectedPreset: String = Utils.custom,
    // --- Fine-tuning UI settings ---
    @ColumnInfo(defaultValue = "0") var showPitchFine: Boolean = false,
    @ColumnInfo(defaultValue = "0") var showSpeedFine: Boolean = false,
    // --- Delay Effect ---
    @ColumnInfo(defaultValue = "0") var delayEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0.35") var delayTime: Float = 0.35f,
    @ColumnInfo(defaultValue = "0.4") var delayFeedback: Float = 0.4f,
    @ColumnInfo(defaultValue = "0.4") var delayMix: Float = 0.4f,
    // --- Reverb ---
    @ColumnInfo(defaultValue = "0") var reverbEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0.5") var reverbRoomSize: Float = 0.5f,
    @ColumnInfo(defaultValue = "0.5") var reverbDamping: Float = 0.5f,
    @ColumnInfo(defaultValue = "0.3") var reverbMix: Float = 0.3f,
    // --- Chorus ---
    @ColumnInfo(defaultValue = "0") var chorusEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "1.5") var chorusRate: Float = 1.5f,
    @ColumnInfo(defaultValue = "0.5") var chorusDepth: Float = 0.5f,
    @ColumnInfo(defaultValue = "0.5") var chorusMix: Float = 0.5f,
    // --- Flanger ---
    @ColumnInfo(defaultValue = "0") var flangerEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0.5") var flangerRate: Float = 0.5f,
    @ColumnInfo(defaultValue = "0.7") var flangerDepth: Float = 0.7f,
    @ColumnInfo(defaultValue = "0.5") var flangerFeedback: Float = 0.5f,
    @ColumnInfo(defaultValue = "0.5") var flangerMix: Float = 0.5f,
    // --- Polyphony ---
    @ColumnInfo(defaultValue = "0") var polyphonyEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0") var polyphonySemitones: Int = 0,
    @ColumnInfo(defaultValue = "0.0") var polyphonyDetune: Float = 0.0f,
    @ColumnInfo(defaultValue = "0.5") var polyphonyMix: Float = 0.5f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Auxr

        if (id != other.id) return false
        if (speed != other.speed) return false
        if (pitch != other.pitch) return false
        if (echo != other.echo) return false
        if (echoDelay != other.echoDelay) return false
        if (echoDecay != other.echoDecay) return false
        if (echoRevert != other.echoRevert) return false
        if (equalizer != other.equalizer) return false
        if (equalizerQ != other.equalizerQ) return false
        if (!equalizerBand.contentEquals(other.equalizerBand)) return false
        if (virtualizerEnabled != other.virtualizerEnabled) return false
        if (virtualizerStrength != other.virtualizerStrength) return false
        if (bassBoostEnabled != other.bassBoostEnabled) return false
        if (bassBoostStrength != other.bassBoostStrength) return false
        if (equalizerType != other.equalizerType) return false
        if (selectedPreset != other.selectedPreset) return false
        if (showPitchFine != other.showPitchFine) return false
        if (showSpeedFine != other.showSpeedFine) return false
        if (delayEnabled != other.delayEnabled) return false
        if (delayTime != other.delayTime) return false
        if (delayFeedback != other.delayFeedback) return false
        if (delayMix != other.delayMix) return false
        if (reverbEnabled != other.reverbEnabled) return false
        if (reverbRoomSize != other.reverbRoomSize) return false
        if (reverbDamping != other.reverbDamping) return false
        if (reverbMix != other.reverbMix) return false
        if (chorusEnabled != other.chorusEnabled) return false
        if (chorusRate != other.chorusRate) return false
        if (chorusDepth != other.chorusDepth) return false
        if (chorusMix != other.chorusMix) return false
        if (flangerEnabled != other.flangerEnabled) return false
        if (flangerRate != other.flangerRate) return false
        if (flangerDepth != other.flangerDepth) return false
        if (flangerFeedback != other.flangerFeedback) return false
        if (flangerMix != other.flangerMix) return false
        if (polyphonyEnabled != other.polyphonyEnabled) return false
        if (polyphonySemitones != other.polyphonySemitones) return false
        if (polyphonyDetune != other.polyphonyDetune) return false
        return polyphonyMix == other.polyphonyMix
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + speed.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + echo.hashCode()
        result = 31 * result + echoDelay.hashCode()
        result = 31 * result + echoDecay.hashCode()
        result = 31 * result + echoRevert.hashCode()
        result = 31 * result + equalizer.hashCode()
        result = 31 * result + equalizerQ.hashCode()
        result = 31 * result + equalizerBand.contentHashCode()
        result = 31 * result + virtualizerEnabled.hashCode()
        result = 31 * result + virtualizerStrength.hashCode()
        result = 31 * result + bassBoostEnabled.hashCode()
        result = 31 * result + bassBoostStrength.hashCode()
        result = 31 * result + equalizerType.hashCode()
        result = 31 * result + selectedPreset.hashCode()
        result = 31 * result + showPitchFine.hashCode()
        result = 31 * result + showSpeedFine.hashCode()
        result = 31 * result + delayEnabled.hashCode()
        result = 31 * result + delayTime.hashCode()
        result = 31 * result + delayFeedback.hashCode()
        result = 31 * result + delayMix.hashCode()
        result = 31 * result + reverbEnabled.hashCode()
        result = 31 * result + reverbRoomSize.hashCode()
        result = 31 * result + reverbDamping.hashCode()
        result = 31 * result + reverbMix.hashCode()
        result = 31 * result + chorusEnabled.hashCode()
        result = 31 * result + chorusRate.hashCode()
        result = 31 * result + chorusDepth.hashCode()
        result = 31 * result + chorusMix.hashCode()
        result = 31 * result + flangerEnabled.hashCode()
        result = 31 * result + flangerRate.hashCode()
        result = 31 * result + flangerDepth.hashCode()
        result = 31 * result + flangerFeedback.hashCode()
        result = 31 * result + flangerMix.hashCode()
        result = 31 * result + polyphonyEnabled.hashCode()
        result = 31 * result + polyphonySemitones.hashCode()
        result = 31 * result + polyphonyDetune.hashCode()
        result = 31 * result + polyphonyMix.hashCode()
        return result
    }


}