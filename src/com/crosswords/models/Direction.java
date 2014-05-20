package com.crosswords.models;

/**
 * Created by Mariusz on 2014-05-18.
 */
public enum Direction {
    Horizontal,
    Vertical;


    public static Direction getRandomDirection() {
        return values()[(int) (Math.random() * values().length)];
    }

    public static Direction getOpositeDirection(Direction d) {
        return values()[(d.ordinal() + 1) % 2];
    }
}
