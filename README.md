# Lazy JSON parser

Lazy JSON parser parses JSON lazily.
I.e. when it first meets beginning of nested object (`{`) or array (`[`) it doesn't parse the object but instead just skips it (and all nested objects/arrays) and keeps a record "object/array starts at this position".
This approach is mostly useful when parsing and/or allocation of nested objects/arrays is expensive in terms of memory or time.

## Features

* Lazy parsing of JSON
* Written in Common Kotlin, thus can be compiled for any of supported platforms (JVM, JS, native) without changes in behavior
* Support customization for addition of new types of parsed objects

## Usage

Current implementation accepts input data as string only.

### ImmutablePrimitiveLazyJsonParser

`ImmutablePrimitiveLazyJsonParser` parses JSON into tree of immutable `Map`s and `List`s.
 It uses custom implementations of `Map` and `List` so the actual content is parsed on demand.

```kotlin
val json = ImmutablePrimitiveLazyJsonParser()

json.parse("""{"foo": ["bar", 42]}""")
// -> (lazy equivalent of) mapOf("foo" to listOf("bar", 42.0))
```

### Extending LazyJsonParser

It's possible to create custom lazy parser that parses JSON into structures different from tree of maps and lists by extending `LazyJsonParser` class and implementing some methods:

```kotlin
interface MyObject { /*...*/ }

class MyObjectBuilder { /*...*/ }

interface MyArray { /*...*/ }

class MyArrayBuilder { /*...*/ }

class MyParser: LazyJsonParser<
    MyObject, MyArray,
    MyObjectBuilder, MyArrayBuilder
> {
    class MyLazyArray(
        source: String,
        spanStart: Int,
        spanEnd: Int
    ): BaseLazyObject<MyObject>(source, spanStart, spanEnd), MyObject {
        override fun parse(source: String): MyObject = parseObject(source, spanStart, spanEnd)
        
        // ... MyArray method implementations ...
    }
    
    class MyLazyObject(
        source: String,
        spanStart: Int,
        spanEnd: Int
    ): BaseLazyObject<MyObject>(source, spanStart, spanEnd), MyObject {
        override fun parse(source: String): MyObject = parseArray(source, spanStart, spanEnd)
        
        // ... MyObject method implementations ...
    }

    override fun lazyObject(string: String, spanStart: Int, spanEnd: Int): Map<String, Any?> = MyLazyObject(
        string, spanStart, spanEnd
    )
    
    // Method called when parser starts to parse actual object content
    override fun startObject(): MyObjectBuilder = MyObjectBuilder()

    // Method called when parser meets a field in document content.
    // `builder` argument is a value returned by `startObject` call
    // or by previous call of `addField`.
    override fun addField(builder: MyObjectBuilder, fieldName: String, value: Any?): MyObjectBuilder = builder.field(fieldName, value)

    // Method called when parser reaches the end of object while
    // parsing it's content. `builder` is a value returned by 
    // `startObject` or by las call of `addField`.
    override fun endObject(builder: MyObjectBuilder): MyObject = builder.build()

    // Method called when a parser skips an object.
    override fun lazyObject(string: String, spanStart: Int, spanEnd: Int): MyObject = MyLazyObject(
        string, spanStart, spanEnd
    )

    // Same methods for arrays...

    override fun startArray(): MyArrayBuilder = MyArrayBuilder()
    override fun addElement(builder: MyArrayBuilder, value: Any?): MyArrayBuilder = builder.add(value)
    override fun endArray(builder: MyArrayBuilder): MyArray = builder.build()
    override fun lazyArray(string: String, spanStart: Int, spanEnd: Int): MyArray = MyArray(
        string, spanStart, spanEnd
    )
}
```
