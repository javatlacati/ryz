# Introducción #

Esta página muestra como compilar un archivo .ryz y ejecutar la clase generada.


# Detalles #

> Como pre-requisito se debe de obtener el código fuente del compilador por supuesto.
> Asumiendo que esa parte está lista, hay que compilar usando ant, esto se puede hacer desde un IDE o desde la línea de comandos. Los pasos son los siguientes.

  * Primero hay que ejecutar la tarea `dist` de ant que genera un archivo .jar con la fecha de compilación:

Ejemplo:
```
$ ant dist 
...
  [testng] ===============================================
   [testng] Ant suite
   [testng] Total tests run: 64, Failures: 0, Skips: 0
   [testng] ===============================================
   [testng] 

dist:
      [jar] Building jar: /Users/oscarryz/code/ryz/dist/ryzc-20110922.jar

BUILD SUCCESSFUL
Total time: 6 seconds
```
  * Esto crea el _compilador_ de Ryz. Luego hay que copiar ese archivo a algún directorio donde estemos trabajando:

```
    cp /Users/oscarryz/code/ryz/dist/ryzc-20110922.jar  /Users/oscarryz/samples
```


  * Después hay que crear un programa en Ryz y  ejecutar el compilador:

```
$cat > HelloAll.ryz <<.
> demo.HelloAll {
>    __ main( args : String* ) {
>       args.each((name:String){
>          name.println()
>       })
>    }
> }
> .
$java -jar ryzc-20110922.jar HelloAll.ryz 
$
```

  * Y listo, finalmente solo hay que ejecutar la clase generada agregando al classpath la biblioteca de ryz ( que sucede que es la misma que la que contiene el compilador, pero esto cambiará )

```
$java -cp ryzc-20110922.jar:. demo.HelloAll  Hugo Paco Luis
Hugo
Paco
Luis
```


Actualmente el compilador es extremadamente simple y muchas construcciones aún no están disponibles.


Los ejemplos de lo que ya compila hoy en día están en el directorio: [test-samples](http://code.google.com/p/ryz/source/browse/#hg%2Ftest-samples)