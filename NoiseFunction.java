import java.util.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.text.DecimalFormat;
import java.io.FileWriter;
import java.io.IOException;

public class NoiseFunction {
    public static void generateMap(int[][] data) {
        try {
            int width = data[0].length;
            int height = data.length;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage imageBW = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            BufferedImage imageBWGated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = image.createGraphics();
            Graphics2D g2dBW = imageBW.createGraphics();
            Graphics2D g2dBWG = imageBWGated.createGraphics();
            int max = findMax(data);
            int min = findMin(data);
            int avg = findAvg(data);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double value = data[y][x];
                    // Linear scaling to color gradient (0 to 1)
                    double normalizedValue = value / max;

                    // Color gradient (blue to red)
                    int red = (int) (255 * normalizedValue);
                    int blue = 255 - red;
                    Color color = new Color(red, 0, blue);
                    Color colorBW = new Color(red, red, red);
                    Color colorBWG;
                    if (value >= avg) { colorBWG = new Color(255, 255, 255); }
                    else { colorBWG = new Color(0, 0, 0); }

                    g2d.setColor(color);
                    g2d.fillRect(x, y, 1, 1); // Draw a 1x1 pixel rectangle

                    g2dBW.setColor(colorBW);
                    g2dBW.fillRect(x, y, 1, 1);

                    g2dBWG.setColor(colorBWG);
                    g2dBWG.fillRect(x, y, 1, 1);
                }
            }

            g2d.dispose();
            g2dBW.dispose();
            g2dBWG.dispose();

            File outputFile = new File("heatmap.png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("Heatmap image saved to heatmap.png");

            File outputFileBW = new File("heatmapBW.png");
            ImageIO.write(imageBW, "png", outputFileBW);
            System.out.println("B&W Heatmap image saved to heatmapBW.png");

            File outputFileBWG = new File("heatmapBWG.png");
            ImageIO.write(imageBWGated, "png", outputFileBWG);
            System.out.println("B&W Gated Heatmap image saved to heatmapBWG.png");


        } catch(IOException e) {
            System.out.println("Exception occured :" + e.getMessage());
        }
    }

    private static int findMax(int[][] data) {
      int max = Integer.MIN_VALUE;
      for (int[] row : data) {
          for (int value : row) {
              max = Math.max(max, value);
          }
      }
      return max;
    }

    private static int findMin(int[][] data) {
        int min = Integer.MAX_VALUE;
      for (int[] row : data) {
          for (int value : row) {
              min = Math.min(min, value);
          }
      }
      return min;
    }

    private static int findAvg(int[][] data) {
        int avg = 0;
        for (int[] row : data) {
            for (int value : row) {
                avg += value;
            }
        }
        avg /= data.length * data[0].length;
        return avg;
    }

    public static int[][] genNoise(int l, int w, double chainFreq, double jaggedness, boolean scrambled) {
        // set up array
        double baseChainLen = 16/chainFreq;
        int[][] noise = new int[l][w];
        for (int i = 0; i < l; i++) { Arrays.fill(noise[i], 0); }

        // do noise
        int numChains = (int)(Math.sqrt(l*w) * chainFreq);
        int chainLen = (int)(Math.sqrt(l*w) * baseChainLen);
        for (int i = 0; i < numChains; i++) {
            int rL = (int)(Math.random() * l);
            int rW = (int)(Math.random() * w);
            noise[rL][rW]++;
            int curChainLen = (int)((Math.random()*0.5 + 0.75) * chainLen); // 0.75-1.25 * chainLen

            for (int j = 0; j < curChainLen; j++) {
                int surroundingAvg = -1;
                while (noise[rL][rW] > (1+jaggedness)*surroundingAvg) {
                    rL = (int)(Math.random() * 3) - 1 + rL;
                    if (rL <= -1) { rL = 0; }
                    if (rL >= l) { rL = l-1; }
                    rW = (int)(Math.random() * 3) - 1 + rW;
                    if (rW <= -1) { rW = 0; }
                    if (rW >= w) { rW = w-1; }

                    surroundingAvg = 0;
                    int used = 0;
                    for (int r = -1; r <= 1; r++) {
                        for (int c = -1; c <= 1; c++) {
                            int curR = r+rL;
                            int curW = c+rW;
                            if (!(r == 0 && c == 0) && curR < l && curR > -1 && curW < w && curW > -1) {
                                surroundingAvg += noise[curR][curW];
                                used++;
                            }
                        }
                    }
                    surroundingAvg /= used;
                }
                noise[rL][rW]++;
            }
        }

        if (scrambled) {
            int[][] scrNoise = new int[l][w];
            for (int i = 0; i < l; i++) { Arrays.fill(scrNoise[i], -1); }

            for (int r = 0; r < l; r++) {
                for (int c = 0; c < w; c++) {
                    int cur = noise[r][c];
                    int randR = (int)(Math.random() * l);
                    int randW = (int)(Math.random() * w);
                    while (scrNoise[randR][randW] != -1) {
                        randR = (int)(Math.random() * l);
                        randW = (int)(Math.random() * w);
                    }
                    scrNoise[randR][randW] = cur;
                }
            }
            noise = scrNoise;
        }

        return noise;
    }

    public static String hueToAnsiCode(double hue) { // just helps with interpretation in noise
        float[] hsb = {(float)hue, (float)0.8, (float)0.8};
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return String.format("\033[38;2;%d;%d;%dm", red, green, blue);
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("L: ");
        int l = input.nextInt();
        System.out.print("W: ");
        int w = input.nextInt();
        System.out.print("chainFreq: ");
        double chainFreq = input.nextDouble();
        System.out.print("jaggedness: ");
        double jaggedness = input.nextDouble();
        System.out.print("scrambled: ");
        boolean scrambled = input.nextBoolean();
        int startTime = (int)System.currentTimeMillis();
        int[][] noise = genNoise(l, w, chainFreq, jaggedness, scrambled);
        
        /*
        int red;
        int green;
        int blue;

        String resetCode = "\033[0m"; // Resets text color to default

        int maxVal = 0;
        int minVal = Integer.MAX_VALUE;
        for (int i = 0; i < noise.length; i++) {
            for (int j = 0; j < noise[0].length; j++) {
                if (noise[i][j] > maxVal) { maxVal = noise[i][j]; }
                if (noise[i][j] < minVal) { minVal = noise[i][j]; }
            }
        }

        for (int i = 0; i < noise.length; i++) {
            for (int j = 0; j < noise[0].length; j++) {
                double hue = 0.8 - (((noise[i][j] - minVal) / (double)(maxVal - minVal)) * 0.75);
                String colorCode = hueToAnsiCode(hue);
                System.out.printf(colorCode + "%3d" + resetCode, noise[i][j]);
            }
            System.out.println();
        }
        */

        /*
        int[] valueSpread = new int[maxVal+1];
        for (int i = 0; i < noise.length; i++) {
            for (int j = 0; j < noise[0].length; j++) {
                valueSpread[noise[i][j]]++;
            }
        }
        for (int i = 0; i <= maxVal; i++) {
            System.out.println(i + ": " + valueSpread[i]);
        }
        */

        generateMap(noise);
        int endTime = (int)System.currentTimeMillis();
        int totalTime = (endTime - startTime);
        System.out.println("Runtime: " + totalTime + " ms");

        
        int maxVal = 0;
        int minVal = Integer.MAX_VALUE;
        for (int i = 0; i < noise.length; i++) {
            for (int j = 0; j < noise[0].length; j++) {
                if (noise[i][j] > maxVal) { maxVal = noise[i][j]; }
                if (noise[i][j] < minVal) { minVal = noise[i][j]; }
            }
        }

        try {
            FileWriter writer = new FileWriter("100samples/sample1.txt");
            for (int i = 0; i < noise.length; i++) {
                for (int j = 0; j < noise[0].length; j++) {
                    double current = (double)(noise[i][j]-minVal)/(maxVal-minVal);
                    current = (double)Math.round(current*1000) / 1000.0;
                    String toWrite = current + ",\n";
                    writer.write(toWrite);
                }
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }



    }
}