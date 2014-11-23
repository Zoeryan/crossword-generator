import com.crosswords.controlers.Generator;
import com.crosswords.controlers.GeneratorThread;

/**
 * Created by Mariusz on 2014-05-18.
 */
public class Main {

    public static void main(String[] args) {
        try {

            int maxWordsCount = 0;
            int maxSizeVertical = 10;
            int maxSizeHorizontal = 10;

            Generator gen = new Generator();
            GeneratorThread gt = gen.generate(maxWordsCount, maxSizeVertical, maxSizeHorizontal);
            char[][] matrix = gt.getMatrix();
            int minY = gt.getMinY();
            int maxY = gt.getMaxY();
            int minX = gt.getMinX();
            int maxX = gt.getMaxX();

            //for (int y=minY; y<=maxY; y++) {
            //    for (int x =minX;x<=maxX; x++)
            //      matrix[x][y];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
