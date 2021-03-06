package kr.jung.keyboard_hangle.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;
import java.util.regex.Pattern;

import kr.jung.keyboard_hangle.Constants;
import kr.jung.keyboard_hangle.R;
import kr.jung.keyboard_hangle.ime.IMEDanmoeum;
import kr.jung.keyboard_hangle.ime.INEMaster;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private KeyboardView keyboardView;
    private Keyboard keyboard;

    private short mTypeInput = 0; //0:단모음, 1:쿼티, 2:
    private KeyTimer mKeyTimer = new KeyTimer(500, 100);
    private StringBuilder mCandidateString = new StringBuilder();
    private INEMaster mIME = new IMEDanmoeum();

    private TextView[] mCandText;
    private DictionaryDBHelper mCandDBHelper;


    @Override
    public View onCreateInputView() {
        //Timer Class
        mKeyTimer.start();

        //Candidate
        this.setCandidatesViewShown(true);

        //Set IME
        mIME = new IMEDanmoeum();
        mIME.resetIME();

        //Keyboard View
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        EditorInfo ei = getCurrentInputEditorInfo();
        setKeyboardLayout(selectKeyboardLayout(ei.inputType));
        keyboardView.setOnKeyboardActionListener(this);

        //Keyboard Theme
        setKeyboardTheme();

        return keyboardView;
    }

    @Override
    public View onCreateCandidatesView() {
        LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View wordBar = li.inflate(R.layout.keyboard_candidate_three, null);
        ConstraintLayout ll = wordBar.findViewById(R.id.keyboard_candidate);
        mCandText = new TextView[]{
                wordBar.findViewById(R.id.txt_candidate_center),
                wordBar.findViewById(R.id.txt_candidate_left),
                wordBar.findViewById(R.id.txt_candidate_right),
        };
        setCandidatesViewShown(true);
        setCandidatesView(wordBar);

        mCandDBHelper = new DictionaryDBHelper(this, Constants.DICT_DB_ALL, null, 1);

        return wordBar;
    }

    @Override
    public void onFinishInput() {
        completeCandidate();
    }

    private void setKeyboardTheme() {
        //Change Key Color
        Keyboard currentKeyboard = keyboardView.getKeyboard();
        List<Keyboard.Key> keys = currentKeyboard.getKeys();
        keyboardView.invalidateAllKeys();
        for(int i = 0; i < keys.size() - 1; i++ )
        {
            Keyboard.Key currentKey = keys.get(i);

            //If your Key contains more than one code, then you will have to check if the codes array contains the primary code
            if(currentKey.codes[0] == -5 || currentKey.codes[0] == -4 || currentKey.codes[0] == -3)
            {
                //currentKey.label = null;
                Drawable d = currentKey.icon;
                d.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN);
                Log.i("Sys","Key [" +i+"] Icon Color Change "+currentKey.codes[0]);
                currentKey.icon = d;
            }
        }
    }

    private int selectKeyboardLayout(int t) {
        switch(t) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
            case EditorInfo.TYPE_NUMBER_FLAG_DECIMAL:
            case EditorInfo.TYPE_NUMBER_FLAG_SIGNED:
            case EditorInfo.TYPE_DATETIME_VARIATION_DATE:
            case EditorInfo.TYPE_DATETIME_VARIATION_NORMAL:
            case EditorInfo.TYPE_DATETIME_VARIATION_TIME:
                Log.i("Sys","Set Keyboard Layout to Number");
                return R.xml.layout_number;
            default:
                Log.i("Sys","Set Keyboard Layout to danmoeum");
                return R.xml.layout_danmoeum;
        }
    }

    private void setKeyboardLayout(int layout) {
        keyboard = new Keyboard(this, layout);
        keyboardView.setKeyboard(keyboard);
    }

    @Override
    public void onPress(int primaryCode) {
        //Play Sound
        playClick(primaryCode);

        //Preview
        keyboardView.setPreviewEnabled(true);
    }

    @Override
    public void onRelease(int primaryCode) {
        //Preview
        keyboardView.setPreviewEnabled(checkPreview(primaryCode));
        Log.i("Key","Key Released "+primaryCode);
    }

    public boolean checkPreview(int i) {
        if (i < 0 || i == 32) {
            return false;
        }
        return true;
    }

    @Override
    public void onKey(int i, int[] ints) {
        InputConnection inputConnection = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        if (inputConnection == null) {
            return;
        }

        //Code Control
        switch (i) {
            case Keyboard.KEYCODE_DELETE :
                deleteOneLetter();
                break;
            case Keyboard.KEYCODE_SHIFT:
                break;
            case Keyboard.KEYCODE_DONE:
                completeCandidate();
                inputDone(inputConnection, info);
                break;
            case -3://Language
                //Complete Candidate
                completeCandidate();
                //Change Language
                InputMethodManager imm =(InputMethodManager) getSystemService((Context.INPUT_METHOD_SERVICE));
                boolean ttt;
                if (android.os.Build.VERSION.SDK_INT < 28) {
                    ttt=imm.switchToNextInputMethod(keyboardView.getWindowToken(), false);
                    Log.i("Sys","Change Keyboard (SDK Lower than 28)");
                } else {
                    ttt=switchToNextInputMethod(false);
                    Log.i("Sys","Change Keyboard (SDK Bigger than 28)");
                    if (!ttt) {
                        Log.i("Sys","Changed to Previous Keyboard");
                        ttt=switchToPreviousInputMethod();
                    }
                    if (!ttt) {
                        imm.showInputMethodPicker();
                    }
                }
                break;
            default:
                if (mIME.checkCode(i)) {
                    mIME.createLetter(i,!mKeyTimer.isTimerEnd);
                } else {
                    completeCandidate();
                    inputChar(i);
                }
                //Log.i("Hangul",(char) mLetters[0]+", "+(char) mLetters[1]+", "+(char) mLetters[2]);
        }

        //Others
        startKeyTimer();
        refreshCandidate();
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent e) {
        switch(i) {
            case -3://Lang
                Log.i("Sys","Lang Key Down");
                e.startTracking();
                return true;
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent e) {
        switch(i) {
            case -3://Language
                Log.i("Sys","Language Selector due to long press");
                InputMethodManager imm =(InputMethodManager) getSystemService((Context.INPUT_METHOD_SERVICE));
                imm.showInputMethodPicker();
                return true;
        }
        return false;
    }

    private void completeCandidate() {
        //Input
        InputConnection inputConnection = getCurrentInputConnection();
        inputConnection.commitText(mCandidateString.toString(), 1);
        mIME.mInfo.mCreateLetters=new StringBuilder();
        mIME.resetIME();
        refreshCandidate();
    }

    private void refreshCandidate() {
        mCandidateString=mIME.mInfo.mCreateLetters;
        replaceAll(mCandidateString,String.valueOf(mIME.mBlankWord),"");
        InputConnection inputConnection = getCurrentInputConnection();
        inputConnection.setComposingText(mCandidateString,1);
        Log.i("Cand","Candidate: "+mCandidateString);

        //Candidate View
        if (mCandidateString.toString().equals("")) {
            resetCandidateText();
            //When CandidateString is empty, clear all
        }
        List<CandidateInfo> l = mCandDBHelper.searchWord(mCandidateString.toString());
        int m = l.size();
        if (m > mCandText.length) {
            m=mCandText.length;
        }
        Log.i("Cand","Candidate DB Dynamics [Size: "+l.size()+"], [For Statement Maximum: "+m+"]");
        for(int i=0; i < m; i++) {
            mCandText[i].setText(l.get(i).getWord());
            Log.i("Cand","Candidate Found["+i+"]: "+l.get(i).getWord());
        }
    }

    public static StringBuilder replaceAll(StringBuilder sb, String find, String replace){
        return new StringBuilder(Pattern.compile(find).matcher(sb).replaceAll(replace));
    }

    private void inputDone(InputConnection ic, EditorInfo ei) {
        int type=ei.imeOptions & EditorInfo.IME_MASK_ACTION;
        switch(type) {
            case EditorInfo.IME_ACTION_UNSPECIFIED:
            case EditorInfo.IME_ACTION_NONE:
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_GO:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SEARCH));
                break;
            default:
                if ((ei.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                } else {
                    ic.performEditorAction(type);
                }
                break;
        }
    }

    //Delete
    private void deleteOneLetter() {
        Log.i("Key","One Letter Delete");
        mIME.deleteOrder();
        boolean a = mIME.mInfo.deleteOneLetter();
        if (!a) {
            InputConnection inputConnection = getCurrentInputConnection();
            CharSequence selectedText = inputConnection.getSelectedText(0);
            if (TextUtils.isEmpty(selectedText)) {
                inputConnection.deleteSurroundingText(1, 0);
            } else {
                inputConnection.commitText("", 1);
            }
        }
    }

    //Timer Tick
    private void startKeyTimer() {
        mKeyTimer.cancel();
        mKeyTimer = new KeyTimer(500, 100);
        mKeyTimer.start();
        Log.i("KEY", "Timer Began");
    }

    private void inputChar(int i) {
        //Input
        InputConnection inputConnection = getCurrentInputConnection();
        char code=(char) i;
        inputConnection.commitText(String.valueOf(code), 1);
    }

    private void playClick(int i) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch(i){
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private void resetCandidateText() {
        try {
            for (int i = 0; i < mCandText.length; i++) {
                mCandText[i].setText("");
            }
        } catch (NullPointerException e) {
            Log.e("Sys","Unable to candidate text");
        }
    }
}
