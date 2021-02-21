package com.github.trilobiitti.json.parsers.lazy

/**
 * Base class for lazily parsable objects of type [T].
 *
 * Keeps source string and range of it's characters containing object's data (represented as [spanStart] and [spanEnd]).
 *
 * Provides [parsed] property that can be accessed by a subclass and contains a value that is lazily parsed using a
 * [parse] method of a subclass.
 *
 * @param source the source string
 * @param spanStart index of the first character of this object's data in source string
 * @param spanEnd index of first character after this object's data in source string
 */
abstract class BaseLazyObject<T>(
    source: String,
    protected val spanStart: Int,
    protected val spanEnd: Int
) {
    protected abstract fun parse(source: String): T

    private var src: String? = source

    protected val parsed: T by lazy {
        val p = parse(src!!)

        // Don't keep reference to source string after it is parsed
        src = null

        p
    }

    override fun toString(): String = "${this::class.simpleName}(${src ?: parsed})"
}
