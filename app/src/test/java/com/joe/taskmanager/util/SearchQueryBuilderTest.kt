package com.joe.taskmanager.util

import com.joe.taskmanager.data.local.SearchQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A malformed MATCH expression makes SQLite throw, which would crash the search
 * screen. The invariant that matters is not "the word NEAR is absent" -- an
 * operator name inside a quoted term is a harmless literal -- it is that the
 * expression stays well formed: every term quoted, quotes balanced, and no
 * unquoted operator characters able to reach the FTS4 parser.
 */
class SearchQueryBuilderTest {

    @Test
    fun `blank input produces no query`() {
        assertEquals("", SearchQueryBuilder.build(""))
        assertEquals("", SearchQueryBuilder.build("   "))
        assertFalse(SearchQueryBuilder.isSearchable(""))
    }

    @Test
    fun `single term becomes a quoted prefix term`() {
        assertEquals("\"chem\"*", SearchQueryBuilder.build("chem"))
    }

    @Test
    fun `multiple terms are ANDed`() {
        assertEquals("\"chem\"* AND \"exam\"*", SearchQueryBuilder.build("chem exam"))
    }

    @Test
    fun `quotes and grouping characters are stripped`() {
        val built = SearchQueryBuilder.build("""che"m (group)""")
        assertFalse("unbalanced quote survived", built.contains("\"\""))
        assertFalse("grouping paren survived", built.contains("(") || built.contains(")"))
        assertTrue(built.startsWith("\""))
    }

    @Test
    fun `every emitted term is quoted so operators stay literal`() {
        val built = SearchQueryBuilder.build("NEAR OR AND NOT")
        // Each token must be wrapped, which is what makes OR/AND/NEAR literals
        // rather than FTS4 operators.
        built.split(" AND ").forEach { term ->
            assertTrue("unquoted term: $term", term.startsWith("\"") && term.endsWith("\"*"))
        }
    }

    @Test
    fun `quote count is always even`() {
        listOf("a", "a b", "\"", "a\"b", "((()))", "chem exam final").forEach { input ->
            val built = SearchQueryBuilder.build(input)
            assertEquals(
                "odd number of quotes for input [$input]: $built",
                0,
                built.count { it == '"' } % 2
            )
        }
    }

    @Test
    fun `duplicates collapse`() {
        assertEquals("\"a\"*", SearchQueryBuilder.build("a a a"))
    }

    @Test
    fun `term count is capped`() {
        val built = SearchQueryBuilder.build((1..30).joinToString(" ") { "t$it" })
        assertEquals(8, built.split(" AND ").size)
    }

    @Test
    fun `input of only punctuation is not searchable`() {
        assertEquals("", SearchQueryBuilder.build("""\"\"\"()*:"""))
        assertFalse(SearchQueryBuilder.isSearchable("***"))
    }

    @Test
    fun `hyphen is preserved inside a word`() {
        assertEquals("\"full-screen\"*", SearchQueryBuilder.build("full-screen"))
    }

    @Test
    fun `arabic input is preserved`() {
        assertEquals("\"كيمياء\"*", SearchQueryBuilder.build("كيمياء"))
    }

    @Test
    fun `mixed arabic and latin input yields separate quoted terms`() {
        assertEquals("\"chem\"* AND \"كيمياء\"*", SearchQueryBuilder.build("chem كيمياء"))
    }
}
