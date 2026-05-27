package com.example.myapplication

import android.R.attr.dialogTitle
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.myapplication.ui.theme.MyApplicationTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.github.jan.supabase.exceptions.RestException

class StudyScreen : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            StudySetDatabase::class.java,
            "studyset_database"
        )            .fallbackToDestructiveMigration()
            .build()

        setContent {
            MyApplicationTheme {
                var showAddStudySet by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                var studysets by remember {
                    mutableStateOf<List<StudySet>>(emptyList())
                }

                var studysetssupa by remember {
                    mutableStateOf<List<OnlineStudySet>>(emptyList())
                }

                // Load exams once
                LaunchedEffect(Unit) {
                    studysets = db.studySetDao().getAll()

                    try {
                        studysetssupa = SupabaseManager.client
                            .from("studysets")
                            .select()
                            .decodeList<OnlineStudySet>()

                    } catch(e: Exception) {
                        println(e.message)
                    }
                }


                Scaffold() {
                    Column() {
                        if(showAddStudySet) {
                            DisplayAddStudySet(
                                onAdd = { studyset, terms, definitions ->
                                    scope.launch {
                                        // insert the studyset in local db
                                        val localId = db.studySetDao().insert(studyset)

                                        try {
                                            // 1. Insert the parent study set using a simple String-only map (this works fine)
                                            val inserted = SupabaseManager.client
                                                .from("studysets")
                                                .insert(
                                                    mapOf(
                                                        "name" to studyset.name,
                                                        "subject" to studyset.subject,
                                                        "description" to studyset.description
                                                    )
                                                ) { select() }
                                                .decodeSingle<OnlineStudySet>()

                                            // 2. Get the generated ID from Supabase
                                            val studysetId = inserted.id!!

                                            // 3. Convert your terms and definitions into a list of your serializable data class
                                            val flashcardsData = terms.zip(definitions)
                                                .filter { (term, def) -> term.isNotBlank() && def.isNotBlank() }
                                                .map { (term, def) ->
                                                    OnlineStudySetConn(
                                                        term = term,
                                                        definition = def,
                                                        studyset_id = studysetId
                                                    )
                                                }

                                            // 4. Send the whole list to Supabase in ONE efficient network call
                                            if (flashcardsData.isNotEmpty()) {
                                                SupabaseManager.client
                                                    .from("flashcards")
                                                    .insert(flashcardsData)

                                                println("Inmatning lyckades!")
                                            }

                                            // Refresh your online state list
                                            studysetssupa = SupabaseManager.client
                                                .from("studysets")
                                                .select()
                                                .decodeList()

                                        } catch (e: Exception) {
                                            // This will catch any remaining database or schema issues
                                            println("Något oväntat gick fel: ${e.message}")
                                            e.printStackTrace()
                                        }

                                        studysets = db.studySetDao().getAll()
                                        showAddStudySet = false

                                        // insert terms and definitions into local db
                                        if(terms.size == definitions.size) {
                                            repeat(terms.size) { index ->
                                                db.studySetConnDao().insert(localId.toInt(), terms[index], definitions[index])
                                            }
                                        }
                                    }
                                }
                            )
//                            DisplayAddStudySet(
//                                onAdd = { studyset, terms, definitions ->
//                                    scope.launch {
//                                        val localId = db.studySetDao().insert(studyset)
//
//                                        try {
//                                            val inserted = SupabaseManager.client
//                                                .from("studysets")
//                                                .insert(
//                                                    mapOf(
//                                                        "name" to studyset.name,
//                                                        "subject" to studyset.subject,
//                                                        "description" to studyset.description
//                                                    )
//                                                ) { select() }
//                                                .decodeSingle<OnlineStudySet>()
//
//                                            val studysetId = inserted.id!!
//
//                                            // FIXED: Map directly to your @Serializable data class instead of a Map<String, Any>
//                                            val flashcardsData = terms.zip(definitions)
//                                                .filter { (term, def) -> term.isNotBlank() && def.isNotBlank() }
//                                                .map { (term, def) ->
//                                                    OnlineStudySetConn(
//                                                        term = term,
//                                                        definition = def,
//                                                        studysetid = studysetId // This matches your Postgres column name 'studysetid'
//                                                    )
//                                                }
//
//                                            // Safely bulk-insert the strongly-typed objects
//                                            if (flashcardsData.isNotEmpty()) {
//                                                SupabaseManager.client
//                                                    .from("flashcards") // Make sure your Supabase table is named exactly "flashcards"
//                                                    .insert(flashcardsData)
//                                            }
//
//                                            // Refresh online list
//                                            studysetssupa = SupabaseManager.client
//                                                .from("studysets")
//                                                .select()
//                                                .decodeList()
//
//                                        } catch (e: Exception) {
//                                            // This will now catch and print explicit database or network issues if they occur
//                                            android.util.Log.e("SupabaseError", "Failed to insert flashcards: ${e.message}", e)
//                                            e.printStackTrace()
//                                        }
//
//                                        // 3. Save clean rows to Room Local DB
//                                        val validPairs = terms.zip(definitions).filter { it.first.isNotBlank() && it.second.isNotBlank() }
//                                        validPairs.forEach { (term, def) ->
//                                            db.studySetConnDao().insert(localId.toInt(), term, def)
//                                        }
//
//                                        studysets = db.studySetDao().getAll()
//                                        showAddStudySet = false
//                                    }
//                                }
//                            )
                        }
                        else {
                            Button(onClick = {
                                showAddStudySet = !showAddStudySet
                            }) { Text("Lägg till") }
                            DisplayStudySets(
                                studysets,
                                studysetssupa,
                                onOpen = { studyset, onlinestudyset ->

                                    if(studyset != null){
                                        val intent = Intent(
                                            this@StudyScreen,
                                            StudySetScreen::class.java
                                        )

                                        intent.putExtra("studyset_id", studyset.id)

                                        startActivity(intent)
                                    }
                                    else if(onlinestudyset != null){
                                        val intent = Intent(
                                            this@StudyScreen,
                                            StudySetScreen::class.java
                                        )

                                        intent.putExtra("studyset_id", onlinestudyset.id)

                                        startActivity(intent)
                                    }

                                },
                                onDelete = { studyset, onlinestudyset ->
                                    scope.launch {
                                        if (studyset != null) {
                                            db.studySetDao().delete(studyset.id)
                                            studysets = db.studySetDao().getAll()
                                        } else if (onlinestudyset != null) {
                                            try {
                                                SupabaseManager.client
                                                    .from("studysets")
                                                    .delete {
                                                        filter {
                                                            eq("id", onlinestudyset.id!!)
                                                        }
                                                    }

                                                // refresh list after delete
                                                studysetssupa = SupabaseManager.client
                                                    .from("studysets")
                                                    .select()
                                                    .decodeList()

                                            } catch (e: Exception) {
                                                println("Delete failed: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DisplayAddStudySet(onAdd: (StudySet, List<String>, List<String>) -> Unit){
//    var title by remember { mutableStateOf("") }
//    var subject by remember { mutableStateOf("") }
//    var description by remember { mutableStateOf("") }
//    val subjects = listOf(
//        "Matte",
//        "Fysik",
//        "Programmering"
//    )
//    var expanded by remember { mutableStateOf(false) }
//    var numberOfTerms by remember { mutableStateOf(0) }
//
//    val listOfTerms = remember { mutableStateListOf(String())}
//    val listOfDefinitions = remember { mutableStateListOf(String())}
//
//    Column(
//        modifier = Modifier.verticalScroll(
//        rememberScrollState()
//    )) {
//        Card() {
//            //region add study set inputs
//            OutlinedTextField(
//                value = title,
//                onValueChange = { title = it },
//                label = { Text("Namn") },
//                modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)
//            )
//
//            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.padding(bottom = 30.dp, end = 100.dp)) {
//                OutlinedTextField(
//                    value = subject,
//                    onValueChange = {},
//                    readOnly = true,
//                    label = {
//                        Text("Ämne")
//                    },
//                    modifier = Modifier
//                        .menuAnchor()
//                        .fillMaxWidth(),
//                    trailingIcon = {
//                        ExposedDropdownMenuDefaults.TrailingIcon(
//                            expanded = expanded
//                        )
//                    }
//                )
//
//                ExposedDropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = {
//                        expanded = false
//                    }
//                ) {
//
//                    subjects.forEach { subjectitem ->
//
//                        DropdownMenuItem(
//                            text = {
//                                Text(subjectitem)
//                            },
//                            onClick = {
//
//                                subject = subjectitem
//                                expanded = false
//                            }
//                        )
//                    }
//                }
//            }
//
//            OutlinedTextField(
//                value = description,
//                onValueChange = { description = it },
//                label = {Text("Beskrivning")},
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(150.dp),
//                placeholder = { Text("Skriv en beskrivning...") },
//                maxLines = 10
//            )
//            //endregion
//        }
//        //region add terms and definitions
//
//        Text("Termer", style = MaterialTheme.typography.headlineSmall)
//
//        Column(){
//            repeat(numberOfTerms) { index ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 10.dp, vertical = 8.dp),
//                    elevation = CardDefaults.cardElevation(
//                        defaultElevation = 4.dp
//                    )
//                ) {
//                    Column() {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            OutlinedTextField(
//                                modifier = Modifier.weight(1f),
//                                value = listOfTerms[index],
//                                onValueChange = { listOfTerms[index] = it },
//                                label = {Text("Term")}
//                            )
//                            OutlinedTextField(
//                                modifier = Modifier.weight(1f),
//                                value = listOfDefinitions[index],
//                                onValueChange = { listOfDefinitions[index] = it },
//                                label = {Text("Defenition")}
//                            )
//                        }
//                        IconButton(onClick = {
//                            listOfTerms.removeAt(index)
//                            listOfDefinitions.removeAt(index)
//                            numberOfTerms--
//                        }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")}
//                    }
//                }
//            }
//            Card(onClick = {
//                listOfTerms.add("")
//                listOfDefinitions.add("")
//                numberOfTerms++
//            }) { Text("Lägg till en ny term")}
//            Button(onClick = {
//                onAdd(
//                    StudySet(
//                        name = title,
//                        subject = subject,
//                        description = description
//                    ),
//                    listOfTerms,
//                    listOfDefinitions
//                )
//            }) {
//                Text("Spara")
//            }
//        }
//        //endregion
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayAddStudySet(onAdd: (StudySet, List<String>, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val subjects = listOf("Matte", "Fysik", "Programmering")
    var expanded by remember { mutableStateOf(false) }

    // Start with completely clean, empty state lists
    val listOfTerms = remember { mutableStateListOf<String>() }
    val listOfDefinitions = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Card() {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Namn") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(bottom = 30.dp, end = 100.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ämne") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    subjects.forEach { subjectitem ->
                        DropdownMenuItem(
                            text = { Text(subjectitem) },
                            onClick = {
                                subject = subjectitem
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beskrivning") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Skriv en beskrivning...") },
                maxLines = 10
            )
        }

        Text("Termer", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))

        Column {
            // Controlled explicitly by the size of the list state
            repeat(listOfTerms.size) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).padding(end = 4.0.dp),
                                value = listOfTerms[index],
                                onValueChange = { listOfTerms[index] = it },
                                label = { Text("Term") }
                            )
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).padding(start = 4.0.dp),
                                value = listOfDefinitions[index],
                                onValueChange = { listOfDefinitions[index] = it },
                                label = { Text("Definition") }
                            )
                        }
                        IconButton(onClick = {
                            listOfTerms.removeAt(index)
                            listOfDefinitions.removeAt(index)
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                onClick = {
                    listOfTerms.add("")
                    listOfDefinitions.add("")
                }
            ) {
                Text("Lägg till en ny term", modifier = Modifier.padding(16.dp))
            }

            Button(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                onClick = {
                    onAdd(
                        StudySet(name = title, subject = subject, description = description),
                        listOfTerms,
                        listOfDefinitions
                    )
                }
            ) {
                Text("Spara")
            }
        }
    }
}

@Composable
fun DisplayStudySets(studysets: List<StudySet>, supastudysets: List<OnlineStudySet>, onOpen: (StudySet?, OnlineStudySet?) -> Unit, onDelete: (StudySet?, OnlineStudySet?) -> Unit) {

    val useLocal = supastudysets.isEmpty()

    LazyColumn {

        if(!useLocal) {
            items(supastudysets) { studyset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {
                        onOpen(null, studyset)
                    }
                ) {
                    Row() {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = studyset.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = studyset.subject,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = {
                                onDelete(null, studyset)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            }
        }
        else {
            items(studysets) { studyset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {
                        onOpen(studyset, null)
                    }
                ) {
                    Row {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = studyset.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = studyset.subject,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        IconButton(
                            onClick = {
                                onDelete(studyset, null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            }
        }
    }
}
