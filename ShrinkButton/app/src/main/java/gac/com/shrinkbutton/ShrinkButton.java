package gac.com.shrinkbutton;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

public class ShrinkButton extends Button implements View.OnTouchListener{
    public static final int STATE_EXPANDED = 0;//按钮属于正常状态
    public static final int STATE_SHRINKING = 1;//按钮属于 正在收缩动作的状态
    public static final int STATE_SHRINKED = 2;//按钮完成收缩动作的状态
    public static final int STATE_PROGRESSING = 3;//按钮处于进度条执行状态
    public static final int STATE_EXPANDING = 4;//按钮处于 由进度条变为正常状态的动作时候
    //The button's width first created.
    private int mButtonInitialWidth;
    //Used to control the progressing drawing.
    private float mProgressFraction = 0;
    //How speed these animations would displayed.
    private int mShrinkingDuration = 400;
    private int mProgressingDuration = 600;
    //Used to control the button shrinking animation.
    private ValueAnimator mShrinkingController;
    //Used to control the progressing animation.
    private ValueAnimator mProgressingController;
    //Used to control the back-expanding animation.
    private ValueAnimator mExpandingController;
    //To remark which
    private int animationState = -1;
    //Used to draw the inner pattern.
    private DynamicPatternDrawer patternDrawer;
    //Used to indicate whether is showing on the front window.
    private boolean isWindowFocused = false;
    //Used to record the button text (if there is).
    private String buttonText;

    public ShrinkButton(Context context) {
        super(context);
        init();
    }

    public ShrinkButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShrinkButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        animationState = STATE_EXPANDED;
        patternDrawer = new CircleLineAndPoint();
        setOnTouchListener(this);
    }

    //button 开始收缩的的动作
    private void initShrinkingAnimationController(){
        mShrinkingController = ValueAnimator.ofFloat(0, 1);
        mShrinkingController.setDuration(mShrinkingDuration);
        mShrinkingController.setInterpolator(new DecelerateInterpolator());
        mShrinkingController.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //不断更新按钮的宽度
                ViewGroup.LayoutParams lp = getLayoutParams();
                //最后放缩到button 宽高值一样 ，高的设置为45dp
                lp.width = (int) (mButtonInitialWidth - (mButtonInitialWidth - lp.height) * (float) animation.getAnimatedValue());
                setLayoutParams(lp);
            }
        });
        mShrinkingController.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                //Make text disappeared.
                buttonText = getText().toString();
                setText(null);
               // setEnabled(false);
                animationState = STATE_SHRINKING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animationState = STATE_SHRINKED;
                //放缩完成后 开始执行 进度条动作
                //setBackground(generateCircleDrawable(Color.RED));
                startProgressingAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * button 由 圆形回复为原来形状时候的动作
     */
    private void initExpandingAnimationController(){
        mExpandingController = ValueAnimator.ofFloat(0, 1);
        mExpandingController.setDuration(mShrinkingDuration);
        mExpandingController.setInterpolator(new DecelerateInterpolator());
        mExpandingController.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //恢复按钮长度
                ViewGroup.LayoutParams lp = getLayoutParams();
                lp.width = (int) (lp.height + (mButtonInitialWidth - lp.height) * (float) animation.getAnimatedValue());
                setLayoutParams(lp);
            }
        });
        mExpandingController.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animationState = STATE_EXPANDING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animationState = STATE_EXPANDED;
                if (null != buttonText){
                    setText(buttonText);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void initProgressingAnimationController(){
        mProgressingController = ValueAnimator.ofFloat(0, 1);
        mProgressingController.setDuration(mProgressingDuration);
        mProgressingController.setRepeatCount(ValueAnimator.INFINITE);
        mProgressingController.setInterpolator(new DecelerateInterpolator());
        mProgressingController.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgressFraction = (float) animation.getAnimatedValue();
                postInvalidate();
                //不断刷新界面 根据mProgressFraction 执行进度条的动作
            }
        });
        mProgressingController.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animationState = STATE_PROGRESSING;
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                animationState = STATE_SHRINKED;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //在界面恢复的时候 执行onLayout方法 此时如果animationState > 0 只要按钮不是正常状态 ，就应该让按钮处于进度条状态
        if (animationState > 0){
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.width = getHeight();
            setLayoutParams(lp);
            setText(null);
            if (animationState == STATE_PROGRESSING) {
                startProgressingAnimation();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((animationState == STATE_PROGRESSING || animationState == STATE_SHRINKED) && patternDrawer != null) {
            patternDrawer.onDrawPattern(canvas, getWidth(), getHeight(), mProgressFraction);//根据valueAnimator 的值 执行进度条的动作
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus){
            //Showed on the front window.
            isWindowFocused = true;
        }else{
            isWindowFocused = false;
            stop();
            //Release All resource;
            if (mShrinkingController != null){
                mShrinkingController.removeAllListeners();
                mShrinkingController.removeAllUpdateListeners();
                mShrinkingController = null;
            }
            if (mProgressingController != null){
                mProgressingController.removeAllListeners();
                mProgressingController.removeAllUpdateListeners();
                mProgressingController = null;
            }
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt("animation_state", animationState);
        bundle.putParcelable("super_state", super.onSaveInstanceState());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle){
            Bundle bundle = (Bundle) state;
            animationState = (bundle.getInt("animation_state"));
            state = bundle.getParcelable("super_state");
        }
        super.onRestoreInstanceState(state);
    }

    //开始shrinkAnimation 按钮开始收缩 当shrinAnimation 执行完 开始执行 progressingAnimation
    /**
     * Start the whole series of animation, include shrinking and progrssing.
     */
    public void startWholeAnimation(){
        if (animationState > 0 || !isWindowFocused){
            return;
        }
        //Initial those animation fraction firstly.
         initAnimationParameters();
        if (mShrinkingController == null || mProgressingController == null){
            initShrinkingAnimationController();
            initProgressingAnimationController();
        }
        if (mShrinkingController.isRunning() || mProgressingController.isRunning()){
            mShrinkingController.end();
            mShrinkingController.cancel();
            mProgressingController.end();
            mProgressingController.cancel();
        }
        mShrinkingController.start();
    }

    /**
     * Start progressing animation separately.
     */
    private void startProgressingAnimation(){
        if (animationState == STATE_PROGRESSING || !isWindowFocused){
            return;
        }
        if (mProgressingController == null){
            initProgressingAnimationController();
        }
        if (mProgressingController.isRunning()) {
            mProgressingController.end();
            mProgressingController.cancel();
        }
        mProgressingController.start();
    }

    /**
     * Start back-expanding animation.
     */
    private void startExpandingAnimation(){
        if (!isWindowFocused)   return;
        if (mExpandingController == null){
            initExpandingAnimationController();
        }
        if (mExpandingController.isRunning() || mExpandingController.isStarted()){
            mExpandingController.end();
            mExpandingController.cancel();
        }
        mExpandingController.start();
    }

    /**
     * Stop all animations including shrinking and progressing.
     */
    public void stop(){
        if (mShrinkingController != null && mShrinkingController.isRunning()){
            mShrinkingController.end();
            mShrinkingController.cancel();
        }
        if (mProgressingController != null && mProgressingController.isRunning()){
            mProgressingController.end();
            mProgressingController.cancel();
        }
    }

    private void initAnimationParameters(){
        mProgressFraction = 0;
        mButtonInitialWidth = getWidth();
    }

    public int getAnimationState() {
        return animationState;
    }

    public void setAnimationState(int animationState) {
        if (animationState < 0 || animationState > 3) {
            return;
        }
        this.animationState = animationState;
    }

    public int getShrinkingDuration() {
        return mShrinkingDuration;
    }

    public void setShrinkingDuration(int shrinkingDuration) {
        if (shrinkingDuration < 0){
            return;
        }
        this.mShrinkingDuration = shrinkingDuration;
        if (mShrinkingController != null){
            mShrinkingController.setDuration(this.mShrinkingDuration);
        }
    }

    public int getProgressingDuration() {
        return mProgressingDuration;
    }

    public void setProgressingDuration(int progressingDuration) {
        if (progressingDuration < 0){
            return;
        }
        this.mProgressingDuration = progressingDuration;
        if (mProgressingController != null){
            mProgressingController.setDuration(this.mProgressingDuration);
        }
    }

    public DynamicPatternDrawer getPatternDrawer() {
        return patternDrawer;
    }

    public void setPatternDrawer(DynamicPatternDrawer patternDrawer) {
        this.patternDrawer = patternDrawer;
    }

    //手动调用执行 ExapandAnimation 按钮的回复动作
    public void reset(){
        //Stop the previous progressing animation.
        stop();
        startExpandingAnimation();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (MotionEventCompat.getActionMasked(event)){
            case MotionEvent.ACTION_DOWN:
                if (!isWindowFocused){//如果界面还未完全显示 则此时不进行事件处理
                    return true;
                }
                if (animationState == STATE_EXPANDED){//当按钮的动作状态处于 扩展状态时候，（正常状态时候）,此时点击事件发生，开始执行动画效果，
                    startWholeAnimation();              //并且应该onTouch返回fasle，不拦截onClickListener事件 ，按钮动作状态处于其它状态,
                    //this.callOnClick();               //此时onTouch返回true 拦截onClickListener 不让点击事件发生。
                    return false;
                }else{
                    return true;
                }


            case MotionEvent.ACTION_UP:

                return false;
        }
        return true;
    }
}