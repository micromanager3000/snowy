package com.snowy.pet.ui

/** Snowy's emotional states from SOUL.md */
enum class Emotion {
    HAPPY, ECSTATIC, CURIOUS, PLAYFUL, CONTENT, SLEEPY, LONELY, CONFUSED, ALERT;

    companion object {
        fun fromString(s: String): Emotion = when (s.lowercase().trim()) {
            "happy" -> HAPPY
            "ecstatic" -> ECSTATIC
            "curious" -> CURIOUS
            "playful" -> PLAYFUL
            "content" -> CONTENT
            "sleepy" -> SLEEPY
            "lonely" -> LONELY
            "confused" -> CONFUSED
            "alert" -> ALERT
            else -> HAPPY
        }
    }
}

/**
 * Maps an emotion to concrete face parameters for rendering.
 * All values are 0f..1f unless noted.
 */
data class FaceParams(
    val eyeOpenness: Float = 0.8f,       // 0=closed, 1=wide open
    val pupilSize: Float = 0.5f,         // 0=tiny, 1=big
    val pupilOffsetX: Float = 0f,        // -1=left, 0=center, 1=right
    val pupilOffsetY: Float = 0f,        // -1=up, 0=center, 1=down
    val mouthCurve: Float = 0.5f,        // 0=frown, 0.5=neutral, 1=big smile
    val mouthOpen: Float = 0f,           // 0=closed, 1=wide open
    val earLeftAngle: Float = 0f,        // -1=drooped, 0=neutral, 1=perked
    val earRightAngle: Float = 0f,
    val tailWagSpeed: Float = 0.5f,      // 0=still, 1=max wag
    val tailHeight: Float = 0.5f,        // 0=tucked, 1=high
    val bgHue: Float = 220f,            // background tint hue (HSL)
    val bgSaturation: Float = 0.15f,
    val sparkle: Boolean = false,
)

fun Emotion.toFaceParams(): FaceParams = when (this) {
    Emotion.HAPPY -> FaceParams(
        eyeOpenness = 0.85f, pupilSize = 0.55f, mouthCurve = 0.8f,
        earLeftAngle = 0.3f, earRightAngle = 0.3f,
        tailWagSpeed = 0.7f, tailHeight = 0.7f,
        bgHue = 45f, bgSaturation = 0.12f
    )
    Emotion.ECSTATIC -> FaceParams(
        eyeOpenness = 1f, pupilSize = 0.7f, mouthCurve = 1f, mouthOpen = 0.4f,
        earLeftAngle = 0.6f, earRightAngle = 0.6f,
        tailWagSpeed = 1f, tailHeight = 1f,
        bgHue = 50f, bgSaturation = 0.2f, sparkle = true
    )
    Emotion.CURIOUS -> FaceParams(
        eyeOpenness = 0.95f, pupilSize = 0.65f, pupilOffsetX = 0.3f,
        mouthCurve = 0.55f,
        earLeftAngle = 0.5f, earRightAngle = -0.2f,
        tailWagSpeed = 0.4f, tailHeight = 0.6f,
        bgHue = 180f, bgSaturation = 0.1f
    )
    Emotion.PLAYFUL -> FaceParams(
        eyeOpenness = 0.9f, pupilSize = 0.6f, mouthCurve = 0.85f, mouthOpen = 0.2f,
        earLeftAngle = 0.4f, earRightAngle = 0.4f,
        tailWagSpeed = 0.9f, tailHeight = 0.8f,
        bgHue = 130f, bgSaturation = 0.12f
    )
    Emotion.CONTENT -> FaceParams(
        eyeOpenness = 0.6f, pupilSize = 0.45f, mouthCurve = 0.65f,
        earLeftAngle = 0f, earRightAngle = 0f,
        tailWagSpeed = 0.25f, tailHeight = 0.5f,
        bgHue = 30f, bgSaturation = 0.08f
    )
    Emotion.SLEEPY -> FaceParams(
        eyeOpenness = 0.25f, pupilSize = 0.35f, pupilOffsetY = 0.3f,
        mouthCurve = 0.45f,
        earLeftAngle = -0.6f, earRightAngle = -0.6f,
        tailWagSpeed = 0.05f, tailHeight = 0.3f,
        bgHue = 260f, bgSaturation = 0.1f
    )
    Emotion.LONELY -> FaceParams(
        eyeOpenness = 0.7f, pupilSize = 0.6f, pupilOffsetY = 0.2f,
        mouthCurve = 0.25f,
        earLeftAngle = -0.5f, earRightAngle = -0.5f,
        tailWagSpeed = 0.05f, tailHeight = 0.15f,
        bgHue = 220f, bgSaturation = 0.15f
    )
    Emotion.CONFUSED -> FaceParams(
        eyeOpenness = 0.9f, pupilSize = 0.5f, pupilOffsetX = -0.2f,
        mouthCurve = 0.4f, mouthOpen = 0.1f,
        earLeftAngle = 0.3f, earRightAngle = -0.4f,
        tailWagSpeed = 0.1f, tailHeight = 0.3f,
        bgHue = 280f, bgSaturation = 0.1f
    )
    Emotion.ALERT -> FaceParams(
        eyeOpenness = 1f, pupilSize = 0.4f,
        mouthCurve = 0.5f,
        earLeftAngle = 0.8f, earRightAngle = 0.8f,
        tailWagSpeed = 0.15f, tailHeight = 0.65f,
        bgHue = 10f, bgSaturation = 0.12f
    )
}
