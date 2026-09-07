package com.flowkeys.android.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.flowkeys.android.core.model.DictationState
import com.flowkeys.android.core.model.Language
import com.flowkeys.android.core.model.ScriptMode
import kotlin.math.sin

/**
 * Hardware-accelerated floating micro-pill rendering FlowKeys states:
 * - Dormant / Ready: Minimal pill with language & script indicator
 * - Recording: Pulsing waveform with audio level reactivity
 * - Processing: Smooth rotating accent glow
 * - Success: Green confirmation pulse
 * - Error: Subtle orange warning
 */
class FloatingPillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Listener {
        fun onPillPressStart()
        fun onPillPressRelease(isHoldAction: Boolean)
        fun onPillClicked()
        fun onPillDoubleTapped()
        fun onPillTripleTapped()
        fun onPillDragged(dx: Float, dy: Float)
        fun onDragFinished()
    }

    var listener: Listener? = null

    private var currentState: DictationState = DictationState.Dormant
    private var selectedLanguage: Language = Language.DEFAULT
    private var targetLanguage: Language? = null
    private var scriptMode: ScriptMode = ScriptMode.DEFAULT
    private var tapCount = 0
    private var lastTapTimeMs = 0L

    // Stitch Kinetic Audio Studio Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#121316") // Stitch surface-1 OLED dark
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5E36") // Stitch accent-coral
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F8FAFC") // Stitch high-emphasis primary WCAG AAA
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val micPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5E36") // Stitch accent-coral
        style = Paint.Style.FILL
    }

    private val micStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5E36") // Stitch accent-coral
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3.5f
    }

    private val waveformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5E36") // Stitch accent-coral
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6.5f
    }

    private val rectF = RectF()
    private val micCapsuleRect = RectF()
    private val micCradleRect = RectF()
    private var waveformPhase = 0f
    private var currentAudioLevel = 0f

    // Touch and Gesture Tracking
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isHoldGesture = false
    private val touchSlop = 14f

    private val holdRunnable = Runnable {
        if (!isDragging) {
            isHoldGesture = true
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            animate().scaleX(1.08f).scaleY(1.08f).setDuration(120).start()
            listener?.onPillPressStart()
        }
    }

    private val tapTimeoutRunnable = Runnable {
        when (tapCount) {
            1 -> listener?.onPillClicked()
            2 -> listener?.onPillDoubleTapped()
            3 -> listener?.onPillTripleTapped()
        }
        tapCount = 0
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            waveformPhase = it.animatedValue as Float
            if (currentState is DictationState.Recording || currentState is DictationState.Processing) {
                invalidate()
            }
        }
    }

    init {
        pulseAnimator.start()
    }

    fun updateState(state: DictationState) {
        this.currentState = state
        when (state) {
            is DictationState.Recording -> {
                currentAudioLevel = state.audioLevel
                selectedLanguage = state.language
                borderPaint.color = Color.parseColor("#FF5E36") // Stitch accent-coral active recording
            }
            is DictationState.Processing -> {
                selectedLanguage = state.language
                borderPaint.color = Color.parseColor("#FFA07A") // Stitch accent-peach processing
            }
            is DictationState.Success -> {
                borderPaint.color = Color.parseColor("#34D399") // Stitch emerald success
            }
            is DictationState.Error -> {
                borderPaint.color = Color.parseColor("#F87171") // Stitch error
            }
            is DictationState.FieldFocused -> {
                selectedLanguage = state.selectedLanguage
                borderPaint.color = Color.parseColor("#FF5E36") // Stitch ready focus ring
            }
            DictationState.Dormant -> {
                borderPaint.color = Color.parseColor("#2A2E35") // Stitch surface-3 dormant
            }
        }
        invalidate()
    }

    fun updateLanguage(language: Language) {
        selectedLanguage = language
        invalidate()
    }

    fun updateTargetLanguage(target: Language?) {
        targetLanguage = target
        invalidate()
    }

    fun updateScriptMode(mode: ScriptMode) {
        scriptMode = mode
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cornerRadius = h / 2f
        val unit = (h / 46f).coerceAtLeast(1f)

        rectF.set(4f, 4f, w - 4f, h - 4f)

        // 1. Draw pill background & border
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, backgroundPaint)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        // 2. Draw interior content based on state
        when (val state = currentState) {
            is DictationState.Recording -> {
                drawWaveform(canvas, w, h, state.audioLevel, unit)
            }
            is DictationState.Processing -> {
                drawProcessingGlow(canvas, w, h)
            }
            is DictationState.Success -> {
                textPaint.color = Color.parseColor("#10B981")
                textPaint.textSize = 24f * unit
                canvas.drawText("✓", w / 2f, h / 2f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
            }
            is DictationState.Error -> {
                textPaint.color = Color.parseColor("#F43F5E")
                textPaint.textSize = 24f * unit
                canvas.drawText("!", w / 2f, h / 2f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
            }
            else -> {
                drawReadyState(canvas, w, h, unit)
            }
        }
    }

    private fun drawReadyState(canvas: Canvas, w: Float, h: Float, unit: Float) {
        val srcCode = when (selectedLanguage) {
            Language.ENGLISH -> "EN"
            Language.HINDI -> "HI"
            Language.BENGALI -> "বাং"
        }

        val hasExplicitTarget = targetLanguage != null && targetLanguage != selectedLanguage
        val langCode = if (hasExplicitTarget) {
            val tgtCode = when (targetLanguage) {
                Language.ENGLISH -> "EN"
                Language.HINDI -> "HI"
                Language.BENGALI -> "বাং"
                null -> ""
            }
            "$srcCode→$tgtCode"
        } else {
            when (scriptMode) {
                ScriptMode.TRANSLATED_ENGLISH -> if (selectedLanguage != Language.ENGLISH) "$srcCode→EN" else "EN"
                ScriptMode.NATIVE_SCRIPT -> srcCode
                ScriptMode.PHONETIC_LATIN -> if (selectedLanguage != Language.ENGLISH) "$srcCode(Lat)" else "EN"
            }
        }

        textPaint.color = Color.parseColor("#F8FAFC")
        val isLongText = langCode.length > 3
        textPaint.textSize = if (isLongText) 10.5f * unit else 14.5f * unit
        val textWidth = textPaint.measureText(langCode)

        // Vector microphone dimensions
        val micW = 8.5f * unit
        val micH = 12.5f * unit
        val gap = 5.5f * unit
        val totalW = micW + gap + textWidth
        val startX = (w - totalW) / 2f
        val cy = h / 2f

        // Draw Vector Mic
        val micCenterX = startX + micW / 2f
        val capsuleRadius = 3.0f * unit
        micCapsuleRect.set(
            micCenterX - capsuleRadius,
            cy - 6.5f * unit,
            micCenterX + capsuleRadius,
            cy + 2f * unit
        )
        canvas.drawRoundRect(micCapsuleRect, capsuleRadius, capsuleRadius, micPaint)

        // Cradle arc
        val cradleRadius = 5.0f * unit
        micCradleRect.set(
            micCenterX - cradleRadius,
            cy - 4.5f * unit,
            micCenterX + cradleRadius,
            cy + 4f * unit
        )
        canvas.drawArc(micCradleRect, 0f, 180f, false, micStrokePaint)

        // Stem & Base
        canvas.drawLine(micCenterX, cy + 4f * unit, micCenterX, cy + 6.5f * unit, micStrokePaint)
        canvas.drawLine(micCenterX - 3.5f * unit, cy + 6.5f * unit, micCenterX + 3.5f * unit, cy + 6.5f * unit, micStrokePaint)

        // Draw Language / Mode Badge Text
        val textCenterX = startX + micW + gap + (textWidth / 2f)
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(langCode, textCenterX, textY, textPaint)
    }

    private fun drawWaveform(canvas: Canvas, w: Float, h: Float, level: Float, unit: Float) {
        val centerY = h / 2f
        val barCount = 5
        val barSpacing = 12f * unit
        val startX = (w - ((barCount - 1) * barSpacing)) / 2f

        waveformPaint.strokeWidth = 4.5f * unit
        for (i in 0 until barCount) {
            val x = startX + (i * barSpacing)
            val waveMod = sin(Math.toRadians((waveformPhase + (i * 45)).toDouble())).toFloat()
            val dynamicHeight = (8f * unit + (level * 28f * unit) * (0.5f + 0.5f * waveMod)).coerceIn(6f * unit, h * 0.65f)
            canvas.drawLine(x, centerY - dynamicHeight / 2f, x, centerY + dynamicHeight / 2f, waveformPaint)
        }
    }

    private fun drawProcessingGlow(canvas: Canvas, w: Float, h: Float) {
        textPaint.color = Color.parseColor("#F59E0B")
        textPaint.textSize = 18f
        val dots = when ((waveformPhase / 90).toInt() % 4) {
            0 -> "●"
            1 -> "● ●"
            2 -> "● ● ●"
            else -> "●"
        }
        canvas.drawText(dots, w / 2f, h / 2f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                isHoldGesture = false
                performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start()
                postDelayed(holdRunnable, 220L)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (!isDragging && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                    isDragging = true
                    removeCallbacks(holdRunnable)
                    removeCallbacks(tapTimeoutRunnable)
                    tapCount = 0
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }
                if (isDragging) {
                    listener?.onPillDragged(dx, dy)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(holdRunnable)
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                if (isDragging) {
                    listener?.onDragFinished()
                } else if (isHoldGesture) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    listener?.onPillPressRelease(isHoldAction = true)
                } else {
                    val now = System.currentTimeMillis()
                    removeCallbacks(tapTimeoutRunnable)
                    if (now - lastTapTimeMs < 300L) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapTimeMs = now

                    if (tapCount >= 3) {
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        listener?.onPillTripleTapped()
                        tapCount = 0
                    } else {
                        postDelayed(tapTimeoutRunnable, 280L)
                    }
                }
                isDragging = false
                isHoldGesture = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(holdRunnable)
                removeCallbacks(tapTimeoutRunnable)
                tapCount = 0
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                if (isHoldGesture) {
                    listener?.onPillPressRelease(isHoldAction = true)
                }
                if (isDragging) {
                    listener?.onDragFinished()
                }
                isDragging = false
                isHoldGesture = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
