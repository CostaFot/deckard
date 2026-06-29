package com.markedusduplicate.deckard.poc

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.markedusduplicate.design.theme.AppTheme
import kotlinx.serialization.Serializable

@Serializable
private data object ScreenA : NavKey

@Serializable
private data object ScreenB : NavKey

@Serializable
private data object ScreenC : NavKey

/**
 * POC: a right-edge overlay panel hosting a Navigation 3 [NavDisplay] across three screens, to verify
 * that ViewModels can be **scoped per screen** inside the Service-hosted Compose overlay.
 *
 * - The back stack is a Nav3 [rememberNavBackStack] of [NavKey] routes.
 * - `entryDecorators` includes [rememberViewModelStoreNavEntryDecorator], which gives each [androidx.navigation3.runtime.NavEntry]
 *   its own `ViewModelStore` — so each screen's VM is created on entry and cleared when popped.
 * - Each screen resolves its VM with `viewModel(factory = [viewModelFactory])`: the per-entry store
 *   (from the decorator) provides **scoping**; our singleton-graph-backed factory provides **DI**.
 *
 * [NavDisplay] (nav3 1.0.0) installs its back handler through the NavigationEvent library, so the host
 * must supply a `LocalNavigationEventDispatcherOwner`. An `Activity` provides one; a `Service` doesn't,
 * so we provide [FakeNavigationEventDispatcherOwner] (same trick as the old keyboard service). On-screen
 * Back buttons also drive the back stack directly.
 */
@SuppressLint("ViewConstructor")
class PocNavOverlayView(
    context: Context,
    private val viewModelFactory: ViewModelProvider.Factory,
    private val onClose: () -> Unit,
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner,
        ) {
            AppTheme {
                val backStack = rememberNavBackStack(ScreenA)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .width(300.dp)
                        .padding(8.dp),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<ScreenA> {
                                val vm: PocScreenAViewModel = viewModel(factory = viewModelFactory)
                                ScreenScaffold(
                                    title = "Screen A",
                                    vm = vm,
                                    backStackDepth = backStack.size,
                                    onBack = null,
                                    primaryAction = "Go to B →" to { backStack.add(ScreenB) },
                                    onClose = onClose,
                                )
                            }
                            entry<ScreenB> {
                                val vm: PocScreenBViewModel = viewModel(factory = viewModelFactory)
                                ScreenScaffold(
                                    title = "Screen B",
                                    vm = vm,
                                    backStackDepth = backStack.size,
                                    onBack = { backStack.removeLastOrNull() },
                                    primaryAction = "Go to C →" to { backStack.add(ScreenC) },
                                    onClose = onClose,
                                )
                            }
                            entry<ScreenC> {
                                val vm: PocScreenCViewModel = viewModel(factory = viewModelFactory)
                                ScreenScaffold(
                                    title = "Screen C",
                                    vm = vm,
                                    backStackDepth = backStack.size,
                                    onBack = { backStack.removeLastOrNull() },
                                    primaryAction = null,
                                    onClose = onClose,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenScaffold(
    title: String,
    vm: PocScreenViewModel,
    backStackDepth: Int,
    onBack: (() -> Unit)?,
    primaryAction: Pair<String, () -> Unit>?,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose) { Text("✕") }
        }
        Text("VM instance #${vm.instanceId}", style = MaterialTheme.typography.bodySmall)
        Text("Back-stack depth: $backStackDepth", style = MaterialTheme.typography.bodySmall)
        Button(onClick = vm::bump) { Text("Clicks (kept in VM): ${vm.clicks}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onBack != null) {
                OutlinedButton(onClick = onBack) { Text("← Back") }
            }
            if (primaryAction != null) {
                Button(
                    onClick = primaryAction.second,
                    modifier = Modifier.weight(1f),
                ) { Text(primaryAction.first) }
            }
        }
    }
}

/**
 * A do-nothing [NavigationEventDispatcherOwner] so Nav3's [NavDisplay] back handler has a dispatcher
 * to register with when hosted outside an `Activity` (here, the overlay `Service`). Back navigation is
 * driven by the on-screen buttons / back stack, not by system back events through this dispatcher.
 */
internal val FakeNavigationEventDispatcherOwner: NavigationEventDispatcherOwner =
    object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()
    }
