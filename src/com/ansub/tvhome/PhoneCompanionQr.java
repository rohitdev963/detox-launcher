package com.ansub.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small local QR encoder for the companion's one-time LAN pairing URL (QR version 3-L, byte mode). */
final class PhoneCompanionQr {
    private static final int SIZE = 29;
    private static final int DATA_CODEWORDS = 55;
    private static final int ECC_CODEWORDS = 15;

    private PhoneCompanionQr() {}

    static Bitmap make(String text, int requestedPixels) {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        if (payload.length > 53) return null;
        boolean[][] modules = encode(payload);
        int quiet = 4, modulesWithQuiet = SIZE + quiet * 2;
        int scale = Math.max(1, requestedPixels / modulesWithQuiet);
        Bitmap bitmap = Bitmap.createBitmap(modulesWithQuiet * scale, modulesWithQuiet * scale, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(); paint.setColor(Color.BLACK); paint.setAntiAlias(false);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            if (modules[y][x]) canvas.drawRect((x + quiet) * scale, (y + quiet) * scale,
                    (x + quiet + 1) * scale, (y + quiet + 1) * scale, paint);
        }
        return bitmap;
    }

    private static boolean[][] encode(byte[] payload) {
        List<Integer> bits = new ArrayList<>();
        append(bits, 0x4, 4); // byte mode
        append(bits, payload.length, 8);
        for (byte b : payload) append(bits, b & 255, 8);
        int capacity = DATA_CODEWORDS * 8;
        for (int i = 0; i < Math.min(4, capacity - bits.size()); i++) bits.add(0);
        while ((bits.size() & 7) != 0) bits.add(0);
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < bits.size(); i += 8) {
            int value = 0; for (int j = 0; j < 8; j++) value = (value << 1) | bits.get(i + j);
            data.add(value);
        }
        for (int pad = 0; data.size() < DATA_CODEWORDS; pad++) data.add((pad & 1) == 0 ? 0xEC : 0x11);
        int[] divisor = reedSolomonDivisor(ECC_CODEWORDS);
        int[] remainder = reedSolomonRemainder(data, divisor);
        List<Integer> codewords = new ArrayList<>(DATA_CODEWORDS + ECC_CODEWORDS);
        codewords.addAll(data);
        for (int value : remainder) codewords.add(value);

        boolean[][] matrix = new boolean[SIZE][SIZE], function = new boolean[SIZE][SIZE];
        finder(matrix, function, 0, 0);
        finder(matrix, function, SIZE - 7, 0);
        finder(matrix, function, 0, SIZE - 7);
        for (int i = 8; i < SIZE - 8; i++) {
            setFunction(matrix, function, i, 6, (i & 1) == 0);
            setFunction(matrix, function, 6, i, (i & 1) == 0);
        }
        alignment(matrix, function, 22, 22);
        reserveFormat(function);
        placeData(matrix, function, codewords);
        applyMask0(matrix, function);
        drawFormat(matrix, function);
        return matrix;
    }

    private static void append(List<Integer> bits, int value, int count) {
        for (int i = count - 1; i >= 0; i--) bits.add((value >>> i) & 1);
    }

    private static void finder(boolean[][] matrix, boolean[][] function, int left, int top) {
        for (int dy = -1; dy <= 7; dy++) for (int dx = -1; dx <= 7; dx++) {
            int x = left + dx, y = top + dy;
            if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) continue;
            boolean inside = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6;
            boolean dark = inside && (dx == 0 || dx == 6 || dy == 0 || dy == 6
                    || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
            setFunction(matrix, function, x, y, dark);
        }
    }

    private static void alignment(boolean[][] matrix, boolean[][] function, int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++)
            setFunction(matrix, function, cx + dx, cy + dy,
                    Math.max(Math.abs(dx), Math.abs(dy)) != 1);
    }

    private static void setFunction(boolean[][] matrix, boolean[][] function, int x, int y, boolean dark) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return;
        matrix[y][x] = dark; function[y][x] = true;
    }

    private static void reserveFormat(boolean[][] function) {
        for (int i = 0; i <= 5; i++) function[i][8] = true;
        function[7][8] = true; function[8][8] = true; function[8][7] = true;
        for (int i = 9; i < 15; i++) function[8][14 - i] = true;
        for (int i = 0; i < 8; i++) function[8][SIZE - 1 - i] = true;
        for (int i = 8; i < 15; i++) function[SIZE - 15 + i][8] = true;
        function[SIZE - 8][8] = true;
    }

    private static void drawFormat(boolean[][] matrix, boolean[][] function) {
        int data = (1 << 3); // low error correction level, mask 0
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ (((rem >>> 9) & 1) * 0x537);
        int format = ((data << 10) | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) matrix[i][8] = bit(format, i);
        matrix[7][8] = bit(format, 6); matrix[8][8] = bit(format, 7); matrix[8][7] = bit(format, 8);
        for (int i = 9; i < 15; i++) matrix[8][14 - i] = bit(format, i);
        for (int i = 0; i < 8; i++) matrix[8][SIZE - 1 - i] = bit(format, i);
        for (int i = 8; i < 15; i++) matrix[SIZE - 15 + i][8] = bit(format, i);
        matrix[SIZE - 8][8] = true;
    }

    private static boolean bit(int value, int index) { return ((value >>> index) & 1) != 0; }

    private static void placeData(boolean[][] matrix, boolean[][] function, List<Integer> codewords) {
        int bitIndex = 0; boolean upward = true;
        for (int right = SIZE - 1; right >= 1; right -= 2) {
            if (right == 6) right--;
            for (int vert = 0; vert < SIZE; vert++) {
                int y = upward ? SIZE - 1 - vert : vert;
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    if (function[y][x]) continue;
                    boolean dark = bitIndex < codewords.size() * 8
                            && ((codewords.get(bitIndex >>> 3) >>> (7 - (bitIndex & 7))) & 1) != 0;
                    // Mask is applied once, after all data modules have been placed.
                    matrix[y][x] = dark; bitIndex++;
                }
            }
            upward = !upward;
        }
    }

    private static void applyMask0(boolean[][] matrix, boolean[][] function) {
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++)
            if (!function[y][x] && ((x + y) & 1) == 0) matrix[y][x] = !matrix[y][x];
    }

    private static int[] reedSolomonDivisor(int degree) {
        int[] result = new int[degree]; result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = multiply(result[j], root);
                if (j + 1 < degree) result[j] ^= result[j + 1];
            }
            root = multiply(root, 2);
        }
        return result;
    }

    private static int[] reedSolomonRemainder(List<Integer> data, int[] divisor) {
        int[] result = new int[divisor.length];
        for (int value : data) {
            int factor = value ^ result[0];
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) result[i] ^= multiply(divisor[i], factor);
        }
        return result;
    }

    private static int multiply(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ (((z >>> 7) & 1) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 255;
    }
}
