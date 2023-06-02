package data

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import javax.swing.JFileChooser

abstract class FileChooserWrapper {
    protected val fileChooser: JFileChooser = JFileChooser()
    abstract fun showOpenDialog(): Int
    abstract fun getSelectedFile(): File
    abstract fun fileSelectionMode()
}

class JFileChooserWrapper : FileChooserWrapper() {
    override fun showOpenDialog(): Int {
        return fileChooser.showOpenDialog(null)
    }

    override fun getSelectedFile(): File {
        return fileChooser.selectedFile
    }

    override fun fileSelectionMode() {
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }

}

class FileHandler(private val fileChooserWrapper: FileChooserWrapper) {

    fun getScenariosDirectoryPath(): String? {
        val fileChooser = fileChooserWrapper
        // open file chooser dialog
        val returnValue = fileChooserWrapper.showOpenDialog()

        // check selected option
        return if (returnValue == JFileChooser.APPROVE_OPTION) {
            val selectedFolder = fileChooser.getSelectedFile()
            selectedFolder.absolutePath
        } else null
    }

    fun readScenarios(scenariosPath: String): MutableMap<String, File> {
        val subFolders = mutableListOf<File>()
        val scenariosMainDirectory = File(scenariosPath)
        // get subfolders from main scenarios folder and add it to MutableList
        if (scenariosMainDirectory.exists() && scenariosMainDirectory.isDirectory) {
            subFolders.addAll(scenariosMainDirectory.listFiles { file -> file.isDirectory }.orEmpty())
        }
        val scenariosMap = mutableMapOf<String, File>()
        for (folder in subFolders) {
            val files = folder.listFiles { file ->
                file.isFile
            }.orEmpty()

            if (files.isNotEmpty()) {
                scenariosMap[folder.name] = files[0]
            }
        }

        return scenariosMap


    }

    fun readFile(file: File) {
        try {
            val fileReader = FileReader(file.absoluteFile)
            val bufferedReader = BufferedReader(fileReader)
            var line: String?
            val stringBuilder = StringBuilder()

            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }
            bufferedReader.close()
            println(stringBuilder.toString())
        } catch (e: Exception) {
            println(e.printStackTrace())
        }
    }
}