package ohjelmointi3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		
		Scanner scanner = new Scanner(System.in);
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		File inputFile = new File("input.txt");
		PrintWriter printWriter = null;
		
		try {printWriter = new PrintWriter(inputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		String input;
		String formattedInput = null;
		
		System.out.println("Name?");
		String username = scanner.nextLine();
		
		boolean using = true;
		while (using) {
			System.out.println("<last> to show last input, <stop> to stop, other to make new input");
			input = scanner.nextLine();
			
			if(input.equals("last")) {

				System.out.println(formattedInput);
			} else
			if (input.equals("stop")) {
				using = false;
			}
			
			formattedInput = ("(" + timestamp.getTime() + ") "
					+ "<" + username + "> "
					+ input);
			
			printWriter.println(formattedInput);
		}
				
		printWriter.close();
		scanner.close();
		
	}

}
