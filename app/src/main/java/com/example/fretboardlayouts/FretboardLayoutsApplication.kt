package com.example.fretboardlayouts

// ================================================================
// APPLICATION CLASS
// NEW made by Claude 18/08/2026
//
// Holds the single SessionState instance that lives for the entire
// process lifetime. Both MainActivity and JamLabActivity access it
// via: LocalContext.current.applicationContext as FretboardLayoutsApplication
//
// Registered in AndroidManifest.xml via android:name=".FretboardLayoutsApplication"
// ================================================================

import android.app.Application

class FretboardLayoutsApplication : Application() {
    val session = SessionState()
}
