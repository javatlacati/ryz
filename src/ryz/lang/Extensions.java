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

package ryz.lang;


import ryz.lang.block.Block0;
import ryz.lang.block.Block1;
import ryz.lang.block.Block2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: 5/4/11
 * Time: 12:11 PM
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class Extensions {


    public static void notNull$qm( Object value, Block0<Void> b ) {
        Bool.valueOf( value != null ).ifTrue(b);
    }
    public static void isNull$qm( Object value, Block0<Void> b ) {
        Bool.valueOf( value == null ).ifTrue(b);
    }
    public static Bool isTrue$qm( Boolean condition , Block0<Void> b ) {
        return ifTrue( condition, b );
    }
    public static Bool ifTrue( Boolean condition , Block0<Void> b ) {
        return Bool.valueOf(condition).ifTrue(b);
    }

    public static Bool isFalse$qm( Boolean condition , Block0<Void> b ) {
        return ifFalse( condition, b );
    }

    private static Bool ifFalse(Boolean condition, Block0<Void> b) {
        return Bool.valueOf(condition).ifFalse(b);
    }

    public static Boolean not( Boolean value ) {
        //return Bool.valueOf(!value);
        return !value;
    }
    public static void whileTrue(Block0<Boolean> condition, Block0<Void> b  ) {
        while( condition.run() ) {
            b.run();
        }
    }
    public static <T> void each( Collection<T> collection, Block1<Void,T> b ) {
        for( T e : collection ) {
            b.run( e );
        }
    }

    public static <T> void each( T[] array, Block1<Void,T> b ){
        each( Arrays.asList(array), b);
    }
    public static void print( String s ) { 
        System.out.print( s );
    }
    public static void println( String s ) {
        System.out.println( s );
    }


    public static <T extends Comparable<T>> void sort$em( T[] array,
                                 final Block2<Integer,T,T> blockComparator ) {
        Comparator<T> c = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return blockComparator.run(o1,o2);  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        Arrays.sort(array, c);
    }
    public static <T> int $lt$eq$gt( Comparable<T> a, T b ) {
        return a.compareTo(b);
    }

    public static int $plus( int a, int b ) {
        return a + b;
    }

    public static int $minus( int a, int b ) {
        return a - b;
    }

    public static int $star( int a, int b ) {
        return a * b;
    }

    public static int $slash( int a, int b ) {
        return a / b;
    }

    public static int $percent( int a, int b ) {
        return a % b;
    }

    public static boolean $lt( int a, int b ) {
        return a < b;
    }

    public static boolean $lt$eq( int a, int b ) {
        return a <= b;
    }


    public static boolean $gt( int a, int b ) {
        return a > b;
    }

    public static boolean $gt$eq( int a, int b ) {
        return a >= b;
    }

    public static boolean $eq$eq( Object a, Object b ) {
        return a .equals( b );
    }

    public static boolean $em$eq( int a, int b ) {
        return a != b;
    }

    public static boolean $amp$amp( boolean a, boolean b ) {
        return a && b;
    }

    public static <T1,T2> void max(T1 t1, T2 t2, Block2<Void,T1,T2> b) {
        b.run(t1, t2);
    }

    public static boolean $lt( String a, String b ) {
        return a.compareTo(b) < 0;
    }

    public static boolean $gt$eq( String a, String b ) {
        return a.compareTo(b) >= 0;
    }

    public static String $percent( String format, Object ... values ) {
        return String.format( format , values );
    }
    
    public static boolean $bar$bar( boolean a, boolean b ) {
        return a || b;
    }
    /*

    public static int $qm( int a, int b ) {
        return a ? b;
    }

    public static int $colon( int a, int b ) {
        return a : b;
    }
    */

    //These "call" methods will be removed when
    // block "signature" is supported, they are not
    // really a language extension, just for testing purposes
    public static <T> void call( Block1<Void,String> b ) {
        b.run("I'm getting closer");
    }
    public static void call( Block2<Void,String,String> b ) {
        b.run("I'm getting closer", "And closer");
    }
    public static void callVarArgs( Block1<Void,String[]> b) {
         b.run(new String[]{"1","2","3"});
    }
}
