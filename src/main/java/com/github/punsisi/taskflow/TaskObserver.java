package com.github.punsisi.taskflow;

public interface TaskObserver {


    void onTaskCompleted(String taskId);


    void onTaskFailed(String taskId, Throwable cause);
}
