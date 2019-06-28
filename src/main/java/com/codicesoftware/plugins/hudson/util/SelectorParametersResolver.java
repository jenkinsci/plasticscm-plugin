package com.codicesoftware.plugins.hudson.util;

import hudson.Util;
import hudson.model.ParameterValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SelectorParametersResolver {

    private static final Logger logger = Logger.getLogger(SelectorParametersResolver.class.getName());

    public static String resolve(
            String selector, List<ParameterValue> parameters) {
        if (parameters == null)
            return selector;

        logger.info("Replacing build parameters in selector...");

        Map<String, String> parametersMap = new HashMap<>();
        for (ParameterValue parameter : parameters) {
            parametersMap.put(parameter.getName(), parameter.getValue().toString());
        }

        return Util.replaceMacro(selector, parametersMap);
    }
}
