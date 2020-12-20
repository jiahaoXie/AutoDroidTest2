package edu.nju.autodroid.main;

import SimHash.SimHashTool;
import com.android.ddmlib.*;
import edu.nju.autodroid.androidagent.AdbAgent;
import edu.nju.autodroid.androidagent.IAndroidAgent;
import edu.nju.autodroid.hierarchyHelper.LayoutTree;
import edu.nju.autodroid.strategy.IStrategy;
import edu.nju.autodroid.strategy.birthMark;
import edu.nju.autodroid.uiautomator.UiautomatorClient;
import edu.nju.autodroid.utils.AdbTool;
import edu.nju.autodroid.utils.Configuration;
import edu.nju.autodroid.utils.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author xjh 2018.08
 */
public class Main_Single {
	public static File outFile;
	// 二进制哈希编码位数
	public static int hashBitNum = 64;

	// 相同位置占比最小阈值
	public static double minRate = 0.99999999999; // 64位海明距离在3以内

	public static void main(String[] args) throws TimeoutException, AdbCommandRejectedException,
			ShellCommandUnresponsiveException, IOException, InterruptedException {
		/**
		 * 1 选择运行模式 1，输入目标apk数据集文件夹 输出结果到指定的txt 初始化adb 获得apk数据集
		 */
		if (args.length != 3) {
			System.out.println("Usage: java -jar AutoDroid.jar <Mode> <APK-Folder-path><APK-outputPath>");
			return;
		}
		int mode = Integer.parseInt(args[0]);

		if (mode == 0) {
			Main.main(args);
			return;
		}

		outFile = new File(args[2]);
		if(outFile.exists()){
			System.out.println("文件已存在");
		}else{
			// 创建文件，根据给定的路径创建
			outFile.createNewFile(); // 创建文件，根据给定的路径创建
			System.out.println("创建文件成功~");
		}
		
		SimHashTool tool = new SimHashTool(hashBitNum, minRate);

		DdmPreferences.setTimeOut(10000);
		AdbTool.initializeBridge();

		Map<String, LayoutTree> map = new HashMap<String, LayoutTree>();
		// 定义 一个map用来存贮apk的路径和胎记信息

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(args[1])), "UTF-8"));
		String lineTxt = null;
		while ((lineTxt = br.readLine()) != null) { // 逐行读入文本中的数据
			List<String> finishedList = Main.getFinishedList("finishedList.txt");
			if (finishedList.contains(lineTxt)) // 在这里finishedList.txt一次存放的是可疑应用对的路径
				continue;

			List<LayoutTree> LTree = new ArrayList<LayoutTree>(); // 定义一个布局树数组用来存放临时的两个布局树胎记
//			LayoutTree lTree1=null;
//			LayoutTree lTree2=null;
			String[] names = lineTxt.split(",  ");
			Logger.logInfo("Total Apk counts：" + names.length); // 一次应该是两个apk
			IDevice device = AdbTool.getDefaultDevice();// 使用默认的device
			
			for (int i = 0; i <names.length; i++) {
				LayoutTree lTree=null;
				if (map.keySet().contains(names[i])) { // 如果已有的map中含有当前apk的胎记信息
													   // 则直接从map中找
					lTree=map.get(names[i]); 		// 得到临时布局树胎记1
				} else { 	// 否则需要 进行动态遍历生成动态胎记
					File apkFile = new File(names[i]);
					IAndroidAgent agent = new AdbAgent(device, UiautomatorClient.PHONE_PORT,
							UiautomatorClient.PHONE_PORT);
					boolean result;
					result = agent.init();
					Logger.logInfo("Init agent：" + result);

					/**
					 * 2 利用Adb从apk中得到包名getPackageFromApk()
					 * ；利用Adb在模拟器上安装apkinstallApk()
					 * 安装成功，利用Adb得到apk的具体活动getLaunchableAcvivity（）
					 */
					String packageName = AdbTool.getPackageFromApk(names[i]);
					// if(!AdbTool.hasInstalledPackage(agent.getDevice(),
					// packageName))
					{
						result = AdbTool.installApk(agent.getDevice().getSerialNumber(), names[i]);
						Logger.logInfo("Install apk：" + result);
					}

					if (result) {
						// 初始化adb成功 得到相应apk路径下的文件包名
						String laubchableActivity = AdbTool.getLaunchableAcvivity(names[i]);

						/**
						 * 3 进行apk动态解析（包括动态布局提取） 重点在run()函数
						 */
						// 解析apk 得到相应的应用胎记LGGs
						if (!laubchableActivity.endsWith("/")) {
							String apkName = apkFile.getName().substring(0, apkFile.getName().lastIndexOf('.'));
							IStrategy strategy = new birthMark(agent, Configuration.getMaxStep(), laubchableActivity,
									new Logger(apkName, "logger_output\\" + apkName + ".txt"));
							// logger_output文件夹 下是apk遍历的日志文件
							Logger.logInfo("Start Strategy：" + strategy.getStrategyName());
							Logger.logInfo("Strategy target：" + names[i]);
							try {
								// run（）函数 是生成胎记最重要的部分
								if (strategy.run()) {
									Logger.logInfo("Strategy finished successfully！");
								} else {
									Logger.logInfo("Strategy finished with errors！");
								}

							} catch (Exception e) {
								Logger.logException("Strategy can't finish！");
								e.printStackTrace();
							}
							// run()函数成功之后 得到 apk的胎记信息 将其apk的路径和胎记信息存入
							map.put(names[i], strategy.mergeAllGroupTrees()); //这里调用的是birthmark中的mergeAllGroupTrees()方法
							lTree=map.get(names[i]); // 得到临时布局树胎记1							
						} else {
							Logger.logInfo("Can not get Launchable Activity");
						}  
						// 动态布局提取后 卸载安装的apk(但是我感觉代码里没有实现这部分功能)
						AdbTool.unInstallApk(agent.getDevice().getSerialNumber(), packageName);
					}

					agent.terminate();

					Thread.sleep(2000);
				}
				LTree.add(lTree);
//				if(i==0){
//					lTree1=lTree;
//				} else{
//					lTree2=lTree;
//				} 
			} // for循环结束 得到 当前两个apk的布局树胎记 胎记信息暂时存放在lTree数组中

			finishedList.add(lineTxt); // 存放的也是克隆应用对的路径
			Main.setFinishedList("finishedList.txt", finishedList);
			System.out.println("LTree Length:" + LTree.size());
			// 接下来进行两个胎记信息的相似度比较 基于布局树的比较方法存在报错 这里 还是采用SimHash的方法
			if (LTree.size()!= 0) {
				List<int[]> list = new ArrayList<int[]>(); // 用一个list存储目标文件对应的哈希值
				System.out.println("LTree1:"+LTree.get(0).toString());
				System.out.println("LTree2:"+LTree.get(1).toString());
				list.add(tool.compareArticalNew(LTree.get(0).toString()));
				list.add(tool.compareArticalNew(LTree.get(1).toString()));

				// 接下来对list中的哈希值进行比较
				// 比较哈希位数相同个数
				int sameNum = 0;
				for (int i = 0; i < hashBitNum; i++) {
					if (list.get(0)[i] == list.get(1)[i]) {
						sameNum++;
					}
				}

				String s0 = getKey(map, LTree.get(0));
				String s1 = getKey(map, LTree.get(1));

				// 与最小阈值进行比较
				if (sameNum >=hashBitNum * minRate) {
					System.out.println(String.format("相似度为%s,超过阈值%s,所以" + s0 + "与" + s1 + "相似",
							sameNum * 1.0 / hashBitNum, minRate));
					System.out.println("写入文件");
					writeToFile(outFile, s0, s1);

				} else {
					System.out.println(String.format("相似度为%s,没超过阈值%s,所以" + s0 + "与" + s1 + "不相似",
							sameNum * 1.0 / hashBitNum, minRate));
				}

			}

		} // while循环结束
		br.close();
		AdbTool.terminateBridge();
		Logger.endLogging();
	}

	// 根据value值获取到对应的一个key值
	public static String getKey(Map<String, LayoutTree> map, LayoutTree value) {
		String key = null;
		// Map,HashMap并没有实现Iteratable接口.不能用于增强for循环.
		for (String getKey : map.keySet()) {
			if (map.get(getKey).equals(value)) {
				key = getKey;
			}
		}
		return key;
		// 这个key肯定是最后一个满足该条件的key.
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
