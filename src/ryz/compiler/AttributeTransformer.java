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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Some utility functions
import static ryz.compiler.LineTransformer.checkObjectInitialization;
import static ryz.compiler.LineTransformer.scapeName;

/**
 * Date: 1/10/11
 * Time: 2:08 PM
 */
class AttributeTransformer extends LineTransformer {
	
	private static final String boolInitialValue    = " = %s;";
    private static final String literalInitialValue = " = %s;";
    private static final String multiLineInitialValue = " =  %s"+lineSeparatorRepresentation+"\"";
    private static final String regexInitialValue = " = java.util.regex.Pattern.compile(\"%s\");%n";
    private static final String dateInitialValue = " = ryz.lang.DateLiteral.valueOf(\"%s 00:00:00\");";
    private static final String blockInitialValue = " = /* block */ new ryz.lang.block.Block%s<%s %s>(){%n    public %s run(%s){%n";


    // [+#~-] hola ...

    //\((.*)\)\s*:\s*(\w+)
    private final static Pattern blockPattern = Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\\((.*)\\)|\\((.*)\\)\\s*:\\s*((\\w+)))?\\s*\\{");
    //private final static Pattern blockPattern = regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=(\\s*)?\\{");
    private static final Pattern multilineString = Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\".*[^\"])");


    private static final List<Match> matchers = Arrays.asList(
            // hola : String* // varargs
            Match.declaration(Pattern.compile("(__)?(\\w+)\\s*:\\s*((\\w+)\\*)")),
            // + __ hola : String
            Match.declaration(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*:\\s*(\\w+)")),
            // + __ hola = String()
            Match.initialization(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))")),
            // + __ hola : String = a
            Match.typeAndInit(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)")),
            // + __ hola = 1
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\\d+)"), "int", literalInitialValue),
            // + __ hola = "uno
            Match.literal(multilineString, "String", multiLineInitialValue),
            // + __ hola = "uno"
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\".*\")"), "String", literalInitialValue),
            // + __ hola = true
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(true|false)"), "Boolean", boolInitialValue),
            // + __ hola = 'c'
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*('.')"), "char", literalInitialValue),
            // + __ hola = 2011-01-06
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\\d{4}-\\d{2}-\\d{2})"), "java.util.Date", dateInitialValue),
            // + __ hola = /^(\d*)$/
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*\\/\\^(.*)\\$\\/"), "java.util.regex.Pattern", regexInitialValue),
            // hola = {
            // }
            Match.block(blockPattern, "ryz.lang.block.Block%s<%s %s>", blockInitialValue),
            // hola = null
            Match.literal(Pattern.compile("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(null)"), "java.lang.Object", literalInitialValue)

    );


    private final boolean includeScope;

    public AttributeTransformer(RyzClassState state, boolean includeScope) {
        super(state);
        this.includeScope = includeScope;
    }

    public AttributeTransformer(RyzClassState state) {
        this(state, true);
    }



    @Override
    public void transform(String line, List<String> generatedSource) {


        Variable variable = null;
        for (Match matcher : matchers) {
            matcher.setTransformer(this);
            variable = matcher.matches(line, variable);
        }

        // add the variable to the class 
        if (variable != null) {
            String accessModifier = getScope(line, this.includeScope, "private");
            boolean added = currentClass().addVariable(accessModifier, variable.name, variable.type);
            String type = added ? variable.type : "";
            if(logger.isLoggable(Level.FINEST) && !added) {
                logger.finest(currentClass().methods().toString());
            }

            generatedSource.add(String.format("    /*attribute*/%s %s %s %s %s %n",
                    accessModifier,
                    variable.staticOrInstance,
                    type,
                    variable.name,
                    variable.initialValue));
            if (blockPattern.matcher(line).matches()  ) {
                currentClass().insideBlock(String.format("%s:%s", variable.parameters, variable.returnType));
            }

        }


        // and change the class state
        if( multilineString.matcher(line).matches()){
            //If a multiline string is found
            //get the identation to be used in the
            // new lines
            Matcher matcher = multilineString.matcher(line);
            matcher.matches();
            int indentation = 0;
            String multilineBegin = matcher.group( 3 );
            while( Character.isWhitespace( multilineBegin.charAt( ++indentation )));
            currentClass().insideMultilineString(--indentation);
        }
        

    }


}

/**
 * Abstract class to match variables declaration.
 */
abstract class Match {

    private final Pattern pattern;
    LineTransformer transformer;
    Match(Pattern pattern) {
        this.pattern = pattern;
    }

    Variable matches(String line, Variable variable) {
        Matcher m = pattern.matcher(line);
        if (m.matches()) {
            return variableFrom(m);
        }
        return variable;
    }

    static String staticOrInstance(Matcher matcher) {
        return matcher.group(1) == null ? "" : "static";
    }

    // Factory methods
    static Match declaration(Pattern pattern) {
        return new DeclarationMatcher(pattern);
    }

    static Match initialization(Pattern pattern) {
        return new InitMatcher(pattern);
    }

    static Match typeAndInit(Pattern pattern) {
        return new TypeWithInitMatcher(pattern);
    }

    static Match literal(Pattern pattern, String literalType, String format) {
        return new LiteralMatcher(pattern, literalType, format);
    }
    static Match block(Pattern pattern, String literalType, String format) {
        return new BlockLiteralMatcher(pattern, literalType, format);
    }

    protected abstract Variable variableFrom(Matcher matcher);

    public LineTransformer getTransformer() {
        return transformer;
    }

    public void setTransformer(LineTransformer transformer) {
        this.transformer = transformer;
    }
}

/**
 * Matches variable declaration such as xyz : Type = InitialValue
 */
class TypeWithInitMatcher extends Match {
    public TypeWithInitMatcher(Pattern pattern) {
        super(pattern);
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        return new Variable(scapeName(matcher.group(2)),
                scapeName(matcher.group(3)),
                staticOrInstance(matcher),
                " = " + scapeName(checkObjectInitialization(matcher.group(4))) + ";");
    }
}

/**
 * Matches variableName '=' InitialValue
 */
class InitMatcher extends Match {
    public InitMatcher(Pattern pattern) {
        super(pattern);
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        return new Variable(scapeName(matcher.group(2)),
                scapeName(getTransformer().inferType(matcher.group(3))),
                staticOrInstance(matcher),
                " = " + scapeName(checkObjectInitialization(matcher.group(3))) + ";");
    }
}

/**
 * Matches name ':' Type
 */
class DeclarationMatcher extends InitMatcher {
    public DeclarationMatcher(Pattern pattern) {
        super(pattern);
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        Variable v = super.variableFrom(matcher);
        v.initialValue = ";";
        return v;
    }
}

/**
 * Matches literals   name = 1 or name = "Hola"
 */
class LiteralMatcher extends Match {

    final String literalType;
    final String format;

    public LiteralMatcher(Pattern pattern, String literalType, String format) {
        super(pattern);
        this.literalType = literalType;
        this.format = format;
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        String group3 = matcher.group(3) == null ? "" : matcher.group(3);
        return new Variable(scapeName(matcher.group(2)),
                literalType,
                staticOrInstance(matcher),
                String.format(format,
                        group3.replaceAll("\\\\", "\\\\\\\\")));// only for regex
    }
}
class BlockLiteralMatcher extends LiteralMatcher {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public BlockLiteralMatcher(Pattern pattern, String literalType, String format) {
        super(pattern, literalType, format);
    }
    @Override
    protected Variable variableFrom(Matcher matcher) {
        logger.finest(matcher.pattern().toString());
        for (int i = 0; i < matcher.groupCount(); i++) {
            logger.finest("m.group(" + i + ") = " + matcher.group(i));
        }
        String params = "";
        String returnType = "Void";
        if (matcher.group(3) != null ) {
            int paramsIndex = 4;
            if( matcher.group(6) != null ) {
                returnType = scapeName( matcher.group(6) );
                paramsIndex = 5;
            }
            params = matcher.group(paramsIndex);
        }
        String parameters = transformer.transformParameters(params);
        List<ParameterInfo> parameterInfo = ParameterInfo.parse(parameters);
        String types = ParameterInfo.getTypes(parameterInfo);
        int size = parameterInfo.size();
        return new Variable(scapeName(matcher.group(2)),
                String.format(literalType, size, returnType, types),
                staticOrInstance(matcher),
                String.format(format,
                        size,
                        returnType,
                        types,
                        returnType,
                        parameters),parameters,returnType);
    }


}


class Variable {
    final String name;
    final String type;
    String staticOrInstance = "";
    String initialValue = ";";
    public String parameters;
    public String returnType;


    private Variable(String name, String type) {
        this.name = name;
        this.type = type;

    }

    Variable(String name, String type, String staticOrInstance, String initialValue) {
        this(name, type);
        this.staticOrInstance = staticOrInstance;
        this.initialValue = initialValue;
    }

    Variable(String name, String type, String staticOrInstance, String initialValues, String parameters, String returnType) {
        this(name, type, staticOrInstance, initialValues);
        this.parameters = parameters;
        this.returnType = returnType;

    }
}
