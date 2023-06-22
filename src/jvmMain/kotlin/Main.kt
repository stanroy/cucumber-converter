import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.Dictionary
import data.FileHandler
import data.Generator
import data.JFileChooserWrapper
import theme.CucumberConverterColors
import theme.CucumberConverterTheme
import theme.CucumberConverterTypography
import theme.Shapes
import javax.swing.JFileChooser


class Main() {

    private val fileChooserWrapper = JFileChooserWrapper()
    private val fileHandler = FileHandler(fileChooserWrapper)

    init {
        // only accept directories
        fileChooserWrapper.fileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    }

    @Composable
    @Preview
    fun AppInterface() {
        var scenarioPathInputState by remember { mutableStateOf("") }
        var generatedFilesPathInputState by remember { mutableStateOf("") }
        var clearScenarioPathState by remember { mutableStateOf(false) }
        var clearGeneratedPathState by remember { mutableStateOf(false) }

        val errorConditions = setOf(
            Dictionary.chooseValidDirectory,
            Dictionary.noScenariosFound
        )

        val scenarioPathContainsError = when (scenarioPathInputState) {
            in errorConditions -> true
            else -> scenarioPathInputState.contains(Dictionary.scenarioReadingError.dropLast(2), ignoreCase = true)
        }

        val generatedPathContainsError = generatedFilesPathInputState in errorConditions

        LaunchedEffect(key1 = scenarioPathInputState, key2 = generatedFilesPathInputState) {
            clearScenarioPathState =
                !(scenarioPathInputState.isEmpty() || scenarioPathContainsError)
            clearGeneratedPathState =
                !(generatedFilesPathInputState.isEmpty() || generatedPathContainsError)
        }

        val generateButtonEnabled =
            scenarioPathInputState.isNotEmpty() && generatedFilesPathInputState.isNotEmpty() && !scenarioPathContainsError && !generatedPathContainsError

        fun generatorError(errorMessage: String) {
            scenarioPathInputState = errorMessage
        }

        CucumberConverterTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    GeneratorInterface(
                        currentScenarioPath = scenarioPathInputState.ifEmpty { Dictionary.chooseScenariosLabel },
                        currentGeneratedPath = generatedFilesPathInputState.ifEmpty { Dictionary.chooseFolderToGenerateIn },
                        scenarioPathCrossFadeState = clearScenarioPathState,
                        generatedPathCrossFadeState = clearGeneratedPathState,
                        buttonEnabled = generateButtonEnabled,
                        onPathInputClick = {
                            scenarioPathInputState = if (clearScenarioPathState) {
                                ""
                            } else {
                                // open file chooser dialog && catch returned directory value
                                // or return error text
                                fileHandler.openFileDialogAndValidateScenarioPath() ?: Dictionary.chooseValidDirectory
                            }
                        },
                        onGeneratedPathInputClick = {
                            generatedFilesPathInputState = if (clearGeneratedPathState) {
                                ""
                            } else {
                                // open file chooser dialog && catch returned directory value
                                // or return error text
                                fileHandler.openFileDialog() ?: Dictionary.chooseValidDirectory
                            }
                        }
                    ) {
                        val generator = Generator()

                        fileHandler.processScenarios(
                            scenariosPath = scenarioPathInputState,
                            onScenariosFound = { generator.clearOldTestFiles(generatedFilesPathInputState) },
                            onEachScenarioProcessed = { scenarioFile ->
                                generator.createSwiftFile(scenarioFile, generatedFilesPathInputState)

                            },
                            onEmptyScenarios = {
                                generatorError(Dictionary.noScenariosFound)
                            },
                            onScenarioReadingFailure = { scenarioName ->
                                val errorMessage = String.format(Dictionary.scenarioReadingError, scenarioName)
                                generatorError(errorMessage)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorInterface(
    currentScenarioPath: String,
    currentGeneratedPath: String,
    scenarioPathCrossFadeState: Boolean,
    generatedPathCrossFadeState: Boolean,
    buttonEnabled: Boolean,
    onPathInputClick: () -> Unit,
    onGeneratedPathInputClick: () -> Unit,
    onGenerateClick: () -> Unit
) {
    Column(
        modifier = Modifier.clip(shape = Shapes.large).border(
            width = 4.dp, color = CucumberConverterColors.Green500, shape = Shapes.large
        ).heightIn(min = 72.dp).fillMaxWidth(0.85f), verticalArrangement = Arrangement.Center
    ) {
        // Scenario path
        PathInput(
            currentPath = currentScenarioPath,
            crossFadeTargetState = scenarioPathCrossFadeState,
            onClick = onPathInputClick
        )

        // Generated files path
        PathInput(
            currentPath = currentGeneratedPath,
            crossFadeTargetState = generatedPathCrossFadeState,
            onClick = onGeneratedPathInputClick
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryButton(
        enabled = buttonEnabled,
        onClick = onGenerateClick
    )
}

@Composable
fun PathInput(currentPath: String, crossFadeTargetState: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = MutableInteractionSource(),
            indication = rememberRipple(color = Color.Gray),
            onClick = onClick
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.padding(32.dp).weight(1f),
            text = currentPath,
            style = CucumberConverterTypography.RobotoPlaceholderUiL.copy(color = CucumberConverterColors.Gray),
        )

        Crossfade(
            targetState = crossFadeTargetState,
            animationSpec = tween(250)
        ) { targetState ->
            Image(
                modifier = Modifier.padding(end = 32.dp).size(64.dp),
                painter = painterResource(if (targetState) "cancel.svg" else "folder.svg"),
                contentDescription = Dictionary.chooseDirectoryCd,
                colorFilter = ColorFilter.tint(color = CucumberConverterColors.Green500)
            )
        }
    }
}

@Composable
fun PrimaryButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(0.34f),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(disabledBackgroundColor = CucumberConverterColors.Gray)
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = Dictionary.generateScenarios,
            style = CucumberConverterTypography.RobotoPrimaryButtonUiC
        )
    }
}


fun main() = application {
    Window(
        title = Dictionary.windowTitle,
        onCloseRequest = ::exitApplication
    ) {
        val main = Main()
        main.AppInterface()
    }
}
