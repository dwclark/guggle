package io.github.guggle.studio;

import io.github.guggle.api.*;
import io.github.guggle.utils.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

//@Actor
class Example {

    // void event(final String str) {
    //     System.out.println(str);
    // }
    
    // String convert(final int val) {
    //     return Integer.toString(val);
    // }

}

// class ExampleConverted implements StudioId {

//     private ExampleConverted() {}

//     public static ExampleConverted create() {
//         ExampleConverted ret = new ExampleConverted();
//         Studio.instance().singleton(ret);
//         return ret;
//     }
    
//     private final Identity id = new Identity(this);
    
//     public Object studioId() {
//         return id;
//     }

//     void event(final String str) {
//         Studio.instance().message(id, new EventMessage(str, false));
//     }

//     CompletableFuture<Void> eventAsync(final String str) {
//         EventMessage message = new EventMessage(str, true);
//         Studio.instance().message(id, message);
//         return message.getFuture();
//     }
    
//     protected void _event(final String str) {
//         System.out.println(str);
//     }

//     String convert(final int val) throws InterruptedException, ExecutionException {
//         return convertAsync(val).get();
//     }

//     CompletableFuture<String> convertAsync(final int val) {
//         ConvertMessage message = new ConvertMessage(val);
//         Studio.instance().message(id, message);
//         return message.getFuture();
//     }

//     protected String _convert(final int val) {
//         return Integer.toString(val);
//     }
    
//     private static class EventMessage extends AbstractVoidMessage {

//         private static final Class<ExampleConverted> type = ExampleConverted.class;
//         private ExampleConverted target;
//         private final String a1;

//         public EventMessage(final String p1, final boolean usesFuture) {
//             super(usesFuture);
//             this.a1 = p1;
//         }
        
//         public void resource(Object o) {
//             this.target = type.cast(o);
//         }
        
//         public void runIt() {
//             target._event(a1);
//         }
//     }

//     private static class ConvertMessage extends AbstractReturningMessage<String> {

//         private static final Class<ExampleConverted> type = ExampleConverted.class;
//         private ExampleConverted target;
//         private final int a1;

//         public ConvertMessage(final int p1) {
//             super();
//             this.a1 = p1;
//         }
        
//         public void resource(Object o) {
//             this.target = type.cast(o);
//         }

//         @Override
//         public String runIt() {
//             return target._convert(a1);
//         }
//     }
// }
