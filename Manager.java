import java.io.*;
import java.util.Scanner;
import java.util.concurrent.*;

public class Manager {
    private static long currentTimeoutLimit = 5000; 

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);
        System.out.println("=== Manager Started ===");
        System.out.println("Info: Press 's' + Enter at any time to stop computation manually.");

        while (true) {
            System.out.print("\nEnter x (or 'q' to quit): ");
            String input = console.next();

            if (input.equalsIgnoreCase("q")) {
                break;
            }

            try {
                double x = Double.parseDouble(input.replace(",", "."));
                currentTimeoutLimit = 5000; 
                runComputation(x, console);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        System.out.println("Manager stopped.");
        console.close(); 
    }

    private static void runComputation(double x, Scanner scanner) {
        System.out.println("--- Main System (Mn) Started ---");
        System.out.println("[Mn] Initial input x = " + x);
        System.out.println("[Mn] Initializing pipes and processes...");

        String classPath = System.getProperty("java.class.path");
        
        ProcessBuilder pb1 = new ProcessBuilder("java", "-cp", classPath, "Worker", "0");
        ProcessBuilder pb2 = new ProcessBuilder("java", "-cp", classPath, "Worker", "1");

        pb1.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb2.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process p1 = null;
        Process p2 = null;

        try {
            p1 = pb1.start();
            System.out.println("[Mn] fn1 (Worker 0) process started.");
            p2 = pb2.start();
            System.out.println("[Mn] fn2 (Worker 1) process started.");
        } catch (IOException e) {
            System.err.println("Failed to start processes: " + e.getMessage());
            return;
        }

        System.out.println("[Mn] Sending input to processes...");
        sendInput(p1, x);
        System.out.println("[Mn] Sent input '" + x + "' to fn1.");
        sendInput(p2, x);
        System.out.println("[Mn] Sent input '" + x + "' to fn2.");

        final Process process1 = p1;
        final Process process2 = p2;

        System.out.println("[Mn] Scheduling async read tasks...");
        
        CompletableFuture<Double> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Mn-Async] Waiting for result from fn1...");
            return readOutput(process1);
        });

        CompletableFuture<Double> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("[Mn-Async] Waiting for result from fn2...");
            return readOutput(process2);
        });

        CompletableFuture<Double> finalResult = future1.thenCombine(future2, (res1, res2) -> {
            System.out.println("[Mn-Async] Both results received. Combining: " + res1 + " + " + res2);
            return res1 + res2;
        });

        long startTime = System.currentTimeMillis();
        boolean finished = false;
        System.out.println("[Mn] Entering wait loop with timeout limit: " + currentTimeoutLimit + "ms");

        while (!finished) {

            if (finalResult.isDone()) {
                try {
                    Double result = finalResult.get();
                    System.out.println("--------------------------------");
                    System.out.printf(java.util.Locale.US, "Result: %.4f\n", result);
                    System.out.println("--------------------------------");
                    finished = true;
                } catch (Exception e) {
                    System.out.println("Error getting result: " + e.getMessage());
                }
                break;
            }

            long currentTime = System.currentTimeMillis();
            
            if (currentTime - startTime > currentTimeoutLimit) {
                System.out.println("\n[!] Timeout reached!");
                finished = handleMenu(scanner, future1, future2, process1, process2);

            }

            try {
                if (System.in.available() > 0) {

                    while(System.in.available() > 0) {
                        System.in.read(); 
                    }
                    
                    System.out.println("\n[!] Manual interruption!");
                    finished = handleMenu(scanner, future1, future2, process1, process2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
    }

    private static boolean handleMenu(Scanner sc, CompletableFuture<?> f1, CompletableFuture<?> f2, Process p1, Process p2) {
        
        while (true) {
            System.out.println("\nSelect an action:");
            System.out.println("1. Continue waiting (Extend time +5s)");
            System.out.println("2. Check status");
            System.out.println("3. Cancel everything");
            System.out.print("> ");

            String choice = sc.next();

            if (choice.equals("1")) {
                currentTimeoutLimit += 5000; 
                System.out.println("-> Extended waiting time limit to " + (currentTimeoutLimit/1000) + " seconds total...");
                return false; 

            } else if (choice.equals("2")) {
                System.out.println("\n--- STATUS ---");
                System.out.println("Worker 1 (x^2): " + (f1.isDone() ? "Completed" : "Running..."));
                System.out.println("Worker 2 (2*x): " + (f2.isDone() ? "Completed" : "Running..."));
                System.out.println("--------------");
                
            } else if (choice.equals("3")) {
                System.out.println("-> Cancelling...");
                f1.cancel(true);
                f2.cancel(true);
                p1.destroyForcibly();
                p2.destroyForcibly();
                return true;
                
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    private static void sendInput(Process p, double x) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
            writer.write(String.valueOf(x));
            writer.newLine(); 
            writer.flush();
            writer.close(); 
        } catch (IOException e) {
            System.err.println("Error sending input: " + e.getMessage());
        }
    }

    private static Double readOutput(Process p) {
        try (Scanner sc = new Scanner(p.getInputStream(), "UTF-8").useLocale(java.util.Locale.US)) {
            if (sc.hasNextDouble()) {
                return sc.nextDouble();
            } else {
                throw new RuntimeException("No number returned");
            }
        }
    }
}