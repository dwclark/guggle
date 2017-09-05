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
import static io.github.guggle.ast.transformations.Utils.*;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CacheTransformation extends AbstractASTTransformation {

    AnnotationNode annotationNode;
    MethodNode methodNode;
    ClassNode declaringClassNode;
    SourceUnit sourceUnit;
    
    public void visit(final ASTNode[] astNodes, final SourceUnit sourceUnit) {
        init(astNodes, sourceUnit);
        this.sourceUnit = sourceUnit;
        initNodes(astNodes);
    }

    void initNodes(ASTNode[] astNodes) {
        this.annotationNode = (AnnotationNode) astNodes[0];
        this.methodNode = (MethodNode) astNodes[1];
        this.declaringClassNode = methodNode.getDeclaringClass();

        // MethodNode copiedMethod = new MethodNode(methodNode.getName() + "Copied",
        //                                          methodNode.getModifiers(),
        //                                          copyClassNode(methodNode.getReturnType()),
        //                                          copyParameters(methodNode.getParameters()),
        //                                          ClassNode.EMPTY_ARRAY,
        //                                          returnS(constX(null)));
        // declaringClassNode.addMethod(copiedMethod);

        sourceUnit.getAST().addClass(Utils.abstractBaseClass(methodNode));
    }

        
}
