package navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun navBar(modifier: Modifier, screenState: State<Screen>, onScreenStateChange: (Screen) -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        val isGeneratorActive =
            if (screenState.value == Screen.GenerateScreen) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primaryVariant
            )

        val isResultsActive = if (screenState.value is Screen.ResultsScreen) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primaryVariant
            )
        }

        Button(modifier = Modifier.weight(1f).fillMaxHeight(0.1f),
            colors = isGeneratorActive,
            shape = RectangleShape,
            onClick = { onScreenStateChange(Screen.GenerateScreen) }) {
            Text("Generator")
        }
        Button(modifier = Modifier.weight(1f).fillMaxHeight(0.1f),
            colors = isResultsActive,
            shape = RectangleShape,
            onClick = { onScreenStateChange(Screen.ResultsScreen()) }) {
            Text("Results")
        }
    }
}