/**
 * Enables DSL-like store configuration and effect declarations.
 * Usage:
 *      class Store: MMVIStore<State, Intent> by MMVIStore({ ... })
 * Initialize store state using [MMVIStoreBuilder.startWith]
 * Bind states and intents to trigger effects using [MMVIStoreBuilder.bind].
 */
@Suppress("FunctionName")
fun <S : MMVIState, I : MMVIIntent> MMVIStore(configuration: MMVIStoreBuilder<S, I>.() -> Unit): MMVIStore<S, I> =
    MMVIStoreBuilderImpl<S, I>().apply(configuration).build()