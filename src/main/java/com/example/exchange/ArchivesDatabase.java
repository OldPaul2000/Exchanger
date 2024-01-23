package com.example.exchange;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ArchivesDatabase {

    private final Path localPath = FileSystems.getDefault().getPath("");
    private final String databaseName = "CurrenciesValues.db";
    private final String databaseLocation = "jdbc:sqlite:" + "src" + File.separator + databaseName;

    private Connection connection;

    private final String QUERY_MAX_PRICE = "SELECT MAX(CurrencyValue) FROM ";
    private final String QUERY_MIN_PRICE = "SELECT MIN(CurrencyValue) FROM ";

    private final String QUERY_ALL_YEARS = "SELECT Date FROM ";

    private final String QUERY_BY_CURRENCY_AND_YEAR = "SELECT * FROM ";
    private final String QUERY_BY_CURRENCY_AND_YEAR_CONDITIONALS = "WHERE Date LIKE ?";


    private final String INSERT = "INSERT INTO ";
    private final String INSERT_COLUMN_PLACEHOLDERS = "VALUES(?,?,?,?,?)";


    private PreparedStatement insertValue;
    private PreparedStatement queryCurrencyByYear;


    private static ArchivesDatabase instance;

    private ArchivesDatabase(){}

    public static ArchivesDatabase getInstance(){
        if(instance == null){
            instance = new ArchivesDatabase();
        }
        return instance;
    }

    public double getMinCurrencyValue(String currencyAbbreviation){
        StringBuilder sb = new StringBuilder(QUERY_MIN_PRICE);
        currencyAbbreviation = switchAbbreviationAnd100(currencyAbbreviation);
        sb.append(currencyAbbreviation);

        double minValue = 0;
        try(Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(sb.toString());

            if(resultSet.next()){
                minValue = resultSet.getDouble(1);
            }
        }
        catch (SQLException queryException){
            System.out.println(queryException.getMessage());
        }

        return minValue;
    }

    public double getMaxCurrencyValue(String currencyAbbreviation){
        StringBuilder sb = new StringBuilder(QUERY_MAX_PRICE);
        currencyAbbreviation = switchAbbreviationAnd100(currencyAbbreviation);
        sb.append(currencyAbbreviation);

        double maxValue = 0;
        try(Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(sb.toString());

            if(resultSet.next()){
                maxValue = resultSet.getDouble(1);
            }
        }
        catch (SQLException queryException){
            System.out.println(queryException.getMessage());
        }

        return maxValue;
    }


    public List<String> getAllTablesNames(){
        List<String> tableNames = new ArrayList<>();

        try{
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(null, null, null, new String[] { "TABLE" });

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        }
        catch (SQLException queryException){
            System.out.println(queryException.getMessage());
        }
        return tableNames;
    }

    public List<String> retrieveAllYearsEntries(String tableName){
        //This method will return the years from specific table to know the non-empty years
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder sb = new StringBuilder(QUERY_ALL_YEARS);
        sb.append(tableName);

        List<String> years = new ArrayList<>();

        try(Statement statement = connection.createStatement()){
            ResultSet resultYears = statement.executeQuery(sb.toString());
            while (resultYears.next()){
                String year = resultYears.getString(1).replaceAll("(\\d{4}).*","$1");
                if(!years.contains(year)) {
                    years.add(year);
                }
            }
        }
        catch (SQLException queryYearsException){
            System.out.println("Failed querying years");
            System.out.println(queryYearsException.getMessage());
        }

        return years;
    }

    public List<String[]> queryByTableAndYear(String tableName, String year){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder sb = new StringBuilder(QUERY_BY_CURRENCY_AND_YEAR);
        sb.append(tableName + " ");
        sb.append(QUERY_BY_CURRENCY_AND_YEAR_CONDITIONALS);

        List<String[]> currencyData = new ArrayList<>();

        try{
            queryCurrencyByYear = connection.prepareStatement(sb.toString());
            queryCurrencyByYear.setString(1, year + "%");

            ResultSet entries = queryCurrencyByYear.executeQuery();
            while (entries.next()){
                String abbreviation = entries.getString(1);
                String name = entries.getString(2);
                String price = String.valueOf(entries.getDouble(3));
                String priceDifference = String.valueOf(entries.getDouble(4));
                String date = entries.getString(5);
                currencyData.add(new String[] {abbreviation,name,price,priceDifference,date});
            }
        }
        catch (SQLException queryException){
            System.out.println("Error querying by year");
            System.out.println(queryException.getMessage());
        }
        return currencyData;
    }

    public void insertDataInTable(String tableName, String currencyAbbreviation, String currencyName, double currencyValue, double valueDifference, String date){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder insertIntoTable = new StringBuilder(INSERT);
        insertIntoTable.append(tableName + " ");
        insertIntoTable.append(INSERT_COLUMN_PLACEHOLDERS);

        try{
           connection.setAutoCommit(false);

           insertValue = connection.prepareStatement(insertIntoTable.toString());
           insertValue.setString(1,currencyAbbreviation);
           insertValue.setString(2,currencyName);
           insertValue.setDouble(3,currencyValue);
           insertValue.setDouble(4,valueDifference);
           insertValue.setString(5,date);

           int affectedRows = insertValue.executeUpdate();
           if(affectedRows == 1){
               connection.commit();
           }
           else {
               throw new SQLException("Error inserting value");
           }
        }
        catch (SQLException insertionException){
            insertionException.printStackTrace();
            System.out.println(insertionException.getMessage());
            System.out.println("Insert value exception");
            try{
                connection.rollback();
            }
            catch (SQLException e){
                System.out.println("Error rollback");
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            }
            catch (SQLException autocommitException){
                System.out.println("Error setting autocommit to true");
            }
        }
    }


    public void openDatabase(){
        try{
            connection = DriverManager.getConnection(databaseLocation);
        }
        catch (SQLException connectionException){
            System.out.println("Database connection failed");
            System.out.println(connectionException.getMessage());
        }
    }

    public void closeDatabase(){
        try {
            if(connection != null){
                connection.close();
            }
        }
        catch (SQLException closingException){
            System.out.println("Error closing database");
            System.out.println(closingException.getMessage());
        }
    }


    private String switchAbbreviationAnd100(String abbreviation){
        if(abbreviation.matches("\\d{3}\\p{Upper}{3}")){
            abbreviation = abbreviation.replaceAll("(\\d{3}?)(\\p{Upper}{3})","$2$1");
        }
        return abbreviation;
    }


}
