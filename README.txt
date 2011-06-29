# About Ryz 

## Introduction 

    ryz.programming.Language {
      staticallyType = true
      paradigm = "Object Oriented"
      runsOn( jvm = True) {
          compilesTo = "Java bytecode"
      }
    }

## Details 


Currently Ryz uses Intellj IDEA 10 CE and TestNG 
testing framework plugin included with the IDE.

http://www.jetbrains.com/idea/download/index.html

After loading the project, execute the option: 

run > configuration > `ryz.compiler`
 

### Project structure

    /src
    /test
    /test-samples


`src`: Contains Java source files to compile Ryz source code.

`test`: Contains Java source file to test the RyzC 

`test-samples` : Contains samples of the language.


If test source file name ends in `Spec.ryz` it will be loaded by the
testing framework, compiled and "asserted" against the specification
defined in the file header.

(See [/test-samples/spec-definition-readme.txt](https://github.com/OscarRyz/Ryz/blob/master/test-samples/spec-definition-readme.txt) for a definition of the test spec)


### 3rd party libraries

TestNG

This project uses TesNG http://testng.org testing framework
redistributed under Apache 2.0 license.

You may find a copy of the license in [lib/APACHE-LICENSE.txt](https://github.com/OscarRyz/Ryz/blob/master/lib/APACHE-LICENSE.txt)


Javassist

This project uses Javassit http://www.jboss.org/javassist bytecode manipulation library
redistributed under Mozilla Public License v1.1 

You may find a copy fo the license in [lib/MZL-License.html](https://github.com/OscarRyz/Ryz/blob/master/lib/MZL-License.html)



### Links

  * Docs: http://code.google.com/p/ryz/w/list
  * Issues: http://code.google.com/p/ryz/issues/list
  * Mailing list: http://groups.google.com/group/ryz-lang
  * Twitter: [@RyzLang](https://twitter.com/#!/RyzLang "Ryz Lang on twitter")