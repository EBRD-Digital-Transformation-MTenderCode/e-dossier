package com.procurement.dossier.domain.util.extension

import com.procurement.dossier.domain.util.Option
import com.procurement.dossier.domain.util.Result

inline fun <T, V> Collection<T>.isUnique(selector: (T) -> V): Boolean {
    val unique = HashSet<V>()
    forEach { item ->
        if (!unique.add(selector(item))) return false
    }
    return true
}

inline fun <T, V> Collection<T>?.getDuplicate(selector: (T) -> V): T? {
    val unique = HashSet<V>()
    this?.forEach { item ->
        if (!unique.add(selector(item))) return item
    }
    return null
}

inline fun <T, V> Collection<T>.toSetBy(selector: (T) -> V): Set<V> {
    val collections = LinkedHashSet<V>()
    forEach {
        collections.add(selector(it))
    }
    return collections
}

fun <T, R, E> List<T>.mapResult(block: (T) -> Result<R, E>): Result<List<R>, E> {
    val r = mutableListOf<R>()
    for (element in this) {
        when (val result = block(element)) {
            is Result.Success -> r.add(result.get)
            is Result.Failure -> return result
        }
    }
    return Result.success(r)
}

fun <T, R, E> List<T>?.mapOptionalResult(block: (T) -> Result<R, E>): Result<Option<List<R>>, E> {
    if (this == null)
        return Result.success(Option.none())
    val r = mutableListOf<R>()
    for (element in this) {
        when (val result = block(element)) {
            is Result.Success -> r.add(result.get)
            is Result.Failure -> return result
        }
    }
    return Result.success(Option.pure(r))
}

fun <T> Iterable<T>.getUnknownElements(received: Iterable<T>) = received.getNewElements(known = this)

fun <T> Iterable<T>.getNewElements(known: Iterable<T>): Set<T> {
    val receivedAsSet = this.asSet()
    val knownAsSet = known.asSet()
    return receivedAsSet.subtract(knownAsSet)
}

fun <T> Iterable<T>.getElementsForUpdate(received: Iterable<T>): Set<T> {
    val receivedAsSet = received.asSet()
    val knownAsSet = this.asSet()
    return knownAsSet.intersect(receivedAsSet)
}

private fun <T> Iterable<T>.asSet(): Set<T> = when (this) {
    is Set -> this
    else -> this.toSet()
}
