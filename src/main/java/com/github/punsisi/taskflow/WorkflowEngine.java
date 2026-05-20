package com.github.punsisi.taskflow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WorkflowEngine implements TaskObserver {


    private final ExecutorService executor;
    private Workflow workflow;


    private Map<String, Integer> currentInDegrees;

    private int totalTasks;
    private int completedTasksCount;


    public WorkflowEngine(int numberOfThreads) {
        this.executor = Executors.newFixedThreadPool(numberOfThreads);
    }


    public void startWorkflow(Workflow workflow) {
        this.workflow = workflow;
        this.totalTasks = workflow.getTasks().size();
        this.completedTasksCount = 0;


        this.currentInDegrees = new ConcurrentHashMap<>(workflow.getInDegrees());

        System.out.println("[Engine] Workflow started with " + totalTasks + " tasks.\n");
        executeReadyTasks();
    }


    private synchronized void executeReadyTasks() {
        for (Map.Entry<String, Integer> entry : currentInDegrees.entrySet()) {

            if (entry.getValue() == 0) {
                String taskId = entry.getKey();
                Task task = workflow.getTasks().get(taskId);


                if (task.getStatus() == TaskStatus.PENDING) {
                    task.setStatus(TaskStatus.RUNNING);


                    executor.submit(() -> runTaskWithWrapper(task, workflow.getContext()));
                }
            }
        }
    }


    private void runTaskWithWrapper(Task task, WorkflowContext context) {
        try {

            task.execute(context);

            task.setStatus(TaskStatus.COMPLETED);
            onTaskCompleted(task.getId());
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            onTaskFailed(task.getId(), e);
        }
    }


    @Override
    public synchronized void onTaskCompleted(String taskId) {
        completedTasksCount++;
        System.out.println("[Engine] Task Completed: " + taskId);


        if (completedTasksCount == totalTasks) {
            System.out.println("\n[Engine] All tasks finished. Workflow completed successfully!");
            executor.shutdown();
            return;
        }


        List<String> dependents = workflow.getAdjacencyList().get(taskId);
        if (dependents != null) {
            for (String dependentId : dependents) {
                currentInDegrees.put(dependentId, currentInDegrees.get(dependentId) - 1);
            }
        }


        executeReadyTasks();
    }

    @Override
    public synchronized void onTaskFailed(String taskId, Throwable cause) {
        System.err.println("[Engine] Workflow stopped! Task Failed: " + taskId + " | Reason: " + cause.getMessage());

        executor.shutdownNow();
    }
}
