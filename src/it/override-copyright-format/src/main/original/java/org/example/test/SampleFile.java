package org.example.test;
import java.io.File;


public class SampleFile {
	public static void main(String[] args) {
		File file = new File(args[0]);
		System.out.println(file.getAbsolutePath());
	}
}
