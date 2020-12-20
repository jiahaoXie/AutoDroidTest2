package SimHash;

import java.util.HashMap;
import java.util.Map;

/**
 * 新闻实体类
 * 
 * @author lyq
 * 
 */
public class News {
	// 新闻具体内容
	String content;
	// 新闻包含的词的个数统计值
	HashMap<String, Double> word2Count;

	public News(String content) {
		this.content = content;
		this.word2Count = new HashMap<String, Double>();
	}

	/**
	 * 将分词后的字符串进行关键词词数统计(这里可能需要根据胎记文本信息进行改动)
	 */
	public void statWords() {
		int index;
		int invalidCount;
		double count;

		// 词频
		double wordRate;
		String w;
		String[] array;

		invalidCount = 0;
		array = this.content.split("}");
//		 for (String str : array) {
		for (int i = 0; i < array.length; i++) {
			//过滤掉相邻相同的节点 
			if (i != array.length - 1 && array[i].equals(array[i + 1])) {
				continue;
			} else {
				w = array[i].replaceAll("text='*',|contentDesc='*',", "");
				// System.out.println("节点有效信息："+w);
				count = 0;
				if (this.word2Count.containsKey(w)) {
					count = this.word2Count.get(w);
				}

				// 做计数的更新
				count++;
				this.word2Count.put(w, count);
			}
		}

		// 进行总词语的记录汇总
		for (Map.Entry<String, Double> entry : this.word2Count.entrySet()) {
			w = entry.getKey();
			count = entry.getValue();

			wordRate = 1.0 * count / (array.length - invalidCount);
			this.word2Count.put(w, wordRate);
		}
	}

	/**
	 * 根据词语名称获取词频
	 * 
	 * @param word
	 *            词的名称
	 * 
	 */
	public double getWordFrequentValue(String word) {
		if (this.word2Count.containsKey(word)) {
			return this.word2Count.get(word);
		} else {
			return -1;
		}
	}

}
