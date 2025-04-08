package com.michaelcaldow.taskmanagerapp.ui;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.michaelcaldow.taskmanagerapp.R;
import com.michaelcaldow.taskmanagerapp.Task;
import com.michaelcaldow.taskmanagerapp.TaskDao;
import com.michaelcaldow.taskmanagerapp.TaskDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

public class TaskDetailActivity extends AppCompatActivity {
    private static final int INVALID_TASK_ID = -1;
    private TextView textViewTitle, textViewDescription, textViewDueDate;
    private Button buttonCancel, buttonEdit, buttonDelete;
    private TaskDao taskDao;
    private ExecutorService databaseExecutor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    private int currentTaskId = INVALID_TASK_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);
        setTitle("Task Details");

        textViewTitle = findViewById(R.id.detail_text_view_title);
        textViewDescription = findViewById(R.id.detail_text_view_description);
        textViewDueDate = findViewById(R.id.detail_text_view_due_date);
        buttonCancel = findViewById(R.id.detail_button_cancel);
        buttonEdit = findViewById(R.id.detail_button_edit);
        buttonDelete = findViewById(R.id.detail_button_delete);

        TaskDatabase database = TaskDatabase.getDatabase(this);
        taskDao = database.taskDao();
        databaseExecutor = TaskDatabase.databaseWriteExecutor;

        currentTaskId = getIntent().getIntExtra(AddEditTaskActivity.EXTRA_TASK_ID, INVALID_TASK_ID);

        if (currentTaskId == INVALID_TASK_ID) {
            Toast.makeText(this, "Error: Invalid Task ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTaskDetails();

        // Set button listeners
        buttonCancel.setOnClickListener(v -> finish());

        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(TaskDetailActivity.this, AddEditTaskActivity.class);
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, currentTaskId);
            startActivity(intent);
        });

        buttonDelete.setOnClickListener(v -> {
            deleteCurrentTask();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTaskId != INVALID_TASK_ID) {
            loadTaskDetails();
        }
    }

    private void loadTaskDetails() {
        databaseExecutor.execute(() -> {
            final Task task = taskDao.getTaskById(currentTaskId);
            mainThreadHandler.post(() -> {
                if (task != null) {
                    populateUI(task);
                } else {
                    Toast.makeText(TaskDetailActivity.this, R.string.error_loading_task_toast, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void populateUI(Task task) {
        textViewTitle.setText(task.getTitle());
        textViewDescription.setText(task.getDescription());
        if (task.getDueDate() != null) {
            textViewDueDate.setText(dateFormat.format(new Date(task.getDueDate())));
        } else {
            textViewDueDate.setText("N/A");
        }
    }

    private void deleteCurrentTask() {
        if (currentTaskId == INVALID_TASK_ID) return;

        databaseExecutor.execute(() -> {
            taskDao.deleteById(currentTaskId);
            mainThreadHandler.post(() -> {
                Toast.makeText(TaskDetailActivity.this, R.string.task_deleted_toast, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
