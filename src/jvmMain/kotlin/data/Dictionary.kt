package data

object Dictionary {
    const val windowTitle = "Cucumber Converter"
    const val generateScenarios = "Generate"
    const val chooseScenariosLabel = "Click to choose main scenarios directory"
    const val chooseFolderToGenerateIn = "Click to choose the folder to generate test files in"
    const val chooseDirectoryCd = "Choose directory"
    const val chooseValidDirectory = "Choose valid %s directory"
    const val noScenariosFound = "No scenarios found, choose main scenario directory"
    const val scenarioReadingError = "Failed to read scenario file: %s"
    const val loadingCd = "Loading..."
}

fun String.formatWithParam(param: String): String {
    return String.format(this, param)
}