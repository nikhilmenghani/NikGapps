package com.nikgapps

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.nikgapps.ui.common.ListInstalledApps
import com.nikgapps.ui.theme.NikGappsTheme

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MyApp(context: Context) {
    NikGappsTheme {
        ListInstalledApps(context)
    }
}
