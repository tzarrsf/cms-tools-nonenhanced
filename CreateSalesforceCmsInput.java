import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import org.apache.commons.io.FileUtils;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import net.lingala.zip4j.ZipFile;

public class CreateSalesforceCmsInput {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option allFiles = new Option("allfiles", "include all files");
        options.addOption(allFiles);


// Images
        Option createCmsImageTemplate = Option.builder()
            .longOpt("createCmsImageTemplate")
            .argName("directory").hasArg().desc("use directory name.").build();
        options.addOption(createCmsImageTemplate);

        Option createCmsImageJsonInput = Option.builder()
            .longOpt("createCmsImageJsonInput")
            .argName("directory").hasArg().desc("use directory name.").build();
        options.addOption(createCmsImageJsonInput);

        OptionGroup og = new OptionGroup();
        og.addOption(createCmsImageTemplate);
        og.addOption(createCmsImageJsonInput);


        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        boolean allFilesflag = false;

        if (cmd.hasOption("createCmsImageTemplate")) {
            System.out.println("Option: " + cmd.getOptionValue("createCmsImageTemplate"));
            checkFileExists(cmd.getOptionValue("createCmsImageTemplate"));
            createCmsImageTemplate(cmd.getOptionValue("createCmsImageTemplate"));

        } else if (cmd.hasOption("createCmsImageJsonInput")) {
            System.out.println("Option: " + cmd.getOptionValue("createCmsImageJsonInput"));
            checkFileExists(cmd.getOptionValue("createCmsImageJsonInput"));
            if (cmd.hasOption("allfiles")) {
                allFilesflag = true;
            }
            createCmsImageJsonInput(cmd.getOptionValue("createCmsImageJsonInput"), allFilesflag);

        } else {
            formatter.printHelp("[program name]", options);
        }
    }




    public static void createCmsImageTemplate(String directory) throws Exception {
        String mediaFolderName = directory + "/_media";
        Path mediaDirectoryPath = Paths.get(mediaFolderName);
        File mediaFolder = new File(mediaFolderName);

        if (Files.exists(mediaDirectoryPath)) {
            FileUtils.deleteDirectory(mediaFolder);
        }
        Files.createDirectories(mediaDirectoryPath);

        File inputDirectory = new File(directory);
        List<File> files = (List<File>) FileUtils.listFiles(inputDirectory, new String[] { "png", "PNG", "jpg", "jpeg", "JPG", "JPEG" }, true);
        for (File file : files) {
            copyFileToDirectory(file, mediaFolder);
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(" CMS Images");
        sheet.protectSheet("password");
        XSSFRow row;

        CellStyle boldStyle = createBoldStyle(workbook);
        boldStyle.setLocked(true);

        CellStyle textStyle = createTextStyle(workbook);

        CellStyle textStyleUnlocked = createTextStyle(workbook);
        textStyleUnlocked.setLocked(false);



        int rowid = 0;
        row = sheet.createRow(rowid++);
        Cell cell = null;
        // List<String> headings = List.of("Filename", "Product SKU");
        List<String> headings = List.of("Filename", "Product SKU", "Sort Order");
        for (int i = 0; i < headings.size(); i++) {
            cell = row.createCell(i);
            cell.setCellStyle(boldStyle);
            cell.setCellValue(headings.get(i));
        }

        File filesList[] = mediaFolder.listFiles();
        Arrays.sort(filesList);

        for(File file : filesList) {
            row = sheet.createRow(rowid++);

            cell = row.createCell(0);
            cell.setCellStyle(textStyle);
            cell.setCellValue(file.getName());

            cell = row.createCell(1);
            cell.setCellStyle(textStyleUnlocked);
            cell.setCellValue("");

            cell = row.createCell(2);
            cell.setCellStyle(textStyleUnlocked);
            cell.setCellValue("");

        }

        sheet.setZoom(120);
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }

        String fileName = directory + "/" + inputDirectory.getName() + ".xlsx";
        FileOutputStream out = new FileOutputStream(new File(fileName));
        workbook.write(out);
        workbook.close();
        out.close();
        System.out.println("\n\n" + fileName + " written successfully");
    }



    static void createCmsImageJsonInput(String directory, boolean allFiles) throws Exception {
        File inputDirectory = new File(directory);
        String fileName = directory + "/" + inputDirectory.getName() + ".xlsx";
        List<String> fileRemoveList =new ArrayList<String>();

        Set<String> SkuSet = new HashSet<String>();

        String JsonFileName = directory + "/" + inputDirectory.getName() + ".json";
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator( new File(JsonFileName), JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();

        generator.writeFieldName("content");
        generator.writeStartArray();

        FileInputStream file = new FileInputStream(new File(fileName));
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);

        String sortOrder = "";

        int rowIndex = 0;
        for (Row row: sheet) {
            if (rowIndex > 0 && row.getCell(0) != null) {
                String fileNameValue = row.getCell(0).getStringCellValue();
                Cell productIdCell = row.getCell(1);

                String productIdentifier = null;
                if (productIdCell.getCellType() == CellType.NUMERIC) {
                    productIdentifier =  String.valueOf((int) productIdCell.getNumericCellValue());
                } else if (productIdCell.getCellType() == CellType.STRING) {
                    productIdentifier = productIdCell.getStringCellValue();
                } else {
                    productIdentifier = "";
                }


                Cell sortOrderCell = row.getCell(2);

                if (sortOrderCell.getCellType() == CellType.NUMERIC) {
                    // when pasting into spreadsheet, Excel automatically converts cell to numeric
                    sortOrder =  String.valueOf((int) sortOrderCell.getNumericCellValue());
                } else if (sortOrderCell.getCellType() == CellType.STRING) {
                    sortOrder = sortOrderCell.getStringCellValue();
                } else {
                    sortOrder = null;
                }

                if (SkuSet.contains(productIdentifier)) {
                //   System.out.println("\n\nNote ~ Potential Error: Product SKU " + productIdentifier + " already exists on the spreadsheet");
                //    System.exit(1);
                } else {
                    if(productIdentifier.length() > 0) {
                        SkuSet.add(productIdentifier);
                    }
                }

                checkFileExists(directory + "/_media/" + fileNameValue);
                if(productIdentifier.length() > 0 || allFiles) {
                    String longTime = String.valueOf(new Date().getTime());
                    String urlName  = longTime + "-productimage-" + productIdentifier.toLowerCase() + "-sku" + "-order-" + sortOrder + "";

                    if (productIdentifier.length() == 0) {
                        urlName  = longTime + "-allfiles";
                    }

                    String title = fileNameValue.substring(0, fileNameValue.lastIndexOf('.')).replace("-", "_").replace("'", "").trim();
                    System.out.print("+++ " + rowIndex + " " + fileNameValue);

                    generator.writeStartObject(); // outer
                        generator.writeStringField("urlName", urlName);
                        generator.writeStringField("type", "cms_image");

                        generator.writeFieldName("body");
                        generator.writeStartObject(); // body
                                generator.writeFieldName("source");
                                    generator.writeStartObject(); // source
                                        generator.writeStringField("ref", fileNameValue); // file name5
                                    generator.writeEndObject(); // source
                                    generator.writeStringField("title", title);
                        generator.writeEndObject(); // body
                    generator.writeEndObject(); // outer
                } else {
                    // if a product has not product Identifier then it will not be included in te zip file
                    fileRemoveList.add("_media/" + fileNameValue);
                    System.out.println("Removing: " + fileNameValue);
                }

            System.out.println(fileNameValue + "\t\t" + productIdentifier);
            }
            rowIndex++;
        } // for row
        workbook.close();

        generator.writeEndArray(); // content array
        generator.writeEndObject(); // to close the generator
        generator.close();

        ZipFile zipFile = new ZipFile(directory + "/" + inputDirectory.getName() + ".zip");
        if (allFiles) {
            zipFile = new ZipFile(directory + "/" + inputDirectory.getName() + "-AllFiles.zip");
        }
        zipFile.addFile(new File(JsonFileName));
        zipFile.addFolder(new File(directory + "/_media"));

        if(fileRemoveList.size() > 0) {
            zipFile.removeFiles(fileRemoveList);
        }
        zipFile.close();
    }



    static void checkFileExists(String directory) {
        Path path = Paths.get(directory);
        if (Files.notExists(path)) {
            System.out.println("\n\nERROR: File/Directory does not exist: " + directory);
            System.exit(1);
        }
    }


    public static CellStyle createBoldStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat fmt = workbook.createDataFormat();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short)12);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setDataFormat(fmt.getFormat("@"));
        return style;
    }


    public static CellStyle createTextStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(createBoldStyle(workbook));
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short)12);
        style.setFont(font);
        return style;
    }
}