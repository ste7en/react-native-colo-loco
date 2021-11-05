// TL;DR: This file is included and executed from `./android/settings.gradle`
// like this:
//
// apply from: '../node_modules/react-native-colocate-native/scripts/android.groovy'
// setupReactNativeColocateNative([ appName: rootProject.name, appPath: "../app", appPackageName: "com.myapp", androidPath: "./android/app/src/main/java/com/myapp" ])
ext.setupReactNativeColocateNative = { Map customOptions = [:] ->
  // strip "./android/" from the androidPath
  def androidPath = customOptions.androidPath.replace("android/", "")

  def colocatedJavaFiles = new FileNameFinder().getFileNames(customOptions.appPath, '**/*.java', '')

  // make an array to hold the files to be colocated
  def filesToColocate = new ArrayList<File>()

  def moduleInstantiationString = ""

  // loop through colocatedJavaFiles and check if the class name matches the file name
  for (filepath in colocatedJavaFiles) {
    // read file from filepath
    def file = new File(filepath)
    
    def classString = "class "

    // find classString in file contents
    String fileText = file.text
    def classIndex = fileText.indexOf(classString)

    def restOfClass = fileText.substring(classIndex + classString.length(), fileText.length())

    // get the first word from the restOfClass as className
    def className = restOfClass.substring(0, restOfClass.indexOf(" "))

    // get the file name from the file path
    def fileName = file.getName()

    // get the filename without the extension
    def fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."))

    // verify that the fileNameWithoutExtension matches the className
    if (fileNameWithoutExtension == className) {
      // add the file to the array
      filesToColocate.add(file)
      moduleInstantiationString += "    modules.add(new ${className}(reactContext));\n"
    } else {
      println "WARNING: file was not added because the class name ${className} didn't match the filename ${fileName}"
    }

  }

  // make sure there is a `colocated` folder in the androidPath
  def mkdirCommand = "mkdir -p ${androidPath}/colocated"
  mkdirCommand.execute()
  
  // loop through filesToColocate and colocate the files
  for (fileToColocate in filesToColocate) {
    // shell out to ln to make a hardlink to the file in the android path
    def lnCommand = "ln ${fileToColocate.absolutePath} ${androidPath}/colocated/${fileToColocate.name}"
    // run the lnCommand
    lnCommand.execute()
  }

    // create the manifest file
  def manifestFile = new File(androidPath + "/colocated/", "RNColocate.java")

  def manifestText = """
// This file is autogenerated from a react-native-colocate-native script.
// Please do not edit it directly.
package ${customOptions.appPackageName};

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

class RNColocate {
  public static List<NativeModule> colocatedModules(ReactApplicationContext reactContext) {
    List<NativeModule> modules = new ArrayList<>();
    // This list is auto-generated. Do not edit manually.

${moduleInstantiationString}

    return modules;
  }
}
  """

  // write the manifestText to the manifestFile
  manifestFile.write(manifestText)
}
