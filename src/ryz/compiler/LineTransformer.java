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

    private final RyzClass currentClass;

    public LineTransformer(RyzClassState state) {
        this.currentClass = state.ryzClass();
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

    ImportTransformer(RyzClassState state) {
        super(state);
    }


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
    PackageClassTransformer(RyzClassState state) {
        super(state);
    }

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
                StringBuilder sb = new StringBuilder("");
                for( String s : possiblePackageAndClass.substring(0, liod).split("\\.")){
                    sb.append(scapeName(s));
                    sb.append(".");
                }
                sb.delete(sb.length()-1, sb.length());
                String packageName = sb.toString();
                generatedSource.add(String.format("package %s;%n", packageName));
                String extendsOrImplements = isInterface( possibleSuperClass ) ?
                        "implements" :
                        "extends";
                String className = scapeName(possibleClass);
                generatedSource.add(String.format("public class %s %s %s {%n",
                        className,
                        extendsOrImplements,
                        scapeName(possibleSuperClass)));
                this.currentClass().setPackageName(packageName);
                this.currentClass().setClassName(className);
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
    private final boolean includeScope;

    public AttributeTransformer(RyzClassState state, boolean includeScope ) {
        super(state);
        this.includeScope = includeScope;
    }
    public AttributeTransformer(RyzClassState state) {
        this(state, true);
    }
    // [+#~-] hola ...
    private final Pattern scopePattern
            = Pattern.compile("\\s*([+#~-])\\s*.+");
    // hola : adios
    private final Pattern pattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*:\\s*(\\w+)");
    // hola : adios = xyz
    private final Pattern initializedPattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)");
    // hola: adios = Xyz()
    private final Pattern initializedFromInvocation
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*:" +
                              "\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))");

    // hola = adios //TODO: revisar como saber el tipo de dato de un valor
    private final Pattern attributeInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(.+)");
    // hola = adios()
    private final Pattern inferenceFromInvocationPattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))");


    @Override
    public void transform(String line, List<String> generatedSource) {


        Matcher matcher = pattern.matcher(line);

        //TODO: default must be final
        String accessModifier = getScope(line);
        String attributeName = null;
        String attributeType = null;
        String initialValue = ";"; // is ";" or  "= xyz";
        if( matcher.matches()){

            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(matcher.group(2));

        } else if( (matcher = initializedFromInvocation.matcher(line)).matches() ){

            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(matcher.group(2));
            initialValue = " = "+ scapeName(checkObjectInitialization(matcher.group(3))) + ";";

        } else if( (matcher = initializedPattern.matcher(line)).matches() ){

            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(matcher.group(2));
            initialValue = " = "+ scapeName(matcher.group(3)) + ";";

        } else if( (matcher = inferenceFromInvocationPattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(inferType(matcher.group(2)));
            initialValue = " = "+ scapeName(checkObjectInitialization(matcher.group(2))) + ";";
        }
        if( attributeName != null ) {
            generatedSource.add( String.format("    /*attribute*/ %s %s %s %s %n",
                accessModifier,
                attributeType,
                attributeName,
                initialValue));

        }

    }

    private String getScope(String line){
        if( includeScope == false ) {
            return "";
        }
        Matcher matcher = scopePattern.matcher(line);

        if (matcher.matches()) {
            if (matcher.group(1).equals("+")) {
                return "public";
            } else if (matcher.group(1).equals("#")) {
                return "protected";
            } else if (matcher.group(1).equals("~")) {
                return "";
            } else if (matcher.group(1).equals("-")) {
                return "private";
            }
        }

            return "private";
    }
}
// TODO: multiline comments has problems
class CommentTransformer extends LineTransformer {
    CommentTransformer(RyzClassState state) {
        super(state);
    }

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
    ClosingKeyTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("}")) { // or ends with }
            generatedSource.add( line + lineSeparator);
            this.currentClass().closeKey();
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

    MethodTransformer(RyzClassState state) {
        super(state);
    }


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher;
        //TODO: handle default return

        String methodName = null;
        String methodType = null;
        String instanceOrStatic = null;

        if( (matcher = methodPattern.matcher(line)).matches() ) {
            methodType = scapeName(matcher.group(2));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "";
        } else if ( (matcher = classMethodPattern.matcher(line)).matches() ){
            methodType = scapeName(matcher.group(2));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        } else if( ( matcher = voidMethodPattern.matcher(line)).matches() ){
            // main() {  is special, will create public static void main( String [] args )
            if( "main".equals(matcher.group(1))){

                 generatedSource.add(String.format(
                         "public static void main( String [] args ) {\n" +
                        "  new %s().main();\n" +
                        "}\n" +
                        "public void main() {", currentClass().name()));
                currentClass().addMethod("main","void");
            } else {
                methodType = "void";
                methodName = scapeName(matcher.group(1));
                instanceOrStatic = "";
            }

        } else if( ( matcher = voidClassMethodPattern.matcher(line)).matches()){
            methodType = "void";
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        }
        if( methodName != null ) {
            generatedSource.add( String.format("    /*method*/public %s %s %s() {%n",
                instanceOrStatic,
                methodType,
                methodName));
            currentClass().addMethod(methodName, methodType);
        }
    }
}
// Handles, temporarily the "return" of the method. Eventually the return
// would change to avoid the "return" keyword which will be used only
// when returning from closures
class ReturnTransformer extends LineTransformer {
    private final Pattern returnPattern = Pattern.compile("\\^\\s+(.+)");

    ReturnTransformer(RyzClassState state) {
        super(state);
    }

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

    StatementTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = statementPattern.matcher(line);
        if( m.matches() ) {
            generatedSource.add( String.format("    /*invocation*/%s;%n",
                    line));
            
        }
        
    }
}
class SimpleAssignmentTransformer extends LineTransformer {

    private final Pattern assignPattern = Pattern.compile("(\\w+\\s*=\\s*\\w+\\s*)");

    SimpleAssignmentTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = assignPattern.matcher(line);
        if(m.matches()){
            generatedSource.add( String.format("/*assignment*/ %s;%n", m.group(1)));

        }

    }
}
