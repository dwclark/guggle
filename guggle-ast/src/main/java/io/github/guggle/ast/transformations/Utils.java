package io.github.guggle.ast.transformations;

import io.github.guggle.api.Permanent;
import io.github.guggle.api.MethodId;
import io.github.guggle.utils.Fnv;
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

class Utils extends AbstractASTTransformation {

    public static final String BASE = "Base";
    public static final String SEARCH = "Search";
    public static final String IMMUTABLE = "Immutable";
    
    public static String innerClassName(final MethodNode methodNode, final String suffix) {
        return methodNode.getDeclaringClass().getName() + "$" +
            methodNode.getName().substring(0,1).toUpperCase() +
            methodNode.getName().substring(1) + suffix;
    }
    
    public void visit(ASTNode[] nodes, SourceUnit unit) {
        throw new UnsupportedOperationException("Not really an ast transformation, this is a convenient/safe" +
                                                "way to get access to the Opcodes");
    }
    
    static ClassNode copyClassNode(final ClassNode classNode) {
        if(ClassHelper.isPrimitiveType(classNode)) {
            return classNode;
        }
        
        if(classNode.getGenericsTypes() == null || classNode.getGenericsTypes().length == 0) {
            return ClassHelper.makeWithoutCaching(classNode.getName());
        }

        GenericsType[] generics = classNode.getGenericsTypes();
        GenericsType[] copiedGenerics = new GenericsType[generics.length];
        for(int i = 0; i < generics.length; ++i) {
            copiedGenerics[i] = copyGenericsType(generics[i]);
        }
        
        ClassNode ret = GenericsUtils.makeClassSafeWithGenerics(classNode, copiedGenerics);
        return ret;
    }

    static GenericsType copyGenericsType(final GenericsType genericsType) {
        ClassNode lowerBound = null;
        if(genericsType.getLowerBound() != null) {
            lowerBound = copyClassNode(genericsType.getLowerBound());
        }

        ClassNode[] upperBounds = null;
        if(genericsType.getUpperBounds() != null) {
            ClassNode[] original = genericsType.getUpperBounds();
            upperBounds = new ClassNode[original.length];
            for(int i = 0; i < original.length; ++i) {
                upperBounds[i] = copyClassNode(original[i]);
            }
        }
        
        GenericsType copied = new GenericsType(copyClassNode(genericsType.getType()),
                                               upperBounds, lowerBound);
        copied.setWildcard(genericsType.isWildcard());
        return copied;
    }

    static Parameter[] copyParameters(final Parameter[] original) {
        Parameter[] copied = new Parameter[original.length];
        for(int i = 0; i < original.length; ++i) {
            copied[i] = param(copyClassNode(original[i].getType()), original[i].getName());
        }

        return copied;
    }

    static ClassNode[] parameterTypes(final Parameter[] original) {
        final ClassNode[] ret = new ClassNode[original.length];
        for(int i = 0; i < original.length; ++i) {
            ret[i] = original[i].getType();
        }

        return ret;
    }

    static FieldNode[] fieldNodes(final ClassNode owner, final ClassNode[] types, final boolean immutable) {
        final FieldNode[] ret = new FieldNode[types.length];
        for(int i = 0; i < types.length; ++i) {
            final int modifiers = immutable ? (ACC_PRIVATE | ACC_FINAL) : ACC_PRIVATE;
            ret[i] = new FieldNode("f" + i, modifiers, copyClassNode(types[i]), owner, EmptyExpression.INSTANCE);
            owner.addField(ret[i]);
        }

        return ret;
    }

    static MethodNode[] abstractAccessors(final ClassNode owner, final ClassNode[] types) {
        final MethodNode[] ret = new MethodNode[types.length];
        for(int i = 0; i < types.length; ++i) {
            ret[i] = new MethodNode("m" + i, ACC_PUBLIC | ACC_ABSTRACT, copyClassNode(types[i]),
                                    Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, EmptyStatement.INSTANCE);
            owner.addMethod(ret[i]);
        }

        return ret;
    }

    static MethodNode[] accessors(final ClassNode owner, final FieldNode[] fieldNodes) {
        final MethodNode[] ret = new MethodNode[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            ret[i] = new MethodNode("m" + i, ACC_PUBLIC, copyClassNode(fieldNodes[i].getType()),
                                    Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                    returnS(fieldX(fieldNodes[i])));
            owner.addMethod(ret[i]);
        }

        return ret;
    }

    static Parameter[] setterParameters(final FieldNode[] fieldNodes) {
        final Parameter[] parameters = new Parameter[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            parameters[i] = new Parameter(copyClassNode(fieldNodes[i].getType()), "a" + i);
        }

        return parameters;
    }
    
    static BlockStatement setterAssignments(final FieldNode[] fieldNodes) {
        final Statement[] assignments = new Statement[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            assignments[i] = assignS(fieldX(fieldNodes[i]), varX("a" + i));
        }

        return block(assignments);
    }

    static MethodNode setter(final ClassNode owner, final FieldNode[] fieldNodes) {
        final MethodNode methodNode = new MethodNode("set", ACC_PUBLIC, ClassHelper.VOID_TYPE,
                                                     setterParameters(fieldNodes), ClassNode.EMPTY_ARRAY,
                                                     setterAssignments(fieldNodes));
        owner.addMethod(methodNode);
        return methodNode;
    }

    static ConstructorNode constructor(final ClassNode owner, final FieldNode[] fieldNodes) {
        final ConstructorNode cnode = new ConstructorNode(ACC_PUBLIC,
                                                          setterParameters(fieldNodes), ClassNode.EMPTY_ARRAY,
                                                          setterAssignments(fieldNodes));
        owner.addConstructor(cnode);
        return cnode;
    }

    static MethodNode equalsMethod(final ClassNode thisNode, final MethodNode[] methodNodes) {
        ClassNode objectsNode = ClassHelper.makeWithoutCaching(java.util.Objects.class);
        final String o = "o";
        final String rhs = "rhs";
        BlockStatement block = new BlockStatement();
        block.addStatement(ifS(notX(isInstanceOfX(varX(o), thisNode)), returnS(constX(false, true))));
        CastExpression cast = new CastExpression(thisNode, varX(o));
        cast.setStrict(true);
        VariableExpression rhsVar = new VariableExpression("rhs", thisNode);
        block.addStatement(declS(rhsVar, cast));

        Expression[] comparisons = new Expression[methodNodes.length];
        for(int i = 0; i < methodNodes.length; ++i) {
            MethodNode mnode = methodNodes[i];
            MethodCallExpression myMethod = new MethodCallExpression(varX("this"), mnode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS);
            myMethod.setImplicitThis(true);
            MethodCallExpression theirMethod = new MethodCallExpression(rhsVar, mnode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS);
            myMethod.setImplicitThis(false);
            
            if(ClassHelper.isPrimitiveType(mnode.getReturnType())) {
                comparisons[i] = eqX(myMethod, theirMethod);
            }
            else {
                comparisons[i] = new StaticMethodCallExpression(objectsNode, "equals", args(myMethod, theirMethod));
            }
        }

        for(int i = 0; i < comparisons.length; ++i) {
            block.addStatement(ifS(notX(comparisons[i]), returnS(constX(false, true))));
        }

        block.addStatement(returnS(constX(true, true)));

        final MethodNode method = new MethodNode("equals", ACC_PUBLIC, ClassHelper.boolean_TYPE,
                                                 params(param(ClassHelper.OBJECT_TYPE, o)), ClassNode.EMPTY_ARRAY,
                                                 block);
        thisNode.addMethod(method);
        return method;
    }

    static MethodNode hashCodeMethod(final ClassNode thisNode, final MethodNode[] methodNodes) {
        final ClassNode fnvNode = ClassHelper.make(Fnv.class);
        StaticMethodCallExpression firstCall = new StaticMethodCallExpression(fnvNode, "start", ArgumentListExpression.EMPTY_ARGUMENTS);
        MethodCallExpression nextCall = null;
        for(int i = 0; i < methodNodes.length; ++i) {
            MethodNode m = methodNodes[i];
            if(m.getReturnType() == ClassHelper.byte_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashByte", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);

            }
            else if(m.getReturnType() == ClassHelper.short_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashShort", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else if(m.getReturnType() == ClassHelper.int_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashInt", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else if(m.getReturnType() == ClassHelper.long_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashLong", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else if(m.getReturnType() == ClassHelper.float_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashFloat", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else if(m.getReturnType() == ClassHelper.double_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashDouble", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashObject", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
        }

        nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "finish", ArgumentListExpression.EMPTY_ARGUMENTS);
        nextCall.setImplicitThis(false);
        final MethodNode ret = new MethodNode("hashCode", ACC_PUBLIC, ClassHelper.int_TYPE,
                                              Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                              returnS(nextCall));
        thisNode.addMethod(ret);
        return ret;
    }

    public static InnerClassNode abstractBaseClass(final MethodNode methodNode) {
        final ClassNode outer = methodNode.getDeclaringClass();
        final ClassNode compileStaticNode = ClassHelper.makeWithoutCaching(groovy.transform.CompileStatic.class);
        
        final InnerClassNode icn = new InnerClassNode(outer, methodNode.getDeclaringClass().getName() + "$Base",
                                                      ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT,
                                                      ClassHelper.OBJECT_TYPE);

        final ClassNode[] types = parameterTypes(methodNode.getParameters());
        final MethodNode[] accessors = abstractAccessors(icn, types);
        equalsMethod(icn, accessors);
        hashCodeMethod(icn, accessors);
        icn.addInterface(GenericsUtils.makeClassSafeWithGenerics(Permanent.class, icn));

        final ClassNode methodIdNode = ClassHelper.makeWithoutCaching(MethodId.class);
        final ArgumentListExpression ctorArgs = new ArgumentListExpression();
        ctorArgs.addExpression(classX(copyClassNode(methodNode.getDeclaringClass())));
        ctorArgs.addExpression(constX(methodNode.getName()));
        final ListExpression typeList = new ListExpression();
        for(ClassNode type : types) {
            typeList.addExpression(classX(type));
        }
        ctorArgs.addExpression(typeList);
        
        final FieldNode methodIdField = new FieldNode("METHOD_ID", ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                                                      methodIdNode, icn,
                                                      ctorX(methodIdNode, ctorArgs));
        icn.addField(methodIdField);
        icn.addMethod(new MethodNode("getMethodId", ACC_PUBLIC, methodIdNode,
                                     Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                     returnS(fieldX(methodIdField))));
                                                      
        return icn;
    }

    public static InnerClassNode searchClass(final MethodNode methodNode, final InnerClassNode superNode, final InnerClassNode immutable) {
        final ClassNode outer = copyClassNode(methodNode.getDeclaringClass());
        final InnerClassNode icn = new InnerClassNode(outer, innerClassName(methodNode, SEARCH),
                                                      ACC_PUBLIC | ACC_STATIC,
                                                      superNode);
        final ClassNode[] types = parameterTypes(methodNode.getParameters());
        final FieldNode[] fieldNodes = fieldNodes(icn, types, false);
        final MethodNode[] accessors = accessors(icn, fieldNodes);
        final MethodNode setter = setter(icn, fieldNodes);
        
        final ArgumentListExpression fieldArgs = new ArgumentListExpression();
        for(FieldNode fnode : fieldNodes) {
            fieldArgs.addExpression(fieldX(fnode));
        }
        
        icn.addMethod(new MethodNode("permanent", ACC_PUBLIC, immutable,
                                     Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                     returnS(ctorX(icn, fieldArgs))));
                                     
        return icn;
    }
    
    public static InnerClassNode immutableClass(final MethodNode methodNode, final InnerClassNode superNode) {
        final ClassNode outer = copyClassNode(methodNode.getDeclaringClass());
        final InnerClassNode icn = new InnerClassNode(outer, innerClassName(methodNode, IMMUTABLE),
                                                      ACC_PUBLIC | ACC_STATIC,
                                                      superNode);
        final ClassNode[] types = parameterTypes(methodNode.getParameters());
        final FieldNode[] fieldNodes = fieldNodes(icn, types, true);
        final MethodNode[] accessors = accessors(icn, fieldNodes);
        final ConstructorNode constructor = constructor(icn, fieldNodes);
        icn.addMethod(new MethodNode("permanent", ACC_PUBLIC, icn,
                                     Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, returnS(varX("this"))));
        return icn;
    }

}
