package com.oakandember.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Ember = Color(0xFFB14C24)
private val EmberDark = Color(0xFF5A291A)
private val Oak = Color(0xFF7B5A3C)
private val Cream = Color(0xFFF7F1E8)
private val Ink = Color(0xFF2C2621)
private val Moss = Color(0xFF4F6A55)
private val SoftGray = Color(0xFFF1EEE9)
private val Danger = Color(0xFF9E3D32)

enum class SessionState { Checking, SignedOut, SignedIn }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val repository = remember { OakRepository(applicationContext) }
                OakEmberApp(repository)
            }
        }
    }
}

@Composable
private fun OakEmberApp(repository: OakRepository) {
    val scope = rememberCoroutineScope()
    var sessionState by remember { mutableStateOf(SessionState.Checking) }
    var requests by remember { mutableStateOf<List<OakRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<OakRequest?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var lastSync by remember { mutableStateOf<Long?>(null) }

    fun refresh(silent: Boolean = false) {
        scope.launch {
            if (!silent) loading = true
            try {
                val live = repository.fetchRequests()
                requests = live
                selected = selected?.let { current -> live.firstOrNull { it.id == current.id } ?: current }
                lastSync = System.currentTimeMillis()
                error = null
            } catch (api: OakApiException) {
                if (api.statusCode == 401 || api.statusCode == 403) {
                    repository.clearSession()
                    sessionState = SessionState.SignedOut
                    requests = emptyList()
                    selected = null
                }
                if (!silent) error = api.message
            } catch (throwable: Exception) {
                if (!silent) error = throwable.message ?: "Unable to refresh requests."
            } finally {
                if (!silent) loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val restored = repository.restoreSession()
        sessionState = if (restored) SessionState.SignedIn else SessionState.SignedOut
        if (restored) {
            loading = true
            try {
                requests = repository.fetchRequests()
                lastSync = System.currentTimeMillis()
            } catch (api: OakApiException) {
                error = api.message
                if (api.statusCode == 401 || api.statusCode == 403) {
                    repository.clearSession()
                    sessionState = SessionState.SignedOut
                }
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState == SessionState.SignedIn) {
            while (true) {
                delay(30_000)
                try {
                    val live = repository.fetchRequests()
                    requests = live
                    selected = selected?.let { current -> live.firstOrNull { it.id == current.id } ?: current }
                    lastSync = System.currentTimeMillis()
                } catch (_: Exception) {
                    // Silent background refresh. Manual refresh still reports errors.
                }
            }
        }
    }

    when (sessionState) {
        SessionState.Checking -> SplashScreen()
        SessionState.SignedOut -> LoginScreen(
            initialEmail = repository.lastEmail(),
            onLogin = { email, password ->
                scope.launch {
                    loading = true
                    error = null
                    try {
                        repository.signIn(email, password)
                        sessionState = SessionState.SignedIn
                        requests = repository.fetchRequests()
                        lastSync = System.currentTimeMillis()
                    } catch (api: OakApiException) {
                        error = api.message
                    } catch (throwable: Exception) {
                        error = throwable.message ?: "Unable to sign in."
                    } finally {
                        loading = false
                    }
                }
            },
            loading = loading,
            error = error
        )
        SessionState.SignedIn -> {
            val current = selected
            if (current != null) {
                RequestDetailScreen(
                    request = current,
                    repository = repository,
                    onBack = { selected = null },
                    onSaved = { updated ->
                        requests = requests.map { if (it.id == updated.id) updated else it }
                        selected = updated
                    },
                    onSessionExpired = {
                        repository.clearSession()
                        selected = null
                        requests = emptyList()
                        sessionState = SessionState.SignedOut
                    }
                )
            } else {
                MainShell(
                    tab = tab,
                    onTab = { tab = it },
                    requests = requests,
                    loading = loading,
                    error = error,
                    lastSync = lastSync,
                    onRefresh = { refresh(false) },
                    onOpen = { selected = it },
                    onLogout = {
                        repository.clearSession()
                        requests = emptyList()
                        sessionState = SessionState.SignedOut
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(EmberDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(70.dp).background(Ember, CircleShape), contentAlignment = Alignment.Center) {
                Text("🔥", fontSize = 34.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("OAK & EMBER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(18.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun LoginScreen(
    initialEmail: String,
    onLogin: (String, String) -> Unit,
    loading: Boolean,
    error: String?
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(22.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(62.dp).background(Ember, CircleShape), contentAlignment = Alignment.Center) {
                    Text("🔥", fontSize = 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("OAK & EMBER", color = EmberDark, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Private service manager", color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Operator email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = Danger, fontSize = 13.sp)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onLogin(email.trim(), password) },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember)
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Sign in")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Connected to the live Oak & Ember database", color = Moss, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MainShell(
    tab: Int,
    onTab: (Int) -> Unit,
    requests: List<OakRequest>,
    loading: Boolean,
    error: String?,
    lastSync: Long?,
    onRefresh: () -> Unit,
    onOpen: (OakRequest) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = { BrandHeader(onRefresh, onLogout, loading) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navItems = listOf(
                    Triple("⌂", "Home", 0),
                    Triple("☰", "Requests", 1),
                    Triple("◷", "Calendar", 2),
                    Triple("♙", "Clients", 3)
                )
                navItems.forEach { (icon, label, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { onTab(index) },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> Dashboard(requests, lastSync, onOpen)
                1 -> RequestsScreen(requests, onOpen)
                2 -> CalendarScreen(requests, onOpen)
                3 -> ClientsScreen(requests, onOpen)
            }
            if (!error.isNullOrBlank()) {
                ErrorBanner(error, Modifier.align(Alignment.BottomCenter))
            }
            if (loading && requests.isEmpty()) {
                Box(Modifier.fillMaxSize().background(Cream.copy(alpha = .88f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ember)
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(onRefresh: () -> Unit, onLogout: () -> Unit, loading: Boolean) {
    Surface(color = EmberDark, shadowElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).background(Ember, CircleShape), contentAlignment = Alignment.Center) {
                Text("🔥", fontSize = 21.sp)
            }
            Column(Modifier.padding(start = 11.dp).weight(1f)) {
                Text("OAK & EMBER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color(0xFF79C58A), fontSize = 12.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("LIVE DATABASE", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, letterSpacing = .7.sp)
                }
            }
            TextButton(onClick = onRefresh, enabled = !loading) { Text(if (loading) "…" else "Refresh", color = Color.White) }
            TextButton(onClick = onLogout) { Text("Sign out", color = Color.White.copy(alpha = .82f)) }
        }
    }
}

@Composable
private fun Dashboard(requests: List<OakRequest>, lastSync: Long?, onOpen: (OakRequest) -> Unit) {
    val newCount = requests.count { it.status == "Nueva" }
    val activeCount = requests.count { it.status != "Cerrada" }
    val closedCount = requests.count { it.status == "Cerrada" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Service manager", color = Oak, fontSize = 14.sp)
            Text("Today's overview", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(
                if (lastSync == null) "Connecting to live data…" else "Live records · auto-refresh every 30 seconds",
                color = Ink.copy(alpha = .55f),
                fontSize = 12.sp
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(newCount.toString(), "New", Ember, Modifier.weight(1f))
                MetricCard(activeCount.toString(), "Active", Moss, Modifier.weight(1f))
                MetricCard(closedCount.toString(), "Closed", Oak, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Newest requests") }
        if (requests.isEmpty()) {
            item { EmptyCard("No live requests yet.") }
        } else {
            items(requests.take(5), key = { it.id }) { RequestCard(it, onOpen) }
        }
    }
}

@Composable
private fun RequestsScreen(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val filtered = remember(requests, query, filter) {
        requests.filter { request ->
            val statusOk = when (filter) {
                "New" -> request.status == "Nueva"
                "Open" -> request.status != "Cerrada"
                "Closed" -> request.status == "Cerrada"
                else -> true
            }
            val needle = query.trim().lowercase(Locale.US)
            val searchOk = needle.isBlank() || listOf(
                request.payload.name,
                request.payload.phone,
                request.payload.email,
                request.payload.address,
                request.payload.zip,
                request.payload.details,
                serviceLabel(request.payload.service),
                statusLabel(request.status)
            ).any { it.lowercase(Locale.US).contains(needle) }
            statusOk && searchOk
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Requests", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("${requests.size} live service request${if (requests.size == 1) "" else "s"}", color = Ink.copy(alpha = .55f))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search name, phone, service or ZIP") }
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "New", "Open", "Closed")) { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
        }
        if (filtered.isEmpty()) item { EmptyCard("No requests match this view.") }
        items(filtered, key = { it.id }) { RequestCard(it, onOpen) }
    }
}

@Composable
private fun CalendarScreen(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
    val scheduled = requests.filter { it.scheduledDate().isNotBlank() }
        .sortedBy { it.scheduledDate() }
    val unscheduled = requests.filter { it.scheduledDate().isBlank() && it.status != "Cerrada" }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Calendar", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Scheduled and waiting-to-schedule work", color = Ink.copy(alpha = .55f))
        }
        if (scheduled.isEmpty()) {
            item { EmptyCard("No scheduled visits are stored yet.") }
        } else {
            val groups = scheduled.groupBy { it.scheduledDate() }
            groups.forEach { (date, rows) ->
                item { DayHeader(formatDateOnly(date), "${rows.size} visit${if (rows.size == 1) "" else "s"}") }
                items(rows, key = { it.id }) { RequestCard(it, onOpen) }
            }
        }
        if (unscheduled.isNotEmpty()) {
            item { DayHeader("UNSCHEDULED", "${unscheduled.size} open") }
            items(unscheduled, key = { it.id }) { RequestCard(it, onOpen) }
        }
    }
}

@Composable
private fun ClientsScreen(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
    val groups = remember(requests) {
        requests.groupBy { it.customerKey() }
            .values
            .sortedByDescending { group -> group.maxOfOrNull { it.createdAt } ?: "" }
    }
    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Clients", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Derived from real service requests", color = Ink.copy(alpha = .55f))
        }
        if (groups.isEmpty()) item { EmptyCard("No clients yet.") }
        items(groups, key = { it.first().customerKey() }) { group ->
            val latest = group.maxByOrNull { it.createdAt } ?: group.first()
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(latest) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(SoftGray, CircleShape), contentAlignment = Alignment.Center) {
                        Text(latest.payload.name.take(1).ifBlank { "?" }, color = EmberDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(Modifier.padding(start = 13.dp).weight(1f)) {
                        Text(latest.payload.name.ifBlank { "Unnamed client" }, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            latest.payload.phone.ifBlank { latest.payload.email.ifBlank { latest.payload.zip } },
                            color = Ink.copy(alpha = .55f),
                            fontSize = 13.sp
                        )
                    }
                    Text("${group.size} job${if (group.size == 1) "" else "s"}", color = Oak, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: OakRequest, onClick: (OakRequest) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(request) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(request.status)
                Spacer(Modifier.weight(1f))
                Text(request.id.take(8).uppercase(Locale.US), color = Ink.copy(alpha = .38f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(request.payload.name.ifBlank { "Unnamed client" }, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(serviceLabel(request.payload.service), color = EmberDark, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            val location = listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(" · ")
            if (location.isNotBlank()) Text("📍 $location", color = Ink.copy(alpha = .62f), fontSize = 13.sp)
            Text("🕒 ${request.scheduledTime().ifBlank { "Flexible" }}", color = Ink.copy(alpha = .62f), fontSize = 13.sp)
            if (request.photos.isNotEmpty()) Text("📷 ${request.photos.size} photo${if (request.photos.size == 1) "" else "s"}", color = Ink.copy(alpha = .62f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                request.payload.details.ifBlank { request.notes.ifBlank { "No additional details." } },
                color = Ink.copy(alpha = .58f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RequestDetailScreen(
    request: OakRequest,
    repository: OakRepository,
    onBack: () -> Unit,
    onSaved: (OakRequest) -> Unit,
    onSessionExpired: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var status by remember(request.id, request.status) { mutableStateOf(request.status) }
    var notes by remember(request.id, request.notes) { mutableStateOf(request.notes) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<PhotoRef?>(null) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            Surface(color = EmberDark) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("‹ Back", color = Color.White, fontSize = 16.sp) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SERVICE REQUEST", color = Color.White.copy(alpha = .7f), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(request.id.take(8).uppercase(Locale.US), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(62.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(request.payload.name.ifBlank { "Unnamed client" }, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(serviceLabel(request.payload.service), color = EmberDark, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(formatCreatedAt(request.createdAt), color = Ink.copy(alpha = .5f), fontSize = 12.sp)
            }
            item {
                SectionCard("Status") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allowedStatuses) { option ->
                            FilterChip(selected = status == option, onClick = { status = option }, label = { Text(statusLabel(option)) })
                        }
                    }
                }
            }
            item {
                SectionCard("Contact") {
                    DetailLine("Phone", request.payload.phone.ifBlank { "—" })
                    DetailLine("Email", request.payload.email.ifBlank { "—" })
                    DetailLine("Preferred", request.payload.contact.ifBlank { "—" })
                    DetailLine("Address", listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "—" })
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (request.payload.phone.isNotBlank()) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.payload.phone}")))
                            }) { Text("Call") }
                        }
                        if (request.payload.email.isNotBlank()) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${request.payload.email}")))
                            }) { Text("Email") }
                        }
                        if (request.payload.address.isNotBlank() || request.payload.zip.isNotBlank()) {
                            OutlinedButton(onClick = {
                                val destination = listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(" ")
                                val uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) { Text("Map") }
                        }
                    }
                }
            }
            item {
                SectionCard("Customer request") {
                    DetailLine("Service", serviceLabel(request.payload.service))
                    DetailLine("Appliance", request.payload.appliance.ifBlank { "Not sure" })
                    if (request.payload.reason.isNotBlank()) DetailLine("Reason", request.payload.reason)
                    DetailLine("Best time", request.payload.time.ifBlank { "Flexible" })
                    Spacer(Modifier.height(8.dp))
                    Text(request.payload.details.ifBlank { "No description provided." }, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            if (request.photos.isNotEmpty()) {
                item {
                    SectionCard("Photos (${request.photos.size})") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(request.photos, key = { it.path }) { photo ->
                                AuthenticatedPhoto(
                                    repository = repository,
                                    photo = photo,
                                    modifier = Modifier.size(118.dp).clip(RoundedCornerShape(14.dp)).clickable { selectedPhoto = photo }
                                )
                            }
                        }
                    }
                }
            }
            if (request.visit != null) {
                item {
                    SectionCard("Scheduled visit") {
                        DetailLine("Date", request.scheduledDate().ifBlank { "—" })
                        DetailLine("Time", request.scheduledTime().ifBlank { "—" })
                    }
                }
            }
            if (request.quote != null) {
                item {
                    SectionCard("Quote") {
                        val q = request.quote
                        val total = sequenceOf("total", "amount", "price").map { q.opt(it)?.toString().orEmpty() }.firstOrNull { it.isNotBlank() }.orEmpty()
                        val description = sequenceOf("description", "details", "notes").map { q.optString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
                        if (total.isNotBlank()) DetailLine("Total", total)
                        if (description.isNotBlank()) Text(description, color = Ink, fontSize = 14.sp)
                        if (total.isBlank() && description.isBlank()) Text("Quote information is attached to this request.", color = Ink.copy(alpha = .6f), fontSize = 13.sp)
                    }
                }
            }
            item {
                SectionCard("Internal notes") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 10_000) notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Notes for the team") }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Only operators can read these notes.", color = Ink.copy(alpha = .5f), fontSize = 11.sp)
                }
            }
            if (!message.isNullOrBlank()) {
                item { Text(message!!, color = if (message!!.startsWith("Saved")) Moss else Danger, fontSize = 13.sp) }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            message = null
                            try {
                                val updated = repository.updateRequest(request.id, status, notes)
                                onSaved(updated)
                                message = "Saved to the live database."
                            } catch (api: OakApiException) {
                                if (api.statusCode == 401 || api.statusCode == 403) onSessionExpired()
                                else message = api.message
                            } catch (throwable: Exception) {
                                message = throwable.message ?: "Unable to save changes."
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && (status != request.status || notes != request.notes),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ember)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Save changes")
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    selectedPhoto?.let { photo ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text("Close") } },
            text = {
                AuthenticatedPhoto(
                    repository = repository,
                    photo = photo,
                    modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(14.dp))
                )
            }
        )
    }
}

@Composable
private fun AuthenticatedPhoto(repository: OakRepository, photo: PhotoRef, modifier: Modifier = Modifier) {
    var bitmap by remember(photo.path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var failed by remember(photo.path) { mutableStateOf(false) }
    LaunchedEffect(photo.path) {
        try {
            val bytes = repository.loadPhoto(photo.path)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap = decoded?.asImageBitmap()
            failed = bitmap == null
        } catch (_: Exception) {
            failed = true
        }
    }
    Box(modifier.background(SoftGray), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap = bitmap!!, contentDescription = "Customer photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            failed -> Text("Photo unavailable", color = Ink.copy(alpha = .5f), fontSize = 11.sp)
            else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Ember)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = Ink.copy(alpha = .5f), fontSize = 12.sp, modifier = Modifier.width(86.dp))
        Text(value, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(label, color = Ink.copy(alpha = .6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val background = when (status) {
        "Nueva" -> Color(0xFFFFE6DC)
        "Por contactar" -> Color(0xFFFFEDCB)
        "Esperando información" -> Color(0xFFE7E3F2)
        "Presupuesto enviado" -> Color(0xFFE1ECF5)
        "Aceptada" -> Color(0xFFDDEDE1)
        "Cerrada" -> SoftGray
        else -> SoftGray
    }
    val foreground = when (status) {
        "Nueva" -> Ember
        "Aceptada" -> Moss
        "Cerrada" -> Ink.copy(alpha = .55f)
        else -> EmberDark
    }
    Box(Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(statusLabel(status), color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DayHeader(day: String, count: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(day, color = Oak, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text(count, color = Ink.copy(alpha = .45f), fontSize = 12.sp)
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(18.dp), color = Ink.copy(alpha = .58f), fontSize = 13.sp)
    }
}

@Composable
private fun ErrorBanner(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(12.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7E2)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, Modifier.padding(12.dp), color = Danger, fontSize = 12.sp)
    }
}
