package app.verdant.android.voice

import app.verdant.android.data.model.SpeciesResponse
import java.text.Normalizer

data class SpeciesMatch(
    val species: SpeciesResponse,
    val score: Double,           // 0.0 to 1.0, higher is better
    val matchedName: String,     // which name variant matched
)

object SpeciesMatcher {

    fun findBestMatch(query: String, speciesList: List<SpeciesResponse>, limit: Int = 3): List<SpeciesMatch> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        return speciesList.mapNotNull { species ->
            val candidates = buildList {
                add(species.commonName)
                species.variantName?.let { add("${species.commonName} $it") }
                species.commonNameSv?.let { add(it) }
                if (species.variantNameSv != null && species.commonNameSv != null) {
                    add("${species.commonNameSv} ${species.variantNameSv}")
                }
                // Also try variant alone
                species.variantName?.let { add(it) }
                species.variantNameSv?.let { add(it) }
            }

            var bestScore = 0.0
            var bestName = ""
            for (candidate in candidates) {
                val score = similarity(normalizedQuery, normalize(candidate))
                if (score > bestScore) {
                    bestScore = score
                    bestName = candidate
                }
            }

            if (bestScore > 0.3) SpeciesMatch(species, bestScore, bestName) else null
        }
        .sortedByDescending { it.score }
        .take(limit)
    }

    private fun similarity(query: String, candidate: String): Double {
        // Combine substring matching with token overlap

        // Exact substring match is highest
        if (candidate.contains(query)) return 1.0
        if (query.contains(candidate)) return 0.9

        // Token overlap
        val queryTokens = query.split("\\s+".toRegex()).toSet()
        val candidateTokens = candidate.split("\\s+".toRegex()).toSet()
        val intersection = queryTokens.intersect(candidateTokens)
        val tokenScore = if (queryTokens.isNotEmpty()) {
            intersection.size.toDouble() / queryTokens.size
        } else 0.0

        // Levenshtein on full string (normalized by length)
        val editDistance = levenshtein(query, candidate)
        val maxLen = maxOf(query.length, candidate.length)
        val editScore = if (maxLen > 0) 1.0 - (editDistance.toDouble() / maxLen) else 0.0

        // Weighted combination
        return tokenScore * 0.6 + editScore * 0.4
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }

    private fun normalize(text: String): String {
        val lower = text.lowercase()
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
    }
}
