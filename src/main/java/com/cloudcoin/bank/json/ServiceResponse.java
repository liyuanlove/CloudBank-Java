package com.cloudcoin.bank.json;

import com.cloudcoin.bank.utils.Utils;

public class ServiceResponse {


    /* Standard JSON Fields */

    public String bankServer;
    public String status = "fail";
    public String version = "1.0.062";
    public String time = Utils.getDate();
    public String message;


    /* Input Fields */

    public String account;
    public String pk;
    public String stack;
    public String amount;
    public String id;
    public String action;
    public String emailTarget;
    public String emailSender;
    public String signBy;

    public Integer ones;
    public Integer fives;
    public Integer twentyfives;
    public Integer hundreds;
    public Integer twohundredfifties;


    /* Transaction Fields */

    public String receipt;


    /* Node Fields */

    public String readyCount;
    public String notReadyCount;
}
