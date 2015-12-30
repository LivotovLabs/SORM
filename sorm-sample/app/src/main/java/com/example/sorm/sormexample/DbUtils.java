package com.example.sorm.sormexample;

import android.content.Context;

import java.util.List;

import eu.livotov.labs.android.sorm.EntityManager;
import eu.livotov.labs.android.sorm.EntityManagerFactory;
import eu.livotov.labs.android.sorm.core.config.EntityManagerConfiguration;

public class DbUtils {
//    If you use gradle build variants, provide package name manually. Since it differs from application id
    public static void init(Context context) {
        EntityManagerConfiguration configuration = new EntityManagerConfiguration();
        configuration.setEntitiesPackage(BuildConfig.PACKAGE_NAME);
        EntityManagerFactory.getEntityManager(context, configuration);
    }

    private static EntityManager em(Context c)
    {
        return EntityManagerFactory.getEntityManager(c);
    }

    public static long saveCard(Context c, Card card) {
        em(c).create(card);
        saveActions(c, card);
        return card.id;
    }

    private static void saveActions(Context c, Card card) {
        for (Action action : card.actions) {
            action.cardId = card.id;
            em(c).create(action);
        }
    }

    private static List<Action> getActions(Context c, long cardId) {
        return em(c).createQuery(Action.class).where("CARD_ID").isEqualTo(cardId).load();
    }

    public static Card getCardById(Context c, long id) {
        Card card = em(c).find(Card.class, id);
        card.actions = getActions(c, card.id);
        return card;
    }

    public static Card getCard(Context c, String guid) {
        Card card = em(c).createQuery(Card.class).where("GUID").isEqualTo(guid).loadSingle();
        card.actions = getActions(c, card.id);
        return card;
    }

    public static void updateCard(Context c, Card card) {
        em(c).save(card);
        deleteActions(c, card.id);
        saveActions(c, card);
    }

    private static void deleteActions(Context c, long cardId) {
        em(c).createQuery(Action.class).where("CARD_ID").isEqualTo(cardId).delete();
    }

    public static void deleteCard(Context c, long id) {
        deleteActions(c, id);
        em(c).delete(Card.class, id);
    }
}
