package com.funtime.sciai.ui.theme.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.network.*
import com.funtime.sciai.data.rag.RagService
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────
// Trust Hierarchy Colors
// ──────────────────────────────────────────────
val TrustGreen = Color(0xFF22C55E)    // Peer Reviewed
val TrustYellow = Color(0xFFF59E0B)   // Preprint
val TrustBlue = Color(0xFF3B82F6)     // Background
val CyanAccent = Color(0xFF38BDF8)
val DeepDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val SlateGray = Color(0xFF94A3B8)

enum class ScreenMode { TRENDING, RESULTS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ArticlesScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // ── Core State ──
    var searchQuery by remember { mutableStateOf("") }
    var screenMode by remember { mutableStateOf(ScreenMode.TRENDING) }
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var trendingArticles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isInfiniteLoading by remember { mutableStateOf(false) }
    var isTrendingLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(1) }
    var totalCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf("") }

    // ── Filter State ──
    var selectedSort by remember { mutableStateOf("pub+date") }
    var selectedDomain by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedSource by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf("Latest") }

    // ── Derived filter values for API ──
    val apiSource = when (selectedSource) {
        "PubMed" -> "pubmed"
        "arXiv" -> "arxiv"
        "Wikipedia" -> "wikipedia"
        else -> "all"
    }
    val apiDomain = when (selectedDomain) {
        "Bio" -> "biology"
        "Chemistry" -> "chemistry"
        "Physics" -> "physics"
        else -> "all"
    }
    val apiType = when (selectedType) {
        "Peer Reviewed" -> "peer_reviewed"
        "Preprint" -> "preprint"
        else -> "all"
    }
    val apiDate = when (selectedDate) {
        "Last 5 yrs" -> "last5"
        else -> "all"
    }

    // ── Derived Sections ──
    val displayArticles = if (screenMode == ScreenMode.TRENDING) trendingArticles else articles
    val peerReviewedArticles = displayArticles.filter { it.tier == "peer_reviewed" }
    val preprintArticles = displayArticles.filter { it.tier == "preprint" }
    val backgroundArticles = displayArticles.filter { it.tier == "background" }

    // ── Auto-Load Trending on Screen Open ──
    LaunchedEffect(Unit) {
        isTrendingLoading = true
        RagService.fetchTrending(limit = 10) { results, count ->
            if (results != null) {
                trendingArticles = results
            }
            isTrendingLoading = false
        }
    }

    // ── Search Function ──
    fun performSearch(isNewSearch: Boolean = true) {
        if (searchQuery.isBlank()) return

        if (isNewSearch) {
            isLoading = true
            currentPage = 1
            screenMode = ScreenMode.RESULTS
            articles = emptyList()
            totalCount = 0
        } else {
            isInfiniteLoading = true
        }

        RagService.fetchArticles(
            query = searchQuery,
            sort = selectedSort,
            page = currentPage,
            limit = 10,
            source = apiSource,
            domain = apiDomain,
            dateRange = apiDate,
            type = apiType
        ) { results, count ->
            if (results == null) {
                errorMessage = "Connection error. Check backend."
            } else {
                if (isNewSearch) {
                    articles = results
                    totalCount = count
                } else {
                    articles = articles + results
                    // totalCount stays from original search
                }
                errorMessage = ""
            }
            isLoading = false
            isInfiniteLoading = false
        }
    }

    // ── Infinite Scroll — Fixed with totalCount ──
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val currentSize = articles.size
            lastVisibleItem != null &&
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3 &&
                !isInfiniteLoading &&
                !isLoading &&
                screenMode == ScreenMode.RESULTS &&
                currentSize < totalCount &&
                currentSize > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            currentPage++
            performSearch(false)
        }
    }

    // ── Unified Back Handler Function ──
    val handleBack: () -> Unit = {
        if (screenMode == ScreenMode.RESULTS) {
            screenMode = ScreenMode.TRENDING
            articles = emptyList()
            currentPage = 1
            totalCount = 0
            searchQuery = ""
        } else {
            navController.popBackStack()
        }
    }

    // ── Back Navigation (System Back Button) ──
    BackHandler(enabled = true) {
        handleBack()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            if (screenMode == ScreenMode.RESULTS) {
                performSearch(true)
            } else {
                isTrendingLoading = true
                RagService.fetchTrending(limit = 10) { results, _ ->
                    if (results != null) trendingArticles = results
                    isTrendingLoading = false
                }
            }
        }
    )

    AppScaffold(
        title = "Articles Engine v3.0",
        navController = navController,
        showBack = true,
        onBack = handleBack
    ) { scaffoldModifier ->
        Box(
            modifier = scaffoldModifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ═══════════════════════════════════════
                // SEARCH BAR + FILTERS
                // ═══════════════════════════════════════
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepDark,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // Search TextField
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("🔍 Search Research Papers...", color = Color.Gray) },
                            trailingIcon = {
                                Row {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            if (screenMode == ScreenMode.RESULTS) {
                                                screenMode = ScreenMode.TRENDING
                                                articles = emptyList()
                                                totalCount = 0
                                            }
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                        }
                                    }
                                    IconButton(onClick = { performSearch(true) }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanAccent)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = CyanAccent
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { performSearch(true) }
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Filter Chips Row (Horizontally Scrollable) ──
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            // Domain Filters
                            val domains = listOf("All", "Bio", "Chemistry", "Physics")
                            items(domains) { domain ->
                                PremiumFilterChip(
                                    label = if (domain == "All") "📚 All" else when(domain) {
                                        "Bio" -> "🧬 Bio"
                                        "Chemistry" -> "⚗️ Chem"
                                        "Physics" -> "⚛️ Physics"
                                        else -> domain
                                    },
                                    selected = selectedDomain == domain,
                                    accentColor = when(domain) {
                                        "Bio" -> Color(0xFF22C55E)
                                        "Chemistry" -> Color(0xFFF97316)
                                        "Physics" -> Color(0xFF3B82F6)
                                        else -> CyanAccent
                                    }
                                ) {
                                    selectedDomain = if (selectedDomain == domain) "All" else domain
                                    if (screenMode == ScreenMode.RESULTS) performSearch(true)
                                }
                            }

                            // Date Filters
                            val dates = listOf("Latest", "Last 5 yrs")
                            items(dates) { date ->
                                PremiumFilterChip(
                                    label = "📅 $date",
                                    selected = selectedDate == date,
                                    accentColor = Color(0xFFA78BFA)
                                ) {
                                    selectedDate = if (selectedDate == date) "Latest" else date
                                    if (screenMode == ScreenMode.RESULTS) performSearch(true)
                                }
                            }

                            // Type Filters
                            val types = listOf("All", "Peer Reviewed", "Preprint")
                            items(types) { type ->
                                PremiumFilterChip(
                                    label = when(type) {
                                        "Peer Reviewed" -> "🟢 Peer Reviewed"
                                        "Preprint" -> "🟡 Preprint"
                                        else -> "🧪 All Types"
                                    },
                                    selected = selectedType == type,
                                    accentColor = when(type) {
                                        "Peer Reviewed" -> TrustGreen
                                        "Preprint" -> TrustYellow
                                        else -> CyanAccent
                                    }
                                ) {
                                    selectedType = if (selectedType == type) "All" else type
                                    if (screenMode == ScreenMode.RESULTS) performSearch(true)
                                }
                            }

                            // Source Filters
                            val sources = listOf("All", "PubMed", "arXiv", "Wikipedia")
                            items(sources) { source ->
                                PremiumFilterChip(
                                    label = "⭐ $source",
                                    selected = selectedSource == source,
                                    accentColor = when(source) {
                                        "PubMed" -> TrustGreen
                                        "arXiv" -> TrustYellow
                                        "Wikipedia" -> TrustBlue
                                        else -> CyanAccent
                                    }
                                ) {
                                    selectedSource = if (selectedSource == source) "All" else source
                                    if (screenMode == ScreenMode.RESULTS) performSearch(true)
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════
                // CONTENT AREA
                // ═══════════════════════════════════════
                when {
                    // Loading state (initial search)
                    (isLoading && articles.isEmpty()) || (isTrendingLoading && trendingArticles.isEmpty()) -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyanAccent, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (screenMode == ScreenMode.TRENDING) "Loading trending research..." else "Searching databases...",
                                    color = SlateGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Error state
                    errorMessage.isNotEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(errorMessage, color = Color(0xFFF87171), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = { performSearch(true) }) {
                                    Text("Retry", color = CyanAccent)
                                }
                            }
                        }
                    }

                    // Articles list
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Section Header ──
                            if (screenMode == ScreenMode.TRENDING) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Text("🔥", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Trending Research",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            if (screenMode == ScreenMode.RESULTS) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Results for \"$searchQuery\"",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${articles.size} / $totalCount",
                                            color = SlateGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Sort chips
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SmallSortChip("Latest", selectedSort == "pub+date") {
                                            selectedSort = "pub+date"
                                            performSearch(true)
                                        }
                                        SmallSortChip("Relevance", selectedSort == "relevance") {
                                            selectedSort = "relevance"
                                            performSearch(true)
                                        }
                                    }
                                }
                            }

                            // ── 🟢 Peer Reviewed Section ──
                            if (peerReviewedArticles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        emoji = "📄",
                                        title = "Peer Reviewed",
                                        count = peerReviewedArticles.size,
                                        color = TrustGreen
                                    )
                                }
                                items(peerReviewedArticles, key = { "pr_${it.title}_${it.link}" }) { article ->
                                    PremiumArticleCard(
                                        article = article,
                                        onSave = { saveArticle(article, RagService, scope, snackbarHostState) }
                                    ) {
                                        ArticleState.selectedArticle = article
                                        navController.navigate("article_detail")
                                    }
                                }
                            }

                            // ── 🟡 Preprints Section ──
                            if (preprintArticles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        emoji = "🧪",
                                        title = "Preprints",
                                        count = preprintArticles.size,
                                        color = TrustYellow
                                    )
                                }
                                items(preprintArticles, key = { "pp_${it.title}_${it.link}" }) { article ->
                                    PremiumArticleCard(
                                        article = article,
                                        onSave = { saveArticle(article, RagService, scope, snackbarHostState) }
                                    ) {
                                        ArticleState.selectedArticle = article
                                        navController.navigate("article_detail")
                                    }
                                }
                            }

                            // ── 🔵 Background Knowledge Section ──
                            if (backgroundArticles.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        emoji = "📘",
                                        title = "Background Knowledge",
                                        count = backgroundArticles.size,
                                        color = TrustBlue
                                    )
                                }
                                items(backgroundArticles, key = { "bg_${it.title}_${it.link}" }) { article ->
                                    PremiumArticleCard(
                                        article = article,
                                        onSave = { saveArticle(article, RagService, scope, snackbarHostState) }
                                    ) {
                                        ArticleState.selectedArticle = article
                                        navController.navigate("article_detail")
                                    }
                                }
                            }

                            // ── Empty state ──
                            if (displayArticles.isEmpty() && !isLoading && !isTrendingLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No articles found. Try different filters.", color = SlateGray)
                                    }
                                }
                            }

                            // ── Infinite Loading Indicator ──
                            if (isInfiniteLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = CyanAccent,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Loading more...", color = SlateGray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // ── End of results indicator ──
                            if (screenMode == ScreenMode.RESULTS && articles.size >= totalCount && articles.isNotEmpty() && !isInfiniteLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "— All ${articles.size} results loaded —",
                                            color = Color(0xFF475569),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Bottom spacer
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading || isTrendingLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = CardDark,
                contentColor = CyanAccent
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}


// ═══════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════

@Composable
fun SectionHeader(emoji: String, title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "$count",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.15f),
            selectedLabelColor = accentColor,
            containerColor = Color(0xFF1E293B),
            labelColor = SlateGray
        ),
        border = BorderStroke(
            1.dp,
            if (selected) accentColor.copy(alpha = 0.5f) else Color(0xFF334155)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SmallSortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) CyanAccent.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (selected) CyanAccent.copy(alpha = 0.5f) else Color(0xFF334155)
        )
    ) {
        Text(
            label,
            color = if (selected) CyanAccent else SlateGray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PremiumArticleCard(article: Article, onSave: () -> Unit, onClick: () -> Unit) {
    val tierColor = when (article.tier) {
        "peer_reviewed" -> TrustGreen
        "preprint" -> TrustYellow
        "background" -> TrustBlue
        else -> CyanAccent
    }
    val tierLabel = when (article.tier) {
        "peer_reviewed" -> "Peer Reviewed"
        "preprint" -> "Preprint"
        "background" -> "General"
        else -> "Research"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        // Top gradient accent stripe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(tierColor, tierColor.copy(alpha = 0.3f))
                    )
                )
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: Source Badge + Trust Indicator + Save ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source Badge
                    Surface(
                        color = CyanAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = article.source.uppercase(),
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Trust Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(tierColor)
                        )
                        Text(
                            text = tierLabel,
                            color = tierColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Save",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Title ──
            Text(
                text = article.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            // ── Authors ──
            if (article.authors.isNotEmpty() && article.authors != "Various Authors") {
                Text(
                    text = article.authors,
                    color = SlateGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Summary ──
            Text(
                text = article.summary,
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Footer: Journal · Date · Actions ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (article.journal.isNotEmpty() && article.source != "Wikipedia") {
                        Text(
                            text = article.journal,
                            color = CyanAccent.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatDisplayDate(article.date),
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier.clickable { onClick() },
                        color = CyanAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Read →",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

fun saveArticle(article: Article, service: RagService, scope: kotlinx.coroutines.CoroutineScope, snackbar: SnackbarHostState) {
    service.saveArticle(article) { success ->
        scope.launch {
            snackbar.showSnackbar(if (success) "✅ Saved to Library" else "❌ Failed to save")
        }
    }
}

fun formatDisplayDate(input: String): String {
    if (input == "No Date" || input.isEmpty() || input == "Last Updated") return input

    val parts = input.split("-")
    if (parts.isEmpty()) return input

    val year = parts[0]
    val month = if (parts.size > 1) parts[1] else ""
    val day = if (parts.size > 2) parts[2] else ""

    val monthName = when (month) {
        "01", "Jan" -> "Jan"
        "02", "Feb" -> "Feb"
        "03", "Mar" -> "Mar"
        "04", "Apr" -> "Apr"
        "05", "May" -> "May"
        "06", "Jun" -> "Jun"
        "07", "Jul" -> "Jul"
        "08", "Aug" -> "Aug"
        "09", "Sep" -> "Sep"
        "10", "Oct" -> "Oct"
        "11", "Nov" -> "Nov"
        "12", "Dec" -> "Dec"
        else -> month
    }

    return listOf(monthName, day, year).filter { it.isNotEmpty() }.joinToString(" ").trim().replace("  ", " ")
}
