package io.github.guggle.ast;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import java.lang.annotation.*;
import io.github.guggle.ast.transformations.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass(classes={RichKeyTransformation.class})
public @interface ImmutableKey {

}
