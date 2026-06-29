# Navigation 3 + scoped ViewModels inside a Service-hosted Compose overlay

How we run Jetpack **Navigation 3** with **per-screen, dependency-injected ViewModels** inside a
Compose UI that lives in a **`WindowManager` overlay driven by a plain `Service`** — not an
`Activity`.

This is unusual: every "it just works" affordance in Compose/Hilt/Nav3 assumes an `Activity` (or
`Fragment`/`NavBackStackEntry`) is the host. A `Service` has none of that machinery, so each missing
piece has to be supplied by hand. The payoff: a floating panel that navigates across screens, each
with its own ViewModel that is DI'd, retained across recomposition, and cleared when its screen is
popped.

> Context in this repo: the mascot overlay is hosted by `DeckardOverlayService` (a started
`Service`,
> no `Activity`). The POC panel proving this out is `poc/PocNavOverlayView.kt`.

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
5. **Two host shims Nav3 needs that an `Activity` gives for free:**
    - a `LocalNavigationEventDispatcherOwner` (nav3 1.0.0 routes back through the *NavigationEvent*
      library) — provide a `FakeNavigationEventDispatcherOwner`.
    - the saved-state owner plumbing the entry decorators read.

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
}
```

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
java.lang.RuntimeException: Cannot create an instance of class …ExperimentViewModel
    at …DefaultViewModelProviderFactory.create        ← the reflective default factory ran
Caused by: java.lang.NoSuchMethodException: …ExperimentViewModel.<init> []   ← it wanted a no-arg ctor
```

What happened, step by step:

1. `hiltViewModel()` calls `createHiltViewModelFactory(owner)`.
2. The owner (the service) is **not** a `HasDefaultViewModelProviderFactory` *that Hilt recognizes*,
   so
   it returns `null` and falls back to the reflective `DefaultViewModelProviderFactory`.
3. That factory only knows how to call a **no-arg** constructor → `NoSuchMethodException`.

The deeper reason it can *never* work from a `Service`:

```
SingletonComponent
 └── ActivityRetainedComponent      ← created off an Activity (retained via a hidden VM)
      └── ViewModelComponent        ← @HiltViewModel bindings live HERE
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
class PocScreenAViewModel @Inject constructor(
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
    @ClassKey(PocScreenAViewModel::class)
    fun bindA(vm: PocScreenAViewModel): ViewModel
    // …B, C
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
                        val vm: PocScreenAViewModel =
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

- `entry<T> { }` is a **member** of `EntryProviderScope` (the `entryProvider { }` receiver) — no
  import.
- `rememberNavBackStack(...)` requires routes to be `@Serializable` + `NavKey`.
- Back stack is just a `MutableList<NavKey>`: navigate with `.add(...)`, pop with
  `.removeLastOrNull()`.

---

## The important bit: how per-entry VM scoping actually works

`rememberViewModelStoreNavEntryDecorator()` wraps every entry's content. Reading its source
(`androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator`) is what makes the whole
thing click:

```kotlin
decorate = { entry ->
    // one ViewModelStore PER entry.contentKey, kept in an EntryViewModel that lives in the
    // PARENT (here: the service's) ViewModelStore:
    val viewModelStore = viewModelStore.getEntryViewModel().viewModelStoreForKey(entry.contentKey)

    val childViewModelStoreOwner = remember {
        object : ViewModelStoreOwner,
            SavedStateRegistryOwner by …, HasDefaultViewModelProviderFactory {
        override val viewModelStore get() = viewModelStore          // ← scoping
        override val defaultViewModelProviderFactory
        get() = SavedStateViewModelFactory()                   // ← NOT our factory
    }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides childViewModelStoreOwner) {
        entry.Content()
    }
}

// pop → clear that entry's store → ViewModel.onCleared()
onPop =
    { key -> if (removeViewModelStoreOnPop()) entryViewModel.clearViewModelStoreOwnerForKey(key) }
```

Key takeaways:

- **Inside an entry, `LocalViewModelStoreOwner` is the per-entry owner, not the service.** The
  decorator *shadows* the host. So the service's own `HasDefaultViewModelProviderFactory` is **never
  consulted on these screens** — it only matters for `viewModel()` calls outside a `NavDisplay`.
- **Scoping is by `entry.contentKey`.** Same key on the back stack → same store → same VM instance
  across recomposition. New key instance → new store → new VM. Pop the key → store cleared →
  `onCleared()`.
- **Survives recomposition / in-process reconfiguration for free.** The per-entry stores hang off
  the
  service's `ViewModelStore`, and the overlay service (unlike an `Activity`) isn't torn down on
  rotation — so the same objects simply persist. `rememberNavBackStack` is saveable, so the stack
  restores too.

Observed lifecycle (A → B → C, then Back, Back):

```
A→B→C : ScreenA CREATED #1 · ScreenB CREATED #2 · ScreenC CREATED #3
Back  : ScreenC CLEARED #3            (B retained — its click counter is preserved)
Back  : ScreenB CLEARED #2            (A retained — it's the root, never cleared)
re-B  : ScreenB CREATED #4            (fresh instance, counter reset → proves scoping)
```

---

## Gotchas (the two that cost us the most)

### #1 — `No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner`

In nav3 **1.0.0**, `NavDisplay` registers its back handler through the **NavigationEvent** library
(`androidx.navigationevent`), *not* the old `OnBackPressedDispatcher`. An `Activity` provides a
`NavigationEventDispatcherOwner`; a `Service` does not → crash on first compose.

Fix — provide a no-op owner (lifted verbatim from the old keyboard service):

```kotlin
internal val FakeNavigationEventDispatcherOwner: NavigationEventDispatcherOwner =
    object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher = NavigationEventDispatcher()
    }

// wrap the NavDisplay subtree:
CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides FakeNavigationEventDispatcherOwner) { … }
```

Back navigation is driven by the on-screen buttons / back stack, so the no-op dispatcher is fine;
the
overlay window doesn't need to receive hardware-back events for the demo. (The old service also set
it
on the view tree via `setViewTreeNavigationEventDispatcherOwner` as a belt-and-suspenders
alternative.)

> Pitfall we hit: we first wired an `OnBackPressedDispatcher` into the service — wrong mechanism for
> this nav3 version. Removed it.

### #2 — You **must** pass `viewModel(factory = …)`

Because the per-entry owner's default factory is hardcoded to `SavedStateViewModelFactory()` (see
the
decorator source above), which can only build VMs with a no-arg or `(SavedStateHandle)` constructor.
Our DI'd VMs have neither, so omitting the factory crashes exactly like the first `hiltViewModel()`
attempt. Passing the factory affects **construction only** — the per-entry owner still governs
**scoping**.

Optional ergonomics — hide it behind a `CompositionLocal` + helper so call sites stay clean:

```kotlin
@Composable
inline fun <reified T : ViewModel> overlayViewModel(): T =
    viewModel(factory = LocalOverlayViewModelFactory.current)
```

### #3 — version drift in the recipes

Google's Nav3 recipes paired only `rememberSaveableStateHolderNavEntryDecorator()` +
`rememberViewModelStoreNavEntryDecorator()`. The 1.0.0 VM decorator's `init` asserts the saved-state
owner is at `Lifecycle.State.INITIALIZED` and its error names a `SavedStateNavEntryDecorator`. If
you
hit `requires adding the SavedStateNavEntryDecorator…`, the decorator list needs updating for your
exact artifact versions. (Verify against the artifact sources, not the docs.)

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

| File                                | Role                                                                                |
|-------------------------------------|-------------------------------------------------------------------------------------|
| `mascot/DeckardOverlayService.kt`   | the `Service` host: owns Lifecycle/VMStore/SavedState; adds overlay windows         |
| `mascot/OverlayViewModelFactory.kt` | `ViewModelProvider.Factory` over a `Map<Class, Provider<ViewModel>>`                |
| `di/OverlayViewModelModule.kt`      | `@Binds @IntoMap @ClassKey` ViewModel bindings (singleton graph)                    |
| `poc/PocNavOverlayView.kt`          | right-edge `NavDisplay` panel + decorators + `FakeNavigationEventDispatcherOwner`   |
| `poc/PocScreenViewModels.kt`        | the three per-screen DI'd ViewModels (log create/clear, instance id, click counter) |
| `mascot/ExperimentViewModel.kt`     | earlier standalone experiment VM (no Nav3) kept as scaffold                         |

## Dependencies (already present, mostly transitive)

```toml
navigation3 = "1.0.0"     # androidx.navigation3:navigation3-runtime / -ui
lifecycleViewmodel = "2.10.0"   # androidx.lifecycle:lifecycle-viewmodel-navigation3  (the scoping decorator)
# androidx.navigationevent + -compose come in transitively via nav3 (no explicit dep needed)
```
