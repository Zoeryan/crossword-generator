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
    private static final char wipeChar = ' ';
    Direction firstWordDirect;
    private char[][] matrix;
    private float fitness;
    private List<Word> words;
    private int cellsUsed;          //liczba znaków zostało użytych w słowach
    private int wordsUsed;          //liczba słów zostało użytych
    private int wordsCount;         //liczba słów możliwych
    private int maxWordsCount;      //liczba słów mogących być użyta (ograniczenie); jeśli wynosi 0 nie brane pod uwagę
    private int maxSizeHorizontal;  //maksymnalba szerokość - wymiar w poiomie
    private int maxSizeVertical;    //maksymalna wysokość - wymiar w pionie
    //Najdalej odsunięte zajęte komórki przez znaki słów
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private Random rnd;

    /**
     * @param words             lista słów
     * @param maxWordsCount     liczba słów mogących być użyta (ograniczenie); jeśli wynosi 0 nie brane pod uwagę
     * @param maxSizeVertical   maksymalna wysokość
     * @param maxSizeHorizontal maksymnalba szerokość
     */
    public GeneratorThread(List words, int maxWordsCount, int maxSizeVertical, int maxSizeHorizontal, Direction firstWordDirect) {
        cellsUsed = wordsUsed = 0;
        this.words = words;
        this.maxWordsCount = maxWordsCount;
        rnd = new Random();
        wordsCount = words.size();

        this.firstWordDirect = firstWordDirect;
        this.maxSizeVertical = maxSizeVertical;
        this.maxSizeHorizontal = maxSizeHorizontal;
        matrix = new char[this.maxSizeHorizontal][this.maxSizeVertical]; //inicializacja z początkową wielkościa

    }

    /**
     * Sprawdza czy znak jest znakiem uprawnionym do wystąpowanie w słowie w krzyżówce.
     *
     * @param c             znak do sprawdzenia
     * @return Prawda jeśli jest uprawnionym znakiem do wystąpowanie w słowie w krzyżówce.
     */
    private static boolean isWordChar(char c) {
        return c != '\u0000' && c != wipeChar;
    }

    /**
     * Przegląda słowa do użycia. Czuwa nad zakończeniem algorytmu.
     * Oblicza fitness po zakończeniu.
     */
    public void run() {

        Queue<Word> q = new LinkedList<Word>();

        Word firstWord = words.get(0);     // zaczynamy od najdłuzszego słowa
        words.remove(0);
        q.add(firstWord);


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
            minY = y = getRandomInt(matrix[0].length - firstWordLength);
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
            if (currentWord == null) continue;

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
                    continue;

                Word crossingResult = cross(point, newWordDirection);// spróbuj skrzyżować

                if (crossingResult != null) {
                    //   lackWords = true;
                    // break;

                    wordsUsed++;
                    currentWordSuccessfulCrossings++;
                    words.remove(crossingResult);
                    crossingResult.setDirection(newWordDirection);
                    q.offer(crossingResult); //dodajemy do kolejki nowe słowo
                }

            } while ((float) (currentWordSuccessfulCrossings / currentWordLength) < crossingFactor);
        }
        while (((q.isEmpty() == false || (maxWordsCount == 0 && wordsUsed < maxWordsCount))));

        calculateFitness();
    }

    /**
     * Wyszukuje słowo mogące się skrzyżować w danym punkcie.
     *
     * @param point            punkt krzyżowania
     * @param newWordDirection ułożenia słowa pionowe lub poziome
     * @return obiekt Word jeśli krzyżowanie się powiodło, null w przeciwnym wypadku
     */
    private Word cross(Point point, Direction newWordDirection) {
        int attempts = 0;

        float actual = 1.0f;

        do {

            Word word = searchWordToCross(matrix[point.x][point.y]);
            if (word == null) return null;

            Point p = checkCollisions(word, point, newWordDirection);// sprawdz w matrixie kolize
            if (p != null)   //jesli skrzyzowanie powiedzie sie
            {
                word.setPoint(p);
                return word;
            }
            if (words.size() == 0) return null;
            actual = ((float) ++attempts) / ((float) wordsCount);
        } while (actual < crossingAttemptsFactor);

        return null;// cross failed
    }

    /**
     * Sprawdza czy mieści się w obszarze krzyżówki.
     * Sprawdza czy krzyżuje sie odpowiednio z istniejacymi słowami.
     * Sprawdza czy nie styka sie z istniejacymi słowami na krzyżówce.
     *
     * @param wordW            sprawdzane słowo
     * @param crosspoint       punkt przecięcia z istniejącym słowem na krzyżówce
     * @param newWordDirection ułozenie słowa pionowe lub poziome
     * @return punkt krzyżowania
     */
    private Point checkCollisions(Word wordW, Point crosspoint, Direction newWordDirection) {

        char charS = matrix[crosspoint.x][crosspoint.y];
        String word = wordW.getWord();
        int wordLength = word.length();


        List<Integer> crossPointsStartShift = new ArrayList<Integer>();

        boolean succesfullSearching = false;
        for (int i = 0; i < wordLength; i++)
            if (word.charAt(i) == charS) {
                if (newWordDirection == Direction.Horizontal) {
                    if (crosspoint.x - i < 0 || crosspoint.x + wordLength - i - 1 >= maxSizeHorizontal)  //czy nie wychodzi poza plansze
                        continue;
                    if (crosspoint.x - i - 1 >= 0) {
                        if (isWordChar(matrix[crosspoint.x - i - 1][crosspoint.y]) &&
                                (crosspoint.x + wordLength - i - 1 < maxSizeHorizontal && isWordChar(matrix[crosspoint.x + wordLength - i - 2][crosspoint.y])))
                            continue;           // czy nie graniczy z literą po skrajach   ??
                    }
                    crossPointsStartShift.add(crosspoint.x - i);   // odkad rozpocząć;
                    succesfullSearching = true;
                }

                if (newWordDirection == Direction.Vertical) {
                    if (crosspoint.y - i < 0 || crosspoint.y + wordLength - i - 1 >= maxSizeVertical)
                        continue;
                    if (crosspoint.y - i - 1 >= 0) {


                        if ((isWordChar(matrix[crosspoint.x][crosspoint.y - i - 1])) &&

                                (crosspoint.y + wordLength - i + 1 < maxSizeVertical && isWordChar(matrix[crosspoint.x][crosspoint.y + wordLength - i + 1])))
                            continue;

                    }
                    crossPointsStartShift.add(crosspoint.y - i);
                    succesfullSearching = true;
                }
            }

        if (succesfullSearching == false) return null;

        do {
            int randomIndex = getRandomInt(crossPointsStartShift.size() - 1);  // losowy opunkt startowy
            int startPoint = crossPointsStartShift.get(randomIndex);

            crossPointsStartShift.remove(randomIndex);

            if (trackWord(wordW, newWordDirection, crosspoint, startPoint)) {
                if (newWordDirection == Direction.Horizontal)
                    return new Point(startPoint, crosspoint.y);
                else return new Point(crosspoint.x, startPoint);
            }
        } while (crossPointsStartShift.size() > 0);

        return null;     // nie powiodło się
    }

    /**
     * Wstawia słowo w krzyżówce.
     *
     * @param wordW            sprawdzane słowo
     * @param newWordDirection ułozenie słowa pionowe lub poziome
     * @param crosspoint       punkt przecięcia z istniejącym słowem na krzyżówce
     * @param startPoint       punkt rozpoczęcia nowego słowa (zależy od newWordDirection)
     * @return czy słowo możę być poprawnie ułożone w krzyżówce
     */

    private boolean trackWord(Word wordW, Direction newWordDirection, Point crosspoint, int startPoint) {
        int cellsCurrentUsed = 0;
        List<Point> lettersAdded = new ArrayList<Point>();
        String word = wordW.getWord();

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
                if ((crosspoint.y + 1 < maxSizeVertical && matrix[startPoint + i][crosspoint.y] != word.charAt(i) && isWordChar(matrix[startPoint + i][crosspoint.y + 1])) ||
                        (crosspoint.y - 1 >= 0 && matrix[startPoint + i][crosspoint.y] != word.charAt(i) && isWordChar(matrix[startPoint + i][crosspoint.y - 1]))
                        || (isWordChar(matrix[startPoint + i][crosspoint.y]) && matrix[startPoint + i][crosspoint.y] != word.charAt(i))) {         // jeśli napotka litere x innego słowa i nie jest taka sama
                    undoAddingLetters(lettersAdded);
                    words.add(wordW);
                    return false;
                }

                if (matrix[startPoint + i][crosspoint.y] != word.charAt(i)) {  // jeżeli nie jest miejscem skrzyzowania zinnym slowem i przeszlo wczesniejsze if-y
                    matrix[startPoint + i][crosspoint.y] = word.charAt(i);
                    cellsCurrentUsed++;  // nie liczymy przeciec
                    lettersAdded.add(new Point(startPoint + i, crosspoint.y));
                }

            } else {
                if ((crosspoint.x + 1 < maxSizeHorizontal && matrix[crosspoint.x][startPoint + i] != word.charAt(i) && isWordChar(matrix[crosspoint.x + 1][startPoint + i]))
                        || (crosspoint.x - 1 >= 0 && matrix[crosspoint.x][startPoint + i] != word.charAt(i) && isWordChar(matrix[crosspoint.x - 1][startPoint + i])) || (isWordChar(matrix[crosspoint.x][startPoint + i]) && matrix[crosspoint.x][startPoint + i] != word.charAt(i))) {
                    undoAddingLetters(lettersAdded);
                    words.add(wordW);
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

    /**
     * Usuwa uprzednio dodane dodane litery.
     *
     * @param lettersPlace usytuowanie liter do usunięcia.
     */
    private void undoAddingLetters(List<Point> lettersPlace) {
        Point p;
        for (int i = 0; i < lettersPlace.size(); i++) {
            p = lettersPlace.get(i);
            matrix[p.x][p.y] = wipeChar;
        }
    }

    /**
     * Szukanie słowa z taką samą literą jak bieżące słowo w celach skrzyżowanie z nim.
     *
     * @param c             litera która ma być znaleziona.
     * @return
     */
    private Word searchWordToCross(char c) {   //szukanie słowa z taką samą literą

        int attempts = 0;

        if (words.size() == 0) return null;
        do {
            int currentWordsIndex = getRandomInt(words.size() - 1);
            Word w = words.get(currentWordsIndex);
            if (w.getWord().indexOf(c) != -1) {
                words.remove(currentWordsIndex);
                return w;
            }
        }
        while ((float) (++attempts / words.size()) < searchingIdenticalLetterFactor);
        return null;// failed
    }

    /**
     * Losuje liczbę całkowitą z przedziału od zera włącznie
     *
     * @param maxValue      liczba będąca górnym ograniczeniem (włącznie z nią)
     * @return
     */
    private int getRandomInt(int maxValue) {
        return rnd.nextInt(maxValue + 1);
    }

    /**
     * Oblicza współczynnik przystosowanie (fitness) dla otrzymanej krzyżówki.
     */
    private void calculateFitness() {
        fitness = ((float) cellsUsed / (matrix[0].length * matrix.length) + (float) (wordsUsageFactor * wordsUsed / wordsCount)) / (float) (1 + wordsUsageFactor);
    }

    /**
     * Wypisywanie istotnych informacji w celach testowych.
     */
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
