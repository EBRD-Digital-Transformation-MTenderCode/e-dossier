package com.procurement.dossier.infrastructure.utils

inline fun <T, V> Collection<T>.uniqueBy(selector: (T) -> V): Boolean {
    val unique = HashSet<V>()
    forEach { item ->
        if (!unique.add(selector(item))) return false
    }
    return true
}

inline fun <T, V> Collection<T>.toSetBy(selector: (T) -> V): Set<V> {
    val collections = LinkedHashSet<V>()
    forEach {
        collections.add(selector(it))
    }
    return collections
}

fun <T> T?.toList(): List<T> = if (this != null) listOf(this) else emptyList()