import re

with open('app/src/main/java/com/funtime/sciai/ui/theme/home/HomeScreen.kt', 'r', encoding='utf-8', errors='ignore') as f:
    text = f.read()

# Find the start of the Floating component Box
pattern_start = re.search(r'// [^\n]*FLOATING SMART INTERFACE \(Phase 4\)[^\n]*\n\s*Box\(\n\s*modifier\s*=\s*Modifier\n\s*\.align\(Alignment\.BottomCenter\)', text)
# Find the start of the Helper Composables comment section
pattern_end_match = re.search(r'\n// [^\n]*DASHBOARD HELPER COMPOSABLES', text)

if pattern_start and pattern_end_match:
    start_idx = pattern_start.start()
    end_idx = pattern_end_match.start()
    
    # We replace everything from start_idx to end_idx with our function call
    before = text[:start_idx]
    after = text[end_idx:]
    
    call_code = '''// ── FLOATING SMART INTERFACE (Phase 4) ────────────────
        FloatingSmartInterface(
            query = query,
            onQueryChange = { query = it },
            isSearchFocused = isSearchFocused,
            onFocusChange = { isSearchFocused = it },
            selectedDomain = selectedDomain,
            onDomainSelect = { selectedDomain = it },
            selectedMode = selectedMode,
            onModeSelect = { selectedMode = it },
            lastPredictedContext = lastPredictedContext,
            isProcessingImage = isProcessingImage,
            imageUris = imageUris,
            onAddImageClick = { galleryLauncher.launch("image/*") },
            onVoiceClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                }
                speechLauncher.launch(intent)
            },
            onSendClick = { executeSearch() },
            focusManager = focusManager
        )
    }
}'''
    # Wait, the closing braces. Let me check what was there originally.
    # The `com.funtime.sciai.components.AppScaffold` block had:
    #     Box(modifier = Modifier.fillMaxSize().padding(padding)) {
    #       LazyColumn { ... }
    #       AnimatedVisibility { Box(...) } -> dim overlay
    #       Box { ... } -> Old Floating Smart Interface
    #     }
    #   }
    
    # The call code above includes the closing `}` for `Box` and `AppScaffold`.

    # Definition of the FloatingSmartInterface Component
    new_component = '''
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingSmartInterface(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    selectedDomain: String,
    onDomainSelect: (String) -> Unit,
    selectedMode: String,
    onModeSelect: (String) -> Unit,
    lastPredictedContext: PredictedContext?,
    isProcessingImage: Boolean,
    imageUris: List<Uri>,
    onAddImageClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val CyanAccent = Color(0xFF38BDF8)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, bottom = 24.dp)
            .zIndex(10f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ── SMART CONTEXT BAR (MORPHING) ──────────────────
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(bottom = 12.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (isSearchFocused) {
                        // EXPANDED VIEW
                        Text("SELECT DOMAIN", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                            listOf("Biology", "Physics", "Chemistry", "Science").forEach { domain ->
                                val isSelected = selectedDomain == domain
                                Surface(
                                    onClick = { onDomainSelect(domain) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(when(domain){ "Biology"->"🦠"; "Physics"->"⚛"; "Chemistry"->"🧪"; else->"🌍" }, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(domain, color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SELECT MODE", color = Color(0xFFFBBF24), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                            listOf("Exam", "Concept", "Expert", "Quiz", "Test").forEach { mode ->
                                val isSelected = selectedMode == mode
                                Surface(
                                    onClick = { onModeSelect(mode) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(when(mode){ "Exam"->"📘"; "Concept"->"🧠"; "Expert"->"🔬"; "Quiz"->"🎯"; else->"📝" }, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(mode, color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        lastPredictedContext?.let { ctx ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Surface(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                                ) {
                                    val domainLabel = ctx.domain ?: selectedDomain
                                    val modeLabel = ctx.mode ?: selectedMode
                                    Text(
                                        text = "Detected: $domainLabel • $modeLabel",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // COMPACT VIEW
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(6.dp)) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🦠 $selectedDomain", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("📘 $selectedMode", color = Color(0xFFFBBF24), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ── SEARCH PILL ──────────────────────────────
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.5.dp, if (isSearchFocused) CyanAccent else Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .shadow(20.dp, RoundedCornerShape(32.dp))
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ask SciAI anything...", color = Color.Gray, fontSize = 15.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChange(it.isFocused) },
                    singleLine = false,
                    maxLines = 4,
                    enabled = !isProcessingImage,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { 
                        onSendClick() 
                        onFocusChange(false)
                        focusManager.clearFocus()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = CyanAccent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    leadingIcon = {
                        IconButton(onClick = onAddImageClick) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            if (query.isNotBlank() || imageUris.isNotEmpty()) {
                                IconButton(onClick = { 
                                    onSendClick()
                                    onFocusChange(false)
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = CyanAccent)
                                }
                            } else {
                                IconButton(onClick = onVoiceClick) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
'''
    new_text = before + call_code + after + '\n' + new_component
    
    # We still need to make sure CyanAccent is defined, I already added it in a previous run but maybe we should ensure it
    with open('app/src/main/java/com/funtime/sciai/ui/theme/home/HomeScreen.kt', 'w', encoding='utf-8') as f:
        f.write(new_text)
    print("SUCCESS: Cleaned and replaced properly.")
else:
    print("ERROR matching")
