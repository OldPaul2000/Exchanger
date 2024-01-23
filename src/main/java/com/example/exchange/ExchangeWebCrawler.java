package com.example.exchange;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ExchangeWebCrawler {

    private static ExchangeWebCrawler exchangeWebCrawlerInstance;
    private ExchangeWebCrawler(){};

    public static ExchangeWebCrawler getInstance(){
        if(exchangeWebCrawlerInstance == null){
            exchangeWebCrawlerInstance = new ExchangeWebCrawler();
        }
        return exchangeWebCrawlerInstance;
    }


    private String cursBnrAzi = "https://www.cursbnr.ro/curs-bnr-azi";
    private Connection connection;
    private Document mainPage;

    private double tva;
    private HashMap<String, Double> currenciesAbbreviationAndValue = new LinkedHashMap<>();
    private HashMap<String, String> currenciesAbbreviationAndName = new LinkedHashMap<>();


    public int connectToWebsite() throws IOException{
        connection = Jsoup.connect(cursBnrAzi);
        mainPage = connection.get();
        return connection.response().statusCode();
    }

    public void getDataFromWeb() throws IOException{
        Elements webPageElements = mainPage.select("*");
        for(int i = 0; i < webPageElements.size(); i++){
            String elementTextLine = webPageElements.get(i).text();
            System.out.println(elementTextLine);

            if(elementTextLine.matches("Procent TVA (\\d+%\\s)(\\d+%\\s?)+")){
                tva = Integer.parseInt(elementTextLine.replaceAll("Procent TVA ((\\d+)%\\s)(\\d+%\\s?)+","$2"));
            }

            if(elementTextLine.matches("(100)?\\p{Upper}{3} .*")){
                extractCurrenciesAndTheirValues(elementTextLine);
            }
        }
    }

    private void extractCurrenciesAndTheirValues(String line){
        line = removeLastValue(line);
        String[] separatedValues = line.split(" ");
        String currencyAbbreviation = "";
        String currencyName = "";
        double currencyValue = 0;

        putRONInCurrenciesHashMaps();

        if(separatedValues.length >= 3 && separatedValues.length <= 7){
            currencyAbbreviation = separatedValues[0];
            currencyName = Arrays.toString(Arrays.stream(separatedValues).skip(1).limit(separatedValues.length - 2).toArray());
            currencyName = currencyName.replaceAll("[\\[\\],]","");
            currencyValue = Double.parseDouble(separatedValues[separatedValues.length - 1]);
            if(currencyAbbreviation.matches("\\d+.*")){
                currencyAbbreviation = currencyAbbreviation.replaceAll("\\d","");
                currencyValue /= 100;
            }

            currenciesAbbreviationAndValue.put(currencyAbbreviation,currencyValue);
            currenciesAbbreviationAndName.put(currencyAbbreviation,currencyName);
        }

    }

    private String removeLastValue(String lineInput){
        //Keeps the first three values from a line(Currency abbreviation, currency name and currency value)
        return lineInput.replaceAll("(.*) \\S+","$1");
    }

    private void putRONInCurrenciesHashMaps(){
        //RON currency will be introduced in hashmaps just for not modifying CurrenciesConverter methods.
        //If RON is selected the returned value will be 0 and the conversion methods will know to convert
        //from RON to another currency or vice-versa.

        currenciesAbbreviationAndValue.put("RON",0d);
        currenciesAbbreviationAndName.put("RON","Leu romanesc");
    }

    public HashMap<String, Double> getCurrenciesAbbreviationAndValue(){
        return currenciesAbbreviationAndValue;
    }

    public HashMap<String, String> getCurrenciesAbbreviationAndName(){
        return currenciesAbbreviationAndName;
    }

    public double getTva(){
        return tva;
    }

}
