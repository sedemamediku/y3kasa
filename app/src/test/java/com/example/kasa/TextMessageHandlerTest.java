package com.example.kasa;

import static org.junit.Assert.*;

import com.example.Kasa.TextMessageHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TextMessageHandlerTest {

    private Class<?> contactInfoClass;
    private Constructor<?> contactInfoCtor;
    private Method levenshteinMethod;
    private Method findBestMatchMethod;

    @Before
    public void setUp() throws Exception {
        // grab the nested ContactInfo type
        contactInfoClass = Class.forName("com.example.Kasa.TextMessageHandler$ContactInfo");
        contactInfoCtor  = contactInfoClass.getDeclaredConstructor(String.class, String.class);
        contactInfoCtor.setAccessible(true);

        levenshteinMethod = TextMessageHandler.class
                .getDeclaredMethod("levenshteinDistance", String.class, String.class);
        levenshteinMethod.setAccessible(true);

        findBestMatchMethod = TextMessageHandler.class
                .getDeclaredMethod("findBestMatch", List.class, String.class);
        findBestMatchMethod.setAccessible(true);
    }

    @Test
    public void levenshteinDistance_correctEdits() throws Exception {
        assertEquals(3, (int) levenshtein("kitten","sitting"));
        assertEquals(2, (int) levenshtein("flaw","lawn"));
        assertEquals(0, (int) levenshtein("same","same"));
    }

    @Test
    public void findBestMatch_picksClosestName() throws Exception {
        // build: ["Alice","Bob","Charlie"] with fake phone#
        Object a = contact("Alice","111");
        Object b = contact("Bob","222");
        Object c = contact("Charlie","333");

        List<?> list = Arrays.asList(a,b,c);
        // queryName "Bbo" should match "Bob"
        Object best = findBestMatch(list, "Bbo");

        // reflectively extract name+phone
        Field nameF  = contactInfoClass.getDeclaredField("name");
        Field phoneF = contactInfoClass.getDeclaredField("phone");
        nameF.setAccessible(true);
        phoneF.setAccessible(true);

        assertEquals("Bob",   nameF.get(best));
        assertEquals("222",   phoneF.get(best));
    }

    @Test
    public void findBestMatch_tiesPickFirst() throws Exception {
        // two entries same distance => first one wins
        Object x = contact("Ann","A");
        Object y = contact("Ana","B");
        List<?> list = Arrays.asList(x,y);

        // both are distance 1 from "Ann", but x is first
        Object best = findBestMatch(list, "Ann");
        Field nameF  = contactInfoClass.getDeclaredField("name");
        nameF.setAccessible(true);
        assertEquals("Ann", nameF.get(best));
    }

    // helpers

    private Object contact(String name, String phone) throws Exception {
        return contactInfoCtor.newInstance(name, phone);
    }

    private Integer levenshtein(String s1, String s2) throws Exception {
        return (Integer) levenshteinMethod.invoke(null, s1, s2);
    }

    private Object findBestMatch(List<?> list, String q) throws Exception {
        return findBestMatchMethod.invoke(null, list, q);
    }
}
