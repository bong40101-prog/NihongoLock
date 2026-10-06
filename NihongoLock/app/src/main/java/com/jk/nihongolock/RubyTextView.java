package com.jk.nihongolock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** A small dependency-free text view that draws furigana above kanji. */
public class RubyTextView extends View {
    private static final float RUBY_SCALE = 0.48f;
    private static final float RUBY_GAP_DP = 1.5f;

    private final TextPaint basePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final TextPaint rubyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final float density;
    private String source = "";
    private List<Token> tokens = new ArrayList<>();
    private List<Line> lines = new ArrayList<>();
    private int textColor = Color.WHITE;
    private float baseTextSizeSp = 20f;
    private int measuredContentWidth;

    public RubyTextView(Context context) {
        this(context, null);
    }

    public RubyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        basePaint.setColor(textColor);
        rubyPaint.setColor(textColor);
        setWillNotDraw(false);
    }

    public void setText(CharSequence text) {
        source = text == null ? "" : text.toString();
        tokens = parse(Furigana.annotate(source));
        requestLayout();
        invalidate();
    }

    public CharSequence getText() {
        return source;
    }

    public void setTextSize(float sizeSp) {
        baseTextSizeSp = sizeSp;
        basePaint.setTextSize(sp(sizeSp));
        rubyPaint.setTextSize(sp(sizeSp * RUBY_SCALE));
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = color;
        basePaint.setColor(color);
        rubyPaint.setColor(color);
        invalidate();
    }

    public void setTextPadding(int left, int top, int right, int bottom) {
        setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    public void setRubyButtonBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(93, 93, 96));
        bg.setCornerRadius(dp(4));
        setBackground(bg);
        setTextPadding(14, 9, 14, 9);
        setMinimumHeight(dp(54));
        setClickable(true);
        setFocusable(true);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private float dp(float value) {
        return value * density;
    }

    private static final class Token {
        final String base;
        final String ruby;

        Token(String base, String ruby) {
            this.base = base;
            this.ruby = ruby;
        }

        boolean newline() {
            return "\n".equals(base);
        }
    }

    private static final class Line {
        final List<Token> tokens = new ArrayList<>();
        float width;
        boolean hasRuby;
    }

    private List<Token> parse(String encoded) {
        List<Token> out = new ArrayList<>();
        int index = 0;
        while (index < encoded.length()) {
            if (encoded.charAt(index) == '[') {
                int divider = encoded.indexOf('|', index + 1);
                int end = divider < 0 ? -1 : encoded.indexOf(']', divider + 1);
                if (end > divider) {
                    out.add(new Token(encoded.substring(index + 1, divider), encoded.substring(divider + 1, end)));
                    index = end + 1;
                    continue;
                }
            }
            int codePoint = encoded.codePointAt(index);
            String value = new String(Character.toChars(codePoint));
            out.add(new Token(value, null));
            index += Character.charCount(codePoint);
        }
        return out;
    }

    private List<Line> makeLines(int width) {
        List<Line> result = new ArrayList<>();
        Line current = new Line();
        float available = Math.max(1, width);
        for (Token token : tokens) {
            if (token.newline()) {
                result.add(current);
                current = new Line();
                continue;
            }
            float tokenWidth = tokenWidth(token);
            if (!current.tokens.isEmpty() && current.width + tokenWidth > available) {
                result.add(current);
                current = new Line();
            }
            current.tokens.add(token);
            current.width += tokenWidth;
            current.hasRuby |= token.ruby != null && !token.ruby.isEmpty();
        }
        if (!current.tokens.isEmpty() || result.isEmpty()) result.add(current);
        return result;
    }

    private float tokenWidth(Token token) {
        float baseWidth = basePaint.measureText(token.base);
        if (token.ruby == null || token.ruby.isEmpty()) return baseWidth;
        return Math.max(baseWidth, rubyPaint.measureText(token.ruby));
    }

    private int rubyHeight() {
        Paint.FontMetrics fm = rubyPaint.getFontMetrics();
        return Math.max(1, Math.round(fm.descent - fm.ascent));
    }

    private int baseHeight() {
        Paint.FontMetrics fm = basePaint.getFontMetrics();
        return Math.max(1, Math.round(fm.descent - fm.ascent));
    }

    private int lineHeight(Line line) {
        int ruby = line.hasRuby ? rubyHeight() + Math.round(dp(RUBY_GAP_DP)) : 0;
        return ruby + baseHeight() + dp(4);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int contentWidth;
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            contentWidth = 0;
            for (Token token : tokens) contentWidth += Math.round(tokenWidth(token));
            contentWidth = Math.max(contentWidth, dp(20));
        } else {
            contentWidth = Math.max(1, widthSize - getPaddingLeft() - getPaddingRight());
        }
        measuredContentWidth = contentWidth;
        lines = makeLines(contentWidth);
        int desiredWidth = contentWidth + getPaddingLeft() + getPaddingRight();
        int desiredHeight = getPaddingTop() + getPaddingBottom();
        for (Line line : lines) desiredHeight += lineHeight(line);
        int resolvedWidth = resolveSize(desiredWidth, widthMeasureSpec);
        int resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lines == null || lines.isEmpty()) return;
        float top = getPaddingTop();
        float gap = dp(RUBY_GAP_DP);
        for (Line line : lines) {
            boolean rubyLine = line.hasRuby;
            float rubyArea = rubyLine ? rubyHeight() : 0;
            float baseBaseline = top + rubyArea + (rubyLine ? gap : 0) - basePaint.getFontMetrics().ascent;
            float rubyBaseline = top - rubyPaint.getFontMetrics().ascent;
            float x = getPaddingLeft();
            for (Token token : line.tokens) {
                float baseWidth = basePaint.measureText(token.base);
                float rubyWidth = token.ruby == null ? 0 : rubyPaint.measureText(token.ruby);
                float width = Math.max(baseWidth, rubyWidth);
                if (token.ruby != null && !token.ruby.isEmpty()) {
                    canvas.drawText(token.ruby, x + (width - rubyWidth) / 2f, rubyBaseline, rubyPaint);
                    canvas.drawText(token.base, x + (width - baseWidth) / 2f, baseBaseline, basePaint);
                } else {
                    canvas.drawText(token.base, x, baseBaseline, basePaint);
                }
                x += width;
            }
            top += lineHeight(line);
        }
    }
}
