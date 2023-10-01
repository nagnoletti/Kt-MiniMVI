import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlin.reflect.KClass

interface MMVIStoreBuilder<S : MMVIState, I : MMVIIntent> {
    /**
     * Initialize [MMVIStore] providing an initial state.
     */
    fun startWith(s: S): MMVIStoreBuilder<S, I>

    /**
     * Register an effect that can be triggered when the declared [KClass] [s] is matched
     * and a new intent gets dispatched and matches the declared [KClass] [i].
     * Usage: bind(StateA::class, IntentA::class) { state, intent, dispatch -> /* effect */ }
     * Effects are [ProducerScope] functions that can emit states.
     * Inside the effect's lambda you can access the (current) matched state,
     * the intent that triggered the effect, and the store's dispatch function.
     * Dispatching another Intent might be useful when different intents can
     * share states and behavior of effects, like resetting state or reloading data.
     */
    fun bind(s: KClass<out S>, i: KClass<out I>, effect: suspend ProducerScope<S>.(S, I, dispatch: ((I) -> Unit)) -> Unit): MMVIStoreBuilder<S, I>
}

internal class MMVIStoreBuilderImpl<S : MMVIState, I : MMVIIntent> : MMVIStoreBuilder<S, I> {

    private lateinit var store: MMVIStoreImpl<S, I>
    override fun startWith(s: S) = apply { store = MMVIStoreImpl(s) }

    override fun bind(s: KClass<out S>, i: KClass<out I>, effect: suspend ProducerScope<S>.(S, I, dispatch: ((I) -> Unit)) -> Unit): MMVIStoreBuilder<S, I> =
        apply {
            (s to i).let { key ->
                store.effects[key]?.let {
                    throw IllegalStateException(
                        "Store already contains an effect triggered when state is of class $s and receives an intent of class $i. Check your store builder definitions!"
                    )
                } ?: run {
                    store.effects[key] = { s, i ->
                        channelFlow {
                            effect(s, i, store::dispatch)
                            close()
                        }
                    }
                }
            }
        }

    fun build(): MMVIStore<S, I> = when {
        !::store.isInitialized -> throw IllegalStateException("Store configuration initial state needed, provide it using `startWith` builder method.")
        else -> store
    }

}
