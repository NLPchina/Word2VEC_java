package test;

import java.io.File;
import java.io.IOException;

import com.ansj.vec.Learn;

public class LearnTest {

    public static void main(String[] args) throws IOException {
        Learn learn = new Learn();
        learn.learnFile(new File("corpus.txt"));
        learn.saveModel(new File("model.bin"));
    }
}
