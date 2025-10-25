package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

public class Gateway {
    private Index index;
    private ExecutorService robotExecutor;
    private List<RobotWorker> activeRobots;
    private boolean running;
    private int robotCounter;
    
    public Gateway() {
        this.robotExecutor = Executors.newCachedThreadPool();
        this.activeRobots = Collections.synchronizedList(new ArrayList<>());
        this.running = true;
    }
    
    public boolean connectToIndex(String host, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            index = (Index) registry.lookup("index");
            System.out.println("Gateway connected to Index server at " + host + ":" + port);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to Index server: " + e.getMessage());
            return false;
        }
    }
    
    public void startGateway(int initialRobots) {
        if (index == null) {
            throw new IllegalStateException("Not connected to Index server");
        }
        
        System.out.println("Starting Gateway with " + initialRobots + " robot workers...");
        startRobots(initialRobots);
        
        // Gateway runs indefinitely, managing the workers
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        
        // Keep the gateway alive
        try {
            while (running) {
                Thread.sleep(30000); // Check status every 30 seconds
                monitorWorkers();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void startRobots(int count) {
        for (int i = 0; i < count; i++) {
            startRobotWorker();
        }
    }
    
    private void startRobotWorker() {
        RobotWorker worker = new RobotWorker(index, "Robot-" + (++robotCounter));
        robotExecutor.execute(worker);
        activeRobots.add(worker);
        System.out.println("Started robot worker: " + worker.getId());
    }
    
    private void monitorWorkers() {
        // Check for failed robots and restart them if needed
        synchronized (activeRobots) {
            Iterator<RobotWorker> iterator = activeRobots.iterator();
            while (iterator.hasNext()) {
                RobotWorker worker = iterator.next();
                if (!worker.isRunning()) {
                    System.out.println("Restarting failed robot: " + worker.getId());
                    iterator.remove();
                    startRobotWorker();
                }
            }
        }
        
        System.out.println("Gateway status: " + activeRobots.size() + " active robots");
    }
    
    public Index getIndex() {
        return index;
    }
    
    public void shutdown() {
        System.out.println("Shutting down Gateway...");
        running = false;
        
        // Stop all robots
        synchronized (activeRobots) {
            activeRobots.forEach(RobotWorker::stop);
            activeRobots.clear();
        }
        
        robotExecutor.shutdown();
        try {
            if (!robotExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                robotExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            robotExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        Gateway gateway = new Gateway();
        
        String host = "localhost";
        int port = 8184;
        int robotCount = 3; // Default number of robots
        
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);
        if (args.length >= 3) robotCount = Integer.parseInt(args[2]);
        
        if (gateway.connectToIndex(host, port)) {
            gateway.startGateway(robotCount);
        }
    }
}