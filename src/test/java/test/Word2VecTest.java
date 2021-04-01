package test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import com.ansj.vec.Word2vec;
import com.ansj.vec.util.WordKmeans;
import com.ansj.vec.util.WordKmeans.Classes;

public class Word2VecTest {

    public static void main(String[] args) throws IOException {
        Word2vec vec = new Word2vec();
        vec.loadJavaModel("model.bin");
        // 距离最近的词
        System.out.println(vec.distance("邓小平"));
        System.out.println(vec.distance("魔术队"));
        System.out.println(vec.distance("过年"));
        System.out.println(vec.distance(Arrays.asList("香港", "澳门")));
        // // 计算词之间的距离
        HashMap<String, float[]> map = vec.getWordMap();
        float[] center1 = map.get("春节");
        float[] center2 = map.get("过年");
        double dics = 0;
        for (int i = 0; i < center1.length; i++) {
            dics += center1[i] * center2[i];
        }
        System.out.println(dics);
        // 距离计算
        System.out.println(vec.analogy("毛泽东", "邓小平", "毛泽东思想"));
        System.out.println(vec.analogy("女人", "男人", "女王"));
        System.out.println(vec.analogy("北京", "中国", "巴黎"));
        // 聚类
        WordKmeans wordKmeans = new WordKmeans(vec.getWordMap(), 50, 50);
        Classes[] explain = wordKmeans.explain();
        for (int i = 0; i < explain.length; i++) {
            System.out.println("--------" + i + "---------");
            System.out.println(explain[i].getTop(10));
        }
    }
}
