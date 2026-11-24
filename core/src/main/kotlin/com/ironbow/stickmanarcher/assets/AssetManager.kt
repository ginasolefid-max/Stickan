package com.ironbow.stickmanarcher.assets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

class AssetManager : Disposable {

    lateinit var whitePixel: Texture private set
    lateinit var whiteRegion: TextureRegion private set
    lateinit var circleTex: Texture private set
    lateinit var circleRegion: TextureRegion private set
    lateinit var glowTex: Texture private set
    lateinit var glowRegion: TextureRegion private set
    lateinit var ringTex: Texture private set
    lateinit var ringRegion: TextureRegion private set

    lateinit var fontSmall: BitmapFont private set
    lateinit var fontMedium: BitmapFont private set
    lateinit var fontLarge: BitmapFont private set
    lateinit var fontTitle: BitmapFont private set
    lateinit var uiSkin: Skin private set

    private val createdTextures = ArrayList<Texture>()

    fun load() {
        whitePixel = buildSolidTexture(2, 2, Color.WHITE)
        whiteRegion = TextureRegion(whitePixel)
        circleTex = buildCircleTexture(64, soft = false)
        circleRegion = TextureRegion(circleTex)
        glowTex = buildCircleTexture(96, soft = true)
        glowRegion = TextureRegion(glowTex)
        ringTex = buildRingTexture(64, thickness = 6f)
        ringRegion = TextureRegion(ringTex)

        fontSmall = createScaledFont(1.05f)
        fontMedium = createScaledFont(1.45f)
        fontLarge = createScaledFont(2.05f)
        fontTitle = createScaledFont(2.9f)
        uiSkin = buildUiSkin()
    }

    private fun createScaledFont(scale: Float): BitmapFont = BitmapFont().apply {
        region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        data.setScale(scale)
        setUseIntegerPositions(false)
    }

    private fun buildSolidTexture(w: Int, h: Int, color: Color): Texture {
        val pm = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        return Texture(pm).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            pm.dispose()
            createdTextures.add(it)
        }
    }

    private fun buildCircleTexture(size: Int, soft: Boolean): Texture {
        val pm = Pixmap(size, size, Pixmap.Format.RGBA8888)
        val cx = size * 0.5f
        val cy = size * 0.5f
        val r = size * 0.48f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x + 0.5f - cx
                val dy = y + 0.5f - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= r) {
                    val norm = dist / r
                    val alpha = if (soft) (1f - norm) * (1f - norm) else ((r - dist) * 1.6f).coerceIn(0f, 1f)
                    pm.setColor(1f, 1f, 1f, alpha)
                    pm.drawPixel(x, y)
                }
            }
        }
        return Texture(pm).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            pm.dispose()
            createdTextures.add(it)
        }
    }

    private fun buildRingTexture(size: Int, thickness: Float): Texture {
        val pm = Pixmap(size, size, Pixmap.Format.RGBA8888)
        val cx = size * 0.5f
        val cy = size * 0.5f
        val outer = size * 0.46f
        val inner = outer - thickness
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x + 0.5f - cx
                val dy = y + 0.5f - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist in inner..outer) {
                    pm.setColor(1f, 1f, 1f, 0.9f)
                    pm.drawPixel(x, y)
                }
            }
        }
        return Texture(pm).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            pm.dispose()
            createdTextures.add(it)
        }
    }

    private fun buildPanelTexture(fill: Color, border: Color): Texture {
        val pm = Pixmap(32, 32, Pixmap.Format.RGBA8888)
        pm.setColor(border)
        pm.fill()
        pm.setColor(fill)
        pm.fillRectangle(2, 2, 28, 28)
        return Texture(pm).also {
            pm.dispose()
            createdTextures.add(it)
        }
    }

    private fun buildUiSkin(): Skin {
        val skin = Skin()
        val btnUp = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.16f, 0.21f, 0.29f, 0.96f), Color(0.36f, 0.46f, 0.60f, 1f))))
        val btnDown = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.10f, 0.14f, 0.20f, 0.98f), Color(0.92f, 0.35f, 0.25f, 1f))))
        val btnDisabled = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.12f, 0.13f, 0.16f, 0.7f), Color(0.24f, 0.26f, 0.30f, 0.7f))))
        val accentUp = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.76f, 0.20f, 0.20f, 0.98f), Color(0.98f, 0.55f, 0.36f, 1f))))
        val accentDown = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.54f, 0.12f, 0.12f, 1f), Color(1f, 0.75f, 0.45f, 1f))))
        val panelBg = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.08f, 0.10f, 0.15f, 0.93f), Color(0.28f, 0.36f, 0.48f, 0.95f))))
        val sliderBg = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.16f, 0.20f, 0.26f, 1f), Color(0.35f, 0.42f, 0.52f, 1f)))).apply { minHeight = 14f }
        val sliderKnob = TextureRegionDrawable(circleRegion).apply { minWidth = 28f; minHeight = 28f }
        val checkOff = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.14f, 0.18f, 0.24f, 1f), Color(0.45f, 0.55f, 0.68f, 1f)))).apply { minWidth = 28f; minHeight = 28f }
        val checkOn = TextureRegionDrawable(TextureRegion(buildPanelTexture(Color(0.86f, 0.28f, 0.24f, 1f), Color(1f, 0.85f, 0.45f, 1f)))).apply { minWidth = 28f; minHeight = 28f }

        skin.add("panel", panelBg)
        skin.add("font-small", fontSmall)
        skin.add("font-medium", fontMedium)
        skin.add("font-large", fontLarge)
        skin.add("font-title", fontTitle)
        skin.add("default", Label.LabelStyle(fontMedium, Color.WHITE))
        skin.add("small", Label.LabelStyle(fontSmall, Color(0.84f, 0.88f, 0.94f, 1f)))
        skin.add("large", Label.LabelStyle(fontLarge, Color.WHITE))
        skin.add("title", Label.LabelStyle(fontTitle, Color(1f, 0.84f, 0.34f, 1f)))
        skin.add("gold", Label.LabelStyle(fontMedium, Color(1f, 0.85f, 0.28f, 1f)))

        skin.add("default", TextButton.TextButtonStyle(btnUp, btnDown, btnUp, fontMedium).apply {
            disabled = btnDisabled; fontColor = Color.WHITE; disabledFontColor = Color(0.55f, 0.58f, 0.63f, 1f)
        })
        skin.add("accent", TextButton.TextButtonStyle(accentUp, accentDown, accentUp, fontMedium).apply {
            disabled = btnDisabled; fontColor = Color.WHITE
        })
        skin.add("small", TextButton.TextButtonStyle(btnUp, btnDown, btnUp, fontSmall).apply {
            disabled = btnDisabled; fontColor = Color.WHITE
        })
        skin.add("default", Slider.SliderStyle(sliderBg, sliderKnob))
        skin.add("default", CheckBox.CheckBoxStyle(checkOff, checkOn, fontMedium, Color.WHITE))
        skin.add("default", ScrollPane.ScrollPaneStyle().apply { background = panelBg })
        skin.add("default", Window.WindowStyle(fontLarge, Color.WHITE, panelBg))
        return skin
    }

    fun drawRect(batch: SpriteBatch, x: Float, y: Float, w: Float, h: Float, color: Color) {
        batch.color = color
        batch.draw(whiteRegion, x, y, w, h)
        batch.color = Color.WHITE
    }

    fun drawLimb(batch: SpriteBatch, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Color) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < 0.001f) return
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        batch.color = color
        batch.draw(whiteRegion, x1, y1 - thickness * 0.5f, 0f, thickness * 0.5f, len, thickness, 1f, 1f, angleDeg)
        val r = thickness * 0.5f
        batch.draw(circleRegion, x1 - r, y1 - r, thickness, thickness)
        batch.draw(circleRegion, x2 - r, y2 - r, thickness, thickness)
        batch.color = Color.WHITE
    }

    fun drawCircle(batch: SpriteBatch, cx: Float, cy: Float, radius: Float, color: Color) {
        batch.color = color
        batch.draw(circleRegion, cx - radius, cy - radius, radius * 2f, radius * 2f)
        batch.color = Color.WHITE
    }

    fun drawGlow(batch: SpriteBatch, cx: Float, cy: Float, radius: Float, color: Color) {
        batch.color = color
        batch.draw(glowRegion, cx - radius, cy - radius, radius * 2f, radius * 2f)
        batch.color = Color.WHITE
    }

    override fun dispose() {
        createdTextures.forEach { it.dispose() }
        createdTextures.clear()
        fontSmall.dispose(); fontMedium.dispose(); fontLarge.dispose(); fontTitle.dispose(); uiSkin.dispose()
    }
}
