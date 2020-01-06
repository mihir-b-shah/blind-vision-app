package communication;


import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Scanner;

public class GenData implements DataStream.SpeedListener {
    
    private static final byte[] bytes;  
    private static final int MASK = 0x7f;
    private static final int SHIFT_SMALL = 7;
    private static final int SHIFT_LARGE = 14;
    private static final int NUM_BYTES = 3;
    private static final Queue<Byte> queue;
    
    @Override
    public void speedChanged(double theta, double time) {
        System.out.printf("θ = %f, t = %f\n", theta, time);
    }
    
    static {
        bytes = new byte[NUM_BYTES];
        queue = new ArrayDeque<>();
    }
    
    public static void writeBytes(int val) {
        bytes[0] = (byte) (val & MASK);
        bytes[1] = (byte) ((val >> SHIFT_SMALL) & MASK);
        bytes[2] = (byte) ((val >> SHIFT_LARGE) & MASK);
    }
    
    public static void main(String[] args) throws Exception {
        GenData gd = new GenData();
        Scanner f = new Scanner(new File("C:\\Users\\mihir\\Documents\\"
                + "Arduino projects\\data9.txt"));
        DataStream stream = new DataStream(0.99, gd);        
        
        while(f.hasNextInt()) {
            int time = f.nextInt();
            int value = f.nextInt();
            
            if(time == 56516) { // specific to data file
                queue.add((byte) -1); // special case
                queue.add((byte) 0);
                queue.add((byte) 0);
            }
            
            writeBytes(time);
            for(int i = 0; i<3; ++i) {
                queue.add(bytes[i]);
            }
            
            writeBytes(value);
            for(int i = 0; i<3; ++i) {
                queue.add(bytes[i]);
            }
        }
        
        byte[] buffer = new byte[12]; // simplifies expansion
        while(!queue.isEmpty()) {
            int numBytes = 1 + (int) (12*Math.random());
            int i;
            for(i = 0; !queue.isEmpty() && i<numBytes; ++i) {
                buffer[i] = queue.poll();
            }
            stream.enqueue(buffer, i);
        }   
    }
}
