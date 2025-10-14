package org.mockserver.templates.engine.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import org.mockserver.configuration.Configuration;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.HttpRequest;
import org.mockserver.serialization.ObjectMapperFactory;
import org.mockserver.serialization.model.DTO;
import org.mockserver.templates.engine.TemplateEngine;
import org.mockserver.templates.engine.TemplateFunctions;
import org.mockserver.templates.engine.javascript.bindings.ScriptBindings;
import org.mockserver.templates.engine.model.HttpRequestTemplateObject;
import org.mockserver.templates.engine.serializer.HttpTemplateOutputDeserializer;
import org.slf4j.event.Level;

import javax.script.*;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mockserver.configuration.Configuration.configuration;
import static org.mockserver.formatting.StringFormatter.formatLogMessage;
import static org.mockserver.formatting.StringFormatter.indentAndToString;
import static org.mockserver.log.model.LogEntry.LogMessageType.TEMPLATE_GENERATED;
import static org.mockserver.log.model.LogEntryMessages.TEMPLATE_GENERATED_MESSAGE_FORMAT;

/**
 * @author jamesdbloom
 */
@SuppressWarnings({"RedundantSuppression", "deprecation", "removal", "FieldMayBeFinal"})
public class JavaScriptTemplateEngine implements TemplateEngine {

    private final ThreadLocal<ScriptEngine> engineThreadLocal = new ThreadLocal<>();
    private ObjectMapper objectMapper;
    private final MockServerLogger mockServerLogger;
    private HttpTemplateOutputDeserializer httpTemplateOutputDeserializer;
    private final Configuration configuration;

    public JavaScriptTemplateEngine(MockServerLogger mockServerLogger, Configuration configuration) {
        this.configuration = (configuration == null) ? configuration() : configuration;
        this.mockServerLogger = mockServerLogger;
        this.httpTemplateOutputDeserializer = new HttpTemplateOutputDeserializer(mockServerLogger);
        this.objectMapper = ObjectMapperFactory.createObjectMapper();
    }

    private ScriptEngine getEngine() {
        ScriptEngine engine = engineThreadLocal.get();
        if (engine == null) {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("graal.js");
            if (engine == null) {
                // Fallback to JavaScript engine if graal.js not available
                engine = manager.getEngineByName("javascript");
            }
            
            // Configure security restrictions for GraalJS
            if (engine != null && engine.getClass().getName().contains("graal")) {
                try {
                    // Disable Java class access
                    engine.put("java", null);
                    engine.put("Java", null);
                    engine.put("Packages", null);
                    engine.put("JavaImporter", null);
                    
                    // Disable dangerous global objects
                    engine.put("load", null);
                    engine.put("loadWithNewGlobal", null);
                    engine.put("exit", null);
                    engine.put("quit", null);
                    
                    // Configure GraalJS specific security options via system properties
                    System.setProperty("polyglot.js.allowHostAccess", "false");
                    System.setProperty("polyglot.js.allowHostClassLookup", "false");
                    System.setProperty("polyglot.js.allowCreateThread", "false");
                    System.setProperty("polyglot.js.allowIO", "false");
                    System.setProperty("polyglot.js.allowNativeAccess", "false");
                    System.setProperty("polyglot.js.allowCreateProcess", "false");
                    
                } catch (Exception e) {
                    // Log but don't fail if security configuration fails
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.WARN)
                            .setMessageFormat("Failed to configure JavaScript engine security: {}")
                            .setArguments(e.getMessage())
                    );
                }
            }
            
            engineThreadLocal.set(engine);
        }
        return engine;
    }

    /**
     * Clean up ThreadLocal resources to prevent memory leaks
     */
    public void cleanup() {
        engineThreadLocal.remove();
    }

    @Override
    public <T> T executeTemplate(String template, HttpRequest request, Class<? extends DTO<T>> dtoClass) {
        // Security check: JavaScript templates are disabled by default
        if (!configuration.javascriptTemplatesEnabled()) {
            throw new UnsupportedOperationException(
                "JavaScript templates are disabled for security reasons. " +
                "To enable, set mockserver.javascriptTemplatesEnabled=true or use Configuration.javascriptTemplatesEnabled(true). " +
                "WARNING: Enabling JavaScript templates may expose security vulnerabilities (CVE-2021-32827)."
            );
        }
        
        T result = null;
        String script = wrapTemplate(template);
        try {
            validateTemplate(template);
            ScriptEngine engine = getEngine();
            if (engine != null) {
                Compilable compilable = (Compilable) engine;
                CompiledScript compiledScript = compilable.compile(script + " function serialise(request) { return JSON.stringify(handle(JSON.parse(request)), null, 2); }");

                engine.setBindings(new ScriptBindings(TemplateFunctions.BUILT_IN_FUNCTIONS), ScriptContext.ENGINE_SCOPE);
                compiledScript.eval();

                Object stringifiedResponse;
                if (engine instanceof Invocable) {
                    String requestJson = objectMapper.writeValueAsString(new HttpRequestTemplateObject(request));
                    stringifiedResponse = ((Invocable) engine).invokeFunction("serialise", requestJson);
                } else {
                    stringifiedResponse = null;
                }

            JsonNode generatedObject = null;
            try {
                if (stringifiedResponse != null) {
                        generatedObject = objectMapper.readTree(stringifiedResponse.toString());
                    }
                } catch (Throwable throwable) {
                    if (MockServerLogger.isEnabled(Level.INFO)) {
                        mockServerLogger.logEvent(
                            new LogEntry()
                                .setLogLevel(Level.INFO)
                                .setHttpRequest(request)
                                .setMessageFormat("exception deserialising generated content:{}into json node for request:{}")
                                .setArguments(stringifiedResponse, request)
                        );
                    }
                }
                if (MockServerLogger.isEnabled(Level.INFO)) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(TEMPLATE_GENERATED)
                            .setLogLevel(Level.INFO)
                            .setHttpRequest(request)
                            .setMessageFormat(TEMPLATE_GENERATED_MESSAGE_FORMAT)
                            .setArguments(generatedObject != null ? generatedObject : stringifiedResponse, script, request)
                    );
                }
                result = httpTemplateOutputDeserializer.deserializer(request, (String) stringifiedResponse, dtoClass);
            } else {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setHttpRequest(request)
                        .setMessageFormat(
                            "JavaScript based templating is only available in a JVM with a JavaScript engine, " +
                                "please use a JVM with GraalVM JavaScript engine or add the GraalJS dependency"
                        )
                        .setArguments(new RuntimeException("JavaScript engine not available"))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(formatLogMessage("Exception:{}transforming template:{}for request:{}", isNotBlank(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName(), template, request), e);
        }
        return result;
    }

    static String wrapTemplate(String template) {
        return "function handle(request) {" + indentAndToString(template)[0] + "}";
    }

    private void validateTemplate(String template) {
        if (isNotBlank(template) && isNotBlank(configuration.javascriptDisallowedText())) {
            Iterable<String> deniedStrings = Splitter.on(",").trimResults().split(configuration.javascriptDisallowedText());
            for (String deniedString : deniedStrings) {
                if (template.contains(deniedString)) {
                    throw new UnsupportedOperationException("Found disallowed string \"" + deniedString + "\" in template: " + template);
                }
            }
        }
    }

    private static class DisallowClassesInTemplates {
        private Iterable<String> restrictedClassesList = null;
        private final Configuration configuration;

        private DisallowClassesInTemplates(Configuration configuration) {
            this.configuration = configuration;
            init();
        }

        void init() {
            restrictedClassesList = Splitter.on(",").trimResults().split(configuration.javascriptDisallowedClasses());
        }

        /**
         * Specifies whether the Java class of the specified name be exposed to javascript
         * Note: Class filtering not available with standard ScriptEngine API
         *
         * @param className is the fully qualified name of the java class being checked.
         *                  This will not be null. Only non-array class names will be passed.
         * @return true if the java class can be exposed to javascript, false otherwise
         */
        public boolean exposeToScripts(String className) {
            if (restrictedClassesList != null) {
                return StreamSupport
                    .stream(restrictedClassesList.spliterator(), false)
                    .noneMatch(restrictedClass -> restrictedClass.equalsIgnoreCase(className));
            } else {
                return true;
            }
        }
    }
}
