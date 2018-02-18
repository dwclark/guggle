package io.github.guggle.ast.transformations;

import io.github.guggle.api.MethodId;
import io.github.guggle.api.Permanent;
import io.github.guggle.utils.*;
import java.util.Arrays;
import java.util.List;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.*;
import org.codehaus.groovy.transform.*;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

public class SearchImmutableGeneration extends AbstractASTTransformation {

    final String A_PREFIX = "a";

    final FieldInfo[] fieldInfos;
    final ClassNode targetClassNode;
    final String targetClassName;
    
    InnerClassNode immutableNode;
    InnerClassNode searchNode;
    
    public SearchImmutableGeneration(final ClassNode targetClassNode,
                                     final FieldInfo[] fieldInfos) {
        this.targetClassNode = targetClassNode;
        this.targetClassName = targetClassNode.getName();
        this.fieldInfos = fieldInfos;
    }

    public SearchImmutableGeneration(final ClassNode targetClassNode) {
        this(targetClassNode, FieldInfo.from(targetClassNode));
    }
    
    public void visit(final ASTNode[] nodes, final SourceUnit unit) {
        throw new UnsupportedOperationException("Not really an ast transformation, this is a convenient/safe" +
                                                "way to get access to the Opcodes");
    }

    public String getMutableClassName() {
        return targetClassName + "$Mutable";
    }

    public String getImmutableClassName() {
        return targetClassName +"$Immutable";
    }

    public void pre() { }
    
    public void generate() {
        enhanceAbstract();
        immutableClass();
        searchClass();
    }
    
    public void post() { }

    public ClassNode getBaseNode() {
        return targetClassNode;
    }

    public MethodNode[] getAbstractAccessors() {
        int count = 0;
        for(MethodNode mn : getBaseNode().getMethods()) {
            if(mn.isAbstract() && FieldInfo.isAccessor(mn)) {
                ++count;
            }
        }

        MethodNode[] ret = new MethodNode[count];
        for(int i = 0; i < getBaseNode().getMethods().size(); ++i) {
            final MethodNode mn = getBaseNode().getMethods().get(i);
            if(mn.isAbstract() && FieldInfo.isAccessor(mn)) {
                ret[i] = mn;
            }
        }

        return ret;
    }

    public static boolean existsMethod(final ClassNode classNode, final String name) {
        List<MethodNode> list = classNode.getDeclaredMethods(name);
        return list != null && list.size() > 0;
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

    FieldNode[] addFieldNodes(final ClassNode owner, final boolean immutable) {
        final FieldNode[] ret = new FieldNode[fieldInfos.length];
        for(int i = 0; i < fieldInfos.length; ++i) {
            final FieldInfo fi = fieldInfos[i];
            final int modifiers = immutable ? (ACC_PRIVATE | ACC_FINAL) : ACC_PRIVATE;
            //MUY IMPORTANTE! Uninitialized fields should get null not an empty expression
            ret[i] = new FieldNode(fi.getFieldName(), modifiers, copy(fi.getType()), owner, null);
            owner.addField(ret[i]);
        }

        return ret;
    }

    MethodNode[] addAccessors(final ClassNode owner, final FieldNode[] fieldNodes) {
        final MethodNode[] ret = new MethodNode[fieldNodes.length];
        for(int i = 0; i < fieldNodes.length; ++i) {
            final FieldNode fn = fieldNodes[i];
            final FieldInfo fi = Arrays.stream(fieldInfos).filter(f -> f.getFieldName().equals(fn.getName())).findFirst().get();
            ret[i] = new MethodNode(fi.getGetterName(), ACC_PUBLIC, copy(fieldNodes[i].getType()),
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

    void addSetters(final ClassNode owner, final FieldNode[] fieldNodes) {
        for(int i = 0; i < fieldNodes.length; ++i) {
            final FieldNode fn = fieldNodes[i];
            final FieldInfo fi = Arrays.stream(fieldInfos).filter(f -> f.getFieldName().equals(fn.getName())).findFirst().get();
            BlockStatement body = block(assignS(fieldX(fn), varX("val")));
            Parameter p = new Parameter(copy(fn.getType()), "val");
            owner.addMethod(new MethodNode(fi.getSetterName(), ACC_PUBLIC, ClassHelper.VOID_TYPE,
                                           new Parameter[] { p }, ClassNode.EMPTY_ARRAY, body));
        }
    }
    
    ConstructorNode addConstructor(final ClassNode owner, final FieldNode[] fieldNodes) {
        final ConstructorNode cnode = new ConstructorNode(ACC_PUBLIC,
                                                          setterParameters(fieldNodes), ClassNode.EMPTY_ARRAY,
                                                          setterAssignments(fieldNodes));
        owner.addConstructor(cnode);
        return cnode;
    }

    void addEquals() {
        final ClassNode thisNode = getBaseNode();
        if(existsMethod(thisNode, "equals")) {
            return;
        }
        
        final MethodNode[] methodNodes = getAbstractAccessors();
        
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
                if(mnode.getReturnType() == ClassHelper.boolean_TYPE) {
                    comparisons[i] = eqX(constX(0, true), new StaticMethodCallExpression(ClassHelper.Boolean_TYPE, "compare", args(myCall, theirCall)));
                }
                else {
                    comparisons[i] = eqX(myCall, theirCall);
                }
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
        //return equalsMethod;
    }

    void addHashCode() {
        final ClassNode thisNode = getBaseNode();
        if(existsMethod(thisNode, "hashCode")) {
            return;
        }
        
        final MethodNode[] methodNodes = getAbstractAccessors();
        
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
    }

    public void immutableClass() {
        if(immutableNode == null) {
            immutableNode = new InnerClassNode(targetClassNode, getImmutableClassName(),
                                               ACC_PUBLIC | ACC_STATIC, getBaseNode());
        }
        else {
            immutableNode.setSuperClass(getBaseNode());
        }
        
        final FieldNode[] fieldNodes = addFieldNodes(immutableNode, true);
        final MethodNode[] accessors = addAccessors(immutableNode, fieldNodes);
        final ConstructorNode constructor = addConstructor(immutableNode, fieldNodes);
        immutableNode.addMethod(new MethodNode("permanent", ACC_PUBLIC, immutableNode,
                                               Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, returnS(varX("this"))));
    }

    public void searchClass() {
        if(searchNode == null) {
            searchNode = new InnerClassNode(targetClassNode, getMutableClassName(),
                                            ACC_PUBLIC | ACC_STATIC,
                                            getBaseNode());
        }
        else {
            searchNode.setSuperClass(getBaseNode());
        }
        
        final FieldNode[] fieldNodes = addFieldNodes(searchNode, false);
        final MethodNode[] accessors = addAccessors(searchNode, fieldNodes);
        final MethodNode setter = addSetter(searchNode, fieldNodes);
        addSetters(searchNode, fieldNodes);
        
        final ArgumentListExpression fieldArgs = new ArgumentListExpression();
        for(FieldNode fnode : fieldNodes) {
            fieldArgs.addExpression(fieldX(fnode));
        }
        
        searchNode.addMethod(new MethodNode("permanent", ACC_PUBLIC, immutableNode,
                                            Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                            returnS(ctorX(immutableNode, fieldArgs))));
    }

    public void enhanceAbstract() {
        addEquals();
        addHashCode();
        final ClassNode permClassNode = GenericsUtils.makeClassSafeWithGenerics(Permanent.class, getBaseNode());
        if(!getBaseNode().implementsInterface(permClassNode)) {
            getBaseNode().addInterface(permClassNode);
        }
    }
}
