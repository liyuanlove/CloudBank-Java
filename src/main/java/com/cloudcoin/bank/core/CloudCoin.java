package com.cloudcoin.bank.core;

import com.cloudcoin.bank.utils.CoinUtils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class CloudCoin {


    /* JSON Fields */

    @Expose
    @SerializedName("nn")
    private int nn;
    @Expose
    @SerializedName("sn")
    private int sn;
    @Expose
    @SerializedName("an")
    private ArrayList<String> an = new ArrayList<>();
    @Expose
    @SerializedName("ed")
    private String ed = CoinUtils.calcExpirationDate();
    @Expose
    @SerializedName("pown")
    private String pown = "uuuuuuuuuuuuuuuuuuuuuuuuu";
    @Expose
    @SerializedName("aoid")
    private ArrayList<String> aoid = new ArrayList<>();


    /* Fields */

    public transient String[] pan = new String[Config.nodeCount];

    public transient String folder;

    public transient String currentFilename;

    private transient String fullFilePath;


    /* Methods */

    /**
     * Returns a human readable String describing the contents of the CloudCoin.
     *
     * @return a String describing the CloudCoin.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("cloudcoin: (nn:").append(getNn()).append(", sn:").append(sn);
        if (null != getEd()) builder.append(", ed:").append(getEd());
        if (null != getPown()) builder.append(", pown:").append(getPown());
        if (null != getAoid()) builder.append(", aoid:").append(getAoid().toString());
        if (null != getAn()) builder.append(", an:").append(getAn().toString());
        return builder.toString();
    }


    /* Getters and Setters */

    public int getNn() { return nn; }
    public int getSn() { return sn; }
    public ArrayList<String> getAn() { return an; }
    public String getEd() { return ed; }
    public String getPown() { return pown; }
    public ArrayList<String> getAoid() { return aoid; }
    public String getFolder() { return folder; }
    public String getFullFilePath() { return fullFilePath; }

    public void setEd(String ed) { this.ed = ed; }
    public void setPown(String pown) { this.pown = pown; }

    public void setFolder(String folder) { this.folder = folder; }
    public void setFullFilePath(String fullFilePath) { this.fullFilePath = fullFilePath; }
}
