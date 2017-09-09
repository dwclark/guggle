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
import static io.github.guggle.ast.transformations.Utils.*;
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
        
        this.base = Utils.abstractBaseClass(methodNode);
        sourceUnit.getAST().addClass(base);
        this.immutable = Utils.immutableClass(methodNode, base);
        sourceUnit.getAST().addClass(immutable);
        this.search = Utils.searchClass(methodNode, base, immutable);
        sourceUnit.getAST().addClass(search);
        // this.cacheField = makeCacheField();
        // this.rewrittenMethod = Utils.rewriteMethod(methodNode, cacheField, search);
        // this.function = Utils.functionClass(methodNode, base);
        // sourceUnit.getAST().addClass(function);
        // this.functionFieldNode = makeFunctionField();
    }

    FieldNode makeFunctionField() {
        int fieldModifiers;
        Expression initializer;
        if(methodNode.isStatic()) {
            fieldModifiers = ACC_PUBLIC | ACC_FINAL | ACC_STATIC;
            initializer = new ConstructorCallExpression(function, ArgumentListExpression.EMPTY_ARGUMENTS);
        }
        else {
            fieldModifiers = ACC_PUBLIC | ACC_FINAL;
            final ArgumentListExpression args = new ArgumentListExpression();
            args.addExpression(varX("this"));
            initializer = new ConstructorCallExpression(function, args);
        }
        
        FieldNode ret = new FieldNode(Utils.fieldName(methodNode, Utils.FUNCTION),
                                      fieldModifiers, this.function, declaringClassNode, initializer);
        declaringClassNode.addField(ret);
        return ret;
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
    
    FieldNode makeCacheField() {
        final ClassNode returnType = Utils.copyClassNode(methodNode.getReturnType());
        final ClassExpression keyExpr = classX(base);
        final FieldNode methodIdFieldNode = base.getFields().stream().filter(f -> f.getName().equals("METHOD_ID")).findFirst().get();
        if(methodIdFieldNode == null) {
            addError("Could not find METHOD_ID field", methodNode);
        }
        
        final FieldExpression methodIdFieldExpr = new FieldExpression(methodIdFieldNode);
        final ClassNode cacheRegistryNode = ClassHelper.makeWithoutCaching(CacheRegistry.class, false);
        final StaticMethodCallExpression instanceExpr = new StaticMethodCallExpression(cacheRegistryNode, "instance",
                                                                                       ArgumentListExpression.EMPTY_ARGUMENTS);
        String methodName;
        final ArgumentListExpression args = new ArgumentListExpression();
        ClassNode cacheTypeNode;
        args.addExpression(keyExpr);
        args.addExpression(methodIdFieldExpr);
        args.addExpression(fieldX(functionFieldNode));
        args.addExpression(lifetimeConstructor());
        
        if(returnType == ClassHelper.boolean_TYPE ||
           returnType == ClassHelper.byte_TYPE ||
           returnType == ClassHelper.short_TYPE ||
           returnType == ClassHelper.int_TYPE) {
            methodName = "intView";
            GenericsType[] generics = new GenericsType[] { new GenericsType(base) };
            cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(IntCacheView.class, false), generics);
        }
        else if(returnType == ClassHelper.long_TYPE) {
            methodName = "longView";
            GenericsType[] generics = new GenericsType[] { new GenericsType(base) };
            cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(LongCacheView.class, false), generics);
        }
        else if(returnType == ClassHelper.float_TYPE ||
                returnType == ClassHelper.double_TYPE) {
            methodName = "doubleView";
            GenericsType[] generics = new GenericsType[] { new GenericsType(base) };
            cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(DoubleCacheView.class, false), generics);
        }
        else {
            methodName = "objectView";
            args.addExpression(classX(returnType));
            GenericsType[] generics = new GenericsType[2];
            generics[0] = new GenericsType(base);
            generics[1] = new GenericsType(returnType);
            cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(ObjectCacheView.class, false), generics);
        }

        cacheTypeNode = ClassHelper.OBJECT_TYPE;
        final MethodCallExpression cacheInitializer = new MethodCallExpression(instanceExpr, methodName, args);
        final int modifiers = ACC_PUBLIC | ACC_FINAL | (methodNode.isStatic() ? ACC_STATIC : 0);
        FieldNode retNode = new FieldNode(Utils.fieldName(methodNode, Utils.CACHE), modifiers, cacheTypeNode,
                                          declaringClassNode, cacheInitializer);
        declaringClassNode.addField(retNode);
        return retNode;
    }
    
    //TODO: Add generation functions
    //TODO: Add cache variables
    //TODO: Re-write methods to forward to caches
}
