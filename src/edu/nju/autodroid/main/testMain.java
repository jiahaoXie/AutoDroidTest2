package edu.nju.autodroid.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import edu.nju.autodroid.androidagent.AdbAgent;
import edu.nju.autodroid.androidagent.IAndroidAgent;
import edu.nju.autodroid.hierarchyHelper.LayoutTree;
import edu.nju.autodroid.uiautomator.UiautomatorClient;
import edu.nju.autodroid.utils.AdbTool;
import edu.nju.autodroid.utils.Logger;

/**
 * Created by ysht on 2016/4/19 0019.
 */
public class testMain {
    public static void main(String[] args) throws TimeoutException, AdbCommandRejectedException, 
    	ShellCommandUnresponsiveException, IOException, InterruptedException {

        DdmPreferences.setTimeOut(10000);
        AdbTool.initializeBridge();

        final IDevice device = AdbTool.getDefaultDevice();//使用默认的device

            int uiautomatorTaskId = AdbTool.getTaskId(device, "uiautomator");
            if(uiautomatorTaskId > 0)
                AdbTool.killTask(device, uiautomatorTaskId);
            Thread.sleep(2000);
            if(AdbTool.getTaskId(device, "uiautomator") < 0) {//如果uiautomator没有启动
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        UiautomatorClient.start(device.getSerialNumber(), UiautomatorClient.PHONE_PORT);
                    }
                }).start();
            }
            while (AdbTool.getTaskId(device, "uiautomator") < 0){//等待uiautomator
                Logger.logInfo("Waiting for Uiautomator...");
                Thread.sleep(1000);
            }
            Logger.logInfo("UiAutomator start successfully!");


            IAndroidAgent agent = new AdbAgent(device, UiautomatorClient.PHONE_PORT, UiautomatorClient.PHONE_PORT);
            boolean result;
            result = agent.init();
            Logger.logInfo("Init agent："+result);

            if(result){
                int count = 5000;
                BufferedWriter bw = new BufferedWriter(new FileWriter("testLayout.txt"));
                while(count-- > 0){
                    String layout = agent.getLayout();
                    System.out.println(layout.length());
                    bw.write(new LayoutTree(layout).getTreeSize() + "\t" + layout);
                    bw.newLine();
                }
                bw.close();
            }

            agent.terminate();
            uiautomatorTaskId = AdbTool.getTaskId(device, "uiautomator");
            if(uiautomatorTaskId > 0)
                AdbTool.killTask(device, uiautomatorTaskId);
            Thread.sleep(2000);


        AdbTool.terminateBridge();
        Logger.endLogging();

    }
}
