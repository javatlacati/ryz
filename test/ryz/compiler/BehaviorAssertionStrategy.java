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
 * Date: Dec 31, 2010
 * Time: 11:42:06 AM
 */
class BehaviorAssertionStrategy extends AssertStrategy {


    @Override
    boolean assertSpec(Object o, String elementDescription) {
        Class c = (Class)o;
        logger.finest("o = " + o);
        logger.finest("elementDescription = " + elementDescription);
        if( "new".equals(elementDescription) ){

            return !isNull(createInstance(c));

        } else if(elementDescription.trim().startsWith("invokestatic")
                || elementDescription.trim().startsWith("invokevirtual")){

            return assertMethodInvocation(elementDescription, c);


        } else {
            //throw new UnsupportedOperationException(elementDescription);
            return false;
        }

    }

    //TODO: clean up this method
    private boolean assertMethodInvocation(String elementDescription, Class c) {
        /*
   1.- Get the method name with args types
   2.- invoke it
   3.- assert the expected output
    */

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
        // and ommiting the last parenthesis
        String methodArgs = methodDefinition.substring(i+1, methodDefinition.length()-1);

        // The second part is what happens with the method.
        // might modify the stdout or stderr return something or modify some variable
        // there should be always a "=" separating the "thing" with the value
        String object = methodResult.split("=")[0].trim();
        String expectedValue = methodResult.split("=")[1];

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
                invocationResult            =
                        String.valueOf(
                            c.getMethod(methodName, parameterTypes)
                            .invoke(objectToInvoke(c, instruction), arguments))
                        ;
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
        return "invokevirtual".equals(instruction) ? createInstance(c) : null;
    }

    private Object createInstance(Class c) {
        try {
            Object v = c.newInstance();
            assert c.isInstance(v);
            return v;
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    String getType(Object o) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    int getModifiers(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
