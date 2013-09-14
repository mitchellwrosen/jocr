package com.eriqaugustine.ocr.utils;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Some utilities for dealing with fonts.
 * Note that both True Type Fonts (TTF) and Open Type Fonts (OTF) can be loaded with
 * Font.TRUETYPE_FONT.
 */
public class FontUtils {
   public static final int DEFAULT_FONT_SIZE = 24;

   public static final String[] FONTS = new String[]{
      "Baekmuk Batang",
      "IPAGothic",
      "IPAMincho",
      "NanumMyeongjo",
      "RyuminStd-Bold-KO",
      "RyuminStd-Heavy-KO",
      "MidashiMinPr5-MA31",
   };

   public static void main(String[] args) {
      String text = "アンタにこのセンスは わからないわ";

      /*
      for (String font : FONTS) {
         showFontDialog(String.format("%s:\n%s", font, text), font);
      }
      */

      String[] fontPaths = getLocalFontPaths();
      for (String fontPath : fontPaths) {
         System.out.println(fontPath);

         String fontName = getFontName(fontPath);
         registerFont(fontPath);
         showFontDialog(text, fontName);
      }
   }

   /**
    * Register all the project's local fonts.
    */
   public static void registerLocalFonts() {
      String[] fontPaths = getLocalFontPaths();
      for (String fontPath : fontPaths) {
         registerFont(fontPath);
      }
   }

   /**
    * Get the fonts installed in the project's font directory.
    * Currently, only ttf and otf fonts are accepted.
    */
   public static String[] getLocalFontPaths() {
      String fontDirName = Props.getString("FONT_DIR", "fonts");
      File fontDir = new File(fontDirName);

      String[] fontFiles = fontDir.list(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            String lName = name.toLowerCase();
            return lName.endsWith(".ttf") || lName.endsWith(".otf");
         }
      });

      for (int i = 0; i < fontFiles.length; i++) {
         File fontFile = new File(fontDirName + File.separator + fontFiles[i]);
         fontFiles[i] = fontFile.getAbsolutePath();
      }

      return fontFiles;
   }

   public static String getFontName(String fontPath) {
      String fontName = null;

      try {
         Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));
         fontName = font.getName();
      } catch (Exception ex) {
         System.err.println("Unable to create font.");
         ex.printStackTrace(System.err);
      }

      return fontName;
   }

   public static void showFontDialog(String str, String fontName) {
      showFontDialog(str, fontName, DEFAULT_FONT_SIZE);
   }

   public static void showFontDialog(String str, String fontName, int fontSize) {
      Font font = new Font(fontName, Font.PLAIN, 24);

      JLabel label = new JLabel(str);
      label.setFont(font);
      JOptionPane.showMessageDialog(null, label);
   }

   public static boolean registerFont(String fontPath) {
      try {
         GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
         return ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontPath)));
      } catch (Exception ex) {
         // TODO(eriq): Better logging.
         ex.printStackTrace(System.err);
         return false;
      }
   }

   public static String[] getAvailableFonts() {
      GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
      return env.getAvailableFontFamilyNames();
   }

   public static void printAvailableFonts() {
      String[] fonts = getAvailableFonts();
      System.out.println("Available Fonts:");
      for (String font : fonts) {
         System.out.println("   " + font);
      }
   }
}
