fun test(vararg args: Any) {
    println("Called vararg Any: ${args.joinToString()}")
}

fun test(vararg namedArgs: Pair<String, Any>) {
    println("Called vararg Pair: ${namedArgs.joinToString()}")
}

fun main() {
    test("a" to "b")
    test("a" to "b", "c" to "d")
    test("a", "b")
    test("a" to "b", "c")
}
