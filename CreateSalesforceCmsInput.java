import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import net.lingala.zip4j.ZipFile;

public class CreateSalesforceCmsInput {
    public static void main(String[] args) throws Exception {
        // TOOD: input file name

        String JsonFileName = "CMS_JSON" + ".json";
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator( new File(JsonFileName), JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();
        generator.writeFieldName("content");
        generator.writeStartArray();


        Reader in = new FileReader("CMSInput.csv"); //  Sette.csv
        Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
        int i = 0;
        for (CSVRecord record : records) {
            String productSKU = record.get(0); // Product SKU
            String productName = record.get(1); // Product Name
            String imageType = record.get(2); // Image Type
            String sortOrder = record.get(3); // Sort Order
            String url = record.get(4); // URL

            // TODO: Add basic inout validation
            String urlName = "";
            String longTime = String.valueOf(new Date().getTime());
            String title = productName;
            String titleAppendix = "";

            if (i > 0) {
                System.out.println(i + "\t" + productName);
                if (imageType.equalsIgnoreCase("List")) {
                    sortOrder = "0";

                } else if (imageType.equalsIgnoreCase("Detail")) {
                    titleAppendix = " Detail " + sortOrder;
                } else {
                    // TODO: Throw exception
                }
                urlName  = longTime + "-productimage-" + productSKU.toLowerCase() + "-sku" + "-order-" + sortOrder + "";

                generator.writeStartObject(); // outer
                    generator.writeStringField("urlName", urlName);
                    generator.writeStringField("type", "cms_image");

                    generator.writeFieldName("body");
                    generator.writeStartObject(); // body
                        generator.writeStringField("altText", title);
                            generator.writeFieldName("source");
                                generator.writeStartObject(); // source
                                    generator.writeStringField("url", url);
                                generator.writeEndObject(); // source
                                generator.writeStringField("title", title + titleAppendix);
                    generator.writeEndObject(); // body
                generator.writeEndObject(); // outer
            }
            i++;
        } // records

        generator.writeEndArray(); // content array
        generator.writeEndObject(); // to close the generator
        generator.close();

        ZipFile zipFile = new ZipFile("CMS_JSON.zip");
        zipFile.addFile("CMS_JSON.json");
        zipFile.close();

    }
}
