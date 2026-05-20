# 🚀 TaskFlow Engine

![Java Support](https://img.shields.io/badge/Java-17%2B-blue.svg)
![JitPack](https://img.shields.io/badge/JitPack-Release-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-orange.svg)

**TaskFlow Engine** is a lightweight, high-performance, and dependency-driven background task scheduler for Java and Android.

It uses **Directed Acyclic Graphs (DAG)** to manage complex, inter-dependent tasks and executes them efficiently in parallel using a Thread Pool. It is specifically designed to prevent UI thread blocking in Android apps and optimize data synchronization processes (e.g., Offline-first apps).

## ✨ Key Features
* **Parallel Execution:** Automatically identifies independent tasks and runs them concurrently to drastically reduce execution time.
* **Smart Dependency Management:** Ensures strict execution order (e.g., Task C only starts after Task A and B are completed).
* **Deadlock Prevention:** Built-in cycle detection (Kahn's Algorithm) validates the graph and prevents circular dependencies before execution.
* **Fail-Fast Mechanism:** Safely aborts the entire workflow if a critical task fails, preventing cascading errors.
* **Thread-Safe Data Passing:** Share data between tasks safely using the built-in `WorkflowContext`.
* **Zero External Dependencies:** Built entirely with standard Core Java (`java.util.concurrent`).

---

## 📦 Installation (Setup Guide)

You can easily include this library in your Java, Spring Boot, or Android project via **JitPack**.

### Step 1: Add the JitPack repository
Open your `settings.gradle` (or root `build.gradle` in older projects) and add the JitPack repository url:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // Add this line!
    }
}
```

### Step 2: Add the dependency
Open your app-level `build.gradle` file and add the TaskFlow dependency:

```gradle
dependencies {
    // Replace '1.0.0' with the latest release version
    implementation 'com.github.punsisi:taskflow:1.0.0'
}
```
*Sync your Gradle project, and you are good to go!*

---

## 💻 Quick Start & Usage Guide

Here is a complete example of how to use TaskFlow in your project.

### 1. Implement the Tasks
Create your tasks by implementing the `Task` interface. You can access or modify shared data using the `WorkflowContext`.

```java
import com.github.punsisi.taskflow.*;

public class FetchUserTask implements Task {
    private TaskStatus status = TaskStatus.PENDING;
    
    @Override public String getId() { return "Fetch_User_Task"; }
    @Override public TaskStatus getStatus() { return status; }
    @Override public void setStatus(TaskStatus status) { this.status = status; }

    @Override
    public void execute(WorkflowContext context) throws Exception {
        System.out.println("Fetching user from database...");
        Thread.sleep(1000); // Simulate network call
        
        // Share data with the next task
        context.put("userId", "USR_100"); 
    }
}
```

### 2. Build the Workflow (The DAG)
Use the Builder pattern to register your tasks and define which task depends on which.

```java
Task fetchTask = new FetchUserTask();
Task processTask = new ProcessDataTask("Process_Task");
Task emailTask = new SendEmailTask("Email_Task");

// Creating a flow: Fetch -> Process -> Email
Workflow workflow = Workflow.builder()
        .addTask(fetchTask)
        .addTask(processTask)
        .addTask(emailTask)
        // processTask will NOT start until fetchTask completes
        .addDependency("Fetch_User_Task", "Process_Task")
        // emailTask will NOT start until processTask completes
        .addDependency("Process_Task", "Email_Task")
        .build();
```

### 3. Start the Engine
Create the `WorkflowEngine` by specifying the number of background threads you want to allocate, and start the workflow.

```java
// Create an engine with a pool of 3 background threads
WorkflowEngine engine = new WorkflowEngine(3);

// Execute the workflow
engine.startWorkflow(workflow);
```

---

## 🏗️ Architecture Best Practices (For Android Developers)

If you are using this library in an **Android App** (like an Offline-first architecture with Room DB):

1. **Never update the UI directly from a Task:** TaskFlow runs completely on background threads. Modifying Android Views directly will cause a `CalledFromWrongThreadException`.
2. **The Reactive Pattern:**
    * Use **TaskFlow** to fetch data from your remote server (e.g., Firebase) concurrently.
    * Use your final Task to save the fetched data into your local **Room Database**.
    * Let Room's **`LiveData`** or Kotlin **`StateFlow`** automatically observe the database changes and push the updates to your UI (RecyclerViews).
    * *This ensures a clean 100% Separation of Concerns!*

---

## 📄 License
This project is licensed under the MIT License. Feel free to use, modify, and distribute it in your personal and commercial projects.