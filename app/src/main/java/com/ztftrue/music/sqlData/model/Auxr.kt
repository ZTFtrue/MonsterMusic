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
    @ColumnInfo(defaultValue = Utils.Q.toString()) var equalizerQ: Float=Utils.Q,
    @field:TypeConverters(IntArrayConverters::class)
    @ColumnInfo var equalizerBand: IntArray,
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
        if (equalizer != other.equalizer) return false
        return equalizerBand.contentEquals(other.equalizerBand)
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
        result = 31 * result + equalizerBand.contentHashCode()
        return result
    }


}