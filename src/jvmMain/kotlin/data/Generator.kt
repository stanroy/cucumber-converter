package data

class Generator {

    fun splitScenarios(scenarioFileContents: String): Map<String, Map<String, List<String>>> {
        // Split the scenario file contents into lines and perform initial filtering
        val scenarioFileLines = scenarioFileContents.lines()
            .filter { it.isNotBlank() }
            .filterNot { it.lowercase().contains("feature") }
            .map { it.trimStart() }

        // Create a map to store the scenarios and their steps with data tables
        val scenarioMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        var currentStep: String? = null

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

                else -> {
                    // Otherwise, it's a regular step line
                    currentStep = line
                    // Create a new list to store the data tables of the current step
                    scenarioMap.values.lastOrNull()?.set(currentStep!!, mutableListOf())
                }
            }
        }

        return scenarioMap
    }

}