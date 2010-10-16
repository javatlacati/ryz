package ryz.compiler;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: Oct 15, 2010
 * Time: 8:55:48 AM
 * To change this template use File | Settings | File Templates.
 */
abstract class LineTransformer {

    protected final String ls = System.getProperty("line.separator");

    private static final List<String> javaKeywords = Arrays.asList(
            "abstract","assert","boolean","break","byte","case","catch",
            "char","class","const","continue","default","do","double",
            "else","enum","extends","final ","finally","float","for",
            "goto","if","implements","import","instanceof","int",
            "interface","long","native ","new","package","private ",
            "protected ","public return","short","static ","strictfp ",
            "super","switch","synchronized ","this","throw","transient ",
            "try","void","volatile ","while"
    );

    protected String scapeName(String name) {
        if( javaKeywords.contains(name)){
            return name + "$";
        }

        return name;
    }
    
    public abstract void transform(String line, List<String> generatedSource);
}
class ImportTransformer extends LineTransformer {
    private final Pattern importPattern = Pattern.compile("import\\s*\\((.+)\\s*\\)");



    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("import(")){
            System.out.println("line = " + line);
            Matcher m = importPattern.matcher(line);
            if( m.matches() ) {
                generatedSource.add( String.format("import %s;%n", m.group(1)));
            }
        }
    }
}
class PackageTransformer extends LineTransformer {
    @Override
    public void transform(String line, List<String> generatedSource) {

        int iod = line.indexOf(".");
        int iok = line.indexOf("{", iod);
        if (iod > 0 && iok > iod) {
            // ej: some.package.Name {
            String possiblePackageAndClass = line.substring(0, iok);
            int liod = possiblePackageAndClass.lastIndexOf(".");
            String possibleClass = possiblePackageAndClass.substring(liod+1).trim();

            if (Character.isUpperCase(possibleClass.charAt(0))) {
                String packageName = possiblePackageAndClass.substring(0, liod);
                StringBuilder sb = new StringBuilder("");
                for( String s : packageName.split("\\.")){
                    sb.append(scapeName(s));
                    sb.append(".");
                }
                sb.delete(sb.length()-1, sb.length());
                generatedSource.add(String.format("package %s;%n", sb.toString()));
                generatedSource.add(String.format("public class %s {%n", scapeName(possibleClass)));
            }
        }
    }

}
class AttributeTransformer extends LineTransformer {

    private final Pattern attributePattern = Pattern.compile("(\\w+)\\s*:\\s*(\\w+)");
    private final Pattern attributeInitializedPattern = Pattern.compile("(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)");

    @Override
    public void transform(String line, List<String> generatedSource) {
                Matcher matcher = attributePattern.matcher(line);

                if( matcher.matches()){
                    generatedSource.add( String.format("    /*attribute*/private %s %s;%n", scapeName(matcher.group(2)),scapeName(matcher.group(1))));
                } else if( (matcher = attributeInitializedPattern.matcher(line)).matches() ){
                    generatedSource.add( String.format("    /*attribute*/private %s %s = %s;%n", scapeName(matcher.group(2)),scapeName(matcher.group(1)), scapeName(matcher.group(3))));
                }
    }
}
// TODO: multiline comments has problems
class CommentTransformer extends LineTransformer {

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("/*")
          || line.startsWith("//")
          || line.endsWith("*/") ){
            generatedSource.add(line + ls );

        }
    }
}
class ClosingKeyTransformer extends LineTransformer {

    @Override
    public void transform(String line, List<String> generatedSource) {
        if( line.startsWith("}")) {
            generatedSource.add( line + ls );
        }
    }
}
class MethodTransformer extends LineTransformer {

    private final Pattern methodPattern = Pattern.compile("(\\w+)\\(\\)\\s*:\\s*(\\w+)\\s*\\{");
    
    @Override
    public void transform(String line, List<String> generatedSource) {
        Matcher matcher = methodPattern.matcher(line);
        //TODO: handle default return
        if( matcher.matches() ) {
            generatedSource.add( String.format("    /*method*/public %s %s() {%n    return \"\";%n", scapeName(matcher.group(2)),scapeName(matcher.group(1))));
        }


    }
}
