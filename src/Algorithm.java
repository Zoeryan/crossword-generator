import com.crosswords.controlers.Generator;
import com.crosswords.controlers.GeneratorThread;

/**
 * Created by Mariusz on 2014-05-18.
 */
public class Algorithm {

    public static char[][] generate(String[] args) {
        char[][] matrix=null;
        try {

            int maxWordsCount = 0;
            int maxSizeVertical = Integer.parseInt(args[1]);
            int maxSizeHorizontal = Integer.parseInt(args[0]);

            Generator gen = new Generator();
            GeneratorThread gt = gen.generate(maxWordsCount, maxSizeVertical, maxSizeHorizontal);
            matrix = gt.getMatrix();
            int minY = gt.getMinY();
            int maxY = gt.getMaxY();
            int minX = gt.getMinX();
            int maxX = gt.getMaxX();

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (matrix[x][y] == '\u0000') System.out.print(' ');
                    else
                        System.out.print(matrix[x][y]);
                }

                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matrix;
    }
}
