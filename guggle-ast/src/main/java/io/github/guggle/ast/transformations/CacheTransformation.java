package io.github.guggle.ast.transformations;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.*;
import org.codehaus.groovy.transform.*;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import io.github.guggle.api.*;
import java.util.concurrent.TimeUnit;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CacheTransformation extends AbstractASTTransformation {

    AnnotationNode annotationNode;
    MethodNode methodNode;
    ClassNode declaringClassNode;
    SourceUnit sourceUnit;

    Expires expires;
    Refresh refresh;
    long interval;
    TimeUnit timeUnit;
    int maxSize;

    InnerClassNode base;
    InnerClassNode immutable;
    InnerClassNode search;
    MethodNode rewrittenMethod;
    InnerClassNode function;
    FieldNode functionFieldNode;
    FieldNode cacheField;
    
    public void visit(final ASTNode[] astNodes, final SourceUnit sourceUnit) {
        init(astNodes, sourceUnit);
        this.sourceUnit = sourceUnit;
        initNodes(astNodes);
    }

    void initNodes(ASTNode[] astNodes) {
        this.annotationNode = (AnnotationNode) astNodes[0];
        this.methodNode = (MethodNode) astNodes[1];
        this.declaringClassNode = methodNode.getDeclaringClass();

        this.expires = populateExpires();
        this.refresh = populateRefresh();
        this.interval = populateInterval();
        this.timeUnit = populateTimeUnit();
        this.maxSize = populateMaxSize();

        final CacheGeneration generator = new CacheGeneration(methodNode, lifetimeConstructor());
        generator.pre();
        generator.generate();
        generator.post();
        sourceUnit.getAST().addClass(generator.baseNode);
        sourceUnit.getAST().addClass(generator.immutableNode);
        sourceUnit.getAST().addClass(generator.searchNode);
        sourceUnit.getAST().addClass(generator.functionNode);
    }

    Expires populateExpires() {
        final Expression e = annotationNode.getMember("expires");
        if(e == null) {
            return Expires.NEVER;
        }
        else {
            final PropertyExpression pe = (PropertyExpression) e;
            final ConstantExpression ce = (ConstantExpression) pe.getProperty();
            return Expires.valueOf((String) ce.getValue());
        }
    }

    Refresh populateRefresh() {
        final Expression e = annotationNode.getMember("refresh");
        if(e == null) {
            return Refresh.NONE;
        }
        else {
            final PropertyExpression pe = (PropertyExpression) e;
            final ConstantExpression ce = (ConstantExpression) pe.getProperty();
            return Refresh.valueOf((String) ce.getValue());
        }
    }

    long populateInterval() {
        final Expression e = annotationNode.getMember("interval");
        if(e == null) {
            return Long.MAX_VALUE;
        }
        else {
            final PropertyExpression pe = (PropertyExpression) e;
            final ConstantExpression ce = (ConstantExpression) pe.getProperty();
            return Long.parseLong((String) ce.getValue());
        }
    }

    TimeUnit populateTimeUnit() {
        final Expression e = annotationNode.getMember("timeUnit");
        if(e == null) {
            return TimeUnit.MILLISECONDS;
        }
        else {
            final PropertyExpression pe = (PropertyExpression) e;
            final ConstantExpression ce = (ConstantExpression) pe.getProperty();
            return TimeUnit.valueOf((String) ce.getValue());
        }
    }

    int populateMaxSize() {
        final Expression e = annotationNode.getMember("maxSize");
        if(e == null) {
            return Integer.MAX_VALUE;
        }
        else {
            final PropertyExpression pe = (PropertyExpression) e;
            final ConstantExpression ce = (ConstantExpression) pe.getProperty();
            return Integer.parseInt((String) ce.getValue());
        }
    }

    ConstructorCallExpression lifetimeConstructor() {
        final ArgumentListExpression alist = new ArgumentListExpression();

        final ClassNode expiresNode = ClassHelper.makeWithoutCaching(Expires.class, false);
        alist.addExpression(propX(classX(expiresNode), expires.name()));

        final ClassNode refreshNode = ClassHelper.makeWithoutCaching(Refresh.class, false);
        alist.addExpression(propX(classX(refreshNode), refresh.name()));

        alist.addExpression(constX(interval, true));
        final ClassNode timeUnitNode = ClassHelper.makeWithoutCaching(TimeUnit.class, false);
        alist.addExpression(propX(classX(TimeUnit.class), timeUnit.name()));

        alist.addExpression(constX(maxSize, false));

        final ClassNode lifetimeNode = ClassHelper.makeWithoutCaching(Lifetime.class, false);
        return new ConstructorCallExpression(lifetimeNode, alist);
    }
}
