在NLPChina/Word2Vec的基础上做了如下改进：

### 支持训练预分类的文本

​	在项目过程中发现原始文本直接按照词频生成的hoffman树会导致很多词义相反的词向量距离接近，为了优化这种情况，支持将不同的文本预分类，构造hoffman树

- 将Neuron中的freq修改为double类型
- 在Neuron中增加category属性，默认为-1
- 增加支持预分类的方法learnFile(File summaryFile, File[] classifiedFiles)

  ​修改之后重新训练模型进行了测试，兼容之前的训练方法，训练速度并没有显著下降，训练效果基本一致