package com.example.ecommerce.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DatabaseUtils {
    private DatabaseUtils() {
    }

    public static String placeholders(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("?");
        }
        return builder.toString();
    }

    public static String[] cartSelectionArgs(long userId, long[] cartIds) {
        String[] args = new String[cartIds.length + 1];
        args[0] = String.valueOf(userId);
        for (int i = 0; i < cartIds.length; i++) {
            args[i + 1] = String.valueOf(cartIds[i]);
        }
        return args;
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }
}
