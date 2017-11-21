package com.zhangyue.campare;

import com.zhangyue.campare.model.ClassInfo;
import com.zhangyue.campare.model.ClassModel;
import com.zhangyue.campare.model.MethodModel;
import com.zhangyue.campare.tools.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by zy1 on 16/11/2017.
 */
public class ComPareJar {
    private String first_jar_path = "";     //第一个jar的路径
    private String second_jar_path = "";    //第二个jar的路径
    public static final String unZipFileDir=Utils.getCurPath()+"\\temp"; //解压的缓存目录
    private  boolean mIgnoreFile = false;   //是否忽略字段的变化
    private  boolean mIgnoreMethod = false; //是否忽略方法的变化

    private int mAddFieldCount =  0;
    private int mRemoveFieldCount =  0;
    private int mAddMethodCount = 0;
    private int mRemoveMethodCount = 0;
    private StringBuffer sbResult;
    public ComPareJar(){

    }

    public void compare(){
        Map<String, ClassInfo> map1 = readJarFileMd5(first_jar_path,unZipFileDir+"/1");

        Map<String, ClassInfo> map2 = readJarFileMd5(second_jar_path,unZipFileDir+"/2");
        List<String> mDiffFileKey = new ArrayList<>();
        sbResult = new StringBuffer();
        Iterator<Map.Entry<String, ClassInfo>> iterator = map2.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, ClassInfo> next = iterator.next();
            String key = next.getKey();
            if (map1.containsKey(key)){
                ClassInfo classInfo1 = map1.get(key);
                ClassInfo classInfo2 = map2.get(key);
                if (!classInfo1.equals(classInfo2) ){
                    mDiffFileKey.add(key);
                }
            }else {
                // TODO: 21/11/2017  增加了类 需要后续去处理
                //新增了类  
//                Log.d("增加了类："+map2.get(key).getmClassName());
            }
        }

        for (String key : mDiffFileKey) {
            //从不同的mDiffFileKey 取出两个class
            //1
            ClassInfo classInfo1 = map1.get(key);
            ClassModel classMod1 = readClassModelFromFile(classInfo1.getmClassPath());
            //2
            ClassInfo classInfo2 = map2.get(key);
            ClassModel classMod2 = readClassModelFromFile(classInfo2.getmClassPath());

            compareModel(classMod1,classMod2);
        }
        
        Log.d("总结：");
        Log.d("增加方法总数："+mAddMethodCount);
        Log.d("删除方法总数："+mRemoveMethodCount);
        Log.d("增加字段总数："+mAddFieldCount);
        Log.d("删除字段总数："+mRemoveFieldCount);

        sbResult.append("总结：");
        sbResult.append("\r\n");
        sbResult.append("增加方法总数："+mAddMethodCount);
        sbResult.append("\r\n");
        sbResult.append("删除方法总数："+mRemoveMethodCount);
        sbResult.append("\r\n");
        sbResult.append("增加字段总数："+mAddFieldCount);
        sbResult.append("\r\n");
        sbResult.append("删除字段总数："+mRemoveFieldCount);
        sbResult.append("\r\n");

        try {
            String resultFilePath = unZipFileDir + "/result.txt";
            Path path = Paths.get(resultFilePath);
            Files.deleteIfExists(path);
            Files.createFile(path);
            Utils.write2File(resultFilePath,sbResult.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        clearTemp();
    }

    private void compareModel(ClassModel mod1,ClassModel mod2) {
        if (mod1 ==null || mod2 ==null) {
            return;
        }

        if (mod1.equals(mod2)) {
            return;
        }

        Log.d(mod1.getmClassName()+"的差异");
        sbResult.append(mod1.getmClassName()+"的差异");
        sbResult.append("\r\n");

        //对比字段
        List<String> filed1 = mod1.getmFiled();
        List<String> filed2 = mod2.getmFiled();
        if (!filed1.equals(filed2)  && !mIgnoreFile) {
            for (String field : filed1) {
                if (filed2.contains(field)) {
                    filed2.remove(field);
                    continue;
                }
                Log.d("filed2 删除了字段"+field);
                sbResult.append("filed2 删除了字段"+field);
                sbResult.append("\r\n");
                mRemoveFieldCount++;

            }

            for (String field : filed2) {
                if (filed1.contains(field)){
                    continue;
                }
                Log.d("file 2 增加了字段"+field);
                sbResult.append("file 2 增加了字段"+field);
                sbResult.append("\r\n");
                mAddFieldCount++;
            }
        }

        //对比方法

        List<MethodModel> methodMod1 = mod1.getmMethod();
        List<MethodModel> methodMod2 = mod2.getmMethod();
        if (!methodMod1.equals(methodMod2) && !mIgnoreMethod) {
            for (MethodModel method : methodMod1) {
                if (methodMod2.contains(method)) {
                    methodMod2.remove(method);
                    continue;
                }
                Log.d("filed2 删除了方法 "+method.getmName());
                sbResult.append("filed2 删除了方法 "+method.getmName());
                sbResult.append("\r\n");
                mRemoveMethodCount++;
            }

            for (MethodModel method : methodMod2) {
                if (methodMod1.contains(method)){
                    continue;
                }
                Log.d("file 2 增加了方法 "+method.getmName());
                sbResult.append("file 2 增加了方法 "+method.getmName());
                sbResult.append("\r\n");
                mAddMethodCount++;
            }
        }
        Log.d("-------------------------------------------");
        sbResult.append("-------------------------------------------");
        sbResult.append("\r\n");
    }

    private Map<String,ClassModel> readJarFile(String jarFilePath) {
        Zip zip = new Zip();
        zip.unzip(jarFilePath,unZipFileDir,true);
        Map<String,ClassModel> classModelMap= new HashMap<>();
        for (String mZipFile : zip.mZipFiles) {
            //内部类先不管
            if (mZipFile.contains("$")) {
                continue;
            }

        }
        return classModelMap;
    }

    /**
     * 从class文件中读取ClassModel
     * @param classFilePath
     * @return
     */
    private ClassModel readClassModelFromFile(String classFilePath){
        String string = JavaCommand.javapClass(classFilePath);
        if (Utils.isEmpty(string)) {
            return null;
        }

        ClassModel classModel = analyzeClassString(string);
        return classModel;
    }

    /**
     * 读取jar里面的所有的文件  以及md5
     * @param jarFilePath
     * @param unZipPath
     * @return
     */
    public Map<String,ClassInfo> readJarFileMd5(String jarFilePath, String unZipPath){
        Map<String,ClassInfo> jarMap = new HashMap<>();
        Zip zip = new Zip();
        zip.unzip(jarFilePath,unZipPath,true);
        for (String mZipFile : zip.mZipFiles) {
            //如果是内部类 包含$ 则不管
            if (mZipFile.contains("$")) {
                continue;
            }
            String key = mZipFile.substring(mZipFile.indexOf(unZipPath) + unZipPath.length() + 1);
            jarMap.put(key, new ClassInfo(mZipFile,MD5.getMd5ByFile(new File(mZipFile))));
        }
        return jarMap;
    }

    /**
     * 根据字符数组解析ClassModel
     * @param classStr
     * @return
     */
    private ClassModel analyzeClassString(String classStr) {
        //去掉  Compiled from "CONSTANT.java 这一行
        String[] oldStrArr = classStr.split("\n");
        String[] newStrArr = new String[oldStrArr.length-1];
        System.arraycopy(oldStrArr,1,newStrArr,0,oldStrArr.length-1);
        //解析classmodel
        ClassModel classModel = new ClassModel();
        List<MethodModel> methodList = new ArrayList<>();
        List<String> filedList = new ArrayList<>();
        for (int i = 0; i < newStrArr.length; i++) {
            String temp = newStrArr[i].replaceAll(";","");
            temp = temp.replaceAll("}","");
            //第一行是类名
            if (i==0) {
                classModel.setmClassName(temp.replace("{",""));
                continue;
            }

            //方法
            if (temp.contains("(") && temp.contains(")") && !mIgnoreMethod) {
                MethodModel methodModel = anaylzeMethodStringV2(temp);
                if (methodModel != null) {
                    methodList.add(methodModel);
                }
                continue;
            }
            //字段
            if (!Utils.isEmpty(temp) && !mIgnoreFile) {
                filedList.add(temp);
            }
        }
        classModel.setmFiled(filedList);
        classModel.setmMethod(methodList);
        return classModel;
    }

    /**
     * 解析方法
     * @param methodString
     * @return
     */
    private MethodModel anaylzeMethodString(String methodString) {
//        private void initDialogProgress();
        MethodModel methodModel = new MethodModel();
        //得到参数
        String argString = methodString.substring(methodString.indexOf("(")+1, methodString.indexOf(")"));
        if (!Utils.isEmpty(argString)) {
            List<String> args =Arrays.asList(argString.split(","));
            methodModel.setmArgs(args);
        }

        //得到参数之后  去掉括号内的参数信息
        String temp = methodString.substring(0, methodString.indexOf("("));
        //得到方法名
        //public void onActivityResult(int, int, android.content.Intent)
        String[] split = temp.split(" ");
        // 减一是因为第一个是方法名
        String methodName=split[split.length-1];
        //此时，methodName是方法名，不包含后面的括号和参数
        if (methodName.contains("$")) {
            return null;
        }
        methodName = methodString.substring(methodString.indexOf(methodName));
        methodModel.setmName(methodName);

        //得到返回类型
        //如果方法名中包含. 那么就是构造方法 返回值就给方法名即可
        if (methodName.contains(".")) {
            methodModel.setmReturnType(methodName);
        }else{
            // -2 是因为倒数第二个是返回类型
            methodModel.setmReturnType(split[split.length-2]);
        }
        return methodModel;
    }


    /**
     * 解析方法
     * 这个版本的解析方法很粗暴， 直接设置一个名字 不去解析返回类型，参数
     * @param methodString
     * @return
     */
    private MethodModel anaylzeMethodStringV2(String methodString) {
        MethodModel methodModel = new MethodModel();
        methodModel.setmName(methodString);
        return methodModel;
    }
    public void setmIgnoreFile(boolean mIgnoreFile) {
        this.mIgnoreFile = mIgnoreFile;
    }

    public void setmIgnoreMethod(boolean mIgnoreMethod) {
        this.mIgnoreMethod = mIgnoreMethod;
    }

    public void setFirst_jar_path(String first_jar_path) {
        this.first_jar_path = first_jar_path;
    }

    public void setSecond_jar_path(String second_jar_path) {
        this.second_jar_path = second_jar_path;
    }


    /**
     * 删除缓存文件   后续处理
     */
    private void clearTemp() {
//        try {
//            Utils.deleteIfExists(Paths.get(unZipFileDir));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        Command.exeCmd("rm -rf "+unZipFileDir);
    }

    private static class FindJavaVisitor extends SimpleFileVisitor<Path>{
        private List result;
        public FindJavaVisitor(List result){
            this.result = result;
        }
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs){
//            if(file.toString().endsWith(".java")){
//                result.add(file.getFileName());
//            }
            return FileVisitResult.CONTINUE;
        }
    }

}
