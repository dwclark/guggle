package io.github.guggle.utils;

import java.util.concurrent.TimeUnit;
import java.time.temporal.ChronoUnit;

public class TimeUnits {
    
    private final long interval;
    private final TimeUnit units;
    
    public TimeUnits(final long interval, final TimeUnit units) {
        this.interval = interval;
        this.units = units;
    }

    public TimeUnits(final long interval, final ChronoUnit units) {
        this.interval = interval;
        this.units = toTimeUnit(units);
    }

    public long getInterval() {
        return interval;
    }

    public TimeUnit getTimeUnit() {
        return units;
    }

    public ChronoUnit getChronoUnit() {
        return toChronoUnit(units);
    }
    
    public static ChronoUnit toChronoUnit(final TimeUnit units) {
        switch(units) {
        case NANOSECONDS: return ChronoUnit.NANOS;
        case MICROSECONDS: return ChronoUnit.MICROS;
        case MILLISECONDS: return ChronoUnit.MILLIS;
        case SECONDS: return ChronoUnit.SECONDS;
        case MINUTES: return ChronoUnit.MINUTES;
        case HOURS: return ChronoUnit.HOURS;
        case DAYS: return ChronoUnit.DAYS;
        default:
            throw new IllegalArgumentException("Unmatched switch case " + units);
        }
    }

    public static TimeUnit toTimeUnit(final ChronoUnit units) {
        switch(units) {
        case NANOS: return TimeUnit.NANOSECONDS;
        case MICROS: return TimeUnit.MICROSECONDS;
        case MILLIS: return TimeUnit.MILLISECONDS;
        case SECONDS: return TimeUnit.SECONDS;
        case MINUTES: return TimeUnit.MINUTES;
        case HOURS: return TimeUnit.HOURS;
        case DAYS: return TimeUnit.DAYS;
        default:
            throw new IllegalArgumentException("Unmatched switch case " + units);
        }
    }

    public static TimeUnits seconds(final long interval) {
        return new TimeUnits(interval, TimeUnit.SECONDS);
    }

    public static TimeUnits minutes(final long interval) {
        return new TimeUnits(interval, TimeUnit.MINUTES);
    }

    public static TimeUnits milliseconds(final long interval) {
        return new TimeUnits(interval, TimeUnit.MILLISECONDS);
    }

    public static TimeUnits microseconds(final long interval) {
        return new TimeUnits(interval, TimeUnit.MICROSECONDS);
    }
}
