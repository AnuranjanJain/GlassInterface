package com.glassinterface.core.voice

/**
 * Parses raw speech-to-text output into structured [VoiceCommand] intents.
 *
 * Uses keyword matching — fast and works fully offline.
 * The parser is intentionally forgiving: "save my face" and "remember face" both match.
 */
object VoiceCommandParser {

    fun parse(text: String): VoiceCommand {
        val lower = text.lowercase().trim()

        return when {
            // Face commands
            lower.containsAny("save face", "save my face", "remember this person", "remember face", "save this face") ->
                VoiceCommand(CommandType.SAVE_FACE, extractAfter(lower, "as", "named"))

            lower.containsAny("who is this", "who is that", "identify", "recognize") ->
                VoiceCommand(CommandType.IDENTIFY_FACE)

            // Object commands
            lower.containsAny("save this", "save object", "remember this object", "remember this", "save what i see") ->
                VoiceCommand(CommandType.SAVE_OBJECT)

            // Contact commands
            lower.containsAny("save contact", "add contact", "remember contact") ->
                VoiceCommand(CommandType.SAVE_CONTACT, extractAfter(lower, "contact", "named", "called"))

            // Location commands
            lower.containsAny("save location", "remember this place", "save this place", "mark location", "save where i am") ->
                VoiceCommand(CommandType.SAVE_LOCATION, extractAfter(lower, "as", "called"))

            // Timestamp commands
            lower.containsAny("save time", "mark time", "save timestamp", "mark timestamp", "remember time") ->
                VoiceCommand(CommandType.SAVE_TIMESTAMP, extractAfter(lower, "as", "note"))

            // Note commands
            lower.containsAny("save note", "take note", "note that", "remember that") ->
                VoiceCommand(CommandType.SAVE_NOTE, extractAfter(lower, "note", "that"))

            // Describe scene
            lower.containsAny("what do you see", "describe", "what's around", "what is around", "tell me what you see", "scene") ->
                VoiceCommand(CommandType.DESCRIBE_SCENE)

            // List memories
            lower.containsAny("list memories", "what did i save", "my memories", "what do i have", "list saved", "show memories") ->
                VoiceCommand(CommandType.LIST_MEMORIES)

            // Help
            lower.containsAny("help", "what can you do", "commands", "options") ->
                VoiceCommand(CommandType.HELP)

            else -> VoiceCommand(CommandType.UNKNOWN, lower)
        }
    }

    /**
     * Extract the payload text after any of the given keywords.
     * e.g. "save face as John" → "John"
     */
    private fun extractAfter(text: String, vararg keywords: String): String {
        for (keyword in keywords) {
            val idx = text.indexOf(keyword)
            if (idx >= 0) {
                val after = text.substring(idx + keyword.length).trim()
                if (after.isNotBlank()) return after
            }
        }
        return ""
    }

    private fun String.containsAny(vararg phrases: String): Boolean =
        phrases.any { this.contains(it) }
}
