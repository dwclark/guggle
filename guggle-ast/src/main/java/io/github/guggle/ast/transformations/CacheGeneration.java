package io.github.guggle.ast.transformations;

import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import java.util.function.*;
import io.github.guggle.api.*;
import io.github.guggle.utils.*;

class CacheGeneration extends BaseGeneration {

    final String functionClassName;
    final String forwardingMethodName;
    final ConstructorCallExpression lifetimeConstructorCall;
    
    MethodNode forwardingMethodNode;
    InnerClassNode functionNode;
    String functionFieldName;
    FieldNode functionFieldNode;
    String cacheFieldName;
    FieldNode cacheFieldNode;
    
    public CacheGeneration(final MethodNode methodNode, final ConstructorCallExpression lifetimeConstructorCall) {
        super(methodNode);
        this.lifetimeConstructorCall = lifetimeConstructorCall;
        this.forwardingMethodName = fieldName("_", "");
        this.functionFieldName = fieldName("", "Function");
        this.functionClassName = className("Function");
        this.cacheFieldName = fieldName("", "Cache");
    }

    public Expression getTemporaryCode() {
        if(returnType == ClassHelper.boolean_TYPE) {
            return constX(false, true);
        }
        else if(returnType == ClassHelper.byte_TYPE) {
            return castX(ClassHelper.byte_TYPE, constX(0, true));
        }
        else if(returnType == ClassHelper.short_TYPE) {
            return castX(ClassHelper.short_TYPE, constX(0, true));
        }
        else if(returnType == ClassHelper.int_TYPE) {
            return constX(0, true);
        }
        else if(returnType == ClassHelper.long_TYPE) {
            return constX(0L, true);
        }
        else if(returnType == ClassHelper.float_TYPE) {
            return constX(0f, true);
        }
        else if(returnType == ClassHelper.double_TYPE) {
            return constX(0d, true);
        }
        else {
            return constX(null);
        }
    }

    public class CacheInfo {
        private String _methodName;
        private ClassNode _classNode;

        private String _cacheMethodName;
        private ClassNode _cacheTypeNode;
        private ArgumentListExpression _cacheFactoryArgs;

        public String getMethodName() {
            if(_methodName == null) {
                populateFunctionInfo();
            }

            return _methodName;
        }

        public ClassNode getClassNode() {
            if(_classNode == null) {
                populateFunctionInfo();
            }

            return _classNode;
        }

        public String getCacheMethodName() {
            if(_cacheMethodName == null) {
                populateCacheInfo();
            }

            return _cacheMethodName;
        }

        public ClassNode getCacheTypeNode() {
            if(_cacheTypeNode == null) {
                populateCacheInfo();
            }

            return _cacheTypeNode;
        }

        public ArgumentListExpression getCacheFactoryArgs() {
            if(_cacheFactoryArgs == null) {
                populateCacheInfo();
            }

            return _cacheFactoryArgs;
        }

        private void populateFunctionInfo() {
            if(returnType == ClassHelper.boolean_TYPE) {
                _methodName = "applyAsBoolean";
                final ClassNode tmp = ClassHelper.make(ToBooleanFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.byte_TYPE) {
                _methodName = "applyAsByte";
                final ClassNode tmp = ClassHelper.make(ToByteFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.short_TYPE) {
                _methodName = "applyAsShort";
                final ClassNode tmp = ClassHelper.make(ToShortFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.int_TYPE) {
                _methodName = "applyAsInt";
                final ClassNode tmp = ClassHelper.make(ToIntFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.long_TYPE) {
                _methodName = "applyAsLong";
                final ClassNode tmp = ClassHelper.make(ToLongFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.float_TYPE) {
                _methodName = "applyAsFloat";
                final ClassNode tmp = ClassHelper.make(ToFloatFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else if(returnType == ClassHelper.double_TYPE) {
                _methodName = "applyAsDouble";
                final ClassNode tmp = ClassHelper.make(ToDoubleFunction.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode));
            }
            else {
                _methodName = "apply";
                final ClassNode tmp = ClassHelper.makeWithoutCaching(Function.class);
                _classNode = GenericsUtils.makeClassSafeWithGenerics(tmp, new GenericsType(baseNode), new GenericsType(copy(returnType)));
            }
        }
        
        void populateCacheInfo() {
            _cacheFactoryArgs = new ArgumentListExpression();
            final FieldNode methodIdFieldNode = baseNode.getFields().stream().filter(f -> f.getName().equals("METHOD_ID")).findFirst().get();
            if(methodIdFieldNode == null) {
                addError("Could not find METHOD_ID field", methodNode);
            }
            
            final FieldExpression methodIdFieldExpr = new FieldExpression(methodIdFieldNode);
            _cacheFactoryArgs.addExpression(classX(baseNode));
            _cacheFactoryArgs.addExpression(methodIdFieldExpr);
            _cacheFactoryArgs.addExpression(fieldX(functionFieldNode));
            _cacheFactoryArgs.addExpression(lifetimeConstructorCall);
            
            if(returnType == ClassHelper.boolean_TYPE ||
               returnType == ClassHelper.byte_TYPE ||
               returnType == ClassHelper.short_TYPE ||
               returnType == ClassHelper.int_TYPE) {
                _cacheMethodName = "intView";
                GenericsType[] generics = new GenericsType[] { new GenericsType(baseNode) };
                _cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(IntCacheView.class, false), generics);
            }
            else if(returnType == ClassHelper.long_TYPE) {
                _cacheMethodName = "longView";
                GenericsType[] generics = new GenericsType[] { new GenericsType(baseNode) };
                _cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(LongCacheView.class, false), generics);
            }
            else if(returnType == ClassHelper.float_TYPE ||
                    returnType == ClassHelper.double_TYPE) {
                _cacheMethodName = "doubleView";
                GenericsType[] generics = new GenericsType[] { new GenericsType(baseNode) };
                _cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(DoubleCacheView.class, false), generics);
            }
            else {
                _cacheMethodName = "objectView";
                _cacheFactoryArgs.addExpression(classX(returnType));
                GenericsType[] generics = new GenericsType[2];
                generics[0] = new GenericsType(baseNode);
                generics[1] = new GenericsType(copy(returnType));
                _cacheTypeNode = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.makeWithoutCaching(ObjectCacheView.class, false), generics);
            }
        }
    }

    @Override
    public void pre() {
        super.pre();
        final int modifiers = ACC_PRIVATE | (methodNode.isStatic() ? ACC_STATIC : ACC_PRIVATE);
        this.forwardingMethodNode = new MethodNode(forwardingMethodName, modifiers,
                                                   copy(returnType), cloneParams(parameters),
                                                   ClassNode.EMPTY_ARRAY, methodNode.getCode());
        outerClassNode.addMethod(forwardingMethodNode);
        methodNode.setCode(returnS(getTemporaryCode()));
    }

    public void generate() {
        super.generate();

        final CacheInfo cacheInfo = new CacheInfo();
        functionClass(cacheInfo);
        functionField();
        cacheField(cacheInfo);
    }

    void functionClass(final CacheInfo cacheInfo) {
        this.functionNode = new InnerClassNode(outerClassNode, functionClassName,
                                               ACC_PUBLIC | ACC_STATIC,
                                               ClassHelper.OBJECT_TYPE);
        
        functionNode.addInterface(cacheInfo.getClassNode());

        final Parameter[] functionParameters = new Parameter[1];
        functionParameters[0] = new Parameter(baseNode, "val");
        functionParameters[0].setModifiers(ACC_FINAL);
        
        final ArgumentListExpression alist = new ArgumentListExpression();
        for(int i = 0; i < parameters.length; ++i) {
            final MethodCallExpression mcall = new MethodCallExpression(varX(parameters[0]), "m" + i, ArgumentListExpression.EMPTY_ARGUMENTS);
            mcall.setImplicitThis(false);
            alist.addExpression(mcall);
        }

        final ClassNode copied = copy(outerClassNode);
        if(methodNode.isStatic()) {
            StaticMethodCallExpression mcall = new StaticMethodCallExpression(outerClassNode, forwardingMethodName, alist);
            functionNode.addMethod(new MethodNode(cacheInfo.getMethodName(), ACC_PUBLIC, returnType,
                                                  functionParameters, ClassNode.EMPTY_ARRAY, returnS(mcall)));
        }
        else {
            FieldNode fnode = new FieldNode("outer", ACC_PRIVATE | ACC_FINAL,
                                            copied, functionNode, EmptyExpression.INSTANCE);
            functionNode.addField(fnode);
            Parameter[] constructorParameters = new Parameter[1];
            constructorParameters[0] = new Parameter(copied, "val");
            constructorParameters[0].setModifiers(ACC_FINAL);
            ConstructorNode cnode = new ConstructorNode(ACC_PUBLIC, constructorParameters, ClassNode.EMPTY_ARRAY,
                                                        assignS(fieldX(fnode), varX(constructorParameters[0])));
            functionNode.addConstructor(cnode);

            FieldExpression fexpr = new FieldExpression(fnode);
            final MethodCallExpression mcall = new MethodCallExpression(fexpr, methodNode.getName(), alist);
            mcall.setImplicitThis(false);
            mcall.setMethodTarget(methodNode);
            mcall.setGenericsTypes(new GenericsType[] { new GenericsType(baseNode) });
            functionNode.addMethod(new MethodNode(cacheInfo.getMethodName(), ACC_PUBLIC, returnType,
                                         functionParameters, ClassNode.EMPTY_ARRAY, returnS(mcall)));
        }
    }

    void functionField() {
        if(functionFieldName == null) {
            throw new IllegalStateException("functionFieldName has not been populated");
        }
        
        if(methodNode.isStatic()) {
            this.functionFieldNode = new FieldNode(functionFieldName, ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                                                   functionNode, outerClassNode, new ConstructorCallExpression(functionNode, ArgumentListExpression.EMPTY_ARGUMENTS));
            outerClassNode.addField(functionFieldNode);
        }
        else {
            this.functionFieldNode = new FieldNode(functionFieldName, ACC_PRIVATE | ACC_FINAL,
                                                   functionNode, outerClassNode,
                                                   new ConstructorCallExpression(functionNode, args(varX("this"))));
            outerClassNode.addField(functionFieldNode);
        }
    }
    
    void cacheField(final CacheInfo cacheInfo) {
        if(cacheFieldName == null) {
            throw new IllegalStateException("cacheFieldName has not been populated");
        }
        
        if(functionFieldNode == null) {
            throw new IllegalStateException("functionFieldNode has not been populated");
        }

        final ClassNode cacheRegistryNode = ClassHelper.makeWithoutCaching(CacheRegistry.class, false);
        if(cacheRegistryNode == null) {
            throw new IllegalStateException("cacheRegistryNode cannot be null");
        }
        
        final StaticMethodCallExpression instanceExpr = new StaticMethodCallExpression(cacheRegistryNode, "instance",
                                                                                       ArgumentListExpression.EMPTY_ARGUMENTS);
        
        final int modifiers = ACC_PRIVATE | ACC_FINAL | ((methodNode.isStatic() ? ACC_STATIC : 0));
        final MethodCallExpression mcall = new MethodCallExpression(instanceExpr, cacheInfo.getCacheMethodName(), cacheInfo.getCacheFactoryArgs());
        mcall.setImplicitThis(false);
        this.cacheFieldNode = new FieldNode(cacheFieldName, modifiers,
                                            cacheInfo.getCacheTypeNode(),
                                            outerClassNode,
                                            mcall);
        outerClassNode.addField(cacheFieldNode);
    }
    
    @Override
    public void post() {
        super.post();

        final BlockStatement block = new BlockStatement();

        // declare the searcher
        final ClassNode typeInstanceNode = ClassHelper.makeWithoutCaching(TypeInstance.class);
        final ArgumentListExpression smCallArgs = new ArgumentListExpression();
        smCallArgs.addExpression(classX(searchNode));
        StaticMethodCallExpression smCall = new StaticMethodCallExpression(typeInstanceNode, "instance", smCallArgs);
        final CastExpression caster = new CastExpression(searchNode, smCall);
        caster.setStrict(true);
        final VariableExpression searcherVar = new VariableExpression("searcher", searchNode);
        block.addStatement(declS(searcherVar, caster));

        //set the parameters on the searcher
        final MethodCallExpression setterCall = new MethodCallExpression(searcherVar, "set", args(parameters));
        setterCall.setImplicitThis(false);
        block.addStatement(new ExpressionStatement(setterCall));
        
        final MethodCallExpression mcall = new MethodCallExpression(fieldX(cacheFieldNode), "value", varX("searcher"));
        mcall.setImplicitThis(false);
        methodNode.setCode(returnS(mcall));
    }
}
