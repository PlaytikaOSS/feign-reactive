@file:JvmName("KtCoroutinesUtils")

package reactivefeign.utils

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.coroutines.Continuation

internal fun Method.isSuspend(): Boolean {
    val classes: Array<Class<*>> = parameterTypes
    return classes.isNotEmpty() && classes[classes.size - 1] == Continuation::class.java
}

internal fun Method.suspendReturnType() = if (isSuspend()) {
    val types: Array<Type> = genericParameterTypes
    val conType = types[types.size - 1] as ParameterizedType
    conType
} else returnType