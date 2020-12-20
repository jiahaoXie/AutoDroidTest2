package edu.nju.autodroid.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import edu.nju.autodroid.androidagent.AdbAgent;
import edu.nju.autodroid.androidagent.IAndroidAgent;
import edu.nju.autodroid.avdagent.AvdAgent;
import edu.nju.autodroid.avdagent.IAvdAgent;
import edu.nju.autodroid.strategy.DepthGroupWeightedStrategy;
import edu.nju.autodroid.strategy.IStrategy;
import edu.nju.autodroid.uiautomator.UiautomatorClient;
import edu.nju.autodroid.utils.AdbTool;
import edu.nju.autodroid.utils.CmdExecutor;
import edu.nju.autodroid.utils.Configuration;
import edu.nju.autodroid.utils.Logger;

/**
 * Created by ysht on 2016/3/7 0007.
 */
public class Main {
    private static Boolean isStartUiAutomator = false;
    private static HashMap<IDevice, Boolean> startedMap = new HashMap<IDevice, Boolean>();
    private static IDevice[] deviceArray;
    
    public static void main(String[] args) throws TimeoutException, AdbCommandRejectedException, 
    ShellCommandUnresponsiveException, IOException, InterruptedException {
        if(args.length != 2 ){
            System.out.println("Usage: java -jar AutoDroid.jar <Mode> <APK-Folder-path>");
            //命令行 输入的命令
            return;
        }
        int mode = Integer.parseInt(args[0]);
        if(mode == 1)
        {//模式1 必须有虚拟机运行
            Main_Single.main(args);
            return;	//模式1执行Main_Single,buzai不在执行后面的
        }
        //这是模式0
        Logger.initalize("log.txt");
        DdmPreferences.setTimeOut(10000);
        AdbTool.initializeBridge();

        List<String> apkFileList = getApkFileList(args[1]);
        //getApkFileList("E:\\APKs\\Wandoujia\\合集");
        
        Logger.logInfo("Total Apk counts：" + apkFileList.size());
        //生成设备
        int deviceCount = Configuration.getParallelCount();
        Logger.logInfo("Run in parallel count: " + deviceCount);
        int[] portArray = new int[deviceCount];
        for(int i=0; i<portArray.length; i++){
            portArray[i] = 22222+i;
        }

        deviceArray = new IDevice[deviceCount];
        final List<String> finishedList = getFinishedList("finishedList.txt");
        for(final String apkFilePath : apkFileList){

            if(finishedList.contains(apkFilePath))
                continue;

            int tempBeforeCount = AdbTool.getDevices().size();
            IAvdAgent avdAgent = AvdAgent.Get();
            avdAgent.startAvd("AutoDroidAvd");
            Logger.logInfo("Creating new AVD instance...");
            while(AdbTool.getDevices().size() == tempBeforeCount){
                Logger.logInfo("Waiting for AVD start...");
                Thread.sleep(1000);
            }
            List<IDevice> deviceList = AdbTool.getDevices();
            for(final IDevice d : deviceList){
                boolean containsDevice = false;
                for(int i=0; i<deviceArray.length; i++){
                    if(deviceArray[i] != null && deviceArray[i].equals(d)){
                        containsDevice = true;
                        break;
                    }
                }
                if(!containsDevice)
                {
                    final int[] port = new int[1];
                    port[0] = -1;
                    for(int i=0; i<deviceCount; i++){
                        if(deviceArray[i]== null){
                            port[0] = portArray[i];
                            deviceArray[i] = d;
                            startedMap.put(d, false);
                            break;
                        }
                    }
                    if(port[0] < 0){
                        Logger.logError("Can't get available port！");
                    }
                    while(!d.isOnline()){
                        Logger.logInfo("Waiting for "+d.getName()+" online...");
                        Thread.sleep(1000);
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                runAutoDroid(d, apkFilePath, finishedList, port[0]);
                            } catch (TimeoutException e) {
                                e.printStackTrace();
                            } catch (AdbCommandRejectedException e) {
                                e.printStackTrace();
                            } catch (ShellCommandUnresponsiveException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;
                }
            }
            Thread.sleep(1000);
            while(true){
                boolean hasStopedDevice = false;
                for(int i=0; i<deviceArray.length; i++){
                    IDevice d = deviceArray[i];
                    if(d == null || (startedMap.get(d) && !d.isOnline())){
                        deviceArray[i] = null;
                        hasStopedDevice = true;
                        break;
                    }
                }
                if(hasStopedDevice)
                    break;
                else
                    Thread.sleep(1000);
            }
            Thread.sleep(1000);
        }

        AdbTool.terminateBridge();
        Logger.endLogging();
    }

    private static void runAutoDroid(final IDevice device, String apkFilePath, List<String> finishedList, final int port) 
    		throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

        while(isStartUiAutomator){
            try {
                Logger.logInfo(device.getName() +"Waiting for Uiautomator in other devices...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized(isStartUiAutomator){
            isStartUiAutomator = true;
        }
        //创建新线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                Logger.logInfo(device.getName() + " Starting UiAutomator...");
                //通过文件操作，将UiAutomatorClient的端口号
                try {
                    File f = new File("src/edu/nju/autodroid/uiautomator/UiautomatorClient.java");
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String content = "";
                    String line;
                    while((line=br.readLine()) != null){
                        if(line.contains("public static int PHONE_PORT ="))
                            content += "public static int PHONE_PORT = "+port+";//this will be changed by file io" + "\n";
                        else
                            content += line + "\n";
                    }
                    br.close();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                    bw.write(content);
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                UiautomatorClient.start(device.getSerialNumber(), port);
            }
        }).start();

        while(AdbTool.getTaskId(device, "uiautomator") < 0)//wait for uiautomator
        {
            try {
                Logger.logInfo(device.getName() + "Waiting for Uiautomator");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        synchronized(isStartUiAutomator){
            isStartUiAutomator = false;
        }
        Logger.logInfo(device.getName() + " UiAutomator start successfully！");

        File apkFile = new File(apkFilePath);

        IAndroidAgent agent = new AdbAgent(device, port,port);
        boolean result;
        result = agent.init();
        Logger.logInfo("Init agent："+result);
        String packageName = AdbTool.getPackageFromApk(apkFilePath);
        if(!AdbTool.hasInstalledPackage(agent.getDevice(), packageName))
        {
            result = AdbTool.installApk(agent.getDevice().getSerialNumber(), apkFilePath);
            Logger.logInfo("Inistalling apk："+result);
        }

        if(result){
            String laubchableActivity = AdbTool.getLaunchableAcvivity(apkFilePath);

            if(!laubchableActivity.endsWith("/")) {
                String apkName = apkFile.getName().substring(0, apkFile.getName().lastIndexOf('.'));
                IStrategy strategy = new DepthGroupWeightedStrategy(agent, Configuration.getMaxStep(), laubchableActivity, new Logger(apkName, "logger_output\\" + apkName + ".txt"));//"com.financial.calculator/.FinancialCalculators"
                Logger.logInfo("Start strategy：" + strategy.getStrategyName());
                Logger.logInfo("Strategy target：" + apkFilePath);
                startedMap.put(agent.getDevice(), true);
                try{
                    if (strategy.run()) {
                        Logger.logInfo("Strategy finished successfully！");
                    } else {
                        Logger.logInfo("Strategy finished with errors！");
                    }
                    strategy.writeToFile("strategy_output\\" + apkName);
                }
                catch (Exception e){
                    Logger.logException("Strategy can't finish！");
                    e.printStackTrace();
                }
            }else{
                Logger.logError("Can not get Launchable Activity");
            }

        }

        agent.terminate();
        finishedList.add(apkFilePath);
        setFinishedList("finishedList.txt", finishedList);
        Logger.logInfo("Stopping device...");
        if(device != null)
        {
            try {
                AdbTool.stopDevice(device);
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String windowTitle = device.getSerialNumber().substring(device.getSerialNumber().indexOf('-')+1)+":AutoDroidAvd";
            CmdExecutor.execCmd("taskkill /f /fi \"WINDOWTITLE eq " + windowTitle + "\"");
            for(int i=0; i<deviceArray.length; i++){
                if(deviceArray[i] != null && deviceArray[i].equals(device)){
                    deviceArray[i] = null;
                    break;
                }
            }
        }
    }
    
    /**
     * 利用ArrayList存储 指定文件夹下的apk文件名
     * @param directoryName
     * @return
     */
    public static List<String> getApkFileList(String directoryName){
        List<String> apkFileList = new ArrayList<String>();
        File directory = new File(directoryName);
        if(directory.exists()){
            for(File f : directory.listFiles()){
                if(f.isDirectory()){
                    apkFileList.addAll(getApkFileList(f.getAbsolutePath()));
                }
                else{
                    if(f.getName().endsWith(".apk")){
                        apkFileList.add(f.getAbsolutePath());
                    }
                }
            }
        }
        return  apkFileList;
    }
    
    /**
     * 解析文件路径 得到文件路径(先利用字符串数组存储每一行分隔的文件路径，然后逐个添加到list中)
     * @param directoryName 细筛输出文件路径
     * @return	用一个list存储该文件下所有apk的路径
     */
    public static List<String> getApkFileListNew(String inputfile){
    	List<String> list=new ArrayList<String>();
		
		try {
			String encoding = "utf-8";
			File file = new File(inputfile);
			String lineinfo = "";
			if (file.isFile() && file.exists()) {
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
				BufferedReader bufferReader = new BufferedReader(read);
				while ((lineinfo = bufferReader.readLine()) != null) {
					StringTokenizer stk = new StringTokenizer(lineinfo, ",  ");// 被读取的文件的字段以","分隔
					String[] strArrty = new String[stk.countTokens()];
					int i = 0;
					while (stk.hasMoreTokens()) {
						strArrty[i++] = stk.nextToken();
					}
					for(int j=0;j<strArrty.length;j++){
						list.add(strArrty[j]);	
					}
					
				}
				read.close();
			}
		} catch (Exception e) {
			System.out.println("读取文件内容出错");
			e.printStackTrace();
		}
		System.out.println("list大小："+list.size());
		for(int k=0;k<list.size();k++){
			System.out.println("第"+(k+1)+"个apk路径："+list.get(k));
		}
		return list;
    }
/*
 * getFinishedList()函数 得到运行后apk文件名（表示 该apk已经处理过）
 */
    public static  List<String> getFinishedList(String fileName){
        List<String> filePathList = new ArrayList<String>();
        if(!new File(fileName).exists()){
            return  filePathList;
        }
        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line=br.readLine()) != null){
                filePathList.add(line);
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePathList;
    }

    public static void setFinishedList(String fileNameToSave, List<String> filePathList){
        try{
            FileWriter fw = new FileWriter(fileNameToSave);
            BufferedWriter bw = new BufferedWriter(fw);
            
            for(String filePath : filePathList){
                bw.write(filePath);
                bw.newLine();
            }
            bw.close();
            fw.close();
        }catch (IOException e){

        }
    }
}
