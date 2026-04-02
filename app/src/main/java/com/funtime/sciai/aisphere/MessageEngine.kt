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
    val priority: Int = 0,
    val emotion: EmotionState? = null
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
            SphereMessage("Bored yet? Tap me! 🎯", MessageContext.IDLE, emotion = EmotionState.CONFUSED),
            SphereMessage("I'm just floating here... doing sphere things 🫧", MessageContext.IDLE, emotion = EmotionState.IDLE),
            SphereMessage("Plot twist: I'm the main character 😎", MessageContext.IDLE, emotion = EmotionState.HAPPY),
            SphereMessage("If I had legs, I'd be pacing right now >w<", MessageContext.IDLE, emotion = EmotionState.SHY)
        ),
        MessageContext.NAVIGATION to listOf(
            SphereMessage("Lost? I got you! Long-press me 🧭", MessageContext.NAVIGATION, emotion = EmotionState.HAPPY),
            SphereMessage("Need a shortcut? I'm basically GPS ✨", MessageContext.NAVIGATION, emotion = EmotionState.EXCITED),
            SphereMessage("Even Google Maps can't navigate this well 💅", MessageContext.NAVIGATION, emotion = EmotionState.HAPPY)
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            SphereMessage("You've been studying like a legend 🔥", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY),
            SphereMessage("Your brain cells are doing overtime 🧠", MessageContext.ENCOURAGEMENT, emotion = EmotionState.EXCITED),
            SphereMessage("Science won't know what hit it 💪", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY),
            SphereMessage("Big brain energy detected 📡 ★‿★", MessageContext.ENCOURAGEMENT, emotion = EmotionState.EXCITED)
        ),
        MessageContext.GREETING to listOf(
            SphereMessage("Hey there, genius! Ready to learn? 🚀", MessageContext.GREETING, emotion = EmotionState.HAPPY),
            SphereMessage("The sphere is back! Miss me? >w<", MessageContext.GREETING, emotion = EmotionState.SHY),
            SphereMessage("Welcome back! Let's science! 🧪", MessageContext.GREETING, emotion = EmotionState.HAPPY)
        ),
        MessageContext.PETTING to listOf(
            SphereMessage("Hehe, that tickles! >w<", MessageContext.PETTING, emotion = EmotionState.HAPPY),
            SphereMessage("I'm blushing! Wait, can spheres blush? 🟠", MessageContext.PETTING, emotion = EmotionState.SHY),
            SphereMessage("Best. Pet. Ever. ♡‿♡", MessageContext.PETTING, emotion = EmotionState.LOVE),
            SphereMessage("More! More! MORE! ★‿★", MessageContext.PETTING, emotion = EmotionState.EXCITED)
        ),
        MessageContext.HELP to listOf(
            SphereMessage("Need help? That's literally my job 📋", MessageContext.HELP, emotion = EmotionState.HAPPY),
            SphereMessage("Stuck? Let me help! Long-press for options ⚡", MessageContext.HELP, emotion = EmotionState.CONFUSED),
            SphereMessage("Tap me if you need anything! ^_^", MessageContext.HELP, emotion = EmotionState.IDLE)
        ),
        MessageContext.TIP to listOf(
            SphereMessage("Try the Expert mode for deep analysis 🔬", MessageContext.TIP, emotion = EmotionState.HAPPY),
            SphereMessage("Check out research articles 📚", MessageContext.TIP, emotion = EmotionState.IDLE),
            SphereMessage("Upload PDFs in the Library 📖", MessageContext.TIP, emotion = EmotionState.HAPPY),
            SphereMessage("You can drag me anywhere on screen! ✋", MessageContext.TIP, emotion = EmotionState.HAPPY)
        ),
        MessageContext.FAREWELL to listOf(
            SphereMessage("Going so soon? I'll miss you! ;_;", MessageContext.FAREWELL, emotion = EmotionState.SAD),
            SphereMessage("See you later, scientist! 👋 ^_^", MessageContext.FAREWELL, emotion = EmotionState.HAPPY)
        )
    )

    private val calmMessages = mapOf(
        MessageContext.IDLE to listOf(
            SphereMessage("Take your time — I'm here when you need me ✨", MessageContext.IDLE, emotion = EmotionState.IDLE),
            SphereMessage("Need help navigating? Just tap ^_^", MessageContext.IDLE, emotion = EmotionState.HAPPY),
            SphereMessage("I'm here if you need anything", MessageContext.IDLE, emotion = EmotionState.IDLE)
        ),
        MessageContext.NAVIGATION to listOf(
            SphereMessage("Long-press me for navigation options 🧭", MessageContext.NAVIGATION, emotion = EmotionState.HAPPY),
            SphereMessage("I can guide you — just ask", MessageContext.NAVIGATION, emotion = EmotionState.IDLE)
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            SphereMessage("You're doing well — keep going 🌱", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY),
            SphereMessage("Great progress today 📈", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY),
            SphereMessage("Steady learning pays off ✨", MessageContext.ENCOURAGEMENT, emotion = EmotionState.IDLE)
        ),
        MessageContext.GREETING to listOf(
            SphereMessage("Welcome back ^_^", MessageContext.GREETING, emotion = EmotionState.HAPPY),
            SphereMessage("Good to see you again ✨", MessageContext.GREETING, emotion = EmotionState.IDLE),
            SphereMessage("Ready when you are 🎯", MessageContext.GREETING, emotion = EmotionState.HAPPY)
        ),
        MessageContext.PETTING to listOf(
            SphereMessage("That's nice, thank you ♡", MessageContext.PETTING, emotion = EmotionState.LOVE),
            SphereMessage("I appreciate that ♡‿♡", MessageContext.PETTING, emotion = EmotionState.LOVE),
            SphereMessage("You're kind ☺️", MessageContext.PETTING, emotion = EmotionState.HAPPY)
        ),
        MessageContext.HELP to listOf(
            SphereMessage("Long-press me for quick navigation 🧭", MessageContext.HELP, emotion = EmotionState.HAPPY),
            SphereMessage("I'm here to help — tap anytime", MessageContext.HELP, emotion = EmotionState.IDLE)
        ),
        MessageContext.TIP to listOf(
            SphereMessage("Try uploading a PDF to the Library 📚", MessageContext.TIP, emotion = EmotionState.HAPPY),
            SphereMessage("Expert mode gives deeper answers 🔬", MessageContext.TIP, emotion = EmotionState.HAPPY),
            SphereMessage("Explore research articles for more context 📖", MessageContext.TIP, emotion = EmotionState.IDLE)
        ),
        MessageContext.FAREWELL to listOf(
            SphereMessage("Take care! See you soon ✨", MessageContext.FAREWELL, emotion = EmotionState.HAPPY),
            SphereMessage("Rest well 🌙", MessageContext.FAREWELL, emotion = EmotionState.SLEEP)
        )
    )

    private val studyMessages = mapOf(
        MessageContext.IDLE to listOf(
            SphereMessage("Focus mode active 📖", MessageContext.IDLE, emotion = EmotionState.IDLE),
            SphereMessage("Need a research shortcut? Tap me", MessageContext.IDLE, emotion = EmotionState.HAPPY)
        ),
        MessageContext.ENCOURAGEMENT to listOf(
            SphereMessage("Good research session 📈", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY),
            SphereMessage("Keep analyzing 🔬", MessageContext.ENCOURAGEMENT, emotion = EmotionState.HAPPY)
        ),
        MessageContext.GREETING to listOf(
            SphereMessage("Study session started 📚", MessageContext.GREETING, emotion = EmotionState.IDLE)
        ),
        MessageContext.PETTING to listOf(
            SphereMessage("Focus maintained ✓", MessageContext.PETTING, emotion = EmotionState.HAPPY)
        ),
        MessageContext.TIP to listOf(
            SphereMessage("Try Expert mode for deeper citations 🔬", MessageContext.TIP, emotion = EmotionState.HAPPY),
            SphereMessage("Upload PDFs for focused analysis 📖", MessageContext.TIP, emotion = EmotionState.HAPPY)
        )
    )

    // Silent mode returns no messages
    private val silentMessages = emptyMap<MessageContext, List<SphereMessage>>()

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

        val message = selectRelevantMessage(messages, currentScreen, totalInteractions)

        return message.copy(
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

    private fun selectRelevantMessage(
        messages: List<SphereMessage>,
        currentScreen: String,
        totalInteractions: Int
    ): SphereMessage {
        val index = (totalInteractions + currentScreen.hashCode().and(0x7FFFFFFF)) % messages.size
        return messages[index]
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
                PersonalityMode.PLAYFUL -> SphereMessage("This is Mission Control. What shall we research? 🚀", MessageContext.TIP, emotion = EmotionState.HAPPY)
                PersonalityMode.CALM -> SphereMessage("Search anything scientific here ✨", MessageContext.TIP, emotion = EmotionState.HAPPY)
                PersonalityMode.STUDY -> SphereMessage("Enter your research query 📖", MessageContext.TIP, emotion = EmotionState.IDLE)
                else -> return null
            }
            currentScreen.startsWith("library") -> when (personality) {
                PersonalityMode.PLAYFUL -> SphereMessage("Your library! Upload a PDF and I'll eat it... I mean read it 📖", MessageContext.TIP, emotion = EmotionState.HAPPY)
                PersonalityMode.CALM -> SphereMessage("Upload PDFs here to ask questions about them 📚", MessageContext.TIP, emotion = EmotionState.IDLE)
                PersonalityMode.STUDY -> SphereMessage("PDF analysis engine ready 📖", MessageContext.TIP, emotion = EmotionState.HAPPY)
                else -> return null
            }
            currentScreen.startsWith("articles") -> when (personality) {
                PersonalityMode.PLAYFUL -> SphereMessage("Research papers! Where boring gets interesting 🤓", MessageContext.TIP, emotion = EmotionState.EXCITED)
                PersonalityMode.CALM -> SphereMessage("Browse peer-reviewed research articles 📄", MessageContext.TIP, emotion = EmotionState.IDLE)
                PersonalityMode.STUDY -> SphereMessage("Article search: PubMed + arXiv 🔬", MessageContext.TIP, emotion = EmotionState.HAPPY)
                else -> return null
            }
            currentScreen.startsWith("answer") -> when (personality) {
                PersonalityMode.PLAYFUL -> SphereMessage("Let the AI brain do its thing... 🧠💨", MessageContext.TIP, emotion = EmotionState.HAPPY)
                PersonalityMode.CALM -> SphereMessage("Your answer is being prepared ✨", MessageContext.TIP, emotion = EmotionState.IDLE)
                PersonalityMode.STUDY -> SphereMessage("Processing query... ⏳", MessageContext.TIP, emotion = EmotionState.IDLE)
                else -> return null
            }
            else -> return null
        }

        return tip
    }


}
