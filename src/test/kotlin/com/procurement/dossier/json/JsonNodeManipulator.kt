package com.procurement.dossier.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import java.math.BigDecimal
import java.math.BigInteger

fun JsonNode.getObject(vararg names: String): ObjectNode {
    var obj: ObjectNode = asType(names[0])
    for (index in 1 until names.size) {
        obj = obj.asType(names[index])
    }
    return obj
}

inline fun JsonNode.getObject(vararg names: String, block: ObjectNode.() -> Unit): ObjectNode =
    getObject(*names).apply { block(this) }

fun JsonNode.getArray(name: String): ArrayNode = asType(name)
inline fun JsonNode.getArray(name: String, block: ArrayNode.() -> Unit): ArrayNode =
    asType<ArrayNode>(name).apply(block)

fun JsonNode.getBoolean(name: String): BooleanNode = asType(name)
fun JsonNode.getNumeric(name: String): NumericNode = asType(name)
fun JsonNode.getBigDecimal(name: String): DecimalNode = asType(name)
fun JsonNode.getBigInteger(name: String): BigIntegerNode = asType(name)
fun JsonNode.getDouble(name: String): DoubleNode = asType(name)
fun JsonNode.getFloat(name: String): FloatNode = asType(name)
fun JsonNode.getInt(name: String): IntNode = asType(name)
fun JsonNode.getLong(name: String): LongNode = asType(name)
fun JsonNode.getShort(name: String): ShortNode = asType(name)
fun JsonNode.getString(name: String): TextNode = asType(name)

fun ArrayNode.getObject(index: Int): ObjectNode = asType(index)

fun ArrayNode.getObject(index: Int, block: ObjectNode.() -> Unit): ObjectNode =
    asType<ObjectNode>(index).apply { block(this) }

fun ArrayNode.getBoolean(index: Int): BooleanNode = asType(index)
fun ArrayNode.getNumeric(index: Int): NumericNode = asType(index)
fun ArrayNode.getBigDecimal(index: Int): DecimalNode = asType(index)
fun ArrayNode.getBigInteger(index: Int): BigIntegerNode = asType(index)
fun ArrayNode.getDouble(index: Int): DoubleNode = asType(index)
fun ArrayNode.getFloat(index: Int): FloatNode = asType(index)
fun ArrayNode.getInt(index: Int): IntNode = asType(index)
fun ArrayNode.getLong(index: Int): LongNode = asType(index)
fun ArrayNode.getShort(index: Int): ShortNode = asType(index)
fun ArrayNode.getString(index: Int): TextNode = asType(index)

fun ArrayNode.putObject(value: ObjectNode): ArrayNode = this.apply { add(value) }

inline fun ObjectNode.putObject(name: String, block: ObjectNode.() -> Unit): ObjectNode =
    this.putObject(name).apply { block(this) }

fun ObjectNode.putObject(name: String, value: ObjectNode): ObjectNode = this.apply { set(name, value) }
fun ObjectNode.putAttribute(name: String, value: Boolean): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: BigDecimal): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: BigInteger): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: Double): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: Float): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: Int): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: Long): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: Short): ObjectNode = this.put(name, value)
fun ObjectNode.putAttribute(name: String, value: String): ObjectNode = this.put(name, value)

fun ObjectNode.setAttribute(name: String, value: Boolean): ObjectNode =
    this.apply { set(name, if (value) BooleanNode.TRUE else BooleanNode.FALSE) }

fun ObjectNode.setAttribute(name: String, value: BigDecimal): ObjectNode = this.apply { set(name, DecimalNode(value)) }
fun ObjectNode.setAttribute(name: String, value: BigInteger): ObjectNode =
    this.apply { set(name, BigIntegerNode(value)) }

fun ObjectNode.setAttribute(name: String, value: Double): ObjectNode = this.apply { set(name, DoubleNode(value)) }
fun ObjectNode.setAttribute(name: String, value: Float): ObjectNode = this.apply { set(name, FloatNode(value)) }
fun ObjectNode.setAttribute(name: String, value: Int): ObjectNode = this.apply { set(name, IntNode(value)) }
fun ObjectNode.setAttribute(name: String, value: Long): ObjectNode = this.apply { set(name, LongNode(value)) }
fun ObjectNode.setAttribute(name: String, value: Short): ObjectNode = this.apply { set(name, ShortNode(value)) }
fun ObjectNode.setAttribute(name: String, value: String): ObjectNode = this.apply { set(name, TextNode(value)) }

fun ObjectNode.deepCopy(block: ObjectNode.() -> Unit): ObjectNode = this.deepCopy().apply(block)

inline fun <reified T> JsonNode.asType(name: String): T {
    val node: JsonNode? = this.get(name)
    return if (node != null) {
        when (node) {
            is T -> node
            else -> throw IllegalStateException("Element '$name' is not a ${T::class.simpleName}")
        }
    } else
        throw IllegalArgumentException("Element with name '$name' not found.")
}

inline fun <reified T> ArrayNode.asType(index: Int): T {
    val node: JsonNode? = this.get(index)
    return if (node != null) {
        when (node) {
            is T -> node
            else -> throw IllegalStateException("Element '$index' is not a ${T::class.simpleName}")
        }
    } else
        throw IllegalArgumentException("Element with index '$index' not found.")
}
