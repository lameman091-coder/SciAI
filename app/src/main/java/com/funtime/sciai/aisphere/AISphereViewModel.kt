package com.funtime.sciai.aisphere

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Central state holder for the AI Sphere Companion.
 * Manages emotion state, messages, position, preferences, behavior ticks,
 * affection tracking, sound effects, and emotion blending.
 */
class AISphereViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AISpherePreferences(application)
    val soundManager = SoundEffectManager(application)

    // ── Intelligence & Gamification ──────────────────────────────────
    private val intelligenceManager = com.funtime.sciai.data.IntelligenceManager(application)
    private val gamificationManager = com.funtime.sciai.data.GamificationManager(application)

    // ── State flows ─────────────────────────────────────────────────

    private val _emotionState = MutableStateFlow(EmotionState.IDLE)
    val emotionState: StateFlow<EmotionState> = _emotionState.asStateFlow()

    private val _currentMessage = MutableStateFlow<SphereMessage?>(null)
    val currentMessage: StateFlow<SphereMessage?> = _currentMessage.asStateFlow()

    private val _isVisible = MutableStateFlow(prefs.isEnabled)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    private val _isMuted = MutableStateFlow(prefs.isMuted)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(prefs.isSoundEnabled)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _spherePosition = MutableStateFlow(
        Offset(prefs.positionX, prefs.positionY)
    )
    val spherePosition: StateFlow<Offset> = _spherePosition.asStateFlow()

    private val _showQuickActions = MutableStateFlow(false)
    val showQuickActions: StateFlow<Boolean> = _showQuickActions.asStateFlow()

    private val _petHappiness = MutableStateFlow(prefs.happinessLevel)
    val petHappiness: StateFlow<Float> = _petHappiness.asStateFlow()

    private val _affectionLevel = MutableStateFlow(prefs.affectionLevel)
    val affectionLevel: StateFlow<Float> = _affectionLevel.asStateFlow()

    private val _reduceAnimations = MutableStateFlow(prefs.reduceAnimations)
    val reduceAnimations: StateFlow<Boolean> = _reduceAnimations.asStateFlow()

    private val _personalityMode = MutableStateFlow(prefs.personalityMode)
    val personalityMode: StateFlow<PersonalityMode> = _personalityMode.asStateFlow()

    private val _isEnabled = MutableStateFlow(prefs.isEnabled)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // ── Companion profile ────────────────────────────────────────────

    private val _companionProfile = MutableStateFlow(prefs.getCompanionProfile())
    val companionProfile: StateFlow<CompanionProfile> = _companionProfile.asStateFlow()

    private val _hasCompletedSetup = MutableStateFlow(prefs.hasCompletedSetup)
    val hasCompletedSetup: StateFlow<Boolean> = _hasCompletedSetup.asStateFlow()

    // ── Internal state ──────────────────────────────────────────────

    private var lastGesture = GestureType.NONE
    private var lastGestureTime = System.currentTimeMillis()
    private var lastMessageTime = 0L
    private var currentScreen = "home"
    private var visitedScreens = mutableSetOf<String>()
    private var sessionStartTime = System.currentTimeMillis()
    private var isDragging = false

    // Override emotion (from rapid-tap, double-tap, etc.)
    private var overrideEmotion: EmotionState? = null
    private var overrideEmotionTime = 0L

    // Detectors
    private val rapidTapDetector = RapidTapDetector()
    private val doubleTapDetector = DoubleTapDetector()

    private var behaviorTickJob: Job? = null
    private var messageAutoDismissJob: Job? = null
    private var previousEmotionForSound: EmotionState = EmotionState.IDLE

    init {
        viewModelScope.launch(Dispatchers.IO) {
            soundManager.initialize()
            soundManager.setSoundEnabled(prefs.isSoundEnabled)
            
            withContext(Dispatchers.Main) {
                startBehaviorLoop()

                // Show greeting on first launch
                if (prefs.isFirstLaunch) {
                    delay(1500)
                    showMessage(MessageContext.GREETING)
                    prefs.isFirstLaunch = false
                }

                // Check if user hasn't visited in a while → mood memory message
                val timeSinceLastVisit = System.currentTimeMillis() - prefs.lastVisitTime
                if (timeSinceLastVisit > 24 * 60 * 60 * 1000L && !prefs.isFirstLaunch) {
                    delay(2000)
                    showMessage(MessageContext.IDLE)
                }
                prefs.lastVisitTime = System.currentTimeMillis()
            }
        }
    }

    // ── Behavior loop ───────────────────────────────────────────────

    private fun startBehaviorLoop() {
        behaviorTickJob?.cancel()
        behaviorTickJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                if (!_isVisible.value || isDragging) continue

                val now = System.currentTimeMillis()
                val timeSinceInteraction = now - prefs.lastInteractionTime
                val gestureAge = now - lastGestureTime
                val overrideAge = now - overrideEmotionTime

                // ── Evaluate emotion ────────────────────────────
                val newEmotion = BehaviorEngine.evaluateEmotion(
                    petHappiness = _petHappiness.value,
                    affectionLevel = _affectionLevel.value,
                    timeSinceLastInteraction = timeSinceInteraction,
                    lastGesture = lastGesture,
                    gestureAge = gestureAge,
                    overrideEmotion = overrideEmotion,
                    overrideAge = overrideAge
                )

                if (newEmotion != _emotionState.value) {
                    _emotionState.value = newEmotion

                    // Play sound on emotion change (not during mute)
                    if (newEmotion != previousEmotionForSound && !_isMuted.value) {
                        soundManager.onEmotionChanged(newEmotion)
                    }
                    previousEmotionForSound = newEmotion
                }

                // Clear stale override
                if (overrideAge > 3000L) {
                    overrideEmotion = null
                }

                // ── Decay happiness ─────────────────────────────
                val decay = BehaviorEngine.calculateHappinessDecay(gestureAge)
                if (decay > 0f) {
                    val newHappiness = (_petHappiness.value - decay).coerceAtLeast(0f)
                    _petHappiness.value = newHappiness
                    prefs.happinessLevel = newHappiness
                }

                // ── Decay affection (very slow) ─────────────────
                val affectionDecay = BehaviorEngine.calculateAffectionDecay(timeSinceInteraction)
                if (affectionDecay > 0f) {
                    val newAffection = (_affectionLevel.value - affectionDecay).coerceAtLeast(0f)
                    _affectionLevel.value = newAffection
                    prefs.affectionLevel = newAffection
                }

                // ── Evaluate message trigger ────────────────────
                val timeSinceMessage = now - lastMessageTime
                val isFirstVisit = currentScreen !in visitedScreens

                val messageContext = BehaviorEngine.evaluateMessageTrigger(
                    timeSinceLastInteraction = timeSinceInteraction,
                    timeSinceLastMessage = timeSinceMessage,
                    lastGesture = lastGesture,
                    gestureAge = gestureAge,
                    currentScreen = currentScreen,
                    sessionDuration = now - sessionStartTime,
                    isFirstVisitToScreen = isFirstVisit,
                    isMuted = _isMuted.value,
                    totalInteractions = prefs.totalInteractions
                )

                if (messageContext != null) {
                    showMessage(messageContext)
                    if (isFirstVisit) visitedScreens.add(currentScreen)
                }

                // ── Study reminder check (every loop tick) ──────
                val hoursSinceStudy = intelligenceManager.getHoursSinceLastStudy()
                if (hoursSinceStudy > 0) {
                    val reminderContext = BehaviorEngine.evaluateStudyReminder(
                        hoursSinceLastStudy = hoursSinceStudy,
                        streakActive = gamificationManager.isStreakActive(),
                        timeSinceLastMessage = timeSinceMessage,
                        isMuted = _isMuted.value
                    )
                    if (reminderContext != null) {
                        showMessage(reminderContext)
                    }
                }

                // ── Smart suggestion check ──────────────────────
                val progressData = intelligenceManager.getProgressData()
                if (BehaviorEngine.shouldShowSmartSuggestion(
                        timeSinceLastMessage = timeSinceMessage,
                        hasWeakTopics = intelligenceManager.getWeakTopics().isNotEmpty(),
                        overallAccuracy = progressData.overallAccuracy,
                        totalQuestions = progressData.totalQuestions,
                        isMuted = _isMuted.value
                    )
                ) {
                    val suggestion = MessageEngine.getSmartSuggestionMessage(
                        personality = _personalityMode.value,
                        hasWeakTopics = intelligenceManager.getWeakTopics().isNotEmpty(),
                        overallAccuracy = progressData.overallAccuracy
                    )
                    if (suggestion != null) {
                        _currentMessage.value = suggestion
                        suggestion.emotion?.let {
                            overrideEmotion = it
                            overrideEmotionTime = System.currentTimeMillis()
                            _emotionState.value = it
                        }
                        lastMessageTime = System.currentTimeMillis()
                        autoCloseMessage()
                    }
                }
            }
        }
    }

    // ── Gesture handlers ────────────────────────────────────────────

    fun onTap() {
        val now = System.currentTimeMillis()
        lastGesture = GestureType.TAP
        lastGestureTime = now
        prefs.recordTap()
        prefs.recordAffection(GestureType.TAP)
        _affectionLevel.value = prefs.affectionLevel

        // Check for double-tap first (→ SHY)
        if (doubleTapDetector.onTap()) {
            overrideEmotion = EmotionState.SHY
            overrideEmotionTime = now
            _emotionState.value = EmotionState.SHY
            soundManager.onEmotionChanged(EmotionState.SHY)

            // Show shy message
            if (!_isMuted.value) {
                showEmotionMessage(EmotionState.SHY)
            }
            return
        }

        // Check for rapid tapping (→ ANGRY)
        if (rapidTapDetector.onTap()) {
            overrideEmotion = EmotionState.ANGRY
            overrideEmotionTime = now
            _emotionState.value = EmotionState.ANGRY
            soundManager.onEmotionChanged(EmotionState.ANGRY)

            // Show angry message
            if (!_isMuted.value) {
                showEmotionMessage(EmotionState.ANGRY)
            }
            return
        }

        // Normal tap → happiness bump
        val newHappiness = (_petHappiness.value + 0.05f).coerceAtMost(1f)
        _petHappiness.value = newHappiness
        prefs.happinessLevel = newHappiness

        _emotionState.value = EmotionState.HAPPY
        soundManager.onEmotionChanged(EmotionState.HAPPY)

        // Show contextual message on tap
        if (!_isMuted.value) {
            val screenTip = MessageEngine.getScreenTip(currentScreen, _personalityMode.value)
            if (screenTip != null && System.currentTimeMillis() - lastMessageTime > 5000) {
                // Apply tip emotion if present
                screenTip.emotion?.let {
                    overrideEmotion = it
                    overrideEmotionTime = System.currentTimeMillis()
                    _emotionState.value = it
                }
                
                _currentMessage.value = screenTip
                lastMessageTime = System.currentTimeMillis()
                autoCloseMessage()
            }
        }

        // Playful mode special burst trigger
        if (_personalityMode.value == PersonalityMode.PLAYFUL && (prefs.totalInteractions % 12 == 0)) {
            triggerBurstAnimation(if (System.currentTimeMillis() % 2 == 0L) EmotionState.PLAYFUL_EVIL else EmotionState.JUGGLING)
        }
    }

    fun onLongPress() {
        val now = System.currentTimeMillis()
        lastGesture = GestureType.LONG_PRESS
        lastGestureTime = now
        prefs.recordInteraction()
        prefs.recordAffection(GestureType.LONG_PRESS)
        _affectionLevel.value = prefs.affectionLevel

        // Long hold → LOVE (if affection is high enough)
        if (_affectionLevel.value > 0.3f) {
            overrideEmotion = EmotionState.LOVE
            overrideEmotionTime = now
            _emotionState.value = EmotionState.LOVE
            soundManager.onEmotionChanged(EmotionState.LOVE)

            if (!_isMuted.value) {
                showEmotionMessage(EmotionState.LOVE)
            }
        } else {
            _showQuickActions.value = true
        }
    }

    fun onDragStart() {
        isDragging = true
        lastGesture = GestureType.DRAG
        lastGestureTime = System.currentTimeMillis()
    }

    fun onDrag(delta: Offset) {
        val current = _spherePosition.value
        _spherePosition.value = Offset(current.x + delta.x, current.y + delta.y)
    }

    fun onDragEnd() {
        isDragging = false
        val pos = _spherePosition.value
        prefs.positionX = pos.x
        prefs.positionY = pos.y
        prefs.recordInteraction()
    }

    fun onPet() {
        lastGesture = GestureType.PET
        lastGestureTime = System.currentTimeMillis()
        prefs.recordPet()
        prefs.recordAffection(GestureType.PET)
        _affectionLevel.value = prefs.affectionLevel

        val newHappiness = (_petHappiness.value + 0.15f).coerceAtMost(1f)
        _petHappiness.value = newHappiness
        prefs.happinessLevel = newHappiness

        // Petting with high affection → LOVE, otherwise EXCITED
        if (_affectionLevel.value > 0.6f) {
            _emotionState.value = EmotionState.LOVE
            soundManager.onEmotionChanged(EmotionState.LOVE)
        } else {
            soundManager.onEmotionChanged(EmotionState.EXCITED)
        }

        showMessage(MessageContext.PETTING)
        
        // Playful mode special burst trigger on petting
        if (_personalityMode.value == PersonalityMode.PLAYFUL && (prefs.totalInteractions % 7 == 0)) {
            triggerBurstAnimation(EmotionState.JUGGLING)
        }
    }

    /**
     * Trigger a short-lived special animation (2.5 seconds).
     */
    fun triggerBurstAnimation(emotion: EmotionState) {
        val now = System.currentTimeMillis()
        overrideEmotion = emotion
        overrideEmotionTime = now
        _emotionState.value = emotion
        soundManager.onEmotionChanged(emotion)
        
        // Optional: show a special message for the burst
        if (!_isMuted.value && emotion == EmotionState.PLAYFUL_EVIL) {
            _currentMessage.value = SphereMessage(
                "Mwhehehe! FEAR ME! 😈",
                MessageContext.EMOTION_REACTION,
                priority = 3,
                emotion = EmotionState.PLAYFUL_EVIL
            )
            lastMessageTime = now
            autoCloseMessage()
        }
    }

    // ── Navigation ──────────────────────────────────────────────────

    fun onScreenChanged(route: String) {
        currentScreen = route
        lastGestureTime = System.currentTimeMillis()
        prefs.recordInteraction()
    }

    fun dismissQuickActions() {
        _showQuickActions.value = false
    }

    // ── Message management ──────────────────────────────────────────

    private fun showMessage(context: MessageContext) {
        val message = MessageEngine.getMessage(
            context = context,
            personality = _personalityMode.value,
            currentScreen = currentScreen,
            totalInteractions = prefs.totalInteractions
        ) ?: return

        _currentMessage.value = message
        
        // Apply message emotion to face
        message.emotion?.let {
            overrideEmotion = it
            overrideEmotionTime = System.currentTimeMillis()
            _emotionState.value = it
        }

        lastMessageTime = System.currentTimeMillis()
        autoCloseMessage()
    }

    /**
     * Show an emotion-specific reaction message.
     */
    private fun showEmotionMessage(emotion: EmotionState) {
        val message = MessageEngine.getEmotionReactionMessage(
            emotion = emotion,
            personality = _personalityMode.value,
            totalInteractions = prefs.totalInteractions
        ) ?: return

        _currentMessage.value = message
        lastMessageTime = System.currentTimeMillis()
        autoCloseMessage()
    }

    fun dismissMessage() {
        _currentMessage.value = null
    }

    private fun autoCloseMessage() {
        messageAutoDismissJob?.cancel()
        messageAutoDismissJob = viewModelScope.launch {
            delay(4000)
            _currentMessage.value = null
        }
    }

    // ── User controls ───────────────────────────────────────────────

    fun toggleVisibility() {
        val newVisible = !_isVisible.value
        _isVisible.value = newVisible
        prefs.isEnabled = newVisible
        _isEnabled.value = newVisible
        if (!newVisible) {
            _showQuickActions.value = false
            _currentMessage.value = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        _isVisible.value = enabled
        prefs.isEnabled = enabled
        if (!enabled) {
            _showQuickActions.value = false
            _currentMessage.value = null
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        prefs.isMuted = newMuted
        if (newMuted) {
            _currentMessage.value = null
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        prefs.isMuted = muted
        if (muted) _currentMessage.value = null
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        prefs.isSoundEnabled = enabled
        soundManager.setSoundEnabled(enabled)
    }

    fun setReduceAnimations(enabled: Boolean) {
        _reduceAnimations.value = enabled
        prefs.reduceAnimations = enabled
    }

    fun setPersonalityMode(mode: PersonalityMode) {
        _personalityMode.value = mode
        prefs.personalityMode = mode
    }

    // ── Position ────────────────────────────────────────────────────

    fun initializePosition(screenWidth: Float, screenHeight: Float) {
        val currentPos = _spherePosition.value
        if (currentPos.x < 0 || currentPos.y < 0) {
            val defaultX = screenWidth - 100f
            val defaultY = screenHeight - 200f
            _spherePosition.value = Offset(defaultX, defaultY)
            prefs.positionX = defaultX
            prefs.positionY = defaultY
        }
    }

    fun clampPosition(maxWidth: Float, maxHeight: Float, sphereSize: Float) {
        val pos = _spherePosition.value
        val clampedX = pos.x.coerceIn(0f, maxWidth - sphereSize)
        val clampedY = pos.y.coerceIn(0f, maxHeight - sphereSize)
        _spherePosition.value = Offset(clampedX, clampedY)
    }

    override fun onCleared() {
        super.onCleared()
        behaviorTickJob?.cancel()
        messageAutoDismissJob?.cancel()
        soundManager.release()
    }

    // ── Companion profile management ────────────────────────────────

    fun updateCompanionProfile(profile: CompanionProfile) {
        _companionProfile.value = profile
        prefs.saveCompanionProfile(profile)
    }

    fun completeSetup() {
        _hasCompletedSetup.value = true
        prefs.hasCompletedSetup = true
    }

    // ── Cognitive Assistant: Performance Events ──────────────────────

    /**
     * Called after a quiz is completed. Triggers emotional response
     * and contextual performance feedback message.
     *
     * @param accuracy Percentage accuracy (0-100)
     */
    fun onQuizCompleted(accuracy: Float) {
        viewModelScope.launch {
            // Evaluate emotional response based on performance
            val performanceEmotion = BehaviorEngine.evaluatePerformanceEmotion(
                accuracy = accuracy,
                streakActive = gamificationManager.isStreakActive(),
                affectionLevel = _affectionLevel.value
            )

            // Set emotion
            overrideEmotion = performanceEmotion
            overrideEmotionTime = System.currentTimeMillis()
            _emotionState.value = performanceEmotion
            soundManager.onEmotionChanged(performanceEmotion)

            // Show performance feedback message
            if (!_isMuted.value) {
                val message = MessageEngine.getPerformanceMessage(
                    accuracy = accuracy,
                    personality = _personalityMode.value
                )
                if (message != null) {
                    _currentMessage.value = message
                    message.emotion?.let {
                        overrideEmotion = it
                        overrideEmotionTime = System.currentTimeMillis()
                        _emotionState.value = it
                    }
                    lastMessageTime = System.currentTimeMillis()
                    autoCloseMessage()
                }
            }
        }
    }

    /**
     * Called after a theory test is submitted.
     * Triggers emotional response based on the evaluation score.
     *
     * @param score The evaluation score (0-100)
     */
    fun onTestSubmitted(score: Float) {
        onQuizCompleted(score) // Same logic, different source
    }

    /**
     * Force a study reminder check. Can be called from HomeScreen.
     */
    fun checkStudyReminder() {
        val hoursSinceStudy = intelligenceManager.getHoursSinceLastStudy()
        if (hoursSinceStudy > 12 && !_isMuted.value) {
            showMessage(MessageContext.STUDY_REMINDER)
        }
    }
}
