package io.github.guggle.ast;

import spock.lang.*;
import groovy.transform.CompileStatic;

@CompileStatic
abstract class MyKey {
    abstract String getFoo();
    abstract int getBar();
    abstract boolean isBlah();

    @MutableKey static class Mutable {}
    @ImmutableKey static class Immutable {}
}

class RichKeySpec extends Specification {

    def 'basic test'(){
        setup:

        MyKey.Mutable mut = new MyKey.Mutable().set("foo", 1, true);
        MyKey.Immutable imm = new MyKey.Immutable("foo", 1, true);
        MyKey mutCopied = mut.permanent();
        MyKey immCopied = imm.permanent();

        expect:
            
        mut == imm;
        imm == mutCopied;
        immCopied.is(imm);
    }

}
