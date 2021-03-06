package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.ListUtils;
import com.eriqaugustine.ocr.utils.MathUtils;

import magick.MagickImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to handle the functionality for PDC (Peripheral Direction Contributivities).
 * See papers in articels.
 */
public final class PDC {
   public static final int SCALE_SIZE = 64;

   private static final int NUM_LAYERS = 3;

   private static final int NUM_CARDINAL_SCAN_DIRECTIONS = 4;
   private static final int NUM_DIAGONAL_SCAN_DIRECTIONS = 4;

   /**
    * The deltas [row, col] for the different directions available to PDC.
    * Starts at 12 and moves clockwise by 1:30.
    */
   public static final int[][] PDC_DIRECTION_DELTAS = new int[][]{
      new int[]{-1, 0},
      new int[]{-1, 1},
      new int[]{0, 1},
      new int[]{1, 1},
      new int[]{0, 1},
      new int[]{1, -1},
      new int[]{0, -1},
      new int[]{-1, -1}
   };

   /**
    * This is a static-only class, don't construct.
    */
   private PDC() {
   }

   /**
    * Get the number of DCs that this will produce.
    * This is the number of base DCs (so before any grouping is done).
    * This will be equal to PDCInfo.numPoints().
    */
   public static int getNumDCs() {
      return
         // Cardinals
         SCALE_SIZE * NUM_CARDINAL_SCAN_DIRECTIONS * NUM_LAYERS +
         // Diagonals (if scale size is odd, will not be the same number as cardinals.
         NUM_DIAGONAL_SCAN_DIRECTIONS * (int)(Math.ceil(SCALE_SIZE / 2.0) * 2) * NUM_LAYERS;
   }

   /**
    * Run PDC on an image.
    * |image| must be already be binary.
    */
   public static PDCInfo pdc(MagickImage baseImage) throws Exception {
      MagickImage scaleImage = ImageUtils.scaleImage(baseImage, SCALE_SIZE, SCALE_SIZE);
      boolean[] discretePixels = Filters.discretizePixels(scaleImage);

      List<Integer> peripherals = new ArrayList<Integer>(SCALE_SIZE *
                                                         NUM_CARDINAL_SCAN_DIRECTIONS *
                                                         NUM_LAYERS);

      // Layers
      for (int i = 0; i < NUM_LAYERS; i++) {
         // Horizontal LTR
         ListUtils.append(peripherals, scan(discretePixels, SCALE_SIZE, i, true, true));

         // Horizontal RTL
         ListUtils.append(peripherals, scan(discretePixels, SCALE_SIZE, i, true, false));

         // Vertial Down
         ListUtils.append(peripherals, scan(discretePixels, SCALE_SIZE, i, false, true));

         // Vertical Up
         ListUtils.append(peripherals, scan(discretePixels, SCALE_SIZE, i, false, false));

         // Diagnals
         int half = (int)(Math.ceil(SCALE_SIZE / 2.0));
         int last = SCALE_SIZE - 1;

         // Top Left to Bottom Right
         peripherals.addAll(diagonalScan(discretePixels, SCALE_SIZE, i, 0, half, 0,
                                                                        0, half, 0,
                                                                        1, 1));

         // Top Right to Bottom Left
         peripherals.addAll(diagonalScan(discretePixels, SCALE_SIZE, i, last - half, last, 0,
                                                                        0, half, last,
                                                                        1, -1));

         // Bottom Left to Top Right
         peripherals.addAll(diagonalScan(discretePixels, SCALE_SIZE, i, 0, half, last,
                                                                        last - half, last, 0,
                                                                        -1, 1));

         // Bottom Right to Top Left
         peripherals.addAll(diagonalScan(discretePixels, SCALE_SIZE, i, last - half, last, last,
                                                                        last - half, last, last,
                                                                        -1, -1));
      }

      assert(peripherals.size() == getNumDCs());
      int[][] lengths = new int[peripherals.size()][];

      for (int i = 0; i < peripherals.size(); i++) {
         if (peripherals.get(i).intValue() == -1) {
            lengths[i] = null;
         } else {
            lengths[i] = dcLengths(discretePixels, SCALE_SIZE, peripherals.get(i).intValue());
         }
      }

      return new PDCInfo(baseImage, scaleImage,
                         NUM_LAYERS,
                         lengths, ListUtils.toIntArray(peripherals));
   }

   public static PDCInfo[] pdc(MagickImage[] images) throws Exception {
      PDCInfo[] rtn = new PDCInfo[images.length];
      for (int i = 0; i < images.length; i++) {
         rtn[i] = pdc(images[i]);
      }
      return rtn;
   }

   /**
    * Get the lengths that are the core components in the DC.
    */
   private static int[] dcLengths(boolean[] image, int imageWidth, int point) {
      int[] lengths = new int[PDC_DIRECTION_DELTAS.length];

      int baseRow = MathUtils.indexToRow(point, imageWidth);
      int baseCol = MathUtils.indexToCol(point, imageWidth);

      for (int delta = 0; delta < PDC_DIRECTION_DELTAS.length; delta++) {
         int length = 0;

         int row = baseRow + PDC_DIRECTION_DELTAS[delta][0];
         int col = baseCol + PDC_DIRECTION_DELTAS[delta][1];
         while (MathUtils.inBounds(row, col, imageWidth, image.length)) {
            length++;

            row += PDC_DIRECTION_DELTAS[delta][0];
            col += PDC_DIRECTION_DELTAS[delta][1];
         }

         lengths[delta] = length;
      }

      return lengths;
   }

   /**
    * Do a diagonal scan and get the peripheral points.
    * See scan().
    * The very short diagonals (for ex, the one length ones at the corners)
    * are not very useful. So, only the longest diagonals are used.
    * This is why we need all the additional parameters.
    */
   private static List<Integer> diagonalScan(boolean[] image,
                                             int imageWidth,
                                             int layerNumber,
                                             int colStart, int colStop, int baseRow,
                                             int rowStart, int rowStop, int baseCol,
                                             int rowDelta, int colDelta) {
      List<Integer> peripherals = new ArrayList<Integer>();

      int[][] startPoints = new int[colStop - colStart + rowStop - rowStart][];
      int count = 0;
      for (int col = colStart; col < colStop; col++) {
         startPoints[count] = new int[]{baseRow, col};
         count++;
      }
      for (int row = rowStart; row < rowStop; row++) {
         startPoints[count] = new int[]{row, baseCol};
         count++;
      }

      for (int[] startPoint : startPoints) {
         int diagonalRow = startPoint[0];
         int diagonalCol = startPoint[1];

         int currentLayer = 0;
         boolean inBody = false;
         boolean found = false;

         while (diagonalRow >= 0 && diagonalRow < image.length / imageWidth &&
                diagonalCol >= 0 && diagonalCol < imageWidth) {
            int index = MathUtils.rowColToIndex(diagonalRow, diagonalCol, imageWidth);

            if (image[index] && !inBody) {
               inBody = true;

               if (currentLayer == layerNumber) {
                  peripherals.add(new Integer(index));
                  found = true;
                  break;
               }
            } else if (!image[index] && inBody) {
               inBody = false;
               currentLayer++;
            }

            diagonalRow += rowDelta;
            diagonalCol += colDelta;
         }

         if (!found) {
            peripherals.add(new Integer(-1));
         }
      }

      return peripherals;
   }

   /**
    * Scan a direction and get all of the peripheral (edge) points.
    * |layerNumber| is the number of solid bodies to pass through before the final
    * peripheral edge. 0 means that it will pass through no bodies.
    * There will be a result for EVERY row/col that is scanned.
    * If a row/col has no peripheral point, then a -1 will be the result.
    */
   private static int[] scan(boolean[] image,
                             int imageWidth,
                             int layerNumber,
                             boolean horizontal,
                             boolean forward) {
      assert(layerNumber >= 0);

      // The end points should be out of bounds.
      int outerStart, outerEnd, outerDelta;
      int innerStart, innerEnd, innerDelta;
      int[] peripherals;

      if (horizontal) {
         // row
         outerStart = 0;
         outerEnd = image.length / imageWidth;
         outerDelta = 1;

         // col
         innerStart = forward ? 0 : imageWidth - 1;
         innerEnd = forward ? imageWidth : -1;
         innerDelta = forward ? 1 : -1;
      } else {
         // col
         outerStart = 0;
         outerEnd = imageWidth;
         outerDelta = 1;

         // row
         innerStart = forward ? 0 : image.length / imageWidth - 1;
         innerEnd = forward ? image.length / imageWidth : -1;
         innerDelta = forward ? 1 : -1;
      }

      peripherals = new int[outerEnd];

      for (int outer = outerStart; outer != outerEnd; outer += outerDelta) {
         int currentLayerCount = 0;
         boolean inBody = false;
         boolean found = false;

         for (int inner = innerStart; inner != innerEnd; inner += innerDelta) {
            int index = horizontal ? MathUtils.rowColToIndex(outer, inner, imageWidth) :
                                     MathUtils.rowColToIndex(inner, outer, imageWidth);

            if (image[index] && !inBody) {
               inBody = true;

               if (currentLayerCount == layerNumber) {
                  peripherals[outer] = index;
                  found = true;
                  break;
               }
            } else if (!image[index] && inBody) {
               inBody = false;
               currentLayerCount++;
            }
         }

         if (!found) {
            peripherals[outer] = -1;
         }
      }

      return peripherals;
   }
}
