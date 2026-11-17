package com.nikgapps.app.presentation

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.nikgapps.app.presentation.ui.component.AccountPopupPositionProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountPopupPositionTest {
    private val position = AccountPopupPositionProvider(margin = 16, gap = 8)

    @Test fun `card stays above profile and within both edges on a narrow phone`() {
        assertEquals(IntOffset(16, 592), position.calculatePosition(
            IntRect(250, 720, 306, 776), IntSize(384, 820), LayoutDirection.Ltr, IntSize(352, 120)))
    }

    @Test fun `wide screens keep the card aligned with the profile button`() {
        assertEquals(IntOffset(440, 592), position.calculatePosition(
            IntRect(744, 720, 800, 776), IntSize(1200, 820), LayoutDirection.Ltr, IntSize(360, 120)))
    }

    @Test fun `card opens below when there is no space above`() {
        assertEquals(IntOffset(16, 84), position.calculatePosition(
            IntRect(16, 20, 72, 76), IntSize(384, 820), LayoutDirection.Ltr, IntSize(352, 120)))
    }

    @Test fun `right to left layout stays inside the same screen margins`() {
        assertEquals(IntOffset(16, 592), position.calculatePosition(
            IntRect(78, 720, 134, 776), IntSize(384, 820), LayoutDirection.Rtl, IntSize(352, 120)))
    }
}
