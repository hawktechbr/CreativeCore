package team.creative.creativecore.common.util.registry;

import java.util.Collection;
import java.util.HashMap;

public class NamedHandlerRegistry<T> {
    
    private HashMap<String, T> handlers = new HashMap<>();
    private HashMap<T, String> handlersInv = new HashMap<>();
    private T defaultHandler;
    private boolean allowOverwrite = false;
    
    public NamedHandlerRegistry(T handler) {
        this.defaultHandler = handler;
    }
    
    public NamedHandlerRegistry<T> allowOverwrite() {
        allowOverwrite = true;
        return this;
    }
    
    public T getDefault() {
        return defaultHandler;
    }
    
    public void register(String id, T handler) {
        if (!allowOverwrite && handlers.containsKey(id))
            throw new IllegalArgumentException("'" + id + "' already exists");
        handlers.put(id, handler);
        handlersInv.put(handler, id);
    }
    
    public String getId(T type) {
        return handlersInv.get(type);
    }
    
    public T get(String id) {
        return handlers.getOrDefault(id, defaultHandler);
    }
    
    public Collection<String> keys() {
        return handlers.keySet();
    }
}
