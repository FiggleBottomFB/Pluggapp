package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class StudySetScreen : ComponentActivity() {
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

        val studySetId = intent.getIntExtra("studyset_id", -1)

        setContent {
            var currentstudyset by remember {
                mutableStateOf<StudySet?>(null)
            }
            LaunchedEffect(Unit) {
                currentstudyset = db.studySetDao().getExam(studySetId)
            }

            MyApplicationTheme {
                val scope = rememberCoroutineScope()
                var playflashcard by remember {mutableStateOf(false)}
                var studysets by remember {
                    mutableStateOf<List<StudySet>>(emptyList())
                }

                val flashCardPairs = remember {
                    mutableStateListOf<Pair<String, String>>()
                }

                // Load exams once
                LaunchedEffect(Unit) {
                    studysets = db.studySetDao().getAll()

                    val result = db.studySetConnDao().getAll(studySetId)

                    flashCardPairs.clear()
                    flashCardPairs.addAll(
                        result.map {
                            it.term to it.definition
                        }
                    )
                }

                Scaffold() {
                    Column(
                        modifier = Modifier.padding(top = 20.dp, start = 20.dp)
                    ) {
                        if(!playflashcard){Button(onClick = {playflashcard = true}) { Text("Starta") }}
                        else{Button(onClick = {playflashcard = false}) { Text("Avbryt") }}

                        if(playflashcard){
                            flashcard(currentstudyset, flashCardPairs)
                        }
                        else{
                            editFlashCard(currentstudyset, flashCardPairs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun flashcard(studyset: StudySet?, pairs: List<Pair<String, String>>){
    var shuffledPairs = remember { pairs.shuffled().toMutableStateList() }
    var nextRoundPairs = remember { mutableStateListOf<Pair<String, String>>() }
    var currentTerm by remember {mutableStateOf("")}
    var startNextTerm by remember { mutableStateOf(false) }
    var answerText by remember {mutableStateOf("")}
    var answerColor by remember { mutableStateOf(Color.White) }
    var completedSet by remember { mutableStateOf(false) }

    if (!completedSet || shuffledPairs.isNotEmpty()) {


        val currentPair = shuffledPairs.first()

        Column {

            Text(text = currentPair.second)

            OutlinedTextField(
                value = currentTerm,
                onValueChange = { currentTerm = it },
                label = { Text("Term") }
            )

            if(!startNextTerm) {
                Button(
                    onClick = {
                        if (currentTerm.equals(currentPair.first, ignoreCase = true)) {
                            answerText = "Du svarade rätt"
                            answerColor = Color.Green
                        } else {
                            val wrongCard = shuffledPairs.removeAt(0)
                            nextRoundPairs.add(wrongCard)
                            answerText = "Du svarade fel"
                            answerColor = Color.Red
                        }
                        startNextTerm = true
                    }
                ) { Text("Svara") }
            }
            else{
                Text(text = answerText, color = answerColor)
                if(answerColor == Color.Red){
                    Text("Rätt svar: ${shuffledPairs[0]}")
                }
                Button(
                    onClick = {
                        shuffledPairs.removeAt(0)
                        currentTerm = ""
                        if(shuffledPairs.isEmpty()){
                            shuffledPairs = nextRoundPairs
                            nextRoundPairs.removeAll { true }
                        }
                        if(shuffledPairs.isEmpty() && nextRoundPairs.isEmpty()){
                            completedSet = true
                        }
                    }
                ) { Text("Nästa") }
            }
        }
    }

    else {

        Text("Alla kort klara 🎉")
    }
}

@Composable
fun editFlashCard(studyset: StudySet?, pairs: List<Pair<String, String>>){

}