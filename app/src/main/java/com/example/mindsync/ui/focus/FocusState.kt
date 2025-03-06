package com.example.mindsync.ui.focus

sealed class FocusState {
    object Stopped : FocusState()
    data class Running(val durationInSeconds: Int) : FocusState()
    data class Paused(val remainingSeconds: Int) : FocusState()
    data class Completed(val durationInSeconds: Int) : FocusState()
} 