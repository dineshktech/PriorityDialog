package com.dino.lib.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dineshkumar.m on 11/03/18.
 */

public abstract class BaseDialogFragment extends DialogFragment implements DialogInterface.OnShowListener{

    protected int mRequestCode;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int theme = resolveTheme();
        Dialog dialog = new Dialog(getActivity(), theme);

        //Bundle args = getArguments();
        /*if (args != null) {
            dialog.setCanceledOnTouchOutside(
                    args.getBoolean(BaseDialogBuilder.ARG_CANCELABLE_ON_TOUCH_OUTSIDE));
        }*/
        dialog.setOnShowListener(this);
        dialog.setOnDismissListener(this);
        dialog.setOnCancelListener(this);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity(), inflater, container);
        return build(builder) != null ? build(builder).create() : super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null) {
            mRequestCode = getTargetRequestCode();
        } else {
            Bundle args = getArguments();
            if (args != null) {
                mRequestCode = args.getInt(BaseDialogBuilder.ARG_REQUEST_CODE, 0);
            }
        }
    }

    protected abstract Builder build(Builder initialBuilder);

    protected abstract String getCustomTag();

    protected abstract int getPriority();

    @Override
    public void onDestroyView() {
        // bug in the compatibility library
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    public void showAllowingStateLoss(FragmentManager manager, String tag) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onShow(DialogInterface dialog) {
        if (getView() != null) {
            ScrollView vMessageScrollView = (ScrollView) getView().findViewById(R.id.sdl_message_scrollview);
            ListView vListView = (ListView) getView().findViewById(R.id.sdl_list);
            FrameLayout vCustomViewNoScrollView = (FrameLayout) getView().findViewById(R.id.sdl_custom);
            boolean customViewNoScrollViewScrollable = false;
            if (vCustomViewNoScrollView.getChildCount() > 0) {
                View firstChild = vCustomViewNoScrollView.getChildAt(0);
                if (firstChild instanceof ViewGroup) {
                    customViewNoScrollViewScrollable = isScrollable((ViewGroup) firstChild);
                }
            }
            boolean listViewScrollable = isScrollable(vListView);
            boolean messageScrollable = isScrollable(vMessageScrollView);
            boolean scrollable = listViewScrollable || messageScrollable || customViewNoScrollViewScrollable;
            modifyButtonsBasedOnScrollableContent(scrollable);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

    }

    protected <T> List<T> getDialogListeners(Class<T> listenerInterface) {
        final Fragment targetFragment = getTargetFragment();
        List<T> listeners = new ArrayList<T>(2);
        if (targetFragment != null && listenerInterface.isAssignableFrom(targetFragment.getClass())) {
            listeners.add((T) targetFragment);
        }
        if (getActivity() != null && listenerInterface.isAssignableFrom(getActivity().getClass())) {
            listeners.add((T) getActivity());
        }
        return Collections.unmodifiableList(listeners);
    }

    private void modifyButtonsBasedOnScrollableContent(boolean scrollable) {
        if (getView() == null) {
            return;
        }
        View vButtonDivider = getView().findViewById(R.id.sdl_button_divider);
        View vButtonsBottomSpace = getView().findViewById(R.id.sdl_buttons_bottom_space);
        View vDefaultButtons = getView().findViewById(R.id.sdl_buttons_default);
        View vStackedButtons = getView().findViewById(R.id.sdl_buttons_stacked);
        if (vDefaultButtons.getVisibility() == View.GONE && vStackedButtons.getVisibility() == View.GONE) {
            // no buttons
            vButtonDivider.setVisibility(View.GONE);
            vButtonsBottomSpace.setVisibility(View.GONE);
        } else if (scrollable) {
            vButtonDivider.setVisibility(View.VISIBLE);
            vButtonsBottomSpace.setVisibility(View.GONE);
        } else {
            vButtonDivider.setVisibility(View.GONE);
            vButtonsBottomSpace.setVisibility(View.VISIBLE);
        }
    }

    private boolean isScrollable(ViewGroup listView) {
        int totalHeight = 0;
        for (int i = 0; i < listView.getChildCount(); i++) {
            totalHeight += listView.getChildAt(i).getMeasuredHeight();
        }
        return listView.getMeasuredHeight() < totalHeight;
    }

    /**
     * Resolves the theme to be used for the dialog.
     *
     * @return The theme.
     */
    @StyleRes
    private int resolveTheme() {
        // First check if getTheme() returns some usable theme.
        int theme = getTheme();
        if (theme != 0) {
            return theme;
        }

        // Get the light/dark attribute from the Activity's Theme.
        boolean useLightTheme = isActivityThemeLight();

        // Now check if developer overrides the Activity's Theme with an argument.
        Bundle args = getArguments();
        if (args != null) {
            if (args.getBoolean(BaseDialogBuilder.ARG_USE_DARK_THEME)) {
                // Developer is explicitly using the dark theme.
                useLightTheme = false;
            } else if (args.getBoolean(BaseDialogBuilder.ARG_USE_LIGHT_THEME)) {
                // Developer is explicitly using the light theme.
                useLightTheme = true;
            }
        }

        return useLightTheme ? R.style.AppTheme_Dialog : R.style.AppTheme_Dialog;
    }

    /**
     * This method resolves the current theme declared in the manifest
     */
    private boolean isActivityThemeLight() {
        try {
            TypedValue val = new TypedValue();

            //Reading attr value from current theme
            getActivity().getTheme().resolveAttribute(R.attr.isLightTheme, val, true);

            //Passing the resource ID to TypedArray to get the attribute value
            TypedArray styledAttributes =
                    getActivity().obtainStyledAttributes(val.data, new int[]{R.attr.isLightTheme});
            boolean lightTheme = styledAttributes.getBoolean(0, false);
            styledAttributes.recycle();

            return lightTheme;
        } catch (RuntimeException e) {
            //Resource not found , so sticking to light theme
            return true;
        }
    }

    /**
     * Custom dialog builder
     */
    protected static class Builder {

        private final Context mContext;

        private final ViewGroup mContainer;

        private final LayoutInflater mInflater;

        private CharSequence mTitle = null;

        private CharSequence mPositiveButtonText;

        private View.OnClickListener mPositiveButtonListener;

        private CharSequence mNegativeButtonText;

        private View.OnClickListener mNegativeButtonListener;

        private CharSequence mNeutralButtonText;

        private View.OnClickListener mNeutralButtonListener;

        private CharSequence mMessage;

        private View mCustomView;

        private ListAdapter mListAdapter;

        private int mListCheckedItemIdx;

        private int mChoiceMode;

        private int[] mListCheckedItemMultipleIds;

        private AdapterView.OnItemClickListener mOnItemClickListener;

        public Builder(Context context, LayoutInflater inflater, ViewGroup container) {
            this.mContext = context;
            this.mContainer = container;
            this.mInflater = inflater;
        }

        public LayoutInflater getLayoutInflater() {
            return mInflater;
        }

        public Builder setTitle(int titleId) {
            this.mTitle = mContext.getText(titleId);
            return this;
        }

        public Builder setTitle(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        public Builder setPositiveButton(int textId, final View.OnClickListener listener) {
            mPositiveButtonText = mContext.getText(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(int textId, final View.OnClickListener listener) {
            mNegativeButtonText = mContext.getText(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(int textId, final View.OnClickListener listener) {
            mNeutralButtonText = mContext.getText(textId);
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(CharSequence text, final View.OnClickListener listener) {
            mNeutralButtonText = text;
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setMessage(int messageId) {
            mMessage = mContext.getText(messageId);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessage = message;
            return this;
        }

        public Builder setItems(ListAdapter listAdapter, int[] checkedItemIds, int choiceMode, final AdapterView.OnItemClickListener listener) {
            mListAdapter = listAdapter;
            mListCheckedItemMultipleIds = checkedItemIds;
            mOnItemClickListener = listener;
            mChoiceMode = choiceMode;
            mListCheckedItemIdx = -1;
            return this;
        }

        /**
         * Set list
         *
         * @param checkedItemIdx Item check by default, -1 if no item should be checked
         */
        public Builder setItems(ListAdapter listAdapter, int checkedItemIdx,
                                final AdapterView.OnItemClickListener listener) {
            mListAdapter = listAdapter;
            mOnItemClickListener = listener;
            mListCheckedItemIdx = checkedItemIdx;
            mChoiceMode = AbsListView.CHOICE_MODE_NONE;
            return this;
        }

        public Builder setView(View view) {
            mCustomView = view;
            return this;
        }

        public View create() {

            LinearLayout content = (LinearLayout) mInflater.inflate(R.layout.view_dialog, mContainer, false);
            TextView vTitle =  content.findViewById(R.id.sdl_title);
            TextView vMessage =  content.findViewById(R.id.sdl_message);
            FrameLayout vCustomView =  content.findViewById(R.id.sdl_custom);
            TextView vPositiveButton =  content.findViewById(R.id.sdl_button_positive);
            TextView vNegativeButton =  content.findViewById(R.id.sdl_button_negative);
            TextView vNeutralButton = content.findViewById(R.id.sdl_button_neutral);
            TextView vPositiveButtonStacked =  content.findViewById(R.id.sdl_button_positive_stacked);
            TextView vNegativeButtonStacked =  content.findViewById(R.id.sdl_button_negative_stacked);
            TextView vNeutralButtonStacked =  content.findViewById(R.id.sdl_button_neutral_stacked);
            View vButtonsDefault = content.findViewById(R.id.sdl_buttons_default);
            View vButtonsStacked = content.findViewById(R.id.sdl_buttons_stacked);
            ListView vList =  content.findViewById(R.id.sdl_list);


            set(vTitle, mTitle);
            set(vMessage, mMessage);
            setPaddingOfTitleAndMessage(vTitle, vMessage);

            if (mCustomView != null) {
                vCustomView.addView(mCustomView);
            }
            if (mListAdapter != null) {
                vList.setAdapter(mListAdapter);
                vList.setOnItemClickListener(mOnItemClickListener);
                if (mListCheckedItemIdx != -1) {
                    vList.setSelection(mListCheckedItemIdx);
                }
                if (mListCheckedItemMultipleIds != null) {
                    vList.setChoiceMode(mChoiceMode);
                    for (int i : mListCheckedItemMultipleIds) {
                        vList.setItemChecked(i, true);
                    }
                }
            }

            if (shouldStackButtons()) {
                set(vPositiveButtonStacked, mPositiveButtonText, mPositiveButtonListener);
                set(vNegativeButtonStacked, mNegativeButtonText, mNegativeButtonListener);
                set(vNeutralButtonStacked, mNeutralButtonText, mNeutralButtonListener);
                vButtonsDefault.setVisibility(View.GONE);
                vButtonsStacked.setVisibility(View.VISIBLE);
            } else {
                set(vPositiveButton, mPositiveButtonText, mPositiveButtonListener);
                set(vNegativeButton, mNegativeButtonText, mNegativeButtonListener);
                set(vNeutralButton, mNeutralButtonText, mNeutralButtonListener);
                vButtonsDefault.setVisibility(View.VISIBLE);
                vButtonsStacked.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(mPositiveButtonText) && TextUtils.isEmpty(mNegativeButtonText) && TextUtils.isEmpty
                    (mNeutralButtonText)) {
                vButtonsDefault.setVisibility(View.GONE);
            }

            return content;
        }

        /**
         * Padding is different if there is only title, only message or both.
         */
        private void setPaddingOfTitleAndMessage(TextView vTitle, TextView vMessage) {
            int grid6 = mContext.getResources().getDimensionPixelSize(R.dimen.grid_6);
            int grid4 = mContext.getResources().getDimensionPixelSize(R.dimen.grid_4);
            if (!TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mMessage)) {
                vTitle.setPadding(grid6, grid6, grid6, grid4);
                vMessage.setPadding(grid6, 0, grid6, grid4);
            } else if (TextUtils.isEmpty(mTitle)) {
                vMessage.setPadding(grid6, grid4, grid6, grid4);
            } else if (TextUtils.isEmpty(mMessage)) {
                vTitle.setPadding(grid6, grid6, grid6, grid4);
            }
        }

        private boolean shouldStackButtons() {
            return shouldStackButton(mPositiveButtonText) || shouldStackButton(mNegativeButtonText)
                    || shouldStackButton(mNeutralButtonText);
        }

        private boolean shouldStackButton(CharSequence text) {
            final int MAX_BUTTON_CHARS = 12; // based on observation, could be done better with measuring widths
            return text != null && text.length() > MAX_BUTTON_CHARS;
        }

        private void set(TextView button, CharSequence text,View.OnClickListener listener) {
            set(button, text);
            if (listener != null) {
                button.setOnClickListener(listener);
            }
        }

        private void set(TextView textView, CharSequence text) {
            if (text != null) {
                textView.setText(text);
            } else {
                textView.setVisibility(View.GONE);
            }
        }
    }
}
