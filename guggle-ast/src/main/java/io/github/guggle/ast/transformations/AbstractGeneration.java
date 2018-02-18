package io.github.guggle.ast.transformations;

import io.github.guggle.api.MethodId;
import io.github.guggle.utils.*;
import java.util.Arrays;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.*;
import org.codehaus.groovy.transform.*;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

public class AbstractGeneration extends SearchImmutableGeneration {

    final MethodNode methodNode;
    final String methodName;

    InnerClassNode baseNode;
    MethodNode[] abstractAccessors;
    
    public AbstractGeneration(final MethodNode methodNode) {
        super(methodNode.getDeclaringClass(), FieldInfo.from(methodNode));
        this.methodNode = methodNode;
        this.methodName = methodNode.getName();
    }

    @Override
    public void generate() {
        this.baseNode = makeAbstractNode();
        this.abstractAccessors = addAbstractAccessors(baseNode);
        super.generate();
        methodIdInfo();
    }

    public String getBaseClassName() {
        return className("Base");
    }
    
    @Override
    public String getMutableClassName() {
        return className("Search");
    }

    @Override
    public String getImmutableClassName() {
        return className("Immutable");
    }

    @Override
    public InnerClassNode getBaseNode() {
        if(baseNode == null) {
            throw new IllegalStateException("baseNode has not yet been generated");
        }
        
        return baseNode;
    }

    @Override
    public MethodNode[] getAbstractAccessors() {
        if(abstractAccessors == null) {
            throw new IllegalStateException("abstractAccessors has not yet been generated");
        }
        
        return abstractAccessors;
    }
    
    protected final String className(final String suffix) {
        return targetClassName + "$" + methodName.substring(0,1).toUpperCase() + methodName.substring(1) + suffix;
    }
    
    MethodNode[] addAbstractAccessors(final ClassNode owner) {
        final MethodNode[] ret = new MethodNode[fieldInfos.length];
        for(int i = 0; i < fieldInfos.length; ++i) {
            final FieldInfo fi = fieldInfos[i];
            ret[i] = new MethodNode(fi.getGetterName(), ACC_PUBLIC | ACC_ABSTRACT, copy(fieldInfos[i].getType()),
                                    Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, EmptyStatement.INSTANCE);
            owner.addMethod(ret[i]);
        }

        return ret;
    }

    InnerClassNode makeAbstractNode() {
        return new InnerClassNode(targetClassNode, getBaseClassName(),
                                  ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT,
                                  ClassHelper.OBJECT_TYPE);
    }

    void methodIdInfo() {
        final ClassNode methodIdNode = ClassHelper.makeWithoutCaching(MethodId.class);
        final ArgumentListExpression ctorArgs = new ArgumentListExpression();
        ctorArgs.addExpression(classX(copy(targetClassNode)));
        ctorArgs.addExpression(constX(methodName));
        final ListExpression typeList = new ListExpression();
        for(FieldInfo fi : fieldInfos) {
            typeList.addExpression(classX(fi.getType()));
        }
        ctorArgs.addExpression(typeList);
        
        final FieldNode methodIdField = new FieldNode("METHOD_ID", ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                                                      methodIdNode, getBaseNode(),
                                                      ctorX(methodIdNode, ctorArgs));
        getBaseNode().addField(methodIdField);
        getBaseNode().addMethod(new MethodNode("getMethodId", ACC_PUBLIC, methodIdNode,
                                               Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY,
                                               returnS(fieldX(methodIdField))));

    }
        
}
