package com.crosswords.controlers;

import com.crosswords.models.Direction;
import com.crosswords.models.Word;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * Created by Mariusz on 2014-05-18.
 */
public class GeneratorThread extends Thread {

    //STAŁE
    private static final float crossingAttemptsFactor = 0.15f;
    private static final float crossingFactor = 0.25f;
    private static final int wordsUsageFactor = 2;
    private static final float searchingIdenticalLetterFactor = 0.75f;
    private static final float reduceSizeFactor = 0.4f;
    private static final char wipeChar = ' ';


    private char[][] matrix;
    private float fitness;
    private List<Word> words;

    private int cellsUsed;
    private int wordsUsed;
    private int wordsCount;
    private int maxWordsCount;
    private int maxSizeHorizontal;
    private int maxSizeVertical;

    private int minX;
    private int minY;
    private int maxX;
    private int maxY;

    private Random rnd;

    public GeneratorThread(List words, int maxWordsCount, int maxSizeVertical, int maxSizeHorizontal) {
        cellsUsed = wordsUsed = 0;
        this.words = words;
        this.maxWordsCount = maxWordsCount;
        rnd = new Random();
        wordsCount = words.size();

        double tmp = Math.max((float) Math.sqrt(words.size()) * reduceSizeFactor, (float) this.words.get(0).getWord().length());
        int autoMaxSizeHorizontal = (int) (tmp * 1.5f);
        int autoMaxSizeVertical = (int) tmp;
        this.maxSizeHorizontal = (maxSizeHorizontal != 0) ? maxSizeHorizontal : autoMaxSizeHorizontal;
        this.maxSizeVertical = (maxSizeVertical != 0) ? maxSizeVertical : autoMaxSizeVertical;
        matrix = new char[this.maxSizeHorizontal][this.maxSizeVertical]; //inicializacja z początkową wielkościa
    }

    public void run() {

        Queue<Word> q = new LinkedList<Word>();

        Word firstWord = words.get(0);     // zaczynamy od najdłuzszego słowa
        q.add(firstWord);

        Direction firstWordDirect = Direction.getRandomDirection();
        firstWord.setDirection(firstWordDirect);
        int firstWordLength = firstWord.getWord().length();
        int x;
        int y;
        if (firstWordDirect == Direction.Horizontal) {   // ustawiania pierwszego słowa poziomo lub pionowo
            minX = x = getRandomInt(matrix.length - 1 - firstWordLength);
            maxY = minY = y = getRandomInt(matrix[0].length - 1);
            for (int i = 0; i < firstWordLength; i++) {
                matrix[x + i][y] = firstWord.getWord().charAt(i);
            }
            maxX = minX + firstWordLength - 1;
        } else {
            maxX = minX = x = getRandomInt(matrix.length - 1);
            minY = y = getRandomInt(matrix[0].length - 1 - firstWordLength);
            for (int i = 0; i < firstWordLength; i++) {
                matrix[x][y + i] = firstWord.getWord().charAt(i);
            }
            maxY = minY + firstWordLength - 1;
        }
        firstWord.setPoint(new Point(x, y));

        // nastepne słowa

        List<Integer> temporaryPoints = new ArrayList<Integer>();

        do {   //dopóki jest coś w kolejce i  nie przekroczono limitu słów (jeśli to ost. jest niezbedne)
            Word currentWord = q.poll(); //wez nieuzyte słowo

            Direction newWordDirection = Direction.getOpositeDirection(currentWord.getDirection());

            int currentWordSuccessfulCrossings = 0;
            int currentWordLength = currentWord.getWord().length();

            int startPoint = (currentWord.getDirection() == Direction.Horizontal) ? currentWord.getPoint().x : currentWord.getPoint().y;

            temporaryPoints.clear();
            for (int i = startPoint; i < startPoint + currentWordLength; i++)     // punkty - litery na słowach
                temporaryPoints.add(i);

            do {   // dopóki nie skrzyżuje na odpowiedniej ilości liter lub przejzy wszystkie

                if (temporaryPoints.size() == 0) break;

                int index = getRandomInt(temporaryPoints.size() - 1);
                int pointOneDim = temporaryPoints.get(index);
                temporaryPoints.remove(index);


                Point point = (currentWord.getDirection() == Direction.Horizontal) ? new Point(pointOneDim, currentWord.getPoint().y) : new Point(currentWord.getPoint().x, pointOneDim);
                if (point.x < 0 || point.y < 0 || point.x >= maxSizeHorizontal || point.y >= maxSizeVertical)
                    continue; //??

                Word crossingResult = cross(point, newWordDirection);// spróbuj skrzyżować

                if (crossingResult != null) {
                    wordsUsed++;
                    currentWordSuccessfulCrossings++;
                    words.remove(crossingResult);
                    crossingResult.setDirection(newWordDirection);
                    q.offer(crossingResult); //dodajemy do kolejki nowe słowo
                }

            } while ((float) (currentWordSuccessfulCrossings / currentWordLength) < crossingFactor);
        }
        while (q.isEmpty() == false || (maxWordsCount == 0 && wordsUsed < maxWordsCount));

        calculateFitness();
    }

    private Word cross(Point point, Direction newWordDirection) {
        int attempts = 0;
        do {

            Word word = searchWordToCross(matrix[point.x][point.y]);
            if (word == null) return null;

            Point p = checkCollisions(word.getWord(), point, newWordDirection);// sprawdz w matrixie kolize
            if (p != null)   //jesli skrzyzowanie powiedzie sie
            {
                word.setPoint(p);
                return word;
            }
        } while ((float) (++attempts / words.size()) < crossingAttemptsFactor);

        return null;// cross failed
    }

    private Point checkCollisions(String word, Point crosspoint, Direction newWordDirection) {
        //sprawdz czy wyjezdza poza obszar
        // sprawedź czy krzyżuje sie odpowiednio z istniejacymi słowami
        // sprawdź czy nie styka sie z istniejacymi sł owami

        char charS = matrix[crosspoint.x][crosspoint.y];
        int wordLength = word.length();


        List<Integer> crossPointsStartShift = new ArrayList<Integer>();

        boolean succesfullSearching = false;
        for (int i = 0; i < wordLength; i++)
            if (word.charAt(i) == charS) {
                if (newWordDirection == Direction.Horizontal) {
                    if (crosspoint.x - i < 0 || crosspoint.x + wordLength - i - 1 >= maxSizeHorizontal)  //czy nie wychodzi poza plansze
                        continue;
                    if ((crosspoint.x - i - 1 >= 0 && isWordChar(matrix[crosspoint.x - i - 1][crosspoint.y])) ||
                            (crosspoint.x + wordLength - i - 1 < maxSizeHorizontal && isWordChar(matrix[crosspoint.x + wordLength - i - 1][crosspoint.y])))
                        continue;           // czy nie graniczy z literą po skrajach   ??

                    crossPointsStartShift.add(crosspoint.x - i);   // odkad rozpocząć;
                    succesfullSearching = true;
                }

                if (newWordDirection == Direction.Vertical) {
                    if (crosspoint.y - i < 0 || crosspoint.y + wordLength - i - 1 >= maxSizeVertical)
                        continue;
                    if ((crosspoint.y - i - 1 >= 0 && isWordChar(matrix[crosspoint.x][crosspoint.y - i - 1])) ||
                            (crosspoint.y + wordLength - i + 1 < maxSizeVertical && isWordChar(matrix[crosspoint.x][crosspoint.y + wordLength - i + 1])))
                        continue;

                    crossPointsStartShift.add(crosspoint.y - i);
                    succesfullSearching = true;
                }
            }

        if (succesfullSearching == false) return null;

        do {
            int randomIndex = getRandomInt(crossPointsStartShift.size() - 1);  // losowy opunkt startowy
            int startPoint = crossPointsStartShift.get(randomIndex);

            crossPointsStartShift.remove(randomIndex);

            if (trackWord(word, newWordDirection, crosspoint, startPoint)) {
                if (newWordDirection == Direction.Horizontal)
                    return new Point(startPoint, crosspoint.y);
                else return new Point(crosspoint.x, startPoint);
            }
        } while (crossPointsStartShift.size() > 0);

        return null;     // nie powiodło się
    }

    private boolean trackWord(String word, Direction newWordDirection, Point crosspoint, int startPoint) {
        int cellsCurrentUsed = 0;
        List<Point> lettersAdded = new ArrayList<Point>();

        if (newWordDirection == Direction.Horizontal)     //??
        {
            if (startPoint - 1 >= 0 && isWordChar(matrix[startPoint - 1][crosspoint.y])) return false;
            if (startPoint + word.length() + 1 < maxSizeHorizontal && isWordChar(matrix[startPoint + word.length() + 1][crosspoint.y]))
                return false;
        } else {
            if (startPoint - 1 >= 0 && isWordChar(matrix[crosspoint.x][startPoint - 1])) return false;
            if (startPoint + word.length() + 1 < maxSizeVertical && isWordChar(matrix[crosspoint.x][startPoint + word.length() + 1]))
                return false;

        }

        for (int i = 0; i < word.length(); i++) {
            if (newWordDirection == Direction.Horizontal) {
                if (crosspoint.y + 1 < maxSizeVertical && matrix[startPoint + i][crosspoint.y] != word.charAt(i) && isWordChar(matrix[startPoint + i][crosspoint.y + 1])) {
                    undoAddingLetters(lettersAdded);
                    return false;
                }
                if (crosspoint.y - 1 >= 0 && matrix[startPoint + i][crosspoint.y] != word.charAt(i) && isWordChar(matrix[startPoint + i][crosspoint.y - 1])) {
                    undoAddingLetters(lettersAdded);
                    return false;
                }
                if (isWordChar(matrix[startPoint + i][crosspoint.y]) && matrix[startPoint + i][crosspoint.y] != word.charAt(i)) {         // jeśli napotka litere x innego słowa i nie jest taka sama
                    undoAddingLetters(lettersAdded);
                    return false;
                }

                if (matrix[startPoint + i][crosspoint.y] != word.charAt(i)) {  // jeżeli nie jest miejscem skrzyzowania zinnym slowem i przeszlo wczesniejsze if-y
                    matrix[startPoint + i][crosspoint.y] = word.charAt(i);
                    cellsCurrentUsed++;  // nie liczymy przeciec
                    lettersAdded.add(new Point(startPoint + i, crosspoint.y));
                }

            } else {
                if (crosspoint.x + 1 < maxSizeHorizontal && matrix[crosspoint.x][startPoint + i] != word.charAt(i) && isWordChar(matrix[crosspoint.x + 1][startPoint + i])) {
                    undoAddingLetters(lettersAdded);
                    return false;
                }
                if (crosspoint.x - 1 >= 0 && matrix[crosspoint.x][startPoint + i] != word.charAt(i) && isWordChar(matrix[crosspoint.x - 1][startPoint + i])) {
                    undoAddingLetters(lettersAdded);
                    return false;
                }
                if (isWordChar(matrix[crosspoint.x][startPoint + i]) && matrix[crosspoint.x][startPoint + i] != word.charAt(i)) {
                    undoAddingLetters(lettersAdded);
                    return false;
                }

                if (matrix[crosspoint.x][startPoint + i] != word.charAt(i)) {
                    matrix[crosspoint.x][startPoint + i] = word.charAt(i);
                    cellsCurrentUsed++;   // nie liczymy przeciec
                    lettersAdded.add(new Point(crosspoint.x, startPoint + i));
                }
            }
        }
        cellsUsed += cellsCurrentUsed;

        if (newWordDirection == Direction.Horizontal) {
            if (startPoint + word.length() - 1 > maxX) maxX = startPoint + word.length() - 1;
            if (startPoint < minX) minX = startPoint;
        } else {
            if (startPoint + word.length() - 1 > maxY) maxY = startPoint + word.length() - 1;
            if (startPoint < minY) minY = startPoint;
        }
        return true;
    }

    private void undoAddingLetters(List<Point> lettersAdded) {
        Point p;
        for (int i = 0; i < lettersAdded.size(); i++) {
            p = lettersAdded.get(i);
            matrix[p.x][p.y] = wipeChar;
        }
    }

    private Word searchWordToCross(char c) {   //szukanie słowa z taką samą literą

        int attempts = 0;
        do {
            int currentWordsIndex = getRandomInt(words.size() - 1);
            Word w = words.get(currentWordsIndex);
            if (w.getWord().indexOf(c) != -1) return w;
        }
        while ((float) (++attempts / words.size()) < searchingIdenticalLetterFactor);
        return null;// failed
    }

    private int getRandomInt(int maxValue) {
        return rnd.nextInt(maxValue + 1);
    }

    private void calculateFitness() {
        fitness = ((float) cellsUsed / (matrix[0].length * matrix.length) + (float) (wordsUsageFactor * wordsUsed / wordsCount)) / (float) (1 + wordsUsageFactor);
    }

    private static boolean isWordChar(char c) {
        return c != '\u0000' && c != wipeChar;
    }

    //DEBUG
    public void printDebug() {
        int sX = maxX - minX + 1;
        int sY = maxY - minY + 1;

        System.out.println("Jestem wątek nr " + this.getId() + ". Mój Fitness: " + this.fitness + " |komórek: " + cellsUsed + " na możliwych: " + matrix[0].length * matrix.length + " |słów użyto: " + wordsUsed + " na możliwych: " + wordsCount);
        System.out.println("Żądany romiar [X,Y]: " + this.maxSizeHorizontal + ", " + this.maxSizeVertical + " Wynikowy rozmiar [X,Y]: " + sX + ", " + sY);
        System.out.println();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!isWordChar(matrix[x][y])) System.out.print(' ');
                else
                    System.out.print(matrix[x][y]);
            }

            System.out.println();
        }
    }

    //GETTERY
    public float getFitness() {
        return fitness;
    }

    public char[][] getMatrix() {
        return matrix;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

}
