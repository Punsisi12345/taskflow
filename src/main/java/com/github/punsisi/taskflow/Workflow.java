package com.github.punsisi.taskflow;

import java.util.*;

public class Workflow {

    private final Map<String, Task> tasks;
    private final Map<String, List<String>> adjacencyList;
    private final Map<String, Integer> inDegrees;


    private final WorkflowContext context;


    private Workflow(Map<String, Task> tasks, Map<String, List<String>> adjacencyList, Map<String, Integer> inDegrees) {
        this.tasks = tasks;
        this.adjacencyList = adjacencyList;
        this.inDegrees = inDegrees;
        this.context = new WorkflowContext();
    }


    public Map<String, Task> getTasks() { return tasks; }
    public Map<String, List<String>> getAdjacencyList() { return adjacencyList; }
    public Map<String, Integer> getInDegrees() { return inDegrees; }
    public WorkflowContext getContext() { return context; }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private final Map<String, Task> tasks = new HashMap<>();
        private final Map<String, List<String>> adjacencyList = new HashMap<>();
        private final Map<String, Integer> inDegrees = new HashMap<>();


        public Builder addTask(Task task) {
            String taskId = task.getId();
            if (tasks.containsKey(taskId)) {
                throw new IllegalArgumentException("Task ID '" + taskId + "' already existed. Task IDs must be unique.");
            }
            tasks.put(taskId, task);
            adjacencyList.putIfAbsent(taskId, new ArrayList<>());
            inDegrees.putIfAbsent(taskId, 0);
            return this;
        }


        public Builder addDependency(String fromTaskId, String toTaskId) {
            if (!tasks.containsKey(fromTaskId) || !tasks.containsKey(toTaskId)) {
                throw new IllegalArgumentException("Both tasks must be added to the workflow before defining a dependency");
            }

            adjacencyList.get(fromTaskId).add(toTaskId);

            inDegrees.put(toTaskId, inDegrees.get(toTaskId) + 1);
            return this;
        }


        public Workflow build() {
            if (tasks.isEmpty()) {
                throw new IllegalStateException("To create a workflow, there must be at least one task.");
            }
            if (hasCycle()) {
                throw new IllegalStateException("Error: Deadlock! There are circular dependencies between tasks.");
            }
            return new Workflow(tasks, adjacencyList, inDegrees);
        }


        private boolean hasCycle() {
            Queue<String> queue = new LinkedList<>();
            Map<String, Integer> tempInDegrees = new HashMap<>(inDegrees);


            for (Map.Entry<String, Integer> entry : tempInDegrees.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.add(entry.getKey());
                }
            }

            int processedCount = 0;

            while (!queue.isEmpty()) {
                String current = queue.poll();
                processedCount++;

                for (String neighbor : adjacencyList.get(current)) {
                    tempInDegrees.put(neighbor, tempInDegrees.get(neighbor) - 1);
                    if (tempInDegrees.get(neighbor) == 0) {
                        queue.add(neighbor);
                    }
                }
            }


            return processedCount != tasks.size();
        }
    }
}
