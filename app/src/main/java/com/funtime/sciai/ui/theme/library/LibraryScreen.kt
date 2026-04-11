package com.funtime.sciai.ui.theme.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.rag.RagService
import com.funtime.sciai.data.Book
import com.funtime.sciai.data.UserManager
import com.funtime.sciai.data.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(navController: NavController, drawerState: androidx.compose.material3.DrawerState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    val userId = remember { userManager.getUserId() }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var savedArticles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isArticlesLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Delete confirmation dialog state
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var articleToUnsave by remember { mutableStateOf<Article?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val refreshBooks = {
        isLoading = true
        RagService.fetchBooks(userId) { bookList ->
            isLoading = false
            if (bookList == null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error: Server unreachable")
                }
            } else {
                books = bookList.map {
                    val finalId = it.id?.takeIf { id -> id.isNotBlank() } 
                        ?: "syn_book_${it.title.hashCode()}"
                    Book(
                        finalId, 
                        it.title ?: "", 
                        it.domain ?: "General", 
                        it.preview ?: ""
                    )
                }
                errorMessage = ""
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { refreshBooks() }
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = FileUtils.uriToFile(context, it)
            if (file != null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Uploading ${file.name}...")
                }
                RagService.uploadBook(file, "General", userId) { success, message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                        if (success) refreshBooks()
                    }
                }
            }
        }
    }

    val refreshArticles = {
        isArticlesLoading = true
        RagService.fetchSavedArticles(userId) { articles ->
            isArticlesLoading = false
            if (articles != null) {
                savedArticles = articles
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBooks()
        refreshArticles()
    }

    // Delete Confirmation Dialog
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) bookToDelete = null
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = {
                Text(
                    "Delete PDF",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${bookToDelete!!.title}\"? This will also remove all indexed data for this document."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToDelete!!
                        isDeleting = true
                        RagService.deleteBook(book.id, userId) { success, message ->
                            scope.launch {
                                isDeleting = false
                                bookToDelete = null
                                snackbarHostState.showSnackbar(message)
                                if (success) refreshBooks()
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookToDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = Color(0xFF38BDF8))
                }
            }
        )
    }
    
    // Article Removal Dialog
    if (articleToUnsave != null) {
        AlertDialog(
            onDismissRequest = { articleToUnsave = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            title = { Text("Remove from Library?", fontWeight = FontWeight.Bold) },
            text = { Text("This article will be permanently removed from your saved research collection.") },
            confirmButton = {
                Button(
                    onClick = {
                        val art = articleToUnsave!!
                        articleToUnsave = null
                        RagService.unsaveArticle(userId, art.id) { success ->
                            if (success) {
                                scope.launch { snackbarHostState.showSnackbar("Removed from library") }
                                refreshArticles()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToUnsave = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    AppScaffold(
        title = "Library v2.0",
        navController = navController,
        showBack = false,
        onMenuClick = { scope.launch { drawerState.open() } },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { launcher.launch("application/pdf") },
                containerColor = Color(0xFF38BDF8),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload PDF")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isLoading && books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Refreshing library...", color = Color.Gray)
                        }
                    }
                } else if (!isLoading && books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                color = Color(0xFF38BDF8).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Info, 
                                        contentDescription = null, 
                                        tint = Color(0xFF38BDF8), 
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Your library is empty", 
                                color = Color.White, 
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Upload research PDFs to start synthesizing knowledge with SciAI.", 
                                color = Color.Gray, 
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (errorMessage.isNotEmpty() && books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage, color = Color.Red)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { refreshBooks() }) {
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books, key = { "library_${it.id}" }) { book ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("libraryDetail/${book.id}")
                                    },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = book.title,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = book.domain,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = book.preview,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { bookToDelete = book },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${book.title}",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 📄 Saved Research Section ──
                        if (savedArticles.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Saved Research",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Surface(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "${savedArticles.size}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(savedArticles, key = { "saved_art_${it.id}" }) { article ->
                                PremiumArticleCard(
                                    article = article,
                                    isSaved = true,
                                    onSave = { articleToUnsave = article }
                                ) {
                                    ArticleState.selectedArticle = article
                                    navController.navigate("article_detail")
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        } else if (!isArticlesLoading && books.isNotEmpty()) {
                             item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    "No research papers saved yet. Search in 'Articles' to find more.", 
                                    color = Color.Gray, 
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8)
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )
        }
    }
}

