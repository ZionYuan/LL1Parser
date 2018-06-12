package DataStructure;

import java.util.ArrayList;

/**
 * Created by yuanzi on 2018/6/11.
 */
public class Production {


    private int count;
    private String left;
    private String[] right;
    //select集
    public ArrayList<String> SelectSet = new ArrayList<String>();

    public Production(String left, String[] right,int count) {
        this.left = left;
        this.right = right;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String[] getRight() {
        return right;
    }

    public void setRight(String[] right) {
        this.right = right;
    }

    public void output(){
        String s = "";
        for(int i = 0;i<right.length;i++){
            s = s + right[i]+" ";
        }
        System.out.println(left+"->"+s);
    }
    public String ToString(){
        String s = "";
        for(int i = 0;i<right.length;i++){
            if(i!=right.length-1) {
                s = s + right[i] + " ";
            }
            else{s = s + right[i];}
        }
        return left+" -> "+s;
    }
    public String RightToString(){
        String s = "";
        for(int i = 0;i<right.length;i++){
            if(i!=right.length-1) {
                s = s + right[i] + " ";
            }
            else{s = s + right[i];}
        }
        return s;
    }

}
