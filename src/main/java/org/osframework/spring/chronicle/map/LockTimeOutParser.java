package org.osframework.spring.chronicle.map;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for parsing of a lock timeout expression and provision of the result duration and {@code TimeUnit}.
 * <h2>Timeout expression</h2>
 * <p>Valid timeout expressions are composed of a long amount and a standard time unit abbreviation. Examples:</p>
 * <pre>
 * 100ns - 100 nanoseconds
 * 300µs - 300 microseconds
 * 500ms - 500 milliseconds
 *   10s -  10 seconds
 *    2m -   2 minutes
 *    1h -   1 hour
 * </pre>
 * <p>This class is package private; it is intended only for use by instances of {@code ChronicleMapBuilderBean}.</p>
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @version 0.0.1
 * @since 2015-06-17
 * @see net.openhft.chronicle.map.ChronicleMapBuilder#lockTimeOut(long, TimeUnit)
 */
class LockTimeOutParser {

    static final Pattern REGEX = Pattern.compile("^(\\d+)(\\S{1,2})$");

    private final String timeOutExpr;
    private long amount = -1L;
    private TimeUnit unit = null;

    /**
     * Create a new parser of the specified lock timeout expression.
     *
     * @param timeOutExpr lock timeout expression
     * @throws IllegalArgumentException if expression is null
     */
    LockTimeOutParser(String timeOutExpr) {
        if (null == timeOutExpr) {
            throw new IllegalArgumentException("Timeout expression argument cannot be null");
        }
        this.timeOutExpr = timeOutExpr;
        Matcher m = REGEX.matcher(timeOutExpr);
        if (m.matches()) {
            this.amount = Long.parseLong(m.group(1));
            this.unit = stringToTimeUnit(m.group(2));
        }
    }

    /**
     * Determine if this parser provides valid timeout values.
     *
     * @return true if this parser contains valid timeout values, false otherwise
     */
    public boolean valid() {
        return (null != unit && 0 < amount);
    }

    /**
     * Get long integer amount of time units of the lock timeout.
     *
     * @return amount of time units
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Get time unit of the lock timeout.
     *
     * @return unit of time
     */
    public TimeUnit getUnit() {
        return unit;
    }

    private TimeUnit stringToTimeUnit(String abbrv) {
        if ("ns".equals(abbrv)) return TimeUnit.NANOSECONDS;
        if ("µs".equals(abbrv)) return TimeUnit.MICROSECONDS;
        if ("ms".equals(abbrv)) return TimeUnit.MILLISECONDS;
        if ("s".equals(abbrv)) return TimeUnit.SECONDS;
        if ("m".equals(abbrv)) return TimeUnit.MINUTES;
        if ("h".equals(abbrv)) return TimeUnit.HOURS;
        return null;
    }

}
