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

/**
 * This class is "currently" used for @InlineBlockTransformer to get type information
 * of the parameters passed to the block.
 */
class ParameterInfo  {
    private final String type;
    private final String name;
    private final static Map<String,String> wrappersMap = new HashMap<String,String>(){{
        put("int", "Integer");
        put("boolean", "Boolean");
    }};

    private ParameterInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public static List<ParameterInfo> parse(String parametersString) {
        //pattern is: /*attribute*/  int i ,    /*attribute*/  String s
        if( parametersString == null || parametersString.trim().equals("")){
            return new ArrayList<ParameterInfo>();
        }
        String[] ptrs = parametersString.split(",");
        List<ParameterInfo> parameterInfo = new ArrayList<ParameterInfo>();
        for(String p : ptrs ) {
            String [] nt  = p.split("\\s+");

            String type;
            String name;
            // Identify "varArgs"
            if( "...".equals( nt[3] ) && nt.length == 5) {
                type = nt[2] + "[]";
                name = nt[4];
            } else {
                type = nt[2];
                name = nt[3];
            }
            parameterInfo.add( new ParameterInfo( name, type ));
        }
        return parameterInfo;
    }

    public static String getTypes(List<ParameterInfo> parameterInfo ) {
        if( parameterInfo.isEmpty() ) {
            return "";
        }
        StringBuilder builder = new StringBuilder(",");
        for( ParameterInfo info : parameterInfo ) {
            builder.append( info.getType() );
            builder.append( ",");
        }
        if( builder.length() > 0 ) {
            builder.deleteCharAt( builder.length()-1);
        }
        return builder.toString();
    }
    private String getType() {
        if( wrappersMap.containsKey( this.type ) ) {
            return wrappersMap.get( this.type );
        }
        return this.type;
    }
}
