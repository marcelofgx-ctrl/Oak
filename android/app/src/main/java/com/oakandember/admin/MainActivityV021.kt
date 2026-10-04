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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateMapOf
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VEmber = Color(0xFFB14C24)
private val VEmberDark = Color(0xFF5A291A)
private val VOak = Color(0xFF7B5A3C)
private val VCream = Color(0xFFF7F1E8)
private val VInk = Color(0xFF2C2621)
private val VMoss = Color(0xFF4F6A55)
private val VSoftGray = Color(0xFFF1EEE9)
private val VDanger = Color(0xFF9E3D32)

enum class V021SessionState { Checking, SignedOut, SignedIn }

class MainActivityV021 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val repository = remember { OakRepository(applicationContext) }
                V021App(repository)
            }
        }
    }
}

@Composable
private fun V021App(repository: OakRepository) {
    val scope = rememberCoroutineScope()
    var sessionState by remember { mutableStateOf(V021SessionState.Checking) }
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
                if (!silent) error = null
            } catch (api: OakApiException) {
                if (api.statusCode == 401 || api.statusCode == 403) {
                    repository.clearSession()
                    sessionState = V021SessionState.SignedOut
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
        sessionState = if (restored) V021SessionState.SignedIn else V021SessionState.SignedOut
        if (restored) {
            loading = true
            try {
                requests = repository.fetchRequests()
                lastSync = System.currentTimeMillis()
                error = null
            } catch (api: OakApiException) {
                error = api.message
                if (api.statusCode == 401 || api.statusCode == 403) {
                    repository.clearSession()
                    sessionState = V021SessionState.SignedOut
                }
            } catch (throwable: Exception) {
                error = throwable.message ?: "Unable to load Oak & Ember."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState == V021SessionState.SignedIn) {
            while (true) {
                delay(30_000)
                try {
                    val live = repository.fetchRequests()
                    requests = live
                    selected = selected?.let { current -> live.firstOrNull { it.id == current.id } ?: current }
                    lastSync = System.currentTimeMillis()
                } catch (_: Exception) {
                    // Silent refresh intentionally keeps the current screen usable.
                }
            }
        }
    }

    when (sessionState) {
        V021SessionState.Checking -> V021Splash()
        V021SessionState.SignedOut -> V021Login(
            initialEmail = repository.lastEmail(),
            loading = loading,
            error = error,
            onLogin = { email, password ->
                scope.launch {
                    loading = true
                    error = null
                    try {
                        repository.signIn(email, password)
                        requests = repository.fetchRequests()
                        lastSync = System.currentTimeMillis()
                        sessionState = V021SessionState.SignedIn
                    } catch (api: OakApiException) {
                        error = api.message
                    } catch (throwable: Exception) {
                        error = throwable.message ?: "Unable to sign in."
                    } finally {
                        loading = false
                    }
                }
            }
        )
        V021SessionState.SignedIn -> {
            val current = selected
            if (current == null) {
                V021MainShell(
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
                        selected = null
                        sessionState = V021SessionState.SignedOut
                    }
                )
            } else {
                V021RequestDetail(
                    request = current,
                    repository = repository,
                    onBack = { selected = null },
                    onSaved = { updated ->
                        requests = requests.map { if (it.id == updated.id) updated else it }
                        selected = updated
                    },
                    onSessionExpired = {
                        repository.clearSession()
                        requests = emptyList()
                        selected = null
                        sessionState = V021SessionState.SignedOut
                    }
                )
            }
        }
    }
}

@Composable
private fun V021Splash() {
    Box(Modifier.fillMaxSize().background(VEmberDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(70.dp).background(VEmber, CircleShape), contentAlignment = Alignment.Center) {
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
private fun V021Login(
    initialEmail: String,
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(VCream)
            .statusBarsPadding()
            .imePadding()
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(62.dp).background(VEmber, CircleShape), contentAlignment = Alignment.Center) {
                    Text("🔥", fontSize = 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("OAK & EMBER", color = VEmberDark, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Private service manager", color = VInk.copy(alpha = .55f), fontSize = 13.sp)
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
                    Text(error, color = VDanger, fontSize = 13.sp)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onLogin(email.trim(), password) },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VEmber)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Sign in")
                }
                Spacer(Modifier.height(12.dp))
                Text("Connected to the live Oak & Ember database", color = VMoss, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V021MainShell(
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
        containerColor = VCream,
        topBar = { V021Header(onRefresh, onLogout, loading) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val nav = listOf(
                    Triple("⌂", "Home", 0),
                    Triple("☰", "Requests", 1),
                    Triple("◷", "Calendar", 2),
                    Triple("♙", "Clients", 3)
                )
                nav.forEach { (icon, label, index) ->
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
                0 -> V021Dashboard(requests, lastSync, onOpen)
                1 -> V021Requests(requests, onOpen)
                2 -> V021Calendar(requests, onOpen)
                3 -> V021Clients(requests, onOpen)
            }
            if (!error.isNullOrBlank()) V021ErrorBanner(error, Modifier.align(Alignment.BottomCenter))
            if (loading && requests.isEmpty()) {
                Box(Modifier.fillMaxSize().background(VCream.copy(alpha = .88f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VEmber)
                }
            }
        }
    }
}

@Composable
private fun V021Header(onRefresh: () -> Unit, onLogout: () -> Unit, loading: Boolean) {
    Surface(color = VEmberDark, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).background(VEmber, CircleShape), contentAlignment = Alignment.Center) {
                Text("🔥", fontSize = 21.sp)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text("OAK & EMBER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = .8.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = Color(0xFF79C58A), fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("LIVE DATABASE", color = Color.White.copy(alpha = .72f), fontSize = 9.sp, letterSpacing = .6.sp)
                }
            }
            TextButton(onClick = onRefresh, enabled = !loading, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                Text(if (loading) "…" else "Refresh", color = Color.White, fontSize = 12.sp)
            }
            TextButton(onClick = onLogout, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                Text("Sign out", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V021Dashboard(requests: List<OakRequest>, lastSync: Long?, onOpen: (OakRequest) -> Unit) {
    val newCount = requests.count { it.status == "Nueva" }
    val activeCount = requests.count { it.status != "Cerrada" }
    val closedCount = requests.count { it.status == "Cerrada" }
    val overdue = requests.count(::isOverdue)
    val unscheduled = requests.count { it.status != "Cerrada" && it.scheduledDate().isBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Service manager", color = VOak, fontSize = 14.sp)
            Text("Today's overview", color = VInk, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(
                lastSync?.let { "Live · synced ${v021SyncTime(it)} · auto-refresh 30s" }
                    ?: "Connecting to live data…",
                color = VInk.copy(alpha = .55f),
                fontSize = 12.sp
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V021Metric(newCount.toString(), "New", VEmber, Modifier.weight(1f))
                V021Metric(activeCount.toString(), "Active", VMoss, Modifier.weight(1f))
                V021Metric(closedCount.toString(), "Closed", VOak, Modifier.weight(1f))
            }
        }
        if (overdue > 0 || unscheduled > 0) {
            item { V021AttentionCard(overdue, unscheduled) }
        }
        item { V021SectionTitle("Newest requests") }
        if (requests.isEmpty()) item { V021EmptyCard("No live requests yet.") }
        else items(requests.take(5), key = { it.id }) { V021RequestCard(it, onOpen) }
    }
}

@Composable
private fun V021AttentionCard(overdue: Int, unscheduled: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E8)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Needs attention", color = VEmberDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val parts = buildList {
                if (overdue > 0) add("$overdue overdue")
                if (unscheduled > 0) add("$unscheduled unscheduled")
            }
            Text(parts.joinToString(" · "), color = VInk.copy(alpha = .65f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun V021Requests(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
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

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Requests", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = VInk)
            Text("${requests.size} live service request${if (requests.size == 1) "" else "s"}", color = VInk.copy(alpha = .55f))
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
        if (filtered.isEmpty()) item { V021EmptyCard("No requests match this view.") }
        items(filtered, key = { it.id }) { V021RequestCard(it, onOpen) }
    }
}

@Composable
private fun V021Calendar(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
    val scheduled = remember(requests) {
        requests.filter { it.scheduledDate().isNotBlank() && it.status != "Cerrada" }
            .sortedWith(compareBy<OakRequest> { it.scheduledDate() }.thenBy { it.scheduledTime() })
    }
    val unscheduled = remember(requests) {
        requests.filter { it.scheduledDate().isBlank() && it.status != "Cerrada" }
    }
    val groups = remember(scheduled) {
        scheduled.groupBy { it.scheduledDate() }.toSortedMap().map { it.key to it.value }
    }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val unscheduledKey = "__unscheduled__"
    val todayKey = calendarToday().toString()

    LaunchedEffect(groups.map { it.first }, unscheduled.size) {
        val validKeys = groups.map { it.first }.toSet() + unscheduledKey
        expanded.keys.filter { it !in validKeys }.forEach { expanded.remove(it) }
        if (expanded.values.none { it }) {
            when {
                groups.any { it.first == todayKey } -> expanded[todayKey] = true
                groups.isNotEmpty() -> expanded[groups.first().first] = true
                unscheduled.isNotEmpty() -> expanded[unscheduledKey] = true
            }
        }
    }

    val todayCount = scheduled.count(::isToday)
    val overdueCount = scheduled.count(::isOverdue)

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Calendar", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = VInk)
            Text("Tap a day to open or close its jobs", color = VInk.copy(alpha = .55f))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V021MiniMetric(todayCount, "Today", VMoss, Modifier.weight(1f))
                V021MiniMetric(overdueCount, "Overdue", VDanger, Modifier.weight(1f))
                V021MiniMetric(unscheduled.size, "Unscheduled", VOak, Modifier.weight(1f))
            }
        }

        if (groups.isEmpty() && unscheduled.isEmpty()) {
            item { V021EmptyCard("No open jobs are scheduled yet.") }
        }

        groups.forEach { (date, rows) ->
            val day = parseScheduledDate(date)
            val isTodayGroup = day == calendarToday()
            val isOverdueGroup = day?.isBefore(calendarToday()) == true && rows.any(::isOverdue)
            item(key = "head-$date") {
                CollapsibleDayHeader(
                    title = calendarDayLabel(date),
                    count = rows.size,
                    expanded = expanded[date] == true,
                    overdue = isOverdueGroup,
                    today = isTodayGroup,
                    onToggle = { expanded[date] = !(expanded[date] ?: false) }
                )
            }
            if (expanded[date] == true) {
                items(rows, key = { "cal-${it.id}" }) { request ->
                    V021RequestCard(request, onOpen, showSchedule = true)
                }
            }
        }

        if (unscheduled.isNotEmpty()) {
            item(key = "head-unscheduled") {
                CollapsibleDayHeader(
                    title = "UNSCHEDULED",
                    count = unscheduled.size,
                    expanded = expanded[unscheduledKey] == true,
                    onToggle = { expanded[unscheduledKey] = !(expanded[unscheduledKey] ?: false) }
                )
            }
            if (expanded[unscheduledKey] == true) {
                items(unscheduled, key = { "uns-${it.id}" }) { request ->
                    V021RequestCard(request, onOpen, showSchedule = true)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun V021Clients(requests: List<OakRequest>, onOpen: (OakRequest) -> Unit) {
    val groups = remember(requests) {
        requests.groupBy { it.customerKey() }
            .values
            .sortedByDescending { group -> group.maxOfOrNull { it.createdAt } ?: "" }
    }

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Clients", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = VInk)
            Text("Derived from real service requests", color = VInk.copy(alpha = .55f))
        }
        if (groups.isEmpty()) item { V021EmptyCard("No clients yet.") }
        items(groups, key = { it.first().customerKey() }) { group ->
            val latest = group.maxByOrNull { it.createdAt } ?: group.first()
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(latest) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(VSoftGray, CircleShape), contentAlignment = Alignment.Center) {
                        Text(latest.payload.name.take(1).ifBlank { "?" }, color = VEmberDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(Modifier.padding(start = 13.dp).weight(1f)) {
                        Text(latest.payload.name.ifBlank { "Unnamed client" }, color = VInk, fontWeight = FontWeight.SemiBold)
                        Text(serviceLabel(latest.payload.service), color = VEmberDark, fontSize = 12.sp)
                        Text(
                            latest.payload.phone.ifBlank { latest.payload.email.ifBlank { latest.payload.zip } },
                            color = VInk.copy(alpha = .55f),
                            fontSize = 12.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${group.size} job${if (group.size == 1) "" else "s"}", color = VOak, fontSize = 12.sp)
                        if (latest.status != "Cerrada") Text(statusLabel(latest.status), color = VMoss, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V021RequestCard(request: OakRequest, onOpen: (OakRequest) -> Unit, showSchedule: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(request) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V021StatusPill(request.status)
                Spacer(Modifier.weight(1f))
                Text(request.id.take(8).uppercase(Locale.US), color = VInk.copy(alpha = .38f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(request.payload.name.ifBlank { "Unnamed client" }, color = VInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(serviceLabel(request.payload.service), color = VEmberDark, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            val location = listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(" · ")
            if (location.isNotBlank()) Text("📍 $location", color = VInk.copy(alpha = .62f), fontSize = 13.sp)
            if (showSchedule) {
                val schedule = if (request.scheduledDate().isBlank()) {
                    "Unscheduled"
                } else {
                    listOf(calendarDayLabel(request.scheduledDate()), request.scheduledTime()).filter { it.isNotBlank() }.joinToString(" · ")
                }
                Text("◷ $schedule", color = if (isOverdue(request)) VDanger else VInk.copy(alpha = .62f), fontSize = 12.sp)
            } else {
                Text("◷ ${request.scheduledTime().ifBlank { "Flexible" }}", color = VInk.copy(alpha = .62f), fontSize = 13.sp)
            }
            if (request.photos.isNotEmpty()) Text("📷 ${request.photos.size} photo${if (request.photos.size == 1) "" else "s"}", color = VInk.copy(alpha = .62f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                request.payload.details.ifBlank { request.notes.ifBlank { "No additional details." } },
                color = VInk.copy(alpha = .58f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun V021RequestDetail(
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
        containerColor = VCream,
        topBar = {
            Surface(color = VEmberDark, shadowElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("‹ Back", color = Color.White, fontSize = 15.sp) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SERVICE REQUEST", color = Color.White.copy(alpha = .68f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text(request.id.take(8).uppercase(Locale.US), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(60.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(request.payload.name.ifBlank { "Unnamed client" }, color = VInk, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(serviceLabel(request.payload.service), color = VEmberDark, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(formatCreatedAt(request.createdAt), color = VInk.copy(alpha = .5f), fontSize = 12.sp)
            }
            item {
                V021SectionCard("Status") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allowedStatuses) { option ->
                            FilterChip(selected = status == option, onClick = { status = option }, label = { Text(statusLabel(option)) })
                        }
                    }
                }
            }
            item {
                V021SectionCard("Contact") {
                    V021DetailLine("Phone", request.payload.phone.ifBlank { "—" })
                    V021DetailLine("Email", request.payload.email.ifBlank { "—" })
                    V021DetailLine("Preferred", request.payload.contact.ifBlank { "—" })
                    V021DetailLine("Address", listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "—" })
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (request.payload.phone.isNotBlank()) item {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.payload.phone}"))) }) { Text("Call") }
                        }
                        if (request.payload.email.isNotBlank()) item {
                            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${request.payload.email}"))) }) { Text("Email") }
                        }
                        if (request.payload.address.isNotBlank() || request.payload.zip.isNotBlank()) item {
                            OutlinedButton(onClick = {
                                val destination = listOf(request.payload.address, request.payload.zip).filter { it.isNotBlank() }.joinToString(" ")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination))))
                            }) { Text("Map") }
                        }
                    }
                }
            }
            item {
                V021SectionCard("Customer request") {
                    V021DetailLine("Service", serviceLabel(request.payload.service))
                    V021DetailLine("Appliance", request.payload.appliance.ifBlank { "Not sure" })
                    if (request.payload.reason.isNotBlank()) V021DetailLine("Reason", request.payload.reason)
                    V021DetailLine("Best time", request.payload.time.ifBlank { "Flexible" })
                    Spacer(Modifier.height(8.dp))
                    Text(request.payload.details.ifBlank { "No description provided." }, color = VInk, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            item {
                V021SectionCard("Schedule") {
                    if (request.scheduledDate().isBlank()) {
                        Text("Not scheduled yet", color = VOak, fontWeight = FontWeight.SemiBold)
                    } else {
                        V021DetailLine("Date", calendarDayLabel(request.scheduledDate()))
                        V021DetailLine("Time", request.scheduledTime().ifBlank { "Flexible" })
                        if (isOverdue(request)) Text("This visit is overdue.", color = VDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (request.photos.isNotEmpty()) {
                item {
                    V021SectionCard("Photos (${request.photos.size})") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(request.photos, key = { it.path }) { photo ->
                                V021AuthenticatedPhoto(
                                    repository = repository,
                                    photo = photo,
                                    modifier = Modifier.size(118.dp).clip(RoundedCornerShape(14.dp)).clickable { selectedPhoto = photo }
                                )
                            }
                        }
                    }
                }
            }
            if (request.quote != null) {
                item {
                    V021SectionCard("Quote") {
                        val q = request.quote
                        val total = sequenceOf("total", "amount", "price").map { q.opt(it)?.toString().orEmpty() }.firstOrNull { it.isNotBlank() }.orEmpty()
                        val description = sequenceOf("description", "details", "notes").map { q.optString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
                        if (total.isNotBlank()) V021DetailLine("Total", total)
                        if (description.isNotBlank()) Text(description, color = VInk, fontSize = 14.sp)
                        if (total.isBlank() && description.isBlank()) Text("Quote information is attached to this request.", color = VInk.copy(alpha = .6f), fontSize = 13.sp)
                    }
                }
            }
            item {
                V021SectionCard("Internal notes") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 10_000) notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Notes for the team") }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Only operators can read these notes.", color = VInk.copy(alpha = .5f), fontSize = 11.sp)
                }
            }
            if (!message.isNullOrBlank()) item {
                Text(message!!, color = if (message!!.startsWith("Saved")) VMoss else VDanger, fontSize = 13.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = VEmber)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Save changes")
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    selectedPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            confirmButton = { TextButton(onClick = { selectedPhoto = null }) { Text("Close") } },
            text = {
                V021AuthenticatedPhoto(
                    repository = repository,
                    photo = photo,
                    modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(14.dp))
                )
            }
        )
    }
}

@Composable
private fun V021AuthenticatedPhoto(repository: OakRepository, photo: PhotoRef, modifier: Modifier = Modifier) {
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
    Box(modifier.background(VSoftGray), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap = bitmap!!, contentDescription = "Customer photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            failed -> Text("Photo unavailable", color = VInk.copy(alpha = .5f), fontSize = 11.sp)
            else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = VEmber)
        }
    }
}

@Composable
private fun V021SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = VInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun V021DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = VInk.copy(alpha = .5f), fontSize = 12.sp, modifier = Modifier.width(86.dp))
        Text(value, color = VInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun V021Metric(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VInk)
            Text(label, color = VInk.copy(alpha = .6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun V021MiniMetric(value: Int, label: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(accent, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(value.toString(), color = VInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(label, color = VInk.copy(alpha = .55f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun V021StatusPill(status: String) {
    val background = when (status) {
        "Nueva" -> Color(0xFFFFE6DC)
        "Por contactar" -> Color(0xFFFFEDCB)
        "Esperando información" -> Color(0xFFE7E3F2)
        "Presupuesto enviado" -> Color(0xFFE1ECF5)
        "Aceptada" -> Color(0xFFDDEDE1)
        "Cerrada" -> VSoftGray
        else -> VSoftGray
    }
    val foreground = when (status) {
        "Nueva" -> VEmber
        "Aceptada" -> VMoss
        "Cerrada" -> VInk.copy(alpha = .55f)
        else -> VEmberDark
    }
    Box(Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(statusLabel(status), color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V021SectionTitle(title: String) {
    Text(title, color = VInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun V021EmptyCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(18.dp), color = VInk.copy(alpha = .58f), fontSize = 13.sp)
    }
}

@Composable
private fun V021ErrorBanner(text: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE7E2)), shape = RoundedCornerShape(14.dp)) {
        Text(text, Modifier.padding(12.dp), color = VDanger, fontSize = 12.sp)
    }
}

private fun v021SyncTime(value: Long): String = try {
    DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        .format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
} catch (_: Exception) {
    "now"
}
