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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.List;
import java.util.Iterator;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class RichKeyTransformation extends AbstractASTTransformation {

    private static final ConcurrentSkipListSet<String> completed = new ConcurrentSkipListSet<>();
    AnnotationNode annotationNode;
    MethodNode methodNode;
    ClassNode outerClassNode;
    InnerClassNode declaringClassNode;
    InnerClassNode searchClassNode;
    InnerClassNode immutableClassNode;
    SourceUnit sourceUnit;

    public void visit(final ASTNode[] astNodes, final SourceUnit sourceUnit) {
        init(astNodes, sourceUnit);
        this.sourceUnit = sourceUnit;
        initNodes(astNodes);

        if(!validate()) {
            return;
        }

        if(completed.contains(outerClassNode.getName())) {
            return;
        }
        else {
            completed.add(outerClassNode.getName());
        }

        SearchImmutableGeneration sig = new SearchImmutableGeneration(outerClassNode);
        sig.searchNode = searchClassNode;
        sig.immutableNode = immutableClassNode;
        sig.enhanceAbstract();
        sig.searchClass();
        sig.immutableClass();
    }

    void initNodes(ASTNode[] astNodes) {
        this.annotationNode = (AnnotationNode) astNodes[0];
        this.declaringClassNode = (InnerClassNode) astNodes[1];
        this.outerClassNode = declaringClassNode.getOuterClass();
        Iterator<InnerClassNode> iter = outerClassNode.getInnerClasses();
        while(iter.hasNext()) {
            InnerClassNode icn = iter.next();
            List<AnnotationNode> anodes = icn.getAnnotations();
            for(AnnotationNode anode : anodes){
                if(anode.getClassNode().getName().indexOf("MutableKey") != -1) {
                    this.searchClassNode = icn;
                }
                else if(anode.getClassNode().getName().indexOf("ImmutableKey") != -1) {
                    this.immutableClassNode = icn;
                }
            }
        }
    }

    private boolean validate() {
        if((outerClassNode.getModifiers() & ACC_ABSTRACT) == 0) {
            addError("Class is not abstract", declaringClassNode);
            return false;
        }

        if(outerClassNode.getMethods()
           .stream()
           .noneMatch(mn -> mn.isAbstract() && FieldInfo.isAccessor(mn))) {
            addError("No abstract getters in class", declaringClassNode);
            return false;
        }

        if(searchClassNode == null) {
            addError("Could not find node annotated with @MutableKey", declaringClassNode);
            return false;
        }

        if(immutableClassNode == null) {
            addError("Could not find node annotated with @ImmutableKey", declaringClassNode);
            return false;
        }

        return true;
    }
            
}
