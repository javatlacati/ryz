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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class defines a way to validate a given class against
 * the spec in the test classes. It is designed to have subclasses
 * and each subclass should provide the elements of the class to be passed
 * <br/>
 *
 * For instance, there may be attributes subclasses which will validate against
 * java.lang.Field instances. 
 *
 * @author oscarryz
 * Date: Dec 30, 2010
 * Time: 3:48:23 PM
 */
abstract class AssertStrategy {

    final Logger logger = Logger.getLogger(this.getClass().getName());

    // HashCode for the access modifiers
    private final static int PUBLIC     = 0xc5bdb269; // public
    private final static int PROTECTED  = 0xdbba6bae; // protected
    private final static int PRIVATE    = 0xed412583; // private
    private final static int DEFAULT    = 0x5c13d641; // default
    private final static int STATIC     = 0xcacdce6e;  // static
    private final static int INSTANCE   = 0x21169495; // instance

    /**
     * Asserts the given class fulfils the definition on the spec.
     * Each subclass must provide the the elements to validate.<br/>
     *
     * This method iterates them and validates they match.
     *
     * @param clazz - The class to be validated.
     * @param spec - The properties containing the values to compare against.
     * @param file - The Ryz source file name where this class was created from.
     */
    final void assertDefinition( Class clazz,
                             Properties spec,
                             String file ) {
        /*
         Values are taken from the specification.

         For instance if we validate attributes the values would be
         something like:

            private name : java.lang.String,  private age : java.lang.Integer
         */

         String values;

         // If there is nothing to validate the method exits.
         if( isNull(clazz)
                 || isNull(values=spec.getProperty(propertyToValidate()))
                 || isEmpty(values)) {
             return;
         }

        // The values to validate  are separated by comma,
        // we split and validate each one and we assert this class complies.
         for( String elementSpecification : values.split(",")){
             boolean matched = false;
             // Iterates the objects to validate.
             // They may be java.lang.reflect.Method or java.lang.reflect.Field
             // etc.
             // see @getObjectsToValidate
             for( Object o : getObjectsToValidate(clazz) ){
                // Do perform the validation 
                if( assertSpec( o, elementSpecification) ){
                    matched = true;
                    break;
                }
             }
             // Assert matched the spec or no
             assert matched :
                    String.format("%s validating \"%s\" didn't fulfilled: \"%s\"",
                             file,  propertyToValidate(), elementSpecification);
         }
     }

    protected abstract String propertyToValidate();


    /**
     * Subclasses should return an array of "things" to validate, they may be
     * Fields, Methods, Interfaces. The class will iterate them and will
     * verify if the class fulfilled the "element" in turn.
     * @param clazz - Things will be obtained from here.
     * @return - An array of elements or things to validate.
     * @see #assertDefinition(Class,  java.util.Properties, String)
     */
    public abstract Object[] getObjectsToValidate(Class clazz);

    /**
     * Subclasses should return the name of the thing to be validated.
     * @param o - The thing to be validated
     * @return - The name of that thing
     */
    abstract String getName(Object o);

    /**
     * Return the type of the thing to be validated, some subclasses might not
     * need to return anything. If they return null , they also have to
     * override the  assertSpec method
     * @param o - the thing whose type we need to know.
     * @return a String with the name of the type. For instance java.lang.String
     */
    abstract String getType(Object o);

    /**
     * Subclasses should return the thing's access modifier.
     * @param o - The things whose access modifier we need to know.
     * @return An integer with the mask of the access modifier as
     * defined in:Êjava.lang.reflect.Modifier
     * @see java.lang.reflect.Modifier
     */
    abstract int getModifiers(Object o);

    /**
     * Compares if the object passed in matches the description defined in
     * elementDescription. For instance, if the "thing" passed in were a
     * java.lang.reflect.Field the "elementDescription" may be something like:
     * "public name : java.lang.String" in which case the method will validate
     * if the name, the type and the access modifier are:
     *  "name", "java.lang.String" and "public" respectively.<br/>
     *
     * Subclasses may override this method to provide different assertion . 
     * @param o - The thing to validate/assert
     * @param elementDescription - A String representation to which
     * the validation will be performed against
     * @return  true if the passed object has the same name, type and access modifier
     * as described in the "elementDescription"
     */
    boolean assertSpec(Object o, String elementDescription) {

        String[] nameType = getNameTypePairs(elementDescription);

        String name        = getNameFromSpec(nameType[0].trim());
        String type        = nameType[1].trim();
        String modifier    = getModifierFromSpec( nameType[0].trim() );

        return name.equals(getName(o))
                    && type.equals(getType(o))
                    && sameModifier(modifier, getModifiers(o));

    }

    private String[] getNameTypePairs(String elementDescription) {
        return elementDescription.contains(":") ?
                elementDescription.split(":") :
                new String[]{
                        elementDescription,
                        elementDescription
                };
    }


    /**
     * Test if the given field has the same access modifier as specified in the
     * modifier string.<br/>
     *
     * The modifierDescription may specify if it is a class or instance artifact,
     * in that case it will be after the access modifier separated by a "-"<br/>
     *
     * For instance: public-static or public-instance<br/>
     *
     * @param modifierDescription - could be any of: "public protected default private"
     * @param modifiers        - a field which modifier is to be evaluated.
     * @return if the value of "modifier" string is public and the field
     *         is indeed public return true. Same goes for protected, default and private.
     *         Otherwise return false.
     */
    private boolean sameModifier(String modifierDescription, int modifiers) {
        String actualModifier;
        boolean instanceOrClassMatched = true;
        if (modifierDescription.contains("-")) {
            instanceOrClassMatched = false;
            actualModifier = modifierDescription.split("-")[0];
            String instanceOrClass = modifierDescription.split("-")[1];
            switch (instanceOrClass.hashCode()) {
                case STATIC:
                    instanceOrClassMatched = Modifier.isStatic(modifiers);
                    break;
                case INSTANCE:
                    instanceOrClassMatched = !Modifier.isStatic(modifiers);
            }
         } else {
            actualModifier = modifierDescription;
        }

        if (!instanceOrClassMatched) {
            return false;
        }
        switch (actualModifier.hashCode()) {

            case PUBLIC:
                return Modifier.isPublic(modifiers);
            case PROTECTED:
                return Modifier.isProtected(modifiers);
            case DEFAULT:
                return !Modifier.isPublic(modifiers)
                        && !Modifier.isProtected(modifiers)
                        && !Modifier.isPrivate(modifiers);
            case PRIVATE:
                return Modifier.isPrivate(modifiers);
            default:
                return false;
        }
    }



    /**
     * Return the acccess modifier specified in the spec.
     * @param spec - The spec containing the access modifier along with the name
     * @return a string representing the access modifier
     */
    private String getModifierFromSpec(String spec) {
        return spec.split(" ")[0];
    }

    /**
     * Return the name of the thing specified in the spec.
     * @param spec - The spec containing the name  along with the access modifier
     * @return a string representing name of the thing.
     */
    private String getNameFromSpec(String spec) {
        return spec.split(" ")[1];
    }
    /**
     * Test if the given object is null.
     * @param object to test
     * @return object == null
     */
    final boolean isNull(Object object) {
        return object == null;
    }


    /**
     * Test if the string is empty.
     * @param string the string to validate
     * @return string.trim().length() == 0
     */
    final boolean isEmpty(String string) {
        return string.trim().length() == 0;
    }

}

/**
 * Validates against "attributes" entry in the spec.
 */
class AttributesAssertStrategy extends AssertStrategy {
    @Override
    protected String propertyToValidate() {
        return "attributes";
    }

    @Override
    public Object[] getObjectsToValidate(Class clazz) {
        return clazz.getDeclaredFields();
    }


    @Override
    String getName(Object o) {
        return ((Field) o).getName();
    }

    @Override
    String getType(Object o) {
        return ((Field) o).getType().getName();
    }

    @Override
    int getModifiers(Object o) {
        return ((Field) o).getModifiers();
    }
}

/**
 *  Validates against "methods" entry in the spec.
 */
class MethodsAssertStrategy extends AssertStrategy {
    @Override
    protected String propertyToValidate() {
        return "methods";
    }

    @Override
    public Object[] getObjectsToValidate(Class clazz) {
        return clazz.getDeclaredMethods();
    }

    @Override
    String getName(Object o) {
        return ((Method) o).getName();
    }

    @Override
    String getType(Object o) {
        return ((Method) o).getReturnType().getName();
    }

    @Override
    int getModifiers(Object o) {
        return ((Method) o).getModifiers();
    }
}

/**
 *  Validates against implemented classed  entry in the spec.
 */
class ImplementsAssertStrategy extends AssertStrategy {

    @Override
    public boolean assertSpec(Object o, String elementDescription) {
        return ((Class)o).getName().equals(elementDescription);

    }

    @Override
    protected String propertyToValidate() {
        return "implements";
    }

    @Override
    public Object[] getObjectsToValidate(Class clazz) {
        return clazz.getInterfaces();
    }

    @Override
    String getName(Object o) {
        return ((Class)o).getName();
    }

    @Override
    String getType(Object o) {
        return ((Class)o).getSuperclass().getName();
    }

    @Override
    int getModifiers(Object o) {
        return ((Class)o).getModifiers();
    }
}

/**
 * Validates against "extends" entry in the spec.
 */
class ExtendsAssertionStrategy extends AssertStrategy {
    @Override
    public boolean  assertSpec(Object o, String elementDescription) {
        return ((Class)o).getName().equals(elementDescription);

    }

    @Override
    protected String propertyToValidate() {
        return "extends";
    }

    @Override
    public Object[] getObjectsToValidate(Class clazz) {
        return new Object[]{clazz.getSuperclass()};
    }

    @Override
    String getName(Object o) {
        return ((Class)o).getName();
    }

    @Override
    String getType(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    int getModifiers(Object o) {
        throw new UnsupportedOperationException();
    }
}

