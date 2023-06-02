import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.Dictionary
import data.FileChooserWrapper
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
    var clearPath by remember { mutableStateOf(false) }
    val fileHandler = FileHandler(JFileChooserWrapper())

    LaunchedEffect(pathInputState) {
        clearPath = !(pathInputState.isEmpty() || pathInputState == Dictionary.chooseValidDirectory)
    }

    CucumberConverterTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Column(
                    modifier = Modifier.clip(shape = Shapes.large).border(
                        width = 4.dp, color = CucumberConverterColors.Green500, shape = Shapes.large
                    ).heightIn(min = 72.dp).fillMaxWidth(0.7f).clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = rememberRipple(color = Color.Gray),
                        onClick = {
                            pathInputState = if (clearPath) {
                                ""
                            } else {
                                // open file chooser dialog && catch returned directory value
                                // or return error text
                                fileHandler.getScenariosDirectoryPath() ?: Dictionary.chooseValidDirectory
                            }
                        }), verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.padding(32.dp),
                            text = pathInputState.ifEmpty { Dictionary.chooseScenariosLabel },
                            style = CucumberConverterTypography.RobotoPlaceholderUiL.copy(color = CucumberConverterColors.Gray)
                        )

                        Crossfade(
                            clearPath,
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
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(0.34f),
                    enabled = pathInputState.isNotEmpty() && pathInputState != Dictionary.chooseValidDirectory,
                    onClick = {
                        val scenariosFound = fileHandler.readScenarios(pathInputState)

                        if (scenariosFound.isNotEmpty()) {
                            scenariosFound.forEach { (subFolder, file) ->
                                println(subFolder)
                                fileHandler.readFile(file)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(disabledBackgroundColor = CucumberConverterColors.Gray)
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = Dictionary.generateScenarios,
                        style = CucumberConverterTypography.RobotoPrimaryButtonUiC
                    )
                }
            }
        }
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
