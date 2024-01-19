package com.example.exchange;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ArchiveCrawler {

    private final String ARCHIVE_EXCHANGES_URL = "https://www.cursbnr.ro/arhiva-curs-bnr";
    private Connection connection;
    private Document mainPage;

    private Path localPath = FileSystems.getDefault().getPath("");
    private String lastArchiveFileName = "Last downloaded archive.txt";
    private String lastArchiveFileUrl = "src" + File.separator + lastArchiveFileName;

    private LocalDate archiveDate;
    private LocalDate lastArchiveDate;
    public static List<LocalDate> archives = new ArrayList<>();

    ArchivesDatabase database = ArchivesDatabase.getInstance();



    public void crawlArchiveLink(){
        for(int i = 0; i < archives.size(); i++){
            archiveDate = archives.get(i);
            Connection connection = Jsoup.connect(ARCHIVE_EXCHANGES_URL + "-" + archiveDate);
            try{
                Document archivePage = connection.get();
                Elements pageElements = archivePage.select("*");
                for(Element element : pageElements){
                    extractCurrencyInfoLineData(element.text(),archives.get(i).toString());
                }
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private void extractCurrencyInfoLineData(String line, String date){
        String[] separatedValues;

        String currencyAbbreviation = "";
        String currencyName = "";
        double currencyValue = 0;
        double currencyValueDifference = 0;


        if(line.matches("(100)?\\p{Upper}{3} .*")){
            separatedValues = line.split(" ");

            if(separatedValues.length >= 3 && separatedValues.length <= 7){
                currencyAbbreviation = separatedValues[0];
                currencyName = Arrays.toString(Arrays.stream(separatedValues).skip(1).limit(separatedValues.length - 3).toArray());
                currencyName = currencyName.replaceAll("[\\[\\],]","");
                currencyValue = Double.parseDouble(separatedValues[separatedValues.length - 2]);
                currencyValueDifference = Double.parseDouble(separatedValues[separatedValues.length - 1]);
                database.insertDataInTable(currencyAbbreviation,currencyAbbreviation,currencyName,currencyValue,currencyValueDifference,date);
            }
        }
    }


    public void downloadAllNewArchives(){
        //Gets all archives(date from links) from the last downloaded to present;
        Elements allUrlsFromArchive = mainPage.select("a[href]");
        List<LocalDate> newArchiveDates = new ArrayList<>();
        lastArchiveDate = LocalDate.parse(getLastArchiveFromFile());

        for(int i = allUrlsFromArchive.size() - 1; i >= 0; i--){
            String archiveUrl = allUrlsFromArchive.get(i).absUrl("href");
            if(archiveUrl.matches(".*/arhiva-curs-bnr-\\d{4}-\\d{2}-\\d{2}")){
                String date = archiveUrl.replaceAll(".*-(\\d{4}-\\d{2}-\\d{2})","$1");
                archiveDate = LocalDate.parse(date);
                if(lastArchiveDate.isBefore(archiveDate)){
                    newArchiveDates.add(archiveDate);
                }
            }
        }
        newArchiveDates.sort(new ArchivesSorter());
        archives.addAll(newArchiveDates);

        if(archives.size() > 0){
            lastArchiveDate = archives.get(archives.size() - 1);
            writeLastArchiveToFile(lastArchiveDate);
        }
    }


    public void downloadAllArchiveDates(){
        Elements allUrlsFromArchive = mainPage.select("a[href]");

        for(int i = allUrlsFromArchive.size() - 1; i >= 0; i--){
            String archiveUrl = allUrlsFromArchive.get(i).absUrl("href");
            if(archiveUrl.matches(".*/arhiva-curs-bnr-\\d{4}-\\d{2}-\\d{2}")){
                String date = archiveUrl.replaceAll(".*-(\\d{4}-\\d{2}-\\d{2})","$1");
                archiveDate = LocalDate.parse(date);
                archives.add(archiveDate);
            }
        }

        archives.sort(new ArchivesSorter());
        lastArchiveDate = archives.get(archives.size() - 1);
        writeLastArchiveToFile(lastArchiveDate);
    }

    public String getLastArchiveFromFile(){
        String line;
        try(BufferedReader reader = new BufferedReader(new FileReader(lastArchiveFileUrl))){
            if((line = reader.readLine()) != null){
                return line;
            }
        }
        catch (IOException readerException){
            System.out.println(readerException.getMessage());
        }
        return null;
    }

    private void writeLastArchiveToFile(LocalDate lastDate){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(lastArchiveFileUrl))){
            writer.write(lastDate.toString());
        }
        catch (IOException writerException){
            System.out.println(writerException.getMessage());
        }
    }

    public int connectToMainPage(){
        int responseCode = 0;
        try {
            connection = Jsoup.connect(ARCHIVE_EXCHANGES_URL);
            mainPage = connection.get();
            responseCode = connection.response().statusCode();
        }
        catch (IOException connectionException){
            System.out.println(connectionException.getMessage());
        }
        return responseCode;
    }

    private class ArchivesSorter implements Comparator<LocalDate>{

        public int compare(LocalDate date1, LocalDate date2){
            if(date1.getYear() == date2.getYear() &&
               date1.getMonthValue() == date2.getMonthValue()){
                if(date1.getDayOfMonth() < date2.getDayOfMonth()){
                    return -1;
                }
                else if(date1.getDayOfMonth() > date2.getDayOfMonth()){
                    return 1;
                }
            }
            return 0;
        }
    }


    //Just for testing
    public void removeLastNDates(int lastDates){
        for(int i = 0; i < lastDates; i++){
            archives.remove(ArchiveCrawler.archives.size() - 1);
        }
        lastArchiveDate = archives.get(archives.size() - 1);
        writeLastArchiveToFile(lastArchiveDate);
    }

    //Just for testing
    public void printAllDates(DateFormatTypes dateFormatType){
        for(LocalDate date : archives){
            System.out.println(date.format(dateFormatType.dateFormat));
        }
        System.out.println();
        //System.out.println("Last date:" + lastArchiveDate);
    }

}
