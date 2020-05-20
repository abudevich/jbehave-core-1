package org.jbehave.core.model;

import org.jbehave.core.steps.ParameterConverters;
import org.mockito.Mockito;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import org.jbehave.core.model.ExamplesTable.TableProperties;
import org.jbehave.core.steps.ParameterConverters;
import org.junit.jupiter.api.Test;

/**
 * @author Valery Yatsynovich
 */
class TablePropertiesBehaviour {

    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String CONVERTED = "converted";

    private TableProperties createTablePropertiesWithDefaultSeparators(String propertiesAsString) {
        return new TableProperties(null, propertiesAsString, "|", "|", "|--");
    }

    @Test
    void canGetCustomProperties() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators(
                "ignorableSeparator=!--,headerSeparator=!,valueSeparator=!,"
                        + "commentSeparator=#,trim=false,metaByRow=true,transformer=CUSTOM_TRANSFORMER");
        assertThat(properties.getRowSeparator(), equalTo("\n"));
        assertThat(properties.getHeaderSeparator(), equalTo("!"));
        assertThat(properties.getValueSeparator(), equalTo("!"));
        assertThat(properties.getIgnorableSeparator(), equalTo("!--"));
        assertThat(properties.getCommentSeparator(), equalTo("#"));
        assertThat(properties.isTrim(), is(false));
        assertThat(properties.isMetaByRow(), is(true));
        assertThat(properties.getTransformer(), equalTo("CUSTOM_TRANSFORMER"));
    }

    @Test
    void canSetPropertiesWithBackwardSlash() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("custom=\\");
        assertThat(properties.getProperties().getProperty("custom"), equalTo("\\"));
    }

    @Test
    void canSetPropertiesWithSpecialCharsInValues() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("withSpecialChars=value;/=:*$\\");
        assertThat(properties.getProperties().getProperty("withSpecialChars"), equalTo("value;/=:*$\\"));
    }

    @Test
    void canSetPropertiesWithWhitespaceInValue() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("withWhitespace=a value");
        assertThat(properties.getProperties().getProperty("withWhitespace"), equalTo("a value"));
    }

    @Test
    void canSetPropertiesWithMixedCharsInValues() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("withMixedChars=value1;value2:*");
        assertThat(properties.getProperties().getProperty("withMixedChars"), equalTo("value1;value2:*"));
    }

    @Test
    void canSetPropertiesWithSpecialCharsInName() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("p.r:o*p$e|r;t#y=value");
        assertThat(properties.getProperties().getProperty("p.r:o*p$e|r;t#y"), equalTo("value"));
    }

    @Test
    void canSetPropertiesStartingWithSpecialCharsAndContainingBracketsInValue() {
        TableProperties properties = createTablePropertiesWithDefaultSeparators("placeholderKey=${placeholderValue}");
        assertThat(properties.getProperties().getProperty("placeholderKey"), equalTo("${placeholderValue}"));
    }

    @Test
    void canGetDefaultProperties() {
        TableProperties properties = new TableProperties(null, new Properties());
        assertThat(properties.getHeaderSeparator(), equalTo("|"));
        assertThat(properties.getValueSeparator(), equalTo("|"));
        assertThat(properties.getIgnorableSeparator(), equalTo("|--"));
        assertThat(properties.getCommentSeparator(), equalTo("#"));
        assertThat(properties.isTrim(), is(true));
        assertThat(properties.isMetaByRow(), is(false));
        assertThat(properties.getTransformer(), is(nullValue()));
    }

    @Test
    void canGetAllProperties() {
        Properties properties = new Properties();
        properties.setProperty(KEY, VALUE);
        TableProperties tableProperties = new TableProperties(null, properties);
        assertThat(tableProperties.getProperties().containsKey(KEY), is(true));
    }

    @Test
    void canGetPropertiesWithNestedTransformersWithoutEscaping() {
        TableProperties properties = new TableProperties(null,
                "transformer=CUSTOM_TRANSFORMER, " + "tables={transformer=CUSTOM_TRANSFORMER\\, parameter1=value1}");
        assertThat(properties.isTrim(), is(true));
        assertThat(properties.isMetaByRow(), is(false));
        assertThat(properties.getTransformer(), equalTo("CUSTOM_TRANSFORMER"));
        assertThat(properties.getProperties().getProperty("tables"),
                equalTo("{transformer=CUSTOM_TRANSFORMER, parameter1=value1}"));
    }

    @Test
    void shouldApplyConvertersToValues() {
        ParameterConverters converters = Mockito.mock(ParameterConverters.class);
        Mockito.when(converters.convert(VALUE, String.class)).thenReturn(CONVERTED);
        Properties props = new Properties();
        props.put(KEY, VALUE);
        TableProperties properties = new TableProperties(converters, KEY + "=" + VALUE, "|", "|", "|--");
        assertThat(properties.getProperties().getProperty(KEY), equalTo(CONVERTED));
    }

    @Test
    void canDecoratePropertyValuesToTrimOrKeepVerbatim() {
        TableProperties properties = new TableProperties(null,
                "{key1|trim}= surroundedWithSpaces, {key2|verbatim}= surroundedWithSpaces ");
        assertThat(properties.getProperties().getProperty("key1"), equalTo("surroundedWithSpaces"));
        assertThat(properties.getProperties().getProperty("key2"), equalTo(" surroundedWithSpaces "));
    }

    @Test
    void canDecoratePropertyValuesToUpperAndLowerCase() {
        TableProperties properties = new TableProperties(null, "{key1|uppercase}=toUpper, {key2|lowercase}=toLower");
        assertThat(properties.getProperties().getProperty("key1"), equalTo("TOUPPER"));
        assertThat(properties.getProperties().getProperty("key2"), equalTo("tolower"));
    }

    @Test
    void canTrimPropertyValuesByDefault() {
        TableProperties properties = new TableProperties(null, "key1= surroundedWithSpaces , key2= ");
        assertThat(properties.getProperties().getProperty("key1"), equalTo("surroundedWithSpaces"));
        assertThat(properties.getProperties().getProperty("key2"), equalTo(""));
    }

    @Test
    void canChainDecoratorsToDecoratePropertyValues() {
        TableProperties properties = new TableProperties(null,
                "{key1|uppercase|verbatim}= toUpper , {key2|lowercase|trim}= toLower ");
        assertThat(properties.getProperties().getProperty("key1"), equalTo(" TOUPPER "));
        assertThat(properties.getProperties().getProperty("key2"), equalTo("tolower"));
    }

    @Test
    void cantGetMandatoryProperty() {
        TableProperties properties = new TableProperties(null, createProperties(emptyMap()));
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> properties.getMandatoryNonBlankProperty("key", int.class));
        assertEquals("'key' is not set in ExamplesTable properties", thrown.getMessage());
    }

    @Test
    void canGetMandatoryIntProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "1")));
        int actual = properties.getMandatoryNonBlankProperty("key", int.class);
        assertEquals(1, actual);
    }

    @Test
    void canGetMandatoryLongProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "1")));
        long actual = properties.getMandatoryNonBlankProperty("key", long.class);
        assertEquals(1L, actual);
    }

    @Test
    void canGetMandatoryDoubleProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "1")));
        assertEquals(1d, properties.getMandatoryNonBlankProperty("key", double.class));
    }

    @Test
    void canGetMandatoryBooleanProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "true")));
        boolean value = properties.getMandatoryNonBlankProperty("key", boolean.class);
        assertTrue(value);
    }

    @Test
    void canGetMandatoryNonBlankProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "string")));
        assertEquals("string", properties.<String>getMandatoryNonBlankProperty("key", String.class));
    }

    @Test
    void canGetMandatoryEnumProperty() {
        TableProperties properties = new TableProperties(new ParameterConverters(),
                createProperties(singletonMap("key", "BLACK")));
        assertEquals(TestEnum.BLACK, properties.getMandatoryNonBlankProperty("key", TestEnum.class));
    }

    private Properties createProperties(Map<String, String> map) {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    private static enum TestEnum {
        BLACK, WHITE
    }
}
