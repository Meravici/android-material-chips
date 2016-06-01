/*
 * Copyright (C) 2016 Doodle AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.doodle.android.chips;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.doodle.android.chips.views.ChipsEditText;
import com.doodle.android.chips.views.ChipsVerticalLinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChipsView extends ScrollView implements ChipsEditText.InputConnectionWrapperInterface {

    //<editor-fold desc="Static Fields">
    private static final String TAG = "ChipsView";
    private static final int CHIP_HEIGHT = 32; // dp
    private static final int SPACING_TOP = 4; // dp
    private static final int SPACING_BOTTOM = 4; // dp
    public static final int DEFAULT_VERTICAL_SPACING = 1; // dp
    private static final int DEFAULT_MAX_HEIGHT = -1;
    //</editor-fold>

    //<editor-fold desc="Resources">
    private int mChipsBgRes = R.drawable.chip_background;
    //</editor-fold>

    //<editor-fold desc="Attributes">
    private int mMaxHeight; // px
    private int mVerticalSpacing;

    private int mChipsColor;
    private int mChipsColorClicked;
    private int mChipsBgColor;
    private int mChipsBgColorClicked;
    private int mChipsTextColor;
    private int mChipsTextColorClicked;
    private int mChipsPlaceholderResId;
    private int mChipsDeleteResId;


    //<editor-fold desc="Private Fields">
    private float mDensity;
    private RelativeLayout mChipsContainer;
    private ChipsListener mChipsListener;
    private ChipsEditText mEditText;
    private ChipsVerticalLinearLayout mRootChipsLayout;
    private EditTextListener mEditTextListener;
    private List<Chip> mChipList = new ArrayList<>();
    private Object mCurrentEditTextSpan;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public ChipsView(Context context) {
        super(context);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context, attrs);
        init();
    }
    //</editor-fold>

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMaxHeight != DEFAULT_MAX_HEIGHT) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return true;
    }

    //<editor-fold desc="Initialization">
    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ChipsView,
                0, 0);
        try {
            mMaxHeight = a.getDimensionPixelSize(R.styleable.ChipsView_cv_max_height, DEFAULT_MAX_HEIGHT);
            mVerticalSpacing = a.getDimensionPixelSize(R.styleable.ChipsView_cv_vertical_spacing, (int) (DEFAULT_VERTICAL_SPACING * mDensity));
            mChipsColor = a.getColor(R.styleable.ChipsView_cv_color,
                    ContextCompat.getColor(context, R.color.base30));
            mChipsColorClicked = a.getColor(R.styleable.ChipsView_cv_color_clicked,
                    ContextCompat.getColor(context, R.color.colorPrimaryDark));

            mChipsBgColor = a.getColor(R.styleable.ChipsView_cv_bg_color,
                    ContextCompat.getColor(context, R.color.base10));
            mChipsBgColorClicked = a.getColor(R.styleable.ChipsView_cv_bg_color_clicked,
                    ContextCompat.getColor(context, R.color.blue));

            mChipsTextColor = a.getColor(R.styleable.ChipsView_cv_text_color,
                    Color.BLACK);
            mChipsTextColorClicked = a.getColor(R.styleable.ChipsView_cv_text_color_clicked,
                    Color.WHITE);

            mChipsPlaceholderResId = a.getResourceId(R.styleable.ChipsView_cv_icon_placeholder,
                    R.drawable.ic_person_24dp);
            mChipsDeleteResId = a.getResourceId(R.styleable.ChipsView_cv_icon_delete,
                    R.drawable.ic_close_24dp);
        } finally {
            a.recycle();
        }
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;

        mChipsContainer = new RelativeLayout(getContext());
        addView(mChipsContainer);

        // Dummy item to prevent AutoCompleteTextView from receiving focus
        LinearLayout linearLayout = new LinearLayout(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setFocusable(true);
        linearLayout.setFocusableInTouchMode(true);

        mChipsContainer.addView(linearLayout);

        mEditText = new ChipsEditText(getContext(), this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = (int) (SPACING_TOP * mDensity);
        layoutParams.bottomMargin = (int) (SPACING_BOTTOM * mDensity) + mVerticalSpacing;
        mEditText.setLayoutParams(layoutParams);
        mEditText.setMinHeight((int) (CHIP_HEIGHT * mDensity));
        mEditText.setPadding(0, 0, 0, 0);
        mEditText.setLineSpacing(mVerticalSpacing, (CHIP_HEIGHT * mDensity) / mEditText.getLineHeight());
        mEditText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_UNSPECIFIED);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        mChipsContainer.addView(mEditText);

        mRootChipsLayout = new ChipsVerticalLinearLayout(getContext(), mVerticalSpacing);
        mRootChipsLayout.setOrientation(LinearLayout.VERTICAL);
        mRootChipsLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mRootChipsLayout.setPadding(0, (int) (SPACING_TOP * mDensity), 0, 0);
        mChipsContainer.addView(mRootChipsLayout);

        initListener();
    }

    private void initListener() {
        mChipsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
                unselectAllChips();
            }
        });

        mEditTextListener = new EditTextListener();
        mEditText.addTextChangedListener(mEditTextListener);
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    unselectAllChips();
                }
            }
        });
    }
    //</editor-fold>

    //<editor-fold desc="Public Methods">
    public void addChip(String displayName, String avatarUrl, Object data) {
        addChip(displayName, Uri.parse(avatarUrl), data);
    }

    public void addChip(String displayName, Uri avatarUrl, Object data) {
        addChip(displayName, avatarUrl, data, false);
        mEditText.setText("");
        addLeadingMarginSpan();
    }

    public void addChip(String displayName, Uri avatarUrl, Object data, boolean isIndelible) {
        Chip chip = new Chip(displayName, avatarUrl, data, isIndelible);
        mChipList.add(chip);
        if (mChipsListener != null) {
            mChipsListener.onChipAdded(chip);
        }

        onChipsChanged(true);
        post(new Runnable() {
            @Override
            public void run() {
                fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @NonNull
    public List<Chip> getChips() {
        return Collections.unmodifiableList(mChipList);
    }

    public boolean removeChipBy(Object data) {
        for (int i = 0; i < mChipList.size(); i++) {
            if (mChipList.get(i).mData != null && mChipList.get(i).mData.equals(data)) {
                mChipList.remove(i);
                onChipsChanged(true);
                return true;
            }
        }
        return false;
    }

    public void setChipsListener(ChipsListener chipsListener) {
        this.mChipsListener = chipsListener;
    }

    public EditText getEditText() {
        return mEditText;
    }
    //</editor-fold>

    //<editor-fold desc="Private Methods">

    /**
     * rebuild all chips and place them right
     */
    private void onChipsChanged(final boolean moveCursor) {
        ChipsVerticalLinearLayout.TextLineParams textLineParams = mRootChipsLayout.onChipsChanged(mChipList);

        // if null then run another layout pass
        if (textLineParams == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    onChipsChanged(moveCursor);
                }
            });
            return;
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditText.getLayoutParams();
        params.topMargin = (int) ((SPACING_TOP + textLineParams.row * CHIP_HEIGHT) * mDensity) + textLineParams.row * mVerticalSpacing;
        mEditText.setLayoutParams(params);
        addLeadingMarginSpan(textLineParams.lineMargin);
        if (moveCursor) {
            mEditText.setSelection(mEditText.length());
        }
    }

    private void addLeadingMarginSpan(int margin) {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        mCurrentEditTextSpan = new android.text.style.LeadingMarginSpan.LeadingMarginSpan2.Standard(margin, 0);
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void addLeadingMarginSpan() {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void selectOrDeleteLastChip() {
        if (mChipList.size() > 0) {
            onChipInteraction(mChipList.size() - 1);
        }
    }

    private void onChipInteraction(int position) {
        try {
            Chip chip = mChipList.get(position);
            if (chip != null) {
                onChipInteraction(chip, true);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Out of bounds", e);
        }
    }

    private void onChipInteraction(Chip chip, boolean nameClicked) {
        unselectChipsExcept(chip);
        Log.d(TAG, "onChipInteraction() called with: " + "chip = [" + chip + "], nameClicked = [" + nameClicked + "]");
        if (chip.isSelected()) {
            Log.d(TAG, "onChipInteraction: chipIsSelected");
            mChipList.remove(chip);
            if (mChipsListener != null) {
                Log.d(TAG, "onChipInteraction: chiplistener is not null");
                mChipsListener.onChipDeleted(chip);
            }
            onChipsChanged(true);
            if (nameClicked) {
                mEditText.setText(chip.getLabel());
                addLeadingMarginSpan();
                mEditText.requestFocus();
                mEditText.setSelection(mEditText.length());
            }
        } else {
            chip.setSelected(true);
            onChipsChanged(false);
        }
    }

    private void unselectChipsExcept(Chip rootChip) {
        for (Chip chip : mChipList) {
            if (chip != rootChip) {
                chip.setSelected(false);
            }
        }
        onChipsChanged(false);
    }

    private void unselectAllChips() {
        unselectChipsExcept(null);
    }
    //</editor-fold>

    //<editor-fold desc="InputConnectionWrapperInterface Implementation">
    @Override
    public InputConnection getInputConnection(InputConnection target) {
        return new KeyInterceptingInputConnection(target);
    }
    //</editor-fold>

    //<editor-fold desc="EmailListener Implementation">

    //</editor-fold>

    //<editor-fold desc="Inner Classes / Interfaces">
    private class EditTextListener implements TextWatcher {

        private boolean mIsPasteTextChange = false;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count > 1) {
                mIsPasteTextChange = true;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
//            if (mIsPasteTextChange) {
//                mIsPasteTextChange = false;
//                // todo handle copy/paste text here
//
//            } else {
//                // no paste text change
//                if (s.toString().contains("\n")) {
//                    String text = s.toString();
//                    text = text.replace("\n", "");
//                    while (text.contains("  ")) {
//                        text = text.replace("  ", " ");
//                    }
//                    s.clear();
//                    if (text.length() > 1) {
//                        onEnterPressed(text);
//                    } else {
//                        s.append(text);
//                    }
//                }
//            }
            if (mChipsListener != null) {
                mChipsListener.onTextChanged(s);
            }
        }
    }

    private class KeyInterceptingInputConnection extends InputConnectionWrapper {

        public KeyInterceptingInputConnection(InputConnection target) {
            super(target, true);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (mEditText.length() == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        selectOrDeleteLastChip();
                        return true;
                    }
                }
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                mEditText.append("\n");
                return true;
            }

            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (mEditText.length() == 0 && beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    public class Chip implements OnClickListener {

        private static final int MAX_LABEL_LENGTH = 30;

        private String mLabel;
        private final Uri mPhotoUri;
        private final Object mData;
        private final boolean mIsIndelible;

        private RelativeLayout mView;
        private View mIconWrapper;
        private TextView mTextView;

        private ImageView mAvatarView;
        private ImageView mPersonIcon;
        private ImageView mCloseIcon;

        private ImageView mErrorIcon;

        private boolean mIsSelected = false;

        public Chip(String label, Uri photoUri, Object data) {
            this(label, photoUri, data, false);
        }

        public Chip(String label, Uri photoUri, Object data, boolean isIndelible) {
            this.mLabel = label;
            this.mPhotoUri = photoUri;
            this.mData = data;
            this.mIsIndelible = isIndelible;

            if (mLabel.length() > MAX_LABEL_LENGTH) {
                mLabel = mLabel.substring(0, MAX_LABEL_LENGTH) + "...";
            }
        }

        public String getLabel(){
            return mLabel;
        }

        public View getView() {
            if (mView == null) {
                mView = (RelativeLayout) inflate(getContext(), R.layout.chips_view, null);
                mView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (CHIP_HEIGHT * mDensity)));
                mAvatarView = (ImageView) mView.findViewById(R.id.ri_ch_avatar);
                mIconWrapper = mView.findViewById(R.id.rl_ch_avatar);
                mTextView = (TextView) mView.findViewById(R.id.tv_ch_name);
                mPersonIcon = (ImageView) mView.findViewById(R.id.iv_ch_person);
                mCloseIcon = (ImageView) mView.findViewById(R.id.iv_ch_close);

                mErrorIcon = (ImageView) mView.findViewById(R.id.iv_ch_error);

                // set inital res & attrs
                mView.setBackgroundResource(mChipsBgRes);
                mView.post(new Runnable() {
                    @Override
                    public void run() {
                        mView.getBackground().setColorFilter(mChipsBgColor, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                mIconWrapper.setBackgroundResource(R.drawable.circle);
                mTextView.setTextColor(mChipsTextColor);

                // set icon resources
                mPersonIcon.setBackgroundResource(mChipsPlaceholderResId);
                mCloseIcon.setBackgroundResource(mChipsDeleteResId);


                mView.setOnClickListener(this);
                mIconWrapper.setOnClickListener(this);
            }
            updateViews();
            return mView;
        }

        private void updateViews() {
            mTextView.setText(mLabel);
            if (mPhotoUri != null) {
                Glide.with(getContext())
                        .load(mPhotoUri)
                        .into(mAvatarView);
                //TODO on glide success
                mPersonIcon.setVisibility(View.INVISIBLE);
            }
            if (isSelected()) {

                mView.getBackground().setColorFilter(mChipsBgColorClicked, PorterDuff.Mode.SRC_ATOP);
                mTextView.setTextColor(mChipsTextColorClicked);
                mIconWrapper.getBackground().setColorFilter(mChipsColorClicked, PorterDuff.Mode.SRC_ATOP);

                mPersonIcon.animate().alpha(0.0f).setDuration(200).start();
                mAvatarView.animate().alpha(0.0f).setDuration(200).start();
                mCloseIcon.animate().alpha(1f).setDuration(200).setStartDelay(100).start();

            } else {

                mErrorIcon.setVisibility(View.GONE);

                mView.getBackground().setColorFilter(mChipsBgColor, PorterDuff.Mode.SRC_ATOP);
                mTextView.setTextColor(mChipsTextColor);
                mIconWrapper.getBackground().setColorFilter(mChipsColor, PorterDuff.Mode.SRC_ATOP);

                mPersonIcon.animate().alpha(0.3f).setDuration(200).setStartDelay(100).start();
                mAvatarView.animate().alpha(1f).setDuration(200).setStartDelay(100).start();
                mCloseIcon.animate().alpha(0.0f).setDuration(200).start();
            }
        }

        @Override
        public void onClick(View v) {
            mEditText.clearFocus();
            if (v.getId() == mView.getId()) {
                onChipInteraction(this, true);
            } else {
                onChipInteraction(this, false);
            }
        }

        public boolean isSelected() {
            return mIsSelected;
        }

        public void setSelected(boolean isSelected) {
            if (mIsIndelible) {
                return;
            }
            this.mIsSelected = isSelected;
        }

        public Object getData() {
            return mData;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Chip chip = (Chip) o;

            return mData != null ? mData.equals(chip.mData) : chip.mData == null;

        }

        @Override
        public int hashCode() {
            return mData != null ? mData.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "{"
                    + "[Contact: " + mData + "]"
                    + "[Label: " + mLabel + "]"
                    + "[PhotoUri: " + mPhotoUri + "]"
                    + "[IsIndelible" + mIsIndelible + "]"
                    + "}"
                    ;
        }
    }

    public interface ChipsListener {
        void onChipAdded(Chip chip);

        void onChipDeleted(Chip chip);

        void onTextChanged(CharSequence text);
    }

    public static abstract class ChipValidator {
        public abstract boolean isValid(Object contact);
    }
    //</editor-fold>
}
