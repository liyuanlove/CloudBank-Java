package com.cloudcoin.bank.utils;

import com.cloudcoin.bank.core.CloudCoin;
import com.cloudcoin.bank.core.Stack;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

public class FileUtils {


    /* Fields */

    private static Random random = new Random();
    private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


    /* Methods */

    /**
     * Appends a filename with an increasing index if a filename is in use. Loops until a free filename is found.
     * TODO: Potential endless loop if every filename is taken.
     *
     * @param filename
     * @return an unused filename
     */
    public static String ensureFilepathUnique(String filename, String extension, String folder) {
        filename = filename.replace(extension, "");
        if (!Files.exists(Paths.get(folder + filename + extension)))
            return folder + filename + extension;

        filename = filename + '.';
        String newFilename;
        int loopCount = 0;
        do {
            newFilename = filename + Integer.toString(++loopCount);
        }
        while (Files.exists(Paths.get(folder + newFilename + extension)));
        return folder + newFilename + extension;
    }

    public static String randomString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    /**
     * Loads an array of CloudCoins from a Stack file.
     *
     * @param fullFilePath the absolute filepath of the Stack file.
     * @return ArrayList of CloudCoins.
     */
    public static ArrayList<CloudCoin> loadCloudCoinsFromStack(String fullFilePath) {
        try {
            String file = new String(Files.readAllBytes(Paths.get(fullFilePath)));
            Stack stack = Utils.createGson().fromJson(file, Stack.class);
            for (CloudCoin coin : stack.cc)
                coin.setFullFilePath(fullFilePath);
            return new ArrayList<>(Arrays.asList(stack.cc));
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Loads an array of CloudCoins from a Stack JSON String.
     *
     * @param json the JSON string of the Stack file.
     * @return ArrayList of CloudCoins.
     */
    public static ArrayList<CloudCoin> loadCloudCoinsFromStackJson(String json) {
        try {
            Stack stack = Utils.createGson().fromJson(json, Stack.class);
            return new ArrayList<>(Arrays.asList(stack.cc));
        } catch (JsonSyntaxException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns an array containing all filenames in a directory.
     *
     * @param folderPath the folder to check for files.
     * @return String Array.
     */
    public static String[] selectFileNamesInFolder(String folderPath) {
        File folder = new File(folderPath);
        Collection<String> files = new ArrayList<>();
        if (folder.isDirectory()) {
            File[] filenames = folder.listFiles();

            if (null != filenames) {
                for (File file : filenames) {
                    if (file.isFile()) {
                        files.add(file.getName());
                    }
                }
            }
        }
        return files.toArray(new String[]{});
    }
}
