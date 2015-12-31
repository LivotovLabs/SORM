package com.example.sorm.sormexample;

import java.util.Date;

import eu.livotov.labs.android.sorm.annotations.Column;
import eu.livotov.labs.android.sorm.annotations.Entity;
import eu.livotov.labs.android.sorm.annotations.Id;

@Entity(table = "ACTIONS")
public class Action {
    @Id
    @Column(name = "ID")
    public long id;

    @Column(name = "CARD_ID")
    public long cardId;

    @Column(name = "ACTION_DATE")
    public Date actionDate;

    @Column(name = "MESSAGE")
    public String message;

    public Action() {
    }

    public Action(String message, Date actionDate) {
        this.actionDate = actionDate;
        this.message = message;
    }
}
