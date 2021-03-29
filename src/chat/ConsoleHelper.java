package chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message){
        System.out.println(message);
    }

    public static String readString(){
        while(true) {
            try {
                String string = reader.readLine();
                return string;
            } catch (IOException e) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
                readString();
            }
        }
    }

    public static int readInt(){

        while (true) {
            try {
                int integer = Integer.parseInt(readString());
                return integer;
            } catch (NumberFormatException e) {
                System.out.println("Произошла попытка при вводе числа. Попробуйте еще раз.");
            }
        }
    }

}
