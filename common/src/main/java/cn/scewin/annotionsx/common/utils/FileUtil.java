package cn.scewin.annotionsx.common.utils;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

    public static String readFileContent(String path) {
        File file = new File(path);
        StringBuilder builder = new StringBuilder();
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            bufferedReader.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static String saveToFile(Object object, String path) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        File file = new File(path);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(json);
        fileWriter.flush();
        fileWriter.close();
        return path;
    }


}
