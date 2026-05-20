package com.github.punsisi.taskflow;

public interface Task {


    String getId();


    TaskStatus getStatus();


    void setStatus(TaskStatus status);


    void execute(WorkflowContext context) throws Exception;

}
