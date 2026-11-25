package com.example.ecommerce;

import com.example.ecommerce.data.DatabaseHelper;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public final class ReviewEditorDialog {
    private ReviewEditorDialog() {
    }

    public static void show(AppCompatActivity activity, DatabaseHelper databaseHelper,
                            long productId, long orderId, String productName,
                            int existingRating, String existingComment, Runnable onSaved) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_rate_product, null);
        ((TextView) view.findViewById(R.id.tvRateProductName)).setText(productName);
        RatingBar ratingBar = view.findViewById(R.id.rbRateStars);
        EditText comment = view.findViewById(R.id.etReviewComment);
        if (existingRating > 0) {
            ratingBar.setRating(existingRating);
            comment.setText(existingComment == null ? "" : existingComment);
        }

        boolean editing = existingRating > 0;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(editing
                        ? R.string.review_editor_edit_title
                        : R.string.review_editor_title)
                .setView(view)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(editing
                        ? R.string.review_editor_save
                        : R.string.review_editor_submit, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(button -> {
                int rating = Math.max(1, Math.round(ratingBar.getRating()));
                boolean saved = databaseHelper.saveReview(productId, orderId, rating,
                        comment.getText().toString());
                if (!saved) {
                    Toast.makeText(activity, R.string.review_save_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(activity, R.string.review_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (onSaved != null) {
                    onSaved.run();
                }
            });
        });
        dialog.show();
    }
}
