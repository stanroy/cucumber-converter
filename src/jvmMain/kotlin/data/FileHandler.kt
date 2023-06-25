package data

import java.io.File
import javax.swing.JFileChooser

abstract class FileChooserWrapper {
    protected val fileChooser: JFileChooser = JFileChooser()
    abstract fun showOpenDialog(): Int
    abstract fun getSelectedFile(): File
    abstract fun fileSelectionMode(fileSelectionMode: Int)
}

class JFileChooserWrapper : FileChooserWrapper() {
    override fun showOpenDialog(): Int {
        return fileChooser.showOpenDialog(null)
    }

    override fun getSelectedFile(): File {
        return fileChooser.selectedFile
    }

    override fun fileSelectionMode(fileSelectionMode: Int) {
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }

}

class FileHandler(private val fileChooserWrapper: FileChooserWrapper) {

    fun openFileDialogAndValidateScenarioPath(): String? {
        val fileChooser = fileChooserWrapper
        // open file chooser dialog
        val returnValue = fileChooserWrapper.showOpenDialog()

        // check selected option
        return if (returnValue == JFileChooser.APPROVE_OPTION) {
            checkSelectedFolderPath(fileChooser.getSelectedFile())
        } else null
    }

    fun openFileDialog(): String? {
        val fileChooser = fileChooserWrapper
        // open file chooser dialog
        val returnValue = fileChooserWrapper.showOpenDialog()

        return if (returnValue == JFileChooser.APPROVE_OPTION) {
            fileChooser.getSelectedFile().absolutePath
        } else null
    }

    private fun checkSelectedFolderPath(selectedFolder: File): String? {
        return selectedFolder.takeIf { it.isDirectory && containsFeatureFile(it) }?.absolutePath
    }

    private fun containsFeatureFile(directory: File): Boolean {
        val files = directory.listFiles() ?: return false

        if (files.any { it.name.endsWith(".feature") }) {
            return true
        }

        for (file in files) {
            if (file.isDirectory && containsFeatureFile(file)) {
                return true
            }
        }

        return false
    }

    fun readScenarios(scenariosPath: String): MutableMap<String, List<File>> {
        val scenariosMainDirectory = File(scenariosPath)

        val scenariosMap = mutableMapOf<String, List<File>>()
        if (scenariosMainDirectory.exists() && scenariosMainDirectory.isDirectory) {
            scenariosMainDirectory.listFiles { file -> file.isDirectory }?.forEach { folder ->
                val files = folder.listFiles { file ->
                    file.isFile && file.name.endsWith(".feature")
                }.orEmpty()

                if (files.isNotEmpty()) {
                    scenariosMap[folder.name] = files.toList()
                }
            }
        }

        return scenariosMap
    }

    fun readFile(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //    fun processScenarios(
//        scenariosPath: String,
//        onScenariosFound: () -> Unit,
//        onEachScenarioProcessed: (ScenarioFile) -> Unit,
//        onEmptyScenarios: () -> Unit,
//        onScenarioReadingFailure: (scenarioName: String) -> Unit
//    ) {
//        val scenariosFound = readScenarios(scenariosPath)
//        if (scenariosFound.isNotEmpty()) {
//            onScenariosFound()
//            scenariosFound.forEach { (subFolder, fileList) ->
//                fileList.forEach { file ->
//                    val fileContents = readFile(file)
//                    if (fileContents != null) {
//                        onEachScenarioProcessed(
//                            ScenarioFile(
//                                subFolder, file.name.substringBeforeLast('.'), fileContents
//                            )
//                        )
//                    } else {
//                        onScenarioReadingFailure(file.name)
//                    }
//                }
//            }
//        } else {
//            onEmptyScenarios()
//        }
//    }
    fun processScenarios(
        scenariosPath: String,
        onScenariosFound: () -> Unit,
        onEachScenarioProcessed: (ScenarioFile) -> Unit,
        onEmptyScenarios: () -> Unit,
        onScenarioReadingFailure: (scenarioName: String) -> Unit,
        onLastScenarioProcessed: () -> Unit // New lambda parameter
    ) {
        // Read the scenarios from the specified path
        val scenariosFound = readScenarios(scenariosPath)

        if (scenariosFound.isNotEmpty()) {
            // Invoke when scenarios are found
            onScenariosFound()

            // Calculate the total number of scenarios
            val totalScenarioCount = scenariosFound.flatMap { it.value }.size

            // Counter for processed scenarios
            var processedScenarioCount = 0

            // Counter for processed files
            var processedFileCount = 0

            // Iterate over each subfolder and its associated file list
            scenariosFound.forEach { (subFolder, fileList) ->
                // Iterate over each file in the file list
                fileList.forEach { file ->
                    // Read the contents of the current file
                    val fileContents = readFile(file)

                    if (fileContents != null) {
                        // Invoke for each processed scenario
                        onEachScenarioProcessed(
                            ScenarioFile(
                                subFolder, file.name.substringBeforeLast('.'), fileContents
                            )
                        )

                        // Increment the counter for processed scenarios
                        processedScenarioCount++

                        // Check if all scenarios from the last file are processed
                        if (processedScenarioCount == totalScenarioCount && processedFileCount == scenariosFound.size - 1) {
                            // Invoke when the last scenario from the last file is processed
                            onLastScenarioProcessed()
                        }
                    } else {
                        // Invoke when there's a failure in reading the scenario file
                        onScenarioReadingFailure(file.name)
                    }
                }

                // Increment the counter for processed files
                processedFileCount++
            }
        } else {
            // Invoke when no scenarios are found
            onEmptyScenarios()
        }
    }
}

data class ScenarioFile(val subFolder: String, val fileName: String, val fileContents: String)