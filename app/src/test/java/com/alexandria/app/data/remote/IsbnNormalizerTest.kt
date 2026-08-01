package com.alexandria.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsbnNormalizerTest {

    @Test
    fun `isbn10 to isbn13 - clean digits`() {
        assertEquals("9780306406157", IsbnNormalizer.toIsbn13("0306406152"))
    }

    @Test
    fun `isbn10 with dashes and spaces`() {
        assertEquals("9788420413822", IsbnNormalizer.toIsbn13("84-204-1382-4"))
    }

    @Test
    fun `isbn13 passes through`() {
        assertEquals("9780306406157", IsbnNormalizer.toIsbn13("9780306406157"))
    }

    @Test
    fun `invalid values return null`() {
        assertNull(IsbnNormalizer.toIsbn13("1234"))
        assertNull(IsbnNormalizer.toIsbn13(""))
        assertNull(IsbnNormalizer.toIsbn13("9780306406158"))
        assertNull(IsbnNormalizer.toIsbn13("12X"))
    }
}
