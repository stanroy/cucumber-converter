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
    private lateinit var testScenarioFolder: File

    @Before
    fun setup() {
        val testScenarioResource = javaClass.classLoader.getResource("feature_test/test_scenario.feature")
        val testScenarioFile = File(testScenarioResource?.toURI()!!)
        testScenarioFolder = testScenarioFile.parentFile.parentFile
        createFileHandler(selectedFile = testScenarioFolder)
    }

    @Test
    fun `when valid directory selected, expect correct scenarios directory path`() {
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        assertTrue(testScenarioFolder.isDirectory)
        assertEquals(testScenarioFolder.absolutePath, scenariosPath)
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

    @Test
    fun `when valid scenario path chosen, expect scenarios files map with parent folder as key`() {
        val scenariosPath = fileHandler.openFileDialogAndValidateScenarioPath()
        scenariosPath?.let {
            val scenariosFound = fileHandler.readScenarios(it)

            assertTrue(scenariosFound.isNotEmpty())
            assertTrue(scenariosFound.containsKey("feature_test"))
            assertEquals("test_scenario.feature", scenariosFound.getValue("feature_test").name)
        }
    }

    private fun createFileHandler(openDialogResult: Int = JFileChooser.APPROVE_OPTION, selectedFile: File? = null) {
        val fileChooserWrapperMock = mock(JFileChooserWrapper::class.java)
        fileChooserWrapperMock.fileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        `when`(fileChooserWrapperMock.showOpenDialog()).thenReturn(openDialogResult)
        `when`(fileChooserWrapperMock.getSelectedFile()).thenReturn(selectedFile)
        fileHandler = FileHandler(fileChooserWrapperMock)
    }

}