package com.economiainteligente.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

private val Navy = Color(0xFF073B73)
private val Blue = Color(0xFF1265B3)
private val Pale = Color(0xFFEAF3FC)
private val Ink = Color(0xFF12304D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EconomiaApp() }
    }
}

data class Section(val title: String, val icon: ImageVector, val subtitle: String)

private val sections = listOf(
    Section("INTELIGENCIA 360°", Icons.Default.Public, "Mercados, economía y acontecimientos mundiales"),
    Section("LABORATORIO 360°", Icons.Default.Science, "Prueba virtual con 40 €"),
    Section("DEPÓSITOS", Icons.Default.AccountBalance, "Ahorro, intereses y seguridad"),
    Section("BONOS Y DEUDA", Icons.Default.Description, "Deuda pública y renta fija"),
    Section("FONDOS INDEXADOS", Icons.Default.ShowChart, "Invertir de forma diversificada"),
    Section("TIPOS DE INTERÉS", Icons.Default.Percent, "BCE, Fed, Euríbor e inflación"),
    Section("ESCUDO DE SEGURIDAD 360°", Icons.Default.Shield, "Privacidad y protección"),
    Section("HABLA CON CLARA", Icons.Default.Chat, "Ideas, preguntas y propuestas"),
    Section("ACTUALIZACIONES Y EVOLUCIÓN", Icons.Default.SystemUpdate, "Novedades y cambios aprobados")
)

@Composable
fun EconomiaApp() {
    val nav = rememberNavController()
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Navy, secondary = Blue, background = Pale, surface = Color.White
    )) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(onOpen = { nav.navigate("section/${it}") }) }
            composable("section/{name}", arguments = listOf(navArgument("name") { type = NavType.StringType })) {
                val name = it.arguments?.getString("name") ?: ""
                if (name == "LABORATORIO 360°") LabScreen(onBack = { nav.popBackStack() })
                else SectionScreen(name, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
fun Header(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().background(Navy).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Spacer(Modifier.width(4.dp))
        }
        Column {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("ECONOMÍA INTELIGENTE", color = Color(0xFFD6E9FF), fontSize = 11.sp)
        }
    }
}

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Pale)) {
        Header("Bienvenido")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
            Text("Aprende. Comprende. Decide.", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Tu espacio para entender la economía y cuidar tu patrimonio.", color = Ink.copy(alpha=.75f))
            Spacer(Modifier.height(14.dp))
            sections.forEach { section ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clickable { onOpen(section.title) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(Pale, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(section.icon, null, tint = Blue, modifier = Modifier.size(27.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(section.title, color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(section.subtitle, color = Ink.copy(alpha=.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Blue)
                    }
                }
            }
            Text("Versión inicial · 0.1.0 · Datos de mercado aún no conectados",
                color = Ink.copy(alpha=.6f), fontSize = 11.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun SectionScreen(name: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Pale)) {
        Header(name, onBack)
        Column(Modifier.padding(20.dp)) {
            Text("Estamos construyendo este apartado contigo.", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(when(name) {
                "ESCUDO DE SEGURIDAD 360°" -> "La seguridad será prioritaria: permisos mínimos, protección de datos y revisión antes de conectar cualquier cuenta. Esta versión no solicita credenciales ni accede a bancos."
                "HABLA CON CLARA" -> "Aquí podrás plantear dudas y revisar propuestas. Ningún cambio importante se incorporará sin tu aprobación."
                "ACTUALIZACIONES Y EVOLUCIÓN" -> "Aquí registraremos las mejoras, novedades, tareas pendientes y cambios que hayas aprobado."
                else -> "Este módulo se desarrollará paso a paso con explicaciones claras, ejemplos cotidianos y fuentes verificables."
            }, color = Ink, fontSize = 15.sp)
        }
    }
}

@Composable
fun LabScreen(onBack: () -> Unit) {
    var cash by remember { mutableStateOf(40.0) }
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Pale)) {
        Header("LABORATORIO 360°", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
            Text("MODO VIRTUAL", color = Blue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Cartera de prueba", color = Navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Capital virtual disponible", color = Ink.copy(alpha=.7f))
                    Text("€ %.2f".format(cash), color = Navy, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Capital inicial: 40,00 € · Sin dinero real", color = Ink.copy(alpha=.65f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Diario del laboratorio", color = Navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Anota hipótesis, decisiones y motivos. En esta primera versión no se ejecutan operaciones ni se inventan cotizaciones.", color = Ink)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota de seguimiento") },
                modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { note = "" }, enabled = note.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Limpiar nota")
            }
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("MODO REAL", color = Navy, fontWeight = FontWeight.Bold)
                    Text("Bloqueado por ahora. Sólo se estudiará después de revisar resultados, riesgos, costes, seguridad y tu autorización expresa.",
                        color = Ink, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Importante: este saldo es una demostración local. No está conectado a un bróker ni representa una cartera invertida.",
                color = Ink.copy(alpha=.7f), fontSize = 12.sp)
        }
    }
}
