# Navigation 3 + scoped ViewModels inside a Service-hosted Compose overlay

How to run Jetpack **Navigation 3** with **per-screen, dependency-injected ViewModels** inside a
Compose UI that lives in a **`WindowManager` overlay driven by a plain `Service`** — not an
`Activity`.

This is unusual: every "it just works" affordance in Compose/Hilt/Nav3 assumes an `Activity` (or
`Fragment`/`NavBackStackEntry`) is the host. A `Service` has none of that machinery, so each missing
piece has to be supplied by hand. The payoff: a floating panel that navigates across screens, each
with its own ViewModel that is DI'd, retained across recomposition, and cleared when its screen is
popped.

> **Status / how to use this doc.** We proved this end-to-end with a throwaway right-edge POC panel,
> confirmed on device (VMs scoped per screen, retained across recomposition, cleared on pop), then
> **deleted the POC code**. So the `poc/…` files referenced historically below no longer exist —
> this
> document *is* the reference for rebuilding it. What **does** still live in the repo is the
> reusable
> plumbing: `DeckardOverlayService` (the host), `OverlayViewModelFactory`, `OverlayViewModelModule`,
> and `ExperimentViewModel` (an example binding). See the [File map](#file-map).

---

## TL;DR

1. **A `Service` can host Compose** if it implements `LifecycleOwner` + `ViewModelStoreOwner` +
   `SavedStateRegistryOwner` itself and sets those as the **view-tree owners** on each overlay view.
2. **`hiltViewModel()` can't work in a `Service`.** Hilt's `@HiltViewModel` bindings live in
   `ViewModelComponent`, a child of the **Activity-scoped** `ActivityRetainedComponent`. A `Service`
   only reaches `ServiceComponent`/`SingletonComponent`, so those bindings don't exist for it.
3. **Get DI another way:** bind ViewModels into a `Map<Class, Provider<ViewModel>>` in the
   **singleton** graph and resolve them with a custom `ViewModelProvider.Factory` (the classic
   pre-`@HiltViewModel` pattern). Use plain `viewModel(factory = …)`, never `hiltViewModel()`.
4. **Nav3 scopes VMs per entry** via `rememberViewModelStoreNavEntryDecorator()`. That gives each
   `NavEntry` its own `ViewModelStore` keyed by `NavEntry.contentKey`, cleared on pop. Scoping and
   DI
   are **orthogonal**: the decorator decides *which store*; our factory decides *how to construct*.
5. **One host shim Nav3 needs that an `Activity` gives for free:** a
   `LocalNavigationEventDispatcherOwner` (nav3 1.0.0 routes back through the *NavigationEvent*
   library) — provide a no-op `FakeNavigationEventDispatcherOwner`.

---

## The host: making a `Service` a valid Compose host

An overlay `Service` has no decor view and no lifecycle callbacks wired to Compose. So the service
**is** the owner of everything Compose looks for:

```kotlin
@AndroidEntryPoint
class DeckardOverlayService :
    android.app.Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle get() = lifecycleRegistry

    override val viewModelStore = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)        // required before RESUMED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        // … addView(overlay) …
    }

    override fun onDestroy() {
        // … removeView(overlay) …
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()                                   // clears all entry-scoped VMs
        super.onDestroy()
    }
}
```

> `HasDefaultViewModelProviderFactory` on the service is **only** needed for `viewModel()` calls
> made
> *outside* a `NavDisplay` (e.g. a single VM straight in an overlay view). Inside Nav3 entries it is
> never consulted — see [scoping](#the-important-bit-how-per-entry-vm-scoping-actually-works). It's
> kept here because the repo also uses it for the non-Nav `ExperimentViewModel`.

Then, for **each** overlay view, push the service in as the view-tree owner so the composition can
find them via `LocalLifecycleOwner` / `LocalViewModelStoreOwner` / `LocalSavedStateRegistryOwner`:

```kotlin
private fun attachOwners(view: View) {
    view.setViewTreeLifecycleOwner(this)
    view.setViewTreeViewModelStoreOwner(this)
    view.setViewTreeSavedStateRegistryOwner(this)
}
```

Why this matters for Nav3: the Nav3 entry decorators build **child** owners on top of these. The
per-entry `ViewModelStore` map (see below) is itself held in a `ViewModel` stored in the **service's
**
`ViewModelStore` — so it lives exactly as long as the service, and is cleared in `onDestroy` via
`viewModelStore.clear()`.

---

## Why `hiltViewModel()` fails here (and the proof)

First attempt — a normal `@HiltViewModel` + `hiltViewModel()` in the overlay — crashes at
composition:

```
java.lang.RuntimeException: Cannot create an instance of class …SomeViewModel
    at …DefaultViewModelProviderFactory.create        ← the reflective default factory ran
Caused by: java.lang.NoSuchMethodException: …SomeViewModel.<init> []   ← it wanted a no-arg ctor
```

What happened, step by step:

1. `hiltViewModel()` calls `createHiltViewModelFactory(owner)`.
2. The owner (the service) is **not** a `HasDefaultViewModelProviderFactory` *that Hilt recognizes*,
   so it returns `null` and falls back to the reflective `DefaultViewModelProviderFactory`.
3. That factory only knows how to call a **no-arg** constructor → `NoSuchMethodException`.

The deeper reason it can *never* work from a `Service`:

```
SingletonComponent
 ├── ActivityRetainedComponent      ← created off an Activity (retained via a hidden VM)
 │    └── ViewModelComponent        ← @HiltViewModel bindings live HERE
 └── ServiceComponent               ← what a Service gets — no path to ViewModelComponent
```

`@HiltViewModel` classes are multibound inside `ViewModelComponent`. A `Service` has no route to it,
so the bindings simply don't exist for it. No amount of owner wiring changes that.

---

## The DI workaround: singleton-graph factory + multibinding map

Resolve ViewModels from the **singleton** graph (which the `@AndroidEntryPoint` service *does*
have),
using the multibinding pattern people used before `@HiltViewModel` existed.

**1. Plain `@Inject`-constructor ViewModels (NOT `@HiltViewModel`):**

```kotlin
class ScreenAViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,    // any singleton-graph dependency
) : ViewModel()
```

**2. Bind each into a `Map<Class, Provider<ViewModel>>` in `SingletonComponent`:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface OverlayViewModelModule {
    @Binds
    @IntoMap
    @ClassKey(ScreenAViewModel::class)
    fun bindScreenAViewModel(vm: ScreenAViewModel): ViewModel
    // …one @Binds line per screen VM
}
```

**3. A factory that looks up that map:**

```kotlin
class OverlayViewModelFactory @Inject constructor(
    private val providers: Map<Class<*>, @JvmSuppressWildcards Provider<ViewModel>>,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val provider = providers[modelClass] ?: error("No VM bound for $modelClass")
        @Suppress("UNCHECKED_CAST")
        return provider.get() as T
    }
}
```

The service `@Inject`s this factory. Adding a ViewModel later = one new class + one `@IntoMap` line.

> In the repo today the only binding is the example `ExperimentViewModel`. The `ScreenAViewModel`
> names above are illustrative — the Nav3 screen VMs are what you'd add when you build the panel.

---

## Wiring it together: the overlay view + the service

The Nav3 overlay view is a plain `AbstractComposeView` that takes the factory as a constructor
parameter (so the screens can pass it to `viewModel(factory = …)`):

```kotlin
@SuppressLint("ViewConstructor")
class OverlayNavView(
    context: Context,
    private val viewModelFactory: ViewModelProvider.Factory,   // = the injected OverlayViewModelFactory
) : AbstractComposeView(context) {
    @Composable
    override fun Content() { /* NavDisplay — see next section */
    }
}
```

The service injects the factory, builds the view, attaches the owners, and adds it as an overlay
window:

```kotlin
@Inject lateinit var overlayViewModelFactory: OverlayViewModelFactory

// in onCreate(), after the draw-over-permission check:
val nav = OverlayNavView(this, overlayViewModelFactory).also(::attachOwners)
windowManager.addView(nav, overlayParams(/* gravity, x, y */))
// keep a reference and removeView(nav) in onDestroy()
```

> Prefer threading the factory as a constructor arg (above). If you'd rather not pass it to every
> call site, put it in a `CompositionLocal` and add a helper —
> see [gotcha #2](#2--you-must-pass-viewmodelfactory--).

---

## Navigation 3 setup (stable 1.0.0)

```kotlin
@Serializable
private data object ScreenA : NavKey
@Serializable
private data object ScreenB : NavKey
@Serializable
private data object ScreenC : NavKey

@Composable
override fun Content() {
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner,   // see gotcha #1
    ) {
        AppTheme {
            val backStack = rememberNavBackStack(ScreenA)
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),     // ← the scoping decorator
                ),
                entryProvider = entryProvider {
                    entry<ScreenA> {
                        val vm: ScreenAViewModel =
                            viewModel(factory = viewModelFactory)  // see gotcha #2
                        ScreenA(vm, onNext = { backStack.add(ScreenB) })
                    }
                    entry<ScreenB> { /* … */ }
                    entry<ScreenC> { /* … */ }
                },
            )
        }
    }
}
```

API notes for 1.0.0:

- `entry<T> { }` is a **member** of `EntryProviderScope` (the `entryProvider { }` receiver) — **no
  import** (`androidx.navigation3.runtime.entry` does not exist; importing it fails to compile).
- `entryProvider` is imported from `androidx.navigation3.runtime.entryProvider`.
- `rememberNavBackStack(...)` requires routes to be `@Serializable` + `NavKey`
  (`androidx.navigation3.runtime.NavKey`).
- Back stack behaves like a `MutableList<NavKey>`: navigate with `.add(...)`, pop with
  `.removeLastOrNull()`, read depth with `.size`.

Imports worth pinning down (they're spread across three artifacts):

```kotlin
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
```

---

## The important bit: how per-entry VM scoping actually works

`rememberViewModelStoreNavEntryDecorator()` wraps every entry's content. Reading its source
(`androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator`) is what makes the whole
thing click. Paraphrased:

```kotlin
decorate = { entry ->
    // one ViewModelStore PER entry.contentKey, kept in an EntryViewModel that itself lives in the
    // PARENT (here: the service's) ViewModelStore:
    val store = parentViewModelStore.getEntryViewModel().viewModelStoreForKey(entry.contentKey)

    val childOwner = remember {
        object : ViewModelStoreOwner,
            SavedStateRegistryOwner by savedStateRegistryOwner,
            HasDefaultViewModelProviderFactory {
            override val viewModelStore get() = store                          // ← scoping
            override val defaultViewModelProviderFactory
                get() = SavedStateViewModelFactory()                          // ← NOT our factory
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides childOwner) {
        entry.Content()
    }
}

// pop → clear that entry's store → ViewModel.onCleared()
onPop =
    { key -> if (removeViewModelStoreOnPop()) entryViewModel.clearViewModelStoreOwnerForKey(key) }
```

Key takeaways:

- **Inside an entry, `LocalViewModelStoreOwner` is the per-entry owner, not the service.** The
  decorator *shadows* the host, and its default factory is hardcoded to `SavedStateViewModelFactory`
  — which is exactly why you must pass your own factory (gotcha #2). The service's
  `HasDefaultViewModelProviderFactory` is **never consulted on these screens**.
- **Scoping is by `entry.contentKey`.** Same key on the back stack → same store → same VM instance
  across recomposition. New key instance → new store → new VM. Pop the key → store cleared →
  `onCleared()`.
- **Clearing on pop, in a Service.** `removeViewModelStoreOnPop()` defaults to
  `LocalActivity.current?.isChangingConfigurations != true`. In an overlay there is **no** Activity,
  so it returns `true` → stores are reliably cleared on pop (no config-change retention games).
- **Survives recomposition / rotation for free.** The per-entry stores hang off the service's
  `ViewModelStore`, and the overlay service (unlike an `Activity`) isn't torn down on rotation — so
  the same objects persist. `rememberNavBackStack` is saveable, so the stack restores too. (This is
  in-process retention; it is *not* process-death restoration — the service does
  `performRestore(null)`, so nothing is persisted across process death.)

Observed lifecycle (A → B → C, then Back, Back) — confirmed on device:

```
A→B→C : ScreenA CREATED #1 · ScreenB CREATED #2 · ScreenC CREATED #3
Back  : ScreenC CLEARED #3            (B retained — its click counter is preserved)
Back  : ScreenB CLEARED #2            (A retained — it's the root, never cleared)
re-B  : ScreenB CREATED #4            (fresh instance, counter reset → proves scoping)
```

---

## Gotchas (the three that cost us the most)

### #1 — `No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner`

In nav3 **1.0.0**, `NavDisplay` registers its back handler through the **NavigationEvent** library
(`androidx.navigationevent`), *not* the old `OnBackPressedDispatcher`. An `Activity` provides a
`NavigationEventDispatcherOwner`; a `Service` does not → crash on first compose.

Fix — provide a no-op owner (the same trick the old keyboard service used):

```kotlin
internal val FakeNavigationEventDispatcherOwner: NavigationEventDispatcherOwner =
    object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher = NavigationEventDispatcher()
    }

// wrap the NavDisplay subtree:
CompositionLocalProvider(
    LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner,
) { /* AppTheme { NavDisplay(…) } */ }
```

Back navigation is driven by the on-screen buttons / back stack, so the no-op dispatcher is fine;
the
overlay window doesn't need to receive hardware-back events. (Alternatively, set it on the view tree
via `setViewTreeNavigationEventDispatcherOwner(...)`, like `attachOwners` does for the other
owners.)

> Pitfall we hit first: we wired an `OnBackPressedDispatcher` into the service — wrong mechanism for
> this nav3 version. Removed it. Don't repeat that.

### #2 — You **must** pass `viewModel(factory = …)`

Because the per-entry owner's default factory is hardcoded to `SavedStateViewModelFactory()` (see
the
decorator source above), which can only build VMs with a no-arg or `(SavedStateHandle)` constructor.
Our DI'd VMs have neither, so omitting the factory crashes exactly like the first `hiltViewModel()`
attempt. Passing the factory affects **construction only** — the per-entry owner still governs
**scoping**.

Optional ergonomics — hide it behind a `CompositionLocal` + helper so call sites stay clean:

```kotlin
val LocalOverlayViewModelFactory =
    staticCompositionLocalOf<ViewModelProvider.Factory> { error("not provided") }

@Composable
inline fun <reified T : ViewModel> overlayViewModel(): T =
    viewModel(factory = LocalOverlayViewModelFactory.current)
```

Provide it once (next to the `LocalNavigationEventDispatcherOwner` provider), then each screen is
just
`val vm = overlayViewModel<ScreenAViewModel>()`.

### #3 — decorator list: what actually worked

Google's Nav3 recipes pair `rememberSaveableStateHolderNavEntryDecorator()` +
`rememberViewModelStoreNavEntryDecorator()`. **That two-decorator list worked on nav3 1.0.0 in our
overlay** — VMs scoped and cleared correctly, no crash.

Caveat for the future: the VM decorator's `init` asserts the saved-state owner is at
`Lifecycle.State.INITIALIZED`, and its error message names a `SavedStateNavEntryDecorator`. We did
**not** trip it, but if a future artifact version throws
`…requires adding the SavedStateNavEntryDecorator…`, add the missing decorator to the list and
verify
against the **artifact sources**, not the docs (the recipe docs and the shipped code drifted apart
in
1.0.0 — that's how we found the gotchas above).

---

## Decoupling summary (the mental model)

| Concern                                       | Who provides it                                                       | Notes                                  |
|-----------------------------------------------|-----------------------------------------------------------------------|----------------------------------------|
| Compose can compose at all                    | the `Service` (Lifecycle/VMStore/SavedState owner + view-tree owners) | overlay has no Activity                |
| **Which** `ViewModelStore` (scoping/clearing) | `rememberViewModelStoreNavEntryDecorator()`                           | per `entry.contentKey`, cleared on pop |
| **How** a VM is constructed (DI)              | `OverlayViewModelFactory` via `viewModel(factory = …)`                | singleton-graph multibinding map       |
| Back handling plumbing                        | `FakeNavigationEventDispatcherOwner`                                  | nav3 1.0.0 NavigationEvent library     |

Scoping vs. DI are independent axes. Nav3 gives you the first for free; the second is on you because
Hilt's ViewModel support is Activity-rooted.

---

## File map

**Reusable scaffold — still in the repo:**

| File                                | Role                                                                        |
|-------------------------------------|-----------------------------------------------------------------------------|
| `mascot/DeckardOverlayService.kt`   | the `Service` host: owns Lifecycle/VMStore/SavedState; adds overlay windows |
| `mascot/OverlayViewModelFactory.kt` | `ViewModelProvider.Factory` over a `Map<Class, Provider<ViewModel>>`        |
| `di/OverlayViewModelModule.kt`      | `@Binds @IntoMap @ClassKey` ViewModel bindings (singleton graph)            |
| `mascot/ExperimentViewModel.kt`     | example singleton-graph VM (no Nav3) — the one live `@IntoMap` binding      |

**Nav3 overlay panel — removed (rebuild from this doc):** the POC was a right-edge
`AbstractComposeView`
hosting `NavDisplay` with the two decorators and `FakeNavigationEventDispatcherOwner`, plus three
per-screen `@Inject` VMs that logged create/clear. It was deleted after the experiment; recreate it
from the [Wiring](#wiring-it-together-the-overlay-view--the-service) and
[Nav3 setup](#navigation-3-setup-stable-100) sections. (Git history has the original
`poc/PocNavOverlayView.kt` + `poc/PocScreenViewModels.kt` if you want the verbatim version.)

## Dependencies (already present in this project)

```toml
navigation3 = "1.0.0"    # androidx.navigation3:navigation3-runtime / navigation3-ui
lifecycleViewmodel = "2.10.0"   # androidx.lifecycle:lifecycle-viewmodel-navigation3  (scoping decorator)
# androidx.navigationevent + navigationevent-compose come in transitively via nav3 (no explicit dep)
```

`:app` already declares `navigation3-runtime`, `navigation3-ui`, and
`lifecycle-viewmodel-navigation3`.
```
