package test;

import java.io.IOException;

import com.ansj.vec.Word2VEC;

public class Test {
    public static void main(String[] args) throws IOException {
        Word2VEC w1 = new Word2VEC() ;
        w1.loadGoogleModel("library/vectors1.bin") ;
        
        Word2VEC w2 = new Word2VEC() ;
        w2.loadGoogleModel("library/vectors2.bin") ;
        
        System.out.println(w1.distance("毛泽东"));
        System.out.println(w2.distance("毛泽东"));
        
        Word2VEC w3 = new Word2VEC() ;
        w3.loadJavaModel("library/javaVector") ;
        System.out.println(w3.distance("毛泽东"));
    }
}
