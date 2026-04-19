package app.verdant.repository

import app.verdant.entity.DailyWeather
import app.verdant.entity.ObservationType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.time.LocalDate

@ApplicationScoped
class DailyWeatherRepository(private val ds: AgroalDataSource) {

    fun upsert(rows: List<DailyWeather>) {
        if (rows.isEmpty()) return
        ds.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO daily_weather
                    (garden_id, date, observation_type,
                     temp_min_c, temp_max_c, temp_mean_c,
                     precipitation_mm, wind_max_ms, humidity_pct, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (garden_id, date, observation_type) DO UPDATE SET
                    temp_min_c       = EXCLUDED.temp_min_c,
                    temp_max_c       = EXCLUDED.temp_max_c,
                    temp_mean_c      = EXCLUDED.temp_mean_c,
                    precipitation_mm = EXCLUDED.precipitation_mm,
                    wind_max_ms      = EXCLUDED.wind_max_ms,
                    humidity_pct     = EXCLUDED.humidity_pct,
                    fetched_at       = EXCLUDED.fetched_at
                """.trimIndent()
            ).use { ps ->
                for (row in rows) {
                    ps.setLong(1, row.gardenId)
                    ps.setDate(2, Date.valueOf(row.date))
                    ps.setString(3, row.observationType.name)
                    row.tempMinC?.let { ps.setDouble(4, it) } ?: ps.setNull(4, java.sql.Types.DOUBLE)
                    row.tempMaxC?.let { ps.setDouble(5, it) } ?: ps.setNull(5, java.sql.Types.DOUBLE)
                    row.tempMeanC?.let { ps.setDouble(6, it) } ?: ps.setNull(6, java.sql.Types.DOUBLE)
                    row.precipitationMm?.let { ps.setDouble(7, it) } ?: ps.setNull(7, java.sql.Types.DOUBLE)
                    row.windMaxMs?.let { ps.setDouble(8, it) } ?: ps.setNull(8, java.sql.Types.DOUBLE)
                    row.humidityPct?.let { ps.setDouble(9, it) } ?: ps.setNull(9, java.sql.Types.DOUBLE)
                    ps.setObject(10, row.fetchedAt.atOffset(java.time.ZoneOffset.UTC))
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    fun findByGarden(gardenId: Long, fromDate: LocalDate, toDate: LocalDate): List<DailyWeather> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM daily_weather WHERE garden_id = ? AND date >= ? AND date <= ? ORDER BY date ASC, observation_type ASC"
            ).use { ps ->
                ps.setLong(1, gardenId)
                ps.setDate(2, Date.valueOf(fromDate))
                ps.setDate(3, Date.valueOf(toDate))
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toDailyWeather()) }
                }
            }
        }

    fun findActualsByGarden(gardenId: Long, fromDate: LocalDate, toDate: LocalDate): List<DailyWeather> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM daily_weather WHERE garden_id = ? AND date >= ? AND date <= ? AND observation_type = 'ACTUAL' ORDER BY date ASC"
            ).use { ps ->
                ps.setLong(1, gardenId)
                ps.setDate(2, Date.valueOf(fromDate))
                ps.setDate(3, Date.valueOf(toDate))
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toDailyWeather()) }
                }
            }
        }

    fun deleteByGarden(gardenId: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM daily_weather WHERE garden_id = ?").use { ps ->
                ps.setLong(1, gardenId)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toDailyWeather() = DailyWeather(
        id = getLong("id"),
        gardenId = getLong("garden_id"),
        date = getDate("date").toLocalDate(),
        observationType = ObservationType.valueOf(getString("observation_type")),
        tempMinC = getDouble("temp_min_c").takeIf { !wasNull() },
        tempMaxC = getDouble("temp_max_c").takeIf { !wasNull() },
        tempMeanC = getDouble("temp_mean_c").takeIf { !wasNull() },
        precipitationMm = getDouble("precipitation_mm").takeIf { !wasNull() },
        windMaxMs = getDouble("wind_max_ms").takeIf { !wasNull() },
        humidityPct = getDouble("humidity_pct").takeIf { !wasNull() },
        fetchedAt = getTimestamp("fetched_at").toInstant(),
    )
}
