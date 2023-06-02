import data.FileChooserWrapper
import data.FileHandler
import data.JFileChooserWrapper
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import javax.swing.JFileChooser
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class FileHandlerTest {

    private lateinit var fileHandler: FileHandler

    @Test
    fun `when directory selected expect scenarios directory path`() {
        val testScenarioResource = javaClass.classLoader.getResource("test_scenario.feature")
        val testScenarioFile = File(testScenarioResource?.toURI()!!)
        createFileHandler(selectedFile = testScenarioFile)

        val scenariosPath = fileHandler.getScenariosDirectoryPath()

        assertEquals(scenariosPath, testScenarioFile.absolutePath)
    }


    @Test
    fun `when directory chooser canceled expect null value`() {
        createFileHandler(openDialogResult = JFileChooser.CANCEL_OPTION)
        val scenariosPath = fileHandler.getScenariosDirectoryPath()

        assertEquals(scenariosPath, null)
    }

    private fun createFileHandler(openDialogResult: Int = JFileChooser.APPROVE_OPTION, selectedFile: File? = null) {
        val fileChooserWrapperMock = mock(JFileChooserWrapper::class.java)
        `when`(fileChooserWrapperMock.showOpenDialog()).thenReturn(openDialogResult)
        `when`(fileChooserWrapperMock.getSelectedFile()).thenReturn(selectedFile)
        fileHandler = FileHandler(fileChooserWrapperMock)
    }

}