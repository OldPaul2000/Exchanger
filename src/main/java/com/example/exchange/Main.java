package com.example.exchange;

import java.text.DecimalFormat;

public class Main {
    public static void main(String[] args) {

        double number = 0.0001;

        DecimalFormat df = new DecimalFormat("#.####");

        System.out.println(number);
        System.out.println(df.format(number));





    }
}
