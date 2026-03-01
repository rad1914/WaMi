// @path: app/_p6_disabled/lambda/lambda.kt
// @path: app/src/main/java/com/radwrld/lambda/lambda.kt
fun suma(a: Int, b: Int): Int {
    return a + b
}

fun main() {
    println(suma(3, 4))

    val suma_lambda: (Int, Int) -> Int = { a, b -> a + b }
    println(suma_lambda(3, 4))
}
