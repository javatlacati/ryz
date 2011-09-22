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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: Oct 15, 2010
 * Time: 8:55:48 AM
 */
abstract class LineTransformer {

    final static String lineSeparator = System.getProperty("line.separator");
    final static String lineSeparatorRepresentation = lineSeparator.equals("\n")?"\\n":"\\r\\n";
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
        } else if ( name.equals("Bool") ) { // TODO: Bool should be a user defined type, not a keyword
            return "Boolean";
        } else if( name.endsWith("*")) {
            return name.substring(0,name.length()-1) + " ... ";
        }

        return name;
    }
    
    public abstract void transform(String line, List<String> generatedSource);

    static String checkObjectInitialization(String initialValue) {
        if( Character.isUpperCase(initialValue.charAt(0)) && initialValue.matches("\\w*\\(.*\\)")){
            initialValue = "new " + initialValue;
        }
        return initialValue;
    }
    String inferType(String initialValue) {
        Matcher m = Pattern.compile("(.*)\\(.*\\)").matcher(initialValue);
        if( Character.isUpperCase(initialValue.charAt(0)) && m.matches()){
            return m.group(1);
        }
        // Handles if the method belongs to this class and
        // has already been defined ( no forward references yet )
        // TODO: add more scenarios
        if( initialValue.contains("(")) {
            for(String method : currentClass().methods()) {
                String [] nameType = method.split(":");
                if( nameType[0].equals(initialValue.substring(0,initialValue.indexOf("(")))) {
                    return nameType[1];
                }
            }
        }
        return initialValue;
        
    }

    String getScope(String line, boolean includeScope,
                    String defaultScope){
        final Pattern pattern = Pattern.compile("([+#~-])\\s*.+");
        if(!includeScope) {
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

    /**
     * Given a match of listed parameters such as "i : Int, s: String" this
     * method returns the correct Java representation: "int i, String s"
     * @param matchedParameters The string representing the parameters in a Ryz method or block
     * @return a list with the Java representation
     */
    protected final String transformParameters(String matchedParameters) {
        logger.finest("matchedParameters= "+matchedParameters);
        if( matchedParameters.startsWith("(") && matchedParameters.endsWith(")")){
            matchedParameters = matchedParameters.substring(1,matchedParameters.length()-1);
        }
        if( matchedParameters.trim().equals("") ) {
            return "";
        }
        List<String> generatedSource = new ArrayList<String>();
        currentClass().insideParameters();
        String parameters;// Transform the parameters as if they were variables
        // start --
        int linesSoFar = generatedSource.size();
        LineTransformer lineTransformer =
                new AttributeTransformer(this.currentClass().state(), false);

        // do transform the parameter
        for( String param : matchedParameters.trim().split("\\s*,\\s*")) {
            lineTransformer.transform(param, generatedSource);
        }
        parameters = separeteWithComma(generatedSource, linesSoFar);


        // remove the generated  sources
        int removeN = generatedSource.size() - linesSoFar;
        for( int i = 0 ; i < removeN ; i++ ) {
            generatedSource.remove(generatedSource.size()-1);
        }
        currentClass().state().previousState();
        // -- finish
        return parameters;
    }

    /**
     * Used by @transformParameters method to the output generated by @AttributeTransformer
     * like this:
     * <pre>
     * int i;
     * String b;
     * </pre>
     * To <pre>int i, String b</pre> which is needed by the paramters processing.
     * @param generatedSource - Where to find the attributes added by @AttributeTransformer
     * @param linesSoFar - where was the generated output *before* the attributes where
     * generated to know where to extract from.
     * @return - a String with the generated parameter list.
     */
    private String separeteWithComma(List<String> generatedSource, int linesSoFar) {
        String parameters;// remove ";" and add "," instead
        StringBuilder builder = new StringBuilder();
        for( String s : generatedSource.subList(linesSoFar,
                            generatedSource.size())) {
            builder.append(s.substring(0,s.length()-(lineSeparator.length() + 2 )));
            builder.append(",");
        }

        // remove last ","
        builder.deleteCharAt(builder.length()-1);
        parameters = builder.toString();
        return parameters;
    }
}
class ImportTransformer extends LineTransformer {
    private final Pattern importPattern = Pattern.compile("(import|import(Static))\\s*\\((.+)\\s*\\)");

    ImportTransformer(RyzClassState state) {
        super(state);
    }


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = importPattern.matcher(line);
        if( m.matches() ){
            if( m.matches() ) {
                generatedSource.add( String.format("%s %s;%n", m.group(2) == null ? "import" : "import static",m.group(3)));
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
                String previousLine = generatedSource.get(generatedSource.size() - 1);
                String putBeforeClass = "";
                if( previousLine.startsWith("/*annotation*/")){
                    generatedSource.remove(generatedSource.size()-1);
                    putBeforeClass = previousLine;


                }
                generatedSource.add(String.format("package %s;%n", packageName));
                String extendsOrImplements = isInterface( possibleSuperClass ) ?
                        "implements" :
                        "extends";
                String className = scapeName(possibleClass);
                //TODO: solve what to do with public/nonpublic class in the same source file
                generatedSource.add(String.format("/*import */import ryz.lang.Extensions;%n"));
                generatedSource.add(String.format("/*import static*/import static ryz.lang.Extensions.*;%n"));
                generatedSource.add(String.format("/*import static*/import static java.lang.System.out;%n"));
                generatedSource.add(String.format(
                        "%s" + // probably a class annotation
                        "public class %s %s %s { %n" +
                        "    //private final %s self = this;%n",
                        putBeforeClass,
                        className,
                        extendsOrImplements,
                        scapeName(possibleSuperClass),
                        className));
                this.currentClass().packageName(packageName);
                this.currentClass().className(className);
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
    private boolean atLestOneLineProcessed = false;

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
            // End of multiline string was found. 
            // We have to remove the ficticous line
            String last = generatedSource.remove(generatedSource.size()-1);
            logger.finest( "Last line written: " + last );
            // And replace it with the original one
            // To do it we have to trim the ficticious end of line added:

            // first we need to know some lenghts 
            // line separator lenght: \n or \r\n ( 1 or 2 )
            int lsl = lineSeparator.length();
            // line separator reprenentation lenght ( 2 or 4 ) 
            int lsrl = lineSeparatorRepresentation.length();
            // And get the original line 
            int originalStringSize = last.length() - (lsl + (atLestOneLineProcessed?( lsrl + 1 ):2));

            String originalString  = last.substring( 0, originalStringSize );
            generatedSource.add( originalString + "\";" + lineSeparator);
            currentClass().outsideMultilineString();
        } else {
            // Add the line.
            // For instance if line is:
            //    a value
            // transform it to something like:
            //  +"   a value\n"\n
            //That is
            //  string literal  : \"
            //  original string scaping all the " to \"
            //  line terminaror representation: \n ( or \r\n in windows ) 
            //  line separator  : \n  ( or \r\n in windows )
            generatedSource.add( "+\"" 
                                + indentation
                                +line.replace("\"", "\\\"") 
                                + lineSeparatorRepresentation 
                                + "\"" + lineSeparator );
            // set the state to know that at least one of these 
            // was modified.
            atLestOneLineProcessed = true;
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
        }else if( currentClass().state() instanceof InsideCommentState  ) {
                generatedSource.add(line.startsWith("import") ? "-":""+line + lineSeparator);
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
            logger.finest("currentClass().state() = " +  currentClass().state() ); 
            String indentation;
            if( currentClass().state() instanceof InsideBlockState ) { 
                 indentation = "/*ib*/";
                 this.currentClass().closeKey();
                 generatedSource.add(indentation +line + ";"+lineSeparator);
            } else if( currentClass().state() instanceof InsideMethodState ) { 
                indentation = "    ";
                generatedSource.add(indentation +line + lineSeparator);
                this.currentClass().closeKey();
             }  else { 
                 indentation = "/**/";
             generatedSource.add(indentation +line + lineSeparator);
                 this.currentClass().closeKey();
             }
         }
     }
}

class ConstructorTransformer extends LineTransformer {

    //TODO: use a single pattern
    // hola() {
    private final Pattern voidMethodPattern  = Pattern.compile("[+#~-]??\\s*([\\$\\w]+)\\((.*)\\)\\s*\\{");
    ConstructorTransformer(RyzClassState state) {
        super(state);
    }


    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher;

        String accessModifier = "public";

        String constructorName = null;
        // define the values
        if( ( matcher = voidMethodPattern.matcher(line)).matches() ){
            accessModifier = getScope(line, true, "public");
            constructorName = scapeName(matcher.group(1));
        }
        // build the source
        if( constructorName != null && Character.isUpperCase(constructorName.charAt(0))) {
            logger.finest(String.format("scope %s for line %s %n", accessModifier, line));
            currentClass().addConstructor(constructorName);
            String parameters = transformParameters(matcher.group(2));
            generatedSource.add( String.format("    /*constructor*/%s %s(%s) {%n",
                accessModifier,
                constructorName,
                parameters));
        }
    }
}

class MethodTransformer extends LineTransformer {

    //TODO: use a single pattern
    // hola( *parameter_list_goes_here* ):String{
    private final Pattern methodPattern      = Pattern.compile("[+#~-]??\\s*([\\$\\w]+)\\s*\\((.*)\\)\\s*:\\s*(\\w+)\\s*\\{");
    // __ hola() : String {
    private final Pattern classMethodPattern = Pattern.compile("[+#~-]??\\s*_{2}\\s*([\\$\\w]+)\\s*\\((.*)\\)\\s*:\\s*(\\w+)\\s*\\{");
    // hola() {
    private final Pattern voidMethodPattern  = Pattern.compile("[+#~-]??\\s*([\\$\\w]+)\\s*\\((.*)\\)\\s*\\{");
    // __ hola() {
    private final Pattern voidClassMethodPattern = Pattern.compile("[+#~-]??\\s*_{2}\\s*([\\$\\w]+)\\s*\\((.*)\\)\\s*\\{");

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
        // define the values

        if( (matcher = methodPattern.matcher(line)).matches() ) {
            accessModifier = getScope(line, true, "public");
            methodType = scapeName(matcher.group(3));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "";
        } else if ( (matcher = classMethodPattern.matcher(line)).matches() ){
            accessModifier = getScope(line, true, "public");
            methodType = scapeName(matcher.group(3));
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        } else if( ( matcher = voidMethodPattern.matcher(line)).matches() ){
            accessModifier = getScope(line, true, "public");
            methodType = "void";
            instanceOrStatic = "";
            String name = scapeName(matcher.group(1));

            // main() {  is special, will create public static void main( String [] args )
            if( "main".equals(name)){
                addMainMethod(generatedSource);
            } else {
                methodName = scapeName(name);
            }
        } else if( ( matcher = voidClassMethodPattern.matcher(line)).matches()){

            accessModifier = getScope(line, true, "public");
            methodType = "void";
            methodName = scapeName(matcher.group(1));
            instanceOrStatic = "static";
        }

        // build the source
        if( methodName != null && !Character.isUpperCase(methodName.charAt(0))) {
            logger.finest(String.format("scope %s for line %s %n", accessModifier, line));
            currentClass().addMethod(methodName, methodType);
            String parameters = transformParameters(matcher.group(2));
            generatedSource.add( String.format("    /*method*/%s %s %s %s(%s) {%n",
                accessModifier,
                instanceOrStatic,
                methodType,
                methodName,
                parameters));

        }
    }

    private void addMainMethod(List<String> generatedSource) {
        generatedSource.add(String.format(
                "    /*method*/public static void main( String [] args ) {\n" +
               "  new %s().main();\n" +
               "}\n" +
               "    /*method*/public void main() {%n", currentClass().className()));
        currentClass().addMethod("main","void");
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
class InlineBlockTransformer extends LineTransformer {
    // TODO: Handle primitive int he "run" signature when block's parametrized type is Integer
    // eg. new Block0<Void,Integer>(){ public Void run( int i ) { return null; }};
    //something.toString(somethingElse)
    private static final Pattern statementPattern = Pattern.compile(
            "((\\w+)\\s*(\\.\\s*[\\$\\w]+)*)\\s*\\(\\s*" +
                   //(\((.*)\)|\((.*)\)\s*:\s*((\w+)))
                    "(\\((.*)\\)|\\((.*)\\)\\s*:\\s*((\\w+))|\\s*)\\s*\\{");

    InlineBlockTransformer(RyzClassState state) {
        super(state);
    }
    public void transform(String line, List<String> generatedSource) {
        Matcher m = statementPattern.matcher(line);
        if( m.matches() ) {
            logger.finest(m.pattern().toString());
            if( logger.isLoggable(Level.FINEST)) {
                for (int i = 0; i < m.groupCount(); i++) {
                    logger.finest("m.group(" + i + ") = " + m.group(i));
                }
            }
            String returnType = "Void";
            int indexParams = 5;
            if( m.group(4).trim().equals("")) {
                indexParams = 4;
            }
            if (m.group(7) != null) {
                returnType = m.group(7);
                indexParams = 6;
            }
            String parameters = transformParameters(m.group(indexParams));
            List<ParameterInfo> parameterInfo = ParameterInfo.parse(parameters);
            logger.fine("parameters " + parameters);
            logger.fine("line matched " + line);
            currentClass().insideBlock(String.format("%s:%s",parameters,returnType));
            generatedSource.add(String.format(
                    "    /*invocationwithblock*/ %s(new ryz.lang.block.Block%s<%s %s>(){%n    public %s run(%s){%n",
                    m.group(1),
                    parameterInfo.size(),
                    returnType,
                    ParameterInfo.getTypes(parameterInfo),
                    returnType,
                    parameters));
        }
    }
}

class StatementTransformer extends LineTransformer {

    private static final Pattern statementPattern = Pattern.compile("(\"[^\"]*\"|\\w+)\\s*(\\.\\s*[\\$\\w]+)*\\s*(\\(.*\\))");//something.toString(somethingElse)


    StatementTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = statementPattern.matcher(line);
        if( m.matches() ) {
            logger.finest(currentClass().className() +" variables: "+ currentClass().variables().toString());

            String expression = checkObjectInitialization(line);

            String invokedMethod = m.group(1);
            if( isBlockInvocation( invokedMethod )) {
                String args = m.group(3);
                args = args == null ? "" : args;
                expression = invokedMethod+".run"+ args +"";
            }
            generatedSource.add(String.format("    /*invocation*/%s;%n",
                    expression));
        }
    }
    private String currentMethod() {
       return currentClass().methods().isEmpty() ? "---" : currentClass().methods().get( currentClass().methods().size()-1);
    }
    private boolean isBlockInvocation( String methodInvocationName ) {
        if( methodInvocationName == null ) {
            return false;
        }
        String possibleBlock = methodInvocationName + ":ryz.lang.block.Block";
        try {
                return containsDeclaration( currentClass().variables().get("instance"), possibleBlock)
                        || containsDeclaration(currentClass().variables().get(currentMethod()), possibleBlock);
        }  catch( NullPointerException npe ) {
            // I'm really sorry for this,
            // but I'll fix it, I promise
            // TODO: replace Npe catch by
            // extracting this method into
            // the class that has the state
            //information
            // The NPE may originate if there are no
            // methods, or we are not inside a method
            return false;
        }

    }

    /** Checks if the possible block has already been declared in the list.
     * Since blocks types will start with ryz.lang.block.Block"N" where
     * N is the number of parameters received, this method checks
     * startsWith instead of contains.
     * @param list The list where to find the block name. This list should
     * be taked from either the current class atttributes or methods.
     * @param possibleBlock - The name to search for.
     * @return true if eny of the elements of the list start with the name of
     * the possible list.
     */
    private boolean containsDeclaration( List<String> list, String possibleBlock) {
        for (String s : list) {
            if( s.startsWith(possibleBlock)) {
                return true;
            }
        }
        return false;
    }
}
class SimpleAssignmentTransformer extends LineTransformer {

    private final Pattern assignPattern = Pattern.compile("(\\w+\\s*=\\s*(?!(true|false)$)\\w+\\s*)");

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

    private final Pattern singleValuePattern = Pattern.compile("\\w+");
    public SingleValueLineTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {

        Matcher m = singleValuePattern.matcher(line);
        if(m.matches()){
            generatedSource.add( String.format("/*expression*/ %s;%n", checkObjectInitialization(line))) ;
        }
    }
}

class AnnotationTransformer extends LineTransformer {
    private final Pattern annotationPattern = Pattern.compile("@[A-Z]\\w+\\s*(\\(.*\\))?.*");

    AnnotationTransformer(RyzClassState state) {
        super(state);
    }

    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher m = annotationPattern.matcher( line );
        if( m.matches()){
            generatedSource.add( String.format("/*annotation*/ %s%n", line));
        }
    }
}
