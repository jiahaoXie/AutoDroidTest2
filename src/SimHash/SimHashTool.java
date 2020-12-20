package SimHash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 相似哈希算法工具类 
 * @author lyq
 * 
 */
public class SimHashTool {
	// 二进制哈希位数
	private int hashBitNum;
	// 相同位数最小阈值
	private double minSupportValue;
	//输出文件路径
	File outFile;	

	public SimHashTool(int hashBitNum, double minSupportValue) {
		this.hashBitNum = hashBitNum;
		this.minSupportValue = minSupportValue;		
	}

	/**
	 * 比较文章的相似度
	 * 
	 * @param news1  文章路径1
	 * @param news2   文章路径2
	 * @throws IOException 
	 */
	public void compareArticals(String newsPath1, String newsPath2) throws IOException {
		String content1;
		String content2;
		int sameNum;
		int[] hashArray1;
		int[] hashArray2;


		// 读取分词结果
		content1 = readDataFile(newsPath1);
		content2 = readDataFile(newsPath2);
		hashArray1 = calSimHashValue(content1);
		hashArray2 = calSimHashValue(content2);

		// 比较哈希位数相同个数
		sameNum = 0;
		for (int i = 0; i < hashBitNum; i++) {
			if (hashArray1[i] == hashArray2[i]) {
				sameNum++;
			}
		}
		
		// 与最小阈值进行比较
		if (sameNum > this.hashBitNum * this.minSupportValue) {
//			System.out.println(String.format("相似度为%s,超过阈值%s,所以新闻1与新闻2是相似的",
//					sameNum * 1.0 / hashBitNum, minSupportValue));
			System.out.println(String.format("相似度为%s,超过阈值%s,所以"+newsPath1+"与"+newsPath2+"相似",
					sameNum * 1.0 / hashBitNum, minSupportValue));
			System.out.println("写入文件");
			
//			writeToFile(outFile,newsPath1,newsPath2);
			
		} else {
			System.out.println(String.format("相似度为%s,没超过阈值%s,所以"+newsPath1+"与"+newsPath2+"不相似",
					sameNum * 1.0 / hashBitNum, minSupportValue));
		}
	}
	
	/**
	 * 根据compareArticals方法 做修改
	 * 这里的函数功能就是得到目标文件的哈希值
	 * @param newsPath1
	 */
	public int[] compareArticalNew(String content1){		
		int[] hashArray1;		
		// 读取分词结果		
		hashArray1 = calSimHashValue(content1);
		return hashArray1;		
	}	

	/**
	 * 计算文本的相似哈希值
	 * 
	 * @param content
	 *            新闻内容数据
	 * @return
	 */
	private int[] calSimHashValue(String content) {
		int index;
		long hashValue;
		double weight;
		int[] binaryArray;
		int[] resultValue;
		double[] hashArray;
		
		String w;
		String[] words;
		News news;

		news = new News(content);
		news.statWords();
		hashArray = new double[hashBitNum];
		resultValue = new int[hashBitNum];

		words = content.split("}");
//		for (String str : words) {
		for (int k = 0; k < words.length; k++) {
			//过滤掉相邻相同的节点 
			if (k != words.length - 1 && words[k].equals(words[k + 1])) {
				continue;
			} else {
			w=words[k].replaceAll("text='*',|contentDesc='*',","");
//			System.out.println("节点有效信息："+w);
			
			// 获取权重值，根据词频所得
			weight = news.getWordFrequentValue(w);
			if(weight == -1){
				continue;
			}
			// 进行哈希值的计算
			hashValue = BKDRHash(w);
			// 取余把位数变为n位
			hashValue %= Math.pow(2, hashBitNum);

			// 转为二进制的形式
			binaryArray = new int[hashBitNum];
			numToBinaryArray(binaryArray, (int) hashValue);

			for (int i = 0; i < binaryArray.length; i++) {
				// 如果此位置上为1，加权重
				if (binaryArray[i] == 1) {
					hashArray[i] += weight;
				} else {
					// 为0则减权重操作
					hashArray[i] -= weight;
				}
			}
		}
	}

		// 进行数组收缩操作，根据值的正负号，重新改为二进制数据形式
		for (int i = 0; i < hashArray.length; i++) {
			if (hashArray[i] > 0) {
				resultValue[i] = 1;
			} else {
				resultValue[i] = 0;
			}
		}

		return resultValue;
	}

	/**
	 * 数字转为二进制形式
	 * 
	 * @param binaryArray
	 *            转化后的二进制数组形式
	 * @param num
	 *            待转化数字
	 */
	private void numToBinaryArray(int[] binaryArray, int num) {
		int index = 0;
		int temp = 0;
		while (num != 0) {
			binaryArray[index] = num % 2;
			index++;
			num /= 2;
		}

		// 进行数组前和尾部的调换
		for (int i = 0; i < binaryArray.length / 2; i++) {
			temp = binaryArray[i];
			binaryArray[i] = binaryArray[binaryArray.length - 1 - i];
			binaryArray[binaryArray.length - 1 - i] = temp;
		}
	}

	/**
	 * BKDR字符哈希算法
	 * 
	 * @param str
	 * @return
	 */
	public static long BKDRHash(String str) {
		int seed = 31; /* 31 131 1313 13131 131313 etc.. */
		long hash = 0;
		int i = 0;

		for (i = 0; i < str.length(); i++) {
			hash = (hash * seed) + (str.charAt(i));
		}

		hash = Math.abs(hash);
		return hash;
	}

	/**
	 * 从文件中读取数据
	 */
	private String readDataFile(String filePath) {
		File file = new File(filePath);
		StringBuilder strBuilder = null;

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String str;
			strBuilder = new StringBuilder();
			while ((str = in.readLine()) != null) {
				strBuilder.append(str);
			}
			in.close();
		} catch (IOException e) {
			e.getStackTrace();
		}

		return strBuilder.toString();
	}

	/**
	 * 利用分词系统进行新闻内容的分词
	 * 
	 * @param srcPath
	 *            新闻文件路径
	 */
	private void parseNewsContent(String srcPath) {
		// TODO Auto-generated method stub
		int index;
		String dirApi;
		String desPath;

		dirApi = System.getProperty("user.dir") + "\\lib";
		// 组装输出路径值
		index = srcPath.indexOf('.');
		desPath = srcPath.substring(0, index) + "-split.txt";

		try {
			ICTCLAS50 testICTCLAS50 = new ICTCLAS50();
			// 分词所需库的路径、初始化
			if (testICTCLAS50.ICTCLAS_Init(dirApi.getBytes("utf-8")) == false) {
				System.out.println("Init Fail!");
				return;
			}
			// 将文件名string类型转为byte类型
			byte[] Inputfilenameb = srcPath.getBytes();

			// 分词处理后输出文件名、将文件名string类型转为byte类型
			byte[] Outputfilenameb = desPath.getBytes();

			// 文件分词(第一个参数为输入文件的名,第二个参数为文件编码类型,第三个参数为是否标记词性集1 yes,0
			// no,第四个参数为输出文件名)
			testICTCLAS50.ICTCLAS_FileProcess(Inputfilenameb, 0, 1,
					Outputfilenameb);
			// 退出分词器
			testICTCLAS50.ICTCLAS_Exit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
