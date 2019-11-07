package uk.ac.ebi.ena.webin.cli.manifest.processor;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import static uk.ac.ebi.ena.webin.cli.manifest.processor.ProcessorTestUtils.createFieldValue;

public class CustomFieldProcessorTest {

    private ImmutablePair<String,String> parsedValue ;
    private CustomFieldProcessor processor = new CustomFieldProcessor();

    @Before
    public void init() {
        processor.setCallback(keyVal -> parsedValue = new ImmutablePair<>(keyVal.left, keyVal.right));
    }

    @Test
    public void testValid() {
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "CUSTOM_FIELD", "TEST_KEY:TEST_VAL" );
        ValidationResult result = new ValidationResult();
        processor.process( result, fieldValue );
        Assert.assertTrue( result.isValid() );
        Assert.assertEquals( "TEST_KEY", parsedValue.left );
        Assert.assertEquals( "TEST_VAL", parsedValue.right );
    }

    @Test
    public void testInValidValueFormat() {
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "CUSTOM_FIELD", "TEST_KEYTEST_VAL" );
        ValidationResult result = new ValidationResult();
        processor.process( result, fieldValue );
        Assert.assertFalse( result.isValid() );
    }

    @Test
    public void testInValidEmptyValue() {
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "CUSTOM_FIELD", "  " );
        ValidationResult result = new ValidationResult();
        processor.process( result, fieldValue );
        Assert.assertFalse( result.isValid() );
    }

    @Test(expected = AssertionError.class)
    public void testInValidNullValue() {
        ManifestFieldValue fieldValue = createFieldValue( ManifestFieldType.META, "CUSTOM_FIELD", null );
    }
}