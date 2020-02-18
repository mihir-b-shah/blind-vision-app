package com.apps.navai;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.physicaloid.lib.Physicaloid;

public class ArduinoInterface extends IntentService implements DataStream.SpeedListener {

    public static final String INPUT_FILTER = "string_9430493102";
    public static final String MESSAGE_KEY = "message_439403";
    public static final String MESSAGE_DONE = "message_done030";
    public static final String MESSAGE_CHANGE = "message_chg3902";

    private Physicaloid phy;
    private final ByteArray buf;
    private final DataStream stream;

    // computed as cos/sin of 2*pi*wheel_radius/walker_diameter
    // nonsense values
    private static final double WHEEL_RADIUS = 0.41;
    private static final double WALKER_DIAMETER = 1.02;

    private static int cA;
    private static int cB;

    class CartVector {
        private double x;
        private double y;

        public CartVector(double x, double y) {
            this.x = x; this.y = y;
        }

        public void straightWalk(double dist) {
            final double mgn = Math.sqrt(x*x+y*y);
            x += x*dist/mgn;
            y += y*dist/mgn;
        }
    }

    private final CartVector vector;

    @Override
    public void speedChanged(int id, double time) {
        switch(id) {
            case 0:
                ++cA;
                break;
            case 1:
                ++cB;
                break;
            default:
                System.err.printf("Error, %d is not valid wheel id.%n", id);
        }
        int decr = Math.min(cA, cB);
        cA -= decr; cB -= decr;
        vector.straightWalk(decr*Math.PI*WHEEL_RADIUS);

        double xAdj,yAdj;
        if(cA > 0) {
            xAdj = WALKER_DIAMETER/2*(1-Math.cos(cA*Math.PI*WHEEL_RADIUS/WALKER_DIAMETER));
            yAdj = WALKER_DIAMETER/2*Math.sin(cA*Math.PI*WHEEL_RADIUS/WALKER_DIAMETER);
        } else {
            xAdj = -WALKER_DIAMETER/2*(1-Math.cos(cB*Math.PI*WHEEL_RADIUS/WALKER_DIAMETER));
            yAdj = WALKER_DIAMETER/2*Math.sin(cB*Math.PI*WHEEL_RADIUS/WALKER_DIAMETER);
        }

        Intent intent = new Intent(Navigate.XY_FILTER);
        intent.putExtra("X", vector.x+xAdj);
        intent.putExtra("Y", vector.y+yAdj);
        sendBroadcast(intent);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(INPUT_FILTER)) {
                String message = intent.getStringExtra(MESSAGE_KEY);
                switch(message) {
                    case MESSAGE_DONE:
                        close();
                        stopSelf();
                        break;
                    case MESSAGE_CHANGE:
                        byte[] chgKey = {-1,0,0};
                        stream.enqueue(chgKey, chgKey.length);
                        break;
                    default:
                        System.err.println("Wrong message received.");
                }
            }
        }
    };

    public ArduinoInterface() {
        super("ArduinoInterface");
        stream = new DataStream(0.99f, this); // set
        buf = new ByteArray(12); // grows by expand()
        vector = new CartVector(0,0);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        registerReceiver(receiver, new IntentFilter(INPUT_FILTER));

        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            phy.addReadListener(i -> {
                buf.expand(i);
                phy.read(buf.getBuffer(), i);
                Log.v("Buffer val", buf.limString(i));
                stream.enqueue(buf.getBuffer(), i);
            });
        }
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
    }
}
