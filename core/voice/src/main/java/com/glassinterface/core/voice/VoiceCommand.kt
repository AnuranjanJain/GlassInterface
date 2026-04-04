package com.glassinterface.core.voice

/**
 * Parsed voice command intent with optional payload extracted from speech.
 */
data class VoiceCommand(
    val type: CommandType,
    val payload: String = ""
)

/**
 * All supported voice command intents.
 */
enum class CommandType {
    /** "save face" / "remember this person" — crop + save face */
    SAVE_FACE,
    /** "save this" / "remember this object" — crop + save object */
    SAVE_OBJECT,
    /** "save contact [name]" — store a contact */
    SAVE_CONTACT,
    /** "save location" / "remember this place" — save GPS */
    SAVE_LOCATION,
    /** "save time" / "mark timestamp" — save current time */
    SAVE_TIMESTAMP,
    /** "note [text]" / "save note [text]" — free-form note */
    SAVE_NOTE,
    /** "what do you see?" / "describe" — read scene aloud */
    DESCRIBE_SCENE,
    /** "who is this?" — identify the face in frame */
    IDENTIFY_FACE,
    /** "what did I save?" / "list memories" — summary of saves */
    LIST_MEMORIES,
    /** "help" — list available commands */
    HELP,
    /** Unrecognised speech */
    UNKNOWN
}
