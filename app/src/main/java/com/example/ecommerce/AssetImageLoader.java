package com.example.ecommerce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

public final class AssetImageLoader {
    private AssetImageLoader() {
    }

    public static Bitmap load(Context context, String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String normalizedPath = imagePath
                .replace('\\', '/')
                .replaceFirst("^/+", "")
                .replaceFirst("^assets/", "");
        try (InputStream input = context.getAssets().open(normalizedPath)) {
            return BitmapFactory.decodeStream(input);
        } catch (IOException error) {
            if (normalizedPath.equals(imagePath)) {
                return null;
            }
            try (InputStream input = context.getAssets().open(imagePath)) {
                return BitmapFactory.decodeStream(input);
            } catch (IOException fallbackError) {
                return null;
            }
        }
    }
}
