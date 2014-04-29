package com.crosswords.tests;

import com.crosswords.models.CrosswordDictionary;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Created by tomek on 29.04.14.
 */
@RunWith(JUnit4.class)
public class CrosswordDictionaryTest{
    @Test
    public void DictionaryInitTest(){
        CrosswordDictionary cd = new CrosswordDictionary();
        Assert.assertTrue(cd.allEntries().size()>0);
    }

}
