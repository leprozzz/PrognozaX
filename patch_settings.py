import re

with open('app/src/main/java/com/climateinsights/app/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

replacement = """                }
                
                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tema aplikacije", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == null,
                                onClick = { viewModel.setDarkTheme(null) },
                                label = { Text("Sistemska") },
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == false,
                                onClick = { viewModel.setDarkTheme(false) },
                                label = { Text("Svijetla") },
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == true,
                                onClick = { viewModel.setDarkTheme(true) },
                                label = { Text("Tamna") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    text = "Dostupni gradovi (${state.cities.size})","""

pat = re.compile(r'\}\s+Text\(\s+text = "Dostupni gradovi \(\$\{state\.cities\.size\}\)",')
if pat.search(content):
    new_content = pat.sub(replacement, content)
    with open('app/src/main/java/com/climateinsights/app/ui/screens/SettingsScreen.kt', 'w') as f:
        f.write(new_content)
    print('Regex replaced successfully!')
else:
    print('Regex failed too')
