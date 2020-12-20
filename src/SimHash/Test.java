package SimHash;
/**
 * 
 * @author xjh 2018.05.28
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 新闻实体类
 * 
 * @author lyq
 * 
 */
class News01 {
	// 新闻具体内容
	String content;
	// 新闻包含的词的个数统计值
	HashMap<String, Double> word2Count;

	public News01(String content) {
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
		array = this.content.split("}");	//利用}将其切割为一个个节点
		for (String str : array) {
			w=str.replaceAll("text='*',|contentDesc='*',","");
			
			
//			int i=str.indexOf("className='");	//找到className='的位置
//			System.out.println("i= "+i);
//			int j=str.indexOf("'", i+20);
//			System.out.println("j= "+j);
//			w = str.substring(i,j+1);	//w就是树节点的布局类型信息
			System.out.println("布局信息为："+w);			
			
//			index = str.indexOf('}');	//这里表示截取胎记中的每一个节点
//			if (index == -1) {
//				continue;
//			}
//			w = str.substring(0, index);	//w为一个节点的完整信息

			// 原来是只过滤掉标点符/wn，逗号(这里 需要根据自己的文本信息进行信息过滤)		
			//这里应该改成过滤掉text和context中的内容  因为在胎记信息中text和context中的内容属于干扰信息 或者我们只需要classname中的信息
//			if (str.contains("wn") || str.contains("wd")) {
//				invalidCount++;
//				continue;
//			}

			count = 0;
			if (this.word2Count.containsKey(w)) {
				count = this.word2Count.get(w);
			}

			// 做计数的更新
			count++;
			this.word2Count.put(w, count);
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
		if(this.word2Count.containsKey(word)){
			return this.word2Count.get(word);
		}else{
			return -1;
		}
	}
	
	
}


public class Test {
	//写一个测试的主方法	
		public static void main(String[] args) {
			String str="LayoutNode{index=0, text='', className='android.widget.FrameLayout', packageName='com.microvirt.launcher', contentDesc='', checkable=false, checked=false, clickable=false, enabled=true, focusable=false, focuesd=false, scrollable=false, longClickable=false, password=false, selected=false, bound=[0, 0, 1024, 576]}"
					+ "LayoutNode{index=0, text='', className='android.widget.FrameLayout', packageName='com.microvirt.launcher', contentDesc='', checkable=false, checked=false, clickable=false, enabled=true, focusable=false, focuesd=false, scrollable=false, longClickable=false, password=false, selected=false, bound=[0, 0, 1024, 576]}"
					+ "LayoutNode{index=0, text='', className='android.view.View', packageName='com.microvirt.launcher', contentDesc='', checkable=false, checked=false, clickable=false, enabled=true, focusable=false, focuesd=false, scrollable=false, longClickable=false, password=false, selected=false, bound=[0, 0, 1024, 576]}"
					+ "LayoutNode{index=1, text='', className='android.widget.FrameLayout', packageName='com.microvirt.launcher', contentDesc='', checkable=false, checked=false, clickable=false, enabled=true, focusable=false, focuesd=false, scrollable=false, longClickable=false, password=false, selected=false, bound=[0, 0, 1024, 576]}"
					+ "LayoutNode{index=0, text='', className='android.view.View', packageName='com.microvirt.launcher', contentDesc='', checkable=false, checked=false, clickable=false, enabled=true, focusable=false, focuesd=false, scrollable=false, longClickable=false, password=false, selected=false, bound=[67, 59, 959, 423]}";
			
			News01 n=new News01(str);
			n.statWords();
			
			
		}
}
