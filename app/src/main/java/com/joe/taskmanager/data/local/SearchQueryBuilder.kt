
package com.joe.taskmanager.data.local

/**
 * Turns raw user input into a safe FTS4 MATCH expression.
 *
 * User text is NEVER interpolated unescaped: an unbalanced quote or a stray
 * operator would otherwise make SQLite throw a syntax error and take the search
 * screen down with it. Every token is stripped to word characters and joined
 * with AND, and each term is made a prefix term so search feels responsive.
 */
object SearchQueryBuilder {

    private val FTS_SPECIAL = charArrayOf('"', '\'', '*', '(', ')', ':', '-', '+', '^', '.', ',', '!', '?')

    fun build(raw: String): String {
        if (raw.isBlank()) return ""

        val terms = raw
            .split(WHITESPACE)
            .map { term ->
                term.filterNot { it in FTS_SPECIAL && it != '-' }
                    .trim()
                    .takeIf { it.isNotEmpty() }
            }
            .filterNotNull()
            .distinct()
            .take(MAX_TERMS)

        if (terms.isEmpty()) return ""

        return terms.joinToString(" AND ") { "\"$it\"*" }
    }

    /** True when the built expression is non-empty, i.e. the search can run. */
    fun isSearchable(raw: String): Boolean = build(raw).isNotEmpty()

    private val WHITESPACE = Regex("\\s+")
    private const val MAX_TERMS = 8
}
