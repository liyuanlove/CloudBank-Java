package com.cloudcoin.bank.core;

import com.cloudcoin.bank.utils.CoinUtils;
import com.cloudcoin.bank.utils.FileUtils;
import com.cloudcoin.bank.utils.Utils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class FileSystem {


    /* Fields */

    public String RootPath;

    public String DetectedFolder;
    public String SuspectFolder;
    public String ExportFolder;

    public String CounterfeitFolder;
    public String BankFolder;
    public String FrackedFolder;
    public String LostFolder;

    public String TemplateFolder;
    public String LogsFolder;

    public static ArrayList<CloudCoin> importCoins;
    public static ArrayList<CloudCoin> predetectCoins;

    public static ArrayList<CloudCoin> bankCoins;
    public static ArrayList<CloudCoin> frackedCoins;


    /* Constructor */

    public FileSystem(String RootPath) {
        this.RootPath = RootPath;

        DetectedFolder = RootPath + File.separator + Config.TAG_DETECTED + File.separator;
        SuspectFolder = RootPath + File.separator + Config.TAG_SUSPECT + File.separator;

        BankFolder = RootPath + File.separator + Config.TAG_BANK + File.separator;
        CounterfeitFolder = RootPath + File.separator + Config.TAG_COUNTERFEIT + File.separator;
        FrackedFolder = RootPath + File.separator + Config.TAG_FRACKED + File.separator;

        LogsFolder = RootPath + File.separator + Config.TAG_LOGS + File.separator;

        LostFolder = RootPath + File.separator + Config.TAG_LOST + File.separator;
        ExportFolder = RootPath + File.separator + Config.TAG_EXPORT + File.separator;
    }


    /* Methods */

    /**
     * Creates directories in the location defined by RootPath.
     *
     * @return true if all folders were created or already exist, otherwise false.
     */
    public boolean createDirectories() {
        try {
            Files.createDirectories(Paths.get(RootPath));

            Files.createDirectories(Paths.get(ExportFolder));
            Files.createDirectories(Paths.get(BankFolder));
            Files.createDirectories(Paths.get(FrackedFolder));
            Files.createDirectories(Paths.get(LogsFolder));
        } catch (Exception e) {
            System.out.println("FS#CD: " + e.getLocalizedMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }



    /**
     * Load all CloudCoins in a specific folder.
     *
     * @param folder the folder to search for CloudCoin files.
     * @return an ArrayList of all CloudCoins in the specified folder.
     */
    public ArrayList<CloudCoin> loadFolderCoins(String folder) {
        ArrayList<CloudCoin> folderCoins = new ArrayList<>();

        String[] filenames = FileUtils.selectFileNamesInFolder(folder);
        String extension;
        for (int i = 0, length = filenames.length; i < length; i++) {
            int index = filenames[i].lastIndexOf('.');
            if (index == -1) continue;

            extension = filenames[i].substring(index + 1);
            String fullFilePath = folder + filenames[i];

            switch (extension) {
                case "celeb":
                case "celebrium":
                case "stack":
                    ArrayList<CloudCoin> coins = FileUtils.loadCloudCoinsFromStack(fullFilePath);
                    folderCoins.addAll(coins);
                    break;
            }
        }

        return folderCoins;
    }

    /**
     * Deletes a file from a folder.
     *
     * @param folder     the folder to delete from.
     * @param filename   the file to delete.
     */
    public static void removeFile(String folder, String filename) {
        try {
            Files.deleteIfExists(Paths.get(folder + filename));
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes CloudCoins files from a specific folder.
     *
     * @param cloudCoins the ArrayList of CloudCoins to delete.
     * @param folder     the folder to delete from.
     */
    public void removeCoins(ArrayList<CloudCoin> cloudCoins, String folder) {
        for (CloudCoin coin : cloudCoins) {
            try {
                Files.deleteIfExists(Paths.get(folder + coin.getFullFilePath()));
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes an array of CloudCoins to a single Stack file.
     *
     * @param coins    the ArrayList of CloudCoins.
     * @param filePath the absolute filepath of the CloudCoin file, without the extension.
     */
    public void writeCoinsToSingleStack(ArrayList<CloudCoin> coins, String filePath) {
        Gson gson = Utils.createGson();
        try {
            Stack stack = new Stack(coins.toArray(new CloudCoin[0]));
            Files.write(Paths.get(filePath + ".stack"), gson.toJson(stack).getBytes());
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Writes a CloudCoins a Stack file.
     *
     * @param coin     the ArrayList of CloudCoins.
     * @param filePath the absolute filepath of the CloudCoin file, without the extension.
     */
    public void writeCoinToIndividualStacks(CloudCoin coin, String filePath) {
        Stack stack = new Stack(coin);
        try {
            Files.write(Paths.get(filePath + ".stack"), Utils.createGson().toJson(stack).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public CloudCoin loadCoin(String fileName) {
        ArrayList<CloudCoin> coins = FileUtils.loadCloudCoinsFromStack(fileName);

        if (coins != null)
            System.out.println("loaded coins: " + coins.size());
        else
            System.out.println("or not");
        if (coins != null && coins.size() > 0)
            return coins.get(0);
        return null;
    }

    public void overWrite(String folder, CloudCoin cc) {
        String json = Utils.createGson().toJson(cc);

        try {
            Files.write(Paths.get(cc.getFullFilePath() + ".stack"), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads to memory all of the CloudCoins in the Bank and Fracked folders.
     */
    public void loadFileSystem() {
        bankCoins = loadFolderCoins(BankFolder);
        frackedCoins = loadFolderCoins(FrackedFolder);
        //importCoins = loadFolderCoins(ImportFolder);
        //predetectCoins = loadFolderCoins(SuspectFolder);
    }

    public void detectPreProcessing() {
        for (CloudCoin coin : importCoins) {
            String fileName = coin.getFullFilePath();
            int coinExists = 0;
            for (CloudCoin folderCoin : predetectCoins)
                if (folderCoin.getSn() == coin.getSn())
                    coinExists++;
            //int coinExists = (int) Arrays.stream(predetectCoins.toArray(new CloudCoin[0])).filter(x -> x.getSn() == coin.getSn()).count();

            //if (coinExists > 0)
            //{
            //    String suffix = Utils.randomString(16);
            //    fileName += suffix.toLowerCase();
            //}

            Stack stack = new Stack(coin);
            try {
                Files.write(Paths.get(SuspectFolder + fileName + ".stack"), Utils.createGson().toJson(stack).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.out.println("FS#DPP: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public static File[] GetFilesArray(String path, String[] extensions) {
        final ArrayList<String> extensionsArray = new ArrayList<>(Arrays.asList(extensions));
        return new File(path).listFiles(pathname -> {
            String filename = pathname.getAbsolutePath();
            String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            return extensionsArray.contains(extension);
        });
    }

    public void moveCoin(CloudCoin coin, String sourceFolder, String targetFolder, boolean replaceCoins) {
        ArrayList<CloudCoin> folderCoins = loadFolderCoins(targetFolder);

        String fileName = (CoinUtils.generateFilename(coin));
        int coinExists = 0;
        for (CloudCoin folderCoin : folderCoins)
            if (folderCoin.getSn() == coin.getSn())
                coinExists++;
        //int coinExists = (int) Arrays.stream(folderCoins.toArray(new CloudCoin[0])).filter(x -> x.getSn() == coin.getSn()).count();

        if (coinExists > 0 && !replaceCoins) {
            String suffix = FileUtils.randomString(16);
            fileName += suffix.toLowerCase();
        }
        try {
            Gson gson = Utils.createGson();
            Stack stack = new Stack(coin);
            Files.write(Paths.get(targetFolder + fileName + ".stack"), gson.toJson(stack).getBytes(StandardCharsets.UTF_8));
            System.out.println("deleting " + sourceFolder + coin.currentFilename);
            Files.deleteIfExists(Paths.get(sourceFolder + coin.currentFilename));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Converts a byte array to a hexadecimal String.
     *
     * @param data the byte array.
     * @return a hexadecimal String.
     */
    public String bytesToHexString(byte[] data) {
        final String HexChart = "0123456789ABCDEF";
        final StringBuilder hex = new StringBuilder(data.length * 2);
        for (byte b : data)
            hex.append(HexChart.charAt((b & 0xF0) >> 4)).append(HexChart.charAt((b & 0x0F)));
        return hex.toString();
    }
}
