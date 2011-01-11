/*
 * Copyright (c)  2010 - 2011, Oscar Reyes and Ryz language developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the folLowing disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * - Neither the name of Ryz nor the names of its contributors may be used
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
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ryz.compiler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date: 1/10/11
 * Time: 2:08 PM
 */
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
            = Pattern.compile("([+#~-])\\s*.+");
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



    // hola = 0 // TODO:  how to test ( and capture ) the presence of "__" ?
    private final Pattern attributeIntegerInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(\\d+)");
    // __ hola = 0
    private final Pattern classAttributeIntegerInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*=\\s*(\\d+)");

    // hola = "s"
    private final Pattern attributeStringInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(\".*\")");
    // __ hola = "s" // TODO:  how to test ( and capture ) the presence of "__" ?
    private final Pattern classAttributeStringInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*=\\s*(\".*\")");


    // hola = true
    private final Pattern attributeBooleanInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(true|false)");
    // __ hola = false
    private final Pattern classAttributeBooleanInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{0,2}\\s*(\\w+)\\s*=\\s*(true|false)");


    // hola = 'a'
    private final Pattern attributeCharacterInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*('.')");
    // __ hola = 'b'
    private final Pattern classAttributeCharacterInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*=\\s*('.')");

    // hola = '31-12-2010'
    private final Pattern attributeDateInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(\\d{4}-\\d{2}-\\d{2})");

    // __ hola = '31-12-2010'
    private final Pattern classAttributeDateInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*=\\s*(\\d{4}-\\d{2}-\\d{2})");

    // hola = /^\d{2}-\d{2}-\d{4}$/
    private final Pattern attributeRegexInferencePattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*\\/\\^(.*)\\$\\/");

    // __ hola = /^\d{2}-\d{2}-\d{4}$/
    private final Pattern classAttributeRegexInferencePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*=\\s*\\/\\^(.*)\\$\\/");


    // TODO:  create test to infer from method call a = someMethod()
    // hola = adios()
    private final Pattern inferenceFromInvocationPattern
            = Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))");

    // __ hola : adios = xyz
    private final Pattern initializedClassAttributePattern
            = Pattern.compile("[+#~-]??\\s*_{2}\\s*(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)");

    // a = {\n}
    private final Pattern blockPattern =
             Pattern.compile("[+#~-]??\\s*(\\w+)\\s*=\\s*\\{");




    @Override
    public void transform(String line, List<String> generatedSource) {


        Matcher matcher = pattern.matcher(line);

        // TODO: plenty of room for refactoring here :)
        //TODO: default must be final
        String attributeName = null;
        String attributeType = null;
        String instanceOrStatic = "";
        String initialValue = ";"; // is ";" or  "= xyz";
        if( matcher.matches()){
            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(matcher.group(2));

        } else if( (matcher = initializedClassAttributePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = scapeName(matcher.group(2));
            initialValue = " = "+ scapeName(checkObjectInitialization(matcher.group(3))) + ";";
            instanceOrStatic = "static";

        } else if ( (matcher = initializedFromInvocation.matcher(line)).matches() ){
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
        } else if( (matcher = blockPattern.matcher(line)).matches() ){

            attributeName = scapeName(matcher.group(1));
            attributeType = "Runnable"; //TODO: should use class "Block" interface instead

            initialValue = String.format(" = /* block */ new Runnable(){%n    public void run(){%n");
            currentClass().insideBlock();

        } else if( ( matcher = attributeIntegerInferencePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = "int";
            initialValue  = " = "+ matcher.group(2) + ";";
        } else if( (matcher = classAttributeIntegerInferencePattern.matcher(line)).matches()) {
            attributeName = scapeName(matcher.group(1));
            attributeType = "int";
            initialValue  = " = "+ matcher.group(2) + ";";
            instanceOrStatic = "static";
        } else if( ( matcher = attributeStringInferencePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = "String";
            initialValue  = " = "+ matcher.group(2) + ";";
        } else if( (matcher = classAttributeStringInferencePattern.matcher(line)).matches()) {
            attributeName = scapeName(matcher.group(1));
            attributeType = "String";
            initialValue  = " = "+ matcher.group(2) + ";";
            instanceOrStatic = "static";
        } else if( ( matcher = attributeBooleanInferencePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = "boolean";
            initialValue  = " = "+ matcher.group(2) + ";";
        } else if( (matcher = classAttributeBooleanInferencePattern.matcher(line)).matches()) {
            attributeName = scapeName(matcher.group(1));
            attributeType = "boolean";
            initialValue  = " = "+ matcher.group(2) + ";";
            instanceOrStatic = "static";
        } else if( ( matcher = attributeCharacterInferencePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = "char";
            initialValue  = " = "+ matcher.group(2) + ";";
        } else if( (matcher = classAttributeCharacterInferencePattern.matcher(line)).matches()) {
            attributeName = scapeName(matcher.group(1));
            attributeType = "char";
            initialValue  = " = "+ matcher.group(2) + ";";
            instanceOrStatic = "static";
        } else if( ( matcher = attributeDateInferencePattern.matcher(line)).matches() ){ // TODO: this will get messy very quickly, refactor
            attributeName = scapeName(matcher.group(1));
            attributeType = "java.util.Date";
            initialValue  = String.format(";{%n" +
                    "    try {%n" +
                    "      %s = $sdf$.parse(\"%s 00:00:00\");%n" +
                    "    } catch( java.text.ParseException pe ) {%n" +
                    "      throw new IllegalStateException(\"Error in the compiler while identifying a date literal. Original message: \" + pe.getMessage());\n" +
                    "    }%n" +
                    "  }%n", attributeName, matcher.group(2));
        } else if( (matcher = classAttributeDateInferencePattern.matcher(line)).matches()) { // TODO: this will get messy very quickly, refactor
            attributeName = scapeName(matcher.group(1));
            attributeType = "java.util.Date";
            initialValue  = String.format(";{%n" +
                    "    try {%n" +
                    "      %s = $sdf$.parse(\"%s 00:00:00\");%n" +
                    "    } catch( java.text.ParseException pe ) {%n" +
                    "      throw new IllegalStateException(\"Error in the compiler while identifying a date literal. Original message: \" + pe.getMessage());\n" +
                    "    }%n" +
                    "  }%n", attributeName, matcher.group(2));
            instanceOrStatic = "static";
        } else if( ( matcher = attributeRegexInferencePattern.matcher(line)).matches() ){
            attributeName = scapeName(matcher.group(1));
            attributeType = "java.util.regex.Pattern";
            initialValue  = String.format(" = java.util.regex.Pattern.compile(\"%s\");%n", matcher.group(2).replaceAll("\\\\","\\\\\\\\"));
        } else if( (matcher = classAttributeRegexInferencePattern.matcher(line)).matches()) {
            attributeName = scapeName(matcher.group(1));
            attributeType = "java.util.regex.Pattern";
            initialValue  = String.format(" = java.util.regex.Pattern.compile(\"%s\");%n", matcher.group(2).replaceAll("\\\\","\\\\\\\\")     );
            instanceOrStatic = "static";
        }





        if( attributeName != null ) {
            String accessModifier = getScope(line, this.includeScope, scopePattern, "private");
            boolean added = currentClass().addVariable( accessModifier , attributeName, attributeType );
            String type = added ? attributeType : "" ;

            generatedSource.add( String.format("    /*attribute*/%s %s %s %s %s %n",
                accessModifier,
                instanceOrStatic,
                type,
                attributeName,
                initialValue));

        }

    }

}
