package com.crosswords.models;

import java.awt.*;

/**
 * Created by Mariusz on 2014-05-18.
 */
public class Word {

    private String word;
    private Point point;
    private Direction direction;
    private String description;

    public void setPoint(Point point) {
        this.point = point;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Point getPoint() {
        return point;
    }

    public String getWord() {
        return word;
    }

    public Direction getDirection() {
        return direction;
    }

    public Word(String word, String description) {
        this.word = word;
        this.description = description;
    }


}
