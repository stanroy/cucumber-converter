package data

import common.capitalizeIt
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class Generator {

    private fun version(): String {
        val version = System.getProperty("jpackage.app-version")

        return if (version.isNullOrEmpty()) {
            val buildGradle = File("build.gradle.kts")

            val regex = Regex("""version\s*=\s*["'](.+?)["']""")
            val matchResult = regex.find(buildGradle.readText())

            "DEBUG ${matchResult?.groupValues?.getOrNull(1)}"
        } else {
            version
        }
    }

    private fun timeStamp(): String? {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)

        return now.format(formatter)
    }

    private fun splitScenarios(scenarioFileContents: String): Map<String, Map<String, List<String>>> {
        // Split the scenario file contents into lines and perform initial filtering
        val scenarioFileLines =
            scenarioFileContents.lines().filter { it.isNotBlank() }.filterNot { it.lowercase().contains("feature") }
                .map { it.trimStart() }

        // Create a map to store the scenarios and their steps with data tables
        val scenarioMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        var currentStep: String? = null
        var stepCount = 1
        // Go over each line in the scenario file
        scenarioFileLines.forEach { line ->
            when {
                line.startsWith("Scenario:") -> {
                    // If line starts with "Scenario:", it indicates a new scenario
                    // Create a new map to store the steps and data tables of the current scenario
                    scenarioMap[line] = mutableMapOf()
                    currentStep = null
                }

                line.startsWith("|") -> {
                    // If line starts with "|", it indicates a data table row
                    if (currentStep != null) {
                        // Add the data table row to the current step in the scenario map
                        scenarioMap.values.lastOrNull()?.get(currentStep!!)?.add(line)
                    }
                }

                line.isBlank() -> {
                    // If line is blank, it indicates the end of a step or data table
                    currentStep = null
                }

                line.startsWith("#") -> {
                    // line starts with a #, it indicates that this line is a comment
                    currentStep = null
                }

                else -> {
                    // Otherwise, it's a regular step line
                    currentStep = line
                    // Check if the step key already exists
                    if (scenarioMap.values.lastOrNull()?.containsKey(currentStep) == true) {
                        stepCount++
                        currentStep = "$currentStep cucumber-converter-$stepCount"
                    } else {
                        stepCount = 1
                    }
                    scenarioMap.values.lastOrNull()?.set(currentStep!!, mutableListOf())
                }
            }
        }

        return scenarioMap
    }


    fun createSwiftFile(scenarioFile: ScenarioFile, filePath: String) {
        val subfolderPath = "$filePath/${scenarioFile.subFolder}"
        val subfolder = File(subfolderPath)

        // Create subfolder if it doesn't exist
        subfolder.mkdir()

        val swiftFileContents = createSwiftFileContents(scenarioFile)
        val swiftFilePath = "$subfolderPath/${scenarioFile.fileName}.swift"

        // Generate file
        val fileWriter = FileWriter(swiftFilePath)
        fileWriter.write(swiftFileContents)
        fileWriter.close()
    }

    fun clearOldTestFiles(folderPath: String) {
        val folder = File(folderPath)

        // Check if the folder exists
        if (!folder.exists() || !folder.isDirectory) {
            return
        }

        // Check if parent folder is named features
        if (folder.parentFile.name != "features") {
            return
        }

        // Get all files and subdirectories in the folder
        val files = folder.listFiles()

        // Delete files and subdirectories
        files?.forEach { file ->
            if (file.isDirectory) {
                // Recursively delete subfolders
                clearOldTestFiles(file.absolutePath)
            }
            // Delete file or empty subfolder
            file.delete()
        }
    }


    private fun createSwiftFileContents(scenarioFile: ScenarioFile): String {
        val scenariosMap = splitScenarios(scenarioFile.fileContents)
        fun generateFileContents(): String {

            val functions = StringBuilder()

            functions.apply {
                appendLine("//")
                appendLine("//  ${scenarioFile.fileName}.swift")
                appendLine("//  Generated by: Cucumber Converter ${version()}")
                appendLine("//  Generated on: ${timeStamp()}")
                appendLine("//")
                appendLine()
                appendLine("import XCTest")
                appendLine()
                appendLine("class ${scenarioFile.fileName.capitalizeIt()}: XCTestCase, Steps {")
                appendLine("    let app = XCUIApplication()")
                appendLine()
                scenariosMap.forEach { (scenario, stepMap) ->
                    val scenarioSection = formatScenarioSection(scenario, stepMap)
                    appendLine(scenarioSection)
                }
                appendLine("}")
            }

            return functions.toString()
        }


        return generateFileContents()
    }

    private fun formatScenarioSection(scenario: String, stepMap: Map<String, List<String>>): String {
        val scenarioFunctionBuilder = StringBuilder()
        val scenarioName =
            scenario.substringAfter(':').trimStart().lowercase().filterNot { it == '\'' }.filterNot { it == '-' }
                .replace(' ', '_')

        val scenarioFunctionSignature = "func test_$scenarioName() throws {"

        scenarioFunctionBuilder.apply {
            appendLine("    $scenarioFunctionSignature")
            stepMap.forEach { (step, dataTable) ->
                appendLine(formatStepFunction(step, dataTable))
            }
            appendLine("    }")
            appendLine(" ")
        }


        return scenarioFunctionBuilder.removeDuplicateLines().toString()
    }

    private fun transformDataTableRowToFunctionParameters(inputString: String): String {
        return inputString.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
            .joinToString(", ", transform = { "\"$it\"" })
    }

    private fun formatStepFunction(step: String, dataTable: List<String>): String {
        val stepFunctionBuilder = StringBuilder()
        val duplicateStepRegex = "cucumber-converter-\\d+$".toRegex()

        val stepInitialFormatting =
            step.lowercase().replace(duplicateStepRegex, "").trimEnd().dropWhile { it != ' ' }.trimStart()
                .filterNot { it == '\'' }.filterNot { it == '-' }
                .replace(' ', '_').removeSuffix("_")

        val paramsRegex = "\"([^\"]+)\"".toRegex()

        // check for datatable in a step
        val dataTableVariableNamesRow = mutableListOf<String>()
        val dataTableParameters = mutableListOf<String>()
        if (dataTable.isNotEmpty()) {
            dataTable.forEachIndexed { index, row ->
                val transformedRow = transformDataTableRowToFunctionParameters(row)
                if (index == 0) {
                    dataTableVariableNamesRow.add(transformedRow)
                } else {
                    dataTableParameters.add(transformedRow)
                }
            }
        }

        val dataTableSection =
            if (dataTableParameters.isNotEmpty()) "datatable: [$dataTableVariableNamesRow, $dataTableParameters]" else ""

        // step parameters
        val extractedValues = paramsRegex.findAll(step).map { it.groupValues[1] }.toList()
        val formattedStepParameters = extractedValues.joinToString(",") { "\"$it\"" }
        val parametersSection =
            if (formattedStepParameters.isNotEmpty()) "parameters: [$formattedStepParameters]" else ""

        // step function creation
        val stepName =
            stepInitialFormatting.replace(paramsRegex, "").replace("_+".toRegex(), "_").dropLastWhile { it == '_' }
        val stepFunctionSignature = when {
            parametersSection.isNotEmpty() && dataTableSection.isNotEmpty() -> "try $stepName($parametersSection, $dataTableSection)"
            parametersSection.isNotEmpty() -> "try $stepName($parametersSection)"
            dataTableSection.isNotEmpty() -> "try $stepName($dataTableSection)"
            else -> "try $stepName()"
        }

        stepFunctionBuilder.appendLine("        $stepFunctionSignature")


        return stepFunctionBuilder.toString()
    }

    private fun StringBuilder.removeDuplicateLines(): StringBuilder {
        val lines = this.toString().lines()

        val uniqueLines = lines.distinct().filter { it.isNotEmpty() }

        return StringBuilder(uniqueLines.joinToString("${System.lineSeparator()}${System.lineSeparator()}"))
    }

}