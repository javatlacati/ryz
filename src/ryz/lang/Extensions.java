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

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: 5/4/11
 * Time: 12:11 PM
 */
public final class Extensions {
	
    public static Boolean $eq$eq( String a, String b ) {
        return a.equals( b );
    }

    public static void isNull$qm( Object value, Block b ) {
        Bool.valueOf( value == null ).ifTrue( b );
    }
    public static Bool ifTrue( Boolean condition , Block b ) {
        return Bool.valueOf(condition).ifTrue(b);
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

    public static boolean $gt( int a, int b ) {
        return a > b;
    }

    public static boolean $eq$eq( int a, int b ) {
        return a == b;
    }

    public static boolean $em$eq( int a, int b ) {
        return a != b;
    }

    /*
    public static boolean $amp$amp( int a, int b ) {
        return a && b;
    }

    public static int $bar$bar( int a, int b ) {
        return a || b;
    }

    public static int $qm( int a, int b ) {
        return a ? b;
    }

    public static int $colon( int a, int b ) {
        return a : b;
    }
    */

}
