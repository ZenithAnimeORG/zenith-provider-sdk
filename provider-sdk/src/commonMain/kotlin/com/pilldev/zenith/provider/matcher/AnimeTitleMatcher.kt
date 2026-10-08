package com.pilldev.zenith.provider.matcher

import kotlin.math.abs

/**
 * Intelligent fuzzy title matcher and similarity scorer.
 * Matches anime search queries and titles (including synonyms/alternative titles)
 * against provider candidate titles (Kodik, AnimeLib, Nyaa, Rezka, etc.).
 */
public object AnimeTitleMatcher {
    private val NON_ALPHANUMERIC_CHARS_REGEX = Regex("""[^\p{L}\p{N}]+""")
    private val MULTIPLE_SPACES_REGEX = Regex("""\s+""")
    private val SEASON_KEYWORD_REGEX =
        Regex(
            """(?:(?<!\p{L})(?:season|сезон|part|часть|тв|tv)[\s\-_]*(\d+)(?!\d))|(?:(?<!\d)(\d+)[\s\-_]*(?:st|nd|rd|th)?\s*(?:season|сезон|часть|part)(?!\p{L}))|(?:(?<!\p{L})[sS](\d+)(?!\d))""",
            RegexOption.IGNORE_CASE,
        )
    private val TRAILING_DIGIT_REGEX =
        Regex("""(?:^|[^\w])([2-9])\s*$""")
    private val ROMAN_SEASON_REGEX =
        Regex("""\b(VIII|VII|VI|IV|III|II|IX)\b""", RegexOption.IGNORE_CASE)
    private val TRAILING_PAREN_REGEX =
        Regex("""\s*[\(\[].*?[\)\]]\s*$""")
    private val SEARCH_SUFFIX_REGEX =
        Regex("""(?i)\s*(?:[-–—]\s*)?(?:(?:\d+[\s\-_]*)?(?:season|сезон|part|часть|тв|tv|vost|anilibria|войс|озвучка)|\b[sS]\d+|[\(\[][^\)\]]*(?:season|сезон|part|часть|тв|tv|vost|anilibria|войс|озвучка)[^\)\]]*[\)\]]).*$""")
    private val SEARCH_PUNCTUATION_REGEX =
        Regex("""[:!?]""")

    public fun extractSeasonNumber(title: String): Int? {
        if (title.isBlank()) return null

        val cleanTitle = title.replace(TRAILING_PAREN_REGEX, "").trim()

        SEASON_KEYWORD_REGEX.find(title)?.let { match ->
            val numStr = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
            numStr?.toIntOrNull()?.let { return it }
        }

        TRAILING_DIGIT_REGEX.find(cleanTitle)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { return it }
        }

        ROMAN_SEASON_REGEX.find(title)?.let { match ->
            return when (match.groupValues[1].uppercase()) {
                "II" -> 2
                "III" -> 3
                "IV" -> 4
                "VI" -> 6
                "VII" -> 7
                "VIII" -> 8
                "IX" -> 9
                else -> null
            }
        }

        return null
    }

    public fun cleanForSearch(title: String): String {
        if (title.isBlank()) return ""
        return title
            .replace(SEARCH_SUFFIX_REGEX, "")
            .replace(SEARCH_PUNCTUATION_REGEX, " ")
            .trim()
            .replace(MULTIPLE_SPACES_REGEX, " ")
    }

    public fun normalize(value: String): String =
        value
            .lowercase()
            .replace(NON_ALPHANUMERIC_CHARS_REGEX, " ")
            .trim()
            .replace(MULTIPLE_SPACES_REGEX, " ")

    public fun score(
        query: String,
        candidateTitle: String,
    ): Int {
        val normQuery = normalize(query)
        val normCandidate = normalize(candidateTitle)

        if (normQuery.isEmpty() || normCandidate.isEmpty()) {
            return Int.MIN_VALUE
        }

        if (normQuery == normCandidate) {
            return 1_000_000
        }

        val containsBonus = when {
            normCandidate.contains(normQuery) || normQuery.contains(normCandidate) -> 150_000
            else -> 0
        }

        val queryTokens = normQuery.split(' ').filter(String::isNotEmpty)
        val candidateTokenSet = normCandidate.split(' ').filter(String::isNotEmpty).toSet()

        val matchingTokenCount = queryTokens.count { token -> token in candidateTokenSet }
        val tokenScore = if (queryTokens.isNotEmpty()) {
            matchingTokenCount * 100_000 / queryTokens.size
        } else {
            0
        }

        val commonPrefixLength = normQuery.commonPrefixWith(normCandidate).length
        val lengthPenalty = abs(normQuery.length - normCandidate.length)

        return containsBonus + tokenScore + commonPrefixLength - lengthPenalty
    }

    public fun scoreWithSynonyms(
        queries: List<String>,
        candidateTitles: List<String>,
    ): Int {
        val normQueries = queries.map(::normalize).filter(String::isNotEmpty)
        val normCandidates = candidateTitles.map(::normalize).filter(String::isNotEmpty)

        if (normQueries.isEmpty() || normCandidates.isEmpty()) {
            return Int.MIN_VALUE
        }

        return normCandidates.maxOf { candidate ->
            normQueries.maxOf { query ->
                score(query, candidate)
            }
        }
    }

    public fun <T> sortBySimilarity(
        queries: List<String>,
        items: List<T>,
        titleSelector: (T) -> List<String>,
    ): List<T> {
        if (items.isEmpty()) return emptyList()
        val normQueries = queries.map(::normalize).filter(String::isNotEmpty)
        if (normQueries.isEmpty()) return items

        return items.sortedWith(
            compareByDescending { item ->
                scoreWithSynonyms(queries, titleSelector(item))
            }
        )
    }

    public fun <T> findBestMatch(
        queries: List<String>,
        items: List<T>,
        minScoreThreshold: Int = 50_000,
        titleSelector: (T) -> List<String>,
    ): T? {
        if (items.isEmpty()) return null
        val sorted = sortBySimilarity(queries, items, titleSelector)
        val topItem = sorted.firstOrNull() ?: return null
        val topScore = scoreWithSynonyms(queries, titleSelector(topItem))
        return if (topScore >= minScoreThreshold) topItem else null
    }

    public fun levenshteinDistance(
        s1: String,
        s2: String,
    ): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] =
                    minOf(
                        curr[j - 1] + 1,
                        prev[j] + 1,
                        prev[j - 1] + cost,
                    )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[len2]
    }

    public fun getSimilarityScore(
        candidateTitles: List<String>,
        queries: List<String>,
    ): Double {
        val namesToCompare =
            candidateTitles
                .map { it.lowercase().trim() }
                .filter { it.isNotEmpty() }

        if (namesToCompare.isEmpty()) return 0.0

        var maxScore = 0.0
        for (query in queries) {
            val cleanQuery = query.lowercase().trim()
            if (cleanQuery.isEmpty()) continue

            for (name in namesToCompare) {
                if (name == cleanQuery) {
                    maxScore = maxOf(maxScore, 1.0)
                    continue
                }

                if (name.contains(cleanQuery)) {
                    val subScore = cleanQuery.length.toDouble() / name.length.toDouble()
                    val containmentScore = 0.8 + (subScore * 0.19)
                    maxScore = maxOf(maxScore, containmentScore)
                    continue
                }

                if (cleanQuery.contains(name)) {
                    val subScore = name.length.toDouble() / cleanQuery.length.toDouble()
                    val containmentScore = 0.7 + (subScore * 0.19)
                    maxScore = maxOf(maxScore, containmentScore)
                    continue
                }

                val distance = levenshteinDistance(cleanQuery, name)
                val maxLength = maxOf(cleanQuery.length, name.length)
                val levScore = 1.0 - (distance.toDouble() / maxLength.toDouble())
                val finalScore = levScore * 0.65
                maxScore = maxOf(maxScore, finalScore)
            }
        }
        return maxScore
    }
}
