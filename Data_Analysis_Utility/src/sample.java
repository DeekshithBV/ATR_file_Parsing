import java.io.*;
import java.util.Objects;
import java.util.regex.*;

public class sample {

    public static void main(String[] args) {
        String inputFolderPath = "Java Assignment/test";
        String outputPassFailCSV = "passfail.csv";
        String outputFailDetailsCSV = "test_fail_details.csv";

        try (PrintWriter passFailWriter = new PrintWriter(new FileWriter(outputPassFailCSV));
             PrintWriter failDetailsWriter = new PrintWriter(new FileWriter(outputFailDetailsCSV))) {

            passFailWriter.println("File name, test case Id, status, remarks");
            failDetailsWriter.println("File name, stub_errors, script_errors, failures");

            File folder = new File(inputFolderPath);
            File[] listOfFiles = folder.listFiles();

            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile() && file.getName().endsWith(".atr")) {
                        processATRFile(file, passFailWriter, failDetailsWriter);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processATRFile(File file, PrintWriter passFailWriter, PrintWriter failDetailsWriter)
            throws IOException {
        String fileName = file.getName();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int stubErrors = 0;
            int scriptErrors = 0;
            int failures = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("START_COVERAGE: TRACE_FULL ;")) {
                    processTestCases(reader, fileName, passFailWriter,failDetailsWriter);
                }
                if (line.trim().contains("Tests with Stub Failures")) {
                    System.out.println("value :"+line);
                    stubErrors = extractErrorCount(line);
                    System.out.println("value :"+line);
                }
                if (line.trim().contains("Script Errors")) {
                    System.out.println("value :"+line);
                    scriptErrors = extractErrorCount(line);
                    System.out.println("value :"+line);
                }
                if (line.trim().contains("Failed")) {
                    failures = extractErrorCount(line);
                }
            }
            failDetailsWriter.println(fileName + "," + (stubErrors > 0 ? "yes" : "no") + "," + (scriptErrors > 0 ? "yes" : "no") + "," + (failures > 0 ? "yes" : "no"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processTestCases(BufferedReader reader, String fileName, PrintWriter passFailWriter,PrintWriter failDetailsWriter)
            throws IOException {
        String line;

//        int count = 0;
        while ((line = reader.readLine()) != null) {
            //LCR-110_SW_LLR_TC_(\\w+)
            Matcher testCaseMatcher = Pattern.compile("LCR.*").matcher(line);
            if (testCaseMatcher.find()) {
                String testCaseId = testCaseMatcher.group(0);
                String status = "PASSED";
                String remarks = "";
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    String nextline = "";
                    //if (line.matches(".*END TEST (\\w+).*"))
                    if (line.trim().contains("END TEST")) {
                        break;
                        //TODO
                        //else if (line.contains("CHECK ("))
                    } else if (line.replaceAll(" ","").contains("CHECK(")) {
                        //(line.contains("FAILED"))
                        nextline = reader.readLine();
                        if (line.toUpperCase().contains("FAILED") && nextline.replaceAll("\\s","").toUpperCase().contains("FAILED")) {
                            status = "FAILED";
                            remarks = "Multiple test cases are failed. Manual analysis is required";
                            break;
                        }
                        if (line.toUpperCase().contains("FAILED")) {
                            status = "FAILED";
                            count++;
                            if (count>1) {
                                remarks = "Multiple test cases are failed. Manual analysis is required";
                                break;
                            }
                        }
                        nextline = line+nextline;
                        if (nextline.replaceAll("\\s","").toUpperCase().contains("FAILED")) {
                            status = "FAILED";
                            count++;
                            if (count>1) {
                                remarks = "Multiple test cases are failed. Manual analysis is required";
                                break;
                            }
                        }
                        else if (!(line.toUpperCase().contains("PASSED")) && !(nextline.replaceAll("\\s","").toUpperCase().contains("PASSED"))){
                            status = "UNKNOWN";
                            break;
                        }
                    }
                    else if (line.contains("STOP_COVERAGE")){
                        break;
                    }
                }
                passFailWriter.println(fileName + "," + testCaseId + "," + status + "," + remarks);
            }
            if (Objects.requireNonNull(line).contains("STOP_COVERAGE")){
                break;
            }
        }
    }

    private static int extractErrorCount(String line) {
        return Integer.parseInt(line.split(":")[1].trim());
    }
}