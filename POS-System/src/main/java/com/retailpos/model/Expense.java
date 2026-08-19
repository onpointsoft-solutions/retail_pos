package com.retailpos.model;

import java.time.LocalDate;

public class Expense {
    private String id;
    private String category;   // RENT, UTILITIES, SALARIES, SUPPLIES, MAINTENANCE, OTHER
    private String description;
    private double amount;
    private LocalDate date;
    private String reference;
    private String createdBy;
    private String syncStatus;

    public Expense() {}

    public String getId()                     { return id; }
    public void   setId(String id)            { this.id = id; }
    public String getCategory()               { return category; }
    public void   setCategory(String v)       { this.category = v; }
    public String getDescription()            { return description; }
    public void   setDescription(String v)    { this.description = v; }
    public double getAmount()                 { return amount; }
    public void   setAmount(double v)         { this.amount = v; }
    public LocalDate getDate()                { return date; }
    public void   setDate(LocalDate v)        { this.date = v; }
    public String getReference()              { return reference; }
    public void   setReference(String v)      { this.reference = v; }
    public String getCreatedBy()              { return createdBy; }
    public void   setCreatedBy(String v)      { this.createdBy = v; }
    public String getSyncStatus()             { return syncStatus; }
    public void   setSyncStatus(String v)     { this.syncStatus = v; }
}
