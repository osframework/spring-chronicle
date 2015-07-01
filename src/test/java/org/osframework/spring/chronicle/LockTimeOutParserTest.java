package org.osframework.spring.chronicle;

import org.osframework.spring.chronicle.LockTimeOutParser;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@code LockTimeOutParser}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @version 0.0.1
 * @since 2015-06-17
 */
public class LockTimeOutParserTest {

    @Test(dataProvider = "validTestParamSets", groups = "valid")
    public void testValid(String timeoutExpression, boolean expected) {
        LockTimeOutParser ltop = new LockTimeOutParser(timeoutExpression);
        assertEquals(ltop.valid(), expected);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, groups = "valid")
    public void testValidWithNullExpression() {
        LockTimeOutParser ltop = new LockTimeOutParser(null);
        assertEquals(ltop.valid(), false);
    }

    @Test(dataProvider = "getAmountTestParamSets", dependsOnGroups = "valid")
    public void testGetAmount(String timeoutExpression, long expectedAmount) {
        LockTimeOutParser ltop = new LockTimeOutParser(timeoutExpression);
        assertTrue(ltop.valid());
        assertEquals(ltop.getAmount(), expectedAmount);
    }

    @Test(dataProvider = "getUnitTestParamSets", dependsOnGroups = "valid")
    public void testGetUnit(String timeoutExpression, TimeUnit expectedUnit) {
        LockTimeOutParser ltop = new LockTimeOutParser(timeoutExpression);
        assertTrue(ltop.valid());
        assertEquals(ltop.getUnit(), expectedUnit);
    }

    @DataProvider
    public Object[][] validTestParamSets() {
        return new Object[][] {
          new Object[] { "5ns", true },
          new Object[] { "12h", true },
          new Object[] { "250µs", true },
          new Object[] { "-3s", false },
          new Object[] { "", false }
        };
    }

    @DataProvider
    public Object[][] getAmountTestParamSets() {
        return new Object[][] {
                new Object[] { "5ns", 5L },
                new Object[] { "12h", 12L },
                new Object[] { "250µs", 250L }
        };
    }

    @DataProvider
    public Object[][] getUnitTestParamSets() {
        return new Object[][] {
                new Object[] { "5ns", TimeUnit.NANOSECONDS },
                new Object[] { "12h", TimeUnit.HOURS },
                new Object[] { "250µs", TimeUnit.MICROSECONDS }
        };
    }

}
