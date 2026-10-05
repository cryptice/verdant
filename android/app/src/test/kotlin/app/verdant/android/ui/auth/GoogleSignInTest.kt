package app.verdant.android.ui.auth

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInTest {
    @Test fun `manual sign-in requests the button flow rather than bottom-sheet discovery`() {
        val request = googleButtonSignInRequest("test.apps.googleusercontent.com")
        assertEquals(1, request.credentialOptions.size)
        val option = request.credentialOptions.single()
        assertTrue(option is GetSignInWithGoogleOption)
        assertEquals("test.apps.googleusercontent.com", (option as GetSignInWithGoogleOption).serverClientId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing client configuration is rejected before requesting credentials`() {
        googleButtonSignInRequest(" ")
    }
}
