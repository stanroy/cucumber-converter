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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.Dictionary
import data.FileHandler
import data.JFileChooserWrapper
import theme.CucumberConverterColors
import theme.CucumberConverterTheme
import theme.CucumberConverterTypography
import theme.Shapes
import javax.swing.JFileChooser


@Composable
@Preview
fun App() {
    var pathInputState by remember { mutableStateOf("") }
    var clearPathState by remember { mutableStateOf(false) }

    val fileChooserWrapper = JFileChooserWrapper()
    // only accept directories
    fileChooserWrapper.fileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    val fileHandler = FileHandler(fileChooserWrapper)


    LaunchedEffect(pathInputState) {
        clearPathState = !(pathInputState.isEmpty() || pathInputState == Dictionary.chooseValidDirectory)
    }

    CucumberConverterTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                GeneratorInterface(
                    currentPath = pathInputState.ifEmpty { Dictionary.chooseScenariosLabel },
                    crossFadeTargetState = clearPathState,
                    buttonEnabled = pathInputState.isNotEmpty() && pathInputState != Dictionary.chooseValidDirectory,
                    onPathInputClick = {
                        pathInputState = if (clearPathState) {
                            ""
                        } else {
                            // open file chooser dialog && catch returned directory value
                            // or return error text
                            fileHandler.openFileDialogAndValidateScenarioPath() ?: Dictionary.chooseValidDirectory
                        }
                    },
                    onGenerateClick = {
                        val scenariosFound = fileHandler.readScenarios(pathInputState)
                        if (scenariosFound.isNotEmpty()) {
                            scenariosFound.forEach { (subFolder, fileList) ->
                                println(subFolder)
                                fileList.forEach { file ->
                                    fileHandler.readFile(file)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GeneratorInterface(
    currentPath: String,
    crossFadeTargetState: Boolean,
    buttonEnabled: Boolean,
    onPathInputClick: () -> Unit,
    onGenerateClick: () -> Unit
) {
    Column(
        modifier = Modifier.clip(shape = Shapes.large).border(
            width = 4.dp, color = CucumberConverterColors.Green500, shape = Shapes.large
        ).heightIn(min = 72.dp).fillMaxWidth(0.7f).clickable(
            interactionSource = MutableInteractionSource(),
            indication = rememberRipple(color = Color.Gray),
            onClick = onPathInputClick
        ), verticalArrangement = Arrangement.Center
    ) {
        PathInput(
            currentPath = currentPath,
            crossFadeTargetState = crossFadeTargetState
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryButton(
        enabled = buttonEnabled,
        onClick = onGenerateClick
    )
}

@Composable
fun PathInput(currentPath: String, crossFadeTargetState: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.padding(32.dp),
            text = currentPath,
            style = CucumberConverterTypography.RobotoPlaceholderUiL.copy(color = CucumberConverterColors.Gray)
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
        icon = BitmapPainter(useResource("plane.png", ::loadImageBitmap)),
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
