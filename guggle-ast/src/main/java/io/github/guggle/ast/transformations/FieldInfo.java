package io.github.guggle.ast.transformations;

import org.codehaus.groovy.ast.*;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

class FieldInfo {
    final String canonicalName;
    final ClassNode classNode;

    private String camelCased() {
        return canonicalName.substring(0,1).toUpperCase() + canonicalName.substring(1);
    }

    public static boolean isBooleanAccessor(final MethodNode mn) {
        return (mn.getName().startsWith("is") &&
                mn.getName().length() > 2 &&
                mn.getParameters().length == 0 &&
                (mn.getReturnType() == ClassHelper.boolean_TYPE || mn.getReturnType() == ClassHelper.Boolean_TYPE));
    }
    
    public static boolean isNormalAccessor(final MethodNode mn) {
        return (mn.getName().startsWith("get") &&
                mn.getName().length() > 3 &&
                mn.getParameters().length == 0 &&
                mn.getReturnType() != ClassHelper.VOID_TYPE);
    }

    public static boolean isAccessor(final MethodNode mn) {
        return isNormalAccessor(mn) || isBooleanAccessor(mn);
    }

    public static String extractCanonical(final MethodNode mn) {
        int first = isNormalAccessor(mn) ? 3 : 2;
        return mn.getName().substring(first,first+1).toLowerCase() + mn.getName().substring(first+1);
    }
    
    private FieldInfo(final String canonicalName, final ClassNode classNode) {
        this.canonicalName = canonicalName;
        this.classNode = classNode;
    }

    public static FieldInfo from(final Parameter p) {
        return new FieldInfo(p.getName(), p.getType());
    }

    public static FieldInfo[] from(final MethodNode mn) {
        Parameter[] params = mn.getParameters();
        final FieldInfo[] ret = new FieldInfo[params.length];
        for(int i = 0; i < params.length; ++i) {
            ret[i] = from(params[i]);
        }

        return ret;
    }

    public static FieldInfo[] from(final ClassNode cn) {
        int needed = 0;
        
        for(MethodNode mn : cn.getMethods()) {
            if(isAccessor(mn)) {
                ++needed;
            }
        }

        FieldInfo[] ret = new FieldInfo[needed];
        int index = 0;
        for(MethodNode mn : cn.getMethods()) {
            if(isAccessor(mn)) {
                ret[index++] = new FieldInfo(extractCanonical(mn), mn.getReturnType());
            }
        }

        return ret;
    }

    public boolean isBoolean() {
        return (classNode == ClassHelper.boolean_TYPE || classNode == ClassHelper.Boolean_TYPE);
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getFieldName() {
        return "_" + canonicalName;
    }

    public String getGetterName() {
        return isBoolean() ? "is" + camelCased() : "get" + camelCased();
    }

    public String getSetterName() {
        return "set" + camelCased();
    }

    public ClassNode getType() {
        return classNode;
    }
}
