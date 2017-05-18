package com.ansj.vec;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.ansj.vec.util.MapCount;
import com.ansj.vec.domain.HiddenNeuron;
import com.ansj.vec.domain.Neuron;
import com.ansj.vec.domain.WordNeuron;
import com.ansj.vec.util.Haffman;

public class Learn {

  private Map<String, Neuron> wordMap = new HashMap<>();
  /**
   * 训练多少个特征
   */
  private int layerSize = 200;

  /**
   * 上下文窗口大小
   */
  private int window = 5;

  private double sample = 1e-3;
  private double alpha = 0.025;
  private double startingAlpha = alpha;

  public int EXP_TABLE_SIZE = 1000;

  private Boolean isCbow = false;

  private double[] expTable = new double[EXP_TABLE_SIZE];

  private int trainWordsCount = 0;

  private int MAX_EXP = 6;

  private int vocabSize = 0;

  int hs = 1, negative = 0;

  int table_size = (int) 1e8;

  int[] table = new int[table_size];

  HashMap<Integer, Neuron> hashMap = new HashMap<Integer, Neuron>();//存储<index，WordNeuron>

  public Learn(Boolean isCbow, Integer layerSize, Integer window, Double alpha,
      Double sample) {
    createExpTable();
    if (isCbow != null) {
      this.isCbow = isCbow;
    }
    if (layerSize != null)
      this.layerSize = layerSize;
    if (window != null)
      this.window = window;
    if (alpha != null)
      this.alpha = alpha;
    if (sample != null)
      this.sample = sample;
  }

  public Learn() {
    createExpTable();
  }

  /**
   * trainModel
   * 
   * @throws IOException
   */
  private void trainModel(File file) throws IOException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new FileInputStream(file)))) {
      String temp = null;
      long nextRandom = 5;
      int wordCount = 0;
      int lastWordCount = 0;
      int wordCountActual = 0;
      while ((temp = br.readLine()) != null) {
        if (wordCount - lastWordCount > 10000) {
          System.out.println("alpha:" + alpha + "\tProgress: "
              + (int) (wordCountActual / (double) (trainWordsCount + 1) * 100)
              + "%");
          wordCountActual += wordCount - lastWordCount;
          lastWordCount = wordCount;
          alpha = startingAlpha
              * (1 - wordCountActual / (double) (trainWordsCount + 1));
          if (alpha < startingAlpha * 0.0001) {
            alpha = startingAlpha * 0.0001;
          }
        }
        String[] strs = temp.split(" ");
        wordCount += strs.length;
        List<WordNeuron> sentence = new ArrayList<WordNeuron>();
        for (int i = 0; i < strs.length; i++) {
          Neuron entry = wordMap.get(strs[i]);
          if (entry == null) {
            continue;
          }
          // The subsampling randomly discards frequent words while keeping the
          // ranking same
          if (sample > 0) {
            //应该为词频数cn
            double ran = (Math.sqrt(entry.cn / (sample * trainWordsCount)) + 1)
                * (sample * trainWordsCount) / entry.cn;
            nextRandom = nextRandom * 25214903917L + 11;
            if (ran < (nextRandom & 0xFFFF) / (double) 65536) {
              continue;
            }
          }
          sentence.add((WordNeuron) entry);
        }

        for (int index = 0; index < sentence.size(); index++) {
          nextRandom = nextRandom * 25214903917L + 11;
          if (isCbow) {
            cbowGram(index, sentence, (int) ((nextRandom % window) + nextRandom) % window);
          } else {
            skipGram(index, sentence, (int) ((nextRandom % window) + nextRandom) % window);//避免为负
          }
        }

      }
      System.out.println("Vocab size: " + wordMap.size());
      System.out.println("Words in train file: " + trainWordsCount);
      System.out.println("sucess train over!");
    }
  }

  /**
   * skip gram 模型训练
   * 
   * @param index
   * @param sentence
   * @param b
   */
  private void skipGram(int index, List<WordNeuron> sentence, int b) {
    // TODO Auto-generated method stub
    WordNeuron word = sentence.get(index);
    long nextRandom = b;
    int target;
    int label;
    int a, c = 0;
    for (a = b; a < window * 2 + 1 - b; a++) {
      if (a == window) {
        continue;
      }
      c = index - window + a;
      if (c < 0 || c >= sentence.size()) {
        continue;
      }

      double[] neu1e = new double[layerSize];// 误差项
      // HIERARCHICAL SOFTMAX
      List<Neuron> neurons = word.neurons;
      WordNeuron we = sentence.get(c);

      if (hs > 0) {
        for (int i = 0; i < neurons.size(); i++) {
          HiddenNeuron out = (HiddenNeuron) neurons.get(i);
          double f = 0;
          // Propagate hidden -> output
          for (int j = 0; j < layerSize; j++) {
            f += we.syn0[j] * out.syn1[j];
          }
          if (f <= -MAX_EXP || f >= MAX_EXP) {
            continue;
          } else {
            f = (f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2);
            f = expTable[(int) f];
          }
          // 'g' is the gradient multiplied by the learning rate
          double g = (1 - word.codeArr[i] - f) * alpha;
          // Propagate errors output -> hidden
          for (c = 0; c < layerSize; c++) {
            neu1e[c] += g * out.syn1[c];
          }
          // Learn weights hidden -> output
          for (c = 0; c < layerSize; c++) {
            out.syn1[c] += g * we.syn0[c];
          }
        }
      }

      // NEGATIVE SAMPLING
      if (negative > 0) {
        WordNeuron temp;
        for (int d = 0; d < negative + 1; d++) {
          if (d == 0) {
            target = word.index;
            temp = word;
            label = 1;
          } else {
            nextRandom = nextRandom * 25214903917l + 11;
            target = table[(int) (((nextRandom >> 16) % table_size) + table_size) % table_size];
//            if (target == 0) {
//              target = (int) (((nextRandom % (vocabSize - 1)) + vocabSize - 1) % (vocabSize - 1)) + 1;
//            }
            if (target == word.index) {
              continue;
            }
            temp = (WordNeuron) hashMap.get(target);
            label = 0;
          }
          double f = 0;
          for (c = 0; c < layerSize; c++) {
            f += we.syn0[c] * temp.syn1neg[c];
          }
          double g;
          if (f > MAX_EXP) {
            g = (label - 1) * alpha;
          } else if (f < -MAX_EXP) {
            g = (label - 0) * alpha;
          } else {
            g = (label - expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
          }
          for (c = 0; c < layerSize; c++) {
            neu1e[c] += g * temp.syn1neg[c];
          }
          for (c = 0; c < layerSize; c++) {
            temp.syn1neg[c] += g * we.syn0[c];
          }
        }
      }

      // Learn weights input -> hidden
      for (int j = 0; j < layerSize; j++) {
        we.syn0[j] += neu1e[j];
      }
    }

  }

  /**
   * 词袋模型
   * 
   * @param index
   * @param sentence
   * @param b
   */
  private void cbowGram(int index, List<WordNeuron> sentence, int b) {
    WordNeuron word = sentence.get(index);
    long nextRandom = b;
    int target;
    int label;
    int a, c = 0;

    List<Neuron> neurons = word.neurons;
    double[] neu1e = new double[layerSize];// 误差项
    double[] neu1 = new double[layerSize];// 误差项
    WordNeuron last_word = null;

    for (a = b; a < window * 2 + 1 - b; a++)
      if (a != window) {
        c = index - window + a;
        if (c < 0)
          continue;
        if (c >= sentence.size())
          continue;
        last_word = sentence.get(c);
        if (last_word == null)
          continue;
        for (c = 0; c < layerSize; c++)
          neu1[c] += last_word.syn0[c];
      }

    // HIERARCHICAL SOFTMAX
    if (hs > 0) {
      for (int d = 0; d < neurons.size(); d++) {
        HiddenNeuron out = (HiddenNeuron) neurons.get(d);
        double f = 0;
        // Propagate hidden -> output
        for (c = 0; c < layerSize; c++)
          f += neu1[c] * out.syn1[c];
        if (f <= -MAX_EXP)
          continue;
        else if (f >= MAX_EXP)
          continue;
        else
          f = expTable[(int) ((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
        // 'g' is the gradient multiplied by the learning rate
        // double g = (1 - word.codeArr[d] - f) * alpha;
        // double g = f*(1-f)*( word.codeArr[i] - f) * alpha;
        double g = f * (1 - f) * (word.codeArr[d] - f) * alpha;
        //
        for (c = 0; c < layerSize; c++) {
          neu1e[c] += g * out.syn1[c];
        }
        // Learn weights hidden -> output
        for (c = 0; c < layerSize; c++) {
          out.syn1[c] += g * neu1[c];
        }
      }
    }

    // NEGATIVE SAMPLING
    if (negative > 0) {
      WordNeuron temp = null;
      for (int d = 0; d < negative + 1; d++) {
        if (d == 0) {
          target = word.index;
          temp = word;
          label = 1;
        } else {
          nextRandom = nextRandom * 25214903917l + 11;
          target = table[(int) (((nextRandom >> 16) % table_size) + table_size) % table_size];

          if (target == word.index){
            continue;
          }
          temp = (WordNeuron) hashMap.get(target);
          label = 0;
        }
        double f = 0;
        for (c = 0; c < layerSize; c++) {
          f += last_word.syn0[c] * temp.syn1neg[c];
        }
        double g;
        if (f > MAX_EXP){
          g = (label - 1) * alpha;
        } else if (f < -MAX_EXP){
          g = (label - 0) * alpha;
        } else {
          g = (label - expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
        }
        for (c = 0; c < layerSize; c++) {
          neu1e[c] += g * temp.syn1neg[c];
        }
        for (c = 0; c < layerSize; c++) {
          temp.syn1neg[c] += g * neu1[c];
        }
      }
    }

    for (a = b; a < window * 2 + 1 - b; a++) {
      if (a != window) {
        c = index - window + a;
        if (c < 0)
          continue;
        if (c >= sentence.size())
          continue;
        last_word = sentence.get(c);
        if (last_word == null)
          continue;
        for (c = 0; c < layerSize; c++)
          last_word.syn0[c] += neu1e[c];
      }

    }
  }

  /**
   * 每个单词的能量分布表，table在负采样中用到
   */
  private void InitUnigramTable() {
    int a, i;
    long train_words_pow = 0;
    double d1, power = 0.75;

    for (Entry<String, Neuron> entry : wordMap.entrySet()) {
      WordNeuron temp = (WordNeuron) entry.getValue();
      hashMap.put(temp.index, temp);
      train_words_pow += Math.pow(temp.cn, power);
    }
//    for (a = 0; a < vocabSize; a++) {
//      train_words_pow += Math.pow(wordMap.get(hashMap.get(a)).cn, power);
//    }
    i = 0;
    d1 = Math.pow(hashMap.get(i).cn, power) / (double) train_words_pow;
    for (a = 0; a < table_size; a++) {
      table[a] = i;
      if (a / (double) table_size > d1) {
        i++;
        d1 += Math.pow(hashMap.get(i).cn, power) / (double) train_words_pow;
      }
      if (i >= hashMap.size()) {
        i = hashMap.size() - 1;
      }
    }
  }

  /**
   * 统计词频
   * 
   * @param file
   * @throws IOException
   */
  private void readVocab(File file) throws IOException {
    MapCount<String> mc = new MapCount<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new FileInputStream(file)))) {
      String temp = null;
      while ((temp = br.readLine()) != null) {
        String[] split = temp.split(" ");
        trainWordsCount += split.length;
        for (String string : split) {
          mc.add(string);
        }
      }
    }

    if (hs > 0) {
      for (Entry<String, Integer> element : mc.get().entrySet()) {
        wordMap.put(element.getKey(), new WordNeuron(element.getKey(),
                (double) element.getValue() / mc.size(), layerSize));
      }
    }

    if (negative > 0) {
      List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(mc.get().entrySet());

      Collections.sort(list,new Comparator<Entry<String,Integer>>() {
        //降序排序
        public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
          return -o1.getValue().compareTo(o2.getValue());
        }
      });
      int index = 0;

      for (Entry<String, Integer> element : list) {
        wordMap.put(element.getKey(), new WordNeuron(element.getKey(), element.getValue(), index++,
                (double) element.getValue() / mc.size(), layerSize));
      }
    }
    vocabSize = wordMap.size();
    System.out.println("vocabSize " + vocabSize + "    " + "trainWordsCount " + trainWordsCount);
  }

  /**
   * 对文本进行预分类
   * 
   * @param files
   * @throws IOException
   * @throws FileNotFoundException
   */
  private void readVocabWithSupervised(File[] files) throws IOException {
    for (int category = 0; category < files.length; category++) {
      // 对多个文件学习
      MapCount<String> mc = new MapCount<>();
      try (BufferedReader br = new BufferedReader(new InputStreamReader(
          new FileInputStream(files[category])))) {
        String temp = null;
        while ((temp = br.readLine()) != null) {
          String[] split = temp.split(" ");
          trainWordsCount += split.length;
          for (String string : split) {
            mc.add(string);
          }
        }
      }
      for (Entry<String, Integer> element : mc.get().entrySet()) {
        double tarFreq = (double) element.getValue() / mc.size();
        if (wordMap.get(element.getKey()) != null) {
          double srcFreq = wordMap.get(element.getKey()).freq;
          if (srcFreq >= tarFreq) {
            continue;
          } else {
            Neuron wordNeuron = wordMap.get(element.getKey());
            wordNeuron.category = category;
            wordNeuron.freq = tarFreq;
          }
        } else {
          wordMap.put(element.getKey(), new WordNeuron(element.getKey(),
              tarFreq, category, layerSize));
        }
      }
    }
  }

  /**
   * Precompute the exp() table f(x) = x / (x + 1)
   */
  private void createExpTable() {
    for (int i = 0; i < EXP_TABLE_SIZE; i++) {
      expTable[i] = Math.exp(((i / (double) EXP_TABLE_SIZE * 2 - 1) * MAX_EXP));
      expTable[i] = expTable[i] / (expTable[i] + 1);
    }
  }

  /**
   * 根据文件学习
   * 
   * @param file
   * @throws IOException
   */
  public void learnFile(File file) throws IOException {
    readVocab(file);
    if (hs > 0) {
      new Haffman(layerSize).make(wordMap.values());

      // 查找每个神经元
      for (Neuron neuron : wordMap.values()) {
        ((WordNeuron) neuron).makeNeurons();
      }
    }

    if(negative > 0){
      InitUnigramTable();
      System.out.println("InitUnigramTable");
    }

    trainModel(file);
  }

  /**
   * 根据预分类的文件学习
   * 
   * @param summaryFile
   *          合并文件
   * @param classifiedFiles
   *          分类文件
   * @throws IOException
   */
  public void learnFile(File summaryFile, File[] classifiedFiles)
      throws IOException {
    readVocabWithSupervised(classifiedFiles);
    new Haffman(layerSize).make(wordMap.values());
    // 查找每个神经元
    for (Neuron neuron : wordMap.values()) {
      ((WordNeuron) neuron).makeNeurons();
    }
    trainModel(summaryFile);
  }

  /**
   * 保存模型
   */
  public void saveModel(File file) {
    // TODO Auto-generated method stub

    try (DataOutputStream dataOutputStream = new DataOutputStream(
        new BufferedOutputStream(new FileOutputStream(file)))) {
      dataOutputStream.writeInt(wordMap.size());
      dataOutputStream.writeInt(layerSize);
      double[] syn0 = null;
      for (Entry<String, Neuron> element : wordMap.entrySet()) {
        dataOutputStream.writeUTF(element.getKey());
        syn0 = ((WordNeuron) element.getValue()).syn0;
        for (double d : syn0) {
          dataOutputStream.writeFloat(((Double) d).floatValue());
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * 以txt格式保存模型
   */
  public void saveTxtModel(File file) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
      bufferedWriter.write(String.valueOf(wordMap.size()));
      bufferedWriter.write(String.valueOf(layerSize));
      bufferedWriter.newLine();
      double[] syn0 = null;
      for (Entry<String, Neuron> element : wordMap.entrySet()) {
        bufferedWriter.write(element.getKey());
        syn0 = ((WordNeuron) element.getValue()).syn0;
        for (double d : syn0) {
          bufferedWriter.write(String.valueOf((float) d));
        }
        bufferedWriter.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int getLayerSize() {
    return layerSize;
  }

  public void setLayerSize(int layerSize) {
    this.layerSize = layerSize;
  }

  public int getWindow() {
    return window;
  }

  public void setWindow(int window) {
    this.window = window;
  }

  public double getSample() {
    return sample;
  }

  public void setSample(double sample) {
    this.sample = sample;
  }

  public double getAlpha() {
    return alpha;
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
    this.startingAlpha = alpha;
  }

  public Boolean getIsCbow() {
    return isCbow;
  }

  public void setIsCbow(Boolean isCbow) {
    this.isCbow = isCbow;
  }

  public static void main(String[] args) throws IOException {
    Learn learn = new Learn();
    long start = System.currentTimeMillis();
    learn.learnFile(new File("library/xh.txt"));
    System.out.println("use time " + (System.currentTimeMillis() - start));
    learn.saveModel(new File("library/javaVector"));

  }
}
