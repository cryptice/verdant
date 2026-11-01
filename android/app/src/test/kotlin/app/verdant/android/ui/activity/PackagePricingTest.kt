package app.verdant.android.ui.activity

import org.junit.Assert.*
import org.junit.Test

class PackagePricingTest {
    @Test fun `a price per seed gives the package price`() {
        assertEquals("39", packagePriceFor("0,78", 50))
        assertEquals("", packagePriceFor("0,78", 0))
        assertEquals("", packagePriceFor("", 50))
    }

    @Test fun `a package price gives the price per seed, rounded to öre`() {
        assertEquals("0.99", perSeedPriceFor("39,50", 40))
        assertNull(perSeedPriceFor("39", 0))
    }

    @Test fun `kronor and öre convert both ways`() {
        assertEquals(78, sekToCents("0,78"))
        assertEquals(4500, sekToCents("45"))
        assertNull(sekToCents("abc"))
        assertEquals("0.78", centsToSek(78))
        assertEquals("45", centsToSek(4500))
        assertEquals("", centsToSek(null))
    }
}
