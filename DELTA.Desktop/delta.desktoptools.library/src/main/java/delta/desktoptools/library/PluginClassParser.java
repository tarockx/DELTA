package delta.desktoptools.library;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import unipd.elia.delta.sharedlib.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;

/**
 * Created by Elia on 23/04/2015.
 */


public class PluginClassParser {
    private List<PluginConfiguration> pluginClasses = null;
    public PluginClassParser(){
        pluginClasses = new LinkedList<>();
    }

    public List<PluginConfiguration> Parse(List<File> files){
        pluginClasses.clear();
        for(File f: files){
            Parse(f);
        }
        return  pluginClasses;
    }

    private void Parse(File inputFile){
        try{
            FileInputStream in = new FileInputStream(inputFile);
            final CompilationUnit cu =  JavaParser.parse(in);

            VoidVisitorAdapter vva = new VoidVisitorAdapter() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                    List<ClassOrInterfaceType> implementedInterfaces = n.getImplements();
                    if(implementedInterfaces != null){
                        PluginConfiguration pc = null;
                        boolean isPolling = false, isEvent = false;


                        for (int i = 0; i < implementedInterfaces.size(); i++) {
                            ClassOrInterfaceType interfaceType = implementedInterfaces.get(i);

                            //It's a plugin
                            if(interfaceType.getName().equals("IDeltaPlugin")) {
                                if(pc == null)
                                    pc = new PluginConfiguration();

                                pc.PluginClassQualifiedName = cu.getPackage().getName() + "." + n.getName();

                            }

                            //Polling plugin
                            if(interfaceType.getName().equals("IDeltaPollingPlugin")) {
                                isPolling = true;
                            }
                            //Event plugin
                            if(interfaceType.getName().equals("IDeltaEventPlugin")) {
                                isEvent = true;
                            }
                        }

                        if(pc != null && (isPolling || isEvent)) {
                            List<AnnotationExpr> annotations = n.getAnnotations();
                            if(annotations != null) {
                                for (AnnotationExpr annotationExpr : annotations) {
                                    String annotationName = annotationExpr.getName().getName();
                                    if(annotationName.equals("DeltaPluginMetadata")){
                                        readMetadata(annotationExpr, pc);
                                    }

                                    if(annotationName.equals("DeltaOptions")){
                                        List<Node> annotationChildren = annotationExpr.getChildrenNodes();
                                        for(Node annotationChild : annotationChildren){
                                            if(MemberValuePair.class.isInstance(annotationChild)){
                                                MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                                                if(memberValuePair.getName().equals("StringOptions")) {
                                                    ArrayInitializerExpr stringOptionsArray = (ArrayInitializerExpr) memberValuePair.getValue();
                                                    readStringOptions(stringOptionsArray, pc);
                                                }
                                                else if(memberValuePair.getName().equals("BooleanOptions")) {
                                                    ArrayInitializerExpr booleanOptionsArray = (ArrayInitializerExpr) memberValuePair.getValue();
                                                    readBooleanOptions(booleanOptionsArray, pc);
                                                }
                                                else if(memberValuePair.getName().equals("IntegerOptions")) {
                                                    ArrayInitializerExpr integerOptionsArray = (ArrayInitializerExpr) memberValuePair.getValue();
                                                    readIntegerOptions(integerOptionsArray, pc);
                                                }
                                                else if(memberValuePair.getName().equals("DoubleOptions")) {
                                                    ArrayInitializerExpr doubleOptionsArray = (ArrayInitializerExpr) memberValuePair.getValue();
                                                    readDoubleOptions(doubleOptionsArray, pc);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if(pc.PluginAuthor != null && pc.PluginName != null && pc.PluginDescription != null) {
                                pc.supportsPolling = isPolling;
                                pc.supportsEvents = isEvent;
                                pluginClasses.add(pc);
                            }
                        }
                    }
                }


            };
            vva.visit(cu, null);
            in.close();

        }catch (Exception ex){
            return;
        }
    }


    private void readMetadata(AnnotationExpr metadataAnnotation, PluginConfiguration pc){
        List<Node> annotationChildren = metadataAnnotation.getChildrenNodes();
        for(Node annotationChild : annotationChildren){
            if(MemberValuePair.class.isInstance(annotationChild)){
                MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                if(memberValuePair.getName().equals("PluginName"))
                    pc.PluginName = extractString(memberValuePair.getValue());
                else if(memberValuePair.getName().equals("PluginAuthor"))
                    pc.PluginAuthor = extractString(memberValuePair.getValue());
                else if(memberValuePair.getName().equals("PluginDescription"))
                    pc.PluginDescription = extractString(memberValuePair.getValue());
                else if(memberValuePair.getName().equals("DeveloperDescription"))
                    pc.DeveloperDescription = extractString(memberValuePair.getValue());
                else if(memberValuePair.getName().equals("RequiresRoot"))
                    pc.RequiresRoot = ((BooleanLiteralExpr)(memberValuePair.getValue())).getValue();
                else if(memberValuePair.getName().equals("RequiresWakelock"))
                    pc.RequiresWakelock = ((BooleanLiteralExpr)(memberValuePair.getValue())).getValue();
                else if(memberValuePair.getName().equals("MinPollInterval"))
                    pc.MinPollingFrequency = extractInteger(memberValuePair.getValue());
            }
        }
    }


    private void readStringOptions(ArrayInitializerExpr stringOptionsArray, PluginConfiguration pc){
        for(Expression expression : stringOptionsArray.getValues()){
            if(AnnotationExpr.class.isInstance(expression)){
                AnnotationExpr annotationExpr = (AnnotationExpr) expression;
                if(annotationExpr.getName().getName().equals("DeltaStringOption")){
                    StringOption stringOption = new StringOption();

                    List<Node> annotationChildren = annotationExpr.getChildrenNodes();
                    for(Node annotationChild : annotationChildren) {
                        if (MemberValuePair.class.isInstance(annotationChild)) {
                            MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                            if(memberValuePair.getName().equals("ID"))
                                stringOption.ID = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Name"))
                                stringOption.Name = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Description"))
                                stringOption.Description = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Multiline"))
                                stringOption.Multiline = ((BooleanLiteralExpr)(memberValuePair.getValue())).getValue();
                            else if(memberValuePair.getName().equals("AvailableChoices")) {
                                ArrayInitializerExpr availableChoicesArray = ((ArrayInitializerExpr) (memberValuePair.getValue()));
                                List<Expression> availableChoicesArrayValues = availableChoicesArray.getValues();
                                String[] availableChoices = new String[availableChoicesArrayValues.size()];
                                for(int i = 0; i < availableChoicesArrayValues.size(); i++){
                                    availableChoices[i] = extractString(availableChoicesArrayValues.get(i));
                                }
                                stringOption.AvailableChoices = availableChoices;
                            }
                            else if(memberValuePair.getName().equals("defaultValue"))
                                stringOption.defaultValue = extractString(memberValuePair.getValue());
                        }
                    }

                    if(pc.Options == null)
                        pc.Options = new LinkedList<>();
                    pc.Options.add(stringOption);
                }
            }
        }
    }

    private void readBooleanOptions(ArrayInitializerExpr booleanOptionsArray, PluginConfiguration pc){
        for(Expression expression : booleanOptionsArray.getValues()){
            if(AnnotationExpr.class.isInstance(expression)){
                AnnotationExpr annotationExpr = (AnnotationExpr) expression;
                if(annotationExpr.getName().getName().equals("DeltaBooleanOption")){
                    BooleanOption booleanOption = new BooleanOption();

                    List<Node> annotationChildren = annotationExpr.getChildrenNodes();
                    for(Node annotationChild : annotationChildren) {
                        if (MemberValuePair.class.isInstance(annotationChild)) {
                            MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                            if(memberValuePair.getName().equals("ID"))
                                booleanOption.ID = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Name"))
                                booleanOption.Name = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Description"))
                                booleanOption.Description = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("defaultValue"))
                                booleanOption.defaultValue =  ((BooleanLiteralExpr)(memberValuePair.getValue())).getValue();
                        }
                    }

                    if(pc.Options == null)
                        pc.Options = new LinkedList<>();
                    pc.Options.add(booleanOption);
                }
            }
        }
    }

    private void readIntegerOptions(ArrayInitializerExpr integerOptionsArray, PluginConfiguration pc){
        for(Expression expression : integerOptionsArray.getValues()){
            if(AnnotationExpr.class.isInstance(expression)){
                AnnotationExpr annotationExpr = (AnnotationExpr) expression;
                if(annotationExpr.getName().getName().equals("DeltaIntegerOption")){
                    IntegerOption integerOption = new IntegerOption();

                    List<Node> annotationChildren = annotationExpr.getChildrenNodes();
                    for(Node annotationChild : annotationChildren) {
                        if (MemberValuePair.class.isInstance(annotationChild)) {
                            MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                            if(memberValuePair.getName().equals("ID"))
                                integerOption.ID = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Name"))
                                integerOption.Name = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Description"))
                                integerOption.Description = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("MinValue"))
                                integerOption.MinValue = extractInteger(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("MaxValue"))
                                integerOption.MaxValue = extractInteger(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("defaultValue"))
                                integerOption.defaultValue = extractInteger(memberValuePair.getValue());
                        }
                    }

                    if(pc.Options == null)
                        pc.Options = new LinkedList<>();
                    pc.Options.add(integerOption);
                }
            }
        }
    }


    private void readDoubleOptions(ArrayInitializerExpr doubleOptionsArray, PluginConfiguration pc){
        for(Expression expression : doubleOptionsArray.getValues()){
            if(AnnotationExpr.class.isInstance(expression)){
                AnnotationExpr annotationExpr = (AnnotationExpr) expression;
                if(annotationExpr.getName().getName().equals("DeltaDoubleOption")){
                    DoubleOption doubleOption = new DoubleOption();

                    List<Node> annotationChildren = annotationExpr.getChildrenNodes();
                    for(Node annotationChild : annotationChildren) {
                        if (MemberValuePair.class.isInstance(annotationChild)) {
                            MemberValuePair memberValuePair = (MemberValuePair)annotationChild;
                            if(memberValuePair.getName().equals("ID"))
                                doubleOption.ID = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Name"))
                                doubleOption.Name = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("Description"))
                                doubleOption.Description = extractString(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("MinValue"))
                                doubleOption.MinValue = extractDouble(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("MaxValue"))
                                doubleOption.MaxValue = extractDouble(memberValuePair.getValue());
                            else if(memberValuePair.getName().equals("defaultValue"))
                                doubleOption.defaultValue = extractDouble(memberValuePair.getValue());
                        }
                    }

                    if(pc.Options == null)
                        pc.Options = new LinkedList<>();
                    pc.Options.add(doubleOption);
                }
            }
        }
    }

    private String extractString(Expression expr){
        if(expr == null)
            return "";

        if(StringLiteralExpr.class.isInstance(expr))
            return unescapeJava(((StringLiteralExpr) expr).getValue());

        if(BinaryExpr.class.isInstance(expr) && ((BinaryExpr) expr).getOperator() == BinaryExpr.Operator.plus) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;

            return extractString(binaryExpr.getLeft()) + extractString(binaryExpr.getRight());
        }

        return unescapeJava(expr.toStringWithoutComments());
    }

    private int extractInteger(Expression expr){
        if(IntegerLiteralExpr.class.isInstance(expr))
            return Integer.parseInt(expr.toStringWithoutComments());

        if(DoubleLiteralExpr.class.isInstance(expr))
            return (int)Double.parseDouble(expr.toStringWithoutComments());

        if(LongLiteralExpr.class.isInstance(expr))
            return (int)Long.parseLong(expr.toStringWithoutComments());

        if(BinaryExpr.class.isInstance(expr)) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;

            switch (binaryExpr.getOperator()){
                case plus:
                    return extractInteger(binaryExpr.getLeft()) + extractInteger(binaryExpr.getRight());
                case minus:
                    return extractInteger(binaryExpr.getLeft()) - extractInteger(binaryExpr.getRight());
                case times:
                    return extractInteger(binaryExpr.getLeft()) * extractInteger(binaryExpr.getRight());
                case divide:
                    return extractInteger(binaryExpr.getLeft()) / extractInteger(binaryExpr.getRight());
                case remainder:
                    return extractInteger(binaryExpr.getLeft()) % extractInteger(binaryExpr.getRight());
                case xor:
                    return extractInteger(binaryExpr.getLeft()) ^ extractInteger(binaryExpr.getRight());
            }
        }

        throw new ArithmeticException("Unable to decode expression (one of the operand or operators is not supported)");
    }

    private double extractDouble(Expression expr){
        if(IntegerLiteralExpr.class.isInstance(expr))
            return Integer.parseInt(expr.toStringWithoutComments());

        if(DoubleLiteralExpr.class.isInstance(expr))
            return Double.parseDouble(expr.toStringWithoutComments());

        if(LongLiteralExpr.class.isInstance(expr))
            return Long.parseLong(expr.toStringWithoutComments());

        if(BinaryExpr.class.isInstance(expr)) {
            BinaryExpr binaryExpr = (BinaryExpr) expr;

            switch (binaryExpr.getOperator()){
                case plus:
                    return extractDouble(binaryExpr.getLeft()) + extractDouble(binaryExpr.getRight());
                case minus:
                    return extractDouble(binaryExpr.getLeft()) - extractDouble(binaryExpr.getRight());
                case times:
                    return extractDouble(binaryExpr.getLeft()) * extractDouble(binaryExpr.getRight());
                case divide:
                    return extractDouble(binaryExpr.getLeft()) / extractDouble(binaryExpr.getRight());
                case remainder:
                    return extractDouble(binaryExpr.getLeft()) % extractDouble(binaryExpr.getRight());
            }
        }

        throw new ArithmeticException("Unable to decode expression (one of the operand or operators is not supported)");
    }
}
