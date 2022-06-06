package com.lovzoe

fun main() {
    val f = Foo(1,2)
    f.sumInts()
}

@IntSummable
data class Foo(
    val x: Int,
    val y: Int
)