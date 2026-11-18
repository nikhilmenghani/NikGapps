package com.nikgapps.app.presentation.theme

import androidx.compose.ui.graphics.Color
import com.nikgapps.app.data.GlassBackground

internal data class GlassAccent(
    val primary: Color, val onPrimary: Color,
    val container: Color, val onContainer: Color
)

/** Backgrounds and their control accents live together, rather than in individual widgets. */
internal data class GlassBackgroundPalette(
    val darkGround: Color,
    val lightGround: Color,
    val lights: List<Color>,
    val darkAccent: GlassAccent,
    val lightAccent: GlassAccent
)

internal val GlassBackground.palette: GlassBackgroundPalette
    get() = when (this) {
        GlassBackground.GOOGLE -> google
        GlassBackground.OCEAN -> ocean
        GlassBackground.FOREST -> forest
        GlassBackground.DUSK -> dusk
        GlassBackground.GRAPHITE -> graphite
    }

private val google = GlassBackgroundPalette(
    Color(0xFF102A24), Color(0xFFF0F6F2),
    listOf(Color(0xFF4285F4), Color(0xFFFBBC05), Color(0xFFEA4335), Color(0xFF34A853), Color(0xFF4285F4)),
    GlassAccent(Color(0xFF89D5C6), Color(0xFF00382F), Color(0xFF005046), Color(0xFFA5F2E3)),
    GlassAccent(Color(0xFF006B5D), Color.White, Color(0xFFA5F2E3), Color(0xFF002019))
)
private val ocean = GlassBackgroundPalette(
    Color(0xFF102532), Color(0xFFEDF6FA),
    listOf(Color(0xFF3189CB), Color(0xFF238BB3), Color(0xFF497EBA), Color(0xFF329C9B), Color(0xFF326EAE)),
    GlassAccent(Color(0xFFA1CCE8), Color(0xFF003348), Color(0xFF184B60), Color(0xFFC0E9FF)),
    GlassAccent(Color(0xFF1E637D), Color.White, Color(0xFFC0E9FF), Color(0xFF001F2B))
)
private val forest = GlassBackgroundPalette(
    Color(0xFF192B21), Color(0xFFF0F6EC),
    listOf(Color(0xFF439779), Color(0xFF819E53), Color(0xFF789A69), Color(0xFF2E8466), Color(0xFF4D8F77)),
    GlassAccent(Color(0xFFB0D5A0), Color(0xFF1F3715), Color(0xFF354F29), Color(0xFFCCEFB9)),
    GlassAccent(Color(0xFF456832), Color.White, Color(0xFFCCEFB9), Color(0xFF102005))
)
private val dusk = GlassBackgroundPalette(
    Color(0xFF292235), Color(0xFFF8F0FA),
    listOf(Color(0xFF7766AE), Color(0xFF9A73A6), Color(0xFFB67491), Color(0xFF6A79A6), Color(0xFF7E609B)),
    GlassAccent(Color(0xFFD1BEE8), Color(0xFF37264C), Color(0xFF4E3D63), Color(0xFFECDCFF)),
    GlassAccent(Color(0xFF69537E), Color.White, Color(0xFFECDCFF), Color(0xFF241235))
)
private val graphite = GlassBackgroundPalette(
    Color(0xFF21282B), Color(0xFFF1F4F5),
    listOf(Color(0xFF667E87), Color(0xFF7E8C90), Color(0xFF807E83), Color(0xFF66817B), Color(0xFF60747F)),
    GlassAccent(Color(0xFFC1CDD1), Color(0xFF293235), Color(0xFF404B4F), Color(0xFFDDE9ED)),
    GlassAccent(Color(0xFF536267), Color.White, Color(0xFFDDE9ED), Color(0xFF142025))
)
