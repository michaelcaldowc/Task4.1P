package com.michaelcaldow.taskmanagerapp.ui;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.michaelcaldow.taskmanagerapp.R;
import com.michaelcaldow.taskmanagerapp.Task;
import com.michaelcaldow.taskmanagerapp.TaskDao;
import com.michaelcaldow.taskmanagerapp.TaskDatabase;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
    private TaskDao taskDao;
    private TaskAdapter taskAdapter;
    private ExecutorService databaseExecutor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        TaskDatabase database = TaskDatabase.getDatabase(this);
        taskDao = database.taskDao();
        databaseExecutor = TaskDatabase.databaseWriteExecutor;

        setupRecyclerView();

        Button buttonAddTask = findViewById(R.id.button_add_task);
        buttonAddTask.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            startActivity(intent);


            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        });
    }

        @Override
        protected void onResume() {
            super.onResume();
            loadTasks();
        }

        private void setupRecyclerView() {
            RecyclerView recyclerView = findViewById(R.id.recycler_view_tasks);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);
            taskAdapter = new TaskAdapter(this);
            recyclerView.setAdapter(taskAdapter);
        }

        private void loadTasks() {
            databaseExecutor.execute(() -> {
                List<Task> tasks = taskDao.getAllTasksSortedByDueDate();
                mainThreadHandler.post(() -> taskAdapter.setTasks(tasks));
            });
        }

        //Adapter Click Listener Methods
         @Override
         public void onTaskClick(Task task) {
            Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        }

}
