package delta.desktoptools.experimentmaker;

import de.thomasbolz.javafx.NumberSpinner;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unipd.elia.delta.sharedlib.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;

/**
 * Created by Elia on 30/06/2015.
 */
public class OptionsControlController {
    @FXML GridPane gridOptions;
    @FXML VBox vboxMain;
    int currentRow = 0;

    public void Initialize(PluginConfiguration pluginConfiguration){
        if(pluginConfiguration.Options != null && pluginConfiguration.Options.size() > 0) {
            for (DeltaOption option : pluginConfiguration.Options){
                if(StringOption.class.isInstance(option))
                    readStringOptions((StringOption) option);
                else if(BooleanOption.class.isInstance(option))
                    readBooleanOptions((BooleanOption)option);
                else if(IntegerOption.class.isInstance(option))
                    readIntegerOption((IntegerOption) option);
                else if(DoubleOption.class.isInstance(option))
                    readDoubleOption((DoubleOption) option);
            }
        }

    }

    private void readStringOptions(final StringOption stringOption) {
        Label labelName = new Label(stringOption.Name);
        labelName.setWrapText(true);
        Control textEdit;
        stringOption.Value = stringOption.defaultValue;
        if(stringOption.AvailableChoices == null || stringOption.AvailableChoices.length == 0) {
            if (stringOption.Multiline) {
                TextArea textArea = new TextArea();
                textArea.setText(stringOption.defaultValue);

                textArea.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observableValue, String oldVal, String newVal) {
                        if (newVal == null || newVal.isEmpty())
                            stringOption.Value = null;
                        else {
                            stringOption.Value = newVal;
                        }
                    }
                });

                textEdit = textArea;
            }
            else {
                TextField textField = new TextField();
                textField.setText(stringOption.defaultValue);

                textField.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observableValue, String oldVal, String newVal) {
                        if (newVal == null || newVal.isEmpty())
                            stringOption.Value = null;
                        else {
                            stringOption.Value = newVal;
                        }
                    }
                });
                textEdit = textField;
            }
        }
        else {
            ComboBox<String> comboText = new ComboBox<>();
            comboText.getItems().addAll(stringOption.AvailableChoices);
            comboText.getSelectionModel().select(stringOption.defaultValue);

            comboText.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observableValue, String newVal, String oldVal) {
                    if (newVal == null || newVal.isEmpty())
                        stringOption.Value = null;
                    else {
                        stringOption.Value = newVal;
                    }
                }
            });
            comboText.setMaxWidth(Double.MAX_VALUE);

            textEdit = comboText;
        }

        textEdit.setTooltip(new Tooltip(stringOption.Description));
        GridPane.setHgrow(textEdit, Priority.ALWAYS);
        gridOptions.addRow(currentRow, labelName, textEdit);
        currentRow++;
    }

    private void readBooleanOptions(final BooleanOption booleanOption){
        booleanOption.Value = booleanOption.defaultValue;
        CheckBox checkBox = new CheckBox(booleanOption.Name);
        checkBox.setAllowIndeterminate(false);
        //checkBox.setIndeterminate(true);
        checkBox.setTooltip(new Tooltip(booleanOption.Description));
        checkBox.setSelected(booleanOption.defaultValue);

        checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldVal, Boolean newVal) {
                booleanOption.Value = newVal;
            }
        });

        GridPane.setHgrow(checkBox, Priority.ALWAYS);
        GridPane.setColumnSpan(checkBox, 2);
        gridOptions.addRow(currentRow, checkBox);
        currentRow++;
    }


    private void readIntegerOption(final IntegerOption integerOption){
        integerOption.Value = integerOption.defaultValue;
        Label label = new Label(integerOption.Name);
        final NumberSpinner numberSpinner = new NumberSpinner(BigDecimal.valueOf(integerOption.defaultValue),
                BigDecimal.valueOf(1), NumberFormat.getIntegerInstance());

        numberSpinner.numberProperty().addListener(new ChangeListener<BigDecimal>() {
            @Override
            public void changed(ObservableValue<? extends BigDecimal> observableValue, BigDecimal oldVal, BigDecimal newVal) {
                int newIntVal = newVal.toBigInteger().intValue();
                if ((integerOption.MaxValue == null || newIntVal <= integerOption.MaxValue) && (integerOption.MinValue == null || newIntVal >= integerOption.MinValue)) {
                    integerOption.Value = newIntVal;
                } else
                    numberSpinner.setNumber(oldVal);
            }
        });

        numberSpinner.setTooltip(new Tooltip(integerOption.Description));
        GridPane.setHgrow(numberSpinner, Priority.ALWAYS);
        numberSpinner.setMaxWidth(Double.MAX_VALUE);
        gridOptions.addRow(currentRow, label, numberSpinner);
        currentRow++;
    }


    private void readDoubleOption(final DoubleOption doubleOption){
        doubleOption.Value = doubleOption.defaultValue;
        Label label = new Label(doubleOption.Name);
        final NumberSpinner numberSpinner = new NumberSpinner(BigDecimal.valueOf(doubleOption.defaultValue),
                BigDecimal.valueOf(0.01), NumberFormat.getInstance());


        numberSpinner.numberProperty().addListener(new ChangeListener<BigDecimal>() {
            @Override
            public void changed(ObservableValue<? extends BigDecimal> observableValue, BigDecimal oldVal, BigDecimal newVal) {
                double newDoubleVal = newVal.doubleValue();
                if ((doubleOption.MaxValue == null || newDoubleVal <= doubleOption.MaxValue) && (doubleOption.MinValue == null || newDoubleVal >= doubleOption.MinValue)) {
                    doubleOption.Value = newDoubleVal;
                } else
                    numberSpinner.setNumber(oldVal);
            }
        });

        numberSpinner.setTooltip(new Tooltip(doubleOption.Description));
        GridPane.setHgrow(numberSpinner, Priority.ALWAYS);
        numberSpinner.setMaxWidth(Double.MAX_VALUE);
        gridOptions.addRow(currentRow, label, numberSpinner);
        currentRow++;
    }
}
