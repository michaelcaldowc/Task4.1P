package com.michaelcaldow.taskmanagerapp.ui;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.michaelcaldow.taskmanagerapp.R;
import com.michaelcaldow.taskmanagerapp.Task;
import com.michaelcaldow.taskmanagerapp.TaskDao;
import com.michaelcaldow.taskmanagerapp.TaskDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class AddEditTaskActivity extends AppCompatActivity {
    public static final String EXTRA_TASK_ID = "com.michaelcaldow.taskmanagerapp.EXTRA_TASK_ID";
    private static final int DEFAULT_TASK_ID = -1;

    private EditText editTextTitle, editTextDescription;
    private TextView textViewSelectedDueDate;
    private Button buttonSaveTask, buttonCancel;
    private TaskDao taskDao;
    private ExecutorService databaseExecutor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private int currentTaskId = DEFAULT_TASK_ID;
    private Long selectedDueDateMillis = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        textViewSelectedDueDate = findViewById(R.id.text_view_selected_due_date);
        buttonSaveTask = findViewById(R.id.button_save_task);
        buttonCancel = findViewById(R.id.button_cancel);


        TaskDatabase database = TaskDatabase.getDatabase(this);
        taskDao = database.taskDao();
        databaseExecutor = TaskDatabase.databaseWriteExecutor;

        if (getIntent().hasExtra(EXTRA_TASK_ID)) {
            currentTaskId = getIntent().getIntExtra(EXTRA_TASK_ID, DEFAULT_TASK_ID);
        }

        if (currentTaskId == DEFAULT_TASK_ID) {
            setTitle(R.string.add_new_task_title);
        } else {
            setTitle(R.string.edit_task_title);
            loadTaskData();
        }

        textViewSelectedDueDate.setOnClickListener(v -> showDatePickerDialog());
        buttonSaveTask.setOnClickListener(v -> saveTask());

        buttonCancel.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadTaskData() {
        databaseExecutor.execute(() -> {
            final Task task = taskDao.getTaskById(currentTaskId);
            if (task != null) {
                selectedDueDateMillis = task.getDueDate();
                mainThreadHandler.post(() -> {
                    editTextTitle.setText(task.getTitle());
                    editTextDescription.setText(task.getDescription());
                    updateDateTextView();
                });
            } else {
                mainThreadHandler.post(() -> {
                    Toast.makeText(this, R.string.error_loading_task_toast, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();
        if (selectedDueDateMillis != null) cal.setTimeInMillis(selectedDueDateMillis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.set(year1, month1, dayOfMonth);
            selectedDueDateMillis = selectedCal.getTimeInMillis();
            updateDateTextView();
        }, year, month, day).show();
    }

    private void updateDateTextView() {
        if (selectedDueDateMillis != null) {
            textViewSelectedDueDate.setText(dateFormat.format(new Date(selectedDueDateMillis)));
        } else {
            textViewSelectedDueDate.setText("");
            textViewSelectedDueDate.setHint(R.string.select_due_date);
        }
    }

    private void saveTask() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError(getString(R.string.title_required_error));
            editTextTitle.requestFocus();
            return;
        }

        if (selectedDueDateMillis == null) {
            Toast.makeText(this, R.string.due_date_required_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final Task taskToSave = new Task(title, description, selectedDueDateMillis);
        if (currentTaskId != DEFAULT_TASK_ID) taskToSave.setId(currentTaskId);

        databaseExecutor.execute(() -> {
            if (currentTaskId == DEFAULT_TASK_ID) taskDao.insert(taskToSave);
            else taskDao.update(taskToSave);

            mainThreadHandler.post(() -> {
                Toast.makeText(AddEditTaskActivity.this, R.string.task_saved_toast, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}