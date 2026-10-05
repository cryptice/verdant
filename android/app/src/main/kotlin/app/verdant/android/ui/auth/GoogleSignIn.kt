package app.verdant.android.ui.auth

import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

/** Explicit button flow also works when Google bottom-sheet prompts are disabled. */
internal fun googleButtonSignInRequest(webClientId: String): GetCredentialRequest {
    require(webClientId.isNotBlank()) { "Google web client ID is missing" }
    return GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
}
