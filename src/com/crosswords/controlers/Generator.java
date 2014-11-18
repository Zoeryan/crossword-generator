package com.crosswords.controlers;

import com.crosswords.models.CrosswordDictionary;
import com.crosswords.models.IEntry;
import com.crosswords.models.Word;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Mariusz on 2014-05-18.
 */
public class Generator {

    private static List<Word> words;


    public Generator() {
        words = new ArrayList<Word>();

        CrosswordDictionary cd = new CrosswordDictionary();
        createWords(cd.getEntries());
    }

    public static GeneratorThread generate(int maxWordsCount, int maxSizeVertical, int maxSizeHorizontal) throws InterruptedException {

        int processors = Runtime.getRuntime().availableProcessors();

        GeneratorThread[] ths = new GeneratorThread[processors];

        if (words.size() == 0) return null;
        for (int i = 0; i < processors; i++) {
            ths[i] = new GeneratorThread(new ArrayList<Word>(words), maxWordsCount, maxSizeVertical, maxSizeHorizontal);
            ths[i].start();
            ths[i].join();
            ths[i].printDebug();
        }
        return ths[chooseBestResult(ths)];
    }

    private static void createWords(Set<IEntry> entries) {
        Iterator<IEntry> iterator = entries.iterator();
        int maxLength = 0;
        int i = 0;

        while (iterator.hasNext()) {

            IEntry element = (IEntry) iterator.next();

            String tmp = element.getWord();

            if (words.size() == 0)
                words.add(new Word(tmp, element.getClues().get(0)));
            else {
                for (int j = 0; j < words.size(); j++)  // sortowanie malejaco po dlugosci
                    if (words.get(j).getWord().length() < tmp.length()) {
                        words.add(j, new Word(tmp, element.getClues().get(0)));
                        break;
                    }
            }
            i++;
        }
    }

    private static int chooseBestResult(GeneratorThread[] ths) {
        float maxFitness = 0;
        int indexBest = -1;
        for (int i = 0; i < ths.length; i++) {
            float currentFitness = ths[i].getFitness();
            if (currentFitness > maxFitness) {
                indexBest = i;
                maxFitness = currentFitness;
            }
        }
        return indexBest;
    }
}