// About -----------------------------------
ryz.programming.Language {
  staticallyType = True
  paradigm = "Object Oriented"
  runsOn( jvm = True) {
      compilesTo = "Java bytecode"
  }
}
//------------------------------------------


Currently uses Intellj IDEA 10 CE ( http://www.jetbrains.com/idea/download/index.html )
to compile and test using TestNG testing framework plugin included with the IDE.

After loading the project, execute the: Run configuration "ryz.compiler" option.

Java source files for the RyzC are in folder:
/src

Java source files for the unit test are in folder:
/test

Ryz test source files are in folder:
/test-resources

If test source file name ends in "Spec.ryz" it will be loaded by the
testing framework, compiled and "asserted" against the specification
defined in the file header.

(See /test-resources/spec-definition-readme.txt for a definition of the test spec)

Further documentation will be placed on:

http://code.google.com/p/ryz/w/list

The issue list is:
http://code.google.com/p/ryz/issues/list

The mailing list:
http://groups.google.com/group/ryz-lang

Twitter: @ryz_language


3rd party libraries:

[Testgn]

This project uses TesNG <http://testng.org>  testing framework redistributed under Apache 2.0 license. You may find a copy of the license in lib/APACHE-LICENSE.txt


[javassist]

This project uses Javassit <http://www.jboss.org/javassist> bytecode manipulation library redistributed under Mozilla Public License v1.1 You may find a copyt fo the license in lib/MZL-License.html

