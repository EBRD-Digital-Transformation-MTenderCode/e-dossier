package com.procurement.dossier.application.service

import com.procurement.dossier.infrastructure.utils.toSetBy

fun <R, A, K> updateStrategy(
    receivedElements: List<R>,
    keyExtractorForReceivedElement: (R) -> K,
    availableElements: List<A>,
    keyExtractorForAvailableElement: (A) -> K,
    updateBlock: A.(R) -> A,
    createBlock: (R) -> A
): List<A> {
    val receivedElementsById = receivedElements.associateBy { keyExtractorForReceivedElement(it) }
    val availableElementsIds = availableElements.toSetBy { keyExtractorForAvailableElement(it) }

    val updatedElements: MutableList<A> = mutableListOf()

    availableElements.forEach { availableElement ->
        val id = keyExtractorForAvailableElement(availableElement)
        val element = receivedElementsById[id]
            ?.let { receivedElement ->
                availableElement.updateBlock(receivedElement)
            }
            ?: availableElement
        updatedElements.add(element)
    }

    val newIds = receivedElementsById.keys - availableElementsIds
    newIds.forEach { id ->
        val receivedElement = receivedElementsById.getValue(id)
        val element = createBlock(receivedElement)
        updatedElements.add(element)
    }

    return updatedElements
}