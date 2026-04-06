package app.verdant.android.voice

import app.verdant.android.data.model.SupplyInventoryResponse
import java.text.Normalizer

data class SupplyMatch(
    val supply: SupplyInventoryResponse,
    val score: Double,
    val matchedName: String,
)

object SupplyMatcher {

    fun findBestMatch(query: String, supplies: List<SupplyInventoryResponse>, limit: Int = 3): List<SupplyMatch> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        // Group by type, pick one representative per type (highest quantity)
        val byType = supplies.groupBy { it.supplyTypeId }

        return byType.values.mapNotNull { batches ->
            val representative = batches.maxByOrNull { it.quantity } ?: return@mapNotNull null
            val name = normalize(representative.supplyTypeName)
            val score = similarity(normalizedQuery, name)
            if (score > 0.3) SupplyMatch(representative, score, representative.supplyTypeName) else null
        }
        .sortedByDescending { it.score }
        .take(limit)
    }

    private fun similarity(query: String, candidate: String): Double {
        if (candidate.contains(query)) return 1.0
        if (query.contains(candidate)) return 0.9

        val queryTokens = query.split("\\s+".toRegex()).toSet()
        val candidateTokens = candidate.split("\\s+".toRegex()).toSet()
        val intersection = queryTokens.intersect(candidateTokens)
        val tokenScore = if (queryTokens.isNotEmpty()) intersection.size.toDouble() / queryTokens.size else 0.0

        val editDistance = levenshtein(query, candidate)
        val maxLen = maxOf(query.length, candidate.length)
        val editScore = if (maxLen > 0) 1.0 - (editDistance.toDouble() / maxLen) else 0.0

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
