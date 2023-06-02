package data

fun createSwiftFunctions(scenarios: List<List<String>>): List<String> {
    val functions = mutableListOf<String>()
    scenarios.forEach {
        // scenario main function
        val scenarioList = it.toMutableList()
        val scenarioComment = "//${scenarioList.first().filterNot { char -> char == ':' }}"
        val scenarioMainFunction = "func test_${
            scenarioComment.removePrefix("//Scenario ").lowercase().replace(" ", "_").plus("() throws {\n")
        }"
        scenarioList.removeFirst()

        // scenario step functions
        val stepFunctions = filterStepFunctions(scenarioList)


        // scenario full function
        val swiftFunction = "".let { func ->
            func + "$scenarioComment\n" + "$scenarioMainFunction\n" + "${stepFunctions.trimEnd()}\n\n" +
                    // close func
                    "}\n\n"
        }

        functions.add(swiftFunction)
    }
    return functions
}

fun filterStepFunctions(steps: List<String>): String {
    var stepFunctions = ""
    steps.forEach { step ->
        val hasParams = step.contains("\"")
        val testFunc = createTestFunction(step, hasParams)
        val comment = testFunc.first
        val func = testFunc.second

        stepFunctions += comment
        stepFunctions += "$func\n\n"

    }
    return stepFunctions
}

fun createTestFunction(step: String, hasParams: Boolean): Pair<String, String> {
    val func: String
    // basic function comment
    var comment = "\t//$step\n"
    // regex for step prefix
    val reg = """(\b(When|Then|Given|And|But)\b)""".toRegex()

    // regex to find params in steps
    val regParams = "\"([^\"]*)\"".toRegex()
    val matches = regParams.findAll(step)
    val results = matches.map { match ->
        match.value.replace("\"", "")
    }.toMutableList()

    if (!hasParams) {
        func = "\ttry test${
            step.replace(reg, "").lowercase().replace("'", "").replace(" ", "_").plus("()")
        }"
    } else {
        var funcParams = ""
        val newStep = step.replace(regParams, "").replace("  ", " ").trimEnd()
        results.forEachIndexed { index, param ->
            // put params in function comment
            comment += "\t//param$index: \"$param\"\n"
            funcParams += "param$index: TYPE, "
        }

        val funcParamsFiltered = if (results.count() == 1) funcParams.replace(",", "").trimEnd()
        else funcParams.trimEnd().removeSuffix(",")

        func = "\ttry test${
            newStep.replace(reg, "").lowercase().replace("'", "").replace(" ", "_").replace("\"", "")
                .plus("($funcParamsFiltered)")
        }"
    }

    return Pair(comment, func)
}

// split scenarios line by line
fun splitScenarios(text: String): MutableList<List<String>> {
    val regex = """((?=Scenario)|(?=Given)|(?=When)|(?=Then))|(?=And)|(?=But)|(?=#)|(?=Feature)""".toRegex()
    val filteredList = text.split(regex).filterNot { step ->
        step.isEmpty()
    }.filterNot { step -> step.contains("#") }.filterNot { step -> step.contains("Feature") }.toMutableList()
    val bufferList = mutableListOf<String>()
    val scenarioParts = mutableListOf<List<String>>()

    while (filteredList.iterator().hasNext()) {
        // scenario
        if (filteredList.first().contains("Scenario") && bufferList.isEmpty()) {
            bufferList.add(filteredList.first().trimEnd())
            filteredList.removeFirst()
        }

        // scenario steps
        if (!filteredList.iterator().next().contains("Scenario")) {
            bufferList.add(filteredList.iterator().next().trimEnd())
            filteredList.remove(filteredList.iterator().next())
        } else {
            scenarioParts.add(bufferList.toMutableList())
            bufferList.clear()
        }
    }

    if (bufferList.isNotEmpty()) {
        scenarioParts.add(bufferList.toMutableList())
        bufferList.clear()
    }

    return scenarioParts
}