import DataStructure.Production;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by yuanzi on 2018/6/10.
 */
public class Parser {
    static Formatter formatter = new Formatter(System.out);//格式化输出
    public static String GRAMMA = "src/LLGrammar.txt";//文法路径
    public static String CODE = "src/LLtest.txt";// 测试代码路径
    private File file = new File(GRAMMA);
    private File codefile = new File(CODE);

    private ArrayList<String> VnSet = new ArrayList<String>();//非终结符
    private ArrayList<String> VtSet = new ArrayList<String>();//终结符
    private ArrayList<String> Grammar = new ArrayList<>();//文法
    private ArrayList<Production> productions = new ArrayList<>();//产生式
    private HashMap<String, ArrayList<String>> FirstSet = new HashMap<>();//first 集
    private HashMap<String, ArrayList<String>> FollowSet = new HashMap<>();//follow 集


    private String [][] table;//预测分析表,表中为产生式
    private String [][] showtable;//预测分析表,表中为产生式序号

    private String action="";//定义动作
    public Stack<String> analyzeStatck = new Stack<String>();//符号栈

    private ArrayList<String> Token = new ArrayList<>();//

    public void Parse(){
        DeleteOR(file);
        GetProductions();
        GetVnSet();
        GetVtSet();
        System.out.println("非终结符:");
        OutPut(VnSet);
        System.out.println("终结符:");
        OutPut(VtSet);
        GetFirstSet();
        GetFollowSet();
        GetSelect();
        OutPut(FirstSet,"first");
        OutPut(FollowSet,"follow");
        OutPutSelect(productions);
        CreatePredictTable();
        OutPut(showtable);
        Token = GetToken(codefile);

        Analysis();
    }

    //输出函数的几个重载
    public void OutPut(ArrayList<String> al){
        for(String s:al){
            System.out.print(s+" ");
        }
        System.out.println("The length is : "+al.size()+"\n");
    }
    public void OutPut(HashMap<String,ArrayList<String>> fs,String sw){
        for(HashMap.Entry<String,ArrayList<String>> entry:fs.entrySet()){
            String ss="";
            for(String s : entry.getValue()){
                ss = ss + s+" ";
            }
            formatter.format("%-8s %-15s %-6s %-25s %-5s\n",sw+" ( ",entry.getKey(),") = { ",ss,"}");
//            System.out.println("Follow("+entry.getKey()+") = { "+ss+" }");
        }

    }
    public void OutPut(String[][] table){
        for(int i =0;i< VnSet.size()+1;i++){
            for(int j =0;j<VtSet.size()+1;j++){
                formatter.format("%-15s %-3s",table[i][j]," ");
            }
            System.out.println();
        }
    }

    //输出每一个产生式 select 集
    public void OutPutSelect(ArrayList<Production> ps){
        for(Production p:productions){
            String s = "";
            String ss = "";
            for(int i = 0;i < p.SelectSet.size();i++){
                s=s+p.SelectSet.get(i)+" ";
            }
            for(int j = 0;j < p.getRight().length;j++){
                ss=ss+p.getRight()[j]+" ";
            }
//            System.out.println("select( "+p.getLeft()+" -> "+ss+" ) = { "+s+" }");
            formatter.format("%-8s %-15s %5s %38s %5s %30s %3s\n","select (",p.getLeft(),"->",ss," ) = {",s,"}");
        }
    }

    //获取 Latex 类中的关于代码的词法分类
    public ArrayList<String> GetToken(File f){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            String s="";
            while ((line=reader.readLine())!=null) {
                s = s+line+"\n";
            }
            System.out.println(s);
            reader.close();
            return new Latex(s).analyze();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }

    //删除文法中的 | 符号,分割成多个产生式
    public void DeleteOR(File f){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            String left;
            String[] right;
            while ((line=reader.readLine())!=null) {
                left = line.split("->")[0].trim();
                if(line.split("->")[1].contains("|")){
                    right = line.split("->")[1].split(" \\| ");
                    for(String s : right){
                        //System.out.print(s+"\n");
                        Grammar.add(left+" -> "+s);
                    }
                }else{
                    Grammar.add(line);
                }
            }

            reader.close();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    //获取产生式
    public void GetProductions(){
        String left;
        String right;
        int count = 0;
        for(String line:Grammar){

            left = line.split("->")[0].trim();
            right = line.split("->")[1].trim();
            Production production = new Production(left, right.split(" "),count);
            productions.add(production);
            count++;
//            production.output();

        }
    }

    //获取非终结符集合
    public void GetVnSet(){
        for(String line:Grammar){
            String left;
            left = line.split("->")[0].trim();
            if(VnSet.contains(left))
                continue;
            else
                VnSet.add(left);

        }

    }

    //获取终结符集合
    public void GetVtSet(){
        String[] rights;
        for (Production p : productions) {
            rights = p.getRight();
//            p.output();
            //从右侧寻找终结符
            for (int j = 0; j < rights.length; j++) {
                if(VtSet.contains(rights[j])||rights[j].equals("@")) {

                    continue;
                }
                else if(!VnSet.contains(rights[j]))
                    VtSet.add(rights[j]);
            }
        }
    }
    
    //获取 first 集
    public void GetFirstSet()
    {

        ArrayList<String> first;
        //如果X为终结符,First(X)=X
        for (int i = 0; i < VtSet.size(); i++) {
            first = new ArrayList<String>();
            first.add(VtSet.get(i));
            FirstSet.put(VtSet.get(i), first);
        }

        //先占位
        for (int i = 0; i < VnSet.size(); i++) {
            first = new ArrayList<String>();
            FirstSet.put(VnSet.get(i), first);
        }

        // 如果X->ε是产生式，把ε加入First(X)
        // 如果X是非终结符，如X->YZW。从左往右扫描产生式右部，把First(Y)加入First(X)。
        // 如果First(Y)不包含ε，表示Y不可为空，便不再往后处理；
        // 如果First(Y)包含ε，表示Y可为空，则处理Z，依次类推。
        boolean Flag;
        while (true) {
            Flag = true;
            String left;
            String right;
            String[] rights;
            for (int i = 0; i < productions.size(); i++) {
                //遍历每一个产生式
                left = productions.get(i).getLeft();
                rights = productions.get(i).getRight();
                for (int j = 0; j < rights.length; j++) {
                    right = rights[j];
                    if(!right.equals("@")) {
                        for (int l = 0; l < FirstSet.get(right).size(); l++) {
                            if(FirstSet.get(left).contains(FirstSet.get(right).get(l))){
                                continue;
                            }
                            else {
                                FirstSet.get(left).add(FirstSet.get(right).get(l));
                                Flag=false;
                            }
                        }
                    }
                    //如果右部产生空集
                    if (isCanBeNull(right)) {
                        continue;
                    }
                    else {
                        break;
                    }
                }
            }
            if (Flag == true) {
                break;
            }
        }
    }
    //判断是否产生空集
    public boolean isCanBeNull(String symbol) {
        String[] rights;
        for (int i = 0; i < productions.size(); i++) {
            //找到产生式
            if (productions.get(i).getLeft().equals(symbol)) {
                rights = productions.get(i).getRight();
                if (rights[0].equals("@")) {
                    return true;
                }
            }
        }
        return false;
    }
    /*
    在计算First(X)集之后的基础上

    1.$属于FOLLOW(S)，S是开始符
    2.查找输入的所有产生式，确定X后紧跟的终结符
    3.如果存在A->αBβ，（α、β是任意文法符号串，A、B为非终结符），把first(β)的非空符号加入follow(B)
    4.如果存在A->αB或A->αBβ 但first(β)包含空，把follow(A)加入follow(B)


     */
    public void GetFollowSet(){
        {
            //所有非终结符的follow集初始化一下
            ArrayList<String> follow;
            for (int i = 0; i < VnSet.size(); i++) {
                follow = new ArrayList<String>();
                FollowSet.put(VnSet.get(i), follow);
            }
            //将$加入到follow(S)中
            FollowSet.get(productions.get(0).getLeft()).add("$");

            boolean flag;
            boolean fab;
            while (true) {
                flag = true;
                //循环，遍历所有产生式
                for (int i = 0; i < productions.size(); i++) {
                    String left;
                    String right;
                    String[] rights;
                    rights = productions.get(i).getRight();
                    for (int j = 0; j < rights.length; j++) {
                        right = rights[j];

                        //非终结符的情况
                        if (VnSet.contains(right)) {
                            fab = true;
                            for(int k = j+1; k < rights.length; k++) {
                                //查找first集
                                for(int v = 0; v < FirstSet.get(rights[k]).size(); v++) {
                                    //将后一个元素的first集加入到前一个元素的follow集中
                                    if(FollowSet.get(right).contains(FirstSet.get(rights[k]).get(v))) {
                                        continue;
                                    }
                                    else {
                                        FollowSet.get(right).add(FirstSet.get(rights[k]).get(v));
                                        flag=false;
                                    }
                                }
                                if (isCanBeNull(rights[k])) {
                                    continue;
                                }
                                else {
                                    fab = false;
                                    break;
                                }
                            }
                            if(fab) {
                                left = productions.get(i).getLeft();
                                for (int p = 0; p < FollowSet.get(left).size(); p++) {
                                    if (FollowSet.get(right).contains(FollowSet.get(left).get(p))) {
                                        continue;
                                    }
                                    else {
                                        FollowSet.get(right).add(FollowSet.get(left).get(p));
                                        flag = false;
                                    }
                                }
                            }
                        }
                    }
                }
                if(flag==true){
                    break;
                }
            }
        }
    }
    public void GetSelect(){
        String left;
        String right;
        String[] rights;
        ArrayList<String> follow = new ArrayList<String>();
        ArrayList<String> first = new ArrayList<String>();

        for (int i = 0; i < productions.size(); i++) {
            left = productions.get(i).getLeft();
            rights = productions.get(i).getRight();
            if(rights[0].equals("@")) {
                // select(i) = follow(A)
                follow = FollowSet.get(left);
                for (int j = 0; j < follow.size(); j++) {
                    if(productions.get(i).SelectSet.contains(follow.get(j))){
                        continue;
                    }
                    else {
                        productions.get(i).SelectSet.add(follow.get(j));
                    }
                }
            }
            //如果文法G的第i个产生式为A→aβ，则定义
            //SELECT(i)={a}
            else {
                boolean flag = true;
                for (int j = 0; j < rights.length; j++) {
                    right = rights[j];
                    first = FirstSet.get(right);
                    for (int v = 0; v < first.size(); v++) {
                        if (productions.get(i).SelectSet.contains(first.get(v))) {
                            continue;
                        }
                        else {
                            productions.get(i).SelectSet.add(first.get(v));
                        }
                    }
                    if(isCanBeNull(right)) {
                        continue;
                    }
                    else {
                        flag = false;
                        break;
                    }
                }
                //First集中有空
                if (flag) {
                    follow = FollowSet.get(left);
                    for (int j = 0; j < follow.size(); j++) {
                        if (productions.get(i).SelectSet.contains(follow.get(j))) {
                            continue;
                        }
                        else {
                            productions.get(i).SelectSet.add(follow.get(j));
                        }
                    }
                }
            }
        }
    }

    public int WhereAreMe(ArrayList<String> as,String s){
        for(int i = 0;i<as.size();i++){
            if(s.equals(as.get(i))){
                return i;
            }
        }
        return -1;
    }

    /*
    对于每个产生式A->α

    first(α)中的终结符a，把A->α加入M[A,a]
    如果空串在first(α)中，对于follow(A)中的终结符b，把A->α加入M[A,b]
    如果空串在first(α)中，且’$’
    也在follow(A)中，把A->α加入M[A,$]中
     */
    public void CreatePredictTable() {

        // 预测分析表初始化
        table = new String[VnSet.size() + 1][VtSet.size() + 2];
        showtable = new String[VnSet.size() + 1][VtSet.size() + 2];
        table[0][0] = "Vn/Vt";
        showtable[0][0] = "Vn/Vt";
        //初始化首行首列
        for (int i = 0; i < VtSet.size(); i++) {
            table[0][i + 1] = VtSet.get(i);
            showtable[0][i + 1] = VtSet.get(i);
        }
        table[0][VtSet.size() + 1] = "$";
        showtable[0][VtSet.size() + 1] = "$";
        for (int i = 0; i < VnSet.size(); i++) {
            table[i + 1][0] = VnSet.get(i) + "";
            showtable[i + 1][0] = VnSet.get(i) + "";
        }
        //全部置error
        for (int i = 0; i < VnSet.size(); i++)
            for (int j = 0; j < VtSet.size()+1; j++) {
                table[i + 1][j + 1] = "";
                showtable[i + 1][j + 1] = "";
            }
        VtSet.add("$");
        for(int i = 0;i<productions.size();i++){
            Production p = productions.get(i);
            for(String vt : p.SelectSet){
                int row = WhereAreMe(VnSet,p.getLeft());
                int col = WhereAreMe(VtSet,vt);
//                System.out.println(row+" "+col);
                if(row==-1||col==-1){
                    System.out.println("ERROR");
                }
                else {
                    table[row+1][col+1] = p.ToString();
                    showtable[row+1][col+1] = p.getCount()+"";
                }
            }
        }

//
    }

// M函数,见龙书144,
    public String M(String X,String a){

        for (int i = 0; i < VnSet.size() + 1; i++) {
            if (table[i][0].equals(X))
                for (int j = 0; j < VtSet.size() + 1; j++) {
                    if (table[0][j].equals(a))
                        return table[i][j];
                }
        }
        return "";
    }

    //通过产生式的 String 返回产生式
    public Production GetProductionByString(String s){
        for(Production p:productions){
            if(p.ToString().equals(s)){
                return p;
            }
        }
        return null;
    }

    /*
    龙书第三版p144
    输入一个串，文法G的预测分析表，输出推导过程

    $ 和 开始符S入栈
    栈顶符号X，index指向分析串的待分析符号a
    栈非空执行以下循环

    如果X==a，表示匹配，符号栈弹出一个，index++指向下一个符号
    否则，如果X是终结符，出错
    否则，如果查表为error，出错
    否则，查表正确，弹出栈顶符号，把其右部各个符号进栈
    令X为栈顶符号
     */

    public void Analysis(){
        formatter.format("%-9s %-16s %-5s %-40s %-50s\n","已匹配","","动作","","栈");
        int index = 0;
        analyzeStatck.push("$");
        analyzeStatck.push("program");
        String X = analyzeStatck.peek();
        while (!X.equals("$")) {
            String a = Token.get(index);
            if (X.equals(a)) {
                action = "match "+X;
                analyzeStatck.pop();
                index++;
            } else if (VtSet.contains(X)) {
                System.out.println("ERROR!!!!!!!!!!");
                return;
            }
            else if (M(X, a).equals("")) {
                System.out.println("ERROR!!!!!!!!!!");
                return;
            }
            else if (GetProductionByString(M(X,a)).getRight()[0].equals("@")) {
                analyzeStatck.pop();
                action = GetProductionByString(M(X, a)).ToString();
            } else {
                String str = M(X, a);
                if (!str.equals("")) {
                    action = GetProductionByString(str).ToString();
                    analyzeStatck.pop();
                    int len = GetProductionByString(str).getRight().length;
                    for (int i = len - 1; i >= 0; i--)
                        analyzeStatck.push(GetProductionByString(str).getRight()[i]);
                } else {
                    System.out.println("error at '" + Token.get(index) + " in " + index);
                    return;
                }
            }
            X = analyzeStatck.peek();
            String stackString="";
            for(String s : analyzeStatck){

                stackString=stackString+s+"  ";

            }

            if(action.split(" ")[0].equals("match")){
                formatter.format("%-9s %-16s %-5s %-40s %-50s\n",action.split(" ")[1],"","","",stackString);
            }else{
                Production p =GetProductionByString(action);
                formatter.format("%-9s %-16s %-5s %-40s %-50s\n","",p.getLeft()," -> ",p.RightToString(),stackString);
            }
        }
        formatter.format("%20s\n","$");
        formatter.format("%20s\n","分析成功!");
    }
}
