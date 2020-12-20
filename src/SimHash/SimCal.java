package SimHash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 新建一个主体类
 * 
 * @author xjh 2018.05.23
 */
public class SimCal {
	public static void main(String[] args) throws IOException {
		// 1.控制台输入目标文件夹与输出文件夹 （导入目标文件夹下的胎记信息）
		if (args.length != 2) {
			System.out.println("please correct input: <inputFilePath> <outputFilePath>");
			return;
		}

		String path = args[0]; // 读取文件路径
		String outputPath = args[1];
		File outFile = new File(outputPath);
		if (outFile.exists()) { // 如果文件存在则删除
			// 创建文件，根据给定的路径创建
			outFile.delete();
			System.out.println("删除成功~");
			outFile.createNewFile(); // 创建文件，根据给定的路径创建
			System.out.println("创建文件成功~");
		} else {
			try {
				outFile.createNewFile(); // 创建文件，根据给定的路径创建
				System.out.println("创建文件成功~");
			} catch (IOException e) {
				e.printStackTrace(); // 输出异常信息
			}
		}

		// 二进制哈希编码位数
		int hashBitNum = 64;

		// 相同位置占比最小阈值
		double minRate = 0.997; // 64位海明距离在3以内
		SimHashTool tool= new SimHashTool(hashBitNum, minRate);

		List<String> str = new ArrayList<String>(); // 用一个List存放目标文件的中间文件名
		File file = new File(path);
		File[] files = file.listFiles();// 获取目录下的所有文件或文件夹
		List<int[]> list = new ArrayList<int[]>(); // 用一个list存储目标文件对应的哈希值
		System.out.println("该目录下对象个数：" + files.length);
		// 遍历，目录下的所有文件
		for (int i = 0; i < files.length; i++) {
			str.add(files[i].getName());

			list.add(tool.compareArticalNew(files[i].getAbsolutePath()));
		}

		// 接下来对list中的哈希值进行比较
		for (int j = 0; j < list.size(); j++) {
			for (int k = j + 1; k < list.size(); k++) {
				// 这里为了避免错误 对比较次树进行过滤 如果两个文件之间大小相差大于1.5倍 则跳过比较
				long r1 = files[j].length();	//得到该目标文件的字节大小
				long r2 = files[k].length();
				if (r1==0||r2 == 0) {
					continue;
				} else {
					double s = (double) r1 / r2;
					if (s > 1.5 || s < 0.68) {
						continue;
					}
					// 比较哈希位数相同个数
					int sameNum = 0;
					for (int i = 0; i < hashBitNum; i++) {
						if (list.get(j)[i] == list.get(k)[i]) {
							sameNum++;
						}
					}

					// 与最小阈值进行比较
					if (sameNum > hashBitNum * minRate) {
						System.out.println(String.format("相似度为%s,超过阈值%s,所以" + str.get(j) + "与" + str.get(k) + "相似",
								sameNum * 1.0 / hashBitNum, minRate));
						System.out.println("写入文件");

						writeToFile(outFile, str.get(j), str.get(k));

					} else {
						System.out.println(String.format("相似度为%s,没超过阈值%s,所以" + str.get(j) + "与" + str.get(k) + "不相似",
								sameNum * 1.0 / hashBitNum, minRate));
					}
				}
			}
		}

	}

	// 将目标文件名 写入输出文件中
	public static void writeToFile(File f, String s1, String s2) throws IOException {

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f, true);

			OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
			// 构建OutputStreamWriter对象,参数可以指定编码,默认为操作系统默认编码,windows上是gbk
			writer.write(s1);
			writer.append("   ");
			// 写入到缓冲区
			writer.write(s2);

			writer.append("\r\n");
			// 换行

			writer.close();
			// 关闭写入流,同时会把缓冲区内容写入文件,所以上面的注释掉

			fos.close();
			// 关闭输出流,释放系统资源

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
