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


import java.io.IOException;
import java.io.File;
import java.util.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.CannotCompileException;

class ClassInstrumentation { 
    public static void removeCheckedExceptions( RyzClass currentClass, File output ) 
        throws java.io.IOException { 

        try {                    
            Logger logger = Logger.getLogger( ClassInstrumentation.class.getName() );

            File generetedClassFile = new File( output, currentClass.packageName().replaceAll("\\.","/") + "/" +currentClass.className() + ".class" );

            logger.finest("generetedClassFile.getAbsolutePath() "+ generetedClassFile.getAbsolutePath() );
            logger.finest(" generetedClassFile.exists() " +  generetedClassFile.exists() );

            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath( output.getAbsolutePath() );

            CtClass cc = pool.get(currentClass.packageName()+"."+currentClass.className());

            logger.finest( "CtClass: " + cc );
            //TODO: possible bug, this also removed from my Constructors
            for( CtMethod method : cc.getDeclaredMethods() ) { 
                logger.finest("method: "+ method );
                cc.defrost();
                logger.finest("method info : " +  method.getMethodInfo() );
                //TODO: test when a class has a block.
                if( method.getMethodInfo().getExceptionsAttribute() != null ) {
                    logger.finest("exceptions  : " +  java.util.Arrays.toString( method.getMethodInfo().getExceptionsAttribute().getExceptions() ) );
                    method.getMethodInfo().removeExceptionsAttribute();
                }
                logger.finest("method info : " +  method.getMethodInfo() );
            }
            logger.finest(" generetedClassFile.delete()" +  generetedClassFile.delete() );
            cc.writeFile(output.getAbsolutePath());
            //cc.writeFile(output.getParent());
        } catch( CannotCompileException cce ) { 
            throw new IOException( cce );
        } catch( NotFoundException nfe ) { 
            throw new IOException( nfe );
        }
    }
}




