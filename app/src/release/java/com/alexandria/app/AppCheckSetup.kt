package com.alexandria.app

fun setupAppCheck() {
    // App Check deshabilitado en release: la Cloud Function resolveBook usa
    // enforceAppCheck = false, por lo que no se debe enviar token de App Check.
}
