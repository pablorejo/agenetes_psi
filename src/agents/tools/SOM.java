package agents.tools;
import java.util.Locale;
/**
 * This class provides a SOM neural network
 *
 * @author Juan C. Burguillo Rial
 * @version 1.0
 */
public class SOM implements GameCONS {
    // Reemplazar con valores adecuados o modificar según tu proyecto
    private static final int iMapSize = 100; // Ejemplo, ajustar según sea necesario
    private static final int iOUTPUT_VERBOSE = 1; // Ejemplo, ajustar según sea necesario

    private int iGridSide; // Side of the SOM 2D grid
    private int iCellSize; // Size in pixels of a SOM neuron in the grid
    private int[][] iNumTimesBMU; // Number of times a cell has been a BMU
    private int[] iBMU_Pos = new int[2]; // BMU position in the grid

    private int iInputSize; // Size of the input vector
    private int iRadio; // BMU radio to modify neurons
    private double dLearnRate = 1.0; // Learning rate for this SOM
    private double dDecLearnRate = 0.999; // Used to reduce the learning rate
    private double[] dBMU_Vector = null; // BMU state
    private double[][][] dGrid; // SOM square grid + vector state per neuron

    /**
     * This is the class constructor that creates the 2D SOM grid
     * 
     * @param iSideAux       the square side
     * @param iInputSizeAux  the dimensions for the input data
     */
    public SOM(int iSideAux, int iInputSizeAux) {
        iInputSize = iInputSizeAux;
        iGridSide = iSideAux;
        iCellSize = iMapSize / iGridSide; // Ajustar según el contexto del proyecto
        iRadio = iGridSide / 10;
        dBMU_Vector = new double[iInputSize];
        dGrid = new double[iGridSide][iGridSide][iInputSize];
        iNumTimesBMU = new int[iGridSide][iGridSide];

        vResetValues();
    }

    public void vResetValues() {
        dLearnRate = 1.0;
        iNumTimesBMU = new int[iGridSide][iGridSide];
        iBMU_Pos[0] = -1;
        iBMU_Pos[1] = -1;

        for (int i = 0; i < iGridSide; i++) // Initializing the SOM grid/network
            for (int j = 0; j < iGridSide; j++)
                for (int k = 0; k < iInputSize; k++)
                    dGrid[i][j][k] = Math.random();
    }

    // ... Resto del código ...

    // Asegúrate de incluir los demás métodos y lógica necesarios




  public double[] dvGetBMU_Vector() {
      return dBMU_Vector;
  }

  public double dGetLearnRate() {
      return dLearnRate;
  }

  public double[] dGetNeuronWeights(int x, int y) {
      return dGrid[x][y];
  }

  /**
   * This is the main method that returns the coordinates of the BMU and trains its neighbors
   * 
   * @param dmInput  contains the input vector
   * @param bTrain   training or testing phases
   * 
   * @return the coordinates of the BMU as a string
   */
  public String sGetBMU(double[] dmInput, boolean bTrain) {
      int x = 0, y = 0;
      double dNorm, dNormMin = Double.MAX_VALUE;
      String sReturn;

      // Assuming MainWindow.iOutputMode and iOUTPUT_VERBOSE are defined elsewhere
      if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
          System.out.print("\n\n\n\n-------------------- SOM -------------------\ndmInput: \t");
          for (int k = 0; k < iInputSize; k++) {
              System.out.print("  " + String.format(Locale.ENGLISH, "%.5f", dmInput[k]));
          }
      }

      // Finding the BMU
      for (int i = 0; i < iGridSide; i++) {
          for (int j = 0; j < iGridSide; j++) {
              dNorm = 0;
              for (int k = 0; k < iInputSize; k++) {
                  dNorm += Math.pow(dmInput[k] - dGrid[i][j][k], 2);
              }
              if (dNorm < dNormMin) {
                  dNormMin = dNorm;
                  x = i;
                  y = j;
              }
          }
      }

      if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
          System.out.print("\ndBMU_pre: \t");
          for (int k = 0; k < iInputSize; k++) {
              System.out.print("  " + String.format(Locale.ENGLISH, "%.5f", dGrid[x][y][k]));
          }
      }

      if (bTrain) {
          int xAux, yAux;
          for (int v = -iRadio; v <= iRadio; v++) {
              for (int h = -iRadio; h <= iRadio; h++) {
                  xAux = (x + h + iGridSide) % iGridSide;
                  yAux = (y + v + iGridSide) % iGridSide;

                  for (int k = 0; k < iInputSize; k++) {
                      dGrid[xAux][yAux][k] += dLearnRate * (dmInput[k] - dGrid[xAux][yAux][k]) / (1 + v * v + h * h);
                  }
              }
          }

          if (MainWindow.iOutputMode == iOUTPUT_VERBOSE) {
              System.out.print("\ndBMU_post: \t");
              for (int k = 0; k < iInputSize; k++) {
                  System.out.print("  " + String.format(Locale.ENGLISH, "%.5f", dGrid[x][y][k]));
              }
          }
      }

      sReturn = x + "," + y;
      iBMU_Pos[0] = x;
      iBMU_Pos[1] = y;
      dBMU_Vector = dGrid[x][y].clone();
      iNumTimesBMU[x][y]++;
      dLearnRate *= dDecLearnRate;

      return sReturn;
  }
} // from the class SOM


