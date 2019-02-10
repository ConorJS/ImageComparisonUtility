package filehandling;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileHandlerUtil {

    /**
     * Get all files
     *
     * @param path  Root to begin searching from
     * @param recurse Flag to check all subdirectories (and their subdirectories, i.e recursive checking)
     * @return
     */
    public static List<File> getAllFiles(String path, boolean recurse){
        File[] files = new File(path).listFiles();

        List<File> fileList = Arrays.asList(files).stream()
                .flatMap(file -> {

                    List<File> subDirectoryFiles = new ArrayList<>(); // also will store tree leaves (files) in 1-size array

                    if (file.isDirectory() && recurse) {
                        subDirectoryFiles.addAll(getAllFiles(file.getPath(), true));
                    } else {
                        subDirectoryFiles.add(file);
                    }

                    return subDirectoryFiles.stream();
                })
                .collect(Collectors.toList());

        return fileList;
    }

    public static String readFileToString(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        StringBuilder stringBuilder = new StringBuilder();

        bufferedReader.lines().forEach(line -> {
                stringBuilder.append(line).append("\r\n");
        });

        return stringBuilder.toString();
    }

    public static void writeLinesToFile(File file, List<String> lines) throws IOException {
        FileWriter fw = new FileWriter(file);

        for (String line : lines) {
            fw.write(line + "\r\n");
        }

        fw.close();
    }

}
