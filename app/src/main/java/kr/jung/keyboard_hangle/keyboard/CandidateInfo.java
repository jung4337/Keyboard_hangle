package kr.jung.keyboard_hangle.keyboard;

public class CandidateInfo {

    public String mWord;
    public int mCount;

    public CandidateInfo() {
        mWord="";
        mCount=0;
    }

    public String getWord() {
        return mWord;
    }

    public int getCount() {
        return mCount;
    }

    public void setWord(String s) {
        mWord=s;
    }

    public void setCount(int c) {
        mCount=c;
    }
}