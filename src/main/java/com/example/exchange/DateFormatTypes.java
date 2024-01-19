package com.example.exchange;

import java.time.format.DateTimeFormatter;

public enum DateFormatTypes {

    YEAR_MONTHVALUE_DAY(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    DAY_MONTHNAME_YEAR(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
    DAY_MONTHVALUE_YEAR(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    DateTimeFormatter dateFormat;

    DateFormatTypes(DateTimeFormatter dateFormat){
        this.dateFormat = dateFormat;
    }

}
