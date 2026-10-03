package com.ztftrue.music.utils

import com.ztftrue.music.sqlData.model.MusicItem
import com.ztftrue.music.utils.model.AlbumList
import com.ztftrue.music.utils.model.ArtistList
import com.ztftrue.music.utils.model.FolderList
import com.ztftrue.music.utils.model.GenresList
import com.ztftrue.music.utils.model.MusicPlayList
import java.util.Locale

object SortUtils {

    private interface SortEngine {
        fun getSectionLabel(name: String): String
        fun compare(s1: String, s2: String): Int
    }

    private val engine: SortEngine by lazy {
        try {
            IcuSortEngine()
        } catch (_: Throwable) {
            JavaSortEngine()
        }
    }

    private class IcuSortEngine : SortEngine {
        private val immutableIndex = run {
            val index = android.icu.text.AlphabeticIndex<String>(android.icu.util.ULocale.SIMPLIFIED_CHINESE)
            index.addLabels(android.icu.util.ULocale.ENGLISH)
            index.addLabels(android.icu.util.ULocale.JAPANESE)
            index.addLabels(android.icu.util.ULocale.KOREAN)
            index.buildImmutableIndex()
        }

        private val collator: android.icu.text.Collator = run {
            val c = android.icu.text.Collator.getInstance(android.icu.util.ULocale.SIMPLIFIED_CHINESE)
            c.strength = android.icu.text.Collator.TERTIARY
            c
        }

        override fun getSectionLabel(name: String): String {
            val cleanName = clean(name)
            if (cleanName.isEmpty()) return "#"
            if (cleanName[0] in '0'..'9') return "#"
            val idx = immutableIndex.getBucketIndex(cleanName)
            val label = immutableIndex.getBucket(idx).label
            return if (label == "…" || label.isBlank()) "#" else label
        }

        override fun compare(s1: String, s2: String): Int {
            val c1 = clean(s1)
            val c2 = clean(s2)
            val b1 = immutableIndex.getBucketIndex(c1)
            val b2 = immutableIndex.getBucketIndex(c2)
            return if (b1 != b2) {
                b1.compareTo(b2)
            } else {
                val r = collator.compare(c1, c2)
                if (r != 0) r else s1.compareTo(s2)
            }
        }
    }

    private class JavaSortEngine : SortEngine {
        private val collator: java.text.Collator = java.text.Collator.getInstance(Locale.CHINA)

        override fun getSectionLabel(name: String): String {
            val cleanName = clean(name)
            if (cleanName.isEmpty()) return "#"
            val first = cleanName[0]
            if (first in '0'..'9') return "#"
            if (first in 'A'..'Z' || first in 'a'..'z') return first.uppercaseChar().toString()
            // Japanese Hiragana Gojūon rows
            val c = first.code
            if (c in 0x3041..0x304A || c in 0x30A1..0x30AA) return "あ"
            if (c in 0x304B..0x3054 || c in 0x30AB..0x30B4) return "か"
            if (c in 0x3055..0x305E || c in 0x30B5..0x30BE) return "さ"
            if (c in 0x305F..0x3069 || c in 0x30BF..0x30C9) return "た"
            if (c in 0x306A..0x306E || c in 0x30CA..0x30CE) return "な"
            if (c in 0x306F..0x307E || c in 0x30CF..0x30DE) return "は"
            if (c in 0x307F..0x3083 || c in 0x30DF..0x30E3) return "ま"
            if (c in 0x3084..0x3088 || c in 0x30E4..0x30E8) return "や"
            if (c in 0x3089..0x308D || c in 0x30E9..0x30ED) return "ら"
            if (c in 0x308E..0x3093 || c in 0x30EE..0x30F3) return "わ"

            // Estimate Chinese pinyin letter via collator against boundary characters
            if (c in 0x4E00..0x9FA5) {
                val pinyinHeaders = arrayOf(
                    "啊" to "A", "芭" to "B", "擦" to "C", "搭" to "D", "蛾" to "E",
                    "发" to "F", "噶" to "G", "哈" to "H", "击" to "J", "喀" to "K",
                    "垃" to "L", "妈" to "M", "拿" to "N", "哦" to "O", "趴" to "P",
                    "期" to "Q", "然" to "R", "撒" to "S", "塌" to "T", "挖" to "W",
                    "昔" to "X", "压" to "Y", "匝" to "Z"
                )
                for (i in pinyinHeaders.indices.reversed()) {
                    if (collator.compare(cleanName, pinyinHeaders[i].first) >= 0) {
                        return pinyinHeaders[i].second
                    }
                }
                return "A"
            }
            return "#"
        }

        override fun compare(s1: String, s2: String): Int {
            val c1 = clean(s1)
            val c2 = clean(s2)
            val sec1 = getSectionLabel(c1)
            val sec2 = getSectionLabel(c2)
            if (sec1 != sec2) {
                if (sec1 == "#") return -1
                if (sec2 == "#") return 1
                val secCmp = sec1.compareTo(sec2)
                if (secCmp != 0) return secCmp
            }
            val r = collator.compare(c1, c2)
            return if (r != 0) r else s1.compareTo(s2)
        }
    }

    private fun clean(s: String?): String {
        if (s.isNullOrBlank()) return ""
        var start = 0
        val len = s.length
        while (start < len) {
            val ch = s[start]
            if (ch.isWhitespace() || ch in "([{<\"'“‘（【《-_") {
                start++
            } else {
                break
            }
        }
        return if (start < len) s.substring(start) else s.trim()
    }

    fun getSectionLabel(name: String?): String {
        if (name.isNullOrBlank()) return "#"
        return engine.getSectionLabel(name)
    }

    fun compareStrings(s1: String?, s2: String?, ascending: Boolean = true): Int {
        if (s1.isNullOrBlank() && s2.isNullOrBlank()) return 0
        if (s1.isNullOrBlank()) return if (ascending) 1 else -1
        if (s2.isNullOrBlank()) return if (ascending) -1 else 1

        val cmp = engine.compare(s1, s2)
        return if (ascending) cmp else -cmp
    }

    fun sortTracks(list: MutableList<MusicItem>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val comparator = getTrackComparator(sortOrder, !isDesc)
        list.sortWith(comparator)
    }

    fun sortTracksMap(map: LinkedHashMap<Long, MusicItem>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val comparator = getTrackComparator(sortOrder, !isDesc)
        val sorted = map.entries.sortedWith { e1, e2 -> comparator.compare(e1.value, e2.value) }
        map.clear()
        for (entry in sorted) {
            map[entry.key] = entry.value
        }
    }

    private fun getTrackComparator(sortOrder: String?, ascending: Boolean): Comparator<MusicItem> {
        val orderLower = sortOrder?.lowercase().orEmpty()
        return when {
            orderLower.contains("album") -> Comparator { a, b ->
                val c = compareStrings(a.album, b.album, ascending)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("artist") -> Comparator { a, b ->
                val c = compareStrings(a.artist, b.artist, ascending)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("duration") -> Comparator { a, b ->
                val c = if (ascending) a.duration.compareTo(b.duration) else b.duration.compareTo(a.duration)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("year") -> Comparator { a, b ->
                val c = if (ascending) a.year.compareTo(b.year) else b.year.compareTo(a.year)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("track") -> Comparator { a, b ->
                val c = if (ascending) a.songNumber.compareTo(b.songNumber) else b.songNumber.compareTo(a.songNumber)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            else -> Comparator { a, b ->
                compareStrings(a.name, b.name, ascending)
            }
        }
    }

    fun sortAlbums(map: LinkedHashMap<Long, AlbumList>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        val orderLower = sortOrder?.lowercase().orEmpty()
        val comparator = when {
            orderLower.contains("artist") -> Comparator<AlbumList> { a, b ->
                val c = compareStrings(a.artist, b.artist, ascending)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("first_year") || orderLower.contains("minyear") -> Comparator<AlbumList> { a, b ->
                val c = if (ascending) a.firstYear.compareTo(b.firstYear) else b.firstYear.compareTo(a.firstYear)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("last_year") || orderLower.contains("maxyear") -> Comparator<AlbumList> { a, b ->
                val c = if (ascending) a.lastYear.compareTo(b.lastYear) else b.lastYear.compareTo(a.lastYear)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("numsongs") || orderLower.contains("number_of_songs") -> Comparator<AlbumList> { a, b ->
                val c = if (ascending) a.trackNumber.compareTo(b.trackNumber) else b.trackNumber.compareTo(a.trackNumber)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            else -> Comparator<AlbumList> { a, b ->
                compareStrings(a.name, b.name, ascending)
            }
        }
        val sorted = map.entries.sortedWith { e1, e2 -> comparator.compare(e1.value, e2.value) }
        map.clear()
        for (entry in sorted) {
            map[entry.key] = entry.value
        }
    }

    fun sortAlbumsList(list: MutableList<AlbumList>, sortOrder: String? = null) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        list.sortWith { a, b -> compareStrings(a.name, b.name, ascending) }
    }

    fun sortArtists(map: LinkedHashMap<Long, ArtistList>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        val orderLower = sortOrder?.lowercase().orEmpty()
        val comparator = when {
            orderLower.contains("number_of_albums") -> Comparator<ArtistList> { a, b ->
                val c = if (ascending) a.albumNumber.compareTo(b.albumNumber) else b.albumNumber.compareTo(a.albumNumber)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            orderLower.contains("number_of_tracks") -> Comparator<ArtistList> { a, b ->
                val c = if (ascending) a.trackNumber.compareTo(b.trackNumber) else b.trackNumber.compareTo(a.trackNumber)
                if (c != 0) c else compareStrings(a.name, b.name, true)
            }
            else -> Comparator<ArtistList> { a, b ->
                compareStrings(a.name, b.name, ascending)
            }
        }
        val sorted = map.entries.sortedWith { e1, e2 -> comparator.compare(e1.value, e2.value) }
        map.clear()
        for (entry in sorted) {
            map[entry.key] = entry.value
        }
    }

    fun sortArtistsList(list: MutableList<ArtistList>, sortOrder: String? = null) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        list.sortWith { a, b -> compareStrings(a.name, b.name, ascending) }
    }

    fun sortGenres(map: LinkedHashMap<Long, GenresList>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        val comparator = Comparator<GenresList> { a, b ->
            compareStrings(a.name, b.name, ascending)
        }
        val sorted = map.entries.sortedWith { e1, e2 -> comparator.compare(e1.value, e2.value) }
        map.clear()
        for (entry in sorted) {
            map[entry.key] = entry.value
        }
    }

    fun sortPlaylists(list: ArrayList<MusicPlayList>, sortOrder: String?) {
        val isDesc = sortOrder?.contains("DESC", ignoreCase = true) == true
        val ascending = !isDesc
        list.sortWith { a, b -> compareStrings(a.name, b.name, ascending) }
    }

    fun sortFolders(map: LinkedHashMap<Long, FolderList>, ascending: Boolean = true) {
        val sorted = map.entries.sortedWith { e1, e2 ->
            compareStrings(e1.value.name, e2.value.name, ascending)
        }
        map.clear()
        for (entry in sorted) {
            map[entry.key] = entry.value
        }
    }
}
