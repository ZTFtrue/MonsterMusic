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

    private interface CollatorProvider {
        fun compareLatin(s1: String, s2: String): Int
        fun compareChinese(s1: String, s2: String): Int
        fun compareJapanese(s1: String, s2: String): Int
        fun compareGeneral(s1: String, s2: String): Int
    }

    private class IcuCollatorProvider : CollatorProvider {
        private val chineseCollator = run {
            val c = android.icu.text.Collator.getInstance(android.icu.util.ULocale.SIMPLIFIED_CHINESE)
            c.strength = android.icu.text.Collator.TERTIARY
            c
        }
        private val japaneseCollator = run {
            val c = android.icu.text.Collator.getInstance(android.icu.util.ULocale.JAPANESE)
            c.strength = android.icu.text.Collator.TERTIARY
            c
        }
        private val latinCollator = run {
            val c = android.icu.text.Collator.getInstance(android.icu.util.ULocale.ENGLISH)
            c.strength = android.icu.text.Collator.SECONDARY
            c
        }
        private val generalCollator = run {
            val c = android.icu.text.Collator.getInstance(android.icu.util.ULocale.ROOT)
            c.strength = android.icu.text.Collator.TERTIARY
            c
        }

        override fun compareLatin(s1: String, s2: String): Int = latinCollator.compare(s1, s2)
        override fun compareChinese(s1: String, s2: String): Int = chineseCollator.compare(s1, s2)
        override fun compareJapanese(s1: String, s2: String): Int = japaneseCollator.compare(s1, s2)
        override fun compareGeneral(s1: String, s2: String): Int = generalCollator.compare(s1, s2)
    }

    private class JavaCollatorProvider : CollatorProvider {
        private val chineseCollator: java.text.Collator = java.text.Collator.getInstance(Locale.CHINA)
        private val japaneseCollator: java.text.Collator = java.text.Collator.getInstance(Locale.JAPAN)
        private val latinCollator: java.text.Collator = java.text.Collator.getInstance(Locale.ENGLISH)
        private val generalCollator: java.text.Collator = java.text.Collator.getInstance(Locale.ROOT)

        override fun compareLatin(s1: String, s2: String): Int = latinCollator.compare(s1, s2)
        override fun compareChinese(s1: String, s2: String): Int = chineseCollator.compare(s1, s2)
        override fun compareJapanese(s1: String, s2: String): Int = japaneseCollator.compare(s1, s2)
        override fun compareGeneral(s1: String, s2: String): Int = generalCollator.compare(s1, s2)
    }

    private class BaseSortEngine(private val provider: CollatorProvider) : SortEngine {
        override fun getSectionLabel(name: String): String {
            val cleanName = clean(name)
            if (cleanName.isEmpty()) return "#"
            val codePoint = cleanName.codePointAt(0)
            val charCount = Character.charCount(codePoint)
            val firstStr = cleanName.substring(0, charCount)
            return when (getCategory(codePoint)) {
                0 -> "#"
                1 -> firstStr.uppercase()
                2 -> firstStr // Keep Chinese character itself
                3 -> firstStr // Keep Japanese Kana character itself
                4 -> firstStr // Keep Korean character itself
                else -> firstStr.uppercase()
            }
        }

        override fun compare(s1: String, s2: String): Int {
            val c1 = clean(s1)
            val c2 = clean(s2)
            if (c1.isEmpty() && c2.isEmpty()) return s1.compareTo(s2)
            if (c1.isEmpty()) return -1
            if (c2.isEmpty()) return 1

            val codePoint1 = c1.codePointAt(0)
            val codePoint2 = c2.codePointAt(0)
            val cat1 = getCategory(codePoint1)
            val cat2 = getCategory(codePoint2)
            if (cat1 != cat2) {
                return cat1.compareTo(cat2)
            }

            return when (cat1) {
                0 -> {
                    val r = c1.compareTo(c2, ignoreCase = true)
                    if (r != 0) r else s1.compareTo(s2)
                }
                1 -> {
                    val r = provider.compareLatin(c1, c2)
                    if (r != 0) r else s1.compareTo(s2)
                }
                2 -> {
                    val r = provider.compareChinese(c1, c2)
                    if (r != 0) r else s1.compareTo(s2)
                }
                3 -> {
                    val r = provider.compareJapanese(c1, c2)
                    if (r != 0) r else s1.compareTo(s2)
                }
                else -> {
                    val r = provider.compareGeneral(c1, c2)
                    if (r != 0) r else s1.compareTo(s2)
                }
            }
        }
    }

    private val engine: SortEngine by lazy {
        try {
            BaseSortEngine(IcuCollatorProvider())
        } catch (_: Throwable) {
            BaseSortEngine(JavaCollatorProvider())
        }
    }

    private fun getCategory(codePoint: Int): Int {
        if (codePoint in '0'.code..'9'.code) return 0
        if (codePoint in 'A'.code..'Z'.code || codePoint in 'a'.code..'z'.code) return 1
        val script = try {
            Character.UnicodeScript.of(codePoint)
        } catch (_: Throwable) {
            null
        }
        if (script == Character.UnicodeScript.LATIN) return 1
        if (codePoint in 0x4E00..0x9FFF || codePoint in 0x3400..0x4DBF || script == Character.UnicodeScript.HAN) return 2
        if (codePoint in 0x3040..0x30FF || script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA) return 3
        if (codePoint in 0xAC00..0xD7AF || script == Character.UnicodeScript.HANGUL) return 4
        if (Character.isLetter(codePoint)) return 5
        return 0
    }

    private fun clean(s: String?): String {
        if (s.isNullOrBlank()) return ""
        var start = 0
        val len = s.length
        while (start < len) {
            val ch = s[start]
            if (ch.isWhitespace() || ch in "([{<\"'“‘（【《-_「『〈〔") {
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
        val cmp = engine.compare(s1 ?: "", s2 ?: "")
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
