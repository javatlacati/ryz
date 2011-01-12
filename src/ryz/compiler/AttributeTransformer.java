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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// Some utility functions
import static ryz.compiler.LineTransformer.checkObjectInitialization;
import static ryz.compiler.LineTransformer.inferType;
import static ryz.compiler.LineTransformer.scapeName;

/**
 * Date: 1/10/11
 * Time: 2:08 PM
 */
class AttributeTransformer extends LineTransformer {

    private static final String literalInitialValue = " = %s;";
    private static final String regexInitialValue = " = java.util.regex.Pattern.compile(\"%s\");%n";
    private static final String dateInitialValue = " = $sdf$GetDate(\"%s 00:00:00\");";
    private static final String blockInitialValue = " = /* block */ new Runnable(){%n    public void run(){%n";


    // [+#~-] hola ...
    private final static Pattern scopePattern = regexp("([+#~-])\\s*.+");
    private final static Pattern blockPattern = regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=(\\s*)?\\{");


    private static final List<Match> matchers = Arrays.asList(
        // + __ hola : String
        Match.declaration(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*:\\s*(\\w+)")),
        // + __ hola = String()
        Match.initialization(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))")),
        // + __ hola : String = a
        Match.typeAndInit(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)")),
        // + __ hola : String = a() // not used yet
        Match.typeAndInit(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+\\s*\\(.*\\))")),
        // + __ hola = 1
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\\d+)"),             "int",       literalInitialValue),
        // + __ hola = "uno"
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\".*\")"),           "String",   literalInitialValue),
        // + __ hola = true
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(true|false)"),       "boolean",  literalInitialValue),
        // + __ hola = 'c'
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*('.')"),              "char",     literalInitialValue),
        // + __ hola = 2011-01-06
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*(\\d{4}-\\d{2}-\\d{2})"), "java.util.Date", dateInitialValue),
        // + __ hola = /^(\d*)$/
        Match.literal(regexp("[+#~-]??\\s*(__)?\\s*(\\w+)\\s*=\\s*\\/\\^(.*)\\$\\/"),       "java.util.regex.Pattern", regexInitialValue),
        // hola = {
        // }
        Match.literal(blockPattern, "Runnable", blockInitialValue)

    );

    private static Pattern regexp(String re) {
        return Pattern.compile(re);
    }

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
            variable = matcher.matches(line, variable);
        }
        if (blockPattern.matcher(line).matches()) {
            currentClass().insideBlock();
        }


        if (variable != null) {
            String accessModifier = getScope(line, this.includeScope, scopePattern, "private");
            boolean added = currentClass().addVariable(accessModifier, variable.name, variable.type);
            String type = added ? variable.type : "";

            generatedSource.add(String.format("    /*attribute*/%s %s %s %s %s %n",
                    accessModifier,
                    variable.staticOrInstance,
                    type,
                    variable.name,
                    variable.initialValue));

        }

    }


}

abstract class Match {

    final Pattern pattern;

    public Match(Pattern pattern) {
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

    protected abstract Variable variableFrom(Matcher matcher);

    static Match declaration(Pattern pattern) {
        return new DeclarationMatcher(pattern);
    }

    public static Match initialization(Pattern pattern) {
        return new InitMatcher(pattern);
    }

    public static Match typeAndInit(Pattern pattern) {
        return new TypeWithInitMatcher(pattern);
    }

    public static Match literal(Pattern pattern, String literalType, String format) {
        return new LiteralMatcher(pattern, literalType, format);
    }
}

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

class InitMatcher extends Match {
    public InitMatcher(Pattern pattern) {
        super(pattern);
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        return new Variable(scapeName(matcher.group(2)),
                scapeName(inferType(matcher.group(3))),
                staticOrInstance(matcher),
                " = " + scapeName(checkObjectInitialization(matcher.group(3))) + ";");
    }
}

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

class LiteralMatcher extends Match {

    private final String literalType;
    private final String format;

    public LiteralMatcher(Pattern pattern, String literalType, String format) {
        super(pattern);
        this.literalType = literalType;
        this.format = format;
    }

    @Override
    protected Variable variableFrom(Matcher matcher) {
        return new Variable(scapeName(matcher.group(2)),
                literalType,
                staticOrInstance(matcher),
                String.format(format,
                        matcher.group(3).replaceAll("\\\\", "\\\\\\\\")));
    }
}


class Variable {
    String name;
    String type;
    String staticOrInstance = "";
    String initialValue = ";";


    Variable(String name, String type) {
        this.name = name;
        this.type = type;

    }

    Variable(String name, String type, String staticOrInstance, String initialValue) {
        this(name, type);
        this.staticOrInstance = staticOrInstance;
        this.initialValue = initialValue;
    }

}
