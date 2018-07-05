
fun List<String>.matchedStr(targetStr: String): String? {
    this.forEach {
        if ( """^${targetStr}.*""".toRegex().matches(it)) {
            return it
        }
    }
    return null
}