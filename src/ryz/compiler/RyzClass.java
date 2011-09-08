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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class would be used to hold the state of the current compiled class.
 *
 * @author oscarryz
 * Date: Dec 2, 2010
 */
class RyzClass {

    private final List<String> sourceLines;
    private final List<String> generatedSource = new ArrayList<String>();
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private String name;
    private String packageName;
    private RyzClassState state;

    private final List<String> methods;
    private final Map<String, List<String>> variables = new HashMap<String,List<String>>();
    private final String sourceFile;
    private final List<CompilationError> errors = new ArrayList<CompilationError>();
    private final List<String> constructors;

    public RyzClass(String sourceFile, List<String> sourceLines) {

        this.sourceFile = sourceFile;

        if( logger.isLoggable(Level.FINEST)){
            StringBuilder sb = new StringBuilder();
            for (String sourceLine : sourceLines) {
                sb.append(sourceLine);
                sb.append(LineTransformer.lineSeparator); 
            }
            logger.finest(sb.toString());
        }
        this.sourceLines = sourceLines;
        this.methods = new ArrayList<String>();
        this.constructors = new ArrayList<String>();
        state(new InitialState(this));
    }


    public void className(String theClassName ) {
        this.name = theClassName.trim();
        state().nextState();
    }


    public void packageName(String packageName) {
        this.packageName = packageName;
    }
    public String packageName() { 
        return this.packageName;
    }

    public String className() {
        if( name == null ){
            throw new IllegalStateException("Should have name by now");
        }
        return this.name;
    }

    List<String> methods(){
        return this.methods;
    }

      /**
        *  Map of list of names of the variables ( and variables ) defined in this class.
        * For instance, for method test() the variable a could have been defined,
        *  if variable a is redefined, this map will help to know if it was already there.
        *  The entry would be: { foo : [ a ] }
        *  If a is defined in the class ( as an instance attribute ) it will be kept in this map too as:
        *  { instance : [ a]  , foo : [ a ] }
        *
        * @return a map containing the defined variables ( variables ) in this class
        */
    Map<String,List<String>> variables(){
        return this.variables;
    }

    /**
     * Takes the list of source code lines and sends it to the transformers
     * to produce translated  ( java ) code.
     */
    public void transformSourceCode() {

        logger.fine("Processing " + sourceFile );
        generatedSource.add(String.format("//-- Created from: %s %n" , sourceFile()));
        int lineno = 0;
        for( String line : sourceLines ) {
            lineno++;
            int lsf = generatedSource.size();
            for( LineTransformer t : transformers() ) {
                t.transform( line, generatedSource );
            }
            if( lsf == generatedSource.size()
                    && !line.trim().equals("")
                    && !line.trim().equals("\"") ) {
              logger.info("Not processed [" + lineno + "]: " + line);
            }
        }
        
    }

    /**
     * Returns the list of translated source code.
     * @return  A list containing all the generated source code
     */
    public List<String> outputLines() {
        return generatedSource;
    }

    private List<LineTransformer> transformers() {
        return state().transformers();
    }

    /**
     * Close key means the current state has finished and we should return
     * to the previous state.
     */
    public void closeKey() {
        this.state().keyClosed();
    }

    private String lastElementAdded;
    public String lastElementAdded(){
        return this.lastElementAdded;
    }
    public void addMethod(String methodName, String methodType) {
        this.lastElementAdded = methodName + ":" + methodType;
        this.methods.add(lastElementAdded);// TODO: add args
        state().nextState();
    }
    public void addConstructor(String constructorName) {
        this.constructors.add( constructorName );
        this.lastElementAdded = constructorName;
        state().nextState();
        // TODO: change it for insideConstructor()
    }


    public void state(RyzClassState classState) {
        this.state = classState;
    }

    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        return state().addVariable(accessModifier, variableName, variableType);
    }

    /**
     * Makes this class to enter in "comment" mode so the lines are not
     * interpreted anymore
     */
    public void insideComment() {
        state().insideComment();
    }

    /**
     * Makes this class to enter in "outside comment" mode, returning to the
     * previous state
     */
    public void outsideComment() {
        state().outsideComment();
    }

    public String sourceFile() {
        return sourceFile;
    }

    public void markLastLineAsReturn() {
        String lastMethod = lastElementAdded();//methods().get(methods().size() - 1);
        String[] split = lastMethod.split(":");
        if ( split.length == 1 ){
            return ;
        }
        String type = split[1];
        markLastLineAsReturn(type);
    }
    public void markLastLineAsReturn(String returnType) {
        if(returnType.equals("void")) {
           return;
        }
        int lastElementIndex = generatedSource.size() - 2;
        String lastLine = generatedSource.remove(lastElementIndex);
        generatedSource.add( lastElementIndex,
                String.format(returnType.equals("Void") ?
                                    "%s%nreturn null;%n":
                                     "return %s%n",
                        lastLine));
    }


    public void insideBlock(String blockSignature) {
        state().insideBlock(blockSignature);
    }

    public void insideMultilineString(int indentation) {
        state().insideMultilineString(indentation);
    }

    public void outsideMultilineString() {
        state().outsideMultilineString();
    }

    RyzClassState state() {
        return state;
    }

    public void insideParameters() {
        state().insideParameters();
    }
    RyzClass reportExceptions() { 
       for( int i = 0 ; i < generatedSource.size() ; i++ ) {
           String s = generatedSource.get(i);
           if( s.startsWith("    /*method*/") || s.startsWith("    /*constructor*/") && ! s.endsWith("throws Exception {") ){
               generatedSource.set( i ,
                    s.substring( 0, s.length() - LineTransformer.lineSeparator.length() - 1 )
                     + " throws Exception { " + LineTransformer.lineSeparator
               );
           }
       }
       return this;
    }

  /**
     * Records the current error and takes the new source code as a fix.
   * @param fixedSourceCode - The new source code
   * @param errorCode  - Indicates what the error was
   * @param startPosition - error start position
   * @param position - Column of the line where the error appeared.
   */
    public void markError(String fixedSourceCode, String errorCode, int startPosition, int position) {
        errors.add( CompilationError.new$(errorCode, startPosition, position));
        this.generatedSource.clear();
        for( String line : fixedSourceCode.split("\n")) {
            this.generatedSource.add( line + "\n" );
        }
    }

  /**
     * Checks if the given information was already reported.
     *
   * @param code - The data of the error code.
   * @param startPosition - where the error first happened.
   * @param position - column where the error appeared
   * @return true if this exception was already reported.
     * @see #markError(String, String, int, int)
     */
    public boolean isNewProblem(String code, long startPosition, long position) {
        return !errors.contains(CompilationError.new$(code, startPosition, position));
    }


    /**
      * Bean to store a previous compilation error.
      */
    private static class CompilationError {
        private final String errorCode;
        private final long startPosition;
        private final long  position;

       public CompilationError(String errorCode, long startPosition, long position) {
            this.errorCode = errorCode;
            this.startPosition = startPosition;
            this.position = position;
        }

        public static CompilationError new$(String errorCode, long startPosition, long position) {
            return new CompilationError( errorCode, startPosition, position);
        }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;

           CompilationError that = (CompilationError) o;

           if (position != that.position) return false;
           if (startPosition != that.startPosition) return false;
           if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) return false;

           return true;
       }

       @Override
       public int hashCode() {
           int result = errorCode != null ? errorCode.hashCode() : 0;
           result = 31 * result + (int) (startPosition ^ (startPosition >>> 32));
           result = 31 * result + (int) (position ^ (position >>> 32));
           return result;
       }
   }

}
