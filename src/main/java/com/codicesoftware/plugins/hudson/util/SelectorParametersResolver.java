package com.codicesoftware.plugins.hudson.util;

import hudson.Util;
import hudson.model.ParameterValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectorParametersResolver {

    public static String resolve(String text, List<ParameterValue> parameters) {
        if (parameters == null) {
            return text;
        }

        Map<String, String> parametersMap = new HashMap<>();
        for (ParameterValue parameter : parameters) {
            if ((parameter != null) && (parameter.getName() != null) && (parameter.getValue() != null)) {
                parametersMap.put(parameter.getName(), parameter.getValue().toString());
            }
        }

        return Util.replaceMacro(text, parametersMap);
    }
}
