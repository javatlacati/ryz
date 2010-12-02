/*
 * Copyright (c) 2010, Ryz language developers.
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

import com.sun.codemodel.internal.JDefinedClass;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: Oct 15, 2010
 * Time: 8:55:48 AM
 * To change this template use File | Settings | File Templates.
 */
abstract class LineTransformer {

    protected final String lineSeparator = System.getProperty("line.separator");
    protected final Logger logger = Logger.getLogger(this.getClass().getName());
    private static final List<String> javaKeywords = Arrays.asList(
            "abstract","assert","boolean","break","byte","case","catch",
            "char","class","const","continue","default","do","double",
            "else","enum","extends","final ","finally","float","for",
            "goto","if","implements","import","instanceof","int",
            "interface","long","native ","new","package","private ",
            "protected ","public return","short","static ","strictfp ",
            "super","switch","synchronized ","this","throw","transient ",
            "try","void","volatile ","while"
    );

    private RyzClass currentClass;

    public void currentClass(RyzClass currentClass) {
        this.currentClass = currentClass;
    }


    public RyzClass currentClass() {
        return currentClass;
    }

    protected String scapeName(String name) {
        if( javaKeywords.contains(name)){
            return name + "$";
        }

        return name;
    }
    
    public abstract void transform(String line, List<String> generatedSource);

    protected String checkObjectInitialization(String initialValue) {
        if( Character.isUpperCase(initialValue.charAt(0)) && initialValue.matches(".*\\(.*\\)")){
            initialValue = "new " + initialValue;
        }
        return initialValue;
    }
    protected String inferType(String initialValue ) {
        Matcher m = Pattern.compile("(.*)\\(.*\\)").matcher(initialValue);
        if( Character.isUpperCase(initialValue.charAt(0)) && m.matches()){
            return m.group(1);
        }
        return initialValue;
        
    }

}
class ImportTransformer extends LineTransformer {
    private final Pattern importPattern = Pattern.compile("import\\s*\\((.+)\\s*\\)");


    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("import(")){
            logger.finest("line = " + line);
            Matcher m = importPattern.matcher(line);
            if( m.matches() ) {
                generatedSource.add( String.format("import %s;%n", m.group(1)));
            }
        }
    }
}
class PackageClassTransformer extends LineTransformer {
    @Override
    public void transform(String line, List<String> generatedSource) {

        int iod = line.indexOf(".");
        int iok = line.indexOf("{", iod);
        int ioc = line.indexOf(":");

        if (iod > 0 && iok > iod) {
            // ej: some.package.Name {
            String possiblePackageAndClass = line.substring(0, iok);
            int liod = possiblePackageAndClass.lastIndexOf(".");

            String possibleClass;
            String possibleSuperClass = "java.lang.Object";
            if( ioc > 0 ){
                possibleClass = possiblePackageAndClass.substring(liod+1, ioc);
                possibleSuperClass = possiblePackageAndClass.substring(ioc+1).trim();


            } else {
                possibleClass = possiblePackageAndClass.substring(liod+1).trim();

            }

            if (Character.isUpperCase(possibleClass.charAt(0))) {
                String packageName = possiblePackageAndClass.substring(0, liod);
                StringBuilder sb = new StringBuilder("");
                for( String s : packageName.split("\\.")){
                    sb.append(scapeName(s));
                    sb.append(".");
                }
                sb.delete(sb.length()-1, sb.length());
                generatedSource.add(String.format("package %s;%n", sb.toString()));
                String extendsOrImplements = isInterface( possibleSuperClass ) ?
                        "implements" :
                        "extends";
                generatedSource.add(String.format("public class %s %s %s {%n",
                        scapeName(possibleClass),
                        extendsOrImplements,
                        scapeName(possibleSuperClass)));
            }
        }
    }

    private boolean isInterface(String clazz) {
        try {
            logger.fine(clazz);
            Class c = Class.forName(clazz);
            return c.isInterface();
        } catch (ClassNotFoundException e) {
            if( !clazz.startsWith("java.lang")){
                return isInterface("java.lang."+clazz);
            } else {
                return false;// should throw class not found exception actually.
            }
        }
    }

}

class AttributeTransformer extends LineTransformer {

    // hola : adios
    private final Pattern attributePattern = Pattern.compile("(\\w+)\\s*:\\s*(\\w+)");
    // hola : adios = xyz
    private final Pattern attributeInitializedPattern = Pattern.compile("(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)");
    // hola: adio = Xyz()
    private final Pattern attributeInitializedPatternFromInvocation = Pattern.compile("(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))");
    // hola = adios //TODO: revisar como saber el tipo de dato de un valor
    private final Pattern attributeInferencePattern = Pattern.compile("(\\w+)\\s*=\\s*(.+)");
    // hola = adios()
    private final Pattern attributeInferenceFromInvocationPattern = Pattern.compile("(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))");


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher = attributePattern.matcher(line);

        //TODO: default must be final

        if( matcher.matches()){
            generatedSource.add( String.format("    /*attribute*/private %s %s;%n",
                scapeName(matcher.group(2)),
                scapeName(matcher.group(1))));
        } else if( (matcher = attributeInitializedPatternFromInvocation.matcher(line)).matches() ){
            generatedSource.add( String.format("    /*attribute*/private %s %s = %s;%n",
                scapeName(matcher.group(2)),
                scapeName(matcher.group(1)),
                scapeName(checkObjectInitialization(matcher.group(3)))));
        } else if( (matcher = attributeInitializedPattern.matcher(line)).matches() ){
            generatedSource.add( String.format("    /*attribute*/private %s %s = %s;%n",
                scapeName(matcher.group(2)),
                scapeName(matcher.group(1)),
                scapeName(matcher.group(3))));
        } else if( (matcher = attributeInferenceFromInvocationPattern.matcher(line)).matches() ){
            generatedSource.add( String.format("    /*attribute*/private %s %s = %s;%n",
                scapeName(inferType(matcher.group(2))),
                scapeName(matcher.group(1)),
                scapeName(checkObjectInitialization(matcher.group(2)))));
        }

    }


}
// TODO: multiline comments has problems
class CommentTransformer extends LineTransformer {

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("/*")
          || line.startsWith("//")
          || line.endsWith("*/") ){
            generatedSource.add(line + lineSeparator);

        }
    }
}
class ClosingKeyTransformer extends LineTransformer {

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("}")) {
            generatedSource.add( line + lineSeparator);
        }
    }
}
class MethodTransformer extends LineTransformer {

    // hola():String{
    private final Pattern methodPattern = Pattern.compile("(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    // __ hola() : String { 
    private final Pattern classMethodPattern = Pattern.compile("__\\s*(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    // hola() {
    private final Pattern voidMethodPattern = Pattern.compile("(\\w+)\\(\\)\\s*\\s*\\{");
    // __ hola() {
    private final Pattern voidClassMethodPattern = Pattern.compile("_{2}\\s*(\\w+)\\(\\)\\s*\\s*\\{");


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher;
        //TODO: handle default return
        if( (matcher = methodPattern.matcher(line)).matches() ) {
            generatedSource.add( String.format("    /*method*/public %s %s() {%n",
                    scapeName(matcher.group(2)),
                    scapeName(matcher.group(1))));
        } else if ( (matcher = classMethodPattern.matcher(line)).matches() ){
            generatedSource.add( String.format("    /*method*/public static %s %s() {%n",
                    scapeName(matcher.group(2)),
                    scapeName(matcher.group(1))));
        } else if( ( matcher = voidMethodPattern.matcher(line)).matches() ){
            String methodName = matcher.group(1);
            // main() {  is special, will create public static void main( String [] args )
            // TODO: must create: psvm(){ new Instance().main() } public void main(){ ...
            if( "main".equals(methodName)){
;
                 generatedSource.add(String.format(
                         "public static void main( String [] args ) {\n" +
                        "  new %s().main();\n" +
                        "}\n" +
                        "public void main() {", currentClass().name()));
            } else {
                generatedSource.add( String.format("    /*method*/public void %s() {",
                           scapeName(methodName)));
            }

        } else if( ( matcher = voidClassMethodPattern.matcher(line)).matches()){
            generatedSource.add( String.format("    /*method*/public static void %s() {",
                       scapeName(matcher.group(1))));

        }


    }
}
// Handles, temporarily the "return" of the method. Eventually the return
// woudl change to avoid the "return" keyword
class ReturnTransformer extends LineTransformer {
    private final Pattern returnPattern = Pattern.compile("\\^\\s+(.+)");

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = returnPattern.matcher(line);
        if( m.matches() ){
            String returnValue = checkObjectInitialization(m.group(1));
            generatedSource.add( String.format("        return %s;%n", returnValue));
        }
    }
}
class StatementTransformer extends LineTransformer {

    private final Pattern statementPattern = Pattern.compile("\\w+\\.\\w+\\(\\)");//StringBuilder(name).reverse().toString()

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = statementPattern.matcher(line);
        if( m.matches() ) {
            generatedSource.add( String.format("    /*invocation*/%s;%n",
                    line));
            
        }
        
    }
}
