/*
 * Copyright (c)  2010 - 2011, Oscar Reyes and Ryz language developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the folLowing disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *     - Neither the name of Ryz nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES ( INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OR LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * ( INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN ANY WAY OUT OF THE USE  OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ryz.compiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * User: oscarryz
 */
class BehaviorAssertionStrategy extends AssertStrategy {

    private Class  currentClass;
    private Object objectToInvokeOn;

    @Override
    boolean assertSpec(Object itemUnderReview, String elementDescription) {
        Class c = (Class) itemUnderReview;
        if( !c.equals(currentClass)){
            currentClass = c;
            objectToInvokeOn = null;
        }
        logger.finest("itemUnderReview = " + itemUnderReview);
        logger.finest("elementDescription = " + elementDescription);
        if( "new".equals(elementDescription) ){

            return !isNull(createInstance(c));

        } else {
            return (elementDescription.trim().startsWith("invokestatic")
                        || elementDescription.trim().startsWith("invokevirtual"))
                    && assertMethodInvocation(elementDescription, c);
        }

    }

    //TODO: clean up this method
    private boolean assertMethodInvocation(String elementDescription, Class c) {
        /*
        1.- Get the method name with args types
        2.- invoke it
        3.- assert the expected output
        */
        // replace %n in the properties
        // currently java.util.Properties ( from which the behavior spec is read) 
        elementDescription = elementDescription.replaceAll("%n" ,
                                 LineTransformer.lineSeparator).replaceAll("\\\\,",",");



        //TODO: clean up probably using regexp
        // get the first part something like:  invokestatic methodname() |
        String[] methodSpec = elementDescription.split("\\|");

        String methodDefinition = methodSpec[0].trim();
        String methodResult = methodSpec[1];

        // search for the args
        int i = methodDefinition.indexOf("(");
        // starting after "invokestatic" up to the first parenthesis.
        int firstSpace = methodDefinition.indexOf(" ")+1;
        String instruction = methodDefinition.substring(0, firstSpace -1);
        logger.finest("firstSpace = " + firstSpace);
        String methodName = methodDefinition.substring(firstSpace, i);
        logger.finest("methodName = " + methodName);
        // and omitting the last parenthesis
        String methodArgs = methodDefinition.substring(i+1, methodDefinition.length()-1);

        // The second part is what happens with the method.
        // might modify the stdout or stderr return something or modify some variable
        // there should be always a "=" separating the "thing" with the value
        String[] split = methodResult.split("=");
        String object = split[0].trim();
        String expectedValue = split.length == 2 ? split[1] : "";

        // If the method write to stdout check that
        if( "stdout".equals(object) || "result".equals(object)) {
            ByteArrayOutputStream byteArrayOutputStream;
            PrintStream sout;

            byteArrayOutputStream = new ByteArrayOutputStream();
            PrintStream mockOut = new PrintStream(byteArrayOutputStream);
            sout = System.out;
            System.setOut(mockOut);

            String invocationResult;
            try {
                Class<?> [] parameterTypes  = parameterTypes(methodArgs);
                Object[] arguments          = arguments(methodArgs);
                objectToInvokeOn = objectToInvoke(c, instruction);
                invocationResult            =
                        String.valueOf(
                            c.getMethod(methodName, parameterTypes)
                            .invoke(objectToInvokeOn, arguments));
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                System.setOut(sout);
            }

            if("null".equals(invocationResult)){
                invocationResult = byteArrayOutputStream.toString();
            }

            logger.finest("invocationResult = " + invocationResult);
            assert expectedValue.equals(invocationResult) : String.format("Expected [%s] obtained [%s]", expectedValue, invocationResult);
            return true;

        } else {
            throw new IllegalArgumentException(object);
        }
    }

    private Object[] arguments(String methodArgs) {
        return isEmpty(methodArgs) ? new Object[]{} : new Object[]{new String[]{}};
    }

    private Class<?>[] parameterTypes(String methodArgs) throws ClassNotFoundException {
        return isEmpty(methodArgs) ? new Class<?>[]{}  : new Class<?>[]{Class.forName(methodArgs)};
    }

    private Object objectToInvoke(Class c, String instruction) {
        return "invokevirtual".equals(instruction) ?
                    objectToInvokeOn == null ?
                            ( objectToInvokeOn = createInstance(c) ):
                            objectToInvokeOn
                : null;
    }

    private Object createInstance(Class c) {
        try {
            Object v = c.newInstance();
            assert c.isInstance(v);
            return v;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String propertyToValidate() {
        return "behavior";
    }

    @Override
    public Object[] getObjectsToValidate(Class clazz) {
        return new Object[]{clazz};
    }

    @Override
    String getName(Object o) {
        return null;
    }

    @Override
    String getType(Object o) {
        return null;
    }

    @Override
    int getModifiers(Object o) {
        return 0;
    }
}
