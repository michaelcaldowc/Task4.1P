package com.michaelcaldow.taskmanagerapp;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    void insert(Task task);
    @Update
    void update(Task task);
    @Delete
    void delete(Task task);
    @Query("DELETE FROM task_table WHERE id = :taskId")
    void deleteById(int taskId);
    @Query("SELECT * FROM task_table WHERE id = :taskId")
    Task getTaskById(int taskId);
    @Query("SELECT * FROM task_table ORDER BY CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date ASC")
    List<Task> getAllTasksSortedByDueDate();
}
