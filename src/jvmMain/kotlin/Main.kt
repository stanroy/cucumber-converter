import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import common.UserPrefs
import data.*
import kotlinx.coroutines.delay
import theme.CucumberConverterColors
import theme.CucumberConverterTheme
import theme.CucumberConverterTypography
import theme.Shapes
import java.util.prefs.Preferences
import javax.swing.JFileChooser


class Main() {

    private val fileChooserWrapper = JFileChooserWrapper()
    private val fileHandler = FileHandler(fileChooserWrapper)
    private val userPrefs = Preferences.userNodeForPackage(Main::class.java)

    init {
        // only accept directories
        fileChooserWrapper.fileSelectionMode(JFileChooser.DIRECTORIES_ONLY)


    }

    private fun getRememberedPaths(): Pair<String, String>? {
        val scenarioPath = userPrefs.get(UserPrefs.scenariosPath, "")
        val generatedFilesPath = userPrefs.get(UserPrefs.generatedFilesPath, "")

        return if (scenarioPath.isEmpty() || generatedFilesPath.isEmpty()) null else Pair(
            scenarioPath,
            generatedFilesPath
        )
    }


    @Composable
    @Preview
    fun AppInterface() {
        // remembering paths
        val rememberedPaths = getRememberedPaths()
        val rememberedScenarioPath = rememberedPaths?.first ?: ""
        val rememberedGeneratedFilesPath = rememberedPaths?.second ?: ""

        var scenarioPathInputState by remember { mutableStateOf(rememberedScenarioPath) }
        var generatedFilesPathInputState by remember { mutableStateOf(rememberedGeneratedFilesPath) }
        var clearScenarioPathState by remember { mutableStateOf(false) }
        var clearGeneratedPathState by remember { mutableStateOf(false) }
        var canRememberPaths by remember { mutableStateOf(false) }
        var rememberPaths by remember { mutableStateOf(rememberedPaths != null) }
        val generator = Generator()
        var generatorSuccess by remember { mutableStateOf(false) }


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

        LaunchedEffect(generatorSuccess) {
            if (generatorSuccess) {
                delay(1500)
                generatorSuccess = false
            }
        }

        val generateButtonEnabled =
            scenarioPathInputState.isNotEmpty() && generatedFilesPathInputState.isNotEmpty() && !scenarioPathContainsError && !generatedPathContainsError

        canRememberPaths = generateButtonEnabled

        fun scenarioError(errorMessage: String) {
            scenarioPathInputState = errorMessage
        }

        fun openFolderChooserDialog(clearPath: Boolean, invalidDirectory: String, openDialog: () -> String?): String {
            return if (clearPath) "" else openDialog() ?: Dictionary.chooseValidDirectory.formatWithParam(
                invalidDirectory
            )
        }

        CucumberConverterTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    GeneratorInterface(
                        currentScenarioPath = scenarioPathInputState.ifEmpty { Dictionary.chooseScenariosLabel },
                        currentGeneratedPath = generatedFilesPathInputState.ifEmpty { Dictionary.chooseFolderToGenerateIn },
                        scenarioPathCrossFadeState = clearScenarioPathState,
                        generatedPathCrossFadeState = clearGeneratedPathState,
                        generateButtonEnabled = generateButtonEnabled,
                        generatorSuccess = generatorSuccess,
                        canRememberPaths = canRememberPaths,
                        rememberPathsCheckboxEnabled = rememberPaths,
                        onRememberPathsCheckboxStateChanged = {
                            rememberPaths = it

                            if (rememberPaths) {
                                userPrefs.put(UserPrefs.scenariosPath, scenarioPathInputState)
                                userPrefs.put(UserPrefs.generatedFilesPath, generatedFilesPathInputState)
                            } else {
                                userPrefs.remove(UserPrefs.scenariosPath)
                                userPrefs.remove(UserPrefs.generatedFilesPath)
                            }
                        },
                        onPathInputClick = {
                            scenarioPathInputState = openFolderChooserDialog(clearScenarioPathState, "scenario") {
                                // open file chooser dialog && catch returned directory value
                                // or return error text
                                fileHandler.openFileDialogAndValidateScenarioPath()
                            }
                        },
                        onGeneratedPathInputClick = {
                            generatedFilesPathInputState =
                                openFolderChooserDialog(clearGeneratedPathState, "generated files") {
                                    fileHandler.openFileDialog()
                                }
                        }
                    ) {
                        generatorSuccess = false
                        fileHandler.processScenarios(
                            scenariosPath = scenarioPathInputState,
                            onScenariosFound = { generator.clearOldTestFiles(generatedFilesPathInputState) },
                            onEachScenarioProcessed = { scenarioFile ->
                                generator.createSwiftFile(scenarioFile, generatedFilesPathInputState)

                            },
                            onEmptyScenarios = {
                                scenarioError(Dictionary.noScenariosFound)
                            },
                            onScenarioReadingFailure = { scenarioName ->
                                val errorMessage = Dictionary.scenarioReadingError.formatWithParam(scenarioName)
                                scenarioError(errorMessage)
                            },
                            onLastScenarioProcessed = {
                                generatorSuccess = true
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
    generateButtonEnabled: Boolean,
    generatorSuccess: Boolean,
    canRememberPaths: Boolean,
    rememberPathsCheckboxEnabled: Boolean,
    onRememberPathsCheckboxStateChanged: (Boolean) -> Unit,
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
        Spacer(modifier = Modifier.height(4.dp).fillMaxWidth().background(color = CucumberConverterColors.Green500))
        // Generated files path
        PathInput(
            currentPath = currentGeneratedPath,
            crossFadeTargetState = generatedPathCrossFadeState,
            onClick = onGeneratedPathInputClick
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    AnimatedVisibility(canRememberPaths) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.height(24.dp),
                checked = rememberPathsCheckboxEnabled,
                onCheckedChange = onRememberPathsCheckboxStateChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = CucumberConverterColors.Green500,
                    uncheckedColor = CucumberConverterColors.Green500,
                    checkmarkColor = CucumberConverterColors.White
                )
            )
            Text(
                "Remember generator paths",
                style = CucumberConverterTypography.RobotoPlaceholderUiL.copy(color = CucumberConverterColors.Gray)
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        PrimaryButton(
            enabled = generateButtonEnabled,
            onClick = onGenerateClick
        )
        AnimatedVisibility(generatorSuccess) {
            Image(
                modifier = Modifier.padding(start = 32.dp).size(64.dp),
                painter = painterResource("done.svg"),
                contentDescription = Dictionary.loadingCd,
                colorFilter = ColorFilter.tint(color = CucumberConverterColors.Green500)
            )
        }
    }
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
