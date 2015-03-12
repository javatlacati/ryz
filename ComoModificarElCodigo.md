# Introduccion #

Como compilar el código y como agregar nuevos casos de prueba.

# Detalles #


  * Compilar con ant primero ( si usas linea de comando "ant"  si usas idea margen derecho boton ant build "test" )

![http://i.stack.imgur.com/V20wS.png](http://i.stack.imgur.com/V20wS.png)

  * Cualquier cosa que hagas si quieres ver si rompiste algo puedes ver el estatus final del ant, te deben de salir 0 errores.

> ![http://i.stack.imgur.com/63xVu.png](http://i.stack.imgur.com/63xVu.png)
> ![http://i.stack.imgur.com/OKn2g.png](http://i.stack.imgur.com/OKn2g.png)

La forma en la que funciona el "compilador" es la siguiente:

  * Empezando en RyzC  toma un archivo
  * Lee todas sus lineas en un arreglo de String ( una lista de Strings )
  * Lo recorre y le pasa cada linea a los "transformadores"
  * Si a algun transformador le gusta una linea entonces la convierte en código Java y agrega el código generado en un arreglo "salida"


![http://i.stack.imgur.com/tREqm.png](http://i.stack.imgur.com/tREqm.png)
![http://i.stack.imgur.com/Q6vtM.png](http://i.stack.imgur.com/Q6vtM.png)

  * Dependiendo del estado de compilacion se agregan o quitan transformadores, así por ejemplo, cuando empieza no hay transformadores de "metodos" porque a un nivel inicial no hay métodos.


Para buscar si un patrón coincide con el transformador uso expresiones regulares.

Por ejemplo para detectar si una linea es una declaración hago algo como esto ( en términos generales ):

```
   line.matches("\w+ : \w+");
```

O sea si la linea tiene una palabra, separado por `:` y luego otra palabra como en `nombre : String`

Es importantísimo, antes de hacer algun cambio agregar un archivo `XyzSpec.ryz`  vacio y ver que corra.
Luego agregar el código que quieres probar
Finalmente modificar el código hasta que funcione.

Ejemplo:

Agregar en:
```
test-samples/03-attributes
```

El archivo:` MultipleDeclarationSpec.ryz`

```
/*
className: attributes.MultipleDeclarationSpec
classFile: atributes/MultipleDeclarationSpec.class
otherClasses:
extends: java.lang.Object
implements:
attributes:
methods:  public testMultipleDeclarationSpec: void
behavior :
*/
attributes.MultipleDeclarationSpec {
    testMultipleDeclarationSpec() {
    }
}
```
Luego modificarlo para ver si sigue corriendo:


```
/*
className: attributes.MultipleDeclarationSpec
classFile: atributes/MultipleDeclarationSpec.class
otherClasses:
extends: java.lang.Object
implements:
attributes: private-instance r : java.lang.String,\
            private-instance s : java.lang.String,\
            private-instance t : java.lang.String 
methods:  public testMultipleDeclarationSpec: void
behavior :
*/
attributes.MultipleDeclarationSpec {
    r,s,t : String 
    testMultipleDeclarationSpec() {
    }
}
```


Volver a compilar y modificar hasta que el código funcione y no se rompa ningún otro test.


El formato del archivo de pruebas esta en:

http://code.google.com/p/ryz/source/browse/test-samples/spec-definition-readme.txt

Y cada archivo dentro de `test-samples` es en si mismo un ejemplo de como usarlo.

Ver tambien:

http://code.google.com/p/ryz/wiki/RyzIDEAFileTemplate