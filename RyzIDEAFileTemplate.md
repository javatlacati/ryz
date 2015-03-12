# Details #
This file template may be used to add test cases
that will be taken by the compiler test engine.

It may be added to Intellj IDEA through:

`Settings/FileTemplates/Add(+ sign)/`

![http://i.stack.imgur.com/4brkm.png](http://i.stack.imgur.com/4brkm.png)

```
/*
className: ${package}.${className}
classFile: ${package}/${className}.class
otherClasses:
extends: java.lang.Object
implements:
attributes:
methods:  public test${className}: void
behavior :
*/
${package}.${className} {
    test${className}() {
        // add Ryz code here...
    }
}
```
And then a new file test may be created with Right click / new / Ryz

![http://i.stack.imgur.com/H7T9w.png](http://i.stack.imgur.com/H7T9w.png)


/*
className: ${package}.${className}
classFile: ${package}/${className}.class
otherClasses:
extends: java.lang.Object
implements:
attributes:
methods:  public test${className}: void
behavior :
*/
${package}.${className} {
    test${className}() {
        // add Ryz code here...
    }
}
}}}

Description of format of the test spec may be found in:


http://code.google.com/p/ryz/source/browse/test-samples/spec-definition-readme.txt```