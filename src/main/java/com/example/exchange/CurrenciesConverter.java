package com.example.exchange;

public class CurrenciesConverter {

    private double currentTva;
    private double calculatedTva;

    public double convertCurrenciesWithoutTva(double amount, double currency1, double currency2){
        double totalSum = amount * calculateCurrenciesRatio(currency1,currency2);
        calculateTva(totalSum);
        return totalSum;
    }

    public double convertCurrenciesWithTva(double amount, double currency1, double currency2){
        double totalSum = amount * calculateCurrenciesRatio(currency1,currency2);
        calculateTva(totalSum);
        return totalSum + calculatedTva;
    }

    private double calculateCurrenciesRatio(double firstCurrency, double secondCurrency){
        double ratio = 0;
        if(firstCurrency == secondCurrency){
            return 1;
        }
        if(firstCurrency == 0){
            ratio = 1 / secondCurrency;
        }
        else if(secondCurrency == 0){
            ratio = firstCurrency;
        }
        else{
            ratio = firstCurrency / secondCurrency;
        }
        return ratio;
    }

    private double calculateTva(double amount){
        calculatedTva = (currentTva / 100) * amount;
        return calculatedTva;
    }

    public double getCalculatedTva(){
        return calculatedTva;
    }

    public void setCurrentTVA(double tvaValue){
        currentTva = tvaValue;
    }

}
