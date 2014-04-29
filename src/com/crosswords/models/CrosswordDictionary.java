package com.crosswords.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by tomek on 29.04.14.
 */
public class CrosswordDictionary implements ICrosswordDictionary {

    private String[] filenames = new String[] {"datasets/1.csv"};

    private static Set<IEntry> entries;

    @Override
    public Set<IEntry> allEntries() {
        return getEntries();

    }

    public Set<IEntry> getEntries() {
        if(entries == null){
            loadEntries();
        }
        return entries;
    }

    private void loadEntries() {
        entries = new HashSet<IEntry>();
        for(String p: filenames){
            try {
                List<String> strings = Files.readAllLines(FileSystems.getDefault().getPath(p));
                for(String line: strings){
                    if(line.matches(".*,.*")){
                        Entry entry = new Entry();
                        entry.setWord(line.split(",")[0]);
                        entry.setClues(
                                Arrays.asList(
                                        line.substring(entry.getWord().length()).split("\",\"")
                                )
                        );
                        entries.add(entry);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
