package com.localflow.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localflow.sdk.Localflow
import com.localflow.sdk.ui.compose.LocalflowProvider
import com.localflow.sdk.ui.compose.LocalflowSyncEffect
import com.localflow.sdk.ui.compose.localflowString
import com.localflow.sdk.ui.compose.localflowHtmlString
import com.localflow.sdk.ui.compose.rememberLocalflowLanguages

class ComposeDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Wraps composition inside the dynamic provider
            LocalflowProvider {
                // Auto-sync checks when Composable is active
                LocalflowSyncEffect()

                var currentScreen by remember { mutableStateOf("home") }

                if (currentScreen == "home") {
                    ComposeDemoScreen(
                        onGoToDetail = { currentScreen = "detail" },
                        onBack = { finish() }
                    )
                } else if (currentScreen == "detail") {
                    ComposeDetailScreen(
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDemoScreen(
    onGoToDetail: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jetpack Compose Demo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            
            // Dynamic text using Compose helper functions (with HTML support)
            Text(
                text = localflowHtmlString("home.title"),
                fontSize = 30.sp,
                color = Color(0xFF212121) // Removed FontWeight here to allow HTML <b> tag to work
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = localflowHtmlString(
                    "home.subtitle",
                    mapOf("name" to "Locaflow User", "app" to "Android SDK")
                ),
                fontSize = 18.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rendering seeded key values
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dashboard Seeded Translation Keys:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TranslationItem(label = "login.title", value = localflowString("login.title"))
                    TranslationItem(label = "login.button", value = localflowString("login.button"))
                    TranslationItem(label = "profile.empty_state.title", value = localflowString("profile.empty_state.title"))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Language Controls (State Recomposes UI)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Available languages fetched from project bootstrap
            val languages = rememberLocalflowLanguages()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (languages.isEmpty()) {
                    // Default fallback switch options
                    LanguageButton(langCode = "en", label = "English")
                    LanguageButton(langCode = "id", label = "Bahasa")
                    LanguageButton(langCode = "ja", label = "日本語")
                } else {
                    languages.forEach { lang ->
                        LanguageButton(langCode = lang.code, label = lang.name)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGoToDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Open Compose Detail Screen", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text("Go Back to XML Demo", color = Color.Black)
            }
        }
    }
}

@Composable
fun TranslationItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF6200EE), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color(0xFF212121), fontSize = 14.sp)
    }
}


@Composable
fun LanguageButton(langCode: String, label: String) {
    val activeLang = Localflow.getCurrentLanguage()
    val isSelected = activeLang.lowercase() == langCode.lowercase()
    
    Button(
        onClick = { Localflow.setLanguage(langCode) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFE0E0E0)
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDetailScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Detail Page", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = localflowString("detail_title", androidx.compose.ui.res.stringResource(id = R.string.detail_title)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = localflowString("detail_desc", androidx.compose.ui.res.stringResource(id = R.string.detail_desc)),
                fontSize = 16.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text("Back to Home", color = Color.Black)
            }
        }
    }
}
