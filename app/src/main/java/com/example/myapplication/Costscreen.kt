package com.example.myapplication

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.window.Dialog
import java.time.YearMonth

class Costscreen : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            CostDataBase::class.java,
            "cost_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        enableEdgeToEdge()

        setContent {

            MyApplicationTheme {
                var showAddCost by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                var costs by remember {
                    mutableStateOf<List<MonthCost>>(emptyList())
                }


                // Load costs once
                LaunchedEffect(Unit) {
                    costs = db.costDao().getAll()
                }

                // call db and functions
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {

                        Button(
                            onClick = { showAddCost = true },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text("Lägg till utgift")
                        }

                        if (showAddCost) {
                            DisplayAddCost(
                                onAdd = { cost ->
                                    scope.launch {
                                        db.costDao().insert(cost)
                                        costs = db.costDao().getAll()
                                    }
                                    showAddCost = false
                                },
                                onClose = {
                                    showAddCost = false
                                }
                            )
                        }

                        if (costs.isNotEmpty()) {
                            CostList(
                                costs = costs,
                                onDelete = { cost ->
                                    scope.launch {
                                        db.costDao().delete(cost.id)
                                        costs = db.costDao().getAll()
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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayAddCost(onAdd: (MonthCost) -> Unit, onClose: () -> Unit){
    var name by remember {
        mutableStateOf("")
    }
    var cost by remember {
        mutableStateOf("")
    }
    var expanded by remember {
        mutableStateOf(false)
    }


    Dialog(onDismissRequest = { onClose() }){
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Box(
                modifier = Modifier.padding(20.dp)
            ) {

                IconButton(onClick = { onClose() }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("✕")
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {

                    Text(
                        text = "Lägg till kostnad",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Cost name
                    TextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = {
                            Text("Kostnadnamn")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cost price
                    TextField(
                        value = cost,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                cost = newValue
                            }
                        },
                        label = {
                            Text("Pris")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))


                    Spacer(modifier = Modifier.height(16.dp))


                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onAdd(
                                MonthCost(
                                    name = name,
                                    cost = cost.toIntOrNull() ?: 0
                                )
                            )
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lägg till kostnad")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CostList(costs: List<MonthCost>, onDelete: (MonthCost) -> Unit, modifier: Modifier = Modifier){
    var openMonth by remember {
        mutableStateOf<YearMonth?>(null)
    }

    val costsByMonth = costs
        .groupBy {
            YearMonth.from(it.date)
        }
        .toSortedMap(reverseOrder())


    LazyColumn(
        modifier = modifier
    ) {
        costsByMonth.forEach { (month, monthCosts) ->

            val totalCost = monthCosts.sumOf { it.cost }

            // Month
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {
                        openMonth = if (openMonth == month) {
                            null
                        } else {
                            month
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${month.month} ${month.year}",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "Totalt: $totalCost kr"
                        )
                    }
                }
            }

            // Costs belonging to this month
            if (openMonth == month) {
                monthCosts.forEach { cost ->

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 24.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cost.name,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Text(
                                    text = "${cost.cost} kr • ${cost.date}"
                                )

                                Button(
                                    onClick = {
                                        onDelete(cost)
                                    }
                                ) {
                                    Text("Ta Bort")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}