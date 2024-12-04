package com.nikgapps.app.presentation.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikgapps.App
import com.nikgapps.app.presentation.ui.component.cards.ExecuteMountCard
import com.nikgapps.app.presentation.ui.component.cards.InstallZipCard
import com.nikgapps.app.presentation.ui.component.cards.RootAccessCard
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.dumps.RootUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(progressLogViewModel: ProgressLogViewModel) {
    var rootAccessState by remember { mutableStateOf(App.hasRootAccess) }
    LaunchedEffect(Unit) {
        val rootAccess = RootUtility.hasRootAccess()
        Log.d("NikGapps-RootAccess", "Root Access: $rootAccess")
        App.hasRootAccess = rootAccess

        withContext(Dispatchers.Main) {
            rootAccessState = rootAccess
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install NikGapps") }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (rootAccessState) {
                    item { InstallZipCard(progressLogViewModel) }
                    item { ExecuteMountCard() }
                } else {
                    item {
                        RootAccessCard(rootAccessState, onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val rootAccess = RootUtility.hasRootAccess()
                                Log.d("NikGapps-RootAccess", "Root Access: $rootAccess")
                                App.hasRootAccess = rootAccess

                                withContext(Dispatchers.Main) {
                                    rootAccessState = rootAccess
                                }
                            }
                        })
                    }
                }

            }
        }
    )
}