package theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with

object CucumberConverterTypography {
    private val robotoFamily = FontFamily(
        Font("roboto_regular.ttf", FontWeight.Normal),
        Font("roboto_medium.ttf", FontWeight.Medium),
        Font("roboto_italic.ttf", FontWeight.Normal, FontStyle.Italic),
        Font("roboto_bold.ttf", FontWeight.Bold),
    )
    val typography = Typography(
        body1 = TextStyle(
            fontFamily = robotoFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp
        ),
        body2 = TextStyle(
            fontFamily = robotoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        ),
    )

    val RobotoPlaceholderUiL = TextStyle(
        fontFamily = robotoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        textAlign = TextAlign.Start,
        color = Color.Black
    )

    val RobotoPrimaryButtonUiC = TextStyle(
        fontFamily = robotoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        color = Color.White
    )
}