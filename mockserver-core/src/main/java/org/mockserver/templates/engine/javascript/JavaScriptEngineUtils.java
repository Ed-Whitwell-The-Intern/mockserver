package org.mockserver.templates.engine.javascript;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JavaScriptEngineUtils {
    
    public static boolean isAvailable() {
        return getEngine() != null;
    }
    
    public static ScriptEngine getEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        return manager.getEngineByName("graal.js");
    }
}
