import data.FileHandler
import data.JFileChooserWrapper
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Test
import javax.swing.JFileChooser
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class FileHandlerTest {

    private lateinit var fileHandler: FileHandler
    private lateinit var testScenarioFile: File
    private lateinit var testScenarioMainFolder: File

    @Before
    fun setup() {
        setupDefaultFileHierarchy()
    }


    // ------------------------------
    // Tests for openFileDialogAndValidateScenarioPath()
    // ------------------------------

    @Test
    fun `when valid directory selected, expect correct scenarios directory path`() {
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        assertTrue(testScenarioMainFolder.isDirectory)
        assertEquals(testScenarioMainFolder.absolutePath, scenariosPath)
    }

    @Test
    fun `when directory chooser canceled expect null value returned`() {
        createFileHandler(openDialogResult = JFileChooser.CANCEL_OPTION)
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()

        assertNull(scenariosPath)
    }

    @Test
    fun `when wrong directory selected expect null value returned`() {
        val wrongDirectory = javaClass.classLoader.getResource("emptyFolder")
        val testScenarioFile = File(wrongDirectory?.toURI()!!)

        createFileHandler(selectedFile = testScenarioFile)

        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()

        assertNull(scenariosPath)
    }

    // ------------------------------
    // Tests for readScenarios()
    // ------------------------------

    @Test
    fun `when valid scenario main directory chosen, expect scenarios files map with parent folder as key`() {
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        scenariosPath?.let {
            val scenariosFound = fileHandler.readScenarios(it)

            assertTrue(scenariosFound.isNotEmpty())
            assertTrue(scenariosFound.containsKey("feature_test_1"))
            val files = scenariosFound.getValue("feature_test_1")
            assertEquals(1, files.size)
            assertEquals("test_scenario.feature", files[0].name)
        }
    }

    @Test
    fun `when scenario subfolder chosen, expect empty scenarios file map`() {
        setupDefaultFileHierarchy(useSubFolder = true)
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        scenariosPath?.let {
            val scenariosFound = fileHandler.readScenarios(it)

            assertEquals(emptyMap<String, File>(), scenariosFound)
        }
    }

    @Test
    fun `when valid scenarios folder is provided and contains feature files, expect all scenario files to be read successfully`() {
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        scenariosPath?.let {
            val scenariosFound = fileHandler.readScenarios(it).toMap().toSortedMap()

            scenariosFound.entries.forEachIndexed { index, entry ->
                val subFolder = entry.key
                val fileList = entry.value
                assertEquals("feature_test_${index + 1}", subFolder)
                assertEquals("test_scenario.feature", fileList.first().name)
            }
        }
    }

    @Test
    fun `when empty scenarios folder path provided, expect empty scenarios file map`() {
        val scenariosFound = fileHandler.readScenarios("")
        assertEquals(emptyMap<String, List<File>>(), scenariosFound)
    }


    // ------------------------------
    // Tests for readFile()
    // ------------------------------

    @Test
    fun `when reading a file, expect file contents to be returned successfully`() {
        val expectedContents = """
        Feature: Is it Friday yet?

          Scenario: Sunday isn't Friday
            Given today is Sunday
            When I ask whether it's Friday yet
            Then I should be told "Nope"

        """.trimIndent()

        val fileContents = fileHandler.readFile(testScenarioFile)

        assertEquals(expectedContents, fileContents)

    }

    @Test
    fun `when reading a file fails, expect a null value to be returned`() {
        val fileContents = fileHandler.readFile(File(""))

        assertNull(fileContents)
    }

    // ------------------------------
    // Helper methods
    // ------------------------------
    private fun setupDefaultFileHierarchy(useSubFolder: Boolean = false) {
        val testScenarioResource = javaClass.classLoader.getResource("feature_test_1/test_scenario.feature")
        testScenarioFile = File(testScenarioResource?.toURI()!!)
        testScenarioMainFolder =
            if (useSubFolder) testScenarioFile.parentFile else testScenarioFile.parentFile.parentFile
        createFileHandler(selectedFile = testScenarioMainFolder)
    }

    private fun createFileHandler(openDialogResult: Int = JFileChooser.APPROVE_OPTION, selectedFile: File? = null) {
        val fileChooserWrapperMock = mock(JFileChooserWrapper::class.java)
        fileChooserWrapperMock.fileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        `when`(fileChooserWrapperMock.showOpenDialog()).thenReturn(openDialogResult)
        `when`(fileChooserWrapperMock.getSelectedFile()).thenReturn(selectedFile)
        fileHandler = FileHandler(fileChooserWrapperMock)
    }

}

