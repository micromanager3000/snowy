package com.snowy.pet.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PetFaceScreen(emotion: Emotion) {
    val targetParams = emotion.toFaceParams()

    // Animate face parameters smoothly
    val eyeOpenness by animateFloatAsState(targetParams.eyeOpenness, tween(600))
    val pupilSize by animateFloatAsState(targetParams.pupilSize, tween(400))
    val pupilOffsetX by animateFloatAsState(targetParams.pupilOffsetX, tween(500))
    val pupilOffsetY by animateFloatAsState(targetParams.pupilOffsetY, tween(500))
    val mouthCurve by animateFloatAsState(targetParams.mouthCurve, tween(500))
    val mouthOpen by animateFloatAsState(targetParams.mouthOpen, tween(400))
    val earLeftAngle by animateFloatAsState(targetParams.earLeftAngle, tween(700))
    val earRightAngle by animateFloatAsState(targetParams.earRightAngle, tween(700))
    val tailWagSpeed by animateFloatAsState(targetParams.tailWagSpeed, tween(500))
    val tailHeight by animateFloatAsState(targetParams.tailHeight, tween(600))
    val bgHue by animateFloatAsState(targetParams.bgHue, tween(800))
    val bgSaturation by animateFloatAsState(targetParams.bgSaturation, tween(800))

    // Continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "idle")

    // Blink: mostly open, brief close every 4 seconds
    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "blink"
    )
    val blinkMultiplier = if (blinkPhase in 0.92f..0.96f) 0.05f else 1f

    // Tail wag oscillation
    val tailPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "tail"
    )

    // Breathing (subtle scale pulse)
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "breath"
    )
    val breathScale = 1f + sin(breathPhase) * 0.008f

    val bgColor = Color.hsl(bgHue, bgSaturation, 0.12f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.42f // face center, slightly above middle
        val faceRadius = w * 0.32f * breathScale

        // Face (head circle)
        val faceColor = Color(0xFFF5E6D3) // warm cream
        drawCircle(faceColor, radius = faceRadius, center = Offset(cx, cy))

        // Ears
        drawEar(cx - faceRadius * 0.7f, cy - faceRadius * 0.7f, faceRadius, earLeftAngle, isLeft = true)
        drawEar(cx + faceRadius * 0.7f, cy - faceRadius * 0.7f, faceRadius, earRightAngle, isLeft = false)

        // Eyes
        val eyeSpacing = faceRadius * 0.4f
        val eyeY = cy - faceRadius * 0.1f
        val effectiveOpenness = eyeOpenness * blinkMultiplier
        drawEye(cx - eyeSpacing, eyeY, faceRadius * 0.22f, effectiveOpenness, pupilSize, pupilOffsetX, pupilOffsetY, targetParams.sparkle)
        drawEye(cx + eyeSpacing, eyeY, faceRadius * 0.22f, effectiveOpenness, pupilSize, pupilOffsetX, pupilOffsetY, targetParams.sparkle)

        // Nose
        val noseY = cy + faceRadius * 0.15f
        drawCircle(Color(0xFF3D2B1F), radius = faceRadius * 0.07f, center = Offset(cx, noseY))

        // Mouth
        drawMouth(cx, noseY + faceRadius * 0.12f, faceRadius, mouthCurve, mouthOpen)

        // Tail
        val tailX = cx + faceRadius * 1.1f
        val tailY = cy + faceRadius * 1.2f
        val tailSwing = sin(tailPhase) * tailWagSpeed * 25f
        drawTail(tailX, tailY, faceRadius * 0.6f, tailHeight, tailSwing)

        // Body hint (partial oval below face)
        drawOval(
            color = faceColor,
            topLeft = Offset(cx - faceRadius * 0.75f, cy + faceRadius * 0.6f),
            size = Size(faceRadius * 1.5f, faceRadius * 1.1f)
        )
    }
}

private fun DrawScope.drawEye(
    x: Float, y: Float, radius: Float,
    openness: Float, pupilSize: Float,
    offsetX: Float, offsetY: Float,
    sparkle: Boolean
) {
    val eyeHeight = radius * 2f * openness.coerceIn(0.05f, 1f)
    val eyeWidth = radius * 2f

    // White of eye
    drawOval(
        color = Color.White,
        topLeft = Offset(x - eyeWidth / 2, y - eyeHeight / 2),
        size = Size(eyeWidth, eyeHeight)
    )

    // Pupil
    if (openness > 0.1f) {
        val pRadius = radius * pupilSize * 0.7f
        val px = x + offsetX * radius * 0.3f
        val py = y + offsetY * radius * 0.3f
        drawCircle(Color(0xFF2C1810), radius = pRadius, center = Offset(px, py))

        // Highlight
        drawCircle(
            Color.White.copy(alpha = 0.8f),
            radius = pRadius * 0.3f,
            center = Offset(px - pRadius * 0.25f, py - pRadius * 0.25f)
        )

        // Sparkle for ecstatic
        if (sparkle) {
            drawCircle(
                Color.White.copy(alpha = 0.6f),
                radius = pRadius * 0.15f,
                center = Offset(px + pRadius * 0.3f, py + pRadius * 0.2f)
            )
        }
    }
}

private fun DrawScope.drawMouth(
    cx: Float, y: Float, faceRadius: Float,
    curve: Float, openAmount: Float
) {
    val mouthWidth = faceRadius * 0.4f
    val curveOffset = (curve - 0.5f) * faceRadius * 0.2f // negative=frown, positive=smile

    val path = Path().apply {
        moveTo(cx - mouthWidth / 2, y)
        quadraticBezierTo(cx, y + curveOffset, cx + mouthWidth / 2, y)
    }
    drawPath(path, Color(0xFF3D2B1F), style = androidx.compose.ui.graphics.drawscope.Stroke(width = faceRadius * 0.03f))

    // Open mouth
    if (openAmount > 0.05f) {
        val openHeight = openAmount * faceRadius * 0.15f
        drawOval(
            color = Color(0xFF8B4513).copy(alpha = 0.7f),
            topLeft = Offset(cx - mouthWidth * 0.3f, y + curveOffset * 0.3f),
            size = Size(mouthWidth * 0.6f, openHeight)
        )
    }
}

private fun DrawScope.drawEar(
    x: Float, y: Float, faceRadius: Float,
    angle: Float, isLeft: Boolean
) {
    val earLength = faceRadius * 0.55f
    val earWidth = faceRadius * 0.3f
    // angle: -1=drooped, 0=neutral, 1=perked
    val rotation = if (isLeft) {
        -30f + angle * -30f // perked = more upward
    } else {
        30f + angle * 30f
    }

    rotate(degrees = rotation, pivot = Offset(x, y)) {
        // Outer ear
        drawOval(
            color = Color(0xFFD4A574),
            topLeft = Offset(x - earWidth / 2, y - earLength),
            size = Size(earWidth, earLength)
        )
        // Inner ear
        drawOval(
            color = Color(0xFFE8C4A0),
            topLeft = Offset(x - earWidth * 0.3f, y - earLength * 0.85f),
            size = Size(earWidth * 0.6f, earLength * 0.7f)
        )
    }
}

private fun DrawScope.drawTail(
    x: Float, y: Float, length: Float,
    height: Float, swing: Float
) {
    val tailEndY = y - length * height
    val controlX = x + swing
    val path = Path().apply {
        moveTo(x - length * 0.3f, y)
        quadraticBezierTo(controlX, tailEndY - length * 0.3f, x + swing * 0.5f, tailEndY)
    }
    drawPath(
        path,
        Color(0xFFD4A574),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = length * 0.15f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}
