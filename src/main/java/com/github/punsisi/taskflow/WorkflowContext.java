package com.github.punsisi.taskflow;

import java.util.concurrent.ConcurrentHashMap;

public class WorkflowContext {


  private final ConcurrentHashMap<String, Object> data =   new ConcurrentHashMap<>();


    public void put(String key, Object value) {
        data.put(key, value);
    }


    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

}
