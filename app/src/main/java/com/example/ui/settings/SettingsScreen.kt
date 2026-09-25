package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.chat.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel
) {
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf(apiKey) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aiko's Settings & Models") }
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
            // API Key Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = "API Key", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Gemini API Key", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your Gemini API key below to power Aiko and avoid rate limits.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.setApiKey(apiKeyInput) },
                        modifier = Modifier.testTag("save_api_key_button")
                    ) {
                        Text("Save API Key")
                    }
                }
            }

            // Model Selection & Scan Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = "Models", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Gemini Models", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan available models from the Gemini URL or select your active model. Default is gemini-3.5-flash-lite.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.scanModels() },
                        modifier = Modifier.testTag("scan_models_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Available Models")
                    }

                    if (scanStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Active Model: $selectedModel", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Exposed Dropdown for models
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("model_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.setSelectedModel(model)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }



            // Custom Hosted TTS (OpenAI Compatible) Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val customEndpoint by viewModel.customTtsEndpoint.collectAsStateWithLifecycle()
                    val customModel by viewModel.customTtsModel.collectAsStateWithLifecycle()
                    val customTtsApiKey by viewModel.customTtsApiKey.collectAsStateWithLifecycle()

                    var endpointInput by remember { mutableStateOf(customEndpoint) }
                    var modelInput by remember { mutableStateOf(customModel) }
                    var apiKeyInput by remember { mutableStateOf(customTtsApiKey) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(androidx.compose.material.icons.Icons.Default.SmartToy, contentDescription = "Custom TTS", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Custom Hosted TTS Endpoint", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gemini replies will be sent to your hosted TTS endpoint using payload:\n{\"model\": \"tsukuyomi\", \"input\": \"text\"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text("TTS Endpoint URL") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_tts_endpoint_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("TTS Model (e.g. tsukuyomi)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_tts_model_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("TTS API Key / Bearer (Optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_tts_apikey_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.setCustomTtsEndpoint(endpointInput)
                            viewModel.setCustomTtsModel(modelInput)
                            viewModel.setCustomTtsApiKey(apiKeyInput)
                        },
                        modifier = Modifier.testTag("save_custom_tts_button")
                    ) {
                        Text("Save Custom TTS Settings")
                    }
                }
            }
        }
    }
}
