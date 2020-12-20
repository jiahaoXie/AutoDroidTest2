Environment Preparation
1、Android SDK, Android virtual machine, Ant are REQUIRED.
2、If you use AVD as the Android virtual machine, it's STRONGLY recommended that HAXM is supported, installed, and enabled.
3、It's STRONGLY recommended that the Android system is Android 4.2.2 (API17).
4、File "autodroid.properties" should be configured correctly.
5、It's STRONGLY  recommended that the versions of each software or tool in "autodroid.properties" are the same of the default ones.
6、Copy the "avd" folder to the ".android" folder.

Configurations
version: no use
android_jar_version: no use
uiautomator_version: no use
android_sdk_path: The root path of Android SDK
ant_root_path: The root path of Ant
aapt_path: The file path of Aapt. It should be %android_sdk_path%\\build-tools\\<version>\\aapt.exe
dot_android_path: The path of hidden folder ".android". In Windows, it should be <system_disk>\\Users\\<username>\\.android
parallel_count: The parallel thread count shoulde be used. It should be set properly according to the performance of your computer.
max_step: The maximum step that AutoDroid can be run. It should be far larger than "delta_c". 99999 is enough.
delta_c: The maximum step that LGG has no changes
delta_l: The similarity threshold for compare two layout group.

Run AutoDroid
java -jar AutoDroid.jar <Mode> <APK-Collection-Folder>
Mode: 	0- Parallel Mode. ONLY support AVD as the virtual machine. In this mode, AutoDroid will create avd automatically.
		1- Single Thread Mode. This mode supports any Android virtual machine. In this mode, AutoDroid will run on the default online Android virtual machine.
APK-Collection-Folder: The folder path of the apk collection.
NOTE: 	If in Mode 0, NO virtual machine can be executed before AutoDroid runs.
		If in Mode 1, there MUST be a virtual machine online.
		
Result
strategy_output folder contains the LGGs of each corresponding APK.