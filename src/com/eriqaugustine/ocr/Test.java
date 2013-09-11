package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.pdc.PDCClassifier;
import com.eriqaugustine.ocr.utils.CharacterUtils;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import java.awt.GraphicsEnvironment;

/**
 * A general testing driver.
 */
public class Test {
   public static final String HIRAGANA = "あいうえお" +
                                         "かがきぎくぐけげこご" +
                                         "さざしじすずせぜそぞ" +
                                         "ただちぢつづてでとど" +
                                         "なにぬねの" +
                                         "はばぱひびぴふぶぷへべぺほぼぽ" +
                                         "まみむめも" +
                                         "やゆよ" +
                                         "らりるれろ" +
                                         "わゐゑをん";

   public static final String KATAKANA = "アイウエオ" +
                                         "カガキギクグケゲコゴ" +
                                         "サザシジスズセゼソゾ" +
                                         "タダチヂツヅテデトド" +
                                         "ナニヌネノ" +
                                         "ハバパヒビピフブプヘベペホボポ" +
                                         "マミムメモ" +
                                         "ヤユヨ" +
                                         "ラリルレロ" +
                                         "ワヰヱヲン";

   public static void main(String[] args) throws Exception {
      //singleFontGenTest();
      //multiFontGenTest();
      //imageBreakdown();
      //densityComparisonTest();
      //strokeComparisonTest();
      pdcTest();
   }

   public static void strokeComparisonTest() throws Exception {
      String alphabet = HIRAGANA + KATAKANA;

      //TEST
      //PDCClassifier classy = new PDCClassifier(CharacterImage.generateFontImages(alphabet),
      //                                         alphabet, false);
      PDCClassifier classy = new PDCClassifier(CharacterImage.generateFontImages(HIRAGANA),
                                               HIRAGANA, false);

      //ImageInfo info = new ImageInfo("testImages/1Text.png");
      ImageInfo info = new ImageInfo("testImages/hiragana.png");
      MagickImage baseImage = new MagickImage(info);

      MagickImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);

            System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            //TEST
            //CharacterImage.getStrokes(gridTextImage);
            System.out.println("Classify (" + row + ", " + col + "): " +
                               classy.classify(gridTextImage));
         }
      }
   }

   public static void pdcTest() throws Exception {
      String alphabet = HIRAGANA;

      PDCClassifier classy = new PDCClassifier(CharacterImage.generateFontImages(alphabet),
                                               alphabet, false);
                                               // alphabet, true);

      // Not exactly hiragana.
      String characters = "あいうえおかきくけこさしすせそたちつてとなにぬねの" +
                          "はひふへほまみむめもやわゆんよらりるれろ";

      ImageInfo info = new ImageInfo("testImages/partHiragana.png");
      MagickImage baseImage = new MagickImage(info);

      int count = 0;
      int hits = 0;

      MagickImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);

            // System.out.println(ImageUtils.asciiImage(gridTextImage) + "\n-\n");

            String prediction = classy.classify(gridTextImage);
            System.out.println(String.format("Classify (%d, %d)[%s]: %s",
                                             row, col,
                                             "" + characters.charAt(count),
                                             prediction));

            if (prediction.equals("" + characters.charAt(count))) {
               hits++;
            }

            count++;
         }
      }

      System.err.println("Hits: " + hits + " / " + count + " (" + ((double)hits / count) + ")");
   }

   public static void densityComparisonTest() throws Exception {
      String alphabet = HIRAGANA + KATAKANA;

      ImageInfo info = new ImageInfo("testImages/2Text.png");
      MagickImage baseImage = new MagickImage(info);

      double[][][] fontDensityMaps =
         CharacterImage.getFontDensityMaps(alphabet, 4, 4);

      MagickImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);

            double[][] densityMap = CharacterImage.getDensityMap(gridTextImage, 4, 4);

            if (densityMap == null) {
               System.out.println(String.format("(%d, %d) -> <space>", row, col));
            } else {
               int guess = bestMatch(fontDensityMaps, densityMap);
               System.out.println(String.format("(%d, %d) -> %c",
                                                row, col, alphabet.charAt(guess)));
            }
         }
      }
   }

   private static int bestMatch(double[][][] haystack, double[][] needle) {
      int bestGuess = 0;
      double bestDistance = Double.MAX_VALUE;

      for (int i = 0; i < haystack.length; i++) {
         double dist = CharacterImage.densityMapDistance(haystack[i], needle);

         if (dist < bestDistance) {
            bestGuess = i;
            bestDistance = dist;
         }
      }

      return bestGuess;
   }

   public static void singleFontGenTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "fontGen");

      MagickImage image = null;

      int current = (int)'あ';
      int count = 0;
      do {
         // System.err.println(count + " -- " + (char)current + " (" + current + ")");

         image = CharacterUtils.generateCharacter((char)current, true);
         image.setFileName(String.format("%s/char-%03d-%c.png",
                                         outDirectory,
                                         count,
                                         (char)current));
         image.writeImage(new ImageInfo());

         count++;
         current++;
      } while ((char)(current - 1) != 'ん');
   }

   public static void multiFontGenTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "multiFontGen");

      MagickImage image = null;

      for (String fontFamily : CharacterUtils.FONTS) {
         for (int i = 0; i < HIRAGANA.length(); i++) {
            // System.err.println(count + " -- " + (char)current + " (" + current + ")");
            //TEST
            char character = 'ど';
            // char character = HIRAGANA.charAt(i);

            image = CharacterUtils.generateCharacter(character, true,
                                                     CharacterUtils.DEFAULT_FONT_SIZE,
                                                     fontFamily);
            image.setFileName(String.format("%s/char-%s-%03d-%c.png",
                                            outDirectory,
                                            fontFamily,
                                            i,
                                            character));
            image.writeImage(new ImageInfo());

            //TEST
            break;
         }
      }
   }

   public static void imageBreakdown() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "blob");
      ImageInfo info = new ImageInfo("testImages/page.png");
      // ImageInfo info = new ImageInfo("testImages/2Text.png");
      // ImageInfo info = new ImageInfo("testImages/small.png");

      MagickImage baseImage = new MagickImage(info);
      baseImage.setFileName(outDirectory + "/test00-base.png");
      baseImage.writeImage(info);

      MagickImage bubbles = BubbleDetection.fillBubbles(baseImage);
      bubbles.setFileName(outDirectory + "/test01-bubbles.png");
      bubbles.writeImage(info);

      int count = 0;
      MagickImage[] bubbleImages = BubbleDetection.extractBubbles(baseImage);
      for (MagickImage bubbleImage : bubbleImages) {
         bubbleImage.setFileName(
            String.format("%s/test02-bubbles-%02d-0.png", outDirectory, count));
         bubbleImage.writeImage(info);

         MagickImage[][] gridTextImages = TextImage.gridBreakup(bubbleImage);
         for (int row = 0; row < gridTextImages.length; row++) {
            for (int col = 0; col < gridTextImages[row].length; col++) {
               MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);
               gridTextImage.setFileName(
                  String.format("%s/test02-bubbles-%02d-gridTexts-%02d-%02d.png",
                                outDirectory, count, row, col));
               gridTextImage.writeImage(info);

               double[][] densityMap =
                  CharacterImage.getDensityMap(gridTextImage, 3, 3);

               if (densityMap == null) {
                  continue;
               }

               System.out.println(row + ", " + col);
               for (int i = 0; i < densityMap.length; i++) {
                  System.out.print(" ");
                  for (int j = 0; j < densityMap[i].length; j++) {
                     System.out.print(String.format("  %6.4f", densityMap[i][j]));
                  }
                  System.out.println();
               }
            }
         }

         count++;
      }
   }
}
