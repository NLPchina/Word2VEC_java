package test;

import java.io.IOException;

import org.ansj.splitWord.analysis.CRFAnalysis;

import com.ansj.vec.Word2VEC;

public class Test {
    public static void main(String[] args) throws IOException {
//        Word2VEC w1 = new Word2VEC() ;
//        w1.loadJavaModel("library/vector.mod") ;
//        
//        System.out.println(w1.distance("奥尼尔"));
        
        System.out.println(CRFAnalysis.parse("汤姆克鲁斯的英文名字很苦"));
    }
}
