package com.example.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.chat.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    viewModel: ChatViewModel
) {
    var fortuneResult by remember { mutableStateOf("Tap below to get your daily fortune from Aiko! ✨") }
    var triviaQuestion by remember { mutableStateOf("What is Aiko's favorite snack?") }
    var triviaAnswer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }

    val fortunes = listOf(
        "Dai-kichi! Super Lucky! Senpai's wishes will come true today! (≧◡≦)",
        "Chu-kichi! Medium luck! A sweet surprise awaits Senpai this afternoon! 🌸",
        "Shou-kichi! Little luck! Take a coffee break with Aiko-chan! ☕",
        "Lucky love fortune! Aiko is thinking about Senpai right now! ❤️"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aiko's Corner & Games") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Fortune Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, contentDescription = "Fortune", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Daily Fortune with Aiko", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = fortuneResult, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val pick = fortunes.random()
                            fortuneResult = pick
                            viewModel.speakText(pick)
                        },
                        modifier = Modifier.testTag("draw_fortune_button")
                    ) {
                        Text("Draw Fortune ✨")
                    }
                }
            }

            // Anime Trivia Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = "Trivia", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Aiko's Trivia Challenge", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = triviaQuestion, style = MaterialTheme.typography.bodyMedium)
                    if (showAnswer) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Answer: Strawberry Pocky & Strawberry Milk! 🍓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showAnswer = !showAnswer
                            if (showAnswer) {
                                viewModel.speakText("Aiko's favorite snack is strawberry Pocky and strawberry milk!")
                            }
                        },
                        modifier = Modifier.testTag("reveal_trivia_button")
                    ) {
                        Text(if (showAnswer) "Hide Answer" else "Reveal Answer")
                    }
                }
            }

            // Chat companion prompt quick trigger
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ask Aiko to Play", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Want to play a word game or tell stories together with Aiko?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.sendMessage("Let's play Shiritori or tell a magical anime story together!")
                        },
                        modifier = Modifier.testTag("play_story_button")
                    ) {
                        Text("Start Story Game with Aiko")
                    }
                }
            }
        }
    }
}
