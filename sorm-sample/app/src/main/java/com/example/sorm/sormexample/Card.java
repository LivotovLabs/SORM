package com.example.sorm.sormexample;

import java.util.List;

import eu.livotov.labs.android.sorm.annotations.Column;
import eu.livotov.labs.android.sorm.annotations.Entity;
import eu.livotov.labs.android.sorm.annotations.Id;
import eu.livotov.labs.android.sorm.annotations.Index;

@Entity(table = "CARD")
public class Card {
//    Each entity must have id column
    @Id
    @Column(name = "ID")
    public long id;

    @Index(name = "GUID_IDX")
    @Column(name = "GUID")
    public String guid;

    @Column(name = "ACTIVE_FROM")
    public long activeFrom;

    public List<Action> actions;

//    Must be presented empty constructor
    public Card() {
    }

    public Card(String guid, long activeFrom, List<Action> actions) {
        this.guid = guid;
        this.activeFrom = activeFrom;
        this.actions = actions;
    }
}
