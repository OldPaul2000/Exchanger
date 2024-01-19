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

    private final String QUERY_TABLE = "SELECT * FROM ";

    private final String QUERY_ENTRIES_COUNT = "SELECT count(*) FROM ";

    private final String DELETE_ALL = "DELETE FROM ";

    private final String INSERT = "INSERT INTO ";
    private final String INSERT_COLUMN_PLACEHOLDERS = "VALUES(?,?,?,?,?)";


    private final String DROP_TABLE = "DROP TABLE IF EXISTS ";
    private final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ";
    private final String TABLE_COLUMNS = "(CurrencyAbbreviation, CurrencyName, CurrencyValue, ValueDifference, Date)";

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

    public void printTableContent(String tableName){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder query = new StringBuilder(QUERY_TABLE);
        query.append(tableName);

        try(Statement statement = connection.createStatement()){
            ResultSet content = statement.executeQuery(query.toString());

            while (content.next()){
                System.out.println(content.getString(1) + "---" + content.getString(2) + "---" + content.getDouble(3) + "---" +
                        content.getDouble(4) + "---" + content.getString(5));
            }
            System.out.println();
        }
        catch (SQLException queryException){
            queryException.printStackTrace();
            System.out.println(queryException.getMessage());
        }
    }

    public int getTableEntriesCount(String tableName){
        tableName = switchAbbreviationAnd100(tableName);
        StringBuilder sb = new StringBuilder(QUERY_ENTRIES_COUNT);
        sb.append(tableName);

        int tableEntries = 0;

        try(Statement statement = connection.createStatement()){
            ResultSet entriesNumber = statement.executeQuery(sb.toString());
            tableEntries = entriesNumber.getInt(1);
        }
        catch (SQLException countingException){
            System.out.println(countingException.getMessage());
        }
        return tableEntries;
    }

    public void clearTable(String tableName){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder deleteAllFromTable = new StringBuilder(DELETE_ALL);
        deleteAllFromTable.append(tableName);

        try(Statement statement = connection.createStatement()){
            connection.setAutoCommit(false);

            int affectedRows = statement.executeUpdate(deleteAllFromTable.toString());
            if(affectedRows >= 1){
                connection.commit();
            }
            else {
                SQLException exception = new SQLException("Deletion exception");
                System.out.println(exception.getMessage());
                exception.printStackTrace();
                throw exception;
            }
        }
        catch (SQLException deletionException){
            System.out.println(deletionException.getMessage());
            try {
                connection.rollback();
            }
            catch (SQLException rollbackException){
                System.out.println(rollbackException.getMessage());
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            }
            catch (SQLException autocommitException){
                System.out.println(autocommitException.getMessage());
            }
        }
    }

    public void dropTable(String tableName){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder drop = new StringBuilder(DROP_TABLE);
        drop.append(tableName);

        try(Statement statement = connection.createStatement()){
            statement.executeUpdate(drop.toString());
        }
        catch (SQLException schemaException){
            System.out.println("Deletion table failed");
            System.out.println(schemaException.getMessage());
        }
    }


    public void createTable(String tableName){
        tableName = switchAbbreviationAnd100(tableName);

        StringBuilder create = new StringBuilder(CREATE_TABLE);
        create.append(tableName + " ");
        create.append(TABLE_COLUMNS);

        try(Statement statement = connection.createStatement()){
            statement.execute(create.toString());
            System.out.println("Created succesfully:" + tableName);
        }
        catch (SQLException creatingTableException){
            System.out.println("Failed creating table");
            System.out.println(creatingTableException.getMessage());
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
