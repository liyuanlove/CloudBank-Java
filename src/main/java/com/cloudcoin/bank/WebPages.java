package com.cloudcoin.bank;

import com.cloudcoin.bank.core.*;
import com.cloudcoin.bank.json.ServiceResponse;
import com.cloudcoin.bank.utils.CoinUtils;
import com.cloudcoin.bank.utils.FileUtils;
import com.cloudcoin.bank.utils.Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class WebPages {


    /* Web Pages */

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping("/print_welcome")
    public String print_welcome() {
        ServiceResponse response = new ServiceResponse();

        response.bankServer = "localhost";
        response.status = "welcome";
        response.message = "CloudCoin Bank. Used to Authenticate, Store and Payout CloudCoins." +
                "This Software is provided as is with all faults, defects and errors, and without warranty of any kind." +
                "Free from the CloudCoin Consortium.";
        return Utils.createGson().toJson(response);
    }

    @RequestMapping(value = "/echo", method = RequestMethod.GET)
    public String echo(@RequestParam(required = false, value = "account") String account,
                       @RequestParam(required = false, value = "pk") String key) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        return EchoRaida();
    }

    @RequestMapping(value = "/show_coins", method = RequestMethod.GET)
    public String show_coins(@RequestParam(required = false, value = "account") String account,
                             @RequestParam(required = false, value = "pk") String key) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        String accountFolder = FileSystem.AccountFolder + key;
        int[] bankTotals = Banker.countCoins(accountFolder + FileSystem.BankPath);
        int[] frackedTotals = Banker.countCoins(accountFolder + FileSystem.FrackedPath);

        response.ones = bankTotals[1] + frackedTotals[1];
        response.fives = bankTotals[2] + frackedTotals[2];
        response.twentyfives = bankTotals[3] + frackedTotals[3];
        response.hundreds = bankTotals[4] + frackedTotals[4];
        response.twohundredfifties = bankTotals[5] + frackedTotals[5];
        response.status = "coins_shown";
        response.message = "Coin totals returned.";

        return Utils.createGson().toJson(response);
    }

    @RequestMapping(value = "/deposit_one_stack", method = {RequestMethod.POST, RequestMethod.GET})
    public String depositStack(@RequestParam(required = false, value = "account") String account,
                               @RequestParam(required = false, value = "pk") String key,
                               @RequestParam(required = false, value = "stack") String stack) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        if (stack == null || stack.length() == 0) {
            response.message = "The CloudCoin stack was empty or not included in the post.";
            response.status = "error";
            response.receipt = "";
            return Utils.createGson().toJson(response);
        }

        ArrayList<CloudCoin> coins = FileUtils.loadCloudCoinsFromStackJson(stack);
        byte[] stackBytes = stack.getBytes(StandardCharsets.UTF_8);
        String accountFolder = FileSystem.AccountFolder + key;

        try {
            if (coins == null || coins.size() == 0)
                Files.write(Paths.get(accountFolder + FileSystem.TrashPath + new Date().toString()), stackBytes);
            else {
                String filename = CoinUtils.generateFilename(coins.get(0));
                filename = FileUtils.ensureFilenameUnique(filename, ".stack", accountFolder + FileSystem.SuspectPath);
                FileSystem.writeCoinsToSingleStack(coins, filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
            coins = null;
        }

        if (coins == null || coins.size() == 0) {
            response.status = "error";
            response.message = "Error: coins were valid.";
            response.receipt = "";
            return Utils.createGson().toJson(response);
        }

        String detectResponse = detect(accountFolder);
        if (detectResponse != null)
            return detectResponse;

        response.message = "There was a server error, try again later.";
        response.status = "error";
        return Utils.createGson().toJson(response);
    }

    @RequestMapping(value = "/withdraw_one_stack", method = {RequestMethod.POST, RequestMethod.GET})
    public String withdrawStack(@RequestParam(required = false, value = "account") String account,
                                @RequestParam(required = false, value = "pk") String key,
                                @RequestParam(required = false, value = "amount") String amountInput) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        int amount;
        try {
            amount = Integer.parseInt(amountInput);
        } catch (NumberFormatException ex) {
            response.message = "Request Error: Amount of CloudCoins is invalid.";
            return Utils.createGson().toJson(response);
        }
        if (amount == 0) {
            response.message = "Request Error: Amount of CloudCoins is invalid.";
            return Utils.createGson().toJson(response);
        }

        String accountFolder = FileSystem.AccountFolder + key;

        int[] bankTotals = Banker.countCoins(accountFolder + FileSystem.BankPath);
        int[] frackedTotals = Banker.countCoins(accountFolder + FileSystem.FrackedPath);
        int[] totals = Banker.countCoins(accountFolder);
        bankTotals[0] += totals[0];
        bankTotals[1] += totals[1];
        bankTotals[2] += totals[2];
        bankTotals[3] += totals[3];
        bankTotals[4] += totals[4];
        bankTotals[5] += totals[5];
        int AvailableCoins = ((bankTotals[5] + frackedTotals[5]) * 250) + ((bankTotals[4] + frackedTotals[4]) * 100) + ((bankTotals[3] + frackedTotals[3]) * 25) + ((bankTotals[2] + frackedTotals[2]) * 5) + ((bankTotals[1] + frackedTotals[1]) * 1);
        if (amount > AvailableCoins) {
            response.message = "Request Error: Amount of CloudCoins not available ";
            return Utils.createGson().toJson(response);
        }

        return exportStack(amount, accountFolder, accountFolder + FileSystem.ExportPath, response);
    }

    @RequestMapping(value = "/get_receipt", method = {RequestMethod.POST, RequestMethod.GET})
    public String getReceipt(@RequestParam(required = false, value = "account") String account,
                             @RequestParam(required = false, value = "rn") String receiptId) {
        String accountResponse = isAccountValid(account);
        if ('{' == accountResponse.charAt(0))
            return accountResponse;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        if (receiptId == null) {
            response.message = "Request Error: Receipt Number or Account ID not specified";
            return Utils.createGson().toJson(response);
        }

        try {
            Path receiptPath = Paths.get(FileSystem.AccountFolder + accountResponse + FileSystem.ReceiptsPath + receiptId + ".json");
            if (!Files.exists(receiptPath)) {
                response.message = "Receipt not correct.";
                response.account = account;
                return Utils.createGson().toJson(response);
            }

            return new String(Files.readAllBytes(receiptPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            response.message = "Request Error: Private key or Account not found.";
            e.printStackTrace();
            return Utils.createGson().toJson(response);
        }
    }

    @RequestMapping(value = "/write_check", method = {RequestMethod.POST, RequestMethod.GET})
    public String writeCheck(@RequestParam(required = false, value = "account") String account,
                             @RequestParam(required = false, value = "pk") String key,
                             @RequestParam(required = false, value = "amount") String amountInput,
                             @RequestParam(required = false, value = "action") String action,
                             @RequestParam(required = false, value = "emailto") String emailTarget,
                             @RequestParam(required = false, value = "payto") String payTarget,
                             @RequestParam(required = false, value = "fromemail") String emailSender,
                             @RequestParam(required = false, value = "signby") String signBy,
                             @RequestParam(required = false, value = "memo") String memo,
                             @RequestParam(required = false, value = "othercontactinfo") String otherContactInfo) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        int amount = 0;
        try {
            amount = Integer.parseInt(amountInput);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        } finally {
            if (amount == 0) {
                response.message = "Request Error: Amount of CloudCoins is invalid.";
                return Utils.createGson().toJson(response);
            }
        }

        if (!("url".equals(action) || "email".equals(action))) {
            response.message = "Request Error: No action specified";
            return Utils.createGson().toJson(response);
        }

        String accountFolder = FileSystem.AccountFolder + key;
        String checkId = FileUtils.randomString(16);

        int[] bankTotals = Banker.countCoins(accountFolder + FileSystem.BankPath);
        int[] frackedTotals = Banker.countCoins(accountFolder + FileSystem.FrackedPath);
        if (bankTotals[0] + frackedTotals[0] < amount) {
            response.status = "error";
            response.message = "Not enough funds to write Check for " + amount + " CloudCoins";
            return Utils.createGson().toJson(response);
        }

        if ("email".equals(action)) {
            if (!isParameterValid(emailTarget)) {
                response.status = "error";
                response.message = "Email to send check to not specified";
                return Utils.createGson().toJson(response);
            }
            if (!isParameterValid(emailSender)) {
                response.status = "error";
                response.message = "Your email address not specified";
                return Utils.createGson().toJson(response);
            }
        }

        String checkFullFilepath = /*"https://"*/ FileSystem.ChecksFolder + checkId + ".html";
        String CheckHtml = "<html><body><h1>" + getParameter(signBy) + "</h1><email>" + getParameter(emailSender) +
                "</email><h2>PAYTO THE ORDER OF: " + getParameter(emailTarget) + "</h2><h2>AMOUNT: " + amount +
                " CloudCoins</h2>" + "<a href='" + /*"https://"*/ "file://" + FileSystem.ChecksFolder + File.separator +
                checkId + "/index.html" + "'>Cash Check Now</a></body></html>";

        try {
            Files.write(Paths.get(FileSystem.ChecksFolder + checkId + "/index.html"), CheckHtml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ("email".equals(action)) {
            // Need email details to initialize
            //response.status = "Email sent";
            //response.message = "Check Emailed to " + emailTarget + " in the amount of " + amount + " CloudCoins.";
            response.status = "error";
            response.message = "Email option is not yet supported.";
            return Utils.createGson().toJson(response);
        } else {
            response.message = /*"https:/" +*/ "localhost:8080/cash_checks?id=" + checkId;
            response.status = "url";
        }

        //create check's stack file. TODO: This should run before the check is created, otherwise duplicate checks could be created without the stack being created.
        int AvailableCoins = ((bankTotals[5] + frackedTotals[5]) * 250) + ((bankTotals[4] + frackedTotals[4]) * 100) + ((bankTotals[3] + frackedTotals[3]) * 25) + ((bankTotals[2] + frackedTotals[2]) * 5) + ((bankTotals[1] + frackedTotals[1]) * 1);
        if (amount > AvailableCoins) {
            response.message = "Request Error: Amount of CloudCoins not available ";
            response.status = "error";
            return Utils.createGson().toJson(response);
        }

        String stackResponse = exportStack(amount, accountFolder, FileSystem.ChecksFolder, response);
        if (stackResponse.contains("Request Error:"))
            return stackResponse;

        response.message = "Request Error: Could not withdraw CloudCoins.";
        return Utils.createGson().toJson(response);
    }

    @RequestMapping(value = "/cash_check", method = {RequestMethod.POST, RequestMethod.GET})
    public String cash_check(@RequestParam(required = false, value = "id") String checkId) {
        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        try {
            if (getParameter(checkId).length() == 0) {
                response.message = "Please supply a check ID.";
                return Utils.createGson().toJson(response);
            }

            String checkFolder = FileSystem.ChecksFolder + checkId + File.separator;
            String check = new String(Files.readAllBytes(Paths.get(checkFolder + "/index.html")), StandardCharsets.UTF_8);
            if (0 == check.length()) {
                response.message = "The check you requested was not found on the server. It may have been cashed, canceled or you have provided an id that is incorrect.";
                return Utils.createGson().toJson(response);
            }

            ArrayList<CloudCoin> coins = FileSystem.loadFolderCoins(checkFolder);
            int amount = CoinUtils.countCoins(coins);

            String checkResponse = exportStack(amount, checkFolder, null, response);
            if (!checkResponse.contains("Request Error"))
                Files.walk(Paths.get(checkFolder))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

            return checkResponse;
        } catch (IOException e) {
            e.printStackTrace();
            response.message = "The check you requested was not found on the server. It may have been cashed, canceled or you have provided an id that is incorrect.";
            return Utils.createGson().toJson(response);
        }
    }

    @RequestMapping(value = "/mark_coins_forsale", method = {RequestMethod.POST, RequestMethod.GET})
    public String mark_for_sale(@RequestParam(required = false, value = "account") String account,
                                @RequestParam(required = false, value = "pk") String key,
                                @RequestParam(required = false, value = "ones") String onesInput,
                                @RequestParam(required = false, value = "fives") String fivesInput,
                                @RequestParam(required = false, value = "twentyfives") String twentyFivesInput,
                                @RequestParam(required = false, value = "hundreds") String hundredsInput,
                                @RequestParam(required = false, value = "twohundredfifties") String twoHundredFiftiesInput) {
        String badLogin = isAccountAndPasswordValid(account, key);
        if (badLogin != null)
            return badLogin;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        String Ones = getParameter(onesInput);
        String Fives = getParameter(fivesInput);
        String TwentyFives = getParameter(twentyFivesInput);
        String Hundreds = getParameter(hundredsInput);
        String TwoHundredFifties = getParameter(twoHundredFiftiesInput);
        if (Ones.equals("") && Fives.equals("") && TwentyFives.equals("") && Hundreds.equals("") && TwoHundredFifties.equals("")) {
            response.status = "fail";
            response.message = "No coins to mark provided";
            return Utils.createGson().toJson(response);
        }
        int ones, fives, twentyFives, hundreds, twoHundredFifties;
        ones = Utils.tryParseInt(Ones);
        fives = Utils.tryParseInt(Fives);
        twentyFives = Utils.tryParseInt(TwentyFives);
        hundreds = Utils.tryParseInt(Hundreds);
        twoHundredFifties = Utils.tryParseInt(TwoHundredFifties);

        String accountFolder = FileSystem.AccountFolder + key;

        int[] saleCoins = GetCoinsCurrentlyMarkedForSale(accountFolder);
        if (Ones.length() != 0) {
            if (ones != saleCoins[0]) {
                if (ones < saleCoins[0])
                    UnMarkCoins("1", saleCoins[0] - ones, accountFolder);
                else
                    MarkCoins("1", ones - saleCoins[0], accountFolder);
            }
        }
        if (Fives.length() != 0) {
            if (fives != saleCoins[1]) {
                if (fives < saleCoins[1])
                    UnMarkCoins("5", saleCoins[1] - fives, accountFolder);
                else
                    MarkCoins("5", fives - saleCoins[1], accountFolder);
            }
        }
        if (TwentyFives.length() != 0) {
            if (twentyFives != saleCoins[2]) {
                if (twentyFives < saleCoins[2])
                    UnMarkCoins("25", saleCoins[2] - twentyFives, accountFolder);
                else
                    MarkCoins("25", twentyFives - saleCoins[2], accountFolder);
            }
        }
        if (Hundreds.length() != 0) {
            if (hundreds != saleCoins[3]) {
                if (hundreds < saleCoins[3])
                    UnMarkCoins("100", saleCoins[3] - hundreds, accountFolder);
                else
                    MarkCoins("100", hundreds - saleCoins[3], accountFolder);
            }
        }
        if (TwoHundredFifties.length() != 0) {
            if (twoHundredFifties != saleCoins[4]) {
                if (twoHundredFifties < saleCoins[4])
                    UnMarkCoins("250", saleCoins[4] - twoHundredFifties, accountFolder);
                else
                    MarkCoins("250", twoHundredFifties - saleCoins[4], accountFolder);
            }
        }

        response.status = "success";
        response.message = "Coins Marked for Sale";
        return Utils.createGson().toJson(response);
    }


    /* Methods */

    private String isAccountValid(String account) {
        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";
        try {
            if (account == null) {
                response.message = "Request Error: Private key or Account not specified.";
                return Utils.createGson().toJson(response);
            }

            String key = new String(Files.readAllBytes(Paths.get(FileSystem.PasswordFolder + account + ".txt")), StandardCharsets.UTF_8);
            if (0 == key.length()) {
                response.message = "Private key not correct.";
                response.account = account;
                return Utils.createGson().toJson(response);
            }
            return key;
        } catch (IOException e) {
            response.message = "Request Error: Private key or Account not found.";
            e.printStackTrace();
            return Utils.createGson().toJson(response);
        }
    }

    private String isAccountAndPasswordValid(String account, String key) {
        String accountResponse = isAccountValid(account);
        if ('{' == accountResponse.charAt(0))
            return accountResponse;

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        if (key == null) {
            response.message = "Request Error: Private key or Account not specified.";
            return Utils.createGson().toJson(response);
        }
        if (!key.equals(accountResponse)) {
            response.message = "Private key not correct.";
            response.account = account;
            return Utils.createGson().toJson(response);
        }

        return null;
    }

    private boolean isParameterValid(String param) {
        return (param != null && param.length() != 0);
    }

    String getParameter(String param) {
        return (null == param) ? "" : param;
    }

    private String EchoRaida() {
        RAIDA raida = RAIDA.getInstance();
        ArrayList<CompletableFuture<Response>> tasks = raida.getEchoTasks();
        try {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            System.out.println("RAIDA#PNC:" + e.getLocalizedMessage());
        }

        int readyCount = raida.getReadyCount();

        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";
        response.time = Utils.getDate();
        response.readyCount = Integer.toString(readyCount);
        response.notReadyCount = Integer.toString(raida.getNotReadyCount());
        if (readyCount > 20) {
            response.status = "ready";
            response.message = "The RAIDA is ready for counterfeit detection.";
        } else {
            response.status = "fail";
            response.message = "Not enough RAIDA servers can be contacted to import new coins.";
        }

        return Utils.createGson().toJson(response);
    }

    private String detect(String accountFolder) {
        ServiceResponse response = new ServiceResponse();
        response.bankServer = "localhost";

        response.receipt = multi_detect(accountFolder);
        Grader.gradeDetectedFolder(accountFolder);
        response.status = "importing";
        response.message = "The stack file has been imported and detection will begin automatically so long as they are not already in bank. Please check your receipt.";

        response.time = Utils.getDate();
        return Utils.createGson().toJson(response);
    }

    private static String multi_detect(String accountPath) {
        MultiDetect multiDetector = new MultiDetect();
        String receiptFileName = FileUtils.randomString(16);

        try {
            multiDetector.detectMulti(receiptFileName, accountPath).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return receiptFileName;
    }

    private String exportStack(int amount, String accountFolder, String targetFolder, ServiceResponse response) {
        int[] bankTotals = Banker.countCoins(accountFolder + FileSystem.BankPath);
        int[] frackedTotals = Banker.countCoins(accountFolder + FileSystem.FrackedPath);
        int[] totals = Banker.countCoins(accountFolder);
        bankTotals[0] += totals[0];
        bankTotals[1] += totals[1];
        bankTotals[2] += totals[2];
        bankTotals[3] += totals[3];
        bankTotals[4] += totals[4];
        bankTotals[5] += totals[5];

        int exp_1 = 0;
        int exp_5 = 0;
        int exp_25 = 0;
        int exp_100 = 0;
        int exp_250 = 0;
        if (amount >= 250 && bankTotals[5] + frackedTotals[5] > 0) {
            exp_250 = ((amount / 250) < (bankTotals[5] + frackedTotals[5])) ? (amount / 250) : (bankTotals[5] + frackedTotals[5]);
            amount -= (exp_250 * 250);
        }
        if (amount >= 100 && bankTotals[4] + frackedTotals[4] > 0) {
            exp_100 = ((amount / 100) < (bankTotals[4] + frackedTotals[4])) ? (amount / 100) : (bankTotals[4] + frackedTotals[4]);
            amount -= (exp_100 * 100);
        }
        if (amount >= 25 && bankTotals[3] + frackedTotals[3] > 0) {
            exp_25 = ((amount / 25) < (bankTotals[3] + frackedTotals[3])) ? (amount / 25) : (bankTotals[3] + frackedTotals[3]);
            amount -= (exp_25 * 25);
        }
        if (amount >= 5 && bankTotals[2] + frackedTotals[2] > 0) {
            exp_5 = ((amount / 5) < (bankTotals[2] + frackedTotals[2])) ? (amount / 5) : (bankTotals[2] + frackedTotals[2]);
            amount -= (exp_5 * 5);
        }
        if (amount >= 1 && bankTotals[1] + frackedTotals[1] > 0) {
            exp_1 = (amount < (bankTotals[1] + frackedTotals[1])) ? amount : (bankTotals[1] + frackedTotals[1]);
            amount -= (exp_1);
        }

        // Get the CloudCoins that will be used for the export.
        int totalSaved = exp_1 + (exp_5 * 5) + (exp_25 * 25) + (exp_100 * 100) + (exp_250 * 250);
        ArrayList<CloudCoin> totalCoins = FileSystem.loadFolderCoins(accountFolder + FileSystem.BankPath);
        totalCoins.addAll(FileSystem.loadFolderCoins(accountFolder + FileSystem.FrackedPath));
        totalCoins.addAll(FileSystem.loadFolderCoins(accountFolder));

        ArrayList<CloudCoin> onesToExport = new ArrayList<>();
        ArrayList<CloudCoin> fivesToExport = new ArrayList<>();
        ArrayList<CloudCoin> qtrToExport = new ArrayList<>();
        ArrayList<CloudCoin> hundredsToExport = new ArrayList<>();
        ArrayList<CloudCoin> twoFiftiesToExport = new ArrayList<>();

        for (int i = 0, totalCoinsSize = totalCoins.size(); i < totalCoinsSize; i++) {
            CloudCoin coin = totalCoins.get(i);
            int denomination = CoinUtils.getDenomination(coin);
            if (denomination == 1) {
                if (exp_1-- > 0) onesToExport.add(coin);
                else exp_1 = 0;
            } else if (denomination == 5) {
                if (exp_5-- > 0) fivesToExport.add(coin);
                else exp_5 = 0;
            } else if (denomination == 25) {
                if (exp_25-- > 0) qtrToExport.add(coin);
                else exp_25 = 0;
            } else if (denomination == 100) {
                if (exp_100-- > 0) hundredsToExport.add(coin);
                else exp_100 = 0;
            } else if (denomination == 250) {
                if (exp_250-- > 0) twoFiftiesToExport.add(coin);
                else exp_250 = 0;
            }
        }

        if (onesToExport.size() < exp_1 || fivesToExport.size() < exp_5 || qtrToExport.size() < exp_25
                || hundredsToExport.size() < exp_100 || twoFiftiesToExport.size() < exp_250) {
            response.message = "Request Error: Not enough CloudCoins for export.";
            return Utils.createGson().toJson(response);
        }

        ArrayList<CloudCoin> exportCoins = new ArrayList<>();
        exportCoins.addAll(onesToExport);
        exportCoins.addAll(fivesToExport);
        exportCoins.addAll(qtrToExport);
        exportCoins.addAll(hundredsToExport);
        exportCoins.addAll(twoFiftiesToExport);

        if (targetFolder == null) {
            return FileSystem.writeCoinsToSingleStack(exportCoins, null);
        } else {
            String filename = (totalSaved + ".CloudCoin");
            filename = FileUtils.ensureFilenameUnique(filename, ".stack", targetFolder);
            String stack = FileSystem.writeCoinsToSingleStack(exportCoins, filename);
            if (stack != null) {
                FileSystem.removeCoins(exportCoins, accountFolder + FileSystem.BankPath);
                FileSystem.removeCoins(exportCoins, accountFolder + FileSystem.FrackedPath);
                return stack;
            }
        }

        response.message = "Request Error: Could not withdraw CloudCoins.";
        return Utils.createGson().toJson(response);
    }

    private int[] GetCoinsCurrentlyMarkedForSale(String accountFolder) {
        int[] markedCoins = new int[5];
        String[] files = FileUtils.selectFileNamesInFolder(accountFolder + FileSystem.BankPath);

        for (String file : files) {
            if (file.contains("forsale")) {
                String denomination = file.substring(0, file.indexOf('.'));
                if ("1".equals(denomination))
                    markedCoins[0]++;
                else if ("5".equals(denomination))
                    markedCoins[1]++;
                else if ("25".equals(denomination))
                    markedCoins[2]++;
                else if ("100".equals(denomination))
                    markedCoins[3]++;
                else if ("250".equals(denomination))
                    markedCoins[4]++;
            }
        }
        return markedCoins;
    }

    private void MarkCoins(String denomination, int number, String accountFolder) {
        String bankFolder = accountFolder + FileSystem.BankPath;
        String[] files = FileUtils.selectFileNamesInFolder(bankFolder);
        for (String file : files) {
            if (number <= 0) break;
            if (!file.contains("forsale")) {
                String noteAmount = file.substring(0, file.indexOf('.'));
                if (noteAmount.equals(denomination)) {
                    int end = file.lastIndexOf('.');
                    String newFilename = file.substring(0, end) + ".forsale" + file.substring(end);
                    try {
                        Files.move(Paths.get(bankFolder + file), Paths.get(bankFolder + newFilename), StandardCopyOption.REPLACE_EXISTING);
                        number--;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void UnMarkCoins(String denomination, int number, String accountFolder) {
        String bankFolder = accountFolder + FileSystem.BankPath;
        String[] files = FileUtils.selectFileNamesInFolder(bankFolder);
        for (String file : files) {
            if (number <= 0) break;
            if (file.contains("forsale")) {
                String noteAmount = file.substring(0, file.indexOf('.'));
                if (noteAmount.equals(denomination)) {
                    String newFilename = file.replace("forsale", "").replace("..", ".");
                    try {
                        Files.move(Paths.get(bankFolder + file), Paths.get(bankFolder + newFilename), StandardCopyOption.REPLACE_EXISTING);
                        number--;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
