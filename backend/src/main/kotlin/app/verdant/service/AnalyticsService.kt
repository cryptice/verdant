package app.verdant.service

import app.verdant.dto.*
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class AnalyticsService(private val ds: AgroalDataSource) {

    fun getSeasonSummaries(userId: Long): List<SeasonSummaryResponse> {
        data class SpeciesRow(
            val seasonId: Long,
            val seasonName: String,
            val year: Int,
            val speciesId: Long,
            val speciesName: String,
            val plantCount: Int,
            val stemsHarvested: Int,
            val avgStemLength: Double?,
            val avgVaseLife: Double?,
        )

        val rows = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.id AS season_id,
                          s.name AS season_name,
                          s.year,
                          sp.id AS species_id,
                          sp.common_name AS species_name,
                          COUNT(DISTINCT p.id) AS plant_count,
                          COALESCE(SUM(pe.stem_count), 0) AS stems_harvested,
                          AVG(pe.stem_length_cm) AS avg_stem_length,
                          AVG(pe.vase_life_days) AS avg_vase_life
                   FROM plant p
                   JOIN season s ON p.season_id = s.id
                   JOIN species sp ON p.species_id = sp.id
                   LEFT JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.user_id = ?
                   GROUP BY s.id, s.name, s.year, sp.id, sp.common_name
                   ORDER BY s.year DESC, s.name, stems_harvested DESC"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            SpeciesRow(
                                seasonId = rs.getLong("season_id"),
                                seasonName = rs.getString("season_name"),
                                year = rs.getInt("year"),
                                speciesId = rs.getLong("species_id"),
                                speciesName = rs.getString("species_name"),
                                plantCount = rs.getInt("plant_count"),
                                stemsHarvested = rs.getInt("stems_harvested"),
                                avgStemLength = rs.getDouble("avg_stem_length").takeIf { !rs.wasNull() },
                                avgVaseLife = rs.getDouble("avg_vase_life").takeIf { !rs.wasNull() },
                            )
                        )
                    }
                }
            }
        }

        // Fetch quality breakdown separately per species per season
        data class QualityRow(val seasonId: Long, val speciesId: Long, val qualityGrade: String, val qualityCount: Int)
        val qualityRows = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.id AS season_id,
                          sp.id AS species_id,
                          pe.quality_grade,
                          COUNT(*) AS quality_count
                   FROM plant p
                   JOIN season s ON p.season_id = s.id
                   JOIN species sp ON p.species_id = sp.id
                   JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.user_id = ? AND pe.quality_grade IS NOT NULL
                   GROUP BY s.id, sp.id, pe.quality_grade"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            QualityRow(
                                seasonId = rs.getLong("season_id"),
                                speciesId = rs.getLong("species_id"),
                                qualityGrade = rs.getString("quality_grade"),
                                qualityCount = rs.getInt("quality_count"),
                            )
                        )
                    }
                }
            }
        }
        val qualityMap = qualityRows.groupBy { it.seasonId to it.speciesId }
            .mapValues { (_, rows) -> rows.associate { it.qualityGrade to it.qualityCount } }

        // Also fetch total harvest weight per season
        data class WeightRow(val seasonId: Long, val totalWeight: Double)
        val weights = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.id AS season_id,
                          COALESCE(SUM(pe.weight_grams), 0) AS total_weight
                   FROM plant p
                   JOIN season s ON p.season_id = s.id
                   JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.user_id = ?
                   GROUP BY s.id"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            WeightRow(
                                seasonId = rs.getLong("season_id"),
                                totalWeight = rs.getDouble("total_weight"),
                            )
                        )
                    }
                }
            }
        }
        val weightMap = weights.associate { it.seasonId to it.totalWeight }

        return rows.groupBy { Triple(it.seasonId, it.seasonName, it.year) }
            .map { (seasonKey, seasonRows) ->
                val speciesSummaries = seasonRows
                    .map { row ->
                        val qualityBreakdown = qualityMap[row.seasonId to row.speciesId] ?: emptyMap()
                        SpeciesYieldSummary(
                            speciesId = row.speciesId,
                            speciesName = row.speciesName,
                            plantCount = row.plantCount,
                            stemsHarvested = row.stemsHarvested,
                            avgStemLength = row.avgStemLength,
                            avgVaseLife = row.avgVaseLife,
                            qualityBreakdown = qualityBreakdown,
                        )
                    }
                    .sortedByDescending { it.stemsHarvested }

                SeasonSummaryResponse(
                    seasonId = seasonKey.first,
                    seasonName = seasonKey.second,
                    year = seasonKey.third,
                    totalPlants = speciesSummaries.sumOf { it.plantCount },
                    totalStemsHarvested = speciesSummaries.sumOf { it.stemsHarvested },
                    totalHarvestWeightGrams = weightMap[seasonKey.first] ?: 0.0,
                    speciesCount = speciesSummaries.size,
                    topSpecies = speciesSummaries,
                )
            }
    }

    fun getSpeciesComparison(userId: Long, speciesId: Long): SpeciesComparisonResponse {
        data class Row(
            val speciesName: String,
            val seasonId: Long,
            val seasonName: String,
            val year: Int,
            val plantCount: Int,
            val stemsHarvested: Int,
            val avgStemLength: Double?,
            val avgVaseLife: Double?,
        )

        val rows = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT sp.common_name AS species_name,
                          s.id AS season_id,
                          s.name AS season_name,
                          s.year,
                          COUNT(DISTINCT p.id) AS plant_count,
                          COALESCE(SUM(pe.stem_count), 0) AS stems_harvested,
                          AVG(pe.stem_length_cm) AS avg_stem_length,
                          AVG(pe.vase_life_days) AS avg_vase_life
                   FROM plant p
                   JOIN season s ON p.season_id = s.id
                   JOIN species sp ON p.species_id = sp.id
                   LEFT JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.user_id = ? AND sp.id = ?
                   GROUP BY sp.common_name, s.id, s.name, s.year
                   ORDER BY s.year"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            Row(
                                speciesName = rs.getString("species_name"),
                                seasonId = rs.getLong("season_id"),
                                seasonName = rs.getString("season_name"),
                                year = rs.getInt("year"),
                                plantCount = rs.getInt("plant_count"),
                                stemsHarvested = rs.getInt("stems_harvested"),
                                avgStemLength = rs.getDouble("avg_stem_length").takeIf { !rs.wasNull() },
                                avgVaseLife = rs.getDouble("avg_vase_life").takeIf { !rs.wasNull() },
                            )
                        )
                    }
                }
            }
        }

        val speciesName = rows.firstOrNull()?.speciesName ?: ds.connection.use { conn ->
            conn.prepareStatement("SELECT common_name FROM species WHERE id = ?").use { ps ->
                ps.setLong(1, speciesId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("common_name") else "Unknown"
                }
            }
        }

        return SpeciesComparisonResponse(
            speciesId = speciesId,
            speciesName = speciesName,
            seasons = rows.map {
                val stemsPerPlant = if (it.plantCount > 0) it.stemsHarvested.toDouble() / it.plantCount else null
                SpeciesSeasonData(
                    seasonId = it.seasonId,
                    seasonName = it.seasonName,
                    year = it.year,
                    plantCount = it.plantCount,
                    stemsHarvested = it.stemsHarvested,
                    stemsPerPlant = stemsPerPlant,
                    avgStemLength = it.avgStemLength,
                    avgVaseLife = it.avgVaseLife,
                )
            },
        )
    }

    fun getYieldPerBed(userId: Long, seasonId: Long?): List<YieldPerBedResponse> {
        data class Row(
            val bedId: Long,
            val bedName: String,
            val gardenName: String,
            val lengthMeters: Double?,
            val widthMeters: Double?,
            val seasonIdVal: Long,
            val seasonName: String,
            val stemsHarvested: Int,
        )

        val seasonFilter = if (seasonId != null) "AND p.season_id = ?" else ""
        val rows = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT b.id AS bed_id,
                          b.name AS bed_name,
                          g.name AS garden_name,
                          b.length_meters,
                          b.width_meters,
                          s.id AS season_id,
                          s.name AS season_name,
                          COALESCE(SUM(pe.stem_count), 0) AS stems_harvested
                   FROM plant p
                   JOIN bed b ON p.bed_id = b.id
                   JOIN garden g ON b.garden_id = g.id
                   JOIN season s ON p.season_id = s.id
                   LEFT JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE g.owner_id = ? $seasonFilter
                   GROUP BY b.id, b.name, g.name, b.length_meters, b.width_meters, s.id, s.name
                   ORDER BY b.name, s.year"""
            ).use { ps ->
                ps.setLong(1, userId)
                if (seasonId != null) ps.setLong(2, seasonId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            Row(
                                bedId = rs.getLong("bed_id"),
                                bedName = rs.getString("bed_name"),
                                gardenName = rs.getString("garden_name"),
                                lengthMeters = rs.getDouble("length_meters").takeIf { !rs.wasNull() },
                                widthMeters = rs.getDouble("width_meters").takeIf { !rs.wasNull() },
                                seasonIdVal = rs.getLong("season_id"),
                                seasonName = rs.getString("season_name"),
                                stemsHarvested = rs.getInt("stems_harvested"),
                            )
                        )
                    }
                }
            }
        }

        return rows.groupBy { Triple(it.bedId, it.bedName, it.gardenName) }
            .map { (bedKey, seasonRows) ->
                val first = seasonRows.first()
                val areaM2 = if (first.lengthMeters != null && first.widthMeters != null)
                    first.lengthMeters * first.widthMeters else null

                YieldPerBedResponse(
                    bedId = bedKey.first,
                    bedName = bedKey.second,
                    gardenName = bedKey.third,
                    areaM2 = areaM2,
                    seasons = seasonRows.map {
                        BedSeasonYield(
                            seasonId = it.seasonIdVal,
                            seasonName = it.seasonName,
                            stemsHarvested = it.stemsHarvested,
                            stemsPerM2 = if (areaM2 != null && areaM2 > 0) it.stemsHarvested / areaM2 else null,
                        )
                    },
                )
            }
    }
}
