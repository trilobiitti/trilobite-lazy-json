package com.github.trilobiitti.json.parsers.lazy

/**
 * Deserializes JSON string into tree of immutable standard objects.
 *
 * Arrays are deserialized as [List]s, objects are deserialized as [Map]s.
 */
open class ImmutablePrimitiveLazyJsonParser : LazyJsonParser<
        Map<String, Any?>,
        List<Any?>,
        MutableMap<String, Any?>?,
        MutableList<Any?>?
        >() {
    override fun startObject(): MutableMap<String, Any?>? = null

    override fun addField(builder: MutableMap<String, Any?>?, fieldName: String, value: Any?): MutableMap<String, Any?>? =
        builder?.also { it[fieldName] = value } ?: mutableMapOf(fieldName to value)

    override fun endObject(builder: MutableMap<String, Any?>?): Map<String, Any?> = builder ?: emptyMap()

    private inner class LazyMap(
        source: String,
        spanStart: Int,
        spanEnd: Int
    ) : BaseLazyObject<Map<String, Any?>>(source, spanStart, spanEnd), Map<String, Any?> {
        override fun parse(source: String): Map<String, Any?> = parseObject(source, spanStart, spanEnd)

        // WTF-KOTLIN: Cannot delegate interface implementation to property
        override val entries: Set<Map.Entry<String, Any?>>
            get() = parsed.entries
        override val keys: Set<String>
            get() = parsed.keys
        override val size: Int
            get() = parsed.size
        override val values: Collection<Any?>
            get() = parsed.values

        override fun containsKey(key: String): Boolean = parsed.containsKey(key)
        override fun containsValue(value: Any?): Boolean = parsed.containsValue(value)
        override fun get(key: String): Any? = parsed.get(key)
        override fun isEmpty(): Boolean = parsed.isEmpty()

        override fun equals(other: Any?): Boolean = parsed == other
        override fun hashCode(): Int = parsed.hashCode()
    }

    override fun lazyObject(string: String, spanStart: Int, spanEnd: Int): Map<String, Any?> = LazyMap(
        string, spanStart, spanEnd
    )

    override fun startArray(): MutableList<Any?>? = null

    override fun addElement(builder: MutableList<Any?>?, value: Any?): MutableList<Any?>? =
        builder?.also { it.add(value) } ?: mutableListOf(value)

    override fun endArray(builder: MutableList<Any?>?): List<Any?> = builder ?: emptyList()

    private inner class LazyList(
        source: String,
        spanStart: Int,
        spanEnd: Int
    ) : BaseLazyObject<List<Any?>>(source, spanStart, spanEnd), List<Any?> {
        override fun parse(source: String): List<Any?> = parseArray(source, spanStart, spanEnd)

        override val size: Int
            get() = parsed.size

        override fun contains(element: Any?): Boolean = parsed.contains(element)
        override fun containsAll(elements: Collection<Any?>): Boolean = parsed.containsAll(elements)
        override fun get(index: Int): Any? = parsed.get(index)
        override fun indexOf(element: Any?): Int = parsed.indexOf(element)
        override fun isEmpty(): Boolean = parsed.isEmpty()
        override fun iterator(): Iterator<Any?> = parsed.iterator()
        override fun lastIndexOf(element: Any?): Int = parsed.lastIndexOf(element)
        override fun listIterator(): ListIterator<Any?> = parsed.listIterator()
        override fun listIterator(index: Int): ListIterator<Any?> = parsed.listIterator(index)
        override fun subList(fromIndex: Int, toIndex: Int): List<Any?> = parsed.subList(fromIndex, toIndex)

        override fun equals(other: Any?): Boolean = parsed == other
        override fun hashCode(): Int = parsed.hashCode()
    }

    override fun lazyArray(string: String, spanStart: Int, spanEnd: Int): List<Any?> = LazyList(
        string, spanStart, spanEnd
    )
}
