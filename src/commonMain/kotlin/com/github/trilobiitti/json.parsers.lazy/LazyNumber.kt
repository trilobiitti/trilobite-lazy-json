package com.github.trilobiitti.json.parsers.lazy

/**
 * Number implementation that stores a string representation of a number and lazily parses numeric value required type.
 */
/*
// WTF-KOTLIN-JS: Fails with `TypeError: Cannot read property 'prototype' of undefined` from `kotlin.Number`
// https://youtrack.jetbrains.com/issue/KT-17345
class LazyNumber(
    private val source: String
) : Number() {
    override fun toByte(): Byte = source.toByte()
    override fun toChar(): Char = source.toInt().toChar()
    override fun toDouble(): Double = source.toDouble()
    override fun toFloat(): Float = source.toFloat()
    override fun toInt(): Int = source.toInt()
    override fun toLong(): Long = source.toLong()
    override fun toShort(): Short = source.toShort()

    override fun toString(): String = source
}
*/
