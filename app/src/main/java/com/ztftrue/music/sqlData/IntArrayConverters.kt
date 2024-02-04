package com.ztftrue.music.sqlData

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson
import java.lang.reflect.Type


class IntArrayConverters {
    @TypeConverter
    fun fromString(value:String):IntArray {
        val listType: Type = object : TypeToken<IntArray>() {}.type

        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: IntArray?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}
