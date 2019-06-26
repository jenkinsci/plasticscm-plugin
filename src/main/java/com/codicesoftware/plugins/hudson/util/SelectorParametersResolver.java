package com.codicesoftware.plugins.hudson.util;

import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.List;
import java.util.logging.Logger;

public class SelectorParametersResolver {
    private static final Logger logger = Logger.getLogger(
            SelectorParametersResolver.class.getName());

    public static String resolve(
            String selector, List<ParameterValue> parameters) {
        if (parameters == null)
            return selector;

        logger.info("Replacing build parameters in selector...");

        String result = selector;
        for (ParameterValue parameter : parameters) {
            if (!(parameter instanceof StringParameterValue))
                continue;

            StringParameterValue stringParameter = (StringParameterValue)parameter;
            String variable = "%" + stringParameter.getName() + "%";
            String value = stringParameter.value;
            logger.info("Replacing [" + variable + "]->[" + value + "]");
            result = result.replace(variable, value);
        }

        return result;
    }
}
