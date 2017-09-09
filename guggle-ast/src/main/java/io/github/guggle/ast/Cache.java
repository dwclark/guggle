package io.github.guggle.ast;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import java.lang.annotation.*;
import io.github.guggle.ast.transformations.*;
import io.github.guggle.api.*;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
@GroovyASTTransformationClass(classes={CacheTransformation.class})
public @interface Cache {
    Expires expires() default Expires.NEVER;
    Refresh refresh() default Refresh.NONE;
    long interval() default Long.MAX_VALUE;
    TimeUnit units() default TimeUnit.MINUTES;
    int maxSize() default Integer.MAX_VALUE;
}
