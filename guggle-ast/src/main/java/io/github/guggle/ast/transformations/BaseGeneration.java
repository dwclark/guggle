package io.github.guggle.ast.transformations;

import io.github.guggle.api.Permanent;
import io.github.guggle.api.MethodId;
import io.github.guggle.utils.*;
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

public class BaseGeneration extends AbstractASTTransformation {

    final String F_PREFIX = "f";
    final String M_PREFIX = "m";
    final String A_PREFIX = "a";
    
    final MethodNode methodNode;
    final ClassNode returnType;
    final Parameter[] parameters;
    final String methodName;
    final ClassNode outerClassNode;
    final String outerClassName;
    final String outerClassNameNP;

    final String baseClassName;
    final String searchClassName;
    final String immutableClassName;

    InnerClassNode baseNode;
    InnerClassNode immutableNode;
    InnerClassNode searchNode;
    
    public BaseGeneration(final MethodNode methodNode) {
        this.methodNode = methodNode;
        this.returnType = methodNode.getReturnType();
        this.parameters = methodNode.getParameters();
        this.methodName = methodNode.getName();
        this.outerClassNode = methodNode.getDeclaringClass();
        this.outerClassName = outerClassNode.getName();
        this.outerClassNameNP = outerClassNode.getNameWithoutPackage();

        this.baseClassName = className("Base");
        this.searchClassName = className("Search");
        this.immutableClassName = className("Immutable");
    }

    public void pre() { }
    
    public void generate() {
        abstractBaseClass();
        immutableClass();
        searchClass();
    }
    
    public void post() { }
    
    protected final String className(final String suffix) {
        return outerClassName + "$" + methodName.substring(0,1).toUpperCase() + methodName.substring(1) + suffix;
    }
    
    protected final String fieldName(final String prefix, final String suffix) {
        return prefix + methodName + suffix;
    }

    public static ClassNode copy(final ClassNode classNode) {
        if(ClassHelper.isPrimitiveType(classNode)) {
            return classNode;
        }
        
        if(classNode.getGenericsTypes() == null || classNode.getGenericsTypes().length == 0) {
            return ClassHelper.makeWithoutCaching(classNode.getName());
        }

        GenericsType[] generics = classNode.getGenericsTypes();
        GenericsType[] copiedGenerics = new GenericsType[generics.length];
        for(int i = 0; i < generics.length; ++i) {
            copiedGenerics[i] = copy(generics[i]);
        }
        
        ClassNode ret = GenericsUtils.makeClassSafeWithGenerics(classNode, copiedGenerics);
        return ret;
    }

    public static GenericsType copy(final GenericsType genericsType) {
        ClassNode lowerBound = null;
        if(genericsType.getLowerBound() != null) {
            lowerBound = copy(genericsType.getLowerBound());
        }

        ClassNode[] upperBounds = null;
        if(genericsType.getUpperBounds() != null) {
            ClassNode[] original = genericsType.getUpperBounds();
            upperBounds = new ClassNode[original.length];
            for(int i = 0; i < original.length; ++i) {
                upperBounds[i] = copy(original[i]);
            }
        }
        
        GenericsType copied = new GenericsType(copy(genericsType.getType()),
                                               upperBounds, lowerBound);
        copied.setWildcard(genericsType.isWildcard());
        return copied;
    }

    public void visit(final ASTNode[] nodes, final SourceUnit unit) {
        throw new UnsupportedOperationException("Not really an ast transformation, this is a convenient/safe" +
                                                "way to get access to the Opcodes");
    }

    ClassNode[] getParameterTypes() {
        final ClassNode[] ret = new ClassNode[parameters.length];
        for(int i = 0; i < parameters.length; ++i) {
            ret[i] = parameters[i].getType();
        }

        return ret;
    }

    FieldNode[] addFieldNodes(final ClassNode owner, final boolean immutable) {
        final ClassNode[] types = getParameterTypes();
        final FieldNode[] ret = new FieldNode[types.length];
        for(int i = 0; i < types.length; ++i) {
            final int modifiers = immutable ? (ACC_PRIVATE | ACC_FINAL) : ACC_PRIVATE;
            ret[i] = new FieldNode(F_PREFIX + i, modifiers, copy(types[i]), owner, EmptyExpression.INSTANCE);
            owner.addField(ret[i]);
        }

        return ret;
    }

    MethodNode[] addAbstractAccessors(final ClassNode owner) {
        final ClassNode[] types = getParameterTypes();
        final MethodNode[] ret = new MethodNode[types.length];
        for(int i = 0; i < types.length; ++i) {
            ret[i] = new MethodNode(M_PREFIX + i, ACC_PUBLIC | ACC_ABSTRACT, copy(types[i]),
                                    Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, EmptyStatement.INSTANCE);
            owner.addMethod(ret[i]);
        }

        return ret;
    }

    MethodNode[] addAccessors(final ClassNode owner, final FieldNode[] fieldNodes) {
        final MethodNode[] ret = new MethodNode[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            ret[i] = new MethodNode(M_PREFIX + i, ACC_PUBLIC, copy(fieldNodes[i].getType()),
                                    Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                    returnS(fieldX(fieldNodes[i])));
            owner.addMethod(ret[i]);
        }

        return ret;
    }

    Parameter[] setterParameters(final FieldNode[] fieldNodes) {
        final Parameter[] parameters = new Parameter[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            parameters[i] = new Parameter(copy(fieldNodes[i].getType()), A_PREFIX + i);
        }

        return parameters;
    }
    
    BlockStatement setterAssignments(final FieldNode[] fieldNodes) {
        final Statement[] assignments = new Statement[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            assignments[i] = assignS(fieldX(fieldNodes[i]), varX(A_PREFIX + i));
        }

        return block(assignments);
    }

    MethodNode addSetter(final ClassNode owner, final FieldNode[] fieldNodes) {
        final BlockStatement block = setterAssignments(fieldNodes);
        block.addStatement(returnS(varX("this")));
        final MethodNode methodNode = new MethodNode("set", ACC_PUBLIC, owner,
                                                     setterParameters(fieldNodes), ClassNode.EMPTY_ARRAY,
                                                     block);
        owner.addMethod(methodNode);
        return methodNode;
    }

    ConstructorNode addConstructor(final ClassNode owner, final FieldNode[] fieldNodes) {
        final ConstructorNode cnode = new ConstructorNode(ACC_PUBLIC,
                                                          setterParameters(fieldNodes), ClassNode.EMPTY_ARRAY,
                                                          setterAssignments(fieldNodes));
        owner.addConstructor(cnode);
        return cnode;
    }

    MethodNode addEquals(final ClassNode thisNode, final MethodNode[] methodNodes) {
        final ClassNode objectsNode = ClassHelper.makeWithoutCaching(java.util.Objects.class);
        final String o = "o";
        final String rhs = "rhs";
        final BlockStatement block = new BlockStatement();
        block.addStatement(ifS(notX(isInstanceOfX(varX(o), thisNode)), returnS(constX(false, true))));
        final CastExpression cast = new CastExpression(thisNode, varX(o));
        cast.setStrict(true);
        final VariableExpression rhsVar = new VariableExpression("rhs", thisNode);
        block.addStatement(declS(rhsVar, cast));

        Expression[] comparisons = new Expression[methodNodes.length];
        for(int i = 0; i < methodNodes.length; ++i) {
            final MethodNode mnode = methodNodes[i];
            final MethodCallExpression myCall = new MethodCallExpression(varX("this"), mnode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS);
            myCall.setImplicitThis(true);
            myCall.setMethodTarget(mnode);
            final MethodCallExpression theirCall = new MethodCallExpression(rhsVar, mnode.getName(), ArgumentListExpression.EMPTY_ARGUMENTS);
            theirCall.setImplicitThis(false);
            theirCall.setMethodTarget(mnode);
            
            if(ClassHelper.isPrimitiveType(mnode.getReturnType())) {
                comparisons[i] = eqX(myCall, theirCall);
            }
            else {
                comparisons[i] = new StaticMethodCallExpression(objectsNode, "equals", args(myCall, theirCall));
            }
        }

        for(int i = 0; i < comparisons.length; ++i) {
            block.addStatement(ifS(notX(comparisons[i]), returnS(constX(false, true))));
        }

        block.addStatement(returnS(constX(true, true)));

        final MethodNode equalsMethod = new MethodNode("equals", ACC_PUBLIC, ClassHelper.boolean_TYPE,
                                                       params(param(ClassHelper.OBJECT_TYPE, o)), ClassNode.EMPTY_ARRAY,
                                                       block);
        thisNode.addMethod(equalsMethod);
        return equalsMethod;
    }

    MethodNode addHashCode(final ClassNode thisNode, final MethodNode[] methodNodes) {
        final ClassNode fnvNode = ClassHelper.makeWithoutCaching(Fnv.class);
        StaticMethodCallExpression firstCall = new StaticMethodCallExpression(fnvNode, "start", ArgumentListExpression.EMPTY_ARGUMENTS);
        MethodCallExpression nextCall = null;
        for(int i = 0; i < methodNodes.length; ++i) {
            MethodNode m = methodNodes[i];

            if(m.getReturnType() == ClassHelper.boolean_TYPE) {
                nextCall = new MethodCallExpression(nextCall == null ? firstCall : nextCall, "hashBoolean", args(callThisX(m.getName())));
                nextCall.setImplicitThis(false);
            }
            else if(m.getReturnType() == ClassHelper.byte_TYPE) {
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

    public void abstractBaseClass() {
        this.baseNode = new InnerClassNode(outerClassNode, baseClassName,
                                           ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT,
                                           ClassHelper.OBJECT_TYPE);

        final ClassNode[] types = getParameterTypes();
        final MethodNode[] accessors = addAbstractAccessors(baseNode);
        addEquals(baseNode, accessors);
        addHashCode(baseNode, accessors);
        baseNode.addInterface(GenericsUtils.makeClassSafeWithGenerics(Permanent.class, baseNode));

        final ClassNode methodIdNode = ClassHelper.makeWithoutCaching(MethodId.class);
        final ArgumentListExpression ctorArgs = new ArgumentListExpression();
        ctorArgs.addExpression(classX(copy(outerClassNode)));
        ctorArgs.addExpression(constX(methodName));
        final ListExpression typeList = new ListExpression();
        for(ClassNode type : types) {
            typeList.addExpression(classX(type));
        }
        ctorArgs.addExpression(typeList);
        
        final FieldNode methodIdField = new FieldNode("METHOD_ID", ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                                                      methodIdNode, baseNode,
                                                      ctorX(methodIdNode, ctorArgs));
        baseNode.addField(methodIdField);
        baseNode.addMethod(new MethodNode("getMethodId", ACC_PUBLIC, methodIdNode,
                                          Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                          returnS(fieldX(methodIdField))));
    }
    
    public void immutableClass() {
        this.immutableNode = new InnerClassNode(outerClassNode, immutableClassName,
                                                ACC_PUBLIC | ACC_STATIC, baseNode);
        final ClassNode[] types = getParameterTypes();
        final FieldNode[] fieldNodes = addFieldNodes(immutableNode, true);
        final MethodNode[] accessors = addAccessors(immutableNode, fieldNodes);
        final ConstructorNode constructor = addConstructor(immutableNode, fieldNodes);
        immutableNode.addMethod(new MethodNode("permanent", ACC_PUBLIC, immutableNode,
                                               Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, returnS(varX("this"))));
    }

    public void searchClass() {
        this.searchNode = new InnerClassNode(outerClassNode, searchClassName,
                                             ACC_PUBLIC | ACC_STATIC,
                                             baseNode);
        final ClassNode[] types = getParameterTypes();
        final FieldNode[] fieldNodes = addFieldNodes(searchNode, false);
        final MethodNode[] accessors = addAccessors(searchNode, fieldNodes);
        final MethodNode setter = addSetter(searchNode, fieldNodes);
        
        final ArgumentListExpression fieldArgs = new ArgumentListExpression();
        for(FieldNode fnode : fieldNodes) {
            fieldArgs.addExpression(fieldX(fnode));
        }
        
        searchNode.addMethod(new MethodNode("permanent", ACC_PUBLIC, immutableNode,
                                            Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                            returnS(ctorX(immutableNode, fieldArgs))));
    }
}
