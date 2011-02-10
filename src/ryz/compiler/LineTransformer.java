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


import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: Oct 15, 2010
 * Time: 8:55:48 AM
 */
abstract class LineTransformer {

    final String lineSeparator = System.getProperty("line.separator");
    final Logger logger = Logger.getLogger(this.getClass().getName());
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

    LineTransformer(RyzClassState state) {
        this.currentClass = state.ryzClass();
    }




    RyzClass currentClass() {
        return currentClass;
    }

    static String scapeName(String name) {
        if( javaKeywords.contains(name)){
            return name + "$";
        } else if( "Int".equals(name)){ //TODO: Int should be a user defined type, not a keyword
            return "int";
        } else if( name.endsWith("*")) {
            return name.substring(0,name.length()-1) + " ... ";
        }

        return name;
    }
    
    public abstract void transform(String line, List<String> generatedSource);

    static String checkObjectInitialization(String initialValue) {
        if( Character.isUpperCase(initialValue.charAt(0)) && initialValue.matches(".*\\(.*\\)")){
            initialValue = "new " + initialValue;
        }
        return initialValue;
    }
    static String inferType(String initialValue) {
        Matcher m = Pattern.compile("(.*)\\(.*\\)").matcher(initialValue);
        if( Character.isUpperCase(initialValue.charAt(0)) && m.matches()){
            return m.group(1);
        }
        return initialValue;
        
    }

    String getScope(String line, boolean includeScope,
                    Pattern pattern, String defaultScope){

        if( includeScope == false ) {
            return "";
        }
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            if (matcher.group(1).equals("+")) {return "public";
            } else if (matcher.group(1).equals("#")) {
                return "protected";
            } else if (matcher.group(1).equals("~")) {
                return "";
            } else if (matcher.group(1).equals("-")) {
                return "private";
            }
        }

            return defaultScope;

    }
}
class ImportTransformer extends LineTransformer {
    private final Pattern importPattern = Pattern.compile("import\\s*\\((.+)\\s*\\)");

    ImportTransformer(RyzClassState state) {
        super(state);
    }


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = importPattern.matcher(line);
        if( m.matches() ){
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
                //TODO: solve what to do with public/nonpublic class in the same source file
                generatedSource.add(String.format("/*import static*/import static java.lang.System.out;%n"));
                generatedSource.add(String.format(
                        "public class %s %s %s { %n" +
                        "    private final static java.text.DateFormat $sdf$ =new java.text.SimpleDateFormat(\"yyyy-MM-dd hh:mm:ss\");%n"+
                        "    private final static java.util.Date $sdf$GetDate(String aDate){%n" +
                        "      try {%n" +
                        "        return $sdf$.parse(aDate);%n" +
                        "      } catch( java.text.ParseException pe ) {%n" +
                        "        throw new IllegalStateException(\"Error in the compiler while identifying a date literal. Original message: \" + pe.getMessage());\n" +
                        "      }%n" +
                        "    }%n"+
                        "    private final %s self = this;%n",
                        className,
                        extendsOrImplements,
                        scapeName(possibleSuperClass),
                        className));
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
            return !clazz.startsWith("java.lang")
                        && isInterface("java.lang." + clazz);
        }
    }

}
class MultilineStringTransformer extends LineTransformer {

    private final String indentation;

    public MultilineStringTransformer(RyzClassState state, int indentation) {
        super(state);
        StringBuilder builder = new StringBuilder();
        for( int i = 0 ; i < indentation ; i++ ) {
            builder.append(" ");
        }
        this.indentation = builder.toString();

    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.equals("\"")){
            String last = generatedSource.remove(generatedSource.size()-1);
            generatedSource.add(last.substring(0,last.length()-4)+"\";" + lineSeparator);
            currentClass().outsideMultilineString();
        } else {
            //TODO: add original file return (either \r\n or \n ) instead of just \n
            generatedSource.add( "+\""+indentation+line.replace("\"", "\\\"")+"\\n\""+ lineSeparator );
        }
    }
}
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
        if( line.startsWith("/*")){
            currentClass().insideComment();
        } else if( line.endsWith("*/") ){
            currentClass().outsideComment();    
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
            String indentation = currentClass().state() instanceof InsideMethodState ?
                    "    " : "/**/";
            generatedSource.add(indentation +line + lineSeparator);
            this.currentClass().closeKey();
        }
    }
}
class MethodTransformer extends LineTransformer {

    private final Pattern voidScopeInstancePattern = Pattern.compile("([+#~-])\\s*(\\w+)\\(\\)\\s*\\{");
    private final Pattern voidScopeClassPattern = Pattern.compile("([+#~-])\\s*_{2}\\s*(\\w+)\\(\\)\\s*\\{");


    private final Pattern scopeInstancePattern = Pattern.compile("([+#~-])\\s*(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    private final Pattern scopeClassPattern = Pattern.compile("([+#~-])\\s*_{2}\\s*(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");

    // hola():String{
    private final Pattern methodPattern = Pattern.compile("[+#~-]??\\s*(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    // __ hola() : String { 
    private final Pattern classMethodPattern = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    // hola() {
    private final Pattern voidMethodPattern = Pattern.compile("[+#~-]??\\s*(\\w+)\\(\\)\\s*\\{");
    // __ hola() {
    private final Pattern voidClassMethodPattern = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\(\\)\\s*\\{");

    //hola( args : String ) {
    private final Pattern methodWithParam = Pattern.compile("(\\w+)\\((.*)\\)\\s*\\{");

    MethodTransformer(RyzClassState state) {
        super(state);
    }


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher;

        String accessModifier = "public";

        String methodName = null;
        String methodType = null;
        String instanceOrStatic = null;
        String parameters = "";
        // define the values

        if( (matcher = methodPattern.matcher(line)).matches() ) {
            accessModifier = getScope(line, true, scopeInstancePattern, "public");
            methodType = scapeName(matcher.group(2));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "";
        } else if ( (matcher = classMethodPattern.matcher(line)).matches() ){
            accessModifier = getScope(line, true, scopeClassPattern, "public");
            methodType = scapeName(matcher.group(2));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        } else if( ( matcher = voidMethodPattern.matcher(line)).matches() ){
            accessModifier = getScope(line, true, voidScopeInstancePattern, "public");
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

            accessModifier = getScope(line, true, voidScopeClassPattern, "public");

            methodType = "void";
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        } else if ( ( matcher = methodWithParam.matcher(line)).matches() ) {

            String matchedParameters = matcher.group(2);

            logger.info(parameters);

            accessModifier = getScope(line, true, voidScopeClassPattern, "public");
            methodType = "void";
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "";
            parameters = transformParameters(generatedSource, matchedParameters);


        }

        // build the source
        if( methodName != null ) {
            logger.finest(String.format("scope %s for line %s %n", accessModifier, line));
            generatedSource.add( String.format("    /*method*/%s %s %s %s(%s) {%n",
                accessModifier,
                instanceOrStatic,
                methodType,
                methodName,
                parameters));
            currentClass().addMethod(methodName, methodType);
        }
    }

    private final String transformParameters(List<String> generatedSource, String matchedParameters) {
        String parameters;// Transform the parameters as if they were variables
        // start --
        int linesSoFar = generatedSource.size();
        LineTransformer lineTransformer = new AttributeTransformer(
                                                    this.currentClass().state(),
                                                    false);

        for( String param : matchedParameters.trim().split("\\s*,\\s*")) {
            lineTransformer.transform(param, generatedSource);
        }
        StringBuilder builder = new StringBuilder();
        for( String s : generatedSource.subList(linesSoFar,
                            generatedSource.size())) {
            builder.append(s.substring(0,s.length()-3));
            builder.append(",");
        }
        int removeN = generatedSource.size() - linesSoFar;
        for( int i = 0 ; i < removeN ; i++ ) {
            generatedSource.remove(generatedSource.size()-1);
        }

        builder.deleteCharAt(builder.length()-1);
        parameters = builder.toString();
        // -- finish
        return parameters;
    }
}
// Handles, temporarily the "return" of the method. Eventually the return
// would change to avoid the "return" keyword which will be used only
// when returning from closures
class ReturnTransformer extends LineTransformer {
    //TODO: fixme, shouldn't need  ^ to indicate return in regular cases only from early returns
    private final Pattern returnPattern = Pattern.compile("\\^\\s+(.+)");

    ReturnTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = returnPattern.matcher(line);
        if( m.matches() ){
            String returnValue = checkObjectInitialization(m.group(1));
            generatedSource.add( String.format("/* return */ %s;%n", returnValue));
        }
    }
}
class StatementTransformer extends LineTransformer {

    private final Pattern statementPattern = Pattern.compile("\\w+\\.\\w+\\(.*\\)");//something.toString(somethingElse)


    StatementTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = statementPattern.matcher(line);
        if( m.matches() ) {
            generatedSource.add(String.format("    /*invocation*/%s;%n",
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
//TODO: watchout, this may eventually process anything
class SingleValueLineTransformer extends LineTransformer {

    private final Pattern singleValuePattern = Pattern.compile("\\w+|\\w+\\(\\)");
    public SingleValueLineTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {

        Matcher m = singleValuePattern.matcher(line);
        if(m.matches()){
            generatedSource.add( String.format("/*expression*/ %s;%n", checkObjectInitialization(line)));

        }

    }
}