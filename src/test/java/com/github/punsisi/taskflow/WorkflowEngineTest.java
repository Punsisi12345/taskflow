package com.github.punsisi.taskflow;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowEngineTest {


    abstract static class TestTask implements Task {
        private final String id;
        private TaskStatus status = TaskStatus.PENDING;

        public TestTask(String id) { this.id = id; }
        @Override public String getId() { return id; }
        @Override public TaskStatus getStatus() { return status; }
        @Override public void setStatus(TaskStatus status) { this.status = status; }
    }

    @Test
    void testCircularDependencyThrowsException() {
        Task a = new TestTask("A") { @Override public void execute(WorkflowContext ctx) {} };
        Task b = new TestTask("B") { @Override public void execute(WorkflowContext ctx) {} };


        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            Workflow.builder()
                    .addTask(a).addTask(b)
                    .addDependency("A", "B")
                    .addDependency("B", "A")
                    .build();
        });

        assertTrue(exception.getMessage().contains("Deadlock"));
    }

    @Test
    void testWorkflowContextDataPassing() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Task putDataTask = new TestTask("PutData") {
            @Override public void execute(WorkflowContext context) {
                context.put("testKey", "testValue");
                latch.countDown();
            }
        };

        Task getDataTask = new TestTask("GetData") {
            @Override public void execute(WorkflowContext context) {
                String value = context.get("testKey", String.class);
                assertEquals("testValue", value, "Data must have been exchanged correctly.");
                latch.countDown();
            }
        };

        Workflow workflow = Workflow.builder()
                .addTask(putDataTask).addTask(getDataTask)
                .addDependency("PutData", "GetData")
                .build();

        WorkflowEngine engine = new WorkflowEngine(2);
        engine.startWorkflow(workflow);


        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testExecutionOrder() throws InterruptedException {

        List<String> executionOrder = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(4);


        Task a = new TestTask("A") { @Override public void execute(WorkflowContext ctx) throws Exception { Thread.sleep(100); executionOrder.add("A"); latch.countDown(); }};
        Task b = new TestTask("B") { @Override public void execute(WorkflowContext ctx) throws Exception { Thread.sleep(100); executionOrder.add("B"); latch.countDown(); }};
        Task c = new TestTask("C") { @Override public void execute(WorkflowContext ctx) throws Exception { Thread.sleep(100); executionOrder.add("C"); latch.countDown(); }};
        Task d = new TestTask("D") { @Override public void execute(WorkflowContext ctx) throws Exception { Thread.sleep(100); executionOrder.add("D"); latch.countDown(); }};

        Workflow workflow = Workflow.builder()
                .addTask(a).addTask(b).addTask(c).addTask(d)
                .addDependency("A", "B")
                .addDependency("A", "C")
                .addDependency("B", "D")
                .addDependency("C", "D")
                .build();

        WorkflowEngine engine = new WorkflowEngine(3);
        engine.startWorkflow(workflow);

        assertTrue(latch.await(5, TimeUnit.SECONDS));


        assertEquals(4, executionOrder.size());
        assertEquals("A", executionOrder.get(0), "First 'A' must be activated");
        assertEquals("D", executionOrder.get(3), "The 'D' should be on at the end.");
        assertTrue(executionOrder.contains("B") && executionOrder.contains("C"), "B and C must have been activated.");
    }
}