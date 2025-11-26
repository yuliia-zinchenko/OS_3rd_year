import java.util.Scanner;
import java.util.Locale;

public class Worker {
    public static void main(String[] args) {
        int functionType = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        String prefix = (functionType == 0) ? "[fn1 - x^2]" : "[fn2 - 2x]";

        System.err.println(prefix + " Process started.");

        try (Scanner scanner = new Scanner(System.in, "UTF-8").useLocale(Locale.US)) {
            
            if (scanner.hasNextDouble()) {
                double x = scanner.nextDouble();
                System.err.println(prefix + " Received input x = " + x);

                double result = calculate(x, functionType, prefix);
                
                System.err.println(prefix + " Computed result: " + result);
                System.err.println(prefix + " Sending result to Manager...");

                System.out.println(result); 
                
                System.err.println(prefix + " Work finished.");
            } else {
                System.err.println(prefix + " Error: Input stream empty or invalid format.");
            }
        } catch (Exception e) {
            System.err.println(prefix + " Exception: " + e.getMessage());
        }
    }

    private static double calculate(double x, int type, String prefix) throws InterruptedException {
        System.err.println(prefix + " Calculation started...");
        if (type == 0) {
            Thread.sleep(1000); 
            return x * x;
        } else {
            long sleepTime = (x > 100) ? 15000 : 3000; 
            if (sleepTime > 5000) System.err.println(prefix + " Heavy computation detected (will take 15s)...");
            Thread.sleep(sleepTime);
            return 2 * x;
        }
    }
}