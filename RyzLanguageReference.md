



# Introduction #

The following is a brief of the features and characteristics of the Ryz programming language.

While the language is still a work in progress, this reference is aimed to understand what the language is all about.

This is a sample Ryz class definition

```
/*
 This a sample Ryz class
*/
//package.ClassName
say.Hello {
  // attribute
  name : String
  // method
  sayHello( to: String = "world!" ) {
      // a block
      b = () {
          out.println("Hello " .+ to )
      }
      // invoke it
      b() // prints "Hello world!"
  }
}
```


# Details #

The Ryz programming language is a statically typed, object oriented programming language with basic type inference and few reserved words.

It compiles to bytecode which runs directly on the JVM and is compatible with Java.

Source code may be written in unicode.

_Grammar_
```
char       : lowerChar | upperChar;
lowerChar  : 'a'..'z'  | unicodeLowerCase; // should we include $ and _ ?
upperChar  : 'A'..'Z'  | unicodeUpperCase;
anyChar   : // an arbitrary Unicode code point
          ;
anyLetter : // a Unicode code point classified as "Letter" 
          ;
anyDigit  : // a Unicode code point classified as "Digit" 
          ;
//TODO: add letter and digit
```



## Comments ##

Ryz uses Java style comments

Single line:
```
// single line comment
```

And multiline
```
/*
multi
line
comment
*/
```

Comments do no nest

## Access modifiers ##

Access modifiers, follow those defined in UML, namely:

<table>
<tr><td>+</td><td>public</td></tr>
<tr><td>#</td><td>protected</td></tr>
<tr><td>~</td><td>package</td></tr>
<tr><td>-</td><td>private</td></tr>
</table>

_Grammar_
```
accessModifier :  '+'|'#'|'~'|'-'; 
```

When no access modifier is specified, classes and methods are defined as public, attributes and block are defined as private.

Class level artifacts are defined with duoble underscore:

_Grammar_
```
classLevel : '__' ;
```

Non final artifacts are designated with an exclamation mark:

_Grammar_
```
nonFinal : '!' ; //nonFinal looks like Java's not operator. Consider different.
```



## Classes ##


All classes have a preceding package, start with uppercase and an opening  brace:

```
demo.Main {
}
```

The package name starts with lowercase  and the class name starts with uppercase.

_Grammar_
```
class      : [accessModifier] package '.' className '{';
package    : packageName ( '.' packageName )*;
packageName: lowerChar char*;
className  : upperChar char*;

```
## Attributes ##

Attributes start with lowercase followed by colon and attribute type:

```
//private attribute "name" of type String
name : String

// same
- name : String

// public attribute "age" of type Integer
+ age  : Integer

```

Optionally the attribute may have an initial value:

```
name : String = "Ryz"
```

And the type may be inferred from the initial value, so, the following would be equivalent:

```
name = "Ryz"
```

All the attributes are private by default.

_Grammar_
```
attribute     : [accessModifier] attributeName ':' type optionalInit
              | attributeName initialValue
              ;
attributeName : lowerChar anyChar*;
type          : upperChar anyChar*;
optionalInit  : initialValue
              | // or nothing
              ;

initialValue  : '=' expr;
expr          : literal | // TBD
              ;

```
## Methods ##

Methods, like attribues, start with lowercase, but have a parameter list ( which may be empty ) and optional return type and also an opening brace in the same line:

```
//public method "startEngine" returning Boolean
startEngine() : Boolean {
}

// public method "sayHello" with no return type
sayHello( to : String ) {
}

//private method "secret" with no return type
- secret() {
}


```

Method parameters can have default value, and the type of the parameter may also be inferred:

```
saySomething( something : String = "meh" ) { 
}
sayHello( to = "Unknown" ) {
}
```

Var args may be used in the last parameter by using a `*` after the type
```
giveMessage( message: String, to : String* ) { 
   out.print("Dear:")
   to.each((name:String){
       out.print( name )      
   })
   out.print("We have a message for you:")
   out.print( message )
}
```
If a return type is specified the last line of the method body is returned as value:

```
// Returns 10
size() : Int {
   10
}
```

_Grammar_
```
method        : [accessModifier] methodName '(' [parameterList] ')' [':' type] '{';
methodName    : lowerChar | anyChar*;
           
parameterList : [parameter ( , parameter )*] [lastParameter] 
              ;
lastParameter : parameterName ':' type['*']
              ;
parameter     : parameterName ':' type [optionalInit]
              | parameterName initialValue
              ;
parameterName : lowerChar anyChar*;
type          : upperChar anyChar*;
optionalInit  : initialValue
              | // or nothing
              ;
initialValue  : '=' expr;
expr          : literal | // TBD

```
## Blocks ( a.k.a closures ) ##

Blocks of code are anonymous methods which can be assigned to attributes or variables.

Since they are anonymous they don't have a name. Otherwise they are just like methods including the parameter list and the return type. Blocks can be assigned to an attribute, variables and  passed as argument.

```
// declare an attribute of type block:
aBlock : () 

// assign a value 

aBlock = (){
   out.println("Hello from block")
}

// invoke it
aBlock()
```

Since they can be assigned to attribute, the following is also valid:

```
aBlock = () {
    out.println("Hello from block")
}
```

These blocks are central part of the language, for instance, control structures rely on them:

```
result = a.greaterThan( b )
result.isTrue?( ()  {
    out.println("A is greater than B")
})
```

Then there are no parameters in the block, the parenthesis may be omitted in the declaration which result in smaller code.

So these two declarations are equivalent:

```
() { 
  out.println("Hello")
}

{
  out.println("Hello")
}
```

So, the previous example could be written as:

```
result = a.greaterThan( b )
result.isTrue?({
    out.println("A is greater than B")
   }
)
```

_Grammar_
```
block : '(' [parameterList] ')' [':' type] '{'
      | '{' blockBody '}'
      ;      
```

## Optional parenthesis for 1 parameter methods ##

Parameter are mandatory for all the methods invocations, except for those that only receive one parameter, allowing the following constructs.

```
out.println "Hello"
self.save data
data.append moreData
```

Since blocks can be also passed as arguments the rule also applies. The sum of the two rules would look like:

```
result = a.greaterThan( b )
//with parenthesis
result.isTrue?((){
  out.println("A is greater than B")
})

//with optional parenthesis
result.isTrue? {
  out.println "A is greater than B"
}
```

Since blocks could be anonymous and/or be assigned to variables the one parameter exception doesn't apply:

```
same = ( x : Int ) {
   x
}  
same 3 // wrong, parenthesis expected.

((x:Int){ x } ) 3 // wrong, parenthesis expected.

same( 3 ) // right! 
((x:Int){x})(3)//right!
```



## Operators ( lack of ) ##

Ryz doesn't have operators, so object methods should be used instead.


```
a = 0
b = 1
c = a .+( b ) 
```

That is, invoking the "plus" ( + ) method on the `Int` class.

Notice the access modifier may not be confused with the method name, because the method names are **always**  followed by an opening parenthesis:

```
some.Example {
   // defines the public "++" method:
   + ++() : Example {
       // do something 
       // returns self
       self
   }
}
```

In this example, the "+" is the access modifier, and "++" is the method name

This method is invoked as any other:

```
   example = Example()
   example.++()
```


## Extension methods ##

Ryz allows to define new methods for existing classes.  These methods have the restriction of being class level methods and they are basically syntactic sugar for utility methods.

Let's say we need to create a "reverse" method, we could define it as:

```

some.StringUtil {
    /** "reverse()" extension methods. */
    __ java.lang.String.reverse( aString: String ) : String {
        StringBuilder( aString ).reverse().toString()
     }
```

And then invoke it like this:

```
importExtension( some.StringUtil.reverse )
main.Demo {
      main() {
      aString = "Reverse me"
      out.println( aString.reverse() )
   }
}
```

It would print "em esreveR"

_Grammar_

```
extension_method        : [accessModifier] classLevel fullyQualifiedClassName '.' methodName '(' [parameterList] ')' [':' type] '{' ;
fullyQualifiedClassName :  package '.' className
```

To be continued...

Comments are welcome:  [@ryz\_language](http://twitter.com/#!/ryz_language)  | http://groups.google.com/group/ryz-lang