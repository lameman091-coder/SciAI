package com.funtime.sciai.aisphere

/**
 * Context-aware message system for the AI Sphere.
 * Provides micro-messages based on emotion, personality, screen context,
 * and interaction patterns.
 *
 * Personality modes:
 * - PLAYFUL: Bubbly, kaomoji-heavy, anime-inspired
 * - CALM: Warm, gentle, supportive
 * - STUDY: Minimal, academic hints
 * - SILENT: No messages
 */

data class SphereMessage(
    val text: String,
    val context: MessageContext,
    val priority: Int = 0
)

enum class MessageContext {
    IDLE, NAVIGATION, ENCOURAGEMENT, GREETING, PETTING, HELP, TIP, FAREWELL,
    EMOTION_REACTION
}

object MessageEngine {

    // ═══════════════════════════════════════════════════════════════
    // EMOTION REACTION MESSAGES — matched to each emotion state
    // ═══════════════════════════════════════════════════════════════

    private val playfulEmotionMessages = mapOf(
        EmotionState.HAPPY to listOf(
            "Hehe that felt nice >w<",
            "Yay! Keep doing that ^_^",
            "I'm so happy right now! ✨",
            "That tickles! Hehe~ >w<"
        ),
        EmotionState.LOVE to listOf(
            "I like being here with you ♡",
            "You're my favorite human ♡‿♡",
            "This feels so warm and nice~",
            "Stay with me a little longer? ♡"
        ),
        EmotionState.SHY to listOf(
            "E-ehh...! >~<",
            "S-stop that! ...actually don't >~<",
            "Why are you doing that?! >~<",
            "I'm not blushing! You're blushing! >~<"
        ),
        EmotionState.SAD to listOf(
            "You didn't tap me today... ;_;",
            "Hello...? Anyone there? ;_;",
            "I've been floating alone... ;_;",
            "Did you forget about me? ;_;"
        ),
        EmotionState.ANGRY to listOf(
            "Hey! That was too much >:(",
            "Ow ow ow! Stop tapping so hard! >:(",
            "I'm a sphere, not a drum! >:(",
            "Hmph! I need a moment... >:("
        ),
        EmotionState.SLEEP to listOf(
            "I'll just rest a bit... zZ",
            "So sleepy... mm... zZ",
            "Wake me up... later... zZ",
            "*tiny snore* ...zZ"
        ),
        EmotionState.EXCITED to listOf(
            "YESSS! That was amazing! ★‿★",
            "MORE! MORE! I LOVE THIS! ★‿★",
            "I'm literally GLOWING! ✨★‿★",
            "Best day ever!! ★‿★✨"
        ),
        EmotionState.CONFUSED to listOf(
            "Wait... what? o_O",
            "Huh? What was that? o_O",
            "I don't understand... o_O",
            "Something feels different? o_O"
        )
    )

    private val calmEmotionMessages = mapOf(
        EmotionState.HAPPY to listOf(
            "That was nice ^_^",
            "Thank you for that ☺️",
            "A pleasant moment ^_^"
        ),
        EmotionState.LOVE to listOf(
            "I'm happy to be with you ♡",
            "Your presence is calming ♡",
            "I appreciate you ♡"
        ),
        EmotionState.SHY to listOf(
            "Oh... >~<",
            "That's a bit much... >~<",
            "I'm a bit flustered >~<"
        ),
        EmotionState.SAD to listOf(
            "I've been waiting... ;_;",
            "It's been quiet... ;_;",
            "I hope you come back soon ;_;"
        ),
        EmotionState.ANGRY to listOf(
            "Please be gentler >:(",
            "That was a bit rough >:(",
            "I need a moment of calm >:("
        ),
        EmotionState.SLEEP to listOf(
            "Resting now... zZ",
            "Quietly drifting... zZ",
            "Peaceful silence... zZ"
        ),
        EmotionState.EXCITED to listOf(
            "How wonderful ★‿★",
            "That was delightful ★‿★",
            "Such a lovely feeling ★‿★"
        ),
        EmotionState.CONFUSED to listOf(
            "Hmm? o_O",
            "I'm not sure I follow o_O",
            "That's puzzling o_O"
        )
    )

    private val studyEmotionMessages = mapOf(
        EmotionState.HAPPY to listOf(
            "Good interaction noted ^_^",
            "Positive feedback registered ✓"
        ),
        EmotionState.LOVE to listOf(
            "Focus mode appreciated ♡"
        ),
        EmotionState.SHY to listOf(
            "...noted >~<"
        ),
        EmotionState.SAD to listOf(
            "Standing by ;_;"
        ),
        EmotionState.ANGRY to listOf(
            "Input overload detected >:("
        ),
        EmotionState.SLEEP to listOf(
            "Low power mode... zZ"
        ),
        EmotionState.EXCITED to listOf(
            "Optimal engagement ★‿★"
        ),
        EmotionState.CONFUSED to listOf(
            "Unexpected input o_O"
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // CONTEXTUAL MESSAGE POOLS
    // ═══════════════════════════════════════════════════════════════

    private val playfulMessages = mapOf(
        MessageContext.IDLE to listOf(
            "Bored yet? Tap me! 🎯",
            "I'm just floating here... doing sphere things 🫧",
            "Plot twist: I'm the main character 😎",
            "If I had legs, I'd be pacing right now >w<"
        ),
        MessageContext.NAVIGATION to listOf(
            "Lost? I got you! Long-press me 🧭",
            "Need a shortcut? I'm basically GPS ✨",
            "Even Google Maps can't navigate this well 💅"
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            "You've been studying like a legend 🔥",
            "Your brain cells are doing overtime 🧠",
            "Science won't know what hit it 💪",
            "Big brain energy detected 📡 ★‿★"
        ),
        MessageContext.GREETING to listOf(
            "Hey there, genius! Ready to learn? 🚀",
            "The sphere is back! Miss me? >w<",
            "Welcome back! Let's science! 🧪"
        ),
        MessageContext.PETTING to listOf(
            "Hehe, that tickles! >w<",
            "I'm blushing! Wait, can spheres blush? 🟠",
            "Best. Pet. Ever. ♡‿♡",
            "More! More! MORE! ★‿★"
        ),
        MessageContext.HELP to listOf(
            "Need help? That's literally my job 📋",
            "Stuck? Let me help! Long-press for options ⚡",
            "Tap me if you need anything! ^_^"
        ),
        MessageContext.TIP to listOf(
            "Try the Expert mode for deep analysis 🔬",
            "Check out research articles 📚",
            "Upload PDFs in the Library 📖",
            "You can drag me anywhere on screen! ✋"
        ),
        MessageContext.FAREWELL to listOf(
            "Going so soon? I'll miss you! ;_;",
            "See you later, scientist! 👋 ^_^"
        )
    )

    private val calmMessages = mapOf(
        MessageContext.IDLE to listOf(
            "Take your time — I'm here when you need me ✨",
            "Need help navigating? Just tap ^_^",
            "I'm here if you need anything"
        ),
        MessageContext.NAVIGATION to listOf(
            "Long-press me for navigation options 🧭",
            "I can guide you — just ask"
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            "You're doing well — keep going 🌱",
            "Great progress today 📈",
            "Steady learning pays off ✨"
        ),
        MessageContext.GREETING to listOf(
            "Welcome back ^_^",
            "Good to see you again ✨",
            "Ready when you are 🎯"
        ),
        MessageContext.PETTING to listOf(
            "That's nice, thank you ♡",
            "I appreciate that ♡‿♡",
            "You're kind ☺️"
        ),
        MessageContext.HELP to listOf(
            "Long-press me for quick navigation 🧭",
            "I'm here to help — tap anytime"
        ),
        MessageContext.TIP to listOf(
            "Try uploading a PDF to the Library 📚",
            "Expert mode gives deeper answers 🔬",
            "Explore research articles for more context 📖"
        ),
        MessageContext.FAREWELL to listOf(
            "Take care! See you soon ✨",
            "Rest well 🌙"
        )
    )

    private val studyMessages = mapOf(
        MessageContext.IDLE to listOf(
            "Focus mode active 📖",
            "Need a research shortcut? Tap me"
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            "Good research session 📈",
            "Keep analyzing 🔬"
        ),
        MessageContext.GREETING to listOf(
            "Study session started 📚"
        ),
        MessageContext.PETTING to listOf(
            "Focus maintained ✓"
        ),
        MessageContext.TIP to listOf(
            "Try Expert mode for deeper citations 🔬",
            "Upload PDFs for focused analysis 📖"
        )
    )

    // Silent mode returns no messages
    private val silentMessages = emptyMap<MessageContext, List<String>>()

    // ── Message selection logic ─────────────────────────────────────

    fun getMessage(
        context: MessageContext,
        personality: PersonalityMode,
        currentScreen: String = "",
        totalInteractions: Int = 0
    ): SphereMessage? {
        if (personality == PersonalityMode.SILENT) return null

        val pool = when (personality) {
            PersonalityMode.PLAYFUL -> playfulMessages
            PersonalityMode.CALM -> calmMessages
            PersonalityMode.STUDY -> studyMessages
            PersonalityMode.SILENT -> return null
        }

        val messages = pool[context] ?: return null
        if (messages.isEmpty()) return null

        val text = selectRelevantMessage(messages, currentScreen, totalInteractions)

        return SphereMessage(
            text = text,
            context = context,
            priority = when (context) {
                MessageContext.HELP -> 3
                MessageContext.GREETING -> 2
                MessageContext.PETTING -> 2
                MessageContext.EMOTION_REACTION -> 2
                MessageContext.ENCOURAGEMENT -> 1
                MessageContext.TIP -> 1
                else -> 0
            }
        )
    }

    /**
     * Get an emotion-specific reaction message.
     */
    fun getEmotionReactionMessage(
        emotion: EmotionState,
        personality: PersonalityMode,
        totalInteractions: Int = 0
    ): SphereMessage? {
        if (personality == PersonalityMode.SILENT) return null

        val pool = when (personality) {
            PersonalityMode.PLAYFUL -> playfulEmotionMessages
            PersonalityMode.CALM -> calmEmotionMessages
            PersonalityMode.STUDY -> studyEmotionMessages
            PersonalityMode.SILENT -> return null
        }

        val messages = pool[emotion] ?: return null
        if (messages.isEmpty()) return null

        val index = (totalInteractions + emotion.ordinal) % messages.size
        return SphereMessage(
            text = messages[index],
            context = MessageContext.EMOTION_REACTION,
            priority = 2
        )
    }

    /**
     * Get a screen-specific message for navigation tips.
     */
    fun getScreenTip(currentScreen: String, personality: PersonalityMode): SphereMessage? {
        if (personality == PersonalityMode.SILENT) return null

        val tip = when {
            currentScreen.startsWith("home") -> when (personality) {
                PersonalityMode.PLAYFUL -> "This is Mission Control. What shall we research? 🚀"
                PersonalityMode.CALM -> "Search anything scientific here ✨"
                PersonalityMode.STUDY -> "Enter your research query 📖"
                else -> return null
            }
            currentScreen.startsWith("library") -> when (personality) {
                PersonalityMode.PLAYFUL -> "Your library! Upload a PDF and I'll eat it... I mean read it 📖"
                PersonalityMode.CALM -> "Upload PDFs here to ask questions about them 📚"
                PersonalityMode.STUDY -> "PDF analysis engine ready 📖"
                else -> return null
            }
            currentScreen.startsWith("articles") -> when (personality) {
                PersonalityMode.PLAYFUL -> "Research papers! Where boring gets interesting 🤓"
                PersonalityMode.CALM -> "Browse peer-reviewed research articles 📄"
                PersonalityMode.STUDY -> "Article search: PubMed + arXiv 🔬"
                else -> return null
            }
            currentScreen.startsWith("answer") -> when (personality) {
                PersonalityMode.PLAYFUL -> "Let the AI brain do its thing... 🧠💨"
                PersonalityMode.CALM -> "Your answer is being prepared ✨"
                PersonalityMode.STUDY -> "Processing query... ⏳"
                else -> return null
            }
            else -> return null
        }

        return SphereMessage(text = tip, context = MessageContext.TIP, priority = 1)
    }

    private fun selectRelevantMessage(
        messages: List<String>,
        currentScreen: String,
        totalInteractions: Int
    ): String {
        val index = (totalInteractions + currentScreen.hashCode().and(0x7FFFFFFF)) % messages.size
        return messages[index]
    }
}
