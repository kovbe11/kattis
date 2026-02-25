package com.softpaw.systems.resp

data class Tuple2<out A, out B>(val a: A, val b: B)
data class Tuple3<out A, out B, out C>(val a: A, val b: B, val c: C)
data class Tuple4<out A, out B, out C, out D>(val a: A, val b: B, val c: C, val d: D)
data class Tuple5<out A, out B, out C, out D, out E>(val a: A, val b: B, val c: C, val d: D, val e: E)

inline infix fun <reified A, reified B> A.and(b: B): Tuple2<A, B> =
    Tuple2(this, b)

inline infix fun <reified A, reified B, reified C> Tuple2<A, B>.and(c: C): Tuple3<A, B, C> =
    Tuple3(this.a, this.b, c)

inline infix fun <reified A, reified B, reified C, reified D> Tuple3<A, B, C>.and(d: D): Tuple4<A, B, C, D> =
    Tuple4(this.a, this.b, this.c, d)

inline infix fun <reified A, reified B, reified C, reified D, reified E> Tuple4<A, B, C, D>.and(e: E): Tuple5<A, B, C, D, E> =
    Tuple5(this.a, this.b, this.c, this.d, e)
