package com.ansj.vec.domain;

public abstract class Neuron implements Comparable<Neuron> {
  public double freq;
  public Neuron parent;
  public int code;
  // 语料预分类
  public int category = -1;

  @Override
  public int compareTo(Neuron neuron) {
    if (this.category == neuron.category) {
      if (this.freq > neuron.freq) {
        return 1;
      } else {
        return -1;
      }
    } else if (this.category > neuron.category) {
      return 1;
    } else {
      return -1;
    }
  }
}
