package org.vicmns.camerageolocation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

public class CustomToggleButton extends ToggleButton {
	
	
	public CustomToggleButton(Context context) {
		super(context);
	}
	
	public CustomToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomToggleButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		if(HelperObject.rotation != 0) {
			this.setMeasuredDimension(parentWidth, parentHeight / 2);
			this.setLayoutParams(new RelativeLayout.LayoutParams (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));	
		}
		else {
			this.setMeasuredDimension(parentWidth/2, parentHeight);
			this.setLayoutParams(new RelativeLayout.LayoutParams (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));		
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		Drawable d = getBackground();
		if(d != null) {
			canvas.save();
			canvas.rotate(HelperObject.rotation);
			canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
			d.setLevel(DRAWING_CACHE_QUALITY_AUTO);
			d.draw(canvas);
			getLayout().draw(canvas);
			canvas.restore();			
		}
	}

}
