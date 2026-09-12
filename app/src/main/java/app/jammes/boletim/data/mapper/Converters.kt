package app.jammes.boletim.data.mapper

import androidx.room.TypeConverter
import java.time.LocalDate
import kotlin.time.Instant

class Converters {
    @TypeConverter
    fun intToLocalDate(v: Int?): LocalDate? = v?.let {
        LocalDate.of(it / 10000, (it / 100) % 100, it % 100)
    }

    @TypeConverter
    fun localDateToInt(d: LocalDate?): Int? = d?.let {
        it.year * 10000 + it.monthValue * 100 + it.dayOfMonth
    }

    @TypeConverter
    fun longToInstant(v: Long?): Instant? = v?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun instantToLong(i: Instant?): Long? = i?.toEpochMilliseconds()
}