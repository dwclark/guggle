package io.github.guggle.ast;

import groovy.transform.CompileStatic;

@CompileStatic
class BasicCached {

    @Cache
    String first(int one, String two, Map<String,List<String>> map) {
        return "${one} ${two} ${map}";
    }

    @Cache
    String second(String one, Map<? extends List,? extends Number> map) {
        return "${one} ${map}";
    }

    @Cache
    Double third(Map<String, List<List<String>>> map) {
        return 0.0d;
    }

    @Cache
    Map<String,Integer> fourth(Map<?, List<List<? extends Number>>> map) {
        return [:];
    }
}
