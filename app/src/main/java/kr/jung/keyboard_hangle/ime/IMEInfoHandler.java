package kr.jung.keyboard_hangle.ime;

public class IMEInfoHandler {
    public int mDeleteLetters=0;
    public StringBuilder mCreateLetters = new StringBuilder();

    public IMEInfoHandler() {

    }

    public boolean deleteOneLetter() {
        int a = mCreateLetters.length();
        if (a > 0) {
            mCreateLetters.deleteCharAt(a-1);
            return true;
        } else {
            return false;
        }
    }
}
