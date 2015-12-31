package com.example.sorm.sormexample;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        long cardId = create();
        Card card = read(cardId);
        update(card);
        delete(cardId);
    }

    private long create() {
        List<Action> actions = new ArrayList<>();
        String[] msgList = new String[] {"next", "arriba", "previous"};
        for (String msg : msgList) {
            actions.add(new Action(msg, new Date()));
        }
        Card card = new Card("A1", System.currentTimeMillis(), actions);
        return DbUtils.saveCard(this, card);
    }

    private Card read(long id) {
        Card card = DbUtils.getCard(this, "A1");
//        or
        card = DbUtils.getCardById(this, id);
        return card;
    }

    private void update(Card card) {
        card.activeFrom = System.currentTimeMillis();
        DbUtils.updateCard(this, card);
    }

    private void delete(long id) {
        DbUtils.deleteCard(this, id);
    }
}
