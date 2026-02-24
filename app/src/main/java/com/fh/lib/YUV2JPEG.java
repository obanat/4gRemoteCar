package com.fh.lib;

/* renamed from: com.fh.lib.YUV2JPEG */
/* loaded from: classes.dex */
public class YUV2JPEG {
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0079, code lost:
        if (r1 == null) goto L16;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static int convert(byte[] r9, java.lang.String r10, int r11, int r12, int r13) throws java.io.IOException {
        /*
            r13 = 1
            r0 = 0
            r1 = 0
            android.graphics.YuvImage r8 = new android.graphics.YuvImage     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r4 = 17
            r7 = 0
            r2 = r8
            r3 = r9
            r5 = r11
            r6 = r12
            r2.<init>(r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            java.io.ByteArrayOutputStream r9 = new java.io.ByteArrayOutputStream     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r9.<init>()     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            android.graphics.Rect r2 = new android.graphics.Rect     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r2.<init>(r0, r0, r11, r12)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r3 = 80
            r8.compressToJpeg(r2, r3, r9)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            byte[] r2 = r9.toByteArray()     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            byte[] r3 = r9.toByteArray()     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            int r3 = r3.length     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            android.graphics.Bitmap r2 = android.graphics.BitmapFactory.decodeByteArray(r2, r0, r3)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r4 = 3
            java.lang.Object[] r4 = new java.lang.Object[r4]     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            long r5 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            java.lang.Long r5 = java.lang.Long.valueOf(r5)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r4[r0] = r5     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            java.lang.String r11 = java.lang.String.valueOf(r11)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r4[r13] = r11     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r11 = 2
            java.lang.String r12 = java.lang.String.valueOf(r12)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r4[r11] = r12     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            java.lang.String r10 = java.lang.String.format(r10, r4)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            r3.<init>(r10)     // Catch: java.lang.Throwable -> L6c java.io.IOException -> L6e java.io.FileNotFoundException -> L75
            android.graphics.Bitmap$CompressFormat r10 = android.graphics.Bitmap.CompressFormat.JPEG     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L66 java.io.FileNotFoundException -> L69
            r11 = 100
            r2.compress(r10, r11, r3)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L66 java.io.FileNotFoundException -> L69
            byte[] r9 = r9.toByteArray()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L66 java.io.FileNotFoundException -> L69
            r3.write(r9)     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L66 java.io.FileNotFoundException -> L69
            r3.close()     // Catch: java.lang.Throwable -> L63 java.io.IOException -> L66 java.io.FileNotFoundException -> L69
            r3.close()
            goto L7f
        L63:
            r9 = move-exception
            r1 = r3
            goto L80
        L66:
            r9 = move-exception
            r1 = r3
            goto L6f
        L69:
            r9 = move-exception
            r1 = r3
            goto L76
        L6c:
            r9 = move-exception
            goto L80
        L6e:
            r9 = move-exception
        L6f:
            r9.printStackTrace()     // Catch: java.lang.Throwable -> L6c
            if (r1 == 0) goto L7e
            goto L7b
        L75:
            r9 = move-exception
        L76:
            r9.printStackTrace()     // Catch: java.lang.Throwable -> L6c
            if (r1 == 0) goto L7e
        L7b:
            r1.close()
        L7e:
            r13 = 0
        L7f:
            return r13
        L80:
            if (r1 == 0) goto L85
            r1.close()
        L85:
            throw r9
        */
        throw new UnsupportedOperationException("Method not decompiled: com.p005fh.lib.YUV2JPEG.convert(byte[], java.lang.String, int, int, int):int");
    }
}
