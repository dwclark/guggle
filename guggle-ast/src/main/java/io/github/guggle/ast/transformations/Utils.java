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

class Utils {

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

}
