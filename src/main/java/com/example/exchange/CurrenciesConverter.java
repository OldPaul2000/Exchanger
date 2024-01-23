package com.example.exchange;

public class CurrenciesConverter {

    private double currentVAT;
    private double calculatedVAT;

    public double convertCurrenciesWithoutVAT(double amount, double currency1, double currency2){
        double totalSum = amount * calculateCurrenciesRatio(currency1,currency2);
        calculateVAT(totalSum);
        return totalSum;
    }

    public double convertCurrenciesWithVAT(double amount, double currency1, double currency2){
        double totalSum = amount * calculateCurrenciesRatio(currency1,currency2);
        calculateVAT(totalSum);
        return totalSum + calculatedVAT;
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

    private double calculateVAT(double amount){
        calculatedVAT = (currentVAT / 100) * amount;
        return calculatedVAT;
    }

    public double getCalculatedVAT(){
        return calculatedVAT;
    }

    public void setCurrentVAT(double tvaValue){
        currentVAT = tvaValue;
    }

}
