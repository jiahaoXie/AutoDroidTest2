package SimHash;

import java.io.IOException;

/**
 * 这才应该是Simhash的算法过程（有分词 hash 加权 合并 降维几大过程）
 * 相似哈希算法
 * @author lyq
 *
 */
public class Client {
	public static void main(String[] args) throws IOException{
		//二进制哈希编码位数
		int hashBitNum;
		
		//相同位置占比最小阈值
		double minRate;
		String newsPath1;
		String newsPath2;
		String newsPath3;
		SimHashTool tool;
		
		hashBitNum = 64;
		//至少有一半的位置值相同
//		minRate = 0.5;
		minRate=0.75;
		
		newsPath1 = "E:\\javaCode\\AutoDroidTest\\birthMark_output\\anzhiMusic\\0a1db225609791db26e7064892f903b6_27924300.txt";
		newsPath2 = "E:\\javaCode\\AutoDroidTest\\birthMark_output\\anzhiMusic\\1dbe9ab88b963159075274da0fb6231b_96151800.txt";
		newsPath3 = "E:\\javaCode\\AutoDroidTest\\birthMark_output\\anzhiMusic\\1e636c02dc85594c7b776fe6d7cef0a4_88613900.txt";
		
		tool = new SimHashTool(hashBitNum, minRate);
		tool.compareArticals(newsPath1, newsPath2);
		tool.compareArticals(newsPath2, newsPath3);
	}
}
