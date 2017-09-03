package io.github.guggle.api;

import io.github.guggle.utils.TimeUnits;

public final class Lifetime {

    public static final Expires EXPIRES = Expires.NEVER;
    public static final Refresh REFRESH = Refresh.NONE;
    public static final TimeUnits UNITS = TimeUnits.minutes(Long.MAX_VALUE);
    public static final Integer MAX_SIZE = Integer.MAX_VALUE;
    
    public static class Builder {
        
        private Expires expires;
        private Refresh refresh;
        private TimeUnits units;
        private Integer maxSize;

        public Builder from(final Lifetime val) {
            expires = val.expires;
            refresh = val.refresh;
            units = val.units;
            maxSize = val.maxSize;
            
            return this;
        }

        public Builder merge(final Lifetime val) {
            if(val.expires != null) expires = val.expires;
            if(val.refresh != null) refresh = val.refresh;
            if(val.units != null) units = val.units;
            if(val.maxSize != null) maxSize = val.maxSize;

            return this;
        }

        public Builder resolveNulls() {
            if(expires == null) expires = EXPIRES;
            if(refresh == null) refresh = REFRESH;
            if(units == null) units = UNITS;
            if(maxSize == null) maxSize = MAX_SIZE;

            return this;
        }

        public Lifetime build() {
            return new Lifetime(expires, refresh, units, maxSize);
        }

        public Builder expires(final Expires val) {
            expires = val;
            return this;
        }

        public Builder refresh(final Refresh val) {
            refresh = val;
            return this;
        }

        public Builder units(final TimeUnits val) {
            units = val;
            return this;
        }

        public Builder maxSize(final Integer val) {
            maxSize = val;
            return this;
        }
    }

    private final Expires expires;
    private final Refresh refresh;
    private final TimeUnits units;
    private final Integer maxSize;
    
    public Lifetime(final Expires expires,
                    final Refresh refresh,
                    final TimeUnits units,
                    final Integer maxSize) {
        this.expires = expires;
        this.refresh = refresh;
        this.units = units;
        this.maxSize = maxSize;
    }

    
    public Expires getExpires() {
        return expires;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public TimeUnits getUnits() {
        return units;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public Builder toBuilder() {
        return new Builder().from(this);
    }

    public static Builder builder() {
        return new Builder();
    }
}
