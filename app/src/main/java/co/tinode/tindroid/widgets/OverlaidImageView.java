package co.tinode.tindroid.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import co.tinode.tindroid.R;

/**
 * ImageView with a rounded-rect cutout for previewing avatars.
 */
public class OverlaidImageView extends AppCompatImageView {
    private final Paint mBackgroundPaint;
    private final Path mClipPath;
    private final RectF mOverlayBounds;
    private boolean mShowOverlay = false;
    private final float mCornerRadius;

    public OverlaidImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(getResources().getColor(R.color.colorImagePreviewBg, null));
        mBackgroundPaint.setAlpha(0xCC);

        mClipPath = new Path();
        mOverlayBounds = new RectF();
        mCornerRadius = getResources().getDimension(R.dimen.avatar_corner_radius);
    }

    /**
     * Show or hide avatar image overlay.
     *
     * @param on true to show, false to hide
     */
    public void enableOverlay(boolean on) {
        mShowOverlay = on;
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Draw image.
        super.onDraw(canvas);

        // Draw background with rounded-rect cutout.
        if (mShowOverlay) {
            final int width = getWidth();
            final int height = getHeight();

            mClipPath.reset();
            mOverlayBounds.set(0, 0, width, height);
            float radius = Math.min(mCornerRadius, Math.min(width, height) * 0.5f);
            mClipPath.addRoundRect(mOverlayBounds, radius, radius, Path.Direction.CW);
            canvas.clipPath(mClipPath);
            canvas.drawPaint(mBackgroundPaint);
        }
    }
}
