package ru.hebi.cribSheet.mail

import java.util.*
import kotlin.collections.HashSet

internal fun hash(vararg obj: Any?) = obj.sumOf { 31 * hashCode(it) }

internal fun hashCode(obj: Any?) = when (obj) {
    //к сожалению эти массивы не имеют ничего общего, кроме "Array" в названии, поэтому приходится копи-пастить
    is Array<*> -> obj.contentHashCode()
    is ByteArray -> obj.contentHashCode()
    is ShortArray -> obj.contentHashCode()
    is IntArray -> obj.contentHashCode()
    is LongArray -> obj.contentHashCode()
    is FloatArray -> obj.contentHashCode()
    is DoubleArray -> obj.contentHashCode()
    is CharArray -> obj.contentHashCode()

    null -> 0
    else -> obj.hashCode()
}

internal inline fun <I, O> Set<I>.map(transform: (I) -> O): Set<O> {
    return mapTo(HashSet(this.size), transform)
}

internal fun decodeFromBase64(value: String) = Base64.getDecoder().decode(value)
internal fun decodeFromBase64ToString(value: String) = String(decodeFromBase64(value))
internal fun encodeToBase64ToString(value: ByteArray): String = Base64.getEncoder().encodeToString(value)
internal fun encodeToBase64ToString(value: String): String = encodeToBase64ToString(value.toByteArray())