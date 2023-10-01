import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Public interface of the store
 * Observe state changes collecting [stateFlow].
 * Access current state conveniently with [state] getter.
 * Dispatch intents to trigger effects using [dispatch] method.
 */
interface MMVIStore<S : MMVIState, I : MMVIIntent> {
    val stateFlow: StateFlow<S>
    val state: S get() = stateFlow.value
    fun dispatch(i: I)
}

internal class MMVIStoreImpl<S : MMVIState, I : MMVIIntent>(initial: S, override val coroutineContext: CoroutineContext = Dispatchers.Default) : MMVIStore<S, I>,
    CoroutineScope {
    private val mutex = Mutex()
    var effects = mutableMapOf<Pair<KClass<out S>, KClass<out I>>, (S, I) -> Flow<S>>()
    private val _mutStateFlow = MutableStateFlow(initial)
    override val stateFlow: StateFlow<S> get() = _mutStateFlow

    override fun dispatch(i: I) {
        effects[state::class to i::class]?.let { effect ->
            launch {
                effect(state, i).collect { s ->
                    mutex.withLock { _mutStateFlow.update { s } }
                }
            }
        }
    }
}