package com.example.exchange;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.example.exchange.DateFormatTypes.DAY_MONTHNAME_YEAR;

public class ExchangeController {

    @FXML
    public AnchorPane mainBackground;

    @FXML
    public AnchorPane converterBackground;

    @FXML
    public ComboBox<String> inputCurrency;

    @FXML
    public ComboBox<String> outputCurrency;

    @FXML
    public Button currenciesSwapper;

    @FXML
    public TextField sumField;

    @FXML
    public TextField resultField;

    @FXML
    public Label TVAFromCurrentSum;

    @FXML
    public TextField TVAFromAmount;

    @FXML
    public TextField resultWithTVA;

    private Label onlyDigits = new Label("Only numbers accepted");
    private Label noConnectionMessage = new Label("No connection");


    @FXML
    public AnchorPane chartBackground;

    @FXML
    public ComboBox<String> chartCurrencies;

    @FXML
    public ComboBox<String> chartYear;

    @FXML
    public Slider currencyValueScale;

    @FXML
    public AnchorPane chart;

    private final String DECREASE_PRICE_COLOR = "FF7272";
    private final String INCREASE_PRICE_COLOR = "8AE3A5";

    private Rectangle[] chartBars = new Rectangle[22];
    private Label[] priceLabels = new Label[22];
    private Label[] pricesDifference = new Label[22];
    private Label[] currencyDates = new Label[22];
    private String[] completeDates = new String[22];

    private Label previousMonth = new Label();
    private Label nextMonth = new Label();
    private Line monthChartSeparator = new Line();


    private final ExchangeWebCrawler exchangeWebCrawler = ExchangeWebCrawler.getInstance();
    private final CurrenciesConverter converter = new CurrenciesConverter();
    private final ArchiveCrawler crawler = new ArchiveCrawler();
    private final ArchivesDatabase databaseInstance = ArchivesDatabase.getInstance();
    private HashMap<String, Double> currenciesInitialsAndValue;
    private HashMap<String, String> currenciesInitialsAndName;
    private List<String[]> currencyInfo = new ArrayList<>();

    private Thread updateDatabase = new Thread(() -> {
        crawler.downloadAllNewArchives();
        crawler.crawlArchiveLink();
    });

    public void initialize() {
        setOutputTextFieldUneditable();
        setSumFieldInputProperties();
        setDisplayOnlyDigitsLabelProperties();
        setConverterComboBoxesProperties();
        setSwapButtonProperties();
        putBarsInChart();

        setAllStyles();

        if (connectToWebsiteAndRetrieveData() == 200) {
            databaseInstance.openDatabase();

            putCurrenciesInChartCheckbox();
            putCurrenciesInConverterCheckBoxes();
            initializeChartYear();
            putTVAValueInLabel();
            initializeComboBoxesValues();

            currencyInfo = databaseInstance.queryByTableAndYear(chartCurrencies.getSelectionModel().getSelectedItem(),
                                                                chartYear.getSelectionModel().getSelectedItem());
            setChartProperties();

            initializeBarsHeightsAndColors();

            putPriceLabelsInChart();
            putPriceDifferenceInChart();
            putDatesInChart();
            putMonthsInChart();
            putMonthsSeparatorInChart();
            setScaleValues(chartCurrencies.getSelectionModel().getSelectedItem());

            setChartCurrenciesListener();
            crawler.connectToMainPage();
            updateDatabase.start();
            setChartYearComboBoxProperties();
        } else {
            displayNoConnection();
        }


        converter.setCurrentTVA(exchangeWebCrawler.getTva());

    }

    private void initializeBarsHeightsAndColors(){
        int currencyInfoIndex = currencyInfo.size() - 1;

        System.out.println("Currency info index:" + currencyInfoIndex);
        for(int i = chartBars.length - 1; i >= 0; i--){
            System.out.println("i:" + i);
            if(i > currencyInfo.size() - 1){
                System.out.println("i > than chartbars length---chartbars length:" + chartBars.length);
                chartBars[i].setHeight(0);
                currencyInfoIndex = currencyInfo.size() - 1;
            }
            else{
                System.out.println("Initializing bar");
                double currencyValue = Double.parseDouble(currencyInfo.get(currencyInfoIndex)[2]);
                chartBars[i].setHeight(calculateBarHeight(currencyValue));
                chartBars[i].setLayoutY(chart.getPrefHeight() - chartBars[i].getHeight());

                double priceDifference = Double.parseDouble(currencyInfo.get(currencyInfoIndex)[3]);
                if(priceDifference < 0){
                    chartBars[i].setFill(Color.valueOf(DECREASE_PRICE_COLOR));
                }
                else {
                    chartBars[i].setFill(Color.valueOf(INCREASE_PRICE_COLOR));
                }
            }
            currencyInfoIndex--;
            if(currencyInfoIndex < 0){
                break;
            }
        }
    }

    private int calculateBarHeight(double currencyValue){
        double maxCurrencyValue = databaseInstance.getMaxCurrencyValue(chartCurrencies.getSelectionModel().getSelectedItem());
        double barHeightRatio = chart.getPrefHeight() / maxCurrencyValue;

        return (int)(barHeightRatio * currencyValue);
    }


    final int X_PRICE_OFFSET = 2;
    final int Y_PRICE_OFFSET = 10;
    final double Y_PRICE_DIFF = 520;

    final double Y_DATE = 540;
    final double X_MONTH_OFFSET = 60;
    final double Y_MONTH = 620;
    final double X_MONTH_LABEL_MOVING_STEP = 45;
    final double X_START_MONTH_LABEL = 65;
    final double X_END_MONTH_LABEL = 1010;

    final double X_MONTHS_SEPARATOR_OFFSET = 2.5;
    final double Y_START_MONTHS_SEPARATOR = 1;
    final double Y_END_MONTHS_SEPARATOR_OFFSET = 15;

    private double incrementingStep = 0;
    private double slidingPosition = 0;
    private final double FIRST_BAR_POSITION = 5;
    private final double LAST_BAR_POSITION = 950;
    private final double MOVING_STEP = 45;

    private final double BAR_WIDTH = 40;

    private boolean movedRight = false;
    private boolean movedLeft = false;
    private boolean monthsSeparatorMovedLeft = false;

    private int currencyInfoIndex;

    private DecimalFormat decimalFormat = new DecimalFormat("#.####");

    private boolean posPrinted = false;

    private void setChartProperties(){
        currencyInfoIndex = currencyInfo.size() - chartBars.length;

        chart.setOnMouseDragged(mouseEvent -> {
            posPrinted = false;

            movedRight = true;
            movedLeft = false;
            incrementingStep += 0.25;
             if(mouseEvent.getSceneX() > slidingPosition && incrementingStep >= 1){
                 incrementingStep = 0;

                 if(currencyInfoIndex >= 0){
                     for(int i = chartBars.length - 1; i >= 0; i--){
                         if(chartBars[i].getLayoutX() >= LAST_BAR_POSITION){
                             chartBars[i].setLayoutX(FIRST_BAR_POSITION);
                             setBarHeightAndColor(chartBars[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex)[2]),
                                                               Double.parseDouble(currencyInfo.get(currencyInfoIndex)[3]));
                             setPriceLabelValue(priceLabels[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex)[2]));
                             setPriceLabelHeight(priceLabels[i],chartBars[i]);
                             setPriceLabelDiffValue(pricesDifference[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex)[3]));
                             setDateLabelValue(currencyDates[i],currencyInfo.get(currencyInfoIndex)[4]);
                             completeDates[i] = currencyInfo.get(currencyInfoIndex)[4];
                             currencyInfoIndex--;
                         }
                         else {
                             chartBars[i].setLayoutX(chartBars[i].getLayoutX() + MOVING_STEP);
                         }
                         priceLabels[i].setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
                         pricesDifference[i].setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
                         currencyDates[i].setLayoutX(chartBars[i].getLayoutX());
                         if(i < completeDates.length - 1){
                             setMonthLabelsPosition(currencyDates[i],currencyDates[i + 1],completeDates[i],completeDates[i + 1]);
                             setMonthLabelsValue(completeDates[i],completeDates[i + 1]);
                             setMonthSeparatorPosition(chartBars[i + 1],completeDates[i],completeDates[i + 1]);
                         }
                     }
                 }
                 monthsSeparatorMovedLeft = false;
             }

             else if(mouseEvent.getSceneX() < slidingPosition && incrementingStep >= 1){
                 incrementingStep = 0;

                 movedRight = false;
                 movedLeft = true;
                 monthsSeparatorMovedLeft = true;

                 if(currencyInfoIndex < currencyInfo.size() - chartBars.length){
                     for(int i = 0; i < chartBars.length; i++){
                         if(chartBars[i].getLayoutX() <= FIRST_BAR_POSITION){
                             chartBars[i].setLayoutX(LAST_BAR_POSITION);
                             setBarHeightAndColor(chartBars[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex + chartBars.length)[2]),
                                                               Double.parseDouble(currencyInfo.get(currencyInfoIndex + chartBars.length)[3]));
                             setPriceLabelValue(priceLabels[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex + chartBars.length)[2]));
                             setPriceLabelHeight(priceLabels[i],chartBars[i]);
                             setPriceLabelDiffValue(pricesDifference[i],Double.parseDouble(currencyInfo.get(currencyInfoIndex + chartBars.length)[3]));
                             setDateLabelValue(currencyDates[i],currencyInfo.get(currencyInfoIndex + chartBars.length)[4]);
                             completeDates[i] = currencyInfo.get(currencyInfoIndex + chartBars.length)[4];
                             currencyInfoIndex++;
                         }
                         else{
                             chartBars[i].setLayoutX(chartBars[i].getLayoutX() - MOVING_STEP);
                         }
                         priceLabels[i].setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
                         pricesDifference[i].setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
                         if(i < completeDates.length - 1){
                             setMonthSeparatorPosition(chartBars[i],completeDates[i],completeDates[i + 1]);
                             setMonthLabelsPosition(currencyDates[i],currencyDates[i + 1],completeDates[i],completeDates[i + 1]);
                             setMonthLabelsValue(completeDates[i + 1],completeDates[i]);
                         }
                         currencyDates[i].setLayoutX(chartBars[i].getLayoutX());
                     }
                 }
             }
            slidingPosition = mouseEvent.getSceneX();
        });
    }



    private void setMonthSeparatorPosition(Rectangle chartBar, String fullDate1, String fullDate2){
        LocalDate date1 = LocalDate.parse(fullDate1);
        LocalDate date2 = LocalDate.parse(fullDate2);

        if(date1.getMonthValue() < date2.getMonthValue()){
            if(monthsSeparatorMovedLeft){
                monthChartSeparator.setLayoutX(chartBar.getLayoutX() + BAR_WIDTH + X_MONTHS_SEPARATOR_OFFSET);
                monthsSeparatorMovedLeft = false;
            }
            else{
                monthChartSeparator.setLayoutX(chartBar.getLayoutX() - X_MONTHS_SEPARATOR_OFFSET);
            }
        }
    }

    private double previousMonthLabelPosition;
    private double nextMonthLabelPosition;

    private void setMonthLabelsPosition(Label prevDate, Label nextDate, String fullDate1, String fullDate2){
        previousMonthLabelPosition = prevDate.getLayoutX() + X_MONTH_OFFSET;
        nextMonthLabelPosition = nextDate.getLayoutX() + X_MONTH_OFFSET;

        LocalDate date1 = LocalDate.parse(fullDate1);
        LocalDate date2 = LocalDate.parse(fullDate2);

//        System.out.println(previousMonthLabelPosition + "---" + nextMonthLabelPosition);
//        System.out.println(prevDate + "---" + nextDate);


        if(!movedRight){
            previousMonthLabelPosition -= X_MONTH_LABEL_MOVING_STEP;
            nextMonthLabelPosition -= X_MONTH_LABEL_MOVING_STEP;
        }
        else if(movedLeft){
            previousMonthLabelPosition += X_MONTH_LABEL_MOVING_STEP * 2;
            nextMonthLabelPosition += X_MONTH_LABEL_MOVING_STEP * 2;
            movedLeft = false;
        }


        if(!posPrinted){
//           System.out.println("Next x:" + nextMonth.getLayoutX());
//            System.out.println("Moved left:" + movedLeft);
//            System.out.println();
//            System.out.println();
            posPrinted = true;
        }

        if(date1.getMonthValue() < date2.getMonthValue() && previousMonthLabelPosition < nextMonthLabelPosition){
//            System.out.println(previousMonthLabelPosition + "---" + nextMonthLabelPosition);
            previousMonth.setLayoutX(previousMonthLabelPosition);
            nextMonth.setLayoutX(nextMonthLabelPosition);
        }


//        if(previousMonthLabelPosition > X_END_MONTH_LABEL || previousMonthLabelPosition < X_START_MONTH_LABEL){
//            System.out.println("first");
//            previousMonth.setText("");
//        }
//        if(nextMonthLabelPosition > X_END_MONTH_LABEL || nextMonthLabelPosition < X_START_MONTH_LABEL){
//            System.out.println("second");
//            nextMonth.setText("");
//        }
//
    }

    private void setMonthLabelsValue(String prevDate, String nextDate){
        LocalDate date1 = LocalDate.parse(prevDate);
        String month1 = date1.format(DAY_MONTHNAME_YEAR.dateFormat);
        month1 = month1.replaceAll(".*-(.*)-.*","$1");

        LocalDate date2 = LocalDate.parse(nextDate);
        String month2 = date2.format(DAY_MONTHNAME_YEAR.dateFormat);
        month2 = month2.replaceAll(".*-(.*)-.*","$1");

        if(!month1.equals(month2)){
            previousMonth.setText(month1);
            nextMonth.setText(month2);
        }
    }

    private void setDateLabelValue(Label dateLabel, String date){
        LocalDate localDate = LocalDate.parse(date);
        dateLabel.setText(localDate.getDayOfMonth() + "");
    }

    private void setPriceLabelHeight(Label priceLabel, Rectangle bar){
        priceLabel.setLayoutY(bar.getLayoutY() + bar.getHeight() / 2 - Y_PRICE_OFFSET);
    }

    private void setPriceLabelValue(Label priceLabel,double price){
        priceLabel.setText("" + price);
    }

    private void setPriceLabelDiffValue(Label diffLabel, double priceDiff){
        diffLabel.setText(decimalFormat.format(Math.abs(priceDiff)) + "");
    }

    private void setBarHeightAndColor(Rectangle bar, double price, double priceDifference){
        if(priceDifference < 0){
            bar.setFill(Color.valueOf(DECREASE_PRICE_COLOR));
        }
        else{
            bar.setFill(Color.valueOf(INCREASE_PRICE_COLOR));
        }
        bar.setHeight(calculateBarHeight(price));
        bar.setLayoutY(chart.getPrefHeight() - bar.getHeight());
    }

    private void putMonthsSeparatorInChart(){
        int currencyInfoIndex = currencyInfo.size() - currencyDates.length;
        monthChartSeparator.setStroke(Color.BLACK);

        if(currencyInfoIndex > 22){
            for(int i = 0; i < currencyDates.length - 1; i++){
                LocalDate firstDate = LocalDate.parse(currencyInfo.get(currencyInfoIndex)[4]);
                LocalDate secondDate = LocalDate.parse(currencyInfo.get(currencyInfoIndex + 1)[4]);

                if(!firstDate.getMonth().equals(secondDate.getMonth())){
                    System.out.println(firstDate.getMonth() + "---" + secondDate.getMonth());
                    monthChartSeparator.setLayoutX(chartBars[i].getLayoutX() + BAR_WIDTH + X_MONTHS_SEPARATOR_OFFSET);
                    monthChartSeparator.setStartY(Y_START_MONTHS_SEPARATOR);
                    monthChartSeparator.setEndY(chart.getPrefHeight() + Y_END_MONTHS_SEPARATOR_OFFSET);
                    break;
                }
                currencyInfoIndex++;
            }
        }
        else{
            monthChartSeparator.setStroke(Color.TRANSPARENT);
            System.out.println("month chart separator set transparent");
        }

        monthChartSeparator.setStyle("-fx-stroke-dash-array: 5 5");
        if(!chart.getChildren().contains(monthChartSeparator) && currencyInfoIndex > 22){
            chart.getChildren().add(monthChartSeparator);
        }
    }

    private void putMonthsInChart(){
        int currencyInfoIndex = currencyInfo.size() - currencyDates.length;
        if(currencyInfoIndex < 22){
            currencyInfoIndex = 0;
        }

        boolean monthsAreEqual = false;

        for(int i = 0; i < currencyDates.length - 1; i++){
            LocalDate firstDate = null;
            LocalDate secondDate = null;
            String firstMonth = "";
            String secondMonth = "";

            if(i < currencyInfoIndex){
                firstDate = LocalDate.parse(currencyInfo.get(currencyInfoIndex)[4]);
                secondDate = LocalDate.parse(currencyInfo.get(currencyInfoIndex + 1)[4]);
                firstMonth = firstDate.format(DAY_MONTHNAME_YEAR.dateFormat);
                firstMonth = firstMonth.replaceAll(".*-(.*)-.*","$1");
                secondMonth = secondDate.format(DAY_MONTHNAME_YEAR.dateFormat);
                secondMonth = secondMonth.replaceAll(".*-(.*)-.*","$1");
            }
            else{
                previousMonth.setText("");
                nextMonth.setText("");
            }


            if(firstDate != null && secondDate != null &&
               !firstDate.getMonth().equals(secondDate.getMonth())){
                System.out.println(firstDate.getMonth() + "---" + secondDate.getMonth());
                previousMonth.setText(firstMonth);
                nextMonth.setText(secondMonth);
                previousMonth.setLayoutX(chartBars[i].getLayoutX() + X_MONTH_OFFSET);
                nextMonth.setLayoutX(chartBars[i + 1].getLayoutX() + X_MONTH_OFFSET);
                previousMonth.setLayoutY(Y_MONTH);
                nextMonth.setLayoutY(Y_MONTH);

                System.out.println(chartBars[i].getLayoutX() + "---" + chartBars[i + 1].getLayoutX());
                break;
            }
            currencyInfoIndex++;
        }

        previousMonth.setPrefWidth(BAR_WIDTH);
        nextMonth.setPrefWidth(BAR_WIDTH);
        previousMonth.setAlignment(Pos.CENTER_RIGHT);
        nextMonth.setAlignment(Pos.CENTER_LEFT);

        if(chartBackground.getChildren().contains(previousMonth) && chartBackground.getChildren().contains(nextMonth)){

        }
        else{
            chartBackground.getChildren().add(previousMonth);
            chartBackground.getChildren().add(nextMonth);
        }
    }


    private void putDatesInChart(){
        int currencyInfoIndex = currencyInfo.size() - currencyDates.length;
        if(currencyInfoIndex < 22){
            currencyInfoIndex = 0;
        }

        for(int i = 0; i < currencyDates.length; i++){
            chart.getChildren().remove(currencyDates[i]);
            Label date = new Label();
            System.out.println(currencyInfo.size());
            LocalDate localDate = null;
            String completeDateString = "";

            if(i < currencyInfoIndex){
                localDate = LocalDate.parse(currencyInfo.get(currencyInfoIndex)[4]);
                date.setText(localDate.getDayOfMonth() + "");
                completeDateString = localDate.toString();
            }
            else{
                date.setText("");
            }

            date.setPrefWidth(BAR_WIDTH);
            date.setAlignment(Pos.CENTER);
            date.setLayoutX(chartBars[i].getLayoutX());
            date.setLayoutY(Y_DATE);
            chart.getChildren().add(date);
            completeDates[i] = completeDateString;
            currencyDates[i] = date;
            currencyInfoIndex++;
        }
    }

    private void putPriceDifferenceInChart(){
       int currencyInfoIndex = currencyInfo.size() - pricesDifference.length;
        if(currencyInfoIndex < 22){
            currencyInfoIndex = 0;
        }

        for(int i = 0; i < pricesDifference.length; i++){
            chart.getChildren().remove(pricesDifference[i]);
            Label diffLabel = new Label();

            if(i < currencyInfo.size()){
                diffLabel.setText(decimalFormat.format(Math.abs(Double.parseDouble(currencyInfo.get(currencyInfoIndex)[3]))) + "");
            }
            else{
                diffLabel.setText("");
            }

            diffLabel.setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
            diffLabel.setLayoutY(Y_PRICE_DIFF);
            chart.getChildren().add(diffLabel);
            pricesDifference[i] = diffLabel;
            currencyInfoIndex++;
        }
    }

    private void putPriceLabelsInChart(){
        int currencyInfoIndex = currencyInfo.size() - priceLabels.length;
        if(currencyInfoIndex < 22){
            currencyInfoIndex = 0;
        }

        for(int i = 0; i < priceLabels.length; i++){
            chart.getChildren().remove(priceLabels[i]);
            Label priceLabel = new Label();

            if(i < currencyInfo.size()){
                priceLabel.setText(currencyInfo.get(currencyInfoIndex)[2]);
            }
            else{
                priceLabel.setText("");
            }

            priceLabel.setLayoutX(chartBars[i].getLayoutX() + X_PRICE_OFFSET);
            priceLabel.setLayoutY(chartBars[i].getLayoutY() + chartBars[i].getHeight() / 2 - Y_PRICE_OFFSET);
            chart.getChildren().add(priceLabel);
            priceLabels[i] = priceLabel;
            currencyInfoIndex++;
        }
    }

    private void putBarsInChart(){
         double startingX = 5;
         double DEFAULT_BAR_HEIGHT = 100;

        for(int i = 0; i < chartBars.length; i++){
            Rectangle bar = new Rectangle();
            bar.setHeight(DEFAULT_BAR_HEIGHT);
            bar.setLayoutX(startingX);
            bar.setLayoutY(chart.getPrefHeight() - DEFAULT_BAR_HEIGHT);
            bar.setWidth(BAR_WIDTH);

            startingX += BAR_WIDTH + 5;
            chart.getChildren().add(bar);
            chartBars[i] = bar;
        }
    }


    private void setScaleValues(String tableName){
        double minValue = databaseInstance.getMinCurrencyValue(tableName);
        double maxValue = databaseInstance.getMaxCurrencyValue(tableName);
        currencyValueScale.setMin(minValue);
        currencyValueScale.setMax(maxValue);
        currencyValueScale.setMajorTickUnit(maxValue / 15);
        currencyValueScale.setMinorTickCount(5);
    }

    private void displayNoConnection() {
        noConnectionMessage.setFont(Font.font(20));
        noConnectionMessage.setLayoutX(96);
        noConnectionMessage.setLayoutY(450);
        converterBackground.getChildren().add(noConnectionMessage);
    }

    private void setSwapButtonProperties() {
        currenciesSwapper.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().toString().equals("PRIMARY")) {
                String tempComboBoxValue = inputCurrency.getValue();
                inputCurrency.setValue(outputCurrency.getValue());
                outputCurrency.setValue(tempComboBoxValue);
                if (!sumField.getText().isEmpty()) {
                    calculateConversionsAndUpdateTextFields(sumField.getText());
                }
            }
        });
    }

    private void setSumFieldInputProperties() {
        final Pattern ACCEPT_ONLY_DIGITS = Pattern.compile("(\\d+\\.?\\d*)");
        TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
            if (ACCEPT_ONLY_DIGITS.matcher(change.getControlNewText()).matches()) {
                return change;
            }
            if (change.getControlNewText().matches("")) {
                return change;
            }
            return null;
        });
        sumField.setTextFormatter(textFormatter);

        sumField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d+\\.") && !newValue.isEmpty()) {
                calculateConversionsAndUpdateTextFields(newValue);
            } else {
                resultField.setText("");
                TVAFromAmount.setText("");
                resultWithTVA.setText("");
            }
        });

        sumField.setOnKeyPressed(keyEvent -> {
            if (!keyEvent.getCode().isDigitKey() && !keyEvent.getCode().equals(KeyCode.PERIOD) && !keyEvent.getCode().equals(KeyCode.BACK_SPACE)) {
                displayOnlyDigitsWarning();
            } else {
                removeOnlyDigitsWarning();
            }
        });
    }

    private void calculateConversionsAndUpdateTextFields(String doubleInput) {
        DecimalFormat withoutTVA = new DecimalFormat("#,###,###.####");
        DecimalFormat TVAFromCurrentAmount = new DecimalFormat("#,###,###.####");
        DecimalFormat withTVA = new DecimalFormat("#,###,###.####");

        double inputValue = Double.parseDouble(doubleInput);
        double firstCurrency = currenciesInitialsAndValue.get(inputCurrency.getValue());
        double secondCurrency = currenciesInitialsAndValue.get(outputCurrency.getValue());
        resultField.setText(withoutTVA.format(converter.convertCurrenciesWithoutTva(inputValue, firstCurrency, secondCurrency)));
        TVAFromAmount.setText(TVAFromCurrentAmount.format(converter.getCalculatedTva()));
        resultWithTVA.setText(withTVA.format(converter.convertCurrenciesWithTva(inputValue, firstCurrency, secondCurrency)));
    }

    private void removeOnlyDigitsWarning() {
        converterBackground.getChildren().remove(onlyDigits);
    }

    private void displayOnlyDigitsWarning() {
        if (!converterBackground.getChildren().contains(onlyDigits)) {
            converterBackground.getChildren().add(onlyDigits);
        }
    }

    private void setConverterComboBoxesProperties() {
        inputCurrency.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!sumField.getText().isEmpty() && (sumField.getText().matches("\\d+|\\d+\\.\\d+"))) {
                calculateConversionsAndUpdateTextFields(sumField.getText());
            }
        });

        outputCurrency.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!sumField.getText().isEmpty() && (sumField.getText().matches("\\d+|\\d+\\.\\d+"))) {
                calculateConversionsAndUpdateTextFields(sumField.getText());
            }
        });
    }

    private void initializeComboBoxesValues() {
        inputCurrency.getSelectionModel().selectFirst();
        outputCurrency.getSelectionModel().selectFirst();
        chartCurrencies.getSelectionModel().selectFirst();
        chartYear.getSelectionModel().selectFirst();
    }

    private void setDisplayOnlyDigitsLabelProperties() {
        onlyDigits.setLayoutX(99);
        onlyDigits.setLayoutY(155);
        onlyDigits.setFont(Font.font(11));
        onlyDigits.setTextFill(Color.valueOf("FF4700"));
    }

    private void putTVAValueInLabel() {
        TVAFromCurrentSum.setText("TVA(" + (int) exchangeWebCrawler.getTva() + "%)");
    }

    private void initializeChartYear(){
        List<String> years = databaseInstance.retrieveAllYearsEntries(chartCurrencies.getItems().get(0));
        chartYear.getItems().addAll(years);
        chartYear.getSelectionModel().select(0);
    }

    private void setChartCurrenciesListener(){
        chartCurrencies.valueProperty().addListener((observableVal, oldVal, newVal) -> {

            setScaleValues(newVal);
            currencyInfo = databaseInstance.queryByTableAndYear(newVal,
                    chartYear.getSelectionModel().getSelectedItem());

            chartYear.getItems().clear();
            List<String> years = databaseInstance.retrieveAllYearsEntries(newVal);
            chartYear.getItems().addAll(years);
            chartYear.getSelectionModel().select(0);

            initializeBarsHeightsAndColors();

            putPriceLabelsInChart();
            putPriceDifferenceInChart();
            putDatesInChart();
            putMonthsInChart();
            putMonthsSeparatorInChart();
            setScaleValues(chartCurrencies.getSelectionModel().getSelectedItem());
            currencyInfoIndex = currencyInfo.size() - chartBars.length;
        });
    }

    private void setChartYearComboBoxProperties(){
            chartYear.getSelectionModel().select(0);
            chartYear.valueProperty().addListener((observableValue, oldVal, newVal) -> {

            currencyInfo = databaseInstance.queryByTableAndYear(chartCurrencies.getSelectionModel().getSelectedItem(),
                    chartYear.getSelectionModel().getSelectedItem());

            initializeBarsHeightsAndColors();

            putPriceLabelsInChart();
            putPriceDifferenceInChart();
            putDatesInChart();
            putMonthsInChart();
            putMonthsSeparatorInChart();
            setScaleValues(chartCurrencies.getSelectionModel().getSelectedItem());
            currencyInfoIndex = currencyInfo.size() - chartBars.length;
        });
    }

    private void putCurrenciesInConverterCheckBoxes(){
        for (Map.Entry<String, Double> entry : currenciesInitialsAndValue.entrySet()) {
            inputCurrency.getItems().add(entry.getKey());
            outputCurrency.getItems().add(entry.getKey());
        }
    }

    private void putCurrenciesInChartCheckbox() {
        List<String> abbreviations = databaseInstance.getAllTablesNames();
        inputCurrency.getItems().add("RON");
        outputCurrency.getItems().add("RON");
        for (String currency : abbreviations) {
            if(currency.contains("100")){
                chartCurrencies.getItems().add(currency.replaceAll("(\\w+)100","100$1"));
                currency = currency.replaceAll("100","");
            }
            else{
                chartCurrencies.getItems().add(currency);
            }
            inputCurrency.getItems().add(currency);
            outputCurrency.getItems().add(currency);
        }
    }

    private int connectToWebsiteAndRetrieveData() {
        int statusCode = 0;
        try {
            statusCode = exchangeWebCrawler.connectToWebsite();
            exchangeWebCrawler.getDataFromWeb();
            currenciesInitialsAndValue = exchangeWebCrawler.getCurrenciesAbbreviationAndValue();
            currenciesInitialsAndName = exchangeWebCrawler.getCurrenciesAbbreviationAndName();
        } catch (IOException connectionError) {

        }
        return statusCode;
    }

    private void setOutputTextFieldUneditable() {
        resultField.setEditable(false);
        TVAFromAmount.setEditable(false);
        resultWithTVA.setEditable(false);
    }


    private void setAllStyles(){
        final String STYLE_FILE = "Styling.css";

        converterBackground.setBackground(new Background(new BackgroundFill(Color.valueOf("89D050"), null, null)));
        chartBackground.setBackground(new Background(new BackgroundFill(Color.valueOf("519B96"), null, null)));
        chart.setBackground(new Background(new BackgroundFill(Color.valueOf("005D9C"), null, null)));

        converterBackground.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        chartBackground.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        chart.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());

        inputCurrency.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        outputCurrency.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        currenciesSwapper.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        sumField.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        resultField.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        TVAFromAmount.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        resultWithTVA.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        chartCurrencies.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        chartYear.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
        currencyValueScale.getStylesheets().add(getClass().getResource(STYLE_FILE).toExternalForm());
    }

}