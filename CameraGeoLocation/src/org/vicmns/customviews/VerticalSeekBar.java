package org.vicmns.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {
	//Vertical angle
	private int rotationAngle = -90;
	private static final String TAG = "VerticalSeekBar";
	
    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    	switch(Math.abs(rotationAngle)) {
			case 90:
				super.onSizeChanged(h, w, oldh, oldw);
		        break;
			case 0:
			case 180:
				super.onSizeChanged(w, h, oldw, oldh);
				break;
    	}
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	switch(Math.abs(rotationAngle)) {
		case 90:
			super.onMeasure(heightMeasureSpec, widthMeasureSpec);
	        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	        break;
		case 0:
		case 180:
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
			break;
    	}
    }
    
    @Override
    protected void onDraw(Canvas c) {
    	c.rotate(rotationAngle);
    	switch(rotationAngle) {
    		case -90:
    			c.translate(-getHeight(), 0);
    			break;
    		case 90:
    			c.translate(0, -getWidth());
    			break;
    		case 180:
    			c.translate(-getWidth(), -getHeight());
    			break;
    	}
        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            	if(rotationAngle == -90) {
            		int progress = getMax() - (int) (getMax() * event.getY() / getHeight());
            		progress = (progress < 0) ? 0 : progress; 
            		setProgress((progress < getMax()) ? progress : getMax());
            	}
            	else if(rotationAngle == 90) {
            		int progress = (int) (getMax() * event.getY() / getHeight());
            		progress = (progress < 0) ? 0 : progress; 
            		setProgress( (progress < getMax()) ? progress : getMax());
            	}
            	else if(rotationAngle == 0  || Math.abs(rotationAngle) == 180) {
            		int progress = getMax() - (int) (getMax() * event.getX() / getWidth());
            		progress = (progress < 0) ? 0 : progress;
            		setProgress((progress < getMax()) ? progress : getMax());            		
            	}
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
    
    public int getViewAngle() {
    	return rotationAngle;
    }
    
    public void setViewAngle(int angle){
    	this.rotationAngle = angle;
    }
}