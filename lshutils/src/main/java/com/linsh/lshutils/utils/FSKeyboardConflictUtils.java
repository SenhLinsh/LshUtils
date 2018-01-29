package com.linsh.lshutils.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.linsh.utilseverywhere.KeyboardUtils;
import com.linsh.utilseverywhere.SharedPreferenceUtils;
import com.linsh.lshutils.common.FileProperties;

/**
 * <pre>
 *    author : Senh Linsh
 *    github : https://github.com/SenhLinsh
 *    date   : 2017/11/10
 *    desc   : 工具类: 解决全屏状态下软件盘冲突的问题 (软键盘部分遮挡输入框)
 * </pre>
 */
public class FSKeyboardConflictUtils {
    public static final String KEY_KEYBOARD_HEIGHT_LANDSCAPE = "key_keyboard_height_landscape";
    public static final String KEY_KEYBOARD_HEIGHT_PORTRAIT = "key_keyboard_height_portrait";
    private static final int PANEL_HEIGHT_WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int PANEL_HEIGHT_EQUAL_KEYBOARD = ViewGroup.LayoutParams.MATCH_PARENT;
    private static int mPanelHeight = PANEL_HEIGHT_WRAP_CONTENT;

    /**
     * 解决全屏状态下软件盘冲突的问题 (软键盘部分遮挡输入框)
     *
     * @param activity                  当前 Activity
     * @param panelRoot                 面板
     * @param etInput                   输入框
     * @param onKeyboardShowingListener 键盘弹出收起回调
     */
    public static void attach(Activity activity, View panelRoot, View etInput, @Nullable OnKeyboardShowingListener onKeyboardShowingListener) {
        attach(activity, panelRoot, null, etInput, onKeyboardShowingListener, null);
    }

    /**
     * 解决全屏状态下软件盘冲突的问题 (软键盘部分遮挡输入框)
     *
     * @param activity                  当前 Activity
     * @param panelRoot                 面板
     * @param btn                       切换键盘和面板的按钮
     * @param etInput                   输入框
     * @param onKeyboardShowingListener 键盘弹出收起回调
     * @param onPanelShowingListener    面板弹出收起回调
     */
    public static void attach(Activity activity, View panelRoot, @Nullable View btn, View etInput,
                              @Nullable OnKeyboardShowingListener onKeyboardShowingListener, @Nullable OnPanelShowingListener onPanelShowingListener) {
        if (activity == null) {
            throw new RuntimeException("need to bind an Activity.");
        }
        if (panelRoot == null) {
            throw new RuntimeException("need a panel to handler the keyboard conflict.");
        }

        final ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
        KeyboardStatusListener keyboardStatusListener = new KeyboardStatusListener(
                contentView, panelRoot, btn, etInput, onKeyboardShowingListener, onPanelShowingListener);

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardStatusListener);
        if (btn != null) {
            btn.setOnClickListener(keyboardStatusListener);
        }
        if (etInput != null) {
            etInput.setOnClickListener(keyboardStatusListener);
        }
    }

    private static class KeyboardStatusListener implements ViewTreeObserver.OnGlobalLayoutListener,
            View.OnClickListener {
        private final ViewGroup mContentView;
        private final View mPanel;
        private final View mBtn;
        private final View mEtInput;
        private final OnKeyboardShowingListener mOnKeyboardShowingListener;
        private OnPanelShowingListener mOnPanelShowingListener;
        private int maxBottom;
        private int lastKeyboardHeight;
        private boolean keyBoard2Panel;
        private boolean isKeyboardShowed;

        public KeyboardStatusListener(ViewGroup contentView, View panel, @Nullable View btn, View etInput,
                                      @Nullable OnKeyboardShowingListener onKeyboardShowingListener, @Nullable OnPanelShowingListener onPanelShowingListener) {
            mContentView = contentView;
            mPanel = panel;
            mBtn = btn;
            mEtInput = etInput;
            mOnKeyboardShowingListener = onKeyboardShowingListener;
            mOnPanelShowingListener = onPanelShowingListener;
        }

        @Override
        public void onGlobalLayout() {
            Rect rect = new Rect();
            mContentView.getWindowVisibleDisplayFrame(rect);

            // 获取最大底部坐标
            if (maxBottom < rect.bottom) {
                maxBottom = rect.bottom;
            }
            // 获取键盘高度
            int keyboardHeight = 0;
            if (maxBottom != 0 && maxBottom - rect.bottom > 100) {
                keyboardHeight = maxBottom - rect.bottom;
            }
            // 每次初始化界面时都不能直接获取键盘高度,先从本地保存的数据中获取
            // ps.第一次启动应用时,这时本地也没有数据,这是打开软件盘时无法自动匹配键盘高度,导致此时无法解决遮挡问题,等第二次开始之后就好了 (算是一个还没有解决的bug吧)
            if (keyboardHeight == 0) {
                if (isOrientationPortrait()) {
                    keyboardHeight = SharedPreferenceUtils.getInt(KEY_KEYBOARD_HEIGHT_PORTRAIT);
                } else {
                    keyboardHeight = SharedPreferenceUtils.getInt(KEY_KEYBOARD_HEIGHT_LANDSCAPE);
                }
            }
            if (keyboardHeight == 0) {
                FileProperties fileProperties = PropertiesFileUtils.getObject(FileProperties.class);
                if (fileProperties != null) {
                    keyboardHeight = isOrientationPortrait() ? fileProperties.KeyboardHeightPortrait : fileProperties.KeyboardHeightLandscape;
                }
            }
            // 键盘高度发生变化, 保存数据到本地
            if (lastKeyboardHeight != keyboardHeight && keyboardHeight != 0) {
                if (isOrientationPortrait()) {
                    if (SharedPreferenceUtils.getInt(KEY_KEYBOARD_HEIGHT_PORTRAIT) != keyboardHeight) {
                        SharedPreferenceUtils.putInt(KEY_KEYBOARD_HEIGHT_PORTRAIT, keyboardHeight);
                        FileProperties fileProperties = PropertiesFileUtils.getObject(FileProperties.class);
                        if (fileProperties == null) {
                            fileProperties = new FileProperties();
                        }
                        fileProperties.KeyboardHeightPortrait = keyboardHeight;
                        PropertiesFileUtils.putObject(fileProperties);
                    }
                } else {
                    if (SharedPreferenceUtils.getInt(KEY_KEYBOARD_HEIGHT_LANDSCAPE) != keyboardHeight) {
                        SharedPreferenceUtils.putInt(KEY_KEYBOARD_HEIGHT_LANDSCAPE, keyboardHeight);
                        FileProperties fileProperties = PropertiesFileUtils.getObject(FileProperties.class);
                        if (fileProperties == null) {
                            fileProperties = new FileProperties();
                        }
                        fileProperties.KeyboardHeightLandscape = keyboardHeight;
                        PropertiesFileUtils.putObject(fileProperties);
                    }
                }

            }
            // 判断键盘的显示和隐藏
            if (keyboardHeight != 0 && maxBottom - rect.bottom > 100 && !isKeyboardShowed) {
                isKeyboardShowed = true;
                if (mOnKeyboardShowingListener != null) {
                    mOnKeyboardShowingListener.onKeyboardShowing(true);
                }
            }
            if (maxBottom != 0 && maxBottom == rect.bottom && isKeyboardShowed) {
                isKeyboardShowed = false;
                if (mOnKeyboardShowingListener != null) {
                    mOnKeyboardShowingListener.onKeyboardShowing(false);
                }

                if (keyBoard2Panel) { // 隐藏键盘, 显示面板
                    (mPanel).setVisibility(View.VISIBLE);
                    refreshPanelHeight();
                } else { // 隐藏键盘
                    (mPanel).setVisibility(View.GONE);
                }
                keyBoard2Panel = false;
            }
            lastKeyboardHeight = keyboardHeight;
        }

        /**
         * 判断横竖屏, 横屏和竖屏的键盘高度是不一样的, 使用过程中可能会出现横竖屏转换
         */
        public boolean isOrientationPortrait() {
            Activity activity = (Activity) (mPanel).getContext();
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
        }

        @Override
        public void onClick(View v) {
            if (mBtn != null && v.getId() == mBtn.getId()) { /// 键盘面板切换按钮的点击事件
                boolean isKeyBoardShowing = isKeyBoardShowing();
                if (isKeyBoardShowing) { // 键盘正在显示, 切换至面板
                    keyBoard2Panel = true;
                    showPanel(mPanel);
                    if (mOnPanelShowingListener != null) {
                        mOnPanelShowingListener.onPanelShowing(true);
                    }
                } else if ((mPanel).getVisibility() == View.VISIBLE) { // 面板在显示, 切换至键盘
                    (mPanel).setVisibility(View.INVISIBLE);
                    refreshPanelHeight(lastKeyboardHeight);
                    KeyboardUtils.showKeyboard(mEtInput);
                    if (mOnPanelShowingListener != null) {
                        mOnPanelShowingListener.onPanelShowing(false);
                    }
                } else { // 都没有显示, 切换至面板
                    refreshPanelHeight();
                    showPanel(mPanel);
                    if (mOnPanelShowingListener != null) {
                        mOnPanelShowingListener.onPanelShowing(true);
                    }
                }
            } else if (v.getId() == mEtInput.getId()) { /// 输入框的点击事件
                if (!isKeyBoardShowing()) {
                    if (lastKeyboardHeight != 0) {
                        refreshPanelHeight(lastKeyboardHeight);
                        (mPanel).setVisibility(View.INVISIBLE);
                    }
                    if (mOnPanelShowingListener != null) {
                        mOnPanelShowingListener.onPanelShowing(false);
                    }
                }
            }
        }

        /**
         * 判断键盘是否正在显示
         */
        private boolean isKeyBoardShowing() {
            Rect rect = new Rect();
            mContentView.getWindowVisibleDisplayFrame(rect);
            return Math.abs(maxBottom - rect.bottom) > 100;
        }

        private void refreshPanelHeight(int height) {
            if (mPanel.getHeight() == height) {
                return;
            }
            ViewGroup.LayoutParams layoutParams = mPanel.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        height);
                mPanel.setLayoutParams(layoutParams);
            } else {
                layoutParams.height = height;
                mPanel.requestLayout();
            }
        }

        private void refreshPanelHeight() {
            if (mPanelHeight == PANEL_HEIGHT_WRAP_CONTENT) {
                refreshPanelHeight(PANEL_HEIGHT_WRAP_CONTENT);
            } else if (mPanelHeight == PANEL_HEIGHT_EQUAL_KEYBOARD) {
                refreshPanelHeight(lastKeyboardHeight);
            } else if (mPanelHeight > 0) {
                refreshPanelHeight(mPanelHeight);
            } else {
                refreshPanelHeight(PANEL_HEIGHT_WRAP_CONTENT);
            }
        }
    }

    /**
     * 显示键盘并隐藏面板
     *
     * @param panelLayout 面板布局
     * @param focusView   焦点 View
     */
    public static void showKeyboardAndHidePanel(View panelLayout, View focusView) {
        panelLayout.setVisibility(View.INVISIBLE);
        KeyboardUtils.showKeyboard(focusView);
    }

    /**
     * 显示面板
     *
     * @param panelLayout 面板布局
     */
    public static void showPanel(View panelLayout) {
        final Activity activity = (Activity) panelLayout.getContext();
        panelLayout.setVisibility(View.VISIBLE);
        if (activity.getCurrentFocus() != null) {
            KeyboardUtils.hideKeyboard(activity.getCurrentFocus());
        }
    }

    /**
     * 隐藏面板和键盘
     *
     * @param panelLayout 面板布局
     */
    public static void hidePanelAndKeyboard(View panelLayout) {
        final Activity activity = (Activity) panelLayout.getContext();
        final View focusView = activity.getCurrentFocus();
        if (focusView != null) {
            KeyboardUtils.hideKeyboard(activity.getCurrentFocus());
            focusView.clearFocus();
        }
        panelLayout.setVisibility(View.GONE);
    }

    /**
     * 设置面板的高度
     *
     * @param panelHeight 面板高度
     */
    public static void setPanelHeight(int panelHeight) {
        mPanelHeight = panelHeight;
    }

    public interface OnKeyboardShowingListener {
        void onKeyboardShowing(boolean isShowing);
    }

    public interface OnPanelShowingListener {
        void onPanelShowing(boolean isShowing);
    }
}
