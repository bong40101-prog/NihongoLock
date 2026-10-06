package com.jk.nihongolock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 28));
        return l;
    }

    public static TextView text(Context c, String value, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.15f);
        return t;
    }

    public static TextView title(Context c, String value) {
        TextView t = text(c, value, 28f, Color.WHITE);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, dp(c, 14));
        return t;
    }

    public static Button button(Context c, String label) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(16f);
        b.setAllCaps(false);
        b.setMinHeight(dp(c, 54));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(27, 29, 34));
        bg.setCornerRadius(dp(c, 18));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 10));
        card.setLayoutParams(lp);
        return card;
    }

    public static TextView centeredText(Context c, String value, float sp) {
        TextView t = text(c, value, sp, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static void darkSystemBars(Activity a) {
        a.getWindow().setStatusBarColor(Color.rgb(16,17,20));
        a.getWindow().setNavigationBarColor(Color.rgb(16,17,20));
        a.getWindow().getDecorView().setBackgroundColor(Color.rgb(16,17,20));
    }
}
