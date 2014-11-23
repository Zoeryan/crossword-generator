package com.crosswords.controlers;

import com.crosswords.models.CrosswordDictionary;
import com.crosswords.models.Direction;
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

    private static final float reduceSizeFactor = 0.4f;
    private static List<Word> words;

    public Generator() {
        words = new ArrayList<Word>();

        CrosswordDictionary cd = new CrosswordDictionary();
        createWords(cd.getEntries());
    }

    public static GeneratorThread generate(int maxWordsCount, int maxSizeVertical, int maxSizeHorizontal) throws InterruptedException {

        int processors = Runtime.getRuntime().availableProcessors();

        GeneratorThread[] ths = new GeneratorThread[processors];


        double tmp = Math.max((float) Math.sqrt(words.size()) * reduceSizeFactor, (float) words.get(0).getWord().length());
        int autoMaxSizeHorizontal = (int) (tmp * 1.5f);
        int autoMaxSizeVertical = (int) tmp;

        Direction firstWordDirect = Direction.Vertical;

        if (maxSizeHorizontal == 0 && maxSizeVertical == 0)
            firstWordDirect = Direction.getRandomDirection();

        else if (maxSizeHorizontal != 0)
            firstWordDirect = Direction.Vertical;
        else if (maxSizeVertical != 0)
            firstWordDirect = Direction.Horizontal;

        if (maxSizeHorizontal != 0 && maxSizeVertical != 0) {
            firstWordDirect = (maxSizeHorizontal > maxSizeVertical) ? Direction.Horizontal : Direction.Vertical;
            int maxSize = Math.max(maxSizeHorizontal, maxSizeVertical);
            int i = 0;

            while (i < words.size() && words.get(i).getWord().length() > maxSize) i++;

            words.subList(0, i).clear();
        }

        maxSizeHorizontal = (maxSizeHorizontal != 0) ? maxSizeHorizontal : autoMaxSizeHorizontal;
        maxSizeVertical = (maxSizeVertical != 0) ? maxSizeVertical : autoMaxSizeVertical;


        if (words.size() == 0) {
            return null;
        }
        for (int i = 0; i < processors; i++) {
            ths[i] = new GeneratorThread(new ArrayList<Word>(words), maxWordsCount, maxSizeVertical, maxSizeHorizontal, firstWordDirect);
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
        float maxFitness = -1.0f;
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