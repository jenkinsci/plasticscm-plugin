package com.codicesoftware.plugins.hudson.util;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.ParameterValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectorParametersResolver {

    private SelectorParametersResolver() { }

    public static String resolve(String text, List<ParameterValue> parameters, EnvVars environment) {
        if (parameters == null) {
            return text;
        }

        Map<String, String> parametersMap = new HashMap<>();
        for (ParameterValue parameter : parameters) {
            if ((parameter != null) && (parameter.getName() != null) && (parameter.getValue() != null)) {
                parametersMap.put(parameter.getName(), parameter.getValue().toString());
            }
        }

        return environment.expand(Util.replaceMacro(
            resolveLegacyFormat(text, parametersMap), parametersMap));
    }

    static String resolveLegacyFormat(String text, Map<String, String> parametersMap) {
        String result = text;
        for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
            result = result.replaceAll("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
