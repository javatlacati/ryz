All the files in this directory ending with "Spec.ryz" are loaded by the testing
framework.

The header of the file should contain a "test specification" with the following format:


className: Fully qualified class name expected after compilation
classFile: Name of the .class file generated after the compilation
extends: Fully qualifies class name this object inherits from.
implements: Comma separated of fully qualified interfaces implemented by this class
attributes: Comma separated attributes of this class.
methods: Comma separated methods of this class
behavior: Comma separated instructions the generated class must execute.

className and classFile  are mandatory

The method and attribute format is:

[static|instance]-accessModifier name : type

As example:

/*
className: methods.class$.ClassMethod
classFile: methods/class$/ClassMethod.class
extends: java.lang.Thread
implements: java.lang.Runnable
attributes:  private name:java.lang.String, private-static t : java.lang.String
methods: public name: java.lang.String , public main: void, public-static main:void, public-static count: java.lang.Integer
behavior : new,  invokestatic main([Ljava.lang.String;) | stdout=Hello, world\n
*/





