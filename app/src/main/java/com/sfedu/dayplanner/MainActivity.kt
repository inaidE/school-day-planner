package com.sfedu.dayplanner

import com.sfedu.dayplanner.ui.theme.ThemeDayPlanner
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.json.JSONArray
import org.json.JSONObject

data class DaySchedule(val dayName: String, val subjects: List<String>)

data class TakeItem(val thing: String, val subject: String)

val allSubjects = listOf(
    "Русский язык",
    "Математика",
    "Английский",
    "География",
    "Физика",
    "Химия",
    "История",
    "Биология",
    "Литература",
    "Физкультура",
    "Информатика",
    "Музыка",
    "Искусство"
)

private const val PREFS_NAME = "school_day_planner_prefs"
private const val KEY_SCHEDULE = "schedule"
private const val KEY_CHECKLIST = "checklist"

fun defaultSchedule(): List<DaySchedule> {
    return listOf(
        DaySchedule("Понедельник", listOf("Математика", "Русский язык")),
        DaySchedule("Вторник", listOf("География", "Физика")),
        DaySchedule("Среда", listOf("Химия", "История")),
        DaySchedule("Четверг", listOf("Биология", "Литература")),
        DaySchedule("Пятница", listOf("Физкультура", "Информатика")),
        DaySchedule("Суббота", listOf("Музыка", "Искусство"))
    )
}

fun saveSchedule(context: Context, schedule: List<DaySchedule>) {
    val jsonArray = JSONArray()

    schedule.forEach { day ->
        val dayObject = JSONObject()
        dayObject.put("dayName", day.dayName)

        val subjectsArray = JSONArray()
        day.subjects.forEach { subject ->
            subjectsArray.put(subject)
        }

        dayObject.put("subjects", subjectsArray)
        jsonArray.put(dayObject)
    }

    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SCHEDULE, jsonArray.toString())
        .apply()
}

fun loadSchedule(context: Context): List<DaySchedule> {
    val json = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SCHEDULE, null) ?: return defaultSchedule()

    return try {
        val jsonArray = JSONArray(json)

        List(jsonArray.length()) { index ->
            val dayObject = jsonArray.getJSONObject(index)
            val subjectsArray = dayObject.getJSONArray("subjects")

            val subjects = List(subjectsArray.length()) { subjectIndex ->
                subjectsArray.getString(subjectIndex)
            }

            DaySchedule(
                dayName = dayObject.getString("dayName"),
                subjects = subjects
            )
        }
    } catch (e: Exception) {
        defaultSchedule()
    }
}

fun saveChecklist(context: Context, checklist: Map<String, List<TakeItem>>) {
    val rootObject = JSONObject()

    checklist.forEach { (dayName, items) ->
        val itemsArray = JSONArray()

        items.forEach { item ->
            val itemObject = JSONObject()
            itemObject.put("thing", item.thing)
            itemObject.put("subject", item.subject)
            itemsArray.put(itemObject)
        }

        rootObject.put(dayName, itemsArray)
    }

    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CHECKLIST, rootObject.toString())
        .apply()
}

fun loadChecklist(context: Context): Map<String, List<TakeItem>> {
    val json = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CHECKLIST, null) ?: return emptyMap()

    return try {
        val rootObject = JSONObject(json)
        val result = mutableMapOf<String, List<TakeItem>>()

        val keys = rootObject.keys()
        while (keys.hasNext()) {
            val dayName = keys.next()
            val itemsArray = rootObject.getJSONArray(dayName)

            val items = List(itemsArray.length()) { index ->
                val itemObject = itemsArray.getJSONObject(index)

                TakeItem(
                    thing = itemObject.getString("thing"),
                    subject = itemObject.getString("subject")
                )
            }

            result[dayName] = items
        }

        result
    } catch (e: Exception) {
        emptyMap()
    }
}

// Экраны
sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object Schedule : Screen("schedule", Icons.Filled.Home, "Расписание")
    object TakeWithYou : Screen("take", Icons.Filled.Menu, "Взять с собой")

    object Editor : Screen("editor/{dayIndex}", Icons.Filled.Edit, "Редактор") {
        fun createRoute(index: Int) = "editor/$index"
    }
}

private val bottomTabs = listOf(Screen.Schedule, Screen.TakeWithYou)

@Composable
fun NavBar(
    navController: NavHostController,
    scheduleData: MutableList<DaySchedule>,
    takeChecklist: MutableMap<String, List<TakeItem>>,
    onScheduleChanged: () -> Unit,
    onChecklistChanged: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Schedule.route
    ) {
        bottomTabs.forEach { screen ->
            composable(screen.route) {
                when (screen) {
                    Screen.Schedule -> ScheduleScreen(scheduleData) { idx ->
                        navController.navigate(Screen.Editor.createRoute(idx))
                    }

                    Screen.TakeWithYou -> TakeWithYouScreen(
                        scheduleData = scheduleData,
                        takeChecklist = takeChecklist,
                        onChecklistChanged = onChecklistChanged
                    )

                    else -> {}
                }
            }
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("dayIndex") { type = NavType.IntType })
        ) { backEntry ->
            val dayIndex = backEntry.arguments?.getInt("dayIndex") ?: 0

            EditorDayScreen(
                scheduleData = scheduleData,
                dayIndex = dayIndex,
                onCancel = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                onScheduleChanged = onScheduleChanged
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RootFun() {
    val context = LocalContext.current

    val scheduleData = remember {
        mutableStateListOf<DaySchedule>().apply {
            addAll(loadSchedule(context))
        }
    }

    val takeChecklist = remember {
        mutableStateMapOf<String, List<TakeItem>>().apply {
            putAll(loadChecklist(context))
        }
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route?.substringBefore('/')

    val appBarTitle = when (currentRoute) {
        Screen.Schedule.route -> Screen.Schedule.title
        Screen.TakeWithYou.route -> Screen.TakeWithYou.title
        "editor" -> {
            val idx = backStack?.arguments?.getInt("dayIndex") ?: 0
            if (idx in scheduleData.indices) {
                "Редактор: ${scheduleData[idx].dayName}"
            } else {
                "Редактор"
            }
        }

        else -> Screen.Schedule.title
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appBarTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(text = screen.title)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavBar(
                navController = navController,
                scheduleData = scheduleData,
                takeChecklist = takeChecklist,
                onScheduleChanged = {
                    saveSchedule(context, scheduleData)
                },
                onChecklistChanged = {
                    saveChecklist(context, takeChecklist)
                }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThemeDayPlanner {
                RootFun()
            }
        }
    }
}

@Composable
fun ScheduleScreen(
    scheduleData: List<DaySchedule>,
    onDayClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(scheduleData) { index, day ->
            DayCard(
                daySchedule = day,
                onClick = { onDayClick(index) }
            )
        }
    }
}

@Composable
fun DayCard(
    daySchedule: DaySchedule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = daySchedule.dayName,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            daySchedule.subjects.forEach { subject ->
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorDayScreen(
    scheduleData: MutableList<DaySchedule>,
    dayIndex: Int,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onScheduleChanged: () -> Unit
) {
    if (dayIndex !in scheduleData.indices) {
        Text(text = "День не найден")
        return
    }

    val currentDay = scheduleData[dayIndex]

    val subjects = remember(dayIndex) {
        mutableStateListOf<String>().apply {
            addAll(currentDay.subjects)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        subjects.forEachIndexed { index, subject ->
            Spacer(modifier = Modifier.height(8.dp))

            SubjectDropdownRow(
                subjectName = subject,
                onSubjectSelected = { newSubject ->
                    subjects[index] = newSubject
                },
                onDelete = {
                    subjects.removeAt(index)
                }
            )
        }

        OutlinedButton(
            onClick = {
                subjects.add(allSubjects.first())
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(text = "Добавить урок")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scheduleData[dayIndex] = currentDay.copy(subjects = subjects.toList())
                onScheduleChanged()
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Ввести",
                fontSize = 18.sp
            )
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Отмена")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDropdownRow(
    subjectName: String,
    onSubjectSelected: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember(subjectName) { mutableStateOf(subjectName) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = { newText ->
                    selectedText = newText
                    onSubjectSelected(newText)
                },
                label = {
                    Text(text = "Выбор дисциплины")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                allSubjects.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(text = item)
                        },
                        onClick = {
                            selectedText = item
                            onSubjectSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Удалить урок"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeWithYouScreen(
    scheduleData: List<DaySchedule>,
    takeChecklist: MutableMap<String, List<TakeItem>>,
    onChecklistChanged: () -> Unit
) {
    val dayNames = scheduleData.map { it.dayName }

    var selectedDay by remember {
        mutableStateOf(dayNames.firstOrNull() ?: "")
    }

    var dayDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var saved by remember(selectedDay) {
        mutableStateOf(false)
    }

    val takeItems = remember(selectedDay) {
        mutableStateListOf<TakeItem>().apply {
            addAll(takeChecklist[selectedDay] ?: emptyList())
        }
    }

    val selectedSchedule = scheduleData.find { it.dayName == selectedDay }
    val availableSubjects = selectedSchedule?.subjects ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Выберите день и укажите, что нужно взять:",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = dayDropdownExpanded,
            onExpandedChange = {
                dayDropdownExpanded = !dayDropdownExpanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDay,
                onValueChange = {},
                label = {
                    Text(text = "День недели")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = dayDropdownExpanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true
            )

            ExposedDropdownMenu(
                expanded = dayDropdownExpanded,
                onDismissRequest = {
                    dayDropdownExpanded = false
                }
            ) {
                dayNames.forEach { dayName ->
                    DropdownMenuItem(
                        text = {
                            Text(text = dayName)
                        },
                        onClick = {
                            selectedDay = dayName
                            dayDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        takeItems.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = item.thing,
                    onValueChange = { newText ->
                        takeItems[index] = item.copy(thing = newText)
                    },
                    label = {
                        Text(text = "Что взять")
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                var subjectExpanded by remember {
                    mutableStateOf(false)
                }

                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = {
                        subjectExpanded = !subjectExpanded
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = item.subject,
                        onValueChange = {},
                        label = {
                            Text(text = "Предмет")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = subjectExpanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true
                    )

                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = {
                            subjectExpanded = false
                        }
                    ) {
                        availableSubjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = subject)
                                },
                                onClick = {
                                    takeItems[index] = item.copy(subject = subject)
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        takeItems.removeAt(index)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Удалить строку"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                val defaultSubject = availableSubjects.firstOrNull() ?: ""
                takeItems.add(
                    TakeItem(
                        thing = "",
                        subject = defaultSubject
                    )
                )
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Добавить строку"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(text = "Добавить строку")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                takeChecklist[selectedDay] = takeItems.toList()
                onChecklistChanged()
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Сохранить",
                fontSize = 18.sp
            )
        }

        if (saved) {
            Text(
                text = "Данные сохранены для $selectedDay",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}