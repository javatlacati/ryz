All the files in this directory ( except those in the output dir )
are considered as source files to be tested.


They are automatically loaded and tested by the testing framework, if their name
ends with "Spec.ryz" , for instance FirstSpec.ryz will be loaded.


The test specification describes what the compiled class should have.

className : fully qualified class name
attributes : comma separated attributes, the type is separated by :
methods   : comma separated attributes, the type is separated by :
classFile : name of the generated .class file to be deleted after the test


Example:

className: load.test.First
attributes: i:int
methods: i:int
classFile: load/test/First.class



