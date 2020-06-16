package com.procurement.dossier.domain.util.extension

inline fun Boolean.doOnFalse(block: () -> Nothing) {
    if (!this) block()
}