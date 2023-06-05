package playground.common.idempotency.impl

import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KProperty

/**
 * Delegate for extension properties on [Target], that stores property value in [Store.genericStore].
 *
 * @param Target receiver type of the extension property. Must implement [Store].
 * @param Type of the extension property
 * @param namespace fully qualified name of package / class where the property is defined.
 *                  Will be suffixed by property name and then used as key to [Store.genericStore]
 * @param lazyInitializer called to provide initial value, if the property is accessed before previous initialization.
 */
class GenericProperty<Type, Target : GenericProperty.Store>(
    private val namespace: String,
    private val lazyInitializer: Target.() -> Type
) {

    /**
     * Allows storing arbitrary values in [genericStore]. Best used with [GenericProperty].
     *
     * Keys should be fully qualified field name using JVM package naming conventions to avoid conflicts.
     */
    interface Store {
        val genericStore: ConcurrentMap<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Target, property: KProperty<*>): Type {
        return thisRef.genericStore.computeIfAbsent("$namespace.${property.name}") { thisRef.lazyInitializer() } as Type
    }

    operator fun setValue(thisRef: Target, property: KProperty<*>, value: Type) {
        thisRef.genericStore["$namespace.${property.name}"] = value
    }
}
