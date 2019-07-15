package com.codicesoftware.plugins.hudson.util;

import hudson.model.BooleanParameterValue;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SelectorParametersResolverTest {

    @Test
    public void testResolver() {
        List<ParameterValue> parameters = new LinkedList<>();
        parameters.add(new StringParameterValue("PARAM", "VALUE"));
        parameters.add(new BooleanParameterValue("BOOL", true));

        assertEquals("sample VALUE text", SelectorParametersResolver.resolve("sample $PARAM text", parameters));
        assertEquals("sample VALUE text", SelectorParametersResolver.resolve("sample ${PARAM} text", parameters));
        assertEquals("sample PARAM text", SelectorParametersResolver.resolve("sample PARAM text", parameters));
        assertEquals("sample true text", SelectorParametersResolver.resolve("sample $BOOL text", parameters));
    }

    @Test
    public void testLegacyParamsResolver() {
        List<ParameterValue> parameters = new LinkedList<>();
        parameters.add(new StringParameterValue("PARAM", "VALUE"));
        parameters.add(new BooleanParameterValue("BOOL", true));

        assertEquals("sample VALUE text", SelectorParametersResolver.resolve("sample %PARAM% text", parameters));
        assertEquals("sample PARAM text", SelectorParametersResolver.resolve("sample PARAM text", parameters));
        assertEquals("sample true text", SelectorParametersResolver.resolve("sample %BOOL% text", parameters));
    }

    @Test
    public void testMixedFormatsResolver() {
        List<ParameterValue> parameters = new LinkedList<>();
        parameters.add(new StringParameterValue("PARAM", "VALUE"));
        parameters.add(new BooleanParameterValue("BOOL", true));

        assertEquals("sample VALUE VALUE text", SelectorParametersResolver.resolve("sample %PARAM% $PARAM text", parameters));
        assertEquals("sample VALUE VALUE text", SelectorParametersResolver.resolve("sample ${PARAM} %PARAM% text", parameters));
        assertEquals("sample true true text", SelectorParametersResolver.resolve("sample $BOOL %BOOL% text", parameters));
    }
}