package org.etd.framework.business.controller;

import java.io.File;

public class Test {

    public static void main(String[] args) {

        File file=new File("D:\\workspace\\apache-maven-3.8.5\\repository");  //  C:\Users\WX\Desktop\test是仓库所在文件夹
        delete(file);
    }
    public static void delete(File f)
    {

        //数组指向文件夹中的文件和文件夹
        File[] fi=f.listFiles();
        //遍历文件和文件夹
        for(File file:fi)
        {
            //如果是文件夹，递归查找
            if(file.isDirectory())
                delete(file);
            else if(file.isFile())
            {
                //是文件的话，把文件名放到一个字符串中
                String filename=file.getName();
                //如果是“repositories”后缀文件，返回一个boolean型的值
                if(filename.endsWith("repositories"))
                {
                    System.out.println("成功删除：："+file.getName());
                    file.delete();
                }
            }
        }
    }
}
