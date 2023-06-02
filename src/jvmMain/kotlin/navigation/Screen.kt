package navigation

sealed class Screen(val params: List<String>?) {
    object GenerateScreen : Screen(params = emptyList())
    class ResultsScreen(results: List<String>? = null) : Screen(params = results)
}