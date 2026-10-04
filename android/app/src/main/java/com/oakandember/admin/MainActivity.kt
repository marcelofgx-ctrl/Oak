package com.oakandember.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ember = Color(0xFFB14C24)
private val EmberDark = Color(0xFF5A291A)
private val Oak = Color(0xFF7B5A3C)
private val Cream = Color(0xFFF7F1E8)
private val Ink = Color(0xFF2C2621)
private val Moss = Color(0xFF4F6A55)
private val SoftGray = Color(0xFFF1EEE9)

data class ServiceRequest(
    val id: String,
    val customer: String,
    val service: String,
    val city: String,
    val phone: String,
    val whenText: String,
    val status: String,
    val notes: String
)

data class Client(
    val name: String,
    val city: String,
    val phone: String,
    val jobs: Int
)

private val demoRequests = listOf(
    ServiceRequest("OE-1042", "Emily Carter", "Chimney inspection", "Marietta, GA", "+1 770 555 0142", "Today · 10:30 AM", "NEW", "Customer reports smoke backing into the room."),
    ServiceRequest("OE-1041", "James Miller", "Fireplace repair", "Roswell, GA", "+1 678 555 0187", "Today · 2:00 PM", "SCHEDULED", "Gas fireplace ignition is intermittent."),
    ServiceRequest("OE-1040", "Sarah Thompson", "Chimney cleaning", "Alpharetta, GA", "+1 404 555 0194", "Mon · 9:00 AM", "QUOTE SENT", "Annual cleaning and cap inspection."),
    ServiceRequest("OE-1039", "Robert Wilson", "Chimney cap", "Sandy Springs, GA", "+1 470 555 0108", "Tue · 11:30 AM", "APPROVED", "Replace damaged cap after storm.")
)

private val demoClients = listOf(
    Client("Emily Carter", "Marietta, GA", "+1 770 555 0142", 1),
    Client("James Miller", "Roswell, GA", "+1 678 555 0187", 2),
    Client("Sarah Thompson", "Alpharetta, GA", "+1 404 555 0194", 3),
    Client("Robert Wilson", "Sandy Springs, GA", "+1 470 555 0108", 1)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OakEmberApp()
            }
        }
    }
}

@Composable
fun OakEmberApp() {
    var tab by remember { mutableIntStateOf(0) }
    var selectedRequest by remember { mutableStateOf<ServiceRequest?>(null) }

    Scaffold(
        containerColor = Cream,
        topBar = { BrandHeader() },
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
                        onClick = { tab = index },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = Ember,
                    contentColor = Color.White
                ) { Text("+", fontSize = 28.sp) }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> Dashboard(onRequestClick = { selectedRequest = it })
                1 -> RequestsScreen(onRequestClick = { selectedRequest = it })
                2 -> CalendarScreen(onRequestClick = { selectedRequest = it })
                3 -> ClientsScreen()
            }
        }
    }

    selectedRequest?.let { request ->
        RequestDialog(request = request, onDismiss = { selectedRequest = null })
    }
}

@Composable
private fun BrandHeader() {
    Surface(color = EmberDark, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Ember, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 21.sp)
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "OAK & EMBER",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = 1.sp
                )
                Text("Field & service manager", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("●", color = Color(0xFF79C58A), fontSize = 16.sp)
        }
    }
}

@Composable
private fun Dashboard(onRequestClick: (ServiceRequest) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Good morning", color = Oak, fontSize = 14.sp)
            Text("Today's work", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Native Android prototype · local demo data", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("3", "New", Ember, Modifier.weight(1f))
                MetricCard("2", "Today", Moss, Modifier.weight(1f))
                MetricCard("2", "Quotes", Oak, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Priority requests") }
        items(demoRequests.take(3)) { request ->
            RequestCard(request, onRequestClick)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmberDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("This week", color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
                    Text("$2,840 estimated", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("4 active jobs · 2 quotes awaiting approval", color = Color.White.copy(alpha = .72f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(label, color = Ink.copy(alpha = .6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun RequestsScreen(onRequestClick: (ServiceRequest) -> Unit) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Requests", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("All current service requests", color = Ink.copy(alpha = .55f))
        }
        items(demoRequests) { request -> RequestCard(request, onRequestClick) }
    }
}

@Composable
private fun CalendarScreen(onRequestClick: (ServiceRequest) -> Unit) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Calendar", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Upcoming visits and jobs", color = Ink.copy(alpha = .55f))
        }
        item { DayHeader("TODAY", "2 visits") }
        items(demoRequests.take(2)) { RequestCard(it, onRequestClick) }
        item { DayHeader("MONDAY", "1 visit") }
        item { RequestCard(demoRequests[2], onRequestClick) }
        item { DayHeader("TUESDAY", "1 visit") }
        item { RequestCard(demoRequests[3], onRequestClick) }
    }
}

@Composable
private fun ClientsScreen() {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Clients", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Customer history at a glance", color = Ink.copy(alpha = .55f))
        }
        items(demoClients) { client ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).background(SoftGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(client.name.take(1), color = EmberDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(Modifier.padding(start = 13.dp).weight(1f)) {
                        Text(client.name, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(client.city, color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                    }
                    Text("${client.jobs} job${if (client.jobs == 1) "" else "s"}", color = Oak, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RequestCard(request: ServiceRequest, onClick: (ServiceRequest) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(request) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(request.status)
                Spacer(Modifier.weight(1f))
                Text(request.id, color = Ink.copy(alpha = .38f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(request.customer, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(request.service, color = EmberDark, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("📍 ${request.city}", color = Ink.copy(alpha = .62f), fontSize = 13.sp)
            Text("🕒 ${request.whenText}", color = Ink.copy(alpha = .62f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                request.notes,
                color = Ink.copy(alpha = .58f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val background = when (status) {
        "NEW" -> Color(0xFFFFE6DC)
        "SCHEDULED" -> Color(0xFFDDEDE1)
        "QUOTE SENT" -> Color(0xFFE8E1F1)
        "APPROVED" -> Color(0xFFE1ECF5)
        else -> SoftGray
    }
    val foreground = when (status) {
        "NEW" -> Ember
        "SCHEDULED" -> Moss
        else -> EmberDark
    }
    Box(
        Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(status, color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
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
private fun SectionTitle(title: String) {
    Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun RequestDialog(request: ServiceRequest, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.customer, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                StatusPill(request.status)
                Text(request.service, color = EmberDark, fontWeight = FontWeight.SemiBold)
                Text("📍 ${request.city}")
                Text("🕒 ${request.whenText}")
                Text(request.notes, color = Ink.copy(alpha = .7f))
            }
        },
        confirmButton = {
            Button(onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.phone}")))
            }) { Text("Call") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(request.city))
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }) { Text("Map") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
