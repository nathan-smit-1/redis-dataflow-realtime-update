package com.pepkorit.cloud.solutions.realtimeredisupdate.pipeline;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
/**
 * Data model represents the information required to update the current stock value
 */
@DefaultCoder(AvroCoder.class)
public class BranchCompanySkuTransactionValue {

    public BranchCompanySkuTransactionValue(String branch, String company, String sku, String transactionId, String table, String value) {
        this.branch = branch;
        this.company = company;
        this.sku = sku;
        this.transactionId = transactionId;
        this.table = table;
        this.value = value;
    }

    private String branch;
    private String company;
    private String sku;
    private String transactionId;

    private String table;
    private String value;

    public String getTransactionId() {
        return transactionId;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}